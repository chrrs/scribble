package me.chrr.scribble.model;

import me.chrr.scribble.book.RichText;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.util.Formatting;

import java.util.Set;

/**
 * Represents the state of a {@link BookEditScreen},
 */
public record BookEditScreenMemento(
        int pageIndex,
        int selectionStart,
        int selectionEnd,
        RichText currentPageRichText,
        Formatting color,
        Set<Formatting> modifiers
) {
}
