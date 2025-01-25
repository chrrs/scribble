package me.chrr.scribble;

import me.chrr.scribble.book.SynchronizedPageList;
import me.chrr.scribble.book.RichText;
import org.junit.jupiter.api.Test;

import java.util.List;

import static me.chrr.scribble.mixture.CommonMixture.mockPages;
import static me.chrr.scribble.mixture.CommonMixture.mockSynchronizedPageList;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("SequencedCollectionMethodCanBeUsed")
public class SynchronizedPageListTest {

    @Test
    public void getRichPagesShouldReturnListCopyWhenCalled() {
        SynchronizedPageList synchronizedList = mockSynchronizedPageList(3);

        // Action
        List<RichText> firstActual = synchronizedList.getRichPages();
        List<RichText> secondActual = synchronizedList.getRichPages();

        // Assert
        assertEquals(firstActual, secondActual);
        assertNotSame(firstActual, secondActual);
    }


    @Test
    public void populatePagesShouldUpdateRich() {
        List<String> initialPages = mockPages(1, "init");
        List<String> newPages = mockPages(1, "new");
        SynchronizedPageList synchronizedList = new SynchronizedPageList();
        synchronizedList.populate(initialPages);

        // Action
        synchronizedList.populate(newPages);

        // Assert
        assertNotEquals(RichText.fromFormattedString(initialPages.get(0)), synchronizedList.getRichPages().get(0));
        assertEquals(newPages.size(), synchronizedList.size());
        assertEquals(RichText.fromFormattedString(newPages.get(0)), synchronizedList.getRichPages().get(0));
    }

    @Test
    public void setShouldUpdateBothLists() {
        SynchronizedPageList synchronizedList = mockSynchronizedPageList(1);
        RichText newRichText = RichText.fromFormattedString("Updated Page");

        // Action
        synchronizedList.set(0, newRichText);

        // Assert
        assertEquals(newRichText, synchronizedList.getRichPages().get(0));
        assertEquals(newRichText.getAsFormattedString(), synchronizedList.getPages().get(0));
    }

    @Test
    public void addRichTextShouldAddToBothLists() {
        SynchronizedPageList synchronizedList = new SynchronizedPageList();
        RichText newRichText = RichText.fromFormattedString("New Page");

        // Action
        synchronizedList.add(newRichText);

        // Assert
        assertEquals(1, synchronizedList.size());
        assertEquals(newRichText, synchronizedList.getRichPages().get(0));
        assertEquals(newRichText.getAsFormattedString(), synchronizedList.getPages().get(0));
    }

    @Test
    public void addRichTextAtIndexShouldAddToBothLists() {
        SynchronizedPageList synchronizedList = mockSynchronizedPageList(1);
        RichText newRichText = RichText.fromFormattedString("New Page");

        // Action
        synchronizedList.add(0, newRichText);

        // Assert
        assertEquals(2, synchronizedList.size());
        assertEquals(newRichText, synchronizedList.getRichPages().get(0));
        assertEquals(newRichText.getAsFormattedString(), synchronizedList.getPages().get(0));
    }

    @Test
    public void addAllCollectionShouldAddToBothLists() {
        SynchronizedPageList synchronizedList = new SynchronizedPageList();
        List<RichText> newRichTexts = List.of(
                RichText.fromFormattedString("Page 1"),
                RichText.fromFormattedString("Page 2")
        );

        // Action
        synchronizedList.addAll(newRichTexts);

        // Assert
        assertEquals(2, synchronizedList.size());
        assertEquals(newRichTexts.get(0), synchronizedList.getRichPages().get(0));
        assertEquals(newRichTexts.get(1), synchronizedList.getRichPages().get(1));
        assertEquals(newRichTexts.get(0).getAsFormattedString(), synchronizedList.getPages().get(0));
        assertEquals(newRichTexts.get(1).getAsFormattedString(), synchronizedList.getPages().get(1));
    }

    @Test
    public void removeShouldRemoveFromBothLists() {
        SynchronizedPageList synchronizedList = mockSynchronizedPageList(1);

        // Action
        synchronizedList.remove(0);

        // Assert
        assertEquals(0, synchronizedList.size());
        assertTrue(synchronizedList.getRichPages().isEmpty());
        assertTrue(synchronizedList.getPages().isEmpty());
    }

    @Test
    public void clearShouldClearBothLists() {
        SynchronizedPageList synchronizedList = mockSynchronizedPageList(1);

        // Action
        synchronizedList.clear();

        // Assert
        assertEquals(0, synchronizedList.size());
        assertTrue(synchronizedList.getRichPages().isEmpty());
        assertTrue(synchronizedList.getPages().isEmpty());
    }

    @Test
    public void sizeShouldReturnSizeOfRichPages() {
        int initialSize = 2;
        SynchronizedPageList synchronizedList = mockSynchronizedPageList(initialSize);

        int actual = synchronizedList.size();

        assertEquals(initialSize, actual);
    }

    @Test
    public void isEmptyShouldReturnTrueWhenRichPagesIsEmpty() {
        SynchronizedPageList synchronizedList = new SynchronizedPageList();

        boolean actual = synchronizedList.isEmpty();

        assertTrue(actual);
    }

    @Test
    public void isEmptyShouldReturnFalseWhenRichPagesIsNotEmpty() {
        SynchronizedPageList synchronizedList = mockSynchronizedPageList(1);

        boolean actual = synchronizedList.isEmpty();

        assertFalse(actual);
    }

    @Test
    public void arePagesEmptyShouldReturnTrueWhenAllPagesAreEmpty() {
        List<String> pages = List.of("", "");
        SynchronizedPageList synchronizedList = new SynchronizedPageList();
        synchronizedList.populate(pages);

        boolean actual = synchronizedList.arePagesEmpty();

        assertTrue(actual);
    }

    @Test
    public void arePagesEmptyShouldReturnFalseWhenAtLeastOnePageIsNotEmpty() {
        List<String> pages = List.of("", "not empty");
        SynchronizedPageList synchronizedList = new SynchronizedPageList();
        synchronizedList.populate(pages);

        boolean actual = synchronizedList.arePagesEmpty();

        assertFalse(actual);
    }
}
