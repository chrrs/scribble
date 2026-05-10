package me.chrr.scribble.history.command;

import me.chrr.scribble.book.RichText;
import me.chrr.scribble.gui.edit.RichEditBox;
import me.chrr.scribble.gui.edit.RichMultiLineTextField;
import me.chrr.scribble.history.HistoryListener;
import net.minecraft.ChatFormatting;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.function.Consumer;

@NullMarked
public class EditCommand implements Command {
    private final RichText text;
    private final Consumer<RichMultiLineTextField> action;

    public int page = -1;

    public final @Nullable ChatFormatting color;
    public final Set<ChatFormatting> modifiers;

    private final int cursor;
    private final int selectCursor;
    private final boolean selecting;

    public EditCommand(RichEditBox editBox, Consumer<RichMultiLineTextField> action) {
        RichMultiLineTextField textField = editBox.getRichTextField();

        this.text = textField.getRichText();
        this.action = action;

        this.color = editBox.color;
        this.modifiers = editBox.modifiers;

        this.cursor = textField.cursor;
        this.selectCursor = textField.selectCursor;
        this.selecting = textField.selecting;
    }

    public void executeEdit(RichMultiLineTextField textField) {
        textField.cursor = this.cursor;
        textField.selectCursor = this.selectCursor;
        textField.selecting = this.selecting;
        action.accept(textField);
    }

    @Override
    public void execute(HistoryListener listener) {
        RichMultiLineTextField textField = listener.switchAndFocusPage(this.page);
        listener.setFormat(this.color, this.modifiers);
        this.executeEdit(textField);
    }

    @Override
    public void rollback(HistoryListener listener) {
        RichMultiLineTextField textField = listener.switchAndFocusPage(this.page);
        listener.setFormat(this.color, this.modifiers);

        textField.cursor = this.cursor;
        textField.selectCursor = this.selectCursor;
        textField.selecting = this.selecting;
        textField.setValueWithoutUpdating(this.text);

        textField.onValueChange();
        textField.sendUpdateFormat();
    }
}
