package me.chrr.scribble;

import me.chrr.scribble.book.SynchronizedPageList;
import me.chrr.scribble.book.RichText;
import me.chrr.scribble.history.command.DeletePageCommand;
import me.chrr.scribble.history.command.PagesListener;
import org.junit.jupiter.api.Test;

import java.util.List;

import static me.chrr.scribble.mixture.CommonMixture.mockSynchronizedPageList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DeletePageCommandTest {

    @Test
    public void testIfPagesListenerOnPageRemovedIsCalledAfterExecution() {
        PagesListener pagesListener = mock();
        int deleteIndex = 2;
        int size = deleteIndex + 2;
        DeletePageCommand deletePageCommand =
                new DeletePageCommand(mockSynchronizedPageList(size), deleteIndex, pagesListener);

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
        DeletePageCommand deletePageCommand =
                new DeletePageCommand(mockSynchronizedPageList(size), deleteIndex, pagesListener);

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
        SynchronizedPageList pages = mockSynchronizedPageList(size);
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
        SynchronizedPageList pages = mockSynchronizedPageList(size);
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
        SynchronizedPageList synchronizedPageList = mockSynchronizedPageList(size);
        RichText pageToDelete = synchronizedPageList.get(deleteIndex);

        DeletePageCommand deletePageCommand = new DeletePageCommand(synchronizedPageList, deleteIndex, mock());

        // Action
        deletePageCommand.execute();

        // Assert
        assertFalse(synchronizedPageList.getRichPages().contains(pageToDelete));
    }

    @Test
    public void testIfRollbacksRichPagesToOriginState() {
        int deleteIndex = 3;
        int size = deleteIndex + 1;
        SynchronizedPageList synchronizedPageList = mockSynchronizedPageList(size);
        List<RichText> originRichPages = synchronizedPageList.getRichPages();

        DeletePageCommand deletePageCommand = new DeletePageCommand(synchronizedPageList, deleteIndex, mock());

        // Action
        deletePageCommand.execute();
        deletePageCommand.rollback();

        // Assert
        assertEquals(originRichPages, synchronizedPageList.getRichPages());
    }

    @Test
    public void testIfRollbacksReturnsFalseIfCommandWasNotExecuted() {
        int deleteIndex = 3;
        int size = deleteIndex + 1;
        DeletePageCommand deletePageCommand =
                new DeletePageCommand(mockSynchronizedPageList(size), deleteIndex, mock());

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
                () -> new DeletePageCommand(mockSynchronizedPageList(size), deleteIndex, mock())
        );
    }

    @Test
    public void testIfThrowsExceptionIfIndexIsNegative() {
        int size = 4;
        int deleteIndex = -1;

        // Action / Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> new DeletePageCommand(mockSynchronizedPageList(size), deleteIndex, mock())
        );
    }

    @Test
    public void testIfThrowsExceptionIfPagesAreEmpty() {
        int size = 0;
        int deleteIndex = 0;

        // Action / Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> new DeletePageCommand(mockSynchronizedPageList(size), deleteIndex, mock())
        );
    }
}
