package me.chrr.scribble.book;

import net.minecraft.SharedConstants;
import net.minecraft.client.util.SelectionManager;
import net.minecraft.util.Util;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class RichSelectionManager extends SelectionManager {
    private final Supplier<RichText> textGetter;
    private final Consumer<RichText> textSetter;
    private final Predicate<RichText> textFilter;

    public RichSelectionManager(Supplier<RichText> textGetter, Consumer<RichText> textSetter, Supplier<String> clipboardGetter, Consumer<String> clipboardSetter, Predicate<RichText> textFilter) {
        super(() -> textGetter.get().getPlainText(), s -> {
        }, clipboardGetter, clipboardSetter, s -> true);
        this.textGetter = textGetter;
        this.textSetter = textSetter;
        this.textFilter = textFilter;
    }

    @Override
    public boolean insert(char c) {
        if (SharedConstants.isValidChar(c)) {
            this.insert(String.valueOf(c));
        }

        return true;
    }

    @Override
    public void insert(String string) {
        RichText text;
        if (this.selectionStart == this.selectionEnd) {
            text = this.textGetter.get().insert(this.selectionStart, string);
        } else {
            int start = Math.min(this.selectionStart, this.selectionEnd);
            int end = Math.max(this.selectionStart, this.selectionEnd);

            text = this.textGetter.get().replace(start, end, string);
            this.selectionStart = this.selectionEnd = start;
        }

        if (this.textFilter.test(text)) {
            this.textSetter.accept(text);
            this.selectionEnd = this.selectionStart = Math.min(text.getLength(), this.selectionStart + string.length());
        }
    }

    @Override
    public void delete(int offset) {
        RichText text = this.textGetter.get();
        if (this.selectionEnd != this.selectionStart) {
            text = text.replace(this.selectionStart, this.selectionEnd, "");
        } else {
            int cursor = Util.moveCursor(text.getPlainText(), this.selectionStart, offset);
            int start = Math.min(cursor, this.selectionStart);
            int end = Math.max(cursor, this.selectionStart);

            text = text.replace(start, end, "");

            if (offset < 0) {
                this.selectionEnd = this.selectionStart = start;
            }
        }

        this.textSetter.accept(text);
    }

    @Override
    public void cut() {
        this.clipboardSetter.accept(this.getSelectedText(textGetter.get().getPlainText()));
        this.delete(0);
    }

    @Override
    public void paste() {
        this.insert(this.clipboardGetter.get());
        this.selectionEnd = this.selectionStart;
    }
}
