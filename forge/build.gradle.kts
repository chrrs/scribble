import me.modmuss50.mpp.ReleaseType

plugins {
    id("dev.architectury.loom")
    id("architectury-plugin")
    id("com.github.johnrengelman.shadow")
    id("me.modmuss50.mod-publish-plugin")
}

fun Project.hasProp(namespace: String, key: String) = hasProperty("$namespace.$key")
fun Project.prop(namespace: String, key: String) = property("$namespace.$key") as String

val minecraft = stonecutter.current.version
val common = requireNotNull(stonecutter.node.sibling(""))

version = "${common.prop("mod", "version")}+mc$minecraft-forge"
base.archivesName.set(prop("mod", "name"))

architectury {
    platformSetupLoomIde()
    forge()
}

val commonBundle: Configuration by configurations.creating
val shadowBundle: Configuration by configurations.creating

configurations {
    compileClasspath.get().extendsFrom(commonBundle)
    runtimeClasspath.get().extendsFrom(commonBundle)
    get("developmentForge").extendsFrom(commonBundle)
}

repositories {
    maven("https://maven.minecraftforge.net")
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft")
    mappings("net.fabricmc:yarn:${common.prop("fabric", "yarnVersion")}:v2")
    forge("net.minecraftforge:forge:${common.prop("forge", "version")}")

    compileOnly(annotationProcessor("io.github.llamalad7:mixinextras-common:0.4.1")!!)
    implementation(include("io.github.llamalad7:mixinextras-forge:0.4.1")!!)

    commonBundle(project(common.path, "namedElements")) { isTransitive = false }
    shadowBundle(project(common.path, "transformProductionForge")) { isTransitive = false }
}

loom {
    accessWidenerPath = common.project.loom.accessWidenerPath
    forge.convertAccessWideners = true

    forge.mixinConfigs("scribble.mixins.json")
    @Suppress("UnstableApiUsage")
    mixin.useLegacyMixinAp = false

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

        filesMatching("META-INF/mods.toml") {
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

    publishMods.get().dependsOn("buildAndCollect")
}

publishMods {
    val versions = common.prop("platform", "versions").split(",")
    val modVersion = common.prop("mod", "version")
    changelog.set(providers.environmentVariable("CHANGELOG"))

    type = when {
        modVersion.contains("alpha") -> ReleaseType.ALPHA
        modVersion.contains("beta") -> ReleaseType.BETA
        else -> ReleaseType.STABLE
    }

    displayName.set("$modVersion - Forge $minecraft")
    version.set(project.version.toString())
    modLoaders.addAll(prop("platform", "loaders").split(","))
    file.set(tasks.remapJar.get().archiveFile)

    modrinth {
        projectId.set(prop("modrinth", "id"))
        accessToken.set(providers.environmentVariable("MODRINTH_TOKEN"))
        minecraftVersions.addAll(versions)
        optional("cloth-config")
    }

    curseforge {
        projectId.set(prop("curseforge", "id"))
        accessToken.set(providers.environmentVariable("CURSEFORGE_TOKEN"))
        minecraftVersions.addAll(versions)
        optional("cloth-config")
    }
}