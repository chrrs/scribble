package me.chrr.scribble.forge;

import me.chrr.scribble.Scribble;
import me.chrr.scribble.compat.ClothConfigScreenFactory;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod(Scribble.MOD_ID)
public class ScribbleForge {
    public ScribbleForge() {
        Scribble.CONFIG_DIR = FMLPaths.CONFIGDIR.get();
        Scribble.BOOK_DIR = FMLPaths.GAMEDIR.get().resolve("books");

        Scribble.init();

        if (FMLLoader.getDist().isClient() && ModList.get().isLoaded("cloth_config")) {
            //noinspection removal: we're not upgrading Forge past 1.20.1
            ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory((client, parent) -> ClothConfigScreenFactory.create(parent)));
        }
    }
}