plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.nexasense.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.nexasense.android"
        minSdk = 31
        targetSdk = 36
        versionCode = 11
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    signingConfigs {
        // Production signing is driven entirely by environment variables
        // (GitHub Actions secrets in CI, or local env vars for maintainers).
        // Nothing secret lives in the repository. When no keystore is
        // configured, the release build falls back to the debug key — this is
        // documented in the release notes and is NOT production signing.
        create("release") {
            val keystorePath = System.getenv("NEXASENSE_KEYSTORE_PATH")
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("NEXASENSE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("NEXASENSE_KEY_ALIAS")
                keyPassword = System.getenv("NEXASENSE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            val hasKeystore = System.getenv("NEXASENSE_KEYSTORE_PATH") != null
            signingConfig = if (hasKeystore) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            // R8 minification + resource shrinking: smaller APK and
            // obfuscated release code. Safe because the app uses no
            // reflection/serialization (see proguard-rules.pro). Debug builds
            // stay unminified for fast iteration.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":core"))
    implementation(project(":data"))
    implementation(project(":presentation"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    // Explicit coroutines-test for androidTest: compose-ui-test pulls it in
    // transitively, but its META-INF/services exception-handler registration
    // is only packaged when declared directly (otherwise instrumented tests
    // fail with "Exception handler was not found via a ServiceLoader").
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
    // kotlinx-coroutines-test registers its exception collector via
    // META-INF/services. The instrumented-test process resolves services from
    // the main APK's classloader, which only sees coroutines-android's handler
    // — so the test handler must also be packaged into the (debug) app APK,
    // where AGP merges the service files of both AARs. Without this, Compose
    // UI tests fail with "Exception handler was not found via a ServiceLoader".
    debugImplementation(libs.kotlinx.coroutines.test)
}
