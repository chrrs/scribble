package me.chrr.scribble.config;

import me.chrr.scribble.Scribble;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;

public class ClothConfigScreenFactory {
    private ClothConfigScreenFactory() {
    }

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.scribble.title"));

        builder.setSavingRunnable(() -> {
            try {
                Scribble.CONFIG_MANAGER.save();
            } catch (IOException e) {
                Scribble.LOGGER.error("could not save config", e);
            }
        });

        Config config = Scribble.CONFIG_MANAGER.getConfig();

        ConfigCategory category = builder.getOrCreateCategory(Component.empty());
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        category.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("config.scribble.option.copy_formatting_codes"),
                        config.copyFormattingCodes
                )
                .setDefaultValue(Config.DEFAULT.copyFormattingCodes)
                .setTooltip(Component.translatable("config.scribble.description.copy_formatting_codes"))
                .setSaveConsumer((value) -> config.copyFormattingCodes = value)
                .build());

        category.addEntry(entryBuilder.startBooleanToggle(
                        Component.translatable("config.scribble.option.center_book_gui"),
                        config.centerBookGui
                )
                .setDefaultValue(Config.DEFAULT.centerBookGui)
                .setSaveConsumer((value) -> config.centerBookGui = value)
                .build());

        category.addEntry(entryBuilder.startEnumSelector(
                        Component.translatable("config.scribble.option.show_action_buttons"),
                        Config.ShowActionButtons.class, config.showActionButtons
                )
                .setEnumNameProvider((opt) ->
                        Component.translatable("config.scribble.option.show_action_buttons." + opt.name().toLowerCase()))
                .setDefaultValue(Config.DEFAULT.showActionButtons)
                .setSaveConsumer((value) -> config.showActionButtons = value)
                .build());

        category.addEntry(entryBuilder.startIntSlider(
                        Component.translatable("config.scribble.option.edit_history_size"),
                        config.editHistorySize, 8, 128
                )
                .setDefaultValue(Config.DEFAULT.editHistorySize)
                .setSaveConsumer((value) -> config.editHistorySize = value)
                .build());

        return builder.build();
    }
}
