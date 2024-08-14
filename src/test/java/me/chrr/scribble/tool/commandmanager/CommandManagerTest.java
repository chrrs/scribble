package me.chrr.scribble.tool.commandmanager;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    public void testIfCantUndoIfCommandHistoryIsEmpty() {
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
    public void testIfPutsEveryCommandToCommandStack() {
        CommandManager commandManager = new CommandManager();
        assertFalse(commandManager.canUndo(), "Unexpected commandManager state");

        // Action
        commandManager.execute(new DoNothingCommand());

        // Assert
        assertTrue(commandManager.canUndo());
    }

    @Test
    public void testIfCantUndoWhenAllCommandsWereUndo() {
        CommandManager commandManager = new CommandManager();

        // Action
        commandManager.execute(new DoNothingCommand());
        assertTrue(commandManager.tryUndo(), "Unexpected commandManager state");

        // Assert
        assertFalse(commandManager.tryUndo());
    }
}
