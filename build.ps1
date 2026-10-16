# Rebuilds the Windows installer (and the APK when the Android SDK + bash are available): .\build.ps1
$ErrorActionPreference = 'Stop'
& "$PSScriptRoot\windows\scripts\build-windows.ps1"
if ((Get-Command bash -ErrorAction SilentlyContinue) -and $env:ANDROID_HOME) {
  bash "$PSScriptRoot/android/scripts/build-android.sh"
} else {
  Write-Host 'Skipping the APK (needs bash + ANDROID_HOME). Build it with: bash android/scripts/build-android.sh'
}
