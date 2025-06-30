plugins {
    id("dev.kikugie.stonecutter")
    id("dev.architectury.loom") version "1.10-SNAPSHOT" apply false
    id("architectury-plugin") version "3.4-SNAPSHOT" apply false
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
    id("me.modmuss50.mod-publish-plugin") version "0.5.1" apply false
}

stonecutter active "1.21.7" /* [SC] DO NOT EDIT */

for (node in stonecutter.tree.nodes) {
    if (node.metadata != stonecutter.current || node.branch.id.isEmpty()) continue

    val loader = node.branch.id.replaceFirstChar { it.uppercase() }
    node.project.tasks.register("runActive$loader") {
        dependsOn("runClient")
        group = "project"
    }
}
