plugins {
    id("dev.architectury.loom")
    id("architectury-plugin")
    id("me.modmuss50.mod-publish-plugin")
}

fun Project.hasProp(namespace: String, key: String) = hasProperty("$namespace.$key")
fun Project.prop(namespace: String, key: String) = property("$namespace.$key") as String

val minecraft = stonecutter.current.version

group = prop("mod", "group")
version = "${prop("mod", "version")}+mc$minecraft"
base.archivesName.set("${prop("mod", "name")}-common")

architectury.common(stonecutter.tree.branches.mapNotNull {
    if (stonecutter.current.project !in it) null
    else if (!it.hasProp("loom", "platform")) null
    else it.prop("loom", "platform")
})

stonecutter {
    // `renderButton` was changed to renderWidget after 1.20.3.
    val method = if (eval(minecraft, ">=1.20.3")) "renderWidget" else "renderButton"
    swaps["renderWidget"] = "protected void $method(DrawContext context, int mouseX, int mouseY, float delta) {"
}

repositories {
    maven("https://maven.shedaniel.me/")
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft")
    mappings("net.fabricmc:yarn:${prop("fabric", "yarnVersion")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${prop("fabric", "loaderVersion")}")

    modCompileOnly("me.shedaniel.cloth:cloth-config-fabric:${prop("clothconfig", "version")}")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.3")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.3")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.3")
}

loom {
    accessWidenerPath = rootProject.file("src/main/resources/scribble.accesswidener")
}

java {
    val java = if (stonecutter.eval(minecraft, ">=1.20.5"))
        JavaVersion.VERSION_21 else JavaVersion.VERSION_17
    targetCompatibility = java
    sourceCompatibility = java
}

tasks {
    jar {
        from("LICENSE")
    }

    test {
        useJUnitPlatform()
        testLogging { events("passed", "skipped", "failed") }
    }

    build {
        group = "versioned"
        description = "Must run through `chiseledBuild`"
    }
}


//publishMods {
//    val displayLoader = when (loader) {
//        "forge" -> "Forge"
//        "fabric" -> "Fabric"
//        "neoforge" -> "NeoForge"
//        else -> loader
//    }
//
//    displayName.set("${prop("mod", "version")} - $displayLoader $minecraftVersion")
//
//    file.set(tasks.remapJar.get().archiveFile)
//    changelog.set(providers.environmentVariable("CHANGELOG"))
//    type.set(if (prop("mod", "version").contains("beta")) ReleaseType.BETA else ReleaseType.STABLE)
//    modLoaders.addAll(prop("platform", "loaders").split(","))
//
//    val gameVersions = prop("platform", "versions").split(",")
//
//    modrinth {
//        projectId.set(prop("modrinth", "id"))
//        accessToken.set(providers.environmentVariable("MODRINTH_TOKEN"))
//        minecraftVersions.addAll(gameVersions)
//        optional("cloth-config")
//    }
//
//    curseforge {
//        projectId.set(prop("curseforge", "id"))
//        accessToken.set(providers.environmentVariable("CURSEFORGE_TOKEN"))
//        minecraftVersions.addAll(gameVersions)
//        optional("cloth-config")
//    }
//}