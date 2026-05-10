package me.chrr.scribble.neoforge;

import me.chrr.scribble.Scribble;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.jspecify.annotations.NullMarked;

import java.nio.file.Path;

@NullMarked
@Mod(value = Scribble.MOD_ID, dist = Dist.CLIENT)
public class ScribbleNeoForge extends Scribble.Platform {
    public ScribbleNeoForge(ModContainer mod) {
        Scribble.init(this);

        mod.registerExtensionPoint(IConfigScreenFactory.class,
                (container, parent) -> Scribble.buildConfigScreen(parent));
    }

    @Override
    protected String getModVersion() {
        return ModList.get().getModFileById(Scribble.MOD_ID).versionString();
    }

    @Override
    protected Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    protected Path getGameDir() {
        return FMLPaths.GAMEDIR.get();
    }
}