package me.chrr.scribble.history;

import me.chrr.scribble.history.command.Command;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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

    private final List<Runnable> historyCallbacks = new ArrayList<>();

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
        historyCallbacks.forEach(Runnable::run);
    }

    /**
     * Executes the given command and clears any commands that were previously undone.
     * If the history stack exceeds the maximum size, the oldest command is removed.
     * <p>
     * The command won't be added to the history stack if it executed with result value false
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

        if (commandStack.size() >= maxHistorySize && maxHistorySize > 0) {
            // size limit was reached
            // drop a command from the bottom of the stack
            commandStack.pollFirst();
            // also more the current index by one
            lastExecutedCommandIndex--;
        }

        boolean affectedState = command.execute();
        if (affectedState && maxHistorySize > 0) {
            commandStack.add(command);
            lastExecutedCommandIndex++;
        }

        historyCallbacks.forEach(Runnable::run);
    }

    /**
     * Removes any commands that are above the current execution point in the history stack.
     */
    private void dropAllAboveCurrentIndex() {
        while (lastExecutedCommandIndex < commandStack.size() - 1) {
            commandStack.removeLast();
        }

        historyCallbacks.forEach(Runnable::run);
    }

    /**
     * Attempts to undo the last executed commands,
     * until one of commands undo is successful or has nothing to undo.
     *
     * @return True if the undo operation was successful, false otherwise.
     */
    public boolean tryUndo() {
        if (hasCommandsToUndo()) {
            boolean wasRolledBack = commandStack.get(lastExecutedCommandIndex).rollback();
            lastExecutedCommandIndex--;

            // Keep rolling back until we get to a valid state.
            if (!wasRolledBack) {
                return tryUndo();
            }

            historyCallbacks.forEach(Runnable::run);
            return true;
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
            historyCallbacks.forEach(Runnable::run);
            return true;
        } else {
            return false;
        }
    }

    public boolean hasCommandsToRedo() {
        int lastAvailableIndexInStack = commandStack.size() - 1;
        return lastExecutedCommandIndex < lastAvailableIndexInStack;
    }

    public void onHistoryUpdate(Runnable callback) {
        historyCallbacks.add(callback);
    }
}
