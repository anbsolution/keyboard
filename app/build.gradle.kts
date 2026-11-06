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
        versionCode = 6
        versionName = "1.3.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    testImplementation("junit:junit:4.13.2")
}
