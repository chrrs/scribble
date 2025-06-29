package me.chrr.scribble.history;

import me.chrr.scribble.gui.edit.RichEditBox;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public interface HistoryListener {
    void scribble$history$switchPage(int page);

    void scribble$history$setFormat(@Nullable Formatting color, Set<Formatting> modifiers);

    RichEditBox scribble$history$getRichEditBox();
}
