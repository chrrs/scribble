package me.chrr.scribble.compat;

import me.chrr.scribble.Scribble;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.io.IOException;

public class ClothConfigScreenFactory {
    private ClothConfigScreenFactory() {
    }

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("config.scribble.title"));

        builder.setSavingRunnable(() -> {
            try {
                Scribble.CONFIG_MANAGER.save();
            } catch (IOException e) {
                Scribble.LOGGER.error("could not save config", e);
            }
        });

        builder.getOrCreateCategory(Text.empty());

        return builder.build();
    }
}
