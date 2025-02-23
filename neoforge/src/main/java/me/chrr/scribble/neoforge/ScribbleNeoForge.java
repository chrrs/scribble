package me.chrr.scribble.neoforge;

import me.chrr.scribble.Scribble;
import me.chrr.scribble.compat.ClothConfigScreenFactory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = Scribble.MOD_ID, dist = Dist.CLIENT)
public class ScribbleNeoForge {
    public ScribbleNeoForge(ModContainer mod) {
        Scribble.CONFIG_DIR = FMLPaths.CONFIGDIR.get();
        Scribble.BOOK_DIR = FMLPaths.GAMEDIR.get().resolve("books");

        Scribble.init();

        if (ModList.get().isLoaded("cloth_config")) {
            mod.registerExtensionPoint(IConfigScreenFactory.class,
                    (container, parent) -> ClothConfigScreenFactory.create(parent));
        }
    }
}