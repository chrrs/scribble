package me.chrr.scribble.history;

import me.chrr.scribble.book.RichText;
import me.chrr.scribble.gui.edit.RichMultiLineTextField;
import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public interface HistoryListener {
    void scribble$history$switchPage(int page);

    void scribble$history$setFormat(@Nullable ChatFormatting color, Set<ChatFormatting> modifiers);

    void scribble$history$insertPage(int page, @Nullable RichText content);

    void scribble$history$deletePage(int page);

    RichMultiLineTextField scribble$history$getRichEditBox();
}
