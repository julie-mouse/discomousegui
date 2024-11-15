import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    kotlin("plugin.serialization") version "1.8.10"
}

kotlin {
    jvm("desktop")
    
    sourceSets {
        val desktopMain by getting
        
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.materialIconsExtended)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)

            implementation("dev.kord:kord-core:0.11.1")
            implementation("ch.qos.logback:logback-classic:1.4.11")

            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
            implementation("io.ktor:ktor-client-core:2.0.3")
            implementation("io.ktor:ktor-client-content-negotiation:2.0.3")
            implementation("io.ktor:ktor-client-cio:2.0.3")
            implementation("io.ktor:ktor-client-serialization:2.0.3")
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
        }
    }
}


compose.desktop {
    application {
        mainClass = "org.jmouse.project.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi)
            packageName = "DiscomouseGUI"
            packageVersion = "1.0.4"

            includeAllModules = true
        }
    }
}
