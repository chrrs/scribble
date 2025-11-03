package me.chrr.scribble.history.command;

import me.chrr.scribble.book.RichText;
import me.chrr.scribble.gui.edit.RichMultiLineTextField;
import me.chrr.scribble.history.HistoryListener;
import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.Consumer;

public class EditCommand implements Command {
    private final RichText text;
    private final Consumer<RichMultiLineTextField> action;

    public int page = -1;

    @Nullable
    public ChatFormatting color = null;
    public Set<ChatFormatting> modifiers = Set.of();

    private final int cursor;
    private final int selectCursor;
    private final boolean selecting;

    public EditCommand(RichMultiLineTextField editBox, Consumer<RichMultiLineTextField> action) {
        this.text = editBox.getRichText();
        this.action = action;

        this.cursor = editBox.cursor;
        this.selectCursor = editBox.selectCursor;
        this.selecting = editBox.selecting;
    }

    public void executeEdit(RichMultiLineTextField editBox) {
        editBox.cursor = cursor;
        editBox.selectCursor = selectCursor;
        editBox.selecting = selecting;
        action.accept(editBox);
    }

    @Override
    public void execute(HistoryListener listener) {
        listener.scribble$history$switchPage(page);
        listener.scribble$history$setFormat(this.color, this.modifiers);

        RichMultiLineTextField editBox = listener.scribble$history$getRichEditBox();
        this.executeEdit(editBox);
    }

    @Override
    public void rollback(HistoryListener listener) {
        listener.scribble$history$switchPage(page);
        listener.scribble$history$setFormat(this.color, this.modifiers);

        RichMultiLineTextField editBox = listener.scribble$history$getRichEditBox();
        editBox.cursor = cursor;
        editBox.selectCursor = selectCursor;
        editBox.selecting = selecting;
        editBox.setRichTextWithoutUpdating(text);

        editBox.onValueChange();
        editBox.sendUpdateFormat();
    }
}
