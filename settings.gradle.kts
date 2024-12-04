import dev.kikugie.stonecutter.StonecutterSettings

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
    id("dev.kikugie.stonecutter") version "0.4.2"
}

extensions.configure<StonecutterSettings> {
    shared {
        fun add(version: String, vararg loaders: String) =
            loaders.forEach { vers("$version-$it", version) }

        add("1.20.1", "fabric", "forge")
        add("1.21", "fabric", "neoforge")
        add("1.21.4", "fabric", "neoforge")
        vcsVersion = "1.21.4-fabric"
    }

    kotlinController = true
    centralScript = "build.gradle.kts"
    create(rootProject)
}

rootProject.name = "Scribble"
