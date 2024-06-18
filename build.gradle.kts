import me.modmuss50.mpp.ReleaseType

plugins {
    id("fabric-loom") version "1.6-SNAPSHOT"
    id("me.modmuss50.mod-publish-plugin") version "0.5.1"
}

fun Project.prop(namespace: String, key: String) =
    property("$namespace.$key") as String

val minecraftVersion = stonecutter.current.version

group = prop("mod", "group")
version = "${prop("mod", "version")}+mc$minecraftVersion"

base {
    archivesName.set(prop("mod", "name"))
}

loom {
    accessWidenerPath = rootProject.file("src/main/resources/scribble.accesswidener")

    runConfigs["client"].ideConfigGenerated(true)
    runConfigs["client"].runDir = "../../run"

    runConfigs["server"].ideConfigGenerated(false)
}

repositories {
}

dependencies {
    // To change the versions, see the gradle.properties file

    fun fabricApiModule(name: String) =
        modImplementation(fabricApi.module(name, prop("fabric", "apiVersion")))

    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings("net.fabricmc:yarn:${prop("fabric", "yarnVersion")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${prop("fabric", "loaderVersion")}")

    include(fabricApiModule("fabric-resource-loader-v0")!!)
}

// renderButton was changed to renderWidget after 1.20.3
stonecutter.swap("renderWidget") {
    if (stonecutter.compare(minecraftVersion, "1.20.3") >= 0) {
        "protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {"
    } else {
        "protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {"
    }
}

tasks {
    processResources {
        inputs.property("version", project.version)
        filesMatching("fabric.mod.json") {
            expand(
                "modName" to prop("mod", "name"),
                "modVersion" to prop("mod", "version"),
                "minecraftDependency" to ">=${prop("minecraft", "minVersion")} <=${prop("minecraft", "maxVersion")}"
            )
        }
    }

    jar {
        from("LICENSE")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

publishMods {
    displayName.set("${prop("mod", "version")} - Fabric $minecraftVersion")

    file.set(tasks.remapJar.get().archiveFile)
    changelog.set(providers.environmentVariable("CHANGELOG"))
    type.set(if (prop("mod", "version").contains("beta")) ReleaseType.BETA else ReleaseType.STABLE)
    modLoaders.addAll("fabric", "quilt")

    modrinth {
        projectId.set(prop("modrinth", "id"))
        accessToken.set(providers.environmentVariable("MODRINTH_TOKEN"))

        minecraftVersionRange {
            start = prop("minecraft", "minVersion")
            end = prop("minecraft", "maxVersion")
        }
    }
}