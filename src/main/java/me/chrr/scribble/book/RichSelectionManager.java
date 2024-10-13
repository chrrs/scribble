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

    private final Supplier<Formatting> colorGetter;
    private final Supplier<Set<Formatting>> modifiersGetter;

    public RichSelectionManager(
            Supplier<RichText> textGetter,
            Consumer<RichText> textSetter,
            StateCallback stateCallback,
            Supplier<String> clipboardGetter,
            Consumer<String> clipboardSetter,
            Predicate<RichText> textFilter,

            Supplier<Formatting> colorGetter,
            Supplier<Set<Formatting>> modifiersGetter
    ) {
        super(
                () -> textGetter.get().getPlainText(),
                (text) -> Scribble.LOGGER.warn("stringSetter called with \"{}\"", text),
                clipboardGetter, clipboardSetter,
                s -> true
        );

        this.textGetter = textGetter;
        this.textSetter = textSetter;
        this.textFilter = textFilter;
        this.stateCallback = stateCallback;

        this.colorGetter = colorGetter;
        this.modifiersGetter = modifiersGetter;
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

        RichText insertion;

        // If the string contains formatting codes, we keep them in. Otherwise,
        // we want to keep the formatting that is already selected.
        // We consider the RESET formatting code to be void, as it messes with books.
        boolean isFormattedString = !Formatting.strip(string)
                .equals(string.replaceAll(Formatting.RESET.toString(), ""));

        if (isFormattedString) {
            insertion = RichText.fromFormattedString(string);
        } else {
            // We strip any leftover RESET tags from the string.
            string = string.replaceAll(Formatting.RESET.toString(), "");
            insertion = new RichText(string, colorGetter.get(), modifiersGetter.get());
        }

        // If no text is selected, we can just insert instead of replace.
        if (start == end) {
            text = text.insert(start, insertion);
        } else {
            text = text.replace(start, end, insertion);
        }

        if (this.textFilter.test(text)) {
            this.textSetter.accept(text);

            // Using RichText here to remove all formatting from the string
            // and get correct position of a cursor
            String plaintStringToInsert = RichText.fromFormattedString(string).getPlainText();
            int newCursorPosition = Math.min(text.getPlainText().length(), start + plaintStringToInsert.length());
            this.selectionEnd = this.selectionStart = newCursorPosition;

            notifyCursorFormattingChanged();
        }
    }

    @Override
    public void delete(int offset) {
        RichText text = this.textGetter.get();
        if (this.selectionEnd != this.selectionStart) {
            int start = Math.min(this.selectionStart, this.selectionEnd);
            int end = Math.max(this.selectionStart, this.selectionEnd);

            text = text.replace(start, end, RichText.empty());
            this.selectionStart = this.selectionEnd = start;
        } else {
            int cursor = Util.moveCursor(text.getPlainText(), this.selectionStart, offset);
            int start = Math.min(cursor, this.selectionStart);
            int end = Math.max(cursor, this.selectionStart);

            text = text.replace(start, end, RichText.empty());
            this.selectionEnd = this.selectionStart = start;
        }

        this.textSetter.accept(text);
        notifyCursorFormattingChanged();
    }

    @Override
    public void copy() {
        // NOTE: this should only be called by other mods, as we're overriding all
        //       the keyboard shortcuts in BookEditScreenMixin.
        this.clipboardSetter.accept(this.getSelectedFormattedText());
    }

    @Override
    public void cut() {
        // NOTE: this should only be called by other mods, as we're overriding all
        //       the keyboard shortcuts in BookEditScreenMixin.
        this.clipboardSetter.accept(this.getSelectedFormattedText());
        this.delete(0);
    }

    public String getSelectedFormattedText() {
        int i = Math.min(this.selectionStart, this.selectionEnd);
        int j = Math.max(this.selectionStart, this.selectionEnd);
        return textGetter.get().subText(i, j).getAsFormattedString();
    }

    @Override
    public void paste() {
        this.insert(this.clipboardGetter.get());
    }

    public void applyColorForSelection(Formatting color) {
        this.applyFormatting(color, Set.of(), Set.of());
    }

    public void toggleModifierForSelection(Formatting modifier, boolean toggled) {
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
        }
    }

    public void notifyCursorFormattingChanged() {
        if (stateCallback == null) {
            // Can happen when the method is called from the supper constructor
            return;
        }

        Pair<@Nullable Formatting, Set<Formatting>> format = getCursorFormatting();

        Formatting color = format.getLeft();
        Set<Formatting> modifiers = new HashSet<>(format.getRight());
        stateCallback.onCursorFormattingChanged(color, modifiers);
    }

    public Pair<@Nullable Formatting, Set<Formatting>> getCursorFormatting() {
        if (textGetter == null) {
            // Can happen when the method is called from the supper constructor
            return new Pair<>(null, Set.of());
        }

        int start = Math.min(this.selectionStart, this.selectionEnd);
        int end = Math.max(this.selectionStart, this.selectionEnd);
        return this.textGetter.get().getCommonFormat(start, end);
    }

    @Override
    public void setSelection(int start, int end) {
        super.setSelection(start, end);
        notifyCursorFormattingChanged();
    }

    @Override
    public void selectAll() {
        super.selectAll();
        notifyCursorFormattingChanged();
    }

    @Override
    protected void updateSelectionRange(boolean shiftDown) {
        super.updateSelectionRange(shiftDown);
        notifyCursorFormattingChanged();
    }

    public interface StateCallback {
        /**
         * Called when the cursor position or selection range changes,
         * which may result in a different text color or modifiers being applied at the new cursor position.
         *
         * @param color     The formatting color at the new cursor position,
         *                  or null if multiple are colors applied to the selected range.
         * @param modifiers The set of formatting modifiers applied at the new cursor position/to selected range.
         */
        void onCursorFormattingChanged(@Nullable Formatting color, Set<Formatting> modifiers);
    }
}
