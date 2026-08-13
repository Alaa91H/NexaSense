plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.nexasense.presentation"
    compileSdk = 36

    defaultConfig {
        minSdk = 31
        // Instrumented (Compose UI) tests: run with `:presentation:connectedDebugAndroidTest`.
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    // Window size classes: adaptive navigation (bottom bar vs. rail) driven
    // by the available window width, not by device type.
    implementation(libs.compose.material3.windowsizeclass)
    debugImplementation(libs.compose.ui.tooling)

    // Instrumented Compose UI tests (see src/androidTest). Mirrors the app
    // module's setup: the compose-ui-test-manifest debug artifact provides
    // the empty ComponentActivity that createComposeRule() hosts, and
    // kotlinx-coroutines-test is declared in both configurations so its
    // META-INF/services exception-handler registration lands in the test APK
    // (otherwise instrumented tests fail with "Exception handler was not
    // found via a ServiceLoader").
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    debugImplementation(libs.compose.ui.test.manifest)
    debugImplementation(libs.kotlinx.coroutines.test)
}
