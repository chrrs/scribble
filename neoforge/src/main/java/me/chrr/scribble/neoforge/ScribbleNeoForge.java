package me.chrr.scribble.neoforge;

import me.chrr.scribble.Scribble;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.jspecify.annotations.NullMarked;

@NullMarked
@Mod(value = Scribble.MOD_ID, dist = Dist.CLIENT)
public class ScribbleNeoForge {
    public ScribbleNeoForge(ModContainer mod) {
        Scribble.init();

        mod.registerExtensionPoint(IConfigScreenFactory.class,
                (container, parent) -> Scribble.buildConfigScreen(parent));
    }
}