package me.chrr.scribble;

import me.chrr.scribble.book.RichSelectionManager;
import me.chrr.scribble.book.RichText;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


public class RichSelectionManagerInsertTest {

    private RichSelectionManager createRichSelectionManager(
            RichText initialRichText,
            Consumer<RichText> richTextSetter
    ) {
        Supplier<RichText> richTextGetter = () -> initialRichText;

        Consumer<String> stringSetter = string -> {
        };

        RichSelectionManager.StateCallback stateCallback = (color, modifiers) -> {
        };

        Supplier<String> clipboardGetter = () -> "";

        Consumer<String> clipboardSetter = string -> {
        };

        Predicate<RichText> textFilter = new Predicate<>() {
            @Override
            public boolean test(RichText richText) {
                return true;
            }

            @NotNull
            @Override
            public Predicate<RichText> and(@NotNull Predicate<? super RichText> other) {
                return Predicate.super.and(other);
            }

            @NotNull
            @Override
            public Predicate<RichText> negate() {
                return Predicate.super.negate();
            }

            @NotNull
            @Override
            public Predicate<RichText> or(@NotNull Predicate<? super RichText> other) {
                return Predicate.super.or(other);
            }
        };

        return new RichSelectionManager(
                richTextGetter,
                richTextSetter,
                stringSetter,
                stateCallback,
                clipboardGetter,
                clipboardSetter,
                textFilter
        );
    }

    private void moveSelection(RichSelectionManager richSelectionManager, int offset) {
        richSelectionManager.selectionStart = offset;
        richSelectionManager.selectionEnd = offset;
        richSelectionManager.updateSelectionFormatting();
    }

    @Test
    public void testPlainTextInsertionAtTheStartOfRichText() {
        // Arrange
        String stringToInsert = "boo!";
        String singleSegmentPlainString = "The quick brown fox\njumps over the lazy dog.";
        RichText richText = RichText.fromFormattedString(singleSegmentPlainString);
        assertEquals(1, richText.getSegments().size(), "Unexpected initial state");

        Consumer<RichText> richTextSetter = mock();
        RichSelectionManager selectionManager = createRichSelectionManager(richText, richTextSetter);

        // insert position
        moveSelection(selectionManager, 0);


        // Action
        selectionManager.insert(stringToInsert);


        // Asset
        verify(richTextSetter).accept(argThat(newRichText -> newRichText.getSegments().size() == 1));
        verify(richTextSetter).accept(
                argThat(newRichText -> newRichText.getSegments().getFirst().text().contains(stringToInsert))
        );
    }

    @Test
    public void testPlainTextInsertionIntoTheMiddleOfRichTextSegment() {
        // Arrange
        String stringToInsert = "BBBB";
        List<RichText.Segment> originSegments = List.of(
                new RichText.Segment("AA\nAACC\nCC", Formatting.RED, Set.of(Formatting.BOLD)),
                new RichText.Segment("DDDD", Formatting.BLUE, Set.of(Formatting.UNDERLINE))
        );
        RichText richText = new RichText(List.copyOf(originSegments));
        assertEquals(originSegments.size(), richText.getSegments().size(), "Unexpected initial state");

        Consumer<RichText> richTextSetter = mock();
        RichSelectionManager selectionManager = createRichSelectionManager(richText, richTextSetter);

        // insert position
        int middleOfTheFirstSegmentOffset = originSegments.getFirst().text().length() / 2;
        moveSelection(selectionManager, middleOfTheFirstSegmentOffset);


        // Action
        // insert text into the middle of the first segment
        selectionManager.insert(stringToInsert);


        // Assert
        // expected segments:
        // 0: AA\nAA + BBBB + CC\nCC - originSegments[0] with inserted value
        // 1: DDDD - full originSegments[1]
        verify(richTextSetter).accept(argThat(newRichText -> newRichText.getSegments().size() == 2));
        verify(richTextSetter).accept(
                argThat(newRichText -> newRichText.getSegments().getFirst().text().contains(stringToInsert))
        );
    }

    @Test
    public void testPlainTextInsertionBetweenRichTextSegments() {
        // Arrange
        String stringToInsert = "BBBB";
        List<RichText.Segment> originSegments = List.of(
                new RichText.Segment("AA\nAA", Formatting.RED, Set.of(Formatting.BOLD)),
                new RichText.Segment("CC\nCC", Formatting.BLUE, Set.of(Formatting.BOLD))
        );
        RichText richText = new RichText(List.copyOf(originSegments));
        assertEquals(originSegments.size(), richText.getSegments().size(), "Unexpected initial state");

        Consumer<RichText> richTextSetter = mock();
        RichSelectionManager selectionManager = createRichSelectionManager(richText, richTextSetter);

        // insert text at the end of the first / at the start of second segment
        int endOfTheFirstSegmentOffset = originSegments.getFirst().text().length();
        moveSelection(selectionManager, endOfTheFirstSegmentOffset);


        // Action
        selectionManager.insert(stringToInsert);


        // Assert
        verify(richTextSetter).accept(argThat(newRichText -> newRichText.getSegments().size() == 2));
        verify(richTextSetter).accept(
                argThat(newRichText -> newRichText.getSegments().getFirst().text().contains(stringToInsert))
        );
    }

    @Test
    public void testPlainTextInsertionAtTheEndOfRichText() {
        // Arrange
        String stringToInsert = "boo!";
        String singleSegmentPlainString = "The quick brown fox\njumps over the lazy dog.";
        RichText richText = RichText.fromFormattedString(singleSegmentPlainString);
        assertEquals(1, richText.getSegments().size(), "Unexpected initial state");

        Consumer<RichText> richTextSetter = mock();
        RichSelectionManager selectionManager = createRichSelectionManager(richText, richTextSetter);

        moveSelection(selectionManager, richText.getLength());


        // Action
        selectionManager.insert(stringToInsert);


        // Assert
        verify(richTextSetter).accept(argThat(newRichText -> newRichText.getSegments().size() == 1));
        verify(richTextSetter).accept(
                argThat(newRichText -> newRichText.getSegments().getFirst().text().contains(stringToInsert))
        );
    }

    @Test
    public void testIfColorStaysCorrectAfterPlainTextInsertionIntoTheMiddleOfRichTextSegment() {
        // Arrange
        String stringToInsert = "BBBB";
        List<RichText.Segment> originSegments = List.of(
                new RichText.Segment("AA\nAA", Formatting.RED, Set.of(Formatting.BOLD)),
                new RichText.Segment("CC\nCC", Formatting.BLUE, Set.of(Formatting.BOLD))
        );
        RichText richText = new RichText(List.copyOf(originSegments));
        assertEquals(originSegments.size(), richText.getSegments().size(), "Unexpected initial state");

        Consumer<RichText> richTextSetter = mock();
        RichSelectionManager selectionManager = createRichSelectionManager(richText, richTextSetter);

        int middleOfTheFirstSegmentOffset = originSegments.getFirst().text().length() / 2;
        moveSelection(selectionManager, middleOfTheFirstSegmentOffset);

        // Action
        selectionManager.insert(stringToInsert);


        // Assert
        // check origin segments colors
        assertEquals(originSegments.getFirst().color(), richText.getSegments().getFirst().color());
        assertEquals(originSegments.getLast().color(), richText.getSegments().getLast().color());

        // Assert
        verify(richTextSetter).accept(
                argThat(newRichText -> newRichText.getSegments().getFirst().color() == originSegments.getFirst().color())
        );
        verify(richTextSetter).accept(
                argThat(newRichText -> newRichText.getSegments().getLast().color() == originSegments.getLast().color())
        );
    }

    @Test
    public void testIfModifiersStaysCorrectAfterPlainTextInsertionIntoTheMiddleOfRichTextSegment() {
        // Arrange
        String stringToInsert = "BBBB";

        List<RichText.Segment> originSegments = List.of(
                new RichText.Segment("AA\nAA", Formatting.RED, Set.of(Formatting.BOLD)),
                new RichText.Segment("CC\nCC", Formatting.BLUE, Set.of(Formatting.BOLD))
        );
        RichText richText = new RichText(List.copyOf(originSegments));
        assertEquals(originSegments.size(), richText.getSegments().size(), "Unexpected initial state");

        Consumer<RichText> richTextSetter = mock();
        RichSelectionManager selectionManager = createRichSelectionManager(richText, richTextSetter);

        int middleOfTheFirstSegmentOffset = originSegments.getFirst().text().length() / 2;
        moveSelection(selectionManager, middleOfTheFirstSegmentOffset);

        // Action
        selectionManager.insert(stringToInsert);

        // Assert
        verify(richTextSetter).accept(
                argThat(newRichText -> newRichText.getSegments().getFirst().modifiers()
                        .equals(originSegments.getFirst().modifiers()))
        );
        verify(richTextSetter).accept(
                argThat(newRichText -> newRichText.getSegments().getLast().modifiers()
                        .equals(originSegments.getLast().modifiers()))
        );
    }

    @Test
    public void testIfMergingSimilarStyledSegmentsAfterFormattedStringInsertionAtTheStart() {
        // Arrange
        Formatting color = Formatting.BLUE;
        Set<Formatting> modifiers = Set.of(Formatting.UNDERLINE, Formatting.BOLD);
        Formatting otherColor = Formatting.RED;
        Set<Formatting> otherModifiers = Set.of(Formatting.OBFUSCATED);
        String formattedString = otherColor
                + String.join(" ", otherModifiers.stream().map(Formatting::toString).toList())
                + "formatted-string-text";

        List<RichText.Segment> originSegments = List.of(
                new RichText.Segment("AA\nAA", color, Set.copyOf(modifiers)),
                new RichText.Segment("BB\nBB", color, Set.copyOf(modifiers)),
                new RichText.Segment("CC\nCC", color, Set.copyOf(modifiers)),
                new RichText.Segment("DD\nDD", color, Set.copyOf(modifiers)),
                new RichText.Segment("EE\nEE", color, Set.copyOf(modifiers))
        );
        RichText richText = new RichText(List.copyOf(originSegments));
        assertEquals(originSegments.size(), richText.getSegments().size(), "Unexpected initial state");
        assertNotEquals(color, otherColor, "Incorrect initial state");
        assertNotEquals(modifiers, otherModifiers, "Incorrect initial state");

        Consumer<RichText> richTextSetter = mock();
        RichSelectionManager selectionManager = createRichSelectionManager(richText, richTextSetter);

        moveSelection(selectionManager, 0);

        // Action
        selectionManager.insert(formattedString);


        // Assert
        verify(richTextSetter).accept(argThat(newRichText -> newRichText.getSegments().size() == 2));

        verify(richTextSetter).accept(
                argThat(newRichText -> newRichText.getSegments().get(0).color() == otherColor)
        );
        verify(richTextSetter).accept(
                argThat(newRichText -> newRichText.getSegments().get(0).modifiers().equals(otherModifiers))
        );

        verify(richTextSetter).accept(
                argThat(newRichText -> newRichText.getSegments().get(1).color() == color)
        );
        verify(richTextSetter).accept(
                argThat(newRichText -> newRichText.getSegments().get(1).modifiers().equals(modifiers))
        );
    }

    @Test
    public void testIfMergingSimilarStyledSegmentsAfterNoFormattedStringInsertion() {
        // Arrange
        String notFormattedString = "non-formatted";
        Formatting color = Formatting.BLUE;
        Set<Formatting> modifiers = Set.of(Formatting.UNDERLINE, Formatting.BOLD);
        Formatting otherColor = Formatting.RED;
        Set<Formatting> otherModifiers = Set.of(Formatting.OBFUSCATED);
        List<RichText.Segment> originSegments = List.of(
                new RichText.Segment("AA\nAA", color, Set.copyOf(modifiers)),
                new RichText.Segment("BB\nBB", color, Set.copyOf(modifiers)),
                new RichText.Segment("CC\nCC", color, Set.copyOf(modifiers)),
                new RichText.Segment("--\n--", otherColor, Set.copyOf(otherModifiers)),
                new RichText.Segment("DD\nDD", color, Set.copyOf(modifiers)),
                new RichText.Segment("EE\nEE", color, Set.copyOf(modifiers))
        );
        RichText richText = new RichText(List.copyOf(originSegments));
        assertEquals(originSegments.size(), richText.getSegments().size(), "Unexpected initial state");
        assertNotEquals(color, otherColor, "Incorrect initial state");
        assertNotEquals(modifiers, otherModifiers, "Incorrect initial state");

        Consumer<RichText> richTextSetter = mock();
        RichSelectionManager selectionManager = createRichSelectionManager(richText, richTextSetter);

        moveSelection(selectionManager, originSegments.getFirst().text().length() / 2);


        // Action
        selectionManager.insert(notFormattedString);


        // Assert
        verify(richTextSetter).accept(argThat(newRichText -> newRichText.getSegments().size() == 3));

        verify(richTextSetter).accept(
                argThat(newRichText -> newRichText.getSegments().get(0).color() == color)
        );
        verify(richTextSetter).accept(
                argThat(newRichText -> newRichText.getSegments().get(0).modifiers().equals(modifiers))
        );

        verify(richTextSetter).accept(
                argThat(newRichText -> newRichText.getSegments().get(1).color() == otherColor)
        );
        verify(richTextSetter).accept(
                argThat(newRichText -> newRichText.getSegments().get(1).modifiers().equals(otherModifiers))
        );

        verify(richTextSetter).accept(
                argThat(newRichText -> newRichText.getSegments().get(2).color() == color)
        );
        verify(richTextSetter).accept(
                argThat(newRichText -> newRichText.getSegments().get(2).modifiers().equals(modifiers))
        );
    }

    @Test
    public void testIfMergingSimilarStyledSegmentsAfterFormattedStringInsertionInTheMiddle() {
        // Arrange
        Formatting color = Formatting.BLUE;
        Set<Formatting> modifiers = Set.of(Formatting.UNDERLINE, Formatting.BOLD);
        Formatting otherColor = Formatting.RED;
        Set<Formatting> otherModifiers = Set.of(Formatting.OBFUSCATED);
        String formattedString = otherColor
                + String.join(" ", otherModifiers.stream().map(Formatting::toString).toList())
                + "formatted-string-text";

        List<RichText.Segment> originSegments = List.of(
                new RichText.Segment("AA\nAA", color, Set.copyOf(modifiers)),
                new RichText.Segment("BB\nBB", color, Set.copyOf(modifiers)),
                new RichText.Segment("CC\nCC", color, Set.copyOf(modifiers)),
                new RichText.Segment("DD\nDD", color, Set.copyOf(modifiers)),
                new RichText.Segment("EE\nEE", color, Set.copyOf(modifiers))
        );
        RichText richText = new RichText(List.copyOf(originSegments));
        assertEquals(originSegments.size(), richText.getSegments().size(), "Unexpected initial state");
        assertNotEquals(color, otherColor, "Incorrect initial state");
        assertNotEquals(modifiers, otherModifiers, "Incorrect initial state");

        Consumer<RichText> richTextSetter = mock();
        RichSelectionManager selectionManager = createRichSelectionManager(richText, richTextSetter);

        moveSelection(selectionManager, originSegments.get(2).text().length() / 2);


        // Action
        selectionManager.insert(formattedString);


        // Assert
        verify(richTextSetter).accept(argThat(newRichText -> newRichText.getSegments().size() == 3));

        verify(richTextSetter).accept(
                argThat(newRichText -> newRichText.getSegments().get(0).color() == color)
        );
        verify(richTextSetter).accept(
                argThat(newRichText -> newRichText.getSegments().get(0).modifiers().equals(modifiers))
        );

        verify(richTextSetter).accept(
                argThat(newRichText -> newRichText.getSegments().get(1).color() == otherColor)
        );
        verify(richTextSetter).accept(
                argThat(newRichText -> newRichText.getSegments().get(1).modifiers().equals(otherModifiers))
        );

        verify(richTextSetter).accept(
                argThat(newRichText -> newRichText.getSegments().get(2).color() == color)
        );
        verify(richTextSetter).accept(
                argThat(newRichText -> newRichText.getSegments().get(2).modifiers().equals(modifiers))
        );
    }

    @Test
    public void testIfMergingSimilarStyledSegmentsAfterFormattedStringInsertionAtTheEnd() {
        // Arrange
        Formatting color = Formatting.BLUE;
        Set<Formatting> modifiers = Set.of(Formatting.UNDERLINE, Formatting.BOLD);
        Formatting otherColor = Formatting.RED;
        Set<Formatting> otherModifiers = Set.of(Formatting.OBFUSCATED);
        String formattedString = otherColor
                + String.join(" ", otherModifiers.stream().map(Formatting::toString).toList())
                + "formatted-string-text";

        List<RichText.Segment> originSegments = List.of(
                new RichText.Segment("AA\nAA", color, modifiers),
                new RichText.Segment("BB\nBB", color, modifiers),
                new RichText.Segment("CC\nCC", color, modifiers),
                new RichText.Segment("DD\nDD", color, modifiers),
                new RichText.Segment("EE\nEE", color, modifiers)
        );
        RichText richText = new RichText(List.copyOf(originSegments));
        assertEquals(originSegments.size(), richText.getSegments().size(), "Unexpected initial state");
        assertNotEquals(color, otherColor, "Incorrect initial state");
        assertNotEquals(modifiers, otherModifiers, "Incorrect initial state");

        Consumer<RichText> richTextSetter = mock();
        RichSelectionManager selectionManager = createRichSelectionManager(richText, richTextSetter);

        moveSelection(selectionManager, richText.getLength());


        // Action
        selectionManager.insert(formattedString);


        // Assert
        verify(richTextSetter).accept(argThat(newRichText -> newRichText.getSegments().size() == 2));

        verify(richTextSetter).accept(
                argThat(newRichText -> newRichText.getSegments().get(0).color() == color)
        );
        verify(richTextSetter).accept(
                argThat(newRichText -> newRichText.getSegments().get(0).modifiers().equals(modifiers))
        );

        verify(richTextSetter).accept(
                argThat(newRichText -> newRichText.getSegments().get(1).color() == otherColor)
        );
        verify(richTextSetter).accept(
                argThat(newRichText -> newRichText.getSegments().get(1).modifiers().equals(otherModifiers))
        );
    }

}
