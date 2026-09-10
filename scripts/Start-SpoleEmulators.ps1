#requires -Version 7.0
<#
.SYNOPSIS
Start the existing Spole TV/phone profiles with their saved accounts and data.
.EXAMPLE
pwsh -File .\scripts\Start-SpoleEmulators.ps1
.EXAMPLE
pwsh -File .\scripts\Start-SpoleEmulators.ps1 -Device Phone -RestartApp
.NOTES
Windows + Ubuntu/WSL launcher for this workstation. Never creates or wipes AVDs,
installs an APK, clears app data, or runs instrumentation against real accounts.
#>
[CmdletBinding()]
param(
    [ValidateSet('Both', 'TV', 'Phone')][string]$Device = 'Both',
    [switch]$NoWindows,
    [switch]$RestartApp,
    [string]$Distro = 'Ubuntu',
    [string]$DataRoot = 'C:\JellyBin',
    [string]$LinuxSdk = '/home/oyvhov/Android/Sdk',
    [string]$WindowsSdk = "$env:LOCALAPPDATA\Android\Sdk",
    [string]$ScrcpyPath,
    [ValidateRange(60, 600)][int]$BootTimeoutSeconds = 240,
    [ValidateRange(5, 120)][int]$NetworkTimeoutSeconds = 45
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$logRoot = Join-Path $repo 'app/build/emulators'
$runDir = Join-Path $logRoot (Get-Date -Format 'yyyyMMdd-HHmmss-fff')
$adb = Join-Path $WindowsSdk 'platform-tools/adb.exe'
$wsl = (Get-Command wsl.exe -ErrorAction Stop).Source
$mutex = [Threading.Mutex]::new($false, 'Local\SpoleSavedEmulatorLauncher')
$locked = $false

function Invoke-Tool {
    param([string]$File, [string[]]$Arguments, [int]$TimeoutSeconds = 20)
    $info = [Diagnostics.ProcessStartInfo]::new()
    $info.FileName = $File
    $info.UseShellExecute = $false
    $info.CreateNoWindow = $true
    $info.RedirectStandardOutput = $true
    $info.RedirectStandardError = $true
    foreach ($argument in $Arguments) { $info.ArgumentList.Add($argument) }
    $process = [Diagnostics.Process]::new()
    $process.StartInfo = $info
    try {
        [void]$process.Start()
        $stdout = $process.StandardOutput.ReadToEndAsync()
        $stderr = $process.StandardError.ReadToEndAsync()
        if (!$process.WaitForExit($TimeoutSeconds * 1000)) {
            $process.Kill($true)
            throw "Command timed out: $File ($TimeoutSeconds seconds). No emulator data was changed."
        }
        [pscustomobject]@{ Code = $process.ExitCode; Output = $stdout.GetAwaiter().GetResult().Trim(); Error = $stderr.GetAwaiter().GetResult().Trim() }
    } finally { $process.Dispose() }
}

function Invoke-Linux {
    param([string[]]$Command)
    Invoke-Tool $wsl (@('-d', $Distro, '-u', 'root', '--exec') + $Command) -TimeoutSeconds 45
}

function Require-Success {
    param($Result, [string]$Action)
    if ($Result.Code -ne 0) { throw "$Action failed: $($Result.Error) $($Result.Output)" }
    $Result.Output
}

function Start-LoggedProcess {
    param([string]$File, [string[]]$Arguments, [string]$LogName, [switch]$Visible)
    # Start-Process joins its argument array. Quote whitespace for Windows;
    # WSL option names must remain unquoted for its command-line parser.
    # no shell interprets these values. Embedded quotes are deliberately rejected.
    $quoted = foreach ($argument in $Arguments) {
        if ($argument.Contains('"') -or $argument.EndsWith('\')) { throw 'Unsupported quote or trailing backslash in process argument.' }
        if ($argument -match '\s' -or $argument.Length -eq 0) { '"' + $argument + '"' }
        else { $argument }
    }
    $options = @{
        FilePath = $File; ArgumentList = $quoted; PassThru = $true
        RedirectStandardOutput = (Join-Path $runDir "$LogName.log")
        RedirectStandardError = (Join-Path $runDir "$LogName-error.log")
        WindowStyle = $(if ($Visible) { 'Normal' } else { 'Hidden' })
    }
    Start-Process @options
}

function Invoke-Adb {
    param([string[]]$Command, [int]$TimeoutSeconds = 20)
    Invoke-Tool $adb (@('-H', $script:adbHost, '-P', '5037') + $Command) -TimeoutSeconds $TimeoutSeconds
}

try {
    try { $locked = $mutex.WaitOne(0) } catch [Threading.AbandonedMutexException] { $locked = $true }
    if (!$locked) { throw 'Another Spole emulator launcher is already running. Wait for it to finish.' }
    if (!(Test-Path -LiteralPath $adb)) { throw "Windows adb is missing: $adb" }
    New-Item -ItemType Directory -Path $runDir -Force | Out-Null

    if (!$NoWindows -and !$ScrcpyPath) {
        $command = Get-Command scrcpy.exe -ErrorAction SilentlyContinue
        if ($command) { $ScrcpyPath = $command.Source }
        else {
            $packages = Join-Path $env:LOCALAPPDATA 'Microsoft/WinGet/Packages'
            $candidates = @(Get-ChildItem -Path "$packages/Genymobile.scrcpy_*/scrcpy-win64-*/scrcpy.exe" -ErrorAction SilentlyContinue)
            $candidate = $candidates | Sort-Object LastWriteTime -Descending | Select-Object -First 1
            if ($candidate) { $ScrcpyPath = $candidate.FullName }
        }
    }
    if (!$NoWindows -and (!$ScrcpyPath -or !(Test-Path -LiteralPath $ScrcpyPath))) {
        throw 'scrcpy is missing. Supply -ScrcpyPath or use -NoWindows.'
    }

    $linuxRoot = Require-Success (Invoke-Linux @('wslpath', '-a', '-u', $DataRoot)) 'Resolve AVD data path'
    [void](Require-Success (Invoke-Linux @('test', '-x', "$LinuxSdk/emulator/emulator")) 'Find Linux emulator')
    [void](Require-Success (Invoke-Linux @('test', '-x', "$LinuxSdk/platform-tools/adb")) 'Find Linux adb')
    $addresses = Require-Success (Invoke-Linux @('hostname', '-I')) 'Find WSL address'
    $script:adbHost = $addresses -split '\s+' | Where-Object { $_ -match '^\d{1,3}(\.\d{1,3}){3}$' } | Select-Object -First 1
    if (!$script:adbHost) { throw 'No WSL IPv4 address found.' }

    $profiles = @(
        [pscustomobject]@{ Device = 'TV'; Name = 'Spole_GoogleTV_Test'; Home = '.spole-tv-avds'; Port = 5564; Gpu = @('-gpu', 'swiftshader'); Title = 'Spole - Android TV'; Window = @('--window-x=440', '--window-y=100', '--window-width=1200'); Tunnel = 27190 },
        [pscustomobject]@{ Device = 'Phone'; Name = 'Spole_Review'; Home = '.spole-review-avds'; Port = 5560; Gpu = @('-gpu', 'swangle', '-feature', '-Vulkan'); Title = 'Spole - Mobil'; Window = @('--window-x=25', '--window-y=60', '--window-height=850', '--max-size=2048'); Tunnel = 27191 }
    ) | Where-Object { $Device -eq 'Both' -or $_.Device -eq $Device }

    # Preflight every requested profile before starting any background processes.
    foreach ($profile in $profiles) {
        $avdDirectory = Join-Path $DataRoot "$($profile.Home)/$($profile.Name).avd"
        foreach ($file in @('config.ini', 'userdata-qemu.img')) {
            if (!(Test-Path -LiteralPath (Join-Path $avdDirectory $file))) {
                throw "Saved $($profile.Device) profile is missing: $avdDirectory/$file. Restore the existing profile; do not create an empty replacement."
            }
        }
        if (!(Test-Path -LiteralPath (Join-Path $DataRoot "$($profile.Home)/$($profile.Name).ini"))) { throw "Missing AVD registration for $($profile.Name)." }
    }

    $devices = Invoke-Adb @('devices')
    if ($devices.Code -ne 0) {
        Write-Host "Starting shared WSL adb at ${script:adbHost}:5037..."
        $server = Start-LoggedProcess $wsl @('-d', $Distro, '-u', 'root', '--exec', "$LinuxSdk/platform-tools/adb", '-a', '-P', '5037', 'nodaemon', 'server') 'adb-server'
        $deadline = (Get-Date).AddSeconds(30)
        do {
            Start-Sleep -Seconds 1
            $devices = Invoke-Adb @('devices')
            if ($devices.Code -eq 0) { break }
            if ($server.HasExited) { throw "adb server exited. See $runDir/adb-server-error.log. Do not kill another adb server blindly." }
        } while ((Get-Date) -lt $deadline)
        [void](Require-Success $devices 'Connect to WSL adb')
    }

    $processes = Require-Success (Invoke-Linux @('ps', '-eo', 'args')) 'Inspect running emulators'
    foreach ($profile in $profiles) {
        $namePattern = '(^|\s)-avd\s+' + [regex]::Escape($profile.Name) + '(\s|$)'
        $portPattern = '(^|\s)-port\s+' + $profile.Port + '(\s|$)'
        $running = @($processes -split "`n" | Where-Object { $_ -match 'qemu-system' -and $_ -match $namePattern })
        $onPort = @($processes -split "`n" | Where-Object { $_ -match 'qemu-system' -and $_ -match $portPattern })
        if (($running.Count -gt 0 -and !($running[0] -match $portPattern)) -or ($onPort.Count -gt 0 -and !($onPort[0] -match $namePattern))) {
            throw "AVD/port conflict for $($profile.Name) on $($profile.Port). Existing processes have been left untouched."
        }
        $serial = "emulator-$($profile.Port)"
        if ($running.Count -gt 0) { Write-Host "Reusing $($profile.Device): $serial" }
        else {
            if ($devices.Output -match "(?m)^$serial\s") { throw "Unrecognised device already uses $serial. Refusing to replace it." }
            Write-Host "Starting saved $($profile.Device) profile: $($profile.Name)..."
            $arguments = @('-d', $Distro, '-u', 'root', '--exec', '/usr/bin/env', "ANDROID_AVD_HOME=$linuxRoot/$($profile.Home)", "$LinuxSdk/emulator/emulator", '-avd', $profile.Name, '-port', "$($profile.Port)") + $profile.Gpu + @('-memory', '1536', '-no-window', '-no-audio', '-no-snapshot')
            [void](Start-LoggedProcess $wsl $arguments "$($profile.Device)-emulator")
        }
    }

    $results = @()
    foreach ($profile in $profiles) {
        $serial = "emulator-$($profile.Port)"
        $deadline = (Get-Date).AddSeconds($BootTimeoutSeconds)
        $nextNotice = Get-Date
        do {
            $boot = Invoke-Adb @('-s', $serial, 'shell', 'getprop', 'sys.boot_completed')
            if ($boot.Code -eq 0 -and $boot.Output -eq '1') { break }
            if ((Get-Date) -ge $nextNotice) { Write-Host "Waiting for Android on $serial..."; $nextNotice = (Get-Date).AddSeconds(15) }
            Start-Sleep -Seconds 3
        } while ((Get-Date) -lt $deadline)
        if ($boot.Output -ne '1') { throw "Android boot timed out on $serial. Logs: $runDir. No profile was reset." }

        $package = Invoke-Adb @('-s', $serial, 'shell', 'pm', 'path', 'app.reelstack')
        if ($package.Code -ne 0 -or $package.Output -notmatch 'package:') { throw "Spole production package is missing on $serial. This launcher does not install or replace apps." }
        $networkReady = $false
        $deadline = (Get-Date).AddSeconds($NetworkTimeoutSeconds)
        do {
            $network = Invoke-Adb @('-s', $serial, 'shell', 'dumpsys', 'connectivity')
            if ($network.Output -match '(?m)^\s*NetworkAgentInfo\{[^\r\n]*Capabilities:[^\r\n]*&VALIDATED[&\s]') { $networkReady = $true; break }
            Start-Sleep -Seconds 3
        } while ((Get-Date) -lt $deadline)
        if (!$networkReady) { Write-Warning "$serial has no validated network yet. Saved accounts remain intact; live data may need a later -RestartApp." }

        [void](Require-Success (Invoke-Adb @('-s', $serial, 'shell', 'input', 'keyevent', '224')) 'Wake screen')
        [void](Require-Success (Invoke-Adb @('-s', $serial, 'shell', 'wm', 'dismiss-keyguard')) 'Dismiss emulator keyguard')
        if ($RestartApp) { [void](Require-Success (Invoke-Adb @('-s', $serial, 'shell', 'am', 'force-stop', 'app.reelstack')) 'Restart Spole') }
        $launch = Invoke-Adb @('-s', $serial, 'shell', 'am', 'start', '-W', '-n', 'app.reelstack/.MainActivity') -TimeoutSeconds 45
        [void](Require-Success $launch 'Open Spole')
        if ($launch.Output -match '(?m)^Error:') { throw $launch.Output }

        $windowState = 'not requested'
        if (!$NoWindows) {
            $serialPattern = '--serial"?[=\s]+"?' + [regex]::Escape($serial) + '(?:\s|"|$)'
            $existing = @(Get-CimInstance Win32_Process -Filter "Name = 'scrcpy.exe'" | Where-Object { $_.CommandLine -match $serialPattern })
            if ($existing.Count -gt 0) { $windowState = 'reused' }
            else {
                $oldSocket = $env:ADB_SERVER_SOCKET
                try {
                    $env:ADB_SERVER_SOCKET = "tcp:${script:adbHost}:5037"
                    $arguments = @('--serial', $serial, '--force-adb-forward', "--tunnel-host=$script:adbHost", "--port=$($profile.Tunnel)", "--tunnel-port=$($profile.Tunnel)", '--no-audio', '--window-title', $profile.Title) + $profile.Window
                    $mirror = Start-LoggedProcess $ScrcpyPath $arguments "$($profile.Device)-window" -Visible
                    Start-Sleep -Seconds 2
                    if ($mirror.HasExited) { throw "scrcpy exited for $serial. See $runDir/$($profile.Device)-window-error.log." }
                    $windowState = 'opened'
                } finally { $env:ADB_SERVER_SOCKET = $oldSocket }
            }
        }
        $results += [pscustomobject]@{ Device = $profile.Device; Serial = $serial; Avd = $profile.Name; AdbHost = $script:adbHost; NetworkValidated = $networkReady; Window = $windowState; App = 'app.reelstack'; Logs = $runDir }
        Write-Host "Spole ready on $serial; network=$networkReady; window=$windowState."
    }
    $results | ConvertTo-Json -Depth 3 | Set-Content -LiteralPath (Join-Path $logRoot 'last-start.json') -Encoding utf8
    $results
} finally {
    if ($locked) { $mutex.ReleaseMutex() }
    $mutex.Dispose()
}
