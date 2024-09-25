package me.chrr.scribble;

import me.chrr.scribble.tool.Restorable;
import me.chrr.scribble.tool.commandmanager.MementoCommand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
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
    public void testIfExecuteCreatesMemento() {
        // Arrange
        String memento = "STATE";
        Restorable<String> restorable = mock();
        when(restorable.scribble$createMemento()).thenReturn(memento);
        StringMementoCommand command = new StringMementoCommand(restorable);

        // Action
        command.execute();

        // Verify
        assertEquals(memento, command.getOriginalMemento());
    }

    @Test
    public void testIfExecuteReturnsTrueWhenTheRestorablesStateWasEffected() {
        // Arrange
        Restorable<String> restorable = mock();
        when(restorable.scribble$createMemento())
                .thenReturn("state1") // the origin restorable state
                .thenReturn("state2"); // pretend that command execution changed the origin state
        StringMementoCommand command = new StringMementoCommand(restorable);

        // Action
        boolean executeResult = command.execute();

        // Verify
        assertTrue(executeResult);
    }

    @Test
    public void testIfExecuteReturnsTrueWhenTheRestorablesStateWasNotEffected() {
        // Arrange
        Restorable<String> restorable = mock();
        when(restorable.scribble$createMemento())
                .thenReturn("sameState") // the origin restorable state
                .thenReturn("sameState"); // the state stays the same
        StringMementoCommand command = new StringMementoCommand(restorable);

        // Action
        boolean executeResult = command.execute();

        // Verify
        assertFalse(executeResult);
    }

    @Test
    public void testIfRestoreCalledOnRollbackWhenExecuteWasCalledBefore() {
        // Arrange
        String memento = "STATE";
        Restorable<String> restorable = mock();
        when(restorable.scribble$createMemento()).thenReturn(memento);

        StringMementoCommand command = new StringMementoCommand(restorable);
        // execute to create internal memento to be able to roll back
        command.execute();

        // Action
        command.rollback();

        // Verify
        verify(restorable, times(1))
                .scribble$restore(argThat(argument -> argument.equals(memento)));
    }

    @Test
    public void testIfRestoreNotCalledOnRollbackWhenExecuteWasNeverCalledBefore() {
        // Arrange
        String memento = "STATE";
        Restorable<String> restorable = mock();
        when(restorable.scribble$createMemento()).thenReturn(memento);

        StringMementoCommand command = new StringMementoCommand(restorable);

        // Action
        command.rollback();

        // Verify
        verify(restorable, never()).scribble$restore(any());
    }

    @Test
    public void testIfRollbackReturnsTrueWhenTheRestorablesStateWasEffected() {
        // Arrange
        Restorable<String> restorable = mock();
        when(restorable.scribble$createMemento())
                .thenReturn("state1") // the origin restorable state
                .thenReturn("state2"); // pretend that command execution changed the origin state
        StringMementoCommand command = new StringMementoCommand(restorable);
        // execute to create internal memento to be able to roll back
        command.execute();

        // Action
        boolean rollbackResult = command.rollback();

        // Verify
        assertTrue(rollbackResult);
    }

    @Test
    public void testIfRollbackReturnsFalseWhenTheRestorablesStateWasNotEffected() {
        // Arrange
        Restorable<String> restorable = mock();
        when(restorable.scribble$createMemento()).thenReturn("state");
        StringMementoCommand command = new StringMementoCommand(restorable);
        // execute to create internal memento to be able to roll back
        command.execute();

        // Action
        boolean rollbackResult = command.rollback();

        // Verify
        assertFalse(rollbackResult);
    }

    @Test
    public void testIfRollbackReturnsFalseWhenExecuteWasNeverCalledBefore() {
        // Arrange
        String mementoState = "STATE";
        Restorable<String> restorableObject = mock();
        when(restorableObject.scribble$createMemento()).thenReturn(mementoState);

        StringMementoCommand command = new StringMementoCommand(restorableObject);

        // Action
        boolean rollbackResult = command.rollback();

        // Verify
        assertFalse(rollbackResult);
    }

    @Test
    public void testIfRestoresOriginalStateOnExecuteCallWhenMementoExist() {
        // Arrange
        String memento = "STATE";
        Restorable<String> restorable = mock();
        when(restorable.scribble$createMemento()).thenReturn(memento);

        StringMementoCommand command = new StringMementoCommand(restorable);
        command.execute(); // to origin create memento

        // Action
        command.execute(); // to restore origin memento before execution

        // Verify
        verify(restorable, times(1))
                .scribble$restore(argThat(argument -> argument.equals(memento)));
    }

    @Test
    public void testIfDoNotRestoresOriginalStateOnExecuteCallWhenMementoDoNotExist() {
        // Arrange
        String memento = "STATE";
        Restorable<String> restorableObject = mock();
        when(restorableObject.scribble$createMemento()).thenReturn(memento);

        // memento should not exist when command was never executed
        StringMementoCommand command = new StringMementoCommand(restorableObject);

        // Action
        command.execute();

        // Verify
        verify(restorableObject, never()).scribble$restore(any());
    }

    @Test
    public void testIfDoActionIsCalledOnExecute() {
        // Arrange
        Restorable<String> restorable = mock();
        when(restorable.scribble$createMemento()).thenReturn("");
        StringMementoCommand command = spy(new StringMementoCommand(restorable));

        // Action
        command.execute();

        // Verify
        verify(command, times(1)).doAction();
    }
}
