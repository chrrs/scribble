package me.chrr.scribble;

import me.chrr.scribble.book.RichText;
import net.minecraft.util.Formatting;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;


public class RichTextInsertTest {

    @Test
    public void testDifferentFormattedTextInsertionAtTheStartOfRichText() {
        String singleSegmentPlainString = "The quick brown fox\njumps over the lazy dog.";
        RichText richText = RichText.fromFormattedString(singleSegmentPlainString);
        assertEquals(1, richText.getSegments().size(), "Unexpected initial state");

        String stringToInsert = "boo!";
        RichText.Segment segmentToInsert = new RichText.Segment(stringToInsert, Formatting.WHITE, Set.of(Formatting.OBFUSCATED));

        // Action
        // insert at the very start of rich text
        richText = richText.insert(0, new RichText(List.of(segmentToInsert)));

        // Assert
        assertEquals(2, richText.getSegments().size());
    }

    @Test
    public void testDifferentFormattedTextInsertionIntoTheMiddleOfRichTextSegment() {
        List<RichText.Segment> originSegments = List.of(
                new RichText.Segment("AA\nAACC\nCC", Formatting.RED, Set.of(Formatting.BOLD)),
                new RichText.Segment("DDDD", Formatting.BLUE, Set.of(Formatting.UNDERLINE))
        );
        RichText richText = new RichText(List.copyOf(originSegments));
        assertEquals(originSegments.size(), richText.getSegments().size(), "Unexpected initial state");

        String stringToInsert = "BBBB";
        RichText.Segment segmentToInsert = new RichText.Segment(stringToInsert, Formatting.WHITE, Set.of(Formatting.OBFUSCATED));
        int middleOfTheFirstSegmentOffset = originSegments.getFirst().text().length() / 2;

        // Action
        richText = richText.insert(middleOfTheFirstSegmentOffset, new RichText(List.of(segmentToInsert)));

        // Assert
        // expecting 4 segments:
        // 0: AA\nAA - the first half of the originSegments[0]
        // 1: BBBB - inserted
        // 2: CC\nCC - the second half of the originSegments[0]
        // 3: DDDD - full originSegments[1]
        assertEquals(4, richText.getSegments().size());
    }

    @Test
    public void testDifferentFormattedTextInsertionBetweenRichTextSegments() {
        List<RichText.Segment> originSegments = List.of(
                new RichText.Segment("AA\nAA", Formatting.RED, Set.of(Formatting.BOLD)),
                new RichText.Segment("CC\nCC", Formatting.BLUE, Set.of(Formatting.BOLD))
        );
        RichText richText = new RichText(List.copyOf(originSegments));
        assertEquals(originSegments.size(), richText.getSegments().size(), "Unexpected initial state");

        String stringToInsert = "BBBB";
        RichText.Segment segmentToInsert = new RichText.Segment(stringToInsert, Formatting.WHITE, Set.of(Formatting.OBFUSCATED));
        int endOfTheFirstSegmentOffset = originSegments.getFirst().text().length();

        // Action
        richText = richText.insert(endOfTheFirstSegmentOffset, new RichText(List.of(segmentToInsert)));

        // Asset
        assertEquals(originSegments.size() + 1, richText.getSegments().size());
    }

    @Test
    public void testDifferentFormattedTextInsertionAtTheEndOfRichText() {
        String singleSegmentPlainString = "The quick brown fox\njumps over the lazy dog.";
        RichText richText = RichText.fromFormattedString(singleSegmentPlainString);
        assertEquals(1, richText.getSegments().size(), "Unexpected initial state");

        String stringToInsert = "boo!";
        RichText.Segment segmentToInsert = new RichText.Segment(stringToInsert, Formatting.WHITE, Set.of(Formatting.OBFUSCATED));

        // Action
        richText = richText.insert(richText.getLength(), new RichText(List.of(segmentToInsert)));

        // Asset
        assertEquals(2, richText.getSegments().size());
    }

    @Test
    public void testIfColorStaysCorrectAfterDifferentFormattedTextInsertionIntoTheMiddleOfRichTextSegment() {
        // Arrange
        List<RichText.Segment> originSegments = List.of(
                new RichText.Segment("AA\nAA", Formatting.RED, Set.of(Formatting.BOLD)),
                new RichText.Segment("CC\nCC", Formatting.BLUE, Set.of(Formatting.BOLD))
        );
        RichText richText = new RichText(List.copyOf(originSegments));
        assertEquals(originSegments.size(), richText.getSegments().size(), "Unexpected initial state");

        String stringToInsert = "BBBB";
        RichText.Segment segmentToInsert = new RichText.Segment(stringToInsert, Formatting.WHITE, Set.of(Formatting.OBFUSCATED));
        int middleOfTheFirstSegmentOffset = originSegments.getFirst().text().length() / 2;


        // Action
        richText = richText.insert(middleOfTheFirstSegmentOffset, new RichText(List.of(segmentToInsert)));


        // Assert
        // check origin segments colors
        assertEquals(originSegments.getFirst().color(), richText.getSegments().getFirst().color());
        assertEquals(originSegments.getLast().color(), richText.getSegments().getLast().color());

        // check new segment colors
        assertEquals(
                segmentToInsert.color(),
                richText.getSegments().get(1).color() // inserted segment
        );
    }

    @Test
    public void testIfModifiersStaysCorrectAfterDifferentFormattedTextInsertionIntoTheMiddleOfRichTextSegment() {
        // Arrange
        List<RichText.Segment> originSegments = List.of(
                new RichText.Segment("AA\nAA", Formatting.RED, Set.of(Formatting.BOLD)),
                new RichText.Segment("CC\nCC", Formatting.BLUE, Set.of(Formatting.BOLD))
        );
        RichText richText = new RichText(List.copyOf(originSegments));
        assertEquals(originSegments.size(), richText.getSegments().size(), "Unexpected initial state");

        String stringToInsert = "BBBB";
        RichText.Segment segmentToInsert = new RichText.Segment(stringToInsert, Formatting.WHITE, Set.of(Formatting.OBFUSCATED));
        int middleOfTheFirstSegmentOffset = originSegments.getFirst().text().length() / 2;


        // Action
        richText = richText.insert(middleOfTheFirstSegmentOffset, new RichText(List.of(segmentToInsert)));


        // Assert
        // check origin segments colors
        assertEquals(originSegments.getFirst().modifiers(), richText.getSegments().getFirst().modifiers());
        assertEquals(originSegments.getLast().modifiers(), richText.getSegments().getLast().modifiers());

        // check new segment modifiers
        assertEquals(
                segmentToInsert.modifiers(),
                richText.getSegments().get(1).modifiers() // inserted segment
        );
    }

    @Test
    public void testIfMergingSimilarStyledSegmentsAfterDifferentFormattedStringInsertionAtTheStart() {
        // Arrange
        Formatting color = Formatting.BLUE;
        Set<Formatting> modifiers = Set.of(Formatting.UNDERLINE, Formatting.BOLD);
        Formatting otherColor = Formatting.RED;
        Set<Formatting> otherModifiers = Set.of(Formatting.OBFUSCATED);

        String stringToInsert = "string-text";
        RichText.Segment segmentToInsert = new RichText.Segment(stringToInsert, otherColor, Set.copyOf(otherModifiers));

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


        // Action
        richText = richText.insert(0, new RichText(List.of(segmentToInsert)));


        // Assert
        assertEquals(2, richText.getSegments().size());

        assertEquals(otherColor, richText.getSegments().get(0).color());
        assertEquals(otherModifiers, richText.getSegments().get(0).modifiers());

        assertEquals(color, richText.getSegments().get(1).color());
        assertEquals(modifiers, richText.getSegments().get(1).modifiers());
    }

    @Test
    public void testIfMergingSimilarStyledSegmentsAfterFormattedStringInsertionInTheMiddle() {
        // Arrange
        Formatting color = Formatting.BLUE;
        Set<Formatting> modifiers = Set.of(Formatting.UNDERLINE, Formatting.BOLD);
        Formatting otherColor = Formatting.RED;
        Set<Formatting> otherModifiers = Set.of(Formatting.OBFUSCATED);

        String stringToInsert = "string-text";
        RichText.Segment segmentToInsert = new RichText.Segment(stringToInsert, otherColor, Set.copyOf(otherModifiers));

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


        // Action
        richText = richText.insert(originSegments.get(2).text().length() / 2, new RichText(List.of(segmentToInsert)));


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
    public void testIfMergingSimilarStyledSegmentsAfterDifferentFormattedFormattedStringInsertionAtTheEnd() {
        // Arrange
        Formatting color = Formatting.BLUE;
        Set<Formatting> modifiers = Set.of(Formatting.UNDERLINE, Formatting.BOLD);
        Formatting otherColor = Formatting.RED;
        Set<Formatting> otherModifiers = Set.of(Formatting.OBFUSCATED);
        String stringToInsert = "string-text";
        RichText.Segment segmentToInsert = new RichText.Segment(stringToInsert, otherColor, Set.copyOf(otherModifiers));

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


        // Action
        richText = richText.insert(richText.getLength(), new RichText(List.of(segmentToInsert)));


        // Assert
        assertEquals(2, richText.getSegments().size());

        assertEquals(color, richText.getSegments().get(0).color());
        assertEquals(modifiers, richText.getSegments().get(0).modifiers());

        assertEquals(otherColor, richText.getSegments().get(1).color());
        assertEquals(otherModifiers, richText.getSegments().get(1).modifiers());
    }

}
