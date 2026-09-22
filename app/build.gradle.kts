plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

import java.util.Properties

val signingProperties = Properties().apply {
    val propertiesFile = rootProject.file("signing.properties")
    if (propertiesFile.exists()) {
        propertiesFile.inputStream().use(::load)
    }
}

val hasStableReleaseSigning = listOf(
    "storeFile",
    "storePassword",
    "keyAlias",
    "keyPassword",
).all { signingProperties.getProperty(it).isNullOrBlank().not() }
val allowsCiUnsignedRelease = providers.gradleProperty("spole.allowUnsignedRelease").orNull == "true" &&
    System.getenv("CI").equals("true", ignoreCase = true)

tasks.matching { it.name == "validateSigningRelease" || it.name == "packageRelease" }.configureEach {
    doFirst {
        check(hasStableReleaseSigning || allowsCiUnsignedRelease) {
            "Produksjonsbygg krev signing.properties og den eksisterande Spole-nøkkelen. Ikkje lag ein ny nøkkel."
        }
    }
}

android {
    namespace = "app.reelstack"

    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "app.reelstack"
        minSdk = 26
        targetSdk = 36
        versionCode = 126
        versionName = "0.18.0-beta6"

        testInstrumentationRunner = "app.reelstack.SpoleTestRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            if (hasStableReleaseSigning) {
                signingConfig = signingConfigs.create("stableRelease") {
                    storeFile = rootProject.file(signingProperties.getProperty("storeFile"))
                    storePassword = signingProperties.getProperty("storePassword")
                    keyAlias = signingProperties.getProperty("keyAlias")
                    keyPassword = signingProperties.getProperty("keyPassword")
                }
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Both small UI catalogues must be available when a user switches language offline.
    bundle {
        language {
            enableSplit = false
        }
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    testOptions {
        unitTests {
            // Robolectric needs the merged resources and the manifest to stand up a real window.
            isIncludeAndroidResources = true
            all { it.systemProperty("robolectric.logging.enabled", "false") }
        }
    }

    // A sentence written into a Composable is a sentence that exists in one language only, and
    // nothing catches it later: the app still builds, still runs, and quietly shows English —
    // or nynorsk — to everyone. Lint sees every one of them, so the rule is an error rather than
    // a warning in a list of eighty. The build is not set to abort on it: the point is that the
    // report cannot show one, and `lintDebug` says so in its exit code.
    lint {
        error += "HardcodedText"
    }
}

composeCompiler {
    // What the collections in this project promise, and why. Reports are off by default; add
    //   reportsDestination = layout.buildDirectory.dir("compose_reports")
    //   metricsDestination = layout.buildDirectory.dir("compose_reports")
    // here and build with --rerun-tasks to measure stability again.
    stabilityConfigurationFiles.add(layout.projectDirectory.file("compose_stability.conf"))
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.06.00")

    implementation(composeBom)
    androidTestImplementation(composeBom)

    // core-ktx 1.19 and lifecycle 2.11 both require compileSdk 37, which is a platform decision
    // and not a dependency one — `checkDebugAarMetadata` refuses the build outright. They stay here
    // until the compileSdk bump is made deliberately, with the re-verification that deserves.
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.work:work-runtime-ktx:2.11.2")
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    // Spole draws its own icons. The extended Material set was the last thing pulling a
    // second visual language into the app, and with every glyph now in SpoleIcons there is
    // nothing left that references it.
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("io.coil-kt.coil3:coil-compose:3.4.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.4.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.media3:media3-exoplayer:1.11.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.11.1")
    implementation("androidx.media3:media3-ui:1.11.1")
    implementation("androidx.media3:media3-session:1.11.1")
    implementation("androidx.media3:media3-datasource-okhttp:1.11.1")
    implementation("androidx.media3:media3-datasource:1.11.1")
    implementation("androidx.media3:media3-database:1.11.1")
    implementation(project(":playback-ffmpeg"))
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    // The sheet freeze of 16 September was a coroutine-cancellation bug. A virtual clock reproduces
    // it in milliseconds on the JVM; an emulator was never the right instrument for it.
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    // Robolectric runs the Compose tests that genuinely need a window — a dialog that has to be
    // leavable is one of them — on the JVM. The alternative was an emulator this machine cannot
    // start without a root password, which is not a dependency the test suite should have.
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation(composeBom)
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("androidx.test.ext:junit:1.3.0")
    debugImplementation(composeBom)
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
