package me.chrr.scribble.fabric;

import me.chrr.scribble.Scribble;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class ScribbleFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Scribble.CONFIG_DIR = FabricLoader.getInstance().getConfigDir();
        Scribble.BOOK_DIR = FabricLoader.getInstance().getGameDir().resolve("books");

        Scribble.init();
    }
}