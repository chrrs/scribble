package me.chrr.scribble.book;

import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A string of text that can have formatting applied to various parts
 * of its contents. Internally, it's divided into smaller segments with
 * their own formatting each. This rich text can be converted into Minecraft's
 * built-in text component system, or into color-coded plain strings.
 * <br>
 * Note that a RichText instance is immutable, and every method will create a
 * new instance. Treat this class as a sort of `String` alternative.
 * <br>
 * We could've used {@link net.minecraft.text.MutableText} for this, but we
 * need something more flexible to easily edit portions, and make sure the
 * amount of segments doesn't spiral out of control.
 *
 * @author chrrrs
 */
public class RichText implements StringVisitable {
    private final List<Segment> segments;

    /**
     * Create a new RichText object from existing segments.
     *
     * @param segments segments to inherit.
     */
    public RichText(List<Segment> segments) {
        this.segments = segments;
    }

    /**
     * Create a new empty RichText instance.
     *
     * @return a rich text with a single, empty, segment.
     */
    public static RichText empty() {
        return new RichText(List.of(new Segment("", Formatting.RESET, Set.of())));
    }

    /**
     * Create a new RichText instance from a color-coded string. Colors
     * should be indicated with the paragraph-symbol (§) according to the
     * <a href="https://minecraft.wiki/w/Formatting_codes">formatting codes</a>
     * available.
     *
     * @param input the color-coded input string.
     * @return a RichText instance containing exactly the same text as the input.
     */
    public static RichText fromFormattedString(String input) {
        List<Segment> segments = new ArrayList<>();

        StringBuilder text = new StringBuilder();
        Formatting color = Formatting.RESET;
        Set<Formatting> modifiers = new HashSet<>();

        for (int i = 0; i < input.length(); ) {
            int codePoint = input.codePointAt(i);
            i += Character.charCount(codePoint);

            if (codePoint == (int) '§') {
                if (i >= input.length()) {
                    break;
                }

                char code = input.charAt(i);
                i++;

                Formatting formatting = Formatting.byCode(code);
                if (formatting != null) {
                    if (!text.isEmpty()) {
                        segments.add(new Segment(text.toString(), color, new HashSet<>(modifiers)));
                        text = new StringBuilder();
                    }

                    if (formatting.isModifier()) {
                        modifiers.add(formatting);
                    } else {
                        color = formatting;
                        modifiers.clear();
                    }
                }
            } else {
                text.appendCodePoint(codePoint);
            }
        }

        if (!text.isEmpty()) {
            segments.add(new Segment(text.toString(), color, modifiers));
        }

        return new RichText(segments);
    }

    /**
     * @return the text without any formatting.
     */
    public String getPlainText() {
        return segments.stream()
                .map(segment -> segment.text)
                .collect(Collectors.joining());
    }

    /**
     * @return if the rich text has a length of zero.
     */
    public boolean isEmpty() {
        return getPlainText().isEmpty();
    }

    /**
     * @return the length of the plain text.
     */
    public int getLength() {
        return segments.stream()
                .map(segment -> segment.text.length())
                .reduce(0, Integer::sum);
    }

    /**
     * Get a smaller portion of the rich text.
     *
     * @param start start of the sub-text (inclusive).
     * @param end   end of the sub-text (exclusive).
     * @return the selected sub-text.
     */
    public RichText subText(int start, int end) {
        int current = 0;
        List<Segment> subSegments = new ArrayList<>();
        for (Segment segment : segments) {
            int length = segment.text.length();

            // We're before the segment we're searching for
            if (current + length <= start) {
                current += length;
                continue;
            }

            // We're after the segment, so we can stop
            if (current >= end) {
                break;
            }

            int localStart = Math.max(0, start - current);
            int localEnd = Math.min(length, end - current);

            subSegments.add(new Segment(segment.text.substring(localStart, localEnd), segment.color, segment.modifiers));
            current += length;
        }

        return new RichText(subSegments);
    }

    /**
     * Get the rich text with a portion replaced.
     *
     * @param start       start of the replacement area (inclusive).
     * @param end         end of the replacement area (exclusive).
     * @param replacement text to replace the area with.
     * @return a RichText instance with the text in the specified area replaced.
     */
    public RichText replace(int start, int end, String replacement) {
        int current = 0;
        List<Segment> newSegments = new ArrayList<>();
        boolean replacementAppended = false;
        for (Segment segment : segments) {
            int length = segment.text.length();

            // We're before the segment we're replacing in
            if (current + length <= start) {
                newSegments.add(segment);
                current += length;
                continue;
            }

            // We're after the segment we're replacing in
            if (current >= end) {
                newSegments.add(segment);
                continue;
            }

            int localStart = Math.max(0, start - current);
            int localEnd = Math.min(length, end - current);

            // There's some text before our region
            if (localStart > 0) {
                newSegments.add(new Segment(segment.text.substring(0, localStart), segment.color, segment.modifiers));
            }

            if (!replacement.isEmpty() && !replacementAppended) {
                newSegments.add(new Segment(replacement, segment.color, segment.modifiers));
                replacementAppended = true;
            }

            // There's some text after our region
            if (localEnd < length) {
                newSegments.add(new Segment(segment.text.substring(localEnd), segment.color, segment.modifiers));
            }

            current += length;
        }

        return new RichText(newSegments);
    }

    /**
     * Insert a new string of text. This text copies the style of the text
     * before it.
     *
     * @param offset offset into the text to start inserting.
     * @param text   string of text to insert.
     * @return a RichText instance with the specified string inserted.
     */
    public RichText insert(int offset, String text) {
        int current = 0;
        List<Segment> newSegments = new ArrayList<>(segments);
        for (int i = 0; i < segments.size(); i++) {
            Segment segment = segments.get(i);
            int length = segment.text.length();

            // We're before the segment we're searching for
            if (offset > current + length) {
                current += length;
                continue;
            }

            int localOffset = offset - current;
            String newText = segment.text.substring(0, localOffset) + text + segment.text.substring(localOffset);
            newSegments.set(i, new Segment(newText, segment.color, segment.modifiers));
            break;
        }

        return new RichText(newSegments);
    }

    /**
     * Apply formatting to a subsection of the rich text. This subsection can
     * have differing colors and formatting.
     *
     * @param start           the start of the subsection (inclusive).
     * @param end             the end of the subsection (exclusive).
     * @param newColor        the color to set the subsection to, or null to leave it.
     * @param addModifiers    the modifiers to add to the subsection.
     * @param removeModifiers the modifiers to remove from the subsection.
     * @return a RichText instance with the specified formatting applied.
     */
    public RichText applyFormatting(
            int start, int end,
            @Nullable Formatting newColor,
            Set<Formatting> addModifiers,
            Set<Formatting> removeModifiers
    ) {
        int current = 0;
        List<Segment> newSegments = new ArrayList<>();
        for (Segment segment : segments) {
            int length = segment.text.length();

            // We're before the segment we're modifying in
            if (current + length <= start) {
                newSegments.add(segment);
                current += length;
                continue;
            }

            // We're after the segment we're modifying in
            if (current >= end) {
                newSegments.add(segment);
                continue;
            }

            int localStart = Math.max(0, start - current);
            int localEnd = Math.min(length, end - current);

            // There's some text before our region
            if (localStart > 0) {
                newSegments.add(new Segment(segment.text.substring(0, localStart), segment.color, segment.modifiers));
            }

            // The text we're modifying
            String modifiedText = segment.text.substring(localStart, localEnd);

            // Let's calculate the final color and modifiers.
            Formatting color = Optional.ofNullable(newColor).orElse(segment.color);
            Set<Formatting> modifiers = new HashSet<>(segment.modifiers);
            modifiers.addAll(addModifiers);
            modifiers.removeAll(removeModifiers);

            newSegments.add(new Segment(modifiedText, color, modifiers));

            // There's some text after our region
            if (localEnd < length) {
                newSegments.add(new Segment(segment.text.substring(localEnd), segment.color, segment.modifiers));
            }

            current += length;
        }

        return new RichText(newSegments);
    }

    /**
     * Get the rich text as a color-coded string, in line with what is described
     * on the <a href="https://minecraft.wiki/w/Formatting_codes">Formatting Codes wiki page</a>.
     *
     * @return the color-coded string.
     */
    public String getAsFormattedString() {
        StringBuilder out = new StringBuilder();

        Formatting currentColor = Formatting.RESET;
        Set<Formatting> currentModifiers = new HashSet<>();

        for (Segment segment : segments) {
            if (segment.text.isEmpty()) {
                continue;
            }

            boolean colorChanged = !segment.color.equals(currentColor);

            Set<Formatting> modifiersToRemove = new HashSet<>(currentModifiers);
            modifiersToRemove.removeAll(segment.modifiers);
            boolean shouldReapply = colorChanged || !modifiersToRemove.isEmpty();

            Set<Formatting> modifiersToAdd = new HashSet<>(segment.modifiers);
            if (!shouldReapply) {
                modifiersToAdd.removeAll(currentModifiers);
            }

            if (colorChanged || shouldReapply) {
                out.append(segment.color);
            }

            for (Formatting format : modifiersToAdd) {
                out.append(format);
            }

            out.append(segment.text);

            currentColor = segment.color;
            currentModifiers = segment.modifiers;
        }

        return out.toString();
    }

    @Override
    public <T> Optional<T> visit(Visitor<T> visitor) {
        for (Segment segment : segments) {
            Optional<T> out = visitor.accept(segment.text);
            if (out.isPresent()) {
                return out;
            }
        }

        return Optional.empty();
    }

    @Override
    public <T> Optional<T> visit(StyledVisitor<T> styledVisitor, Style baseStyle) {
        for (Segment segment : segments) {
            Style style = baseStyle
                    .withFormatting(segment.modifiers.toArray(new Formatting[0]))
                    .withColor(segment.color);
            Optional<T> out = styledVisitor.accept(style, segment.text);
            if (out.isPresent()) {
                return out;
            }
        }

        return Optional.empty();
    }

    @Override
    public String getString() {
        return this.getPlainText();
    }

    /**
     * A segment of rich text. This segment can only have a single color and set of
     * modifiers.
     *
     * @param text      the text that the segment represents.
     * @param color     the color of the segment.
     * @param modifiers the list of formatting modifiers for the segment.
     */
    public record Segment(String text, Formatting color, Set<Formatting> modifiers) {
    }
}
