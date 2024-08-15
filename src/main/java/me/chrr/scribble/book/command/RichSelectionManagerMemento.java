package me.chrr.scribble.book.command;

import me.chrr.scribble.book.RichText;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public record RichSelectionManagerMemento(
        int selectionStart,
        int selectionEnd,
        RichText richText,
        @Nullable Formatting color,
        Set<Formatting> modifiers
) {
}
