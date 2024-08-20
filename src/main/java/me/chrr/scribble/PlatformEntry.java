package me.chrr.scribble;

//? if fabric {

import net.fabricmc.api.ClientModInitializer;

@SuppressWarnings("unused")
public class PlatformEntry implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Scribble.init();
    }
}
//?} elif neoforge {

/*import me.chrr.scribble.compat.ClothConfigScreenFactory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = Scribble.MOD_ID, dist = Dist.CLIENT)
public class PlatformEntry {
    public PlatformEntry(ModContainer mod) {
        Scribble.init();

        if (ModList.get().isLoaded("cloth_config")) {
            mod.registerExtensionPoint(IConfigScreenFactory.class,
                    (container, parent) -> ClothConfigScreenFactory.create(parent));
        }
    }
}
*///?} elif forge {

/*import me.chrr.scribble.compat.ClothConfigScreenFactory;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;

@Mod(Scribble.MOD_ID)
public class PlatformEntry {
    public PlatformEntry() {
        Scribble.init();

        if (FMLLoader.getDist().isClient() && ModList.get().isLoaded("cloth_config")) {
            ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory((client, parent) -> ClothConfigScreenFactory.create(parent)));
        }
    }
}
*///?}