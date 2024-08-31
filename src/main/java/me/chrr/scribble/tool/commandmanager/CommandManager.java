package me.chrr.scribble.tool.commandmanager;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

/**
 * Manages the execution of commands and maintains a history stack to provide undo/redo functionality.
 * Commands are executed, stored in a stack, and can be undone or redone within the limits of the history size.
 */
public class CommandManager {

    private static final int EMPTY_STACK_INDEX = -1;

    private final int maxHistorySize;
    private final LinkedList<Command> commandStack;

    // Index of the last executed command, or -1 if no commands have been executed.
    private int lastExecutedCommandIndex = EMPTY_STACK_INDEX;

    public CommandManager(int maxHistorySize) {
        this.maxHistorySize = maxHistorySize;
        this.commandStack = new LinkedList<>();
    }

    /**
     * Clears the command history and resets the manager's state.
     * All previously executed commands are forgotten.
     */
    public void clear() {
        lastExecutedCommandIndex = EMPTY_STACK_INDEX;
        commandStack.clear();
    }

    /**
     * Executes the given command and clears any commands that were previously undone.
     * If the history stack exceeds the maximum size, the oldest command is removed.
     *
     * @param command The command to execute.
     */
    public void execute(@NotNull Command command) {
        // dropAllAboveCurrentIndex is needed for cases like:
        // execute A - currentIndex at A
        // execute B - currentIndex at B
        // execute C - currentIndex at C
        // undo - moves currentIndex to B
        // undo - moves currentIndex to A
        // execute D - will drop all commands that were above currentIndex(above A)
        dropAllAboveCurrentIndex();

        if (commandStack.size() >= maxHistorySize) {
            // size limit was reached
            // drop a command from the bottom of the stack
            commandStack.pollFirst();
            // also more the current index by one
            lastExecutedCommandIndex--;
        }

        commandStack.add(command);
        command.execute();
        lastExecutedCommandIndex++;
    }

    /**
     * Removes any commands that are above the current execution point in the history stack.
     */
    private void dropAllAboveCurrentIndex() {
        while (lastExecutedCommandIndex < commandStack.size() - 1) {
            commandStack.pollLast();
        }
    }

    /**
     * Attempts to undo the last executed command.
     *
     * @return True if the undo operation was successful, false otherwise.
     */
    public boolean tryUndo() {
        if (hasCommandsToUndo()) {
            boolean wasRolledBack = commandStack.get(lastExecutedCommandIndex).rollback();
            // ToDo what to do when rollback failed? try to rollback previous command?
            if (wasRolledBack) {
                lastExecutedCommandIndex--;
            }
            return wasRolledBack;
        }
        return false;
    }

    /**
     * Checks if an undo operation is possible.
     *
     * @return True if an undo operation is possible, false otherwise.
     */
    public boolean hasCommandsToUndo() {
        if (commandStack.isEmpty()) {
            lastExecutedCommandIndex = EMPTY_STACK_INDEX;
            return false;
        } else {
            return lastExecutedCommandIndex >= 0;
        }
    }

    /**
     * Attempts to redo the last undone command, reapplying its effects.
     *
     * @return True if the redo operation was successful, false otherwise.
     */
    public boolean tryRedo() {
        if (hasCommandsToRedo()) {
            lastExecutedCommandIndex++;
            commandStack.get(lastExecutedCommandIndex).execute();
            return true;
        } else {
            return false;
        }
    }

    public boolean hasCommandsToRedo() {
        int lastAvailableIndexInStack = commandStack.size() - 1;
        return lastExecutedCommandIndex < lastAvailableIndexInStack;
    }
}
