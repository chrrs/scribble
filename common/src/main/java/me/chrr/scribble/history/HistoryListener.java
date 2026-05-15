package me.chrr.scribble.history;

import me.chrr.scribble.book.RichText;
import me.chrr.scribble.gui.edit.RichMultiLineTextField;
import net.minecraft.ChatFormatting;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Set;

@NullMarked
public interface HistoryListener {
    RichMultiLineTextField switchAndFocusPage(int page);

    void setFormat(@Nullable ChatFormatting color, Set<ChatFormatting> modifiers);

    void insertPageAt(int page, @Nullable RichText content);

    /** @param navigateDirection negative = go left, 0 or positive = stay/go right */
    void deletePage(int page, int navigateDirection);

    int getTotalPages();

    RichText getPageContent(int page);

    void setPageContent(int page, RichText content);

    void refreshPages();
}
