package me.chrr.scribble;

import me.chrr.scribble.book.RichText;
import net.minecraft.util.Formatting;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;


public class RichTextReplaceTest {

    @Test
    public void testDifferentFormattedTextReplacementAtTheStartOfRichText() {
        String textToReplace = "XXXX";
        String replacementText = "YYYYYYYY";
        RichText.Segment replacementTextSegment = new RichText.Segment(
                replacementText, Formatting.GOLD, Set.of(Formatting.OBFUSCATED)
        );
        String singleSegmentPlainString = textToReplace + "AAAA\bBBBB";
        RichText richText = RichText.fromFormattedString(singleSegmentPlainString);
        assertEquals(1, richText.getSegments().size(), "Unexpected initial state");

        // Action
        richText = richText.replace(0, textToReplace.length(), new RichText(List.of(replacementTextSegment)));

        assertEquals(2, richText.getSegments().size());
        String expectedText = replacementText + singleSegmentPlainString.replaceAll(textToReplace, "");
        assertEquals(expectedText, richText.getPlainText());
        assertTrue(richText.getSegments().getFirst().text().contains(replacementText));
    }

    @Test
    public void testDifferentFormattedTextReplacementInTheMiddleOfRichTextSegment() {
        String textToReplace = "XXXX";
        String replacementText = "YYYYYYYY";
        RichText.Segment replacementTextSegment = new RichText.Segment(
                replacementText, Formatting.GOLD, Set.of(Formatting.OBFUSCATED)
        );
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
        richText = richText.replace(replaceStartOffset, replaceEndOffset, new RichText(List.of(replacementTextSegment)));

        // Assert
        // expected size is 4 because of the segments: AA + YY + CC + DD
        assertEquals(4, richText.getSegments().size());
        assertTrue(richText.getSegments().get(1).text().contains(replacementText));
    }

    @Test
    public void testTheWholeRichTextSegmentReplacementWithDifferentFormattedText() {
        String textToReplace = "XXXX";
        String replacementText = "YYYYYYYY";
        RichText.Segment replacementTextSegment = new RichText.Segment(
                replacementText, Formatting.GOLD, Set.of(Formatting.OBFUSCATED)
        );
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
        richText = richText.replace(replaceStartOffset, replaceEndOffset, new RichText(List.of(replacementTextSegment)));

        // Assert
        assertEquals(originSegments.size(), richText.getSegments().size());
        assertEquals(replacementText, richText.getSegments().get(1).text());
    }

    @Test
    public void testDifferentFormattedTextReplacementAtTheEndOfRichText() {
        String textToReplace = "XXXX";
        String replacementText = "YYYYYYYY";
        RichText.Segment replacementTextSegment = new RichText.Segment(
                replacementText, Formatting.GOLD, Set.of(Formatting.OBFUSCATED)
        );
        String singleSegmentPlainString = "AAAA\bBBBB" + textToReplace;
        RichText richText = RichText.fromFormattedString(singleSegmentPlainString);
        assertEquals(1, richText.getSegments().size(), "Unexpected initial state");

        // Action
        richText = richText.replace(
                singleSegmentPlainString.length() - textToReplace.length(),
                singleSegmentPlainString.length(),
                new RichText(List.of(replacementTextSegment))
        );

        // Assert
        assertEquals(2, richText.getSegments().size());
        String expectedText = singleSegmentPlainString.replaceAll(textToReplace, "") + replacementText;
        assertEquals(expectedText, richText.getPlainText());
        assertTrue(richText.getSegments().getLast().text().contains(replacementText));
    }

    @Test
    public void testIfMergingSimilarStyledSegmentsAfterSimilarFormattedStringReplacement() {
        // Arrange
        Formatting color = Formatting.BLUE;
        Set<Formatting> modifiers = Set.of(Formatting.UNDERLINE, Formatting.BOLD);
        Formatting otherColor = Formatting.RED;
        Set<Formatting> otherModifiers = Set.of(Formatting.OBFUSCATED);

        String replacementText = "YYYYYYYY";
        RichText.Segment replacementTextSegment = new RichText.Segment(
                replacementText, color, Set.copyOf(modifiers)
        );

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

        // Action
        richText = richText.replace(
                originSegments.get(0).text().length() / 2,
                originSegments.get(1).text().length() / 2,
                new RichText(List.of(replacementTextSegment))
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
    public void testIfMergingSimilarStyledSegmentsAfterDifferentFormattedStringReplacement() {
        // Arrange
        Formatting color = Formatting.BLUE;
        Set<Formatting> modifiers = Set.of(Formatting.UNDERLINE, Formatting.BOLD);
        Formatting otherColor = Formatting.RED;
        Set<Formatting> otherModifiers = Set.of(Formatting.OBFUSCATED);

        String replacementText = "YYYYYYYY";
        RichText.Segment replacementTextSegment = new RichText.Segment(
                replacementText, otherColor, Set.copyOf(otherModifiers)
        );

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

        // Action
        richText = richText.replace(
                originSegments.get(0).text().length() / 2,
                originSegments.get(1).text().length() / 2,
                new RichText(List.of(replacementTextSegment))
        );

        // Assert
        assertEquals(5, richText.getSegments().size());

        assertEquals(color, richText.getSegments().get(0).color());
        assertEquals(modifiers, richText.getSegments().get(0).modifiers());

        assertEquals(otherColor, richText.getSegments().get(1).color());
        assertEquals(otherModifiers, richText.getSegments().get(1).modifiers());

        assertEquals(color, richText.getSegments().get(2).color());
        assertEquals(modifiers, richText.getSegments().get(2).modifiers());

        assertEquals(otherColor, richText.getSegments().get(3).color());
        assertEquals(otherModifiers, richText.getSegments().get(3).modifiers());
    }
}
