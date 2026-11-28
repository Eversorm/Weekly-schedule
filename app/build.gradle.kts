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
        // Must grow with every update. On GitHub it is the build number (1, 2, 3, ...),
        // so you never have to change it by hand.
        val buildNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionCode = buildNumber
        versionName = "0.1.$buildNumber"
    }

    // The release key. It is never stored in the repository: GitHub passes it to the
    // build through environment variables filled from the repository's secrets
    // (see .github/workflows/build.yml).
    // .trim() removes spaces/line breaks accidentally copied together with a secret.
    fun secret(name: String): String? = System.getenv(name)?.trim()
    val keystorePath = secret("SIGNING_KEYSTORE_PATH")
    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = secret("SIGNING_STORE_PASSWORD")
                keyAlias = secret("SIGNING_KEY_ALIAS")
                keyPassword = secret("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (keystorePath != null) {
                signingConfig = signingConfigs.getByName("release")
            }
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
