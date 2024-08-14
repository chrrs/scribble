package me.chrr.scribble.tool.commandmanager;

import org.junit.jupiter.api.Test;

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
    public void testIfCreatesMementoOnExecuting() {
        // Arrange
        String mementoState = "STATE";
        Restorable<String> restorableObject = mock();
        when(restorableObject.scribble$createMemento()).thenReturn(mementoState);
        StringRestorableCommand command = new StringRestorableCommand(restorableObject);

        // Action
        command.execute();

        // Verify
        verify(restorableObject, times(1)).scribble$createMemento();
        doReturn(mementoState).when(restorableObject).scribble$createMemento();
    }

    @Test
    public void testIfRestoreStateWithMementoOnUndoCall() {
        // Arrange
        String mementoState = "STATE";
        Restorable<String> restorableObject = mock();
        when(restorableObject.scribble$createMemento()).thenReturn(mementoState);

        StringRestorableCommand command = new StringRestorableCommand(restorableObject);
        // to create internal memento
        command.execute();

        // Action
        command.undo();

        // Verify
        verify(restorableObject, times(1))
                .scribble$restore(argThat(argument -> argument.equals(mementoState)));
    }

    @Test
    public void testIfDoNotRestoreOnUndoIfExecuteWasNotCalled() {
        // Arrange
        String mementoState = "STATE";
        Restorable<String> restorableObject = mock();
        when(restorableObject.scribble$createMemento()).thenReturn(mementoState);

        StringRestorableCommand command = new StringRestorableCommand(restorableObject);

        // Action
        command.undo();

        // Verify
        verify(restorableObject, times(0)).scribble$restore(anyString());
    }

}
