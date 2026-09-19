param(
    [string]$Version = "0.17.0-beta33"
)

$ErrorActionPreference = "Stop"
$tag = "v$Version"
$out = "app/build/release-$tag"

if (!(Test-Path $out)) {
    New-Item -ItemType Directory -Path $out -Force | Out-Null
}

$apk = "$out/Spole-$tag.apk"
$mapping = "$out/mapping-$tag.txt"
$ffmpeg = "$out/spole-ffmpeg-source-and-relink.tar.gz"

Copy-Item "app/build/outputs/apk/release/app-release.apk" $apk -Force
Copy-Item "app/build/outputs/mapping/release/mapping.txt" $mapping -Force
Copy-Item "playback-ffmpeg/build/distribution/spole-ffmpeg-source-and-relink.tar.gz" $ffmpeg -Force

$sdk = "$env:LOCALAPPDATA/Android/Sdk/build-tools/36.0.0"

Write-Host "Verifying APK signature..."
& "$sdk/apksigner.bat" verify --print-certs $apk
if ($LASTEXITCODE -ne 0) { throw "Invalid APK signature!" }

Write-Host "Verifying APK badging..."
& "$sdk/aapt.exe" dump badging $apk | Select-String "package: name="

$apkHash = (Get-FileHash $apk -Algorithm SHA256).Hash.ToLower()
$ffmpegHash = (Get-FileHash $ffmpeg -Algorithm SHA256).Hash.ToLower()

$shaContent = "$apkHash  Spole-$tag.apk`n$ffmpegHash  spole-ffmpeg-source-and-relink.tar.gz`n"
Set-Content -Path "$out/SHA256SUMS.txt" -Value $shaContent -Encoding Ascii

Write-Host "Packaging complete for $tag!"
Write-Host "APK Hash: $apkHash"
Write-Host "FFmpeg Hash: $ffmpegHash"
