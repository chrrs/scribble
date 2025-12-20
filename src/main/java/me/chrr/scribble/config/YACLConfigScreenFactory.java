package me.chrr.scribble.config;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import me.chrr.scribble.Scribble;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;

@NullMarked
public class YACLConfigScreenFactory {
    public static Screen create(ConfigManager configManager, Screen parent) {
        Config config = configManager.getConfig();

        // FIXME: if we want to keep YACL, move text to translatable strings.
        return YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("config.scribble.title"))
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("config.scribble.title"))
                        .group(OptionGroup.createBuilder()
                                .name(Component.literal("Appearance"))
                                .description(OptionDescription.of(Component.literal("These options affect how the user interface looks.")))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.literal("Double page viewing"))
                                        .description(OptionDescription.of(Component.literal("Whether to show two pages at the same time when reading and editing books.")))
                                        .binding(Config.DEFAULT.pagesToShow > 1, () -> config.pagesToShow > 1,
                                                (value) -> config.pagesToShow = value ? 2 : 1)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.literal("Vertically center book GUI's"))
                                        .description(OptionDescription.of(Component.literal("If enabled, book viewing and editing GUI's will be approximately vertically centered, instead of being fixed to the top of the screen.")))
                                        .binding(Config.DEFAULT.centerBookGui, () -> config.centerBookGui,
                                                (value) -> config.centerBookGui = value)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<Config.ShowActionButtons>createBuilder()
                                        .name(Component.literal("Show action buttons"))
                                        .description(OptionDescription.of(Component.literal("Action buttons are the white buttons on the left of the page. This option determines when these action buttons should be shown or hidden.\n\nThe 'Show when editing' option shows the action buttons when editing a book using a book & quill, but hides them when reading a written book.")))
                                        .binding(Config.DEFAULT.showActionButtons, () -> config.showActionButtons,
                                                (value) -> config.showActionButtons = value)
                                        .controller((opt) -> EnumControllerBuilder.create(opt)
                                                .enumClass(Config.ShowActionButtons.class)
                                                .formatValue((value) -> switch (value) {
                                                    case ALWAYS -> Component.literal("Always");
                                                    case WHEN_EDITING -> Component.literal("Show when editing");
                                                    case NEVER -> Component.literal("Never");
                                                }))
                                        .build())
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Component.literal("Behaviour"))
                                .description(OptionDescription.of(Component.literal("These options affect how you can write books.")))
                                .option(Option.<Boolean>createBuilder()
                                        .name(Component.literal("Copy formatting codes"))
                                        .description(OptionDescription.of(Component.literal("When copying formatted text, this option determines whether formatting codes (&) should be copied to your clipboard. This allows you to paste text back into the book using the copied formatting.\n\nNote: This option can be temporarily disabled by holding SHIFT while copying or pasting text.")))
                                        .binding(Config.DEFAULT.copyFormattingCodes, () -> config.copyFormattingCodes,
                                                (value) -> config.copyFormattingCodes = value)
                                        .controller(TickBoxControllerBuilder::create)
                                        .build())
                                .option(Option.<Integer>createBuilder()
                                        .name(Component.literal("Edit history limit"))
                                        .description(OptionDescription.of(Component.literal("How many actions Scribble should remember for you to be able to undo them. If the limit is exceeded, the oldest actions will be removed from the undo stack.\n\nNote: Higher values could lead to more RAM usage while editing.")))
                                        .binding(Config.DEFAULT.editHistorySize, () -> config.editHistorySize,
                                                (value) -> config.editHistorySize = value)
                                        .controller((opt) -> IntegerSliderControllerBuilder.create(opt)
                                                .range(8, 128).step(1))
                                        .build())
                                .build())
                        .build())
                .save(() -> {
                    try {
                        configManager.save();
                    } catch (IOException e) {
                        Scribble.LOGGER.error("could not save config", e);
                    }
                })
                .build()
                .generateScreen(parent);
    }
}
