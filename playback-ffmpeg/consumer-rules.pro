# JNI entry points and the callback registered by Media3's FFmpeg module.
-keep,includedescriptorclasses class androidx.media3.decoder.ffmpeg.** { native <methods>; }
-keep,includedescriptorclasses class androidx.media3.decoder.ffmpeg.FfmpegAudioDecoder {
    private java.nio.ByteBuffer growOutputBuffer(androidx.media3.decoder.SimpleDecoderOutputBuffer, int);
}
# DefaultRenderersFactory discovers this renderer by name.
-keep class androidx.media3.decoder.ffmpeg.FfmpegAudioRenderer { public <init>(...); }
