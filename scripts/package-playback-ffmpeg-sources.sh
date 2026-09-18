#!/usr/bin/env bash
# Package the exact retained native build inputs alongside any distributed APK.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
WORK="${SPOLE_NATIVE_WORK:?Set SPOLE_NATIVE_WORK to the completed native build directory}"
OUT="$ROOT/playback-ffmpeg/build/distribution"
STAGE="$(mktemp -d -t spole-ffmpeg-sources-XXXXXX)"
mkdir -p "$OUT" "$STAGE/scripts" "$STAGE/playback-ffmpeg"
cp "$WORK/ffmpeg-source.tar.gz" "$WORK/ffmpeg_jni.cc" "$STAGE/"
cp "$ROOT/scripts/build-playback-ffmpeg.sh" "$STAGE/scripts/"
cp -R "$ROOT/playback-ffmpeg/licenses" "$STAGE/playback-ffmpeg/"
cp "$ROOT/playback-ffmpeg/README.md" "$STAGE/playback-ffmpeg/"
for abi in armeabi-v7a arm64-v8a x86 x86_64; do
    mkdir -p "$STAGE/$abi"
    cp "$WORK/$abi/config.h" "$WORK/$abi/config_components.h" "$STAGE/$abi/"
    cp "$WORK/$abi/libavcodec/libavcodec.a" "$WORK/$abi/libswresample/libswresample.a" \
       "$WORK/$abi/libavutil/libavutil.a" "$STAGE/$abi/"
done
tar -czf "$OUT/spole-ffmpeg-source-and-relink.tar.gz" -C "$STAGE" .
sha256sum "$OUT/spole-ffmpeg-source-and-relink.tar.gz"
echo "Staging inputs retained at $STAGE"
