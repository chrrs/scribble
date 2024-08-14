package me.chrr.scribble.book.bookeditscreencommand;

import me.chrr.scribble.book.RichText;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;

/**
 * Represents the state of a {@link BookEditScreen}.
 * This immutable record captures the essential data needed to restore the screen's state at a later point.
 */
public record BookEditScreenMemento(
        int selectionStart,
        int selectionEnd,
        RichText currentPageRichText
) {
}
