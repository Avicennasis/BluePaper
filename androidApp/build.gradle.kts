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
    compileSdk = 36

    defaultConfig {
        applicationId = "com.avicennasis.bluepaper"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "0.9.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

configurations.all {
    resolutionStrategy {
        force("androidx.core:core:1.19.0")
        force("androidx.core:core-ktx:1.19.0")
        force(
            "io.netty:netty-codec-http2:4.2.17.Final",
            "io.netty:netty-codec-http:4.2.17.Final",
            "io.netty:netty-codec:4.2.17.Final",
            "io.netty:netty-common:4.2.17.Final",
            "io.netty:netty-handler:4.2.17.Final",
            "io.netty:netty-handler-proxy:4.2.17.Final",
            "com.google.protobuf:protobuf-java:4.35.1",
            "org.bouncycastle:bcprov-jdk18on:1.85.2",
            "org.bouncycastle:bcpkix-jdk18on:1.85",
            "org.bitbucket.b_c:jose4j:0.9.6",
            "org.jdom:jdom2:2.0.6.1",
            "commons-io:commons-io:2.22.0",
        )
    }
}
