package me.chrr.scribble;

import me.chrr.scribble.book.RichText;
import me.chrr.scribble.history.command.InsertPageCommand;
import me.chrr.scribble.history.command.PagesListener;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class InsertPageCommandTest {

    private static List<RichText> mockPageList(int size) {
        ArrayList<RichText> list = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            RichText item = RichText.fromFormattedString("page content:" + i);
            list.add(item);
        }

        return list;
    }

    @Test
    public void testIfPagesListenerOnPageAddedIsCalledAfterExecution() {
        PagesListener pagesListener = mock();
        int insertIndex = 2;
        int size = insertIndex + 2;
        InsertPageCommand insertPageCommand = new InsertPageCommand(mockPageList(size), insertIndex, pagesListener);

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
        InsertPageCommand insertPageCommand = new InsertPageCommand(mockPageList(size), insertIndex, pagesListener);
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
                () -> new InsertPageCommand(mockPageList(size), insertIndex, mock())
        );
    }

    @Test
    public void testIfThrowsExceptionIfIndexIsNegative() {
        int size = 4;
        int insertIndex = -1;

        // Action / Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> new InsertPageCommand(mockPageList(size), insertIndex, mock())
        );
    }

}
