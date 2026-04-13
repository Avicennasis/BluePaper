plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    androidTarget()

    sourceSets {
        val androidMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(compose.material3)
                implementation(libs.androidx.activity.compose)
            }
        }
    }
}

android {
    namespace = "com.avicennasis.bluepaper"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.avicennasis.bluepaper"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.7.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
