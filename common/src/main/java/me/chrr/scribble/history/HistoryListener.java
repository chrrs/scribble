package me.chrr.scribble.history;

import me.chrr.scribble.gui.edit.RichMultiLineTextField;
import me.chrr.scribble.text.StyledText;
import net.minecraft.network.chat.Style;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface HistoryListener {
    RichMultiLineTextField switchAndFocusPage(int page);

    void setStyle(Style style);

    void insertPageAt(int page, @Nullable StyledText content);

    void deletePage(int page);
}
