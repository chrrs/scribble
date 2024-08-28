package me.chrr.scribble.model.memento;

import me.chrr.scribble.book.RichSelectionManager;
import me.chrr.scribble.book.RichText;
import me.chrr.scribble.tool.commandmanager.Command;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Represents the mutable state of a {@link RichSelectionManager},
 * which can be modified through the execution of RichSelectionManager___Command.
 * See {@link Command}.
 */
public record RichSelectionManagerMemento(
        int selectionStart,
        int selectionEnd,
        RichText richText,
        @Nullable Formatting color,
        Set<Formatting> modifiers
) {
}
