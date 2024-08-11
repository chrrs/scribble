package me.chrr.scribble;

import me.chrr.scribble.book.RichText;
import net.minecraft.util.Formatting;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;


public class RichTextReplaceTest {

    @Test
    public void testPlainTextReplacementAtTheStartOfRichText() {
        String textToReplace = "XXXX";
        String replacementText = "YYYYYYYY";
        String singleSegmentPlainString = textToReplace + "AAAA\bBBBB";
        RichText richText = RichText.fromFormattedString(singleSegmentPlainString);
        assertEquals(1, richText.getSegments().size(), "Unexpected initial state");

        // Action
        richText = richText.replace(0, textToReplace.length(), replacementText);

        assertEquals(1, richText.getSegments().size());
        String expectedText = replacementText + singleSegmentPlainString.replaceAll(textToReplace, "");
        assertEquals(expectedText, richText.getAsFormattedString());
        assertTrue(richText.getSegments().getFirst().text().contains(replacementText));
    }

    @Test
    public void testPlainTextReplacementInTheMiddleOfRichTextSegment() {
        String textToReplace = "XXXX";
        String replacementText = "YYYYYYYY";
        List<RichText.Segment> originSegments = List.of(
                new RichText.Segment("AA\nAA" + textToReplace + "CC\nCC", Formatting.RED, Set.of(Formatting.BOLD)),
                new RichText.Segment("DDDD", Formatting.BLUE, Set.of(Formatting.UNDERLINE))
        );
        RichText richText = new RichText(List.copyOf(originSegments));
        assertEquals(originSegments.size(), richText.getSegments().size(), "Unexpected initial state");

        // Action
        // replace text in the middle of the first segment
        int replaceStartOffset = (originSegments.getFirst().text().length() - textToReplace.length()) / 2;
        int replaceEndOffset = replaceStartOffset + textToReplace.length();
        richText = richText.replace(replaceStartOffset, replaceEndOffset, replacementText);

        // Assert
        // The number of segments is still the same, because replacementText has no formatting:
        assertEquals(originSegments.size(), richText.getSegments().size());
        assertTrue(richText.getSegments().get(0).text().contains(replacementText));
    }

    @Test
    public void testTheWholeRichTextSegmentReplacementWithPlainText() {
        String textToReplace = "XXXX";
        String replacementText = "YYYYYYYY";
        List<RichText.Segment> originSegments = List.of(
                new RichText.Segment("AAAA", Formatting.RED, Set.of(Formatting.BOLD)),
                new RichText.Segment(textToReplace, Formatting.GREEN, Set.of(Formatting.OBFUSCATED)),
                new RichText.Segment("DDDD", Formatting.BLUE, Set.of(Formatting.UNDERLINE))
        );
        RichText richText = new RichText(List.copyOf(originSegments));
        assertEquals(originSegments.size(), richText.getSegments().size(), "Unexpected initial state");

        // Action
        // replace text in the middle of the first segment
        int replaceStartOffset = originSegments.getFirst().text().length();
        int replaceEndOffset = replaceStartOffset + textToReplace.length();
        richText = richText.replace(replaceStartOffset, replaceEndOffset, replacementText);

        // Assert
        assertEquals(originSegments.size(), richText.getSegments().size());
        assertEquals(replacementText, richText.getSegments().get(1).text());
    }

    @Test
    public void testPlainTextReplacementAtTheEndOfRichText() {
        String textToReplace = "XXXX";
        String replacementText = "YYYYYYYY";
        String singleSegmentPlainString = "AAAA\bBBBB" + textToReplace;
        RichText richText = RichText.fromFormattedString(singleSegmentPlainString);
        assertEquals(1, richText.getSegments().size(), "Unexpected initial state");

        // Action
        richText = richText.replace(
                singleSegmentPlainString.length() - textToReplace.length(),
                singleSegmentPlainString.length(),
                replacementText
        );

        // Assert
        assertEquals(1, richText.getSegments().size());
        String expectedText = singleSegmentPlainString.replaceAll(textToReplace, "") + replacementText;
        assertEquals(expectedText, richText.getAsFormattedString());
        assertTrue(richText.getSegments().getLast().text().contains(replacementText));
    }

    @Test
    public void testInsertedPlainTextTakesColorOfTheSplittedRichTextSegment() {
        // Arrange
        String textToReplace = "XXXX";
        String replacementText = "YYYYYYYY";
        List<RichText.Segment> originSegments = List.of(
                new RichText.Segment("AA\nAA" + textToReplace + "CC\nCC", Formatting.RED, Set.of(Formatting.BOLD)),
                new RichText.Segment("DDDD", Formatting.BLUE, Set.of(Formatting.UNDERLINE))
        );
        RichText richText = new RichText(List.copyOf(originSegments));
        assertEquals(originSegments.size(), richText.getSegments().size(), "Unexpected initial state");

        // Action
        // replace text in the middle of the first segment
        int replaceStartOffset = (originSegments.getFirst().text().length() - textToReplace.length()) / 2;
        int replaceEndOffset = replaceStartOffset + textToReplace.length();
        richText = richText.replace(replaceStartOffset, replaceEndOffset, replacementText);

        // Assert
        // richText.getSegments().get(0) -> AAYYCC
        assertEquals(originSegments.get(0).color(), richText.getSegments().get(0).color());

        // richText.getSegments().get(3) -> DD
        assertEquals(originSegments.get(1).color(), richText.getSegments().get(1).color());
    }

    @Test
    public void testInsertedPlainTextTakesModifiersOfTheSplittedRichTextSegment() {
        // Arrange
        String textToReplace = "XXXX";
        String replacementText = "YYYYYYYY";
        List<RichText.Segment> originSegments = List.of(
                new RichText.Segment("AA\nAA" + textToReplace + "CC\nCC", Formatting.RED, Set.of(Formatting.BOLD)),
                new RichText.Segment("DDDD", Formatting.BLUE, Set.of(Formatting.UNDERLINE))
        );
        RichText richText = new RichText(List.copyOf(originSegments));
        assertEquals(originSegments.size(), richText.getSegments().size(), "Unexpected initial state");

        // Action
        // replace text in the middle of the first segment
        int replaceStartOffset = (originSegments.getFirst().text().length() - textToReplace.length()) / 2;
        int replaceEndOffset = replaceStartOffset + textToReplace.length();
        richText = richText.replace(replaceStartOffset, replaceEndOffset, replacementText);

        // Assert
        // richText.getSegments().get(0) -> AAYYCC
        assertEquals(originSegments.get(0).modifiers(), richText.getSegments().get(0).modifiers());

        // richText.getSegments().get(3) -> DD
        assertEquals(originSegments.get(1).modifiers(), richText.getSegments().get(1).modifiers());
    }

    @Test
    public void testIfMergingSimilarStyledSegmentsAfterNoFormattedStringReplacement() {
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
        richText = richText.replace(
                originSegments.get(0).text().length() / 2,
                originSegments.get(1).text().length() / 2,
                "non-formatted"
        );

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
    public void testIfMergingSimilarStyledSegmentsAfterFormattedStringReplacementAtTheStart() {
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
        richText = richText.replace(0, originSegments.getFirst().text().length() / 2, formattedString);

        // Assert
        assertEquals(2, richText.getSegments().size());

        assertEquals(otherColor, richText.getSegments().get(0).color());
        assertEquals(otherModifiers, richText.getSegments().get(0).modifiers());

        assertEquals(color, richText.getSegments().get(1).color());
        assertEquals(modifiers, richText.getSegments().get(1).modifiers());
    }

    @Test
    public void testIfMergingSimilarStyledSegmentsAfterFormattedStringReplacementInTheMiddle() {
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
        richText = richText.replace(
                originSegments.get(2).text().length() / 2,
                originSegments.get(3).text().length() / 2,
                formattedString
        );

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
    public void testIfMergingSimilarStyledSegmentsAfterFormattedStringReplacementAtTheEnd() {
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
        richText = richText.replace(
                richText.getLength() - originSegments.getLast().text().length() / 2,
                richText.getLength(),
                formattedString
        );

        // Assert
        assertEquals(2, richText.getSegments().size());

        assertEquals(color, richText.getSegments().get(0).color());
        assertEquals(modifiers, richText.getSegments().get(0).modifiers());

        assertEquals(otherColor, richText.getSegments().get(1).color());
        assertEquals(otherModifiers, richText.getSegments().get(1).modifiers());
    }

}
