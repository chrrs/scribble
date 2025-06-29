package me.chrr.scribble.history.command;

import me.chrr.scribble.book.RichText;
import me.chrr.scribble.gui.edit.RichEditBox;
import me.chrr.scribble.history.HistoryListener;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.Consumer;

public class EditCommand implements Command {
    private final RichText text;
    private final Consumer<RichEditBox> action;

    public int page = -1;

    @Nullable
    public Formatting color = null;
    public Set<Formatting> modifiers = Set.of();

    private final int cursor;
    private final int selectionEnd;
    private final boolean selecting;

    public EditCommand(RichEditBox editBox, Consumer<RichEditBox> action) {
        this.text = editBox.getRichText();
        this.action = action;

        this.cursor = editBox.cursor;
        this.selectionEnd = editBox.selectionEnd;
        this.selecting = editBox.selecting;
    }

    public void executeEdit(RichEditBox editBox) {
        editBox.cursor = cursor;
        editBox.selectionEnd = selectionEnd;
        editBox.selecting = selecting;
        action.accept(editBox);
    }

    @Override
    public void execute(HistoryListener listener) {
        listener.scribble$history$switchPage(page);
        listener.scribble$history$setFormat(this.color, this.modifiers);

        RichEditBox editBox = listener.scribble$history$getRichEditBox();
        this.executeEdit(editBox);
    }

    @Override
    public void rollback(HistoryListener listener) {
        listener.scribble$history$switchPage(page);
        listener.scribble$history$setFormat(this.color, this.modifiers);

        RichEditBox editBox = listener.scribble$history$getRichEditBox();
        editBox.cursor = cursor;
        editBox.selectionEnd = selectionEnd;
        editBox.selecting = selecting;
        editBox.setRichTextWithoutUpdating(text);

        editBox.onChange();
        editBox.sendUpdateFormat();
    }
}
