package me.chrr.scribble.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.chrr.scribble.Scribble;
import net.fabricmc.loader.api.FabricLoader;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class ModMenuCompat implements ModMenuApi {
    @Override
    public @Nullable ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (FabricLoader.getInstance().isModLoaded("yet_another_config_lib_v3")) {
            return Scribble::buildConfigScreen;
        }

        return null;
    }
}