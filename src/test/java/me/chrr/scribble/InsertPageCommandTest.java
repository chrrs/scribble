package me.chrr.scribble;

import me.chrr.scribble.history.command.InsertPageCommand;
import me.chrr.scribble.history.command.PagesListener;
import org.junit.jupiter.api.Test;

import static me.chrr.scribble.mixture.CommonMixture.mockSynchronizedPageList;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class InsertPageCommandTest {

    @Test
    public void testIfPagesListenerOnPageAddedIsCalledAfterExecution() {
        PagesListener pagesListener = mock();
        int insertIndex = 2;
        int size = insertIndex + 2;
        InsertPageCommand insertPageCommand =
                new InsertPageCommand(mockSynchronizedPageList(size), insertIndex, pagesListener);

        // Action
        insertPageCommand.execute();

        // Assert
        verify(pagesListener).scribble$onPageAdded(insertIndex);
    }

    @Test
    public void testIfPagesListenerOnPageRemovedIsCalledAfterRollback() {
        PagesListener pagesListener = mock();
        int insertIndex = 2;
        int size = insertIndex + 2;
        InsertPageCommand insertPageCommand =
                new InsertPageCommand(mockSynchronizedPageList(size), insertIndex, pagesListener);
        insertPageCommand.execute();

        // Action
        insertPageCommand.rollback();

        // Assert
        verify(pagesListener).scribble$onPageRemoved(insertIndex);
    }

    @Test
    public void testIfThrowsExceptionIfIndexGreaterThanPagesSize() {
        int size = 4;
        int insertIndex = size + 1;

        // Action / Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> new InsertPageCommand(mockSynchronizedPageList(size), insertIndex, mock())
        );
    }

    @Test
    public void testIfThrowsExceptionIfIndexIsNegative() {
        int size = 4;
        int insertIndex = -1;

        // Action / Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> new InsertPageCommand(mockSynchronizedPageList(size), insertIndex, mock())
        );
    }

}
