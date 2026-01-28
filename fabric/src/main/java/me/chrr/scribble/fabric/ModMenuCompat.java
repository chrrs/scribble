package me.chrr.scribble.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.chrr.scribble.Scribble;
import me.chrr.tapestry.gradle.annotation.FabricEntrypoint;
import org.jspecify.annotations.NullMarked;

@NullMarked
@FabricEntrypoint("modmenu")
public class ModMenuCompat implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return Scribble::buildConfigScreen;
    }
}