#!/bin/bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="${PREVIEW_OUTPUT:-$ROOT/work/ANDROID模擬.app}"
mkdir -p "$OUT/Contents/MacOS" "$OUT/Contents/Resources" "$ROOT/work/swift-cache"
xcrun swiftc -module-cache-path "$ROOT/work/swift-cache" "$ROOT/desktop-preview/main.swift" -o "$OUT/Contents/MacOS/AppAtlasPreview" -framework Cocoa -framework WebKit
ICONSET="$ROOT/work/AndroidWave.iconset"
mkdir -p "$ICONSET"
xcrun swift -module-cache-path "$ROOT/work/swift-cache" "$ROOT/desktop-preview/icon.swift" "$ROOT/work/AndroidWave.png"
for size in 16 32 128 256 512; do
 sips -z "$size" "$size" "$ROOT/work/AndroidWave.png" --out "$ICONSET/icon_${size}x${size}.png" >/dev/null
 doubled=$((size * 2))
 sips -z "$doubled" "$doubled" "$ROOT/work/AndroidWave.png" --out "$ICONSET/icon_${size}x${size}@2x.png" >/dev/null
done
python3 - "$ICONSET" "$OUT/Contents/Resources/AndroidWave.icns" <<'ICON'
import struct,sys
from pathlib import Path
root=Path(sys.argv[1]);chunks=[]
for kind,name in [('icp4','icon_16x16.png'),('icp5','icon_32x32.png'),('icp6','icon_32x32@2x.png'),('ic07','icon_128x128.png'),('ic08','icon_256x256.png'),('ic09','icon_512x512.png'),('ic10','icon_512x512@2x.png')]:
 data=(root/name).read_bytes();chunks.append(kind.encode()+struct.pack('>I',len(data)+8)+data)
body=b''.join(chunks);Path(sys.argv[2]).write_bytes(b'icns'+struct.pack('>I',len(body)+8)+body)
ICON
cp -R "$ROOT/android/assets" "$OUT/Contents/Resources/"
python3 - "$OUT" "$ROOT/android/assets" <<'PY'
import plistlib,sys
from pathlib import Path
info=dict(CFBundleExecutable='AppAtlasPreview',CFBundleIdentifier='local.appatlas.desktoppreview',CFBundleName='ANDROID模擬',CFBundlePackageType='APPL',CFBundleVersion='2',CFBundleIconFile='AndroidWave.icns',NSHighResolutionCapable=True,AtlasAssetsPath=sys.argv[2])
with (Path(sys.argv[1])/'Contents/Info.plist').open('wb') as f:plistlib.dump(info,f)
PY
printf '%s\n' "$OUT"
