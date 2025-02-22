plugins {
    id("dev.architectury.loom")
    id("architectury-plugin")
    id("com.github.johnrengelman.shadow")
}

fun Project.hasProp(namespace: String, key: String) = hasProperty("$namespace.$key")
fun Project.prop(namespace: String, key: String) = property("$namespace.$key") as String

val minecraft = stonecutter.current.version
val common = requireNotNull(stonecutter.node.sibling(""))

version = "${common.prop("mod", "version")}+mc$minecraft"
base.archivesName.set("${prop("mod", "name")}-neoforge")

architectury {
    platformSetupLoomIde()
    neoForge()
}

val commonBundle: Configuration by configurations.creating
val shadowBundle: Configuration by configurations.creating

configurations {
    compileClasspath.get().extendsFrom(commonBundle)
    runtimeClasspath.get().extendsFrom(commonBundle)
    get("developmentNeoForge").extendsFrom(commonBundle)
}

repositories {
    maven("https://maven.neoforged.net/releases/")
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft")

    @Suppress("UnstableApiUsage")
    mappings(loom.layered {
        mappings("net.fabricmc:yarn:${common.prop("fabric", "yarnVersion")}:v2")
        mappings("dev.architectury:yarn-mappings-patch-neoforge:${common.prop("neoforge", "yarnPatch")}")
    })

    neoForge("net.neoforged:neoforge:${common.prop("neoforge", "version")}")

    commonBundle(project(common.path, "namedElements")) { isTransitive = false }
    shadowBundle(project(common.path, "transformProductionNeoForge")) { isTransitive = false }
}

loom {
    accessWidenerPath = common.project.loom.accessWidenerPath

    runConfigs.all {
        isIdeConfigGenerated = false
        runDir = "../../../run"
        vmArgs("-Dmixin.debug.export=true")
    }
}

java {
    val java = if (stonecutter.eval(minecraft, ">=1.20.5"))
        JavaVersion.VERSION_21 else JavaVersion.VERSION_17
    targetCompatibility = java
    sourceCompatibility = java
}

tasks {
    shadowJar {
        configurations = listOf(shadowBundle)
        archiveClassifier = "dev-shadow"
    }

    remapJar {
        atAccessWideners.add("scribble.accesswidener")
        inputFile.set(shadowJar.get().archiveFile)
        archiveClassifier = null
        dependsOn(shadowJar)
    }

    jar {
        archiveClassifier = "dev"
    }

    processResources {
        // We construct our minecraft dependency string based on the versions provided in gradle.properties
        val platformVersions = common.prop("platform", "versions").split(",")
        val minecraftDependency = if (platformVersions.size == 1)
            "[${platformVersions.first()}]" else "[${platformVersions.first()}, ${platformVersions.last()}]"

        filesMatching("META-INF/neoforge.mods.toml") {
            expand(
                "version" to common.prop("mod", "version"),
                "minecraft" to minecraftDependency,
            )
        }
    }

    build {
        group = "versioned"
        description = "Must run through `chiseledBuild`"
    }

    register<Copy>("buildAndCollect") {
        group = "versioned"
        description = "Must run through `chiseledBuild`"

        from(remapJar.get().archiveFile)
        into(rootProject.layout.buildDirectory.file("libs"))
        dependsOn(build)
    }
}