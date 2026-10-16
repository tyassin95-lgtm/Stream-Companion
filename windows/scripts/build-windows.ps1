# Builds the self-contained Windows installer on Windows: dist\StreamCompanion-Setup-<version>.exe
$ErrorActionPreference = 'Stop'
$here = Split-Path -Parent $PSScriptRoot
$dist = Join-Path (Split-Path -Parent $here) 'dist'
Set-Location $here

if (-not (Get-Command node -ErrorAction SilentlyContinue)) { throw 'Node.js 18+ is required (https://nodejs.org)' }

Write-Host '==> Installing dependencies'
npm ci --no-audit --no-fund
if ($LASTEXITCODE) { throw 'npm ci failed' }

Write-Host '==> Fetching bundled ffmpeg.exe'
node scripts/fetch-ffmpeg.js
if ($LASTEXITCODE) { throw 'ffmpeg download failed' }

if ($env:SKIP_TESTS -ne '1') {
  Write-Host '==> Running tests'
  npm test
  if ($LASTEXITCODE) { throw 'tests failed' }
}

Write-Host '==> Building installer'
if (Test-Path release) { Remove-Item -Recurse -Force release }
npx electron-builder --win nsis --x64 --publish never
if ($LASTEXITCODE) { throw 'electron-builder failed' }

New-Item -ItemType Directory -Force $dist | Out-Null
$exe = Get-ChildItem release\StreamCompanion-Setup-*.exe | Select-Object -First 1
Copy-Item $exe.FullName $dist
Write-Host "==> Done: $dist\$($exe.Name)"
