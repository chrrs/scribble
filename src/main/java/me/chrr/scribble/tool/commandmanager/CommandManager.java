package me.chrr.scribble.tool.commandmanager;

import org.jetbrains.annotations.NotNull;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Objects;

/**
 * Manages a stack of commands and provides undo/redo functionality.
 *
 * <p>This class maintains a stack of commands and tracks the current index.
 * It allows executing commands, undoing the last executed command, and checking if an undo operation is possible.</p>
 * <p>
 * ToDo add redo logic
 */
public class CommandManager {

    private static final int DEFAULT_HISTORY_SIZE = 30;
    private static final int EMPTY_STACK_INDEX = -1;

    private final int maxHistorySize;
    private final Deque<Command> commandStack;

    private int currentIndex = EMPTY_STACK_INDEX;

    public CommandManager(int maxHistorySize) {
        this.maxHistorySize = maxHistorySize;
        this.commandStack = new LinkedList<>();
    }

    public CommandManager() {
        this(DEFAULT_HISTORY_SIZE);
    }

    public void clear() {
        currentIndex = EMPTY_STACK_INDEX;
        commandStack.clear();
    }

    /**
     * Executes the given command and clears any subsequent commands in the stack.
     *
     * @param command The command to execute.
     */
    public void execute(@NotNull Command command) {
        dropAllAboveCurrentIndex();

        if (commandStack.size() >= maxHistorySize) {
            // size limit was reached
            // drop a command from the bottom of the stack
            commandStack.pollLast();
        }

        commandStack.push(command);
        command.execute();
        currentIndex++;
    }

    private void dropAllAboveCurrentIndex() {
        // pops all commands that above current index

        // For cases like:
        // execute A - currentIndex at A
        // execute B - currentIndex at B
        // execute C - currentIndex at C
        // undo - moves currentIndex to B
        // undo - moves currentIndex to A
        // execute D - will drop all commands that were above currentIndex(above A)
        while (currentIndex < commandStack.size() - 1) {
            commandStack.pollFirst();
        }
    }

    /**
     * Attempts to undo the last executed command.
     *
     * @return True if the undo operation was successful, false otherwise.
     */
    public boolean tryUndo() {
        if (canUndo()) {
            Objects.requireNonNull(commandStack.pollFirst(), "Check the canUndo() logic.").undo();
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
        if (commandStack.isEmpty()) {
            currentIndex = EMPTY_STACK_INDEX;
            return false;
        } else {
            return currentIndex >= 0;
        }
    }
}
