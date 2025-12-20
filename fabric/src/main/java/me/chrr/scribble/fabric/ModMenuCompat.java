package me.chrr.scribble.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.chrr.scribble.Scribble;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class ModMenuCompat implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return Scribble::buildConfigScreen;
    }
}