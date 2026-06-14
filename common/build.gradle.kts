repositories {
    maven("https://maven.chrr.me/releases")
    mavenLocal()
}

dependencies {
    val root = stonecutter.node.sibling("")!!.project
    fun tapestryModule(name: String) =
        implementation("me.chrr.tapestry:$name:${root.property("tapestry.version")}")

    jij(tapestryModule("tapestry-base")!!)
    jij(tapestryModule("tapestry-config")!!)
}

stonecutter {
    // `setScreen` moved into the `gui` field on Minecraft.
    replacements.string(current.parsed >= "26.2", "!screen") {
        replace("minecraft.screen", "minecraft.gui.screen()")
        replace("minecraft.setScreen(", "minecraft.gui.setScreen(")
    }

    replacements.string(current.parsed >= "26.2", "mixin_screen") {
        replace("Lnet/minecraft/client/Minecraft;setScreen(", "Lnet/minecraft/client/gui/Gui;setScreen(")
        replace("import net.minecraft.client.Minecraft;", "import net.minecraft.client.gui.Gui;")
        replace("Minecraft instance,", "Gui instance,")
    }

    // TextColor constants don't exist before 26.2.
    replacements.regex(current.parsed >= "26.2", "!named_colors") {
        replace(
            "TextColor\\.fromLegacyFormat\\(net\\.minecraft\\.ChatFormatting\\.([A-Z_]+)\\)", "TextColor.$1",
            "TextColor\\.([A-Z_]+)", "TextColor.fromLegacyFormat(net.minecraft.ChatFormatting.$1)",
        )
    }
}