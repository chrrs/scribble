package me.chrr.scribble;

import me.chrr.scribble.book.RichText;
import net.minecraft.util.Formatting;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;


public class RichTextInsertTest {

    // ---- Plain text insertion tests bellow ----

    @Test
    public void testPlainTextInsertionAtTheStartOfRichText() {
        String singleSegmentPlainString = "The quick brown fox\njumps over the lazy dog.";
        RichText richText = RichText.fromFormattedString(singleSegmentPlainString);
        assertEquals(1, richText.getSegments().size(), "Unexpected initial state");
        String stringToInsert = "boo!";

        // insert at the very start of rich text
        richText = richText.insert(0, stringToInsert, Formatting.GOLD, Set.of());

        assertEquals(2, richText.getSegments().size());
    }

    @Test
    public void testPlainTextInsertionIntoTheMiddleOfRichTextSegment() {
        List<RichText.Segment> originSegments = List.of(
                new RichText.Segment("AA\nAACC\nCC", Formatting.RED, Set.of(Formatting.BOLD)),
                new RichText.Segment("DDDD", Formatting.BLUE, Set.of(Formatting.UNDERLINE))
        );
        RichText richText = new RichText(List.copyOf(originSegments));
        assertEquals(originSegments.size(), richText.getSegments().size(), "Unexpected initial state");
        String stringToInsert = "BBBB";

        // Action
        // insert text into the middle of the first segment
        int middleOfTheFirstSegmentOffset = originSegments.getFirst().text().length() / 2;
        richText = richText.insert(middleOfTheFirstSegmentOffset, stringToInsert, Formatting.GOLD, Set.of(Formatting.OBFUSCATED));

        // Assert
        // expecting 4 segments:
        // 0: AA\nAA - the first half of the originSegments[0]
        // 1: BBBB - inserted
        // 2: CC\nCC - the second half of the originSegments[0]
        // 3: DDDD - full originSegments[1]
        assertEquals(4, richText.getSegments().size());
    }

    @Test
    public void testPlainTextInsertionBetweenRichTextSegments() {
        List<RichText.Segment> originSegments = List.of(
                new RichText.Segment("AA\nAA", Formatting.RED, Set.of(Formatting.BOLD)),
                new RichText.Segment("CC\nCC", Formatting.BLUE, Set.of(Formatting.BOLD))
        );
        RichText richText = new RichText(List.copyOf(originSegments));
        assertEquals(originSegments.size(), richText.getSegments().size(), "Unexpected initial state");
        String stringToInsert = "BBBB";

        // insert text at the end of the first / at the start of second segment
        int endOfTheFirstSegmentOffset = originSegments.getFirst().text().length();
        richText = richText.insert(endOfTheFirstSegmentOffset, stringToInsert, Formatting.GOLD, Set.of());

        assertEquals(originSegments.size() + 1, richText.getSegments().size());
    }

    @Test
    public void testPlainTextInsertionAtTheEndOfRichText() {
        String singleSegmentPlainString = "The quick brown fox\njumps over the lazy dog.";
        RichText richText = RichText.fromFormattedString(singleSegmentPlainString);
        assertEquals(1, richText.getSegments().size(), "Unexpected initial state");

        String stringToInsert = "boo!";

        // insert to the very end of rich text
        richText = richText.insert(richText.getLength(), stringToInsert, Formatting.GOLD, Set.of());

        assertEquals(2, richText.getSegments().size());
    }

    @Test
    public void testIfColorStaysCorrectAfterPlainTextInsertionIntoTheMiddleOfRichTextSegment() {
        // Arrange
        List<RichText.Segment> originSegments = List.of(
                new RichText.Segment("AA\nAA", Formatting.RED, Set.of(Formatting.BOLD)),
                new RichText.Segment("CC\nCC", Formatting.BLUE, Set.of(Formatting.BOLD))
        );
        RichText richText = new RichText(List.copyOf(originSegments));
        assertEquals(originSegments.size(), richText.getSegments().size(), "Unexpected initial state");

        String stringToInsert = "BBBB";
        Formatting stringToInsertColor = Formatting.GOLD;

        // Action
        // insert text at the end of the first / at the start of second segment
        int middleOfTheFirstSegmentOffset = originSegments.getFirst().text().length() / 2;
        richText = richText.insert(middleOfTheFirstSegmentOffset, stringToInsert, stringToInsertColor, Set.of());


        // Assert
        // check origin segments colors
        assertEquals(originSegments.getFirst().color(), richText.getSegments().getFirst().color());
        assertEquals(originSegments.getLast().color(), richText.getSegments().getLast().color());

        // check new segment colors
        assertEquals(
                stringToInsertColor,
                richText.getSegments().get(1).color() // inserted segment
        );
    }

    @Test
    public void testIfModifiersStaysCorrectAfterPlainTextInsertionIntoTheMiddleOfRichTextSegment() {
        // Arrange
        List<RichText.Segment> originSegments = List.of(
                new RichText.Segment("AA\nAA", Formatting.RED, Set.of(Formatting.BOLD)),
                new RichText.Segment("CC\nCC", Formatting.BLUE, Set.of(Formatting.BOLD))
        );
        RichText richText = new RichText(List.copyOf(originSegments));
        assertEquals(originSegments.size(), richText.getSegments().size(), "Unexpected initial state");

        String stringToInsert = "BBBB";
        Set<Formatting> stringToInsertModifiers = Set.of(Formatting.BOLD, Formatting.UNDERLINE);


        // Action
        // insert text at the end of the first / at the start of second segment
        int middleOfTheFirstSegmentOffset = originSegments.getFirst().text().length() / 2;
        richText = richText.insert(
                middleOfTheFirstSegmentOffset,
                stringToInsert,
                Formatting.LIGHT_PURPLE,
                stringToInsertModifiers
        );

        // Assert
        // check origin segments colors
        assertEquals(originSegments.getFirst().modifiers(), richText.getSegments().getFirst().modifiers());
        assertEquals(originSegments.getLast().modifiers(), richText.getSegments().getLast().modifiers());

        // check new segment modifiers
        assertEquals(
                stringToInsertModifiers,
                richText.getSegments().get(1).modifiers() // inserted segment
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

        // Action
        richText = richText.insert(0, formattedString);

        // Assert
        assertEquals(2, richText.getSegments().size());

        assertEquals(otherColor, richText.getSegments().get(0).color());
        assertEquals(otherModifiers, richText.getSegments().get(0).modifiers());

        assertEquals(color, richText.getSegments().get(1).color());
        assertEquals(modifiers, richText.getSegments().get(1).modifiers());
    }

    @Test
    public void testIfMergingSimilarStyledSegmentsAfterNoFormattedStringInsertion() {
        // Arrange
        Formatting color = Formatting.BLUE;
        Set<Formatting> modifiers = Set.of(Formatting.UNDERLINE, Formatting.BOLD);
        Formatting otherColor = Formatting.RED;
        Set<Formatting> otherModifiers = Set.of(Formatting.OBFUSCATED);
        List<RichText.Segment> originSegments = List.of(
                new RichText.Segment("AA\nAA", color, modifiers),
                new RichText.Segment("BB\nBB", color, modifiers),
                new RichText.Segment("CC\nCC", color, modifiers),
                new RichText.Segment("--\n--", otherColor, otherModifiers),
                new RichText.Segment("DD\nDD", color, modifiers),
                new RichText.Segment("EE\nEE", color, modifiers)
        );
        RichText richText = new RichText(List.copyOf(originSegments));
        assertEquals(originSegments.size(), richText.getSegments().size(), "Unexpected initial state");
        assertNotEquals(color, otherColor, "Incorrect initial state");
        assertNotEquals(modifiers, otherModifiers, "Incorrect initial state");

        // Action
        richText = richText.insert(originSegments.getFirst().text().length() / 2, "non-formatted");

        // Assert
        assertEquals(3, richText.getSegments().size());

        assertEquals(color, richText.getSegments().get(0).color());
        assertEquals(modifiers, richText.getSegments().get(0).modifiers());

        assertEquals(otherColor, richText.getSegments().get(1).color());
        assertEquals(otherModifiers, richText.getSegments().get(1).modifiers());

        assertEquals(color, richText.getSegments().get(2).color());
        assertEquals(modifiers, richText.getSegments().get(2).modifiers());
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

        // Action
        richText = richText.insert(originSegments.get(2).text().length() / 2, formattedString);

        // Assert
        assertEquals(3, richText.getSegments().size());

        assertEquals(color, richText.getSegments().get(0).color());
        assertEquals(modifiers, richText.getSegments().get(0).modifiers());

        assertEquals(otherColor, richText.getSegments().get(1).color());
        assertEquals(otherModifiers, richText.getSegments().get(1).modifiers());

        assertEquals(color, richText.getSegments().get(2).color());
        assertEquals(modifiers, richText.getSegments().get(2).modifiers());
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

        // Action
        richText = richText.insert(richText.getLength(), formattedString);

        // Assert
        assertEquals(2, richText.getSegments().size());

        assertEquals(color, richText.getSegments().get(0).color());
        assertEquals(modifiers, richText.getSegments().get(0).modifiers());

        assertEquals(otherColor, richText.getSegments().get(1).color());
        assertEquals(otherModifiers, richText.getSegments().get(1).modifiers());
    }

}
