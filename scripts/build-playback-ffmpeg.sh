#!/usr/bin/env bash
# Build the official Media3 audio extension, with no GPL/nonfree codecs or video decoder.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
NDK="${ANDROID_NDK_HOME:?Set ANDROID_NDK_HOME to Linux NDK r27b}"
WORK="${SPOLE_NATIVE_WORK:-$(mktemp -d -t spole-ffmpeg-XXXXXX)}"
MEDIA_REV=8c6678b657ede1e7883fc164ef73ed483c7796c3
FFMPEG_REV=34277e12e80031c7f89494ba543684bc1dd0be8f
MODULE="$ROOT/playback-ffmpeg"
BIN="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin"
mkdir -p "$WORK" "$MODULE/src/main/java/androidx/media3/decoder/ffmpeg" "$MODULE/licenses"
for file in FfmpegAudioDecoder.java FfmpegAudioRenderer.java FfmpegDecoderException.java FfmpegLibrary.java package-info.java; do
    curl -fsSL --retry 3 "https://raw.githubusercontent.com/androidx/media/$MEDIA_REV/libraries/decoder_ffmpeg/src/main/java/androidx/media3/decoder/ffmpeg/$file" \
        -o "$MODULE/src/main/java/androidx/media3/decoder/ffmpeg/$file"
done
curl -fsSL --retry 3 "https://raw.githubusercontent.com/androidx/media/$MEDIA_REV/libraries/decoder_ffmpeg/src/main/jni/ffmpeg_jni.cc" -o "$WORK/ffmpeg_jni.cc"
curl -fsSL --retry 3 "https://raw.githubusercontent.com/androidx/media/$MEDIA_REV/LICENSE" -o "$MODULE/licenses/Apache-2.0.txt"
curl -fsSL --retry 3 "https://codeload.github.com/FFmpeg/FFmpeg/tar.gz/$FFMPEG_REV" -o "$WORK/ffmpeg-source.tar.gz"
tar -xzf "$WORK/ffmpeg-source.tar.gz" -C "$WORK"
SOURCE="$WORK/FFmpeg-$FFMPEG_REV"
cp "$SOURCE/COPYING.LGPLv2.1" "$MODULE/licenses/LGPL-2.1.txt"
mkdir -p "$MODULE/src/main/assets/licenses/ffmpeg"
cp "$MODULE/licenses/"*.txt "$MODULE/src/main/assets/licenses/ffmpeg/"
for abi in armeabi-v7a arm64-v8a x86 x86_64; do
    case "$abi" in
        armeabi-v7a) arch=arm; target=armv7a-linux-androideabi; extra=(--cpu=armv7-a --extra-cflags=-march=armv7-a) ;;
        arm64-v8a) arch=aarch64; target=aarch64-linux-android; extra=(--cpu=armv8-a) ;;
        x86) arch=x86; target=i686-linux-android; extra=(--cpu=i686 --disable-asm) ;;
        x86_64) arch=x86_64; target=x86_64-linux-android; extra=(--cpu=x86-64 --disable-asm) ;;
    esac
    mkdir -p "$WORK/$abi" "$MODULE/src/main/jniLibs/$abi"
    cd "$WORK/$abi"
    "$SOURCE/configure" --target-os=android --arch="$arch" --enable-cross-compile \
        --cc="$BIN/${target}26-clang" --cxx="$BIN/${target}26-clang++" \
        --ar="$BIN/llvm-ar" --ranlib="$BIN/llvm-ranlib" --strip="$BIN/llvm-strip" --nm="$BIN/llvm-nm" \
        --enable-static --disable-shared --enable-pic --disable-gpl --disable-nonfree \
        --disable-doc --disable-programs --disable-everything --disable-autodetect \
        --disable-avdevice --disable-avformat --disable-swscale --disable-postproc --disable-avfilter \
        --disable-symver --enable-swresample --disable-v4l2-m2m --disable-vulkan \
        --enable-decoder=aac,ac3,eac3,dca,truehd,mlp,flac,alac,mp3,vorbis,opus,pcm_mulaw,pcm_alaw \
        "${extra[@]}" > configure.log 2>&1
    make -j"${SPOLE_NATIVE_JOBS:-4}" > build.log 2>&1
    "$BIN/${target}26-clang++" -shared -std=c++17 -fPIC -O2 -fvisibility=hidden \
        -I"$SOURCE" -I. "$WORK/ffmpeg_jni.cc" -Wl,--start-group \
        libavcodec/libavcodec.a libswresample/libswresample.a libavutil/libavutil.a \
        -Wl,--end-group -Wl,-Bsymbolic -Wl,-z,max-page-size=16384 -Wl,-soname,libffmpegJNI.so \
        -static-libstdc++ -llog -landroid -lm -o "$MODULE/src/main/jniLibs/$abi/libffmpegJNI.so"
    "$BIN/llvm-strip" --strip-unneeded "$MODULE/src/main/jniLibs/$abi/libffmpegJNI.so"
    echo "Built $abi"
done
echo "Native build and relinking inputs kept at $WORK"
