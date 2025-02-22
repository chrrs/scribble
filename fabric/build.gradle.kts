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
base.archivesName.set("${prop("mod", "name")}-fabric")

architectury {
    platformSetupLoomIde()
    fabric()
}

val commonBundle: Configuration by configurations.creating
val shadowBundle: Configuration by configurations.creating

configurations {
    compileClasspath.get().extendsFrom(commonBundle)
    runtimeClasspath.get().extendsFrom(commonBundle)
    get("developmentFabric").extendsFrom(commonBundle)
}

repositories {
    maven("https://maven.terraformersmc.com/releases/")
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft")
    mappings("net.fabricmc:yarn:${common.prop("fabric", "yarnVersion")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${common.prop("fabric", "loaderVersion")}")

    fun fabricApiModule(name: String) =
        modImplementation(fabricApi.module(name, common.prop("fabric", "apiVersion")))
    include(fabricApiModule("fabric-resource-loader-v0")!!)

    modCompileOnly("com.terraformersmc:modmenu:${common.prop("modmenu", "version")}")

    commonBundle(project(common.path, "namedElements")) { isTransitive = false }
    shadowBundle(project(common.path, "transformProductionFabric")) { isTransitive = false }
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
        injectAccessWidener = true
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
            platformVersions.first() else ">=${platformVersions.first()} <=${platformVersions.last()}"

        filesMatching("fabric.mod.json") {
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