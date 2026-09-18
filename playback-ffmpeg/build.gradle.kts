plugins { id("com.android.library") }

android {
    namespace = "app.reelstack.ffmpeg"
    compileSdk = 36
    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.media3:media3-exoplayer:1.11.1")
    implementation("androidx.media3:media3-decoder:1.11.1")
    compileOnly("androidx.annotation:annotation:1.9.1")
    compileOnly("org.checkerframework:checker-qual:3.49.1")
}

tasks.named("preBuild") {
    doFirst {
        for (abi in listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")) {
            check(file("src/main/jniLibs/$abi/libffmpegJNI.so").isFile) {
                "Missing FFmpeg for $abi. Run scripts/build-playback-ffmpeg.sh in Linux/WSL; see playback-ffmpeg/README.md."
            }
        }
    }
}
