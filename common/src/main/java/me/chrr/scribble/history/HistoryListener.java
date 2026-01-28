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

    void deletePage(int page);
}
