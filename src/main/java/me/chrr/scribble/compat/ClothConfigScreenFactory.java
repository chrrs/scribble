package me.chrr.scribble.compat;

import me.chrr.scribble.Scribble;
import me.chrr.scribble.config.Config;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
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

        Config config = Scribble.CONFIG_MANAGER.getConfig();

        ConfigCategory category = builder.getOrCreateCategory(Text.empty());
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        category.addEntry(entryBuilder.startBooleanToggle(
                        Text.translatable("config.scribble.option.copy_formatting_codes"),
                        config.copyFormattingCodes
                )
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config.scribble.description.copy_formatting_codes"))
                .setSaveConsumer((value) -> config.copyFormattingCodes = value)
                .build());

        return builder.build();
    }
}
