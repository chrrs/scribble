package me.chrr.scribble.tool.commandmanager;

import java.util.Deque;
import java.util.LinkedList;

/**
 * Manages a stack of commands and provides undo/redo functionality.
 *
 * <p>This class maintains a stack of commands and tracks the current index.
 * It allows executing commands, undoing the last executed command, and checking if an undo operation is possible.</p>
 */
public class CommandManager {

    private final Deque<Command> commandStack;
    private int currentIndex = -1;

    public CommandManager() {
        commandStack = new LinkedList<>();
    }

    /**
     * Executes the given command and clears any subsequent commands in the stack.
     *
     * @param command The command to execute.
     */
    public void execute(Command command) {
        while (currentIndex < commandStack.size() - 1) {
            commandStack.pop();
        }

        commandStack.push(command);
        command.execute();
        currentIndex++;
    }

    /**
     * Attempts to undo the last executed command.
     *
     * @return True if the undo operation was successful, false otherwise.
     */
    public boolean tryUndo() {
        if (canUndo()) {
            commandStack.pop().undo();
            currentIndex--;
            return true;
        }
        return false;
    }

    /**
     * Checks if an undo operation is possible.
     *
     * @return True if an undo operation is possible, false otherwise.
     */
    public boolean canUndo() {
        return currentIndex >= 0;
    }
}
