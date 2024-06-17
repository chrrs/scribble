plugins {
    id("fabric-loom") version "1.6-SNAPSHOT"
}

val minecraftVersion: String by project
val yarnMappings: String by project
val loaderVersion: String by project
val apiVersion: String by project

val modVersion: String by project
val mavenGroup: String by project
val archivesBase: String by project

val fabricVersion: String by project
val jadeVersionId: String by project

group = mavenGroup
version = modVersion

base {
    archivesName.set(archivesBase)
}

loom {
    accessWidenerPath = file("src/main/resources/scribble.accesswidener")
}

repositories {
}

dependencies {
    // To change the versions, see the gradle.properties file

    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings("net.fabricmc:yarn:$yarnMappings:v2")
    modImplementation("net.fabricmc:fabric-loader:$loaderVersion")

    modImplementation("net.fabricmc.fabric-api:fabric-api:$apiVersion")
}

tasks {
    processResources {
        inputs.property("version", project.version)
        filesMatching("fabric.mod.json") {
            expand(project.properties)
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