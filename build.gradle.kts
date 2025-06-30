plugins {
    id("dev.architectury.loom")
    id("architectury-plugin")
}

fun Project.hasProp(namespace: String, key: String) = hasProperty("$namespace.$key")
fun Project.prop(namespace: String, key: String) = property("$namespace.$key") as String

val current = stonecutter.current.version
val minecraft = prop("minecraft", "version")

group = prop("mod", "group")
version = "${prop("mod", "version")}+mc$current-common"
base.archivesName.set(prop("mod", "name"))

architectury.injectInjectables = false
architectury.common(stonecutter.tree.branches.mapNotNull {
    if (stonecutter.current.project !in it) null
    else if (!it.project.hasProp("loom", "platform")) null
    else it.project.prop("loom", "platform")
})

repositories {
    maven("https://maven.shedaniel.me/")
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft")
    mappings("net.fabricmc:yarn:${prop("fabric", "yarnVersion")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${prop("fabric", "loaderVersion")}")

    modCompileOnly("me.shedaniel.cloth:cloth-config-fabric:${prop("clothconfig", "version")}")
}

loom {
    accessWidenerPath = rootProject.file("src/main/resources/scribble.accesswidener")
}

java {
    targetCompatibility = JavaVersion.VERSION_21
    sourceCompatibility = JavaVersion.VERSION_21
}

tasks {
    jar {
        from("LICENSE")
    }
}
