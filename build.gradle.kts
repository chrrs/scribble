import me.modmuss50.mpp.ReleaseType

plugins {
    id("dev.architectury.loom") version "1.7-SNAPSHOT"
    id("me.modmuss50.mod-publish-plugin") version "0.5.1"
}

fun Project.hasProp(namespace: String, key: String) = hasProperty("$namespace.$key")
fun Project.prop(namespace: String, key: String) = property("$namespace.$key") as String

val minecraftVersion = stonecutter.current.version
val loader = loom.platform.get().name.lowercase()

group = prop("mod", "group")
version = "${prop("mod", "version")}+mc$minecraftVersion-$loader"

if (stonecutter.current.isActive) {
    rootProject.tasks.register("runActive") {
        group = "project"
        dependsOn(tasks.named("runClient"))
    }
}

base {
    archivesName.set(prop("mod", "name"))
}

loom {
    accessWidenerPath = rootProject.file("src/main/resources/scribble.accesswidener")

    runConfigs.forEach { it.ideConfigGenerated(false) }
    runConfigs["client"].runDir = "../../run"

    if (loader == "forge") {
        forge.mixinConfig("scribble.mixins.json")
    }
}

repositories {
    maven("https://maven.neoforged.net/releases/")
    maven("https://maven.terraformersmc.com/releases/")
    maven("https://maven.shedaniel.me/")
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")

    // Yarn mappings
    @Suppress("UnstableApiUsage")
    mappings(loom.layered {
        mappings("net.fabricmc:yarn:${prop("fabric", "yarnVersion")}:v2")

        if (hasProp("neoforge", "yarnPatch")) {
            mappings("dev.architectury:yarn-mappings-patch-neoforge:${prop("neoforge", "yarnPatch")}")
        }
    })

    // Loader dependencies
    when (loader) {
        "fabric" -> {
            fun fabricApiModule(name: String) =
                modImplementation(fabricApi.module(name, prop("fabric", "apiVersion")))

            modImplementation("net.fabricmc:fabric-loader:${prop("fabric", "loaderVersion")}")
            include(fabricApiModule("fabric-resource-loader-v0")!!)
        }

        "neoforge" -> "neoForge"("net.neoforged:neoforge:${prop("neoforge", "version")}")
        "forge" -> "forge"("net.minecraftforge:forge:${prop("forge", "version")}")
    }

    // Third party mod compat dependencies
    if (loader == "fabric") modCompileOnly("com.terraformersmc:modmenu:${prop("modmenu", "version")}")
    modCompileOnly("me.shedaniel.cloth:cloth-config-$loader:${prop("clothconfig", "version")}")

    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.3")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.3")
    testImplementation("org.mockito:mockito-core:5.12.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.3")
}

tasks {
    processResources {
        // We construct our minecraft dependency string based on the versions provided in gradle.properties
        val gameVersions = prop("platform", "versions").split(",")
        val first = gameVersions.firstOrNull()!!
        val last = gameVersions.lastOrNull()!!

        // Inject the properties into the mod manifest file
        if (loader == "fabric") {
            inputs.property("version", project.version)
            filesMatching("fabric.mod.json") {
                expand(
                    "modVersion" to prop("mod", "version"),
                    "minecraftDependency" to if (gameVersions.size == 1) first else ">=$first <=$last"
                )
            }

            exclude("META-INF/mods.toml")
            exclude("META-INF/neoforge.mods.toml")
        } else if (loader == "forge" || loader == "neoforge") {
            filesMatching("META-INF/*mods.toml") {
                expand(
                    "modVersion" to prop("mod", "version"),
                    "minecraftDependency" to if (gameVersions.size == 1) "[$first]" else "[$first, $last]"
                )
            }

            exclude("fabric.mod.json")

            if (loader == "neoforge" && stonecutter.eval(minecraftVersion, ">=1.20.5")) {
                exclude("META-INF/mods.toml")
            } else {
                exclude("META-INF/neoforge.mods.toml")
            }
        }
    }

    if (loader == "neoforge" || loader == "forge") {
        remapJar {
            atAccessWideners.add("scribble.accesswidener")
        }
    }

    jar {
        from("LICENSE")
    }

    "test"(Test::class) {
        useJUnitPlatform()

        // To log test status in console
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}

java {
    val version =
        if (stonecutter.eval(minecraftVersion, ">=1.20.6")) JavaVersion.VERSION_21
        else JavaVersion.VERSION_17
    sourceCompatibility = version
    targetCompatibility = version
}

publishMods {
    val displayLoader = when (loader) {
        "forge" -> "Forge"
        "fabric" -> "Fabric"
        "neoforge" -> "NeoForge"
        else -> loader
    }

    displayName.set("${prop("mod", "version")} - $displayLoader $minecraftVersion")

    file.set(tasks.remapJar.get().archiveFile)
    changelog.set(providers.environmentVariable("CHANGELOG"))
    type.set(if (prop("mod", "version").contains("beta")) ReleaseType.BETA else ReleaseType.STABLE)
    modLoaders.addAll(prop("platform", "loaders").split(","))

    val gameVersions = prop("platform", "versions").split(",")

    modrinth {
        projectId.set(prop("modrinth", "id"))
        accessToken.set(providers.environmentVariable("MODRINTH_TOKEN"))
        minecraftVersions.addAll(gameVersions)
        optional("cloth-config")
    }

    curseforge {
        projectId.set(prop("curseforge", "id"))
        accessToken.set(providers.environmentVariable("CURSEFORGE_TOKEN"))
        minecraftVersions.addAll(gameVersions)
        optional("cloth-config")
    }
}