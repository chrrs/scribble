plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.21.4-fabric" /* [SC] DO NOT EDIT */

// Read the versions from CHISELED_VERSIONS, and only build / publish those versions.
// If it's blank, we build / publish all available versions.
val chiseledVersions = providers.environmentVariable("CHISELED_VERSIONS")
    .orNull?.ifBlank { null }?.split(",")
val chiseledProjects = stonecutter.versions
    .filter { chiseledVersions?.contains(it.version) ?: true }

stonecutter registerChiseled tasks.register("chiseledTest", stonecutter.chiseled) {
    versions.set(chiseledProjects)
    group = "project"
    ofTask("test")
}

stonecutter registerChiseled tasks.register("chiseledBuild", stonecutter.chiseled) {
    versions.set(chiseledProjects)
    group = "project"
    ofTask("build")
}

stonecutter registerChiseled tasks.register("chiseledPublish", stonecutter.chiseled) {
    versions.set(chiseledProjects)
    group = "project"
    ofTask("publishMods")
}

stonecutter configureEach {
    // Define loader constants
    val loader = current.project.split("-").last()
    consts(listOf("fabric", "forge", "neoforge").map { it to (loader == it) })

    // renderButton was changed to renderWidget after 1.20.3
    swap("renderWidget") {
        val method =
            if (stonecutter.eval(current.version, ">=1.20.3")) "renderWidget"
            else "renderButton"
        "protected void $method(DrawContext context, int mouseX, int mouseY, float delta) {"
    }
}
