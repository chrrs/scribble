package me.chrr.scribble.gui.edit;

import me.chrr.scribble.book.RichText;
import net.minecraft.ChatFormatting;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * Interface for handling text overflow situations in the book editor.
 * This is called when text operations would cause the page to overflow.
 */
@NullMarked
public interface OverflowHandler {
    /**
     * Called when typing or pasting would overflow the current page.
     *
     * @param currentText the current text on the page
     * @param cursor      the cursor position
     * @param insert      the text to insert
     * @param color       the current color
     * @param modifiers   the current modifiers
     * @return true if the overflow was handled, false if the operation should be rejected
     */
    boolean handleOverflow(RichText currentText, int cursor, RichText insert,
                           @Nullable ChatFormatting color, Set<ChatFormatting> modifiers);

    /**
     * Called when Enter is pressed at the end of a page.
     *
     * @return true if a new page was created
     */
    boolean handleEnterAtEnd();

    /**
     * Called when Backspace is pressed on a completely empty page.
     *
     * @return true if the page was deleted
     */
    boolean handleBackspaceOnEmpty();
}
