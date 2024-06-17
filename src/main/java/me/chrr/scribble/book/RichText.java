package me.chrr.scribble.book;

import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class RichText implements StringVisitable {
    private final List<Segment> segments;

    public RichText(List<Segment> segments) {
        this.segments = segments;
    }

    public static RichText empty() {
        return new RichText(List.of(new Segment("", Formatting.RESET, Set.of())));
    }

    public static RichText fromFormattedString(String string) {
        List<Segment> segments = new ArrayList<>();

        StringBuilder text = new StringBuilder();
        Formatting color = Formatting.RESET;
        Set<Formatting> modifiers = new HashSet<>();

        for (int i = 0; i < string.length(); ) {
            int codePoint = string.codePointAt(i);
            i += Character.charCount(codePoint);

            if (codePoint == (int) '§') {
                if (i >= string.length()) {
                    break;
                }

                char code = string.charAt(i);
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

    public String getPlainText() {
        return segments.stream()
                .map(segment -> segment.text)
                .collect(Collectors.joining());
    }

    public boolean isEmpty() {
        return getPlainText().isEmpty();
    }

    public int getLength() {
        return segments.stream()
                .map(segment -> segment.text.length())
                .reduce(0, Integer::sum);
    }

    // Return a small portion of the rich text, from start to end (exclusive).
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

    // Return the rich text with a portion from start to end (exclusive) replaced.
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

    public RichText insert(int index, String text) {
        int current = 0;
        List<Segment> newSegments = new ArrayList<>(segments);
        for (int i = 0; i < segments.size(); i++) {
            Segment segment = segments.get(i);
            int length = segment.text.length();

            // We're before the segment we're searching for
            if (index > current + length) {
                current += length;
                continue;
            }

            int localIndex = index - current;
            String newText = segment.text.substring(0, localIndex) + text + segment.text.substring(localIndex);
            newSegments.set(i, new Segment(newText, segment.color, segment.modifiers));
            break;
        }

        return new RichText(newSegments);
    }

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

    public record Segment(String text, Formatting color, Set<Formatting> modifiers) {
    }
}
