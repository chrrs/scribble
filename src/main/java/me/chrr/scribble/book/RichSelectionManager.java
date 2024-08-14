package me.chrr.scribble.book;

import me.chrr.scribble.Scribble;
import net.minecraft.client.util.SelectionManager;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class RichSelectionManager extends SelectionManager {

    public static final Formatting DEFAULT_COLOR = Formatting.BLACK;

    private final Supplier<RichText> textGetter;
    private final Consumer<RichText> textSetter;
    private final Predicate<RichText> textFilter;
    private final StateCallback stateCallback;

    @Nullable
    private Formatting color = DEFAULT_COLOR;
    private Set<Formatting> modifiers = new HashSet<>();

    public RichSelectionManager(
            Supplier<RichText> textGetter,
            Consumer<RichText> textSetter,
            Consumer<String> stringSetter,
            StateCallback stateCallback,
            Supplier<String> clipboardGetter,
            Consumer<String> clipboardSetter,
            Predicate<RichText> textFilter
    ) {
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

    private Formatting getSelectedColor() {
        return Optional.ofNullable(this.color).orElse(DEFAULT_COLOR);
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

        List<RichText.Segment> stringSegments = createSegmentsWithSelectedFormatting(string);
        if (start == end) {
            text = text.insert(start, stringSegments);
        } else {
            text = text.replace(start, end, stringSegments);
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

    /**
     * Creates the segments for the given string, applying formatting based on its content and current selected.
     * <p>
     * If the string contains formatting (e.g., color or modifiers), that formatting is preserved.
     * If it does not contain formatting, the current selected formatting {@link #color} and {@link #modifiers} are applied.
     * Strings with only the RESET formatting tag are treated as non-formatted.
     *
     * @param string the string with a text, which may or may not contain formatting
     * @return a list of {@link RichText.Segment} objects representing the formatted segments of the string
     */
    private List<RichText.Segment> createSegmentsWithSelectedFormatting(String string) {
        // Exclude RESET formatting tag from isFormatted check
        // since it doesn't make sense to have it in the string if no any other formatting is present
        boolean isFormattedString = !Formatting.strip(string)
                .equals(string.replaceAll(Formatting.RESET.toString(), ""));

        if (isFormattedString) {
            // The only reason why isFormattedString check is here,
            // is because even if the string has not formatting at all
            // the RichText.fromFormattedString will create a RichText with Back color,
            // instead of keeping the origin color - no color.
            // ToDo make RichText.fromFormattedString return no color for non formatted strings
            RichText richString = RichText.fromFormattedString(string);
            return richString.getSegments();

        } else {
            // The string do not contain any formatting tags (or contains RESET tag only)

            // Remove leftover RESET tags
            string = Formatting.strip(string);

            // Apply current selected formatting
            RichText.Segment segmentWithFormatting = new RichText.Segment(string, getSelectedColor(), modifiers);
            return List.of(segmentWithFormatting);
        }
    }

    @Override
    public void delete(int offset) {
        RichText text = this.textGetter.get();
        if (this.selectionEnd != this.selectionStart) {
            int start = Math.min(this.selectionStart, this.selectionEnd);
            int end = Math.max(this.selectionStart, this.selectionEnd);

            text = text.replace(start, end, createSegmentsWithSelectedFormatting(""));
            this.selectionStart = this.selectionEnd = start;
        } else {
            int cursor = Util.moveCursor(text.getPlainText(), this.selectionStart, offset);
            int start = Math.min(cursor, this.selectionStart);
            int end = Math.max(cursor, this.selectionStart);

            text = text.replace(start, end, createSegmentsWithSelectedFormatting(""));
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
        int i = Math.min(this.selectionStart, this.selectionEnd);
        int j = Math.max(this.selectionStart, this.selectionEnd);
        return textGetter.get().subText(i, j).getAsFormattedString();
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
