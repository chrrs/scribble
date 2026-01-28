plugins {
    id("dev.kikugie.stonecutter")
    id("me.chrr.tapestry.gradle") version "0.0.0" apply false
}

stonecutter active "26.1" /* [SC] DO NOT EDIT */

for (node in stonecutter.tree.nodes) {
    if (node.metadata != stonecutter.current || node.branch.id.isEmpty()) continue

    val loader = node.branch.id.replaceFirstChar { it.uppercase() }
    node.project.tasks.register("runActive$loader") {
        dependsOn("runClient")
        group = "project"
    }
}
