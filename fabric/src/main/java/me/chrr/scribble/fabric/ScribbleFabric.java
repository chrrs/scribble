package me.chrr.scribble.fabric;

import me.chrr.scribble.Scribble;
import me.chrr.tapestry.gradle.annotation.FabricEntrypoint;
import net.fabricmc.api.ClientModInitializer;
import org.jspecify.annotations.NullMarked;

@NullMarked
@FabricEntrypoint("client")
public class ScribbleFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Scribble.init();
    }
}