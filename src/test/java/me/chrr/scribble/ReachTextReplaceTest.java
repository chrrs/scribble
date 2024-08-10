package me.chrr.scribble;

import me.chrr.scribble.book.RichText;
import net.minecraft.util.Formatting;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class ReachTextReplaceTest {

    @Test
    public void testPlainTextReplacementAtTheStartOfRichText() {
        String textToReplace = "XXXX";
        String replacementText = "YYYYYYYY";
        String singleSegmentPlainString = textToReplace + "AAAA\bBBBB";
        RichText richText = RichText.fromFormattedString(singleSegmentPlainString);
        assertEquals(1, richText.getSegments().size(), "Unexpected initial state");

        // Action
        richText = richText.replace(0, textToReplace.length(), replacementText);

        assertEquals(2, richText.getSegments().size());
        assertEquals(
                singleSegmentPlainString.length() - textToReplace.length() + replacementText.length(),
                richText.getAsFormattedString().length()
        );
        assertEquals(replacementText, richText.getSegments().getFirst().text());
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
        // New number of segments is 4 because:
        // origin segment 1 -> splitted into two -> 2 segments
        // origin segment 2 -> still the same  -> 1 segment
        // new segment inserted -> 1 segment
        assertEquals(originSegments.size() + 2, richText.getSegments().size());
        assertEquals(replacementText, richText.getSegments().get(1).text());
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
        assertEquals(2, richText.getSegments().size());
        assertEquals(
                singleSegmentPlainString.length() - textToReplace.length() + replacementText.length(),
                richText.getAsFormattedString().length()
        );
        assertEquals(replacementText, richText.getSegments().getLast().text());
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
        // richText.getSegments().get(0) -> AA - splitted segment, first half
        assertEquals(originSegments.get(0).color(), richText.getSegments().get(0).color());

        // richText.getSegments().get(1) -> replacementText
        assertEquals(originSegments.get(0).color(), richText.getSegments().get(1).color());

        // richText.getSegments().get(2) -> CC - splitted segment, second half
        assertEquals(originSegments.get(0).color(), richText.getSegments().get(2).color());

        // richText.getSegments().get(3) -> DD
        assertEquals(originSegments.get(1).color(), richText.getSegments().get(3).color());
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
        // richText.getSegments().get(0) -> AA - splitted segment, first half
        assertEquals(originSegments.get(0).modifiers(), richText.getSegments().get(0).modifiers());

        // richText.getSegments().get(1) -> replacementText
        assertEquals(originSegments.get(0).modifiers(), richText.getSegments().get(1).modifiers());

        // richText.getSegments().get(2) -> CC - splitted segment, second half
        assertEquals(originSegments.get(0).modifiers(), richText.getSegments().get(2).modifiers());

        // richText.getSegments().get(3) -> DD
        assertEquals(originSegments.get(1).modifiers(), richText.getSegments().get(3).modifiers());
    }

}
