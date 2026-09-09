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

tasks.matching { it.name == "validateSigningRelease" || it.name == "packageRelease" }.configureEach {
    doFirst {
        check(hasStableReleaseSigning) {
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
        versionCode = 49
        versionName = "0.16.0-alpha06"

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
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.06.00")

    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.work:work-runtime-ktx:2.11.2")
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
    implementation("io.coil-kt.coil3:coil-compose:3.4.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.4.0")
    implementation("androidx.media3:media3-exoplayer:1.10.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.10.1")
    implementation("androidx.media3:media3-ui:1.10.1")
    implementation("androidx.media3:media3-session:1.10.1")
    implementation("androidx.media3:media3-datasource-okhttp:1.10.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
