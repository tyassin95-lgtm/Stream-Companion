#!/usr/bin/env bash
# Rebuilds both deliverables into dist/:  ./build.sh [windows|android|all]
set -euo pipefail
cd "$(dirname "$0")"
what="${1:-all}"
[ "$what" = "windows" ] || [ "$what" = "all" ] && windows/scripts/build-windows.sh
[ "$what" = "android" ] || [ "$what" = "all" ] && android/scripts/build-android.sh
( cd dist && sha256sum StreamCompanion-* > SHA256SUMS.txt && cat SHA256SUMS.txt )
