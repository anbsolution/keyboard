plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.voicetype.keyboard"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.voicetype.keyboard"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.1"
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
