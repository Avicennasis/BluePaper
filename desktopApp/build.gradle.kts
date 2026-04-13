plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    jvmToolchain(17)
    jvm("desktop")

    sourceSets {
        val desktopMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(compose.desktop.currentOs)
                implementation(compose.material3)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.avicennasis.bluepaper.MainKt"

        nativeDistributions {
            packageName = "BluePaper"
            packageVersion = "0.3.0"
        }
    }
}
