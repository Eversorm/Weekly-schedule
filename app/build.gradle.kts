// Build configuration of the app module.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)       // Jetpack Compose compiler
    alias(libs.plugins.kotlin.serialization) // JSON export/import
    alias(libs.plugins.ksp)                  // code generator used by Room
}

android {
    namespace = "com.eversorm.weeklyschedule"
    // Android SDK version used to compile: Android 17 (API 37), minor update 2.
    compileSdk {
        version = release(37) {
            minorApiLevel = 2
        }
    }

    defaultConfig {
        applicationId = "com.eversorm.weeklyschedule"
        minSdk = 26        // Android 8.0: oldest Android version the app runs on
        targetSdk = 37     // Android 17: the version the app is designed and tested for
        versionCode = 1    // increase by 1 for every APK you install as an update
        versionName = "0.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

ksp {
    // Room writes a JSON description of the database here. Commit it: it is needed
    // to write safe migrations when the database structure changes later.
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
}
