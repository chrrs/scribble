package me.chrr.scribble;

//? if fabric {

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

@SuppressWarnings("unused")
public class PlatformEntry implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        if (FabricLoader.getInstance().isModLoaded("fixbookgui")) {
            Scribble.LOGGER.info("FixBookGUI is centering the book screen, adapting...");
            Scribble.shouldCenter = true;
        }
    }
}
//?} elif neoforge {
/*import net.neoforged.fml.common.Mod;

@Mod(Scribble.MOD_ID)
public class PlatformEntry {
}
*///?} elif forge {

/*import net.minecraftforge.fml.common.Mod;

@Mod(Scribble.MOD_ID)
public class PlatformEntry {
}
*///?}