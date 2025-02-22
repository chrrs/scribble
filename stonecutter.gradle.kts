plugins {
    id("dev.kikugie.stonecutter")
    id("dev.architectury.loom") version "1.9-SNAPSHOT" apply false
    id("architectury-plugin") version "3.4-SNAPSHOT" apply false
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
    id("me.modmuss50.mod-publish-plugin") version "0.5.1" apply false
}

stonecutter active "1.21.4" /* [SC] DO NOT EDIT */

// Read the versions from CHISELED_VERSIONS, and only build / publish those versions.
// If it's blank, we build / publish all available versions.
val chiseledVersions = providers.environmentVariable("CHISELED_VERSIONS")
    .orNull?.ifBlank { null }?.split(",")

// Build every version into `build/libs`.
stonecutter registerChiseled tasks.register("chiseledBuild", stonecutter.chiseled) {
    versions { _, it -> chiseledVersions?.contains(it.version) ?: true }
    group = "project"
    ofTask("buildAndCollect")
}

// Publish every version after building.
stonecutter registerChiseled tasks.register("chiseledPublish", stonecutter.chiseled) {
    versions { _, it -> chiseledVersions?.contains(it.version) ?: true }
    group = "project"
    ofTask("publishMods")
}

for (node in stonecutter.tree.nodes) {
    if (node.metadata != stonecutter.current || node.branch.id.isEmpty()) continue

    val loader = node.branch.id.replaceFirstChar { it.uppercase() }
    node.tasks.register("runActive$loader") {
        dependsOn("runClient")
        group = "project"
    }
}
