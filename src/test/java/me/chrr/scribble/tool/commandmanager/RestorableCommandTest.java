package me.chrr.scribble.tool.commandmanager;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class RestorableCommandTest {

    private static class StringRestorableCommand extends RestorableCommand<String> {

        protected StringRestorableCommand(Restorable<String> restorable) {
            super(restorable);
        }

        @Override
        public void execute() {
            super.execute();
        }
    }

    @Test
    public void testIfCreatesMementoOnExecute() {
        // Arrange
        String mementoState = "STATE";
        Restorable<String> restorableObject = mock();
        when(restorableObject.scribble$createMemento()).thenReturn(mementoState);
        StringRestorableCommand command = new StringRestorableCommand(restorableObject);

        // Action
        command.execute();

        // Verify
        verify(restorableObject, times(1)).scribble$createMemento();
    }

    @Test
    public void testIfRestoreStateWithCreatedMementoCalledOnUndoWhenExecuteWasCalledBefore() {
        // Arrange
        String mementoState = "STATE";
        Restorable<String> restorableObject = mock();
        when(restorableObject.scribble$createMemento()).thenReturn(mementoState);

        StringRestorableCommand command = new StringRestorableCommand(restorableObject);
        // to create internal memento to be able to restore
        command.execute();

        // Action
        command.undo();

        // Verify
        verify(restorableObject, times(1))
                .scribble$restore(argThat(argument -> argument.equals(mementoState)));
    }

    @Test
    public void testIfRestoreStateWithCreatedMementoNotCalledOnUndoWhenExecuteWasNeverCalledBefore() {
        // Arrange
        String mementoState = "STATE";
        Restorable<String> restorableObject = mock();
        when(restorableObject.scribble$createMemento()).thenReturn(mementoState);

        StringRestorableCommand command = new StringRestorableCommand(restorableObject);

        // Action
        command.undo();

        // Verify
        verify(restorableObject, never());
    }

    @Test
    public void testIfUndoReturnsTrueWhenExecuteWasCalledBefore() {
        // Arrange
        String mementoState = "STATE";
        Restorable<String> restorableObject = mock();
        when(restorableObject.scribble$createMemento()).thenReturn(mementoState);

        StringRestorableCommand command = new StringRestorableCommand(restorableObject);
        // to create internal memento to be able to restore
        command.execute();

        // Action
        boolean undoResult = command.undo();

        // Verify
        assertTrue(undoResult);
    }

    @Test
    public void testIfUndoReturnsFalseWhenExecuteWasNeverCalledBefore() {
        // Arrange
        String mementoState = "STATE";
        Restorable<String> restorableObject = mock();
        when(restorableObject.scribble$createMemento()).thenReturn(mementoState);

        StringRestorableCommand command = new StringRestorableCommand(restorableObject);

        // Action
        boolean undoResult = command.undo();

        // Verify
        assertFalse(undoResult);
    }

}
