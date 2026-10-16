#!/usr/bin/env bash
# Builds the self-contained Windows installer (dist/StreamCompanion-Setup-<version>.exe).
# Works on Linux/macOS (cross-build, needs Wine) and in Git Bash on Windows.
set -euo pipefail
HERE="$(cd "$(dirname "$0")/.." && pwd)"
DIST="$(cd "$HERE/.." && pwd)/dist"
cd "$HERE"

command -v node >/dev/null || { echo "Node.js 18+ is required"; exit 1; }

echo "==> Installing dependencies"
npm ci --no-audit --no-fund

echo "==> Fetching bundled ffmpeg.exe"
node scripts/fetch-ffmpeg.js

if [ "${SKIP_TESTS:-0}" != "1" ]; then
  echo "==> Running tests"
  npm test
fi

case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*) ;;   # native Windows: nothing special needed
  *)
    # electron-builder needs Wine to stamp the icon / version info into the .exe and to run the NSIS stub.
    if ! command -v wine >/dev/null 2>&1 && ! command -v wine64 >/dev/null 2>&1 && [ ! -x /usr/lib/wine/wine ]; then
      echo "Wine is required to cross-build the installer on this OS."
      echo "  Debian/Ubuntu:  sudo dpkg --add-architecture i386 && sudo apt-get update && sudo apt-get install -y wine64 wine32:i386 xvfb"
      exit 1
    fi
    export PATH="$HERE/scripts/wine-shim:$PATH"
    export WINEDEBUG="${WINEDEBUG:--all}"
    ;;
esac

echo "==> Building installer"
rm -rf release
npx electron-builder --win nsis --x64 --publish never

mkdir -p "$DIST"
EXE="$(ls release/StreamCompanion-Setup-*.exe | head -1)"
cp "$EXE" "$DIST/"
echo "==> Done: $DIST/$(basename "$EXE") ($(du -h "$EXE" | cut -f1))"
