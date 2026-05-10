repositories {
    maven("https://maven.chrr.me/releases")
}

dependencies {
    val root = stonecutter.node.sibling("")!!.project
    fun tapestryModule(name: String) =
        implementation("me.chrr.tapestry:$name:${root.property("tapestry.version")}")

    jij(tapestryModule("tapestry-base")!!)
    jij(tapestryModule("tapestry-config")!!)
}