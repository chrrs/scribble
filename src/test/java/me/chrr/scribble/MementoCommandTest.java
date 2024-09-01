package me.chrr.scribble;

import me.chrr.scribble.tool.Restorable;
import me.chrr.scribble.tool.commandmanager.MementoCommand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class MementoCommandTest {

    private static class StringMementoCommand extends MementoCommand<String> {

        protected StringMementoCommand(Restorable<String> restorable) {
            super(restorable);
        }

        @Override
        public void doAction() {
            // do nothing
        }
    }

    @Test
    public void testIfCreatesMementoOnExecute() {
        // Arrange
        String mementoState = "STATE";
        Restorable<String> restorableObject = mock();
        when(restorableObject.scribble$createMemento()).thenReturn(mementoState);
        StringMementoCommand command = new StringMementoCommand(restorableObject);

        // Action
        command.execute();

        // Verify
        verify(restorableObject, times(1)).scribble$createMemento();
    }

    @Test
    public void testIfRestoreStateWithCreatedMementoCalledOnRollbackWhenExecuteWasCalledBefore() {
        // Arrange
        String mementoState = "STATE";
        Restorable<String> restorableObject = mock();
        when(restorableObject.scribble$createMemento()).thenReturn(mementoState);

        StringMementoCommand command = new StringMementoCommand(restorableObject);
        // to create internal memento to be able to restore
        command.execute();

        // Action
        command.rollback();

        // Verify
        verify(restorableObject, times(1))
                .scribble$restore(argThat(argument -> argument.equals(mementoState)));
    }

    @Test
    public void testIfRestoreStateWithCreatedMementoNotCalledOnRollbackWhenExecuteWasNeverCalledBefore() {
        // Arrange
        String mementoState = "STATE";
        Restorable<String> restorableObject = mock();
        when(restorableObject.scribble$createMemento()).thenReturn(mementoState);

        StringMementoCommand command = new StringMementoCommand(restorableObject);

        // Action
        command.rollback();

        // Verify
        verify(restorableObject, never()).scribble$restore(any());
    }

    @Test
    public void testIfRollbackReturnsTrueWhenExecuteWasCalledBefore() {
        // Arrange
        String mementoState = "STATE";
        Restorable<String> restorableObject = mock();
        when(restorableObject.scribble$createMemento()).thenReturn(mementoState);

        StringMementoCommand command = new StringMementoCommand(restorableObject);
        // to create internal memento to be able to restore
        command.execute();

        // Action
        boolean undoResult = command.rollback();

        // Verify
        assertTrue(undoResult);
    }

    @Test
    public void testIfRollbackReturnsFalseWhenExecuteWasNeverCalledBefore() {
        // Arrange
        String mementoState = "STATE";
        Restorable<String> restorableObject = mock();
        when(restorableObject.scribble$createMemento()).thenReturn(mementoState);

        StringMementoCommand command = new StringMementoCommand(restorableObject);

        // Action
        boolean undoResult = command.rollback();

        // Verify
        assertFalse(undoResult);
    }

    @Test
    public void testIfRestoresOriginalStateOnExecuteCallWhenMementoExist() {
        // Arrange
        String mementoState = "STATE";
        Restorable<String> restorableObject = mock();
        when(restorableObject.scribble$createMemento()).thenReturn(mementoState);

        StringMementoCommand command = new StringMementoCommand(restorableObject);
        command.execute(); // to create memento

        // Action
        command.execute(); // to create memento

        // Verify
        verify(restorableObject, times(1))
                .scribble$restore(argThat(argument -> argument.equals(mementoState)));
    }

    @Test
    public void testIfDoNotRestoresOriginalStateOnExecuteCallWhenMementoDoNotExist() {
        // Arrange
        String mementoState = "STATE";
        Restorable<String> restorableObject = mock();
        when(restorableObject.scribble$createMemento()).thenReturn(mementoState);

        // memento should not exist after creating a command
        StringMementoCommand command = new StringMementoCommand(restorableObject);

        // Action
        command.execute();

        // Verify
        verify(restorableObject, never()).scribble$restore(any());
    }

    @Test
    public void testIfCallsDoOnExecute() {
        // Arrange
        StringMementoCommand command = spy(new StringMementoCommand(mock()));

        // Action
        command.execute();

        // Verify
        verify(command, times(1)).doAction();
    }
}
