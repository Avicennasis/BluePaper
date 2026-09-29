// The plugin/buildscript classpath is resolved separately from the project
// configurations below, so `subprojects { configurations.all { ... } }` never
// reaches it. AGP/signing tooling pulls a vulnerable bcprov onto that classpath,
// which Dependabot attributes to settings.gradle.kts (alerts #57 critical, #58
// high; first patched 1.85). Force the same fixed version here. Evaluated before
// `plugins {}`, so it applies to the classpath that block resolves.
buildscript {
    configurations.classpath {
        resolutionStrategy.force(
            "org.bouncycastle:bcprov-jdk18on:1.85",
            "org.bouncycastle:bcpkix-jdk18on:1.85",
            "org.bouncycastle:bcutil-jdk18on:1.85",
        )
    }
}

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
                useVersion("1.85")
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
