plugins {
    id("me.chrr.tapestry.gradle")
}

val root = stonecutter.current.project
val current = stonecutter.current.version

fun sibling(name: String) =
    stonecutter.node.sibling(name)!!.project

tapestry {
    projects {
        common = listOf(sibling("common"))
        fabric = listOf(sibling("fabric"))
        neoforge = listOf(sibling("neoforge"))
    }

    versions {
        minecraft = prop("minecraft.version")
        fabricLoader = prop("fabric.loader.version")
        neoforge = prop("neoforge.version")
    }

    info {
        id = "scribble"
        version = "2.0.0-beta2"

        name = "Scribble"
        description = "Expertly edit your books with rich formatting options, page utilities and more!"
        authors = listOf("chrrrs")
        license = "MIT"

        url = "https://github.com/chrrs/scribble"
        sources = "https://github.com/chrrs/scribble"
        issues = "https://github.com/chrrs/scribble/issues"

        icon = "assets/scribble/icon.png"
        banner = "assets/scribble/banner.png"
    }

    transform {
        classTweaker = "scribble.accesswidener"
        mixinConfigs.add("scribble.mixins.json")
    }

    depends {
        minecraft = prop("minecraft.compatible").map { it.split(",") }
    }

    game {
        runDir = rootProject.file("run")
        username = "chrrz"
    }

    publish {
        readChangelogFrom(rootProject.file("CHANGELOG.md"))
        modrinth = "yXAvIk0x"
        curseforge = "1051344"
    }
}