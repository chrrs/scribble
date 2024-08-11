package me.chrr.scribble.book;

import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import org.jetbrains.annotations.NotNull;
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
     * Merges consecutive segments with the same color and modifiers attributes into a single segment.
     *
     * @param segments a list of {@link Segment} objects to be processed. The list must not be null.
     * @return a list of {@link Segment} objects with consecutive segments of the same style merged.
     */
    @NotNull
    private static List<Segment> mergeSimilarStyledSegments(@NotNull List<Segment> segments) {
        if (segments.isEmpty()) {
            return segments;
        }
        List<Segment> mergedSegments = new ArrayList<>();

        Segment currentStyledSegment = null;
        for (Segment segment : segments) {
            if (currentStyledSegment == null) {
                currentStyledSegment = segment;
                continue;
            }

            boolean isColorMatching = currentStyledSegment.color == segment.color;
            boolean isModifiersMatching = segment.modifiers.containsAll(currentStyledSegment.modifiers())
                    && currentStyledSegment.modifiers().containsAll(segment.modifiers);

            if (isColorMatching && isModifiersMatching) {
                // merge segment with currentStyledSegment
                currentStyledSegment = new Segment(
                        currentStyledSegment.text + segment.text,
                        currentStyledSegment.color,
                        currentStyledSegment.modifiers
                );
            } else {
                // put currentStyledSegment to the result
                mergedSegments.add(currentStyledSegment);

                // and set segment as a new currentStyledSegment
                currentStyledSegment = segment;
            }
        }

        // adding the last styled segment
        mergedSegments.add(currentStyledSegment);

        return mergedSegments;
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
     * <p>
     * If the replacement string contains formatting, its color and modifiers are used. If not, the formatting from the
     * start replacement position is applied. See {@link #createSegmentsWithLocalFormatting(int, int, String)}
     * <p>
     *
     * @param start       start of the replacement area (inclusive).
     * @param end         end of the replacement area (exclusive).
     * @param replacement text to replace the area with.
     * @return a RichText instance with the text in the specified area replaced.
     */
    public RichText replace(int start, int end, String replacement) {
        List<Segment> replacementSegments = createSegmentsWithLocalFormatting(start, end, replacement);
        return replace(start, end, replacementSegments);
    }

    public RichText replace(int start, int end, List<Segment> replacementSegments) {
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

            if (!replacementSegments.isEmpty() && !replacementAppended) {
                newSegments.addAll(replacementSegments);
                replacementAppended = true;
            }

            // There's some text after our region
            if (localEnd < length) {
                newSegments.add(new Segment(segment.text.substring(localEnd), segment.color, segment.modifiers));
            }

            current += length;
        }

        newSegments = mergeSimilarStyledSegments(newSegments);
        return new RichText(newSegments);
    }

    /**
     * Insert a new string of text with specified styling.
     *
     * @param offset    offset into the text to start inserting.
     * @param text      string of text to insert.
     * @param color     color of the text. See {@link Formatting}
     * @param modifiers modifiers of the text. See {@link Formatting}
     * @return a RichText instance with the specified string inserted.
     */
    public RichText insert(int offset, String text, Formatting color, Set<Formatting> modifiers) {
        return insert(offset, List.of(new Segment(text, color, modifiers)));
    }

    /**
     * Inserts the given string at the specified offset.
     * <p>
     * If the string contains formatting, its color and modifiers are used. If not, the formatting from the
     * insertion location is applied. See {@link #createSegmentsWithLocalFormatting(int, int, String)}
     * <p>
     *
     * @param offset the position at which to insert the string
     * @param string the string to insert
     * @return a new {@link RichText} with the string inserted
     */
    public RichText insert(int offset, String string) {
        List<Segment> insertSegments = createSegmentsWithLocalFormatting(offset, offset, string);
        return insert(offset, insertSegments);
    }

    /**
     * Inserts a list of new {@link Segment} objects into the {@link RichText} at the specified text offset.
     *
     * <p>If the offset is beyond the end of the current text, the new segments are appended at the end.
     * The method splits existing segments if necessary and merges similar styled segments afterwards.</p>
     *
     * @param offset      the position at which to insert the new segments (including formatting characters)
     * @param newSegments a list of {@link Segment} objects to insert
     * @return a new {@link RichText} instance with the segments inserted
     */
    public RichText insert(int offset, List<Segment> newSegments) {
        if (newSegments.isEmpty()) {
            return this;
        }

        if (this.segments.isEmpty()) {
            return new RichText(newSegments);
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
            combinedSegments.addAll(i, newSegments);

            boolean shouldSplitSegment = offset < currentOffset + segmentLength;
            if (shouldSplitSegment) {
                // Second: add the end part of the splitted segment
                Segment segmentEndPart = new Segment(segment.text.substring(inSegmentOffset), segment.color, segment.modifiers);
                combinedSegments.add(i + newSegments.size(), segmentEndPart);
            }

            // Third: add the start part of the splitted segment
            if (offset - currentOffset > 0) {
                Segment segmentStartPart =
                        new Segment(segment.text.substring(0, inSegmentOffset), segment.color, segment.modifiers);
                combinedSegments.add(i, segmentStartPart);
            }

            break;
        }

        combinedSegments = mergeSimilarStyledSegments(combinedSegments);
        return new RichText(combinedSegments);
    }

    /**
     * Creates the segments for the given string, applying formatting based on its content and location.
     * <p>
     * If the string contains formatting (e.g., color or modifiers), that formatting is preserved.
     * If it does not contain formatting, the formatting from the start till end location is applied.
     * Strings with only the RESET formatting tag are treated as non-formatted.
     *
     * @param start  the start of a formating selection range in the text
     * @param end    the end of a formating selection range in the text
     * @param string the string to be inserted, which may or may not contain formatting
     * @return a list of {@link Segment} objects representing the formatted segments of the string
     */
    private List<Segment> createSegmentsWithLocalFormatting(int start, int end, String string) {
        // A non-formatted string copied from the book will always contain the RESET formatting tag.
        // We need to exclude this tag from the check to ensure
        // that we can properly apply the existing text formatting
        // ToDo change the logic of copying non-formatted string from a book to return string without any styles at all
        boolean isFormattedString = !Formatting.strip(string)
                .equals(string.replaceAll(Formatting.RESET.toString(), ""));

        if (isFormattedString) {
            // The isFormattedString check is required here,
            // because RichText.fromFormattedString creates a RichText with Back color by default,
            // instead of keeping the origin color(no color).
            // ToDo make RichText.fromFormattedString return no color for non formatted strings
            RichText richString = RichText.fromFormattedString(string);
            return richString.getSegments();

        } else {
            // If the string does not contain formatting, apply formatting from the start-end range.

            // Read formatting from the position just after the start offset when possible
            // to get a more accurate formatting for cases where start != end.
            int offsetToReadCurrentFormatting = Math.max(Math.min(start + 1, end), start);

            Pair<@Nullable Formatting, Set<Formatting>> colorAndModifiersFromOffset =
                    getCommonFormat(offsetToReadCurrentFormatting, offsetToReadCurrentFormatting);
            Segment segmentWithFormatting = new Segment(
                    string,
                    Optional.ofNullable(colorAndModifiersFromOffset.getLeft()).orElse(Formatting.RESET),
                    colorAndModifiersFromOffset.getRight()
            );
            return List.of(segmentWithFormatting);
        }
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
