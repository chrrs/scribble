package me.chrr.scribble.book;

import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
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
     * Create a new single-segment RichText object from existing text and style.
     *
     * @param text      text of the segment.
     * @param color     color of the segment.
     * @param modifiers modifiers of the segment.
     */
    public RichText(String text, Formatting color, Set<Formatting> modifiers) {
        this(List.of(new Segment(text, color, modifiers)));
    }

    public List<Segment> getSegments() {
        return segments;
    }

    /**
     * Create a new empty RichText instance.
     *
     * @return a rich text without any segments.
     */
    public static RichText empty() {
        return new RichText(List.of());
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
        Formatting color = Formatting.BLACK;
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
     * Create a RichText best representing the given StringVisitable object. This
     * operation is lossy, as not every style that JSON-based text can contain is
     * possible to be represented as formatting codes. For RGB-colors, the closest
     * available color is chosen. Hover events, click events and fonts are discarded.
     *
     * @param stringVisitable the StringVisitable to represent.
     * @return the closest approximation of the given StringVisitable.
     */
    public static RichText fromStringVisitableLossy(StringVisitable stringVisitable) {
        List<Segment> segments = new ArrayList<>();

        AtomicReference<Formatting> color = new AtomicReference<>(Formatting.BLACK);
        stringVisitable.visit((style, string) -> {
            if (style.getColor() != null) {
                color.set(formattingFromTextColor(style.getColor()));
            }

            Set<Formatting> modifiers = new HashSet<>();
            if (style.isBold()) modifiers.add(Formatting.BOLD);
            if (style.isItalic()) modifiers.add(Formatting.ITALIC);
            if (style.isUnderlined()) modifiers.add(Formatting.UNDERLINE);
            if (style.isObfuscated()) modifiers.add(Formatting.OBFUSCATED);
            if (style.isStrikethrough()) modifiers.add(Formatting.STRIKETHROUGH);

            segments.add(new Segment(string, color.get(), modifiers));
            return Optional.empty();
        }, Style.EMPTY.withFormatting(Formatting.BLACK));

        return new RichText(segments);
    }

    /**
     * Find the closest formatting code that represents the given color.
     *
     * @param color the TextColor to convert. This can be any RGB color.
     * @return the formatting code best representing the given text color.
     */
    private static Formatting formattingFromTextColor(TextColor color) {
        // If the color has a name, we can look it up directly.
        Formatting byName = Formatting.byName(color.getName());
        if (byName != null) {
            return byName;
        }

        // Otherwise, let's find the closest matching color.
        Formatting closest = Formatting.BLACK;
        int distance = Integer.MAX_VALUE;
        for (Formatting formatting : Formatting.values()) {
            Integer colorValue = formatting.getColorValue();
            if (colorValue == null) {
                continue;
            }

            // Find the Euclidean distance between the two colors (without taking the square root).
            int dr = Math.abs((colorValue >> 16 & 0xff) - (color.getRgb() >> 16 & 0xff));
            int dg = Math.abs((colorValue >> 8 & 0xff) - (color.getRgb() >> 8 & 0xff));
            int db = Math.abs((colorValue & 0xff) - (color.getRgb() & 0xff));

            int dist = dr + dg + db;
            if (dist < distance) {
                closest = formatting;
                distance = dist;
            }
        }

        return closest;
    }

    /**
     * Merges consecutive segments with the same color and modifiers attributes into a single segment.
     *
     * @return a new {@link RichText} object that has the same contents in potentially fewer segments.
     */
    @NotNull
    private RichText mergeSimilarSegments() {
        if (this.segments.isEmpty()) {
            return this;
        }

        List<Segment> mergedSegments = new ArrayList<>();
        Segment previousSegment = null;

        for (Segment segment : this.segments) {
            if (previousSegment == null) {
                previousSegment = segment;
                continue;
            }

            boolean isColorMatching = previousSegment.color == segment.color;
            boolean areModifiersMatching = segment.modifiers.containsAll(previousSegment.modifiers())
                    && previousSegment.modifiers().containsAll(segment.modifiers);

            if (isColorMatching && areModifiersMatching) {
                previousSegment = new Segment(
                        previousSegment.text + segment.text,
                        previousSegment.color,
                        previousSegment.modifiers
                );
            } else {
                mergedSegments.add(previousSegment);
                previousSegment = segment;
            }
        }

        mergedSegments.add(previousSegment);

        return new RichText(mergedSegments);
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
     * Returns the rich text with a portion replaced.
     *
     * @param start       start of the replacement area (inclusive).
     * @param end         end of the replacement area (exclusive).
     * @param replacement text to replace the area with.
     * @return a RichText instance with the text in the specified area replaced.
     */
    public RichText replace(int start, int end, RichText replacement) {
        int current = 0;
        List<Segment> newSegments = new ArrayList<>();
        boolean replacementAppended = false;
        for (Segment segment : this.segments) {
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
                newSegments.addAll(replacement.segments);
                replacementAppended = true;
            }

            // There's some text after our region
            if (localEnd < length) {
                newSegments.add(new Segment(segment.text.substring(localEnd), segment.color, segment.modifiers));
            }

            current += length;
        }

        return new RichText(newSegments).mergeSimilarSegments();
    }

    /**
     * Inserts a piece of text into the {@link RichText} at the specified text offset.
     *
     * <p>If the offset is beyond the end of the current text, the new segments are appended at the end.
     * The method splits existing segments if necessary and merges similar styled segments afterwards.</p>
     *
     * @param offset the position at which to insert the new segments (including formatting characters)
     * @param text   the rich text to insert
     * @return a new {@link RichText} instance with the segments inserted
     */
    public RichText insert(int offset, RichText text) {
        if (text.isEmpty()) {
            return this;
        }

        if (this.segments.isEmpty()) {
            return text;
        }

        List<Segment> combinedSegments = new ArrayList<>(segments);

        int currentOffset = 0;
        for (int i = 0; i < segments.size(); i++) {
            Segment segment = segments.get(i);
            int segmentLength = segment.text.length();

            // Check if we should insert into/before this segment
            if (offset > currentOffset + segmentLength) {
                // We're before the segment we're searching for
                currentOffset += segmentLength;
                continue;
            }

            // former localOffset
            int inSegmentOffset = offset - currentOffset;

            // Merging segments in a specific order, to keep the indices correct.
            // First: adding new segments
            combinedSegments.remove(i);
            combinedSegments.addAll(i, text.segments);

            boolean shouldSplitSegment = offset < currentOffset + segmentLength;
            if (shouldSplitSegment) {
                // Second: add the end part of the split segment
                Segment segmentEndPart = new Segment(segment.text.substring(inSegmentOffset), segment.color, segment.modifiers);
                combinedSegments.add(i + text.segments.size(), segmentEndPart);
            }

            // Third: add the start part of the split segment
            if (offset - currentOffset > 0) {
                Segment segmentStartPart =
                        new Segment(segment.text.substring(0, inSegmentOffset), segment.color, segment.modifiers);
                combinedSegments.add(i, segmentStartPart);
            }

            break;
        }

        return new RichText(combinedSegments).mergeSimilarSegments();
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
        if (start == end) {
            return this;
        }

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

        Formatting currentColor = Formatting.BLACK;
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

    /**
     * Get the common color and modifiers in the specified text range.
     * The color returned will be null if the range has multiple colors.
     *
     * @param start start of text range (inclusive).
     * @param end   end of text range (exclusive).
     * @return a pair with the common color and modifiers.
     */
    public Pair<@Nullable Formatting, Set<Formatting>> getCommonFormat(int start, int end) {
        boolean first = true;
        Set<Formatting> modifiers = Set.of();
        Formatting color = Formatting.BLACK;

        int current = 0;
        for (Segment segment : segments) {
            int length = segment.text.length();

            // If we have a zero-width selection, we want the formatting of
            // the segment before it.
            if (start == end && start <= current + length) {
                return new Pair<>(segment.color, segment.modifiers);
            }

            // We're before the segment we're searching for
            if (current + length <= start) {
                current += length;
                continue;
            }

            // We're after the segment, so we can stop
            if (current >= end) {
                break;
            }

            // For the first segment, we initialize the values. Otherwise,
            // we just adapt them to the current segment.
            if (first) {
                modifiers = new HashSet<>(segment.modifiers);
                color = segment.color;
                first = false;
            } else {
                modifiers.retainAll(segment.modifiers);

                // Set the color to null if it's different.
                if (color != segment.color) {
                    color = null;
                }
            }

            current += length;
        }

        return new Pair<>(color, modifiers);
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

    @Override
    public String toString() {
        return "RichText{" +
                "segments=" + segments +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RichText richText = (RichText) o;
        return Objects.equals(segments, richText.segments);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(segments);
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
