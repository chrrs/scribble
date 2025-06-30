import dev.kikugie.stonecutter.data.StonecutterProject

plugins {
    id("dev.kikugie.stonecutter")
    id("dev.architectury.loom") version "1.10-SNAPSHOT" apply false
    id("architectury-plugin") version "3.4-SNAPSHOT" apply false
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
    id("me.modmuss50.mod-publish-plugin") version "0.5.1" apply false
}

stonecutter active "1.21.6" /* [SC] DO NOT EDIT */

// Read the versions from CHISELED_VERSIONS, and only build / publish those versions.
// If it's blank, we build / publish all available versions. Same for loaders.
val chiseledVersions = providers.environmentVariable("CHISELED_VERSIONS")
    .orNull?.ifBlank { null }?.split(",")
val chiseledLoaders = providers.environmentVariable("CHISELED_LOADERS")
    .orNull?.ifBlank { null }?.split(",")

val versionSelector = { branch: String, project: StonecutterProject ->
    val selectVersion = chiseledVersions?.contains(project.version) ?: true
    val selectBranch = chiseledLoaders?.contains(branch) ?: true
    selectVersion && selectBranch
}

// Build every version into `build/libs`.
stonecutter registerChiseled tasks.register("chiseledBuild", stonecutter.chiseled) {
    versions(versionSelector)
    group = "project"
    ofTask("buildAndCollect")
}

// Publish every version after building.
stonecutter registerChiseled tasks.register("chiseledPublish", stonecutter.chiseled) {
    versions(versionSelector)
    group = "project"
    ofTask("publishMods")
}

// Test on every version
stonecutter registerChiseled tasks.register("chiseledTest", stonecutter.chiseled) {
    versions { branch, _ -> branch == "" }
    group = "project"
    ofTask("test")
}

for (node in stonecutter.tree.nodes) {
    if (node.metadata != stonecutter.current || node.branch.id.isEmpty()) continue

    val loader = node.branch.id.replaceFirstChar { it.uppercase() }
    node.project.tasks.register("runActive$loader") {
        dependsOn("runClient")
        group = "project"
    }
}
