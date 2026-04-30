plugins {
    id("dev.kikugie.stonecutter")
    id("me.chrr.tapestry.gradle") version "1.1.0" apply false
}

stonecutter active "26.1" /* [SC] DO NOT EDIT */

for (node in stonecutter.tree.nodes) {
    if (node.metadata != stonecutter.current || node.branch.id.isEmpty()) continue

    val loader = node.branch.id.replaceFirstChar { it.uppercase() }
    node.project.tasks.register("runActive$loader") {
        description = "Run the client for the active version using $loader."
        group = "project"

        dependsOn("runClient")
    }
}

tasks.register("publishModsSpecificVersions") {
    description = "Publish specific versions only, as specified by the PUBLISH_VERSIONS environment variable."
    group = "project"

    var versions = providers.environmentVariable("PUBLISH_VERSIONS").map { it.split(",") }
    dependsOn(stonecutter.tasks.named("publishMods") { versions.get().contains(metadata.project) })
}