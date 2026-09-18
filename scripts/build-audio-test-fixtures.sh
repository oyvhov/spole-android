#!/usr/bin/env bash
# Rebuild synthetic 5.1 fixtures; no copyrighted soundtrack or production media is used.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
FFMPEG="${SPOLE_FFMPEG:-ffmpeg}"
ASSETS="$ROOT/app/src/androidTest/assets/player"
for codec in dca ac3 eac3 truehd; do
    name="$codec"
    if [[ "$codec" == dca ]]; then name=dts; fi
    "$FFMPEG" -hide_banner -loglevel error -y -i "$ASSETS/video.mp4" \
        -f lavfi -i sine=frequency=440:sample_rate=48000 -map 0:v:0 -map 1:a:0 \
        -c:v copy -af 'pan=5.1|FL=c0|FR=c0|FC=c0|LFE=c0|BL=c0|BR=c0' \
        -c:a "$codec" -strict -2 -t 20 "$ASSETS/$name.mkv"
done
