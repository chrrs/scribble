pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://maven.architectury.dev")
        maven("https://maven.neoforged.net/releases/")
        maven("https://maven.kikugie.dev/releases")
        gradlePluginPortal()
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.5.1"
}

stonecutter {
    kotlinController = true
    centralScript = "build.gradle.kts"

    create(rootProject) {
        versions("1.20.1", "1.21", "1.21.4", "1.21.5")
        branch("fabric") { versions("1.20.1", "1.21", "1.21.4", "1.21.5") }
//        branch("forge") { versions("1.20.1") }
        branch("neoforge") { versions("1.21", "1.21.4", "1.21.5") }
    }
}

rootProject.name = "Scribble"
