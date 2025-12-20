package me.chrr.scribble.fabric;

import me.chrr.scribble.Scribble;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.jspecify.annotations.NullMarked;

import java.nio.file.Path;

@NullMarked
public class ScribbleFabric extends Scribble.Platform implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Scribble.init(this);
    }

    @Override
    protected boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    protected String getModVersion() {
        return FabricLoader.getInstance().getModContainer(Scribble.MOD_ID)
                .orElseThrow().getMetadata().getVersion().getFriendlyString();
    }

    @Override
    protected Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    protected Path getGameDir() {
        return FabricLoader.getInstance().getGameDir();
    }
}