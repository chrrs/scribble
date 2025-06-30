pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://maven.architectury.dev")
        maven("https://maven.neoforged.net/releases/")
        maven("https://maven.kikugie.dev/snapshots")
        gradlePluginPortal()
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.7-beta.4"
}

stonecutter {
    kotlinController = true
    centralScript = "build.gradle.kts"

    create(rootProject) {
        versions("1.21.6", "1.21.7")
        branch("fabric") { versions("1.21.6", "1.21.7") }
        branch("neoforge") { versions("1.21.6", "1.21.7") }
    }
}

rootProject.name = "Scribble"
