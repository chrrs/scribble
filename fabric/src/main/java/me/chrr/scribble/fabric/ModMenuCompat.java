package me.chrr.scribble.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.chrr.scribble.Scribble;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class ModMenuCompat implements ModMenuApi {
    @Override
    public @Nullable ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (Scribble.platform().HAS_YACL) {
            return Scribble::buildConfigScreen;
        }

        return null;
    }
}