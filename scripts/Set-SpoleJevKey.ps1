#requires -Version 7.0
<#
.SYNOPSIS
Prøv Jev og lagre nøkkelen trygt som GitHub-secret for Spole.
.DESCRIPTION
Opprett først ein nøkkel på https://console.typesafe.ai. Nøkkelen blir lesen
skjult, sendt via miljøet til Python og via stdin til gh; aldri som CLI-argument.
24 avgrensa API-kall kan bli fakturerte av TypeSafe. Ingen automatisk retry.
#>
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$jevRoot = Split-Path $PSScriptRoot -Parent
Get-Command gh, python -ErrorAction Stop | Out-Null
& gh auth status --hostname github.com
if ($LASTEXITCODE -ne 0) { throw 'Logg inn med gh auth login først.' }

$jevSecure = Read-Host 'TypeSafe API-nøkkel (skjult; ikkje lim inn i chat)' -AsSecureString
if ($jevSecure.Length -eq 0) { throw 'Ingen nøkkel oppgitt.' }
$jevPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($jevSecure)
$jevPreviousKey = $env:TYPESAFE_API_KEY
try {
    $jevPlain = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($jevPointer)
    $env:TYPESAFE_API_KEY = $jevPlain
    & python "$PSScriptRoot/jev-review.py" --cases "$PSScriptRoot/jev/cases.json" --live --max-calls 24 --output "$jevRoot/app/build/jev-calibration"
    if ($LASTEXITCODE -ne 0) {
        throw 'Prøven vart ikkje fullført. Sjå app/build/jev-calibration/report.md; GitHub er ikkje aktivert.'
    }

    $jevStart = [Diagnostics.ProcessStartInfo]::new()
    $jevStart.FileName = (Get-Command gh).Source
    foreach ($jevArgument in @('secret', 'set', 'TYPESAFE_API_KEY', '--repo', 'oyvhov/spole-android')) {
        $jevStart.ArgumentList.Add($jevArgument)
    }
    $jevStart.UseShellExecute = $false
    $jevStart.CreateNoWindow = $true
    $jevStart.RedirectStandardInput = $true
    $jevStart.RedirectStandardOutput = $true
    $jevStart.RedirectStandardError = $true
    $jevProcess = [Diagnostics.Process]::Start($jevStart)
    try {
        $jevStdout = $jevProcess.StandardOutput.ReadToEndAsync()
        $jevStderr = $jevProcess.StandardError.ReadToEndAsync()
        $jevProcess.StandardInput.Write($jevPlain)
        $jevProcess.StandardInput.Close()
        $jevProcess.WaitForExit()
        $null = $jevStdout.GetAwaiter().GetResult()
        $null = $jevStderr.GetAwaiter().GetResult()
        if ($jevProcess.ExitCode -ne 0) { throw 'Kunne ikkje lagre GitHub-secret. Kontroller repo-tilgangen.' }
    } finally {
        $jevProcess.Dispose()
    }
    & gh variable set JEV_ENABLED --repo oyvhov/spole-android --body true
    if ($LASTEXITCODE -ne 0) { throw 'Nøkkelen er lagra, men JEV_ENABLED kunne ikkje aktiverast.' }
    Write-Host 'Jev er tilgjengeleg som valfri rådgjeving ved manuell CI-køyring på hovudgreina. Sjå kalibreringsrapporten for faktisk treff.'
} finally {
    $env:TYPESAFE_API_KEY = $jevPreviousKey
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($jevPointer)
    $jevPlain = $null
    $jevSecure.Dispose()
}
