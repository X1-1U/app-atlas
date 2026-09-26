#!/bin/bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="${PREVIEW_OUTPUT:-$ROOT/work/拾檔手機預覽.app}"
mkdir -p "$OUT/Contents/MacOS" "$OUT/Contents/Resources" "$ROOT/work/swift-cache"
xcrun swiftc -module-cache-path "$ROOT/work/swift-cache" "$ROOT/desktop-preview/main.swift" -o "$OUT/Contents/MacOS/AppAtlasPreview" -framework Cocoa -framework WebKit
cp -R "$ROOT/android/assets" "$OUT/Contents/Resources/"
python3 - "$OUT" "$ROOT/android/assets" <<'PY'
import plistlib,sys
from pathlib import Path
info=dict(CFBundleExecutable='AppAtlasPreview',CFBundleIdentifier='local.appatlas.desktoppreview',CFBundleName='拾檔手機預覽',CFBundlePackageType='APPL',CFBundleVersion='1',NSHighResolutionCapable=True,AtlasAssetsPath=sys.argv[2])
with (Path(sys.argv[1])/'Contents/Info.plist').open('wb') as f:plistlib.dump(info,f)
PY
printf '%s\n' "$OUT"
