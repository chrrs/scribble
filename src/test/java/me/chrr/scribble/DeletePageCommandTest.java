package me.chrr.scribble;

import me.chrr.scribble.book.RichText;
import me.chrr.scribble.model.command.DeletePageCommand;
import me.chrr.scribble.model.command.PagesListener;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DeletePageCommandTest {

    private static List<RichText> mockPageList(int size) {
        ArrayList<RichText> list = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            RichText item = RichText.fromFormattedString("page content:" + i);
            list.add(item);
        }

        return list;
    }

    @Test
    public void testIfPagesListenerOnPageRemovedIsCalledAfterExecution() {
        PagesListener pagesListener = mock();
        int deleteIndex = 2;
        int size = deleteIndex + 2;
        DeletePageCommand deletePageCommand = new DeletePageCommand(mockPageList(size), deleteIndex, pagesListener);

        // Action
        deletePageCommand.execute();

        // Assert
        verify(pagesListener).scribble$onPageRemoved(deleteIndex);
    }

    @Test
    public void testIfPagesListenerOnPageRemovedIsCalledAfterLastPageExecution() {
        PagesListener pagesListener = mock();
        int size = 1;
        int deleteIndex = 0;
        DeletePageCommand deletePageCommand = new DeletePageCommand(mockPageList(size), deleteIndex, pagesListener);

        // Action
        deletePageCommand.execute();

        // Assert
        verify(pagesListener).scribble$onPageRemoved(deleteIndex);
    }

    @Test
    public void testIfPagesListenerOnPageAddedIsCalledAfterRollback() {
        // Arrange
        PagesListener pagesListener = mock();
        int deleteIndex = 2;
        int size = deleteIndex + 2;
        List<RichText> pages = mockPageList(size);
        DeletePageCommand deletePageCommand = new DeletePageCommand(pages, deleteIndex, pagesListener);
        deletePageCommand.execute();

        // Action
        deletePageCommand.rollback();

        // Assert
        verify(pagesListener).scribble$onPageAdded(deleteIndex);
    }

    @Test
    public void testIfPagesListenerOnPageAddedIsCalledAfterRollbackWithEmptyPagesList() {
        // Arrange
        PagesListener pagesListener = mock();
        int size = 1;
        int deleteIndex = 0;
        List<RichText> pages = mockPageList(size);
        DeletePageCommand deletePageCommand = new DeletePageCommand(pages, deleteIndex, pagesListener);
        deletePageCommand.execute();

        // Action
        deletePageCommand.rollback();

        // Assert
        verify(pagesListener).scribble$onPageAdded(deleteIndex);
    }

    @Test
    public void testIfRemovesRichPageFromTheListOnExecute() {
        int deleteIndex = 1;
        int size = deleteIndex + 1;
        List<RichText> richPages = mockPageList(size);
        RichText pageToDelete = richPages.get(deleteIndex);

        DeletePageCommand deletePageCommand = new DeletePageCommand(richPages, deleteIndex, mock());

        // Action
        deletePageCommand.execute();

        // Assert
        assertFalse(richPages.contains(pageToDelete));
    }

    @Test
    public void testIfRollbacksRichPagesToOriginState() {
        int deleteIndex = 3;
        int size = deleteIndex + 1;
        List<RichText> richPages = mockPageList(size);
        List<RichText> originRichPages = List.copyOf(richPages);

        DeletePageCommand deletePageCommand = new DeletePageCommand(richPages, deleteIndex, mock());

        // Action
        deletePageCommand.execute();
        deletePageCommand.rollback();

        // Assert
        assertEquals(originRichPages, richPages);
    }

    @Test
    public void testIfRollbacksReturnsFalseIfCommandWasNotExecuted() {
        int deleteIndex = 3;
        int size = deleteIndex + 1;
        DeletePageCommand deletePageCommand = new DeletePageCommand(mockPageList(size), deleteIndex, mock());

        // Action / Assert
        assertFalse(deletePageCommand.rollback());
    }

    @Test
    public void testIfThrowsExceptionIfIndexGreaterThanPagesSize() {
        int size = 4;
        int deleteIndex = size + 1;

        // Action / Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> new DeletePageCommand(mockPageList(size), deleteIndex, mock())
        );
    }

    @Test
    public void testIfThrowsExceptionIfIndexIsNegative() {
        int size = 4;
        int deleteIndex = -1;

        // Action / Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> new DeletePageCommand(mockPageList(size), deleteIndex, mock())
        );
    }

    @Test
    public void testIfThrowsExceptionIfPagesAreEmpty() {
        int size = 0;
        int deleteIndex = 0;

        // Action / Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> new DeletePageCommand(mockPageList(size), deleteIndex, mock())
        );
    }
}
