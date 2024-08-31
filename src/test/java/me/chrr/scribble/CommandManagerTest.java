package me.chrr.scribble;

import me.chrr.scribble.tool.commandmanager.Command;
import me.chrr.scribble.tool.commandmanager.CommandManager;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class CommandManagerTest {

    private static final int DEFAULT_HISTORY_SIZE = 30;

    private static Command newMockedCommandWithSuccessRollback(String name) {
        Command command = mock(name);
        when(command.rollback()).thenReturn(true);
        return command;
    }

    @Test
    public void testIfCantUndoIfCommandHistoryIsEmpty() {
        CommandManager commandManager = new CommandManager(DEFAULT_HISTORY_SIZE);

        // Assert
        assertFalse(commandManager.hasCommandsToUndo());
    }

    @Test
    public void testThatCantRedoIfCommandHistoryIsEmpty() {
        CommandManager commandManager = new CommandManager(DEFAULT_HISTORY_SIZE);

        // Assert
        assertFalse(commandManager.hasCommandsToRedo());
    }

    @Test
    public void testIfTryUndoReturnsFalseIfCommandHistoryIsEmpty() {
        CommandManager commandManager = new CommandManager(DEFAULT_HISTORY_SIZE);

        // Assert
        assertFalse(commandManager.tryUndo());
    }

    @Test
    public void testIfTryRedoReturnsFalseIfCommandHistoryIsEmpty() {
        CommandManager commandManager = new CommandManager(DEFAULT_HISTORY_SIZE);

        // Assert
        assertFalse(commandManager.tryRedo());
    }

    @Test
    public void testIfCommandAddedToCommandHistoryWhenExecuted() {
        CommandManager commandManager = new CommandManager(DEFAULT_HISTORY_SIZE);
        assertFalse(commandManager.hasCommandsToUndo(), "Unexpected commandManager state");
        Command doNothingCommand = mock();

        // Action
        commandManager.execute(doNothingCommand);

        // Assert
        assertTrue(commandManager.hasCommandsToUndo());
    }

    @Test
    public void testIfCommandRollbackCalledToOnUndo() {
        Command doNothingCommand = mock();
        CommandManager commandManager = new CommandManager(DEFAULT_HISTORY_SIZE);
        commandManager.execute(doNothingCommand);

        // Action
        commandManager.tryUndo();

        // Assert
        verify(doNothingCommand, times(1)).rollback();
    }

    @Test
    public void testIfCommandExecuteCalledToOnRedo() {
        Command doNothingCommand = newMockedCommandWithSuccessRollback("");
        CommandManager commandManager = new CommandManager(DEFAULT_HISTORY_SIZE);
        commandManager.execute(doNothingCommand);
        commandManager.tryUndo();

        // Action
        commandManager.tryRedo();

        // Assert
        // first when it was added to command manager
        // second - for redo call
        verify(doNothingCommand, times(2)).execute();
    }


    @Test
    public void testIfCantUndoAnymoreWhenAllCommandsWereUndo() {
        CommandManager commandManager = new CommandManager(DEFAULT_HISTORY_SIZE);
        Command doNothingCommand = newMockedCommandWithSuccessRollback("");

        // Action
        commandManager.execute(doNothingCommand);
        assertTrue(commandManager.tryUndo(), "Unexpected commandManager state");


        // Assert
        assertFalse(commandManager.tryUndo());

        // command's undo should be called only once - on .tryUndo() call
        verify(doNothingCommand, times(1)).rollback();
    }

    @Test
    public void testIfDropsFirstExecutedCommandWhenCommandHistoryOverflowed() {
        // Arrange
        CommandManager commandManager = new CommandManager(3);

        // execute 3 commands to fill up the stack
        Command commandToBeDroppedFirst = newMockedCommandWithSuccessRollback("commandToBeDroppedFirst");
        commandManager.execute(commandToBeDroppedFirst);

        Command commandThatWillStay1 = newMockedCommandWithSuccessRollback("commandThatWillStay1");
        commandManager.execute(commandThatWillStay1);

        Command commandThatWillStay2 = newMockedCommandWithSuccessRollback("commandThatWillStay2");
        commandManager.execute(commandThatWillStay2);


        // Action
        Command commandThatWillReachTheStackLimit = newMockedCommandWithSuccessRollback("commandThatWillReachTheStackLimit");
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
    public void testIfUndoCalledOnlyForTheLastExecutedCommand() {
        // Arrange
        CommandManager commandManager = new CommandManager(DEFAULT_HISTORY_SIZE);

        Command commandToChill = mock("commandToChill");
        commandManager.execute(commandToChill);

        Command commandToUndo = mock("commandToUndo");
        commandManager.execute(commandToUndo);

        // Action
        commandManager.tryUndo();

        // Assert
        verify(commandToUndo, times(1)).rollback();
        verify(commandToChill, never()).rollback();
        assertTrue(commandManager.hasCommandsToUndo()); // ensure it's still possible to undo commandToChill
    }

    @Test
    public void testIfThereAreNothingToRedoWhenNewCommandExecutedAfterUndo() {
        // Arrange
        CommandManager commandManager = new CommandManager(DEFAULT_HISTORY_SIZE);

        Command commandToStay1 = newMockedCommandWithSuccessRollback("commandToStay1");
        commandManager.execute(commandToStay1);

        Command commandToStay2 = newMockedCommandWithSuccessRollback("commandToStay2");
        commandManager.execute(commandToStay2);

        Command commandToBeRemoved1 = newMockedCommandWithSuccessRollback("commandToBeRemoved1");
        commandManager.execute(commandToBeRemoved1);

        Command commandToBeRemoved2 = newMockedCommandWithSuccessRollback("commandToBeRemoved2");
        commandManager.execute(commandToBeRemoved2);

        // Action
        commandManager.tryUndo();
        commandManager.tryUndo();

        Command commandToClearRedoCommands = mock("commandToClearRedoCommands");
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
    public void testIfCanExecuteCommandWhenMaxHistorySizeIsZero() {
        // Arrange
        CommandManager commandManager = new CommandManager(0);
        Command command = mock();

        // Action
        commandManager.execute(command);

        // Assert
        verify(command, times(1)).execute();
    }
}
