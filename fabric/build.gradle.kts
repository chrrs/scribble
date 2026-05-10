repositories {
    maven("https://maven.terraformersmc.com/releases/")
}

dependencies {
    val root = stonecutter.node.sibling("")!!.project
    fun fabricApiModule(name: String) =
        implementation(fabricApi.module(name, root.property("fabric.api.version") as String))

    jij(fabricApiModule("fabric-api-base")!!)
    jij(fabricApiModule("fabric-resource-loader-v1")!!)

    compileOnly("com.terraformersmc:modmenu:18.0.0-alpha.5")
}
