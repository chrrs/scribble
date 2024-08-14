package me.chrr.scribble.tool.commandmanager;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class CommandManagerTest {

    private static class DoNothingCommand implements Command {

        @Override
        public void execute() {
        }

        @Override
        public boolean undo() {
            return false;
        }
    }

    @Test
    public void testThatCantUndoIfCommandHistoryIsEmpty() {
        CommandManager commandManager = new CommandManager();

        // Assert
        assertFalse(commandManager.canUndo());
    }

    @Test
    public void testIfTryUndoReturnsFalseIfCommandHistoryIsEmpty() {
        CommandManager commandManager = new CommandManager();

        // Assert
        assertFalse(commandManager.tryUndo());
    }

    @Test
    public void testIfPutsCommandToCommandHistoryWhenExecuted() {
        CommandManager commandManager = new CommandManager();
        assertFalse(commandManager.canUndo(), "Unexpected commandManager state");
        DoNothingCommand doNothingCommand = mock();

        // Action
        commandManager.execute(doNothingCommand);

        // Assert
        assertTrue(commandManager.canUndo());
    }

    @Test
    public void testIfCantUndoAnymoreWhenAllCommandsWereUndo() {
        CommandManager commandManager = new CommandManager();
        DoNothingCommand doNothingCommand = mock();


        // Action
        commandManager.execute(doNothingCommand);
        assertTrue(commandManager.tryUndo(), "Unexpected commandManager state");


        // Assert
        assertFalse(commandManager.tryUndo());

        // command's undo should be called only once - on .tryUndo() call
        verify(doNothingCommand, times(1)).undo();
    }

    @Test
    public void testIfDropsFirstExecutedCommandWhenCommandHistorySizeReached() {
        // Arrange
        CommandManager commandManager = new CommandManager(3);

        // execute 3 commands to fill up the stack
        DoNothingCommand commandToBeDroppedFirst = mock();
        commandManager.execute(commandToBeDroppedFirst);

        DoNothingCommand commandThatWillStay1 = mock();
        commandManager.execute(commandThatWillStay1);

        DoNothingCommand commandThatWillStay2 = mock();
        commandManager.execute(commandThatWillStay2);


        // Action
        DoNothingCommand commandThatWillReachTheStackLimit = mock();
        commandManager.execute(commandThatWillReachTheStackLimit);
        // call undo for all commands in stack
        assertTrue(commandManager.tryUndo());
        assertTrue(commandManager.tryUndo());
        assertTrue(commandManager.tryUndo());


        // Assert
        // ensure command stack is empty
        assertFalse(commandManager.canUndo());

        // undo - should not be called for the first added command
        verify(commandToBeDroppedFirst, never()).undo();

        // ... and should be called for the reset
        verify(commandThatWillStay1, times(1)).undo();
        verify(commandThatWillStay2, times(1)).undo();
        verify(commandThatWillReachTheStackLimit, times(1)).undo();
    }
}
