plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

fun envKey(name: String): String =
    (System.getenv(name) ?: "").replace("\\", "").replace("\"", "").trim()

android {
    namespace = "com.ultron.ai"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ultron.ai"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        // Optional defaults baked in from GitHub secrets. Keys typed in the app's Settings override these.
        buildConfigField("String", "DEFAULT_GEMINI_KEY", "\"${envKey("GEMINI_API_KEY")}\"")
        buildConfigField("String", "DEFAULT_ELEVEN_KEY", "\"${envKey("ELEVEN_API_KEY")}\"")
        buildConfigField("String", "DEFAULT_VOICE_ID", "\"${envKey("ELEVEN_VOICE_ID")}\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
