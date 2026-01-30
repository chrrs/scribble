package me.chrr.scribble.gui;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface TextArea<T> extends Renderable, NarratableEntry, GuiEventListener {
    void setText(T text);

    void setVisible(boolean visible);
}
