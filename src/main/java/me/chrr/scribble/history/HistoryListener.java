package me.chrr.scribble.history;

import me.chrr.scribble.book.RichText;
import me.chrr.scribble.gui.edit.RichEditBox;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public interface HistoryListener {
    void scribble$history$switchPage(int page);

    void scribble$history$setFormat(@Nullable Formatting color, Set<Formatting> modifiers);

    void scribble$history$insertPage(int page, @Nullable RichText content);

    void scribble$history$deletePage(int page);

    RichEditBox scribble$history$getRichEditBox();
}
