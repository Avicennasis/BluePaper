plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.compose.compiler) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
}

subprojects {
    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "io.netty") {
                useVersion("4.1.135.Final")
            }
            if (requested.group == "org.bouncycastle" && requested.name.startsWith("bc")) {
                useVersion("1.84")
            }
            if (requested.group == "org.bitbucket.b_c" && requested.name == "jose4j") {
                useVersion("0.9.6")
            }
            if (requested.group == "org.jdom" && requested.name == "jdom2") {
                useVersion("2.0.6.1")
            }
            if (requested.group == "org.apache.commons" && requested.name == "commons-lang3") {
                useVersion("3.18.0")
            }
            if (requested.group == "org.apache.httpcomponents" && requested.name == "httpclient") {
                useVersion("4.5.13")
            }
            if (requested.group == "io.opentelemetry" && requested.name == "opentelemetry-api") {
                useVersion("1.62.0")
            }
        }
    }
}
