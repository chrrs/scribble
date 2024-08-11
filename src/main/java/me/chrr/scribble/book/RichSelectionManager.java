package me.chrr.scribble.book;

import me.chrr.scribble.Scribble;
import net.minecraft.client.util.SelectionManager;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class RichSelectionManager extends SelectionManager {
    private final Supplier<RichText> textGetter;
    private final Consumer<RichText> textSetter;
    private final Predicate<RichText> textFilter;
    private final StateCallback stateCallback;

    @Nullable
    private Formatting color = Formatting.BLACK;
    private Set<Formatting> modifiers = new HashSet<>();

    public RichSelectionManager(Supplier<RichText> textGetter, Consumer<RichText> textSetter, Consumer<String> stringSetter, StateCallback stateCallback, Supplier<String> clipboardGetter, Consumer<String> clipboardSetter, Predicate<RichText> textFilter) {
        super(
                () -> textGetter.get().getPlainText(),
                (text) -> Scribble.LOGGER.warn("stringSetter called with \"{}\"", text),
                clipboardGetter, clipboardSetter,
                s -> true
        );

        this.textGetter = textGetter;
        this.textFilter = textFilter;
        this.stateCallback = stateCallback;

        this.textSetter = (text) -> {
            textSetter.accept(text);
            stringSetter.accept(text.getAsFormattedString());
        };
    }

    @Override
    public boolean insert(char c) {
        //? if >=1.20.5 {
        Predicate<Character> isValidChar = net.minecraft.util.StringHelper::isValidChar;
        //?} else
        /*Predicate<Character> isValidChar = net.minecraft.SharedConstants::isValidChar;*/

        if (isValidChar.test(c)) {
            this.insert(String.valueOf(c));
        }

        return true;
    }

    @Override
    public void insert(String string) {
        RichText text = this.textGetter.get();
        int start = Math.min(this.selectionStart, this.selectionEnd);
        int end = Math.max(this.selectionStart, this.selectionEnd);

        if (start == end) {
            text = text.insert(start, string);
        } else {
            text = text.replace(start, end, string);
        }

        if (this.textFilter.test(text)) {
            this.textSetter.accept(text);

            // Using RichText here to remove all formatting from the string
            // and get correct position of a cursor
            String plaintStringToInsert = RichText.fromFormattedString(string).getPlainText();
            int newCursorPosition = Math.min(text.getPlainText().length(), start + plaintStringToInsert.length());
            this.selectionEnd = this.selectionStart = newCursorPosition;

            updateSelectionFormatting();
        }
    }

    @Override
    public void delete(int offset) {
        RichText text = this.textGetter.get();
        if (this.selectionEnd != this.selectionStart) {
            int start = Math.min(this.selectionStart, this.selectionEnd);
            int end = Math.max(this.selectionStart, this.selectionEnd);

            text = text.replace(start, end, "");
            this.selectionStart = this.selectionEnd = start;
        } else {
            int cursor = Util.moveCursor(text.getPlainText(), this.selectionStart, offset);
            int start = Math.min(cursor, this.selectionStart);
            int end = Math.max(cursor, this.selectionStart);

            text = text.replace(start, end, "");
            this.selectionEnd = this.selectionStart = start;
        }

        this.textSetter.accept(text);
        updateSelectionFormatting();
    }

    @Override
    public void copy() {
        this.clipboardSetter.accept(this.getSelectedFormattedText());
    }

    @Override
    public void cut() {
        this.clipboardSetter.accept(this.getSelectedFormattedText());
        this.delete(0);
    }

    private String getSelectedFormattedText() {
        String fullText = textGetter.get().getAsFormattedString();

        int i = Math.min(this.selectionStart, this.selectionEnd);
        int j = Math.max(this.selectionStart, this.selectionEnd);
        RichText richText = RichText.fromFormattedString(fullText);
        return richText.subText(i, j).getAsFormattedString();
    }

    @Override
    public void paste() {
        this.insert(this.clipboardGetter.get());
    }

    public void setColor(Formatting color) {
        this.applyFormatting(color, Set.of(), Set.of());
    }

    public void toggleModifier(Formatting modifier, boolean toggled) {
        if (toggled) {
            this.applyFormatting(null, Set.of(modifier), Set.of());
        } else {
            this.applyFormatting(null, Set.of(), Set.of(modifier));
        }
    }

    private void applyFormatting(
            @Nullable Formatting newColor,
            Set<Formatting> addModifiers,
            Set<Formatting> removeModifiers
    ) {
        if (isSelecting()) {
            int start = Math.min(this.selectionStart, this.selectionEnd);
            int end = Math.max(this.selectionStart, this.selectionEnd);

            RichText text = this.textGetter.get()
                    .applyFormatting(start, end, newColor, addModifiers, removeModifiers);
            this.textSetter.accept(text);
        } else {
            if (newColor != null) {
                this.color = newColor;
            }

            this.modifiers.addAll(addModifiers);
            this.modifiers.removeAll(removeModifiers);
        }
    }

    public void updateSelectionFormatting() {
        if (this.textGetter == null) {
            // We're too early, abort.
            return;
        }

        int start = Math.min(this.selectionStart, this.selectionEnd);
        int end = Math.max(this.selectionStart, this.selectionEnd);
        Pair<Formatting, Set<Formatting>> format = this.textGetter.get().getCommonFormat(start, end);

        this.color = format.getLeft();
        this.modifiers = new HashSet<>(format.getRight());

        this.stateCallback.update(this.color, this.modifiers);
    }

    @Override
    public void setSelection(int start, int end) {
        super.setSelection(start, end);
        updateSelectionFormatting();
    }

    @Override
    public void selectAll() {
        super.selectAll();
        updateSelectionFormatting();
    }

    @Override
    protected void updateSelectionRange(boolean shiftDown) {
        super.updateSelectionRange(shiftDown);
        updateSelectionFormatting();
    }

    @Nullable
    public Formatting getColor() {
        return color;
    }

    public interface StateCallback {
        void update(@Nullable Formatting color, Set<Formatting> modifiers);
    }
}
