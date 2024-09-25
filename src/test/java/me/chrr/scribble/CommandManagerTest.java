package me.chrr.scribble;

import me.chrr.scribble.tool.commandmanager.Command;
import me.chrr.scribble.tool.commandmanager.CommandManager;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class CommandManagerTest {

    private static final int HISTORY_SIZE = 30;

    private static Command newExecutableAndRollableBackCommand(String name) {
        return newCommand(name, true, true);
    }

    private static Command newCommand(String name, boolean isExecutable, boolean isRollableBack) {
        Command command = mock(name);
        when(command.execute()).thenReturn(isExecutable);
        when(command.rollback()).thenReturn(isRollableBack);
        return command;
    }

    @Test
    public void testIfCantUndoIfCommandHistoryIsEmpty() {
        CommandManager commandManager = new CommandManager(HISTORY_SIZE);

        // Action & Assert
        assertFalse(commandManager.hasCommandsToUndo());
    }

    @Test
    public void testIfTryUndoReturnsFalseWhenCommandHistoryIsEmpty() {
        CommandManager commandManager = new CommandManager(HISTORY_SIZE);

        // Action & Assert
        assertFalse(commandManager.tryUndo());
    }

    @Test
    public void testIfUndoReturnsFalseWhenMaxHistorySizeIsZero() {
        // Arrange
        CommandManager commandManager = new CommandManager(0);
        Command command = newExecutableAndRollableBackCommand("");
        commandManager.execute(command);

        // Action & Assert
        assertFalse(commandManager.tryUndo());
    }

    @Test
    public void testIfTryUndoReturnsFalseWhenUnableToRollbackAnyCommandsInHistory() {
        // Arrange
        CommandManager commandManager = new CommandManager(HISTORY_SIZE);

        Command commandWithFailedRollback2 = newCommand("", true, false);
        commandManager.execute(commandWithFailedRollback2);

        Command commandWithFailedRollback1 = newCommand("", true, false);
        commandManager.execute(commandWithFailedRollback1);

        // Action & Assert
        assertFalse(commandManager.tryUndo());
    }

    @Test
    public void testIfTryUndoCallsRollbackForCommand() {
        Command doNothingCommand = newExecutableAndRollableBackCommand("");
        CommandManager commandManager = new CommandManager(HISTORY_SIZE);
        commandManager.execute(doNothingCommand);

        // Action
        commandManager.tryUndo();

        // Assert
        verify(doNothingCommand, times(1)).rollback();
    }

    @Test
    public void testIfTryUndoCallsRollbackForEachCommandInHistoryUntilSuccessRollback() {
        // Arrange
        CommandManager commandManager = new CommandManager(HISTORY_SIZE);

        Command commandThatShouldNotRolledBack = newCommand("neverRolledBack", true, true);
        commandManager.execute(commandThatShouldNotRolledBack);

        Command commandWithSucceedRollback = newCommand("succeedRollback", true, true);
        commandManager.execute(commandWithSucceedRollback);

        Command commandWithFailedRollback2 = newCommand("failedRollback2", true, false);
        commandManager.execute(commandWithFailedRollback2);

        Command commandWithFailedRollback1 = newCommand("failedRollback1", true, false);
        commandManager.execute(commandWithFailedRollback1);

        // Action
        commandManager.tryUndo();

        // Assert
        verify(commandWithFailedRollback1, times(1)).rollback();
        verify(commandWithFailedRollback2, times(1)).rollback();
        verify(commandWithSucceedRollback, times(1)).rollback();
        verify(commandThatShouldNotRolledBack, never()).rollback();
    }

    @Test
    public void testIfTryRedoReturnsFalseWhenCommandHistoryIsEmpty() {
        CommandManager commandManager = new CommandManager(HISTORY_SIZE);

        // Action & Assert
        assertFalse(commandManager.tryRedo());
    }

    @Test
    public void testIfRedoReturnsFalseWhenMaxHistorySizeIsZero() {
        // Arrange
        CommandManager commandManager = new CommandManager(0);
        Command command = newExecutableAndRollableBackCommand("");
        commandManager.execute(command);
        commandManager.tryUndo();

        // Action & Assert
        assertFalse(commandManager.tryRedo());
    }

    @Test
    public void testIfTryUndoReturnsFalseWhenAllCommandsWereUndone() {
        CommandManager commandManager = new CommandManager(HISTORY_SIZE);
        Command doNothingCommand = newExecutableAndRollableBackCommand("");

        // Action
        commandManager.execute(doNothingCommand);
        assertTrue(commandManager.tryUndo(), "Unexpected commandManager state");

        // Assert
        assertFalse(commandManager.tryUndo());

        // command's undo should be called only once - on .tryUndo() call
        verify(doNothingCommand, times(1)).rollback();
    }

    @Test
    public void testIfUndoCalledOnlyForTheLastExecutedCommand() {
        // Arrange
        CommandManager commandManager = new CommandManager(HISTORY_SIZE);

        Command commandToChill = newExecutableAndRollableBackCommand("commandToChill");
        commandManager.execute(commandToChill);

        Command commandToUndo = newExecutableAndRollableBackCommand("commandToUndo");
        commandManager.execute(commandToUndo);

        // Action
        commandManager.tryUndo();

        // Assert
        verify(commandToUndo, times(1)).rollback();
        verify(commandToChill, never()).rollback();
        assertTrue(commandManager.hasCommandsToUndo()); // ensure it's still possible to undo commandToChill
    }

    @Test
    public void testIfTryUndoDoNoCallRollbackForUndoneCommands() {
        CommandManager commandManager = new CommandManager(HISTORY_SIZE);
        Command doNothingCommand = newExecutableAndRollableBackCommand("");
        commandManager.execute(doNothingCommand);
        commandManager.tryUndo();

        // Action
        commandManager.tryUndo();

        // Assert
        // command's undo should be called only once when the first .tryUndo() is called
        verify(doNothingCommand, times(1)).rollback();
    }

    @Test
    public void testIfNothingToUndoWhenMaxHistorySizeIsZero() {
        // Arrange
        CommandManager commandManager = new CommandManager(0);
        Command command = newExecutableAndRollableBackCommand("");
        commandManager.execute(command);

        // Action & Assert
        assertFalse(commandManager.hasCommandsToUndo());
    }

    @Test
    public void testIfTryRedoCallsExecuteForCommand() {
        Command doNothingCommand = newExecutableAndRollableBackCommand("");
        CommandManager commandManager = new CommandManager(HISTORY_SIZE);
        commandManager.execute(doNothingCommand);
        commandManager.tryUndo();

        // Action
        commandManager.tryRedo();

        // Assert
        // Expect two .execute() calls:
        // first when it was added to command manager
        // the second when first .tryRedo() was called
        verify(doNothingCommand, times(2)).execute();
    }

    @Test
    public void testIfTryRedoDoNoCallExecuteForRedoneCommands() {
        CommandManager commandManager = new CommandManager(HISTORY_SIZE);
        Command doNothingCommand = newExecutableAndRollableBackCommand("");
        commandManager.execute(doNothingCommand);
        commandManager.tryUndo();
        commandManager.tryRedo();

        // Action
        commandManager.tryRedo();

        // Assert
        // Expect two .execute() calls:
        // the first when command was added to command manager
        // the second when first .tryRedo() was called
        verify(doNothingCommand, times(2)).execute();
    }

    @Test
    public void testIfCommandIsAddedToCommandHistoryWhenExecutionSuccessful() {
        CommandManager commandManager = new CommandManager(HISTORY_SIZE);
        assertFalse(commandManager.hasCommandsToUndo(), "Unexpected commandManager state");
        Command commandWithSuccessExecute = newCommand("", true, false);

        // Action
        commandManager.execute(commandWithSuccessExecute);

        // Assert
        assertTrue(commandManager.hasCommandsToUndo());
    }

    @Test
    public void testIfCommandIsNotAddedToCommandHistoryWhenExecuteFailed() {
        CommandManager commandManager = new CommandManager(HISTORY_SIZE);
        assertFalse(commandManager.hasCommandsToUndo(), "Unexpected commandManager state");
        Command commandWithFailedExecute = newCommand("failedRollback1", false, false);

        // Action
        commandManager.execute(commandWithFailedExecute);

        // Assert
        assertFalse(commandManager.hasCommandsToUndo());
    }

    @Test
    public void testIfDropsFirstExecutedCommandWhenCommandHistoryOverflowed() {
        // Arrange
        CommandManager commandManager = new CommandManager(3);

        // execute 3 commands to fill up the stack
        Command commandToBeDroppedFirst = newExecutableAndRollableBackCommand("commandToBeDroppedFirst");
        commandManager.execute(commandToBeDroppedFirst);

        Command commandThatWillStay1 = newExecutableAndRollableBackCommand("commandThatWillStay1");
        commandManager.execute(commandThatWillStay1);

        Command commandThatWillStay2 = newExecutableAndRollableBackCommand("commandThatWillStay2");
        commandManager.execute(commandThatWillStay2);


        // Action
        Command commandThatWillReachTheStackLimit = newExecutableAndRollableBackCommand("commandThatWillReachTheStackLimit");
        commandManager.execute(commandThatWillReachTheStackLimit);
        // call undo for all commands in stack
        assertTrue(commandManager.tryUndo());
        assertTrue(commandManager.tryUndo());
        assertTrue(commandManager.tryUndo());


        // Assert
        // ensure command stack is empty
        assertFalse(commandManager.hasCommandsToUndo());

        // undo - should not be called for the first added command
        verify(commandToBeDroppedFirst, never()).rollback();

        // ... and should be called for the reset
        InOrder inOrder = inOrder(commandThatWillStay2, commandThatWillStay1);
        inOrder.verify(commandThatWillStay2, times(1)).rollback();
        inOrder.verify(commandThatWillStay1, times(1)).rollback();
        verify(commandThatWillReachTheStackLimit, times(1)).rollback();
    }

    @Test
    public void testIfNothingToRedoWhenNewCommandExecutedAfterUndo() {
        // Arrange
        CommandManager commandManager = new CommandManager(HISTORY_SIZE);

        Command commandToStay1 = newExecutableAndRollableBackCommand("commandToStay1");
        commandManager.execute(commandToStay1);

        Command commandToStay2 = newExecutableAndRollableBackCommand("commandToStay2");
        commandManager.execute(commandToStay2);

        Command commandToBeRemoved1 = newExecutableAndRollableBackCommand("commandToBeRemoved1");
        commandManager.execute(commandToBeRemoved1);

        Command commandToBeRemoved2 = newExecutableAndRollableBackCommand("commandToBeRemoved2");
        commandManager.execute(commandToBeRemoved2);

        // Action
        commandManager.tryUndo();
        commandManager.tryUndo();

        Command commandToClearRedoCommands = newExecutableAndRollableBackCommand("commandToClearRedoCommands");
        commandManager.execute(commandToClearRedoCommands);


        // Assert
        verify(commandToStay1, never()).rollback();
        verify(commandToStay2, never()).rollback();

        InOrder inOrder = inOrder(commandToBeRemoved2, commandToBeRemoved1);
        inOrder.verify(commandToBeRemoved2).rollback();
        inOrder.verify(commandToBeRemoved1).rollback();

        verify(commandToBeRemoved2, times(1)).rollback();
        verify(commandToBeRemoved1, times(1)).rollback();

        assertFalse(commandManager.hasCommandsToRedo());
    }

    @Test
    public void testIfNothingToRedoWhenMaxHistorySizeIsZero() {
        // Arrange
        CommandManager commandManager = new CommandManager(0);
        Command command = newExecutableAndRollableBackCommand("");
        commandManager.execute(command);
        commandManager.tryUndo();

        // Action & Assert
        assertFalse(commandManager.hasCommandsToRedo());
    }

    @Test
    public void testIfItExecutesCommandWhenMaxHistorySizeIsZero() {
        // Arrange
        CommandManager commandManager = new CommandManager(0);
        Command command = newExecutableAndRollableBackCommand("");

        // Action
        commandManager.execute(command);

        // Assert
        verify(command, times(1)).execute();
    }
}
