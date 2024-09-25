package me.chrr.scribble.tool.commandmanager;

import me.chrr.scribble.tool.Restorable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Class for commands that implements undo/redo functionality using memento state.
 *
 * <p>This class encapsulates the logic for creating a memento before command execution
 * and restoring the state from the memento when the command is undone.</p>
 *
 * @param <T> The type of the memento used to capture the state of the {@link Restorable} object.
 */
public abstract class MementoCommand<T> implements Command {

    @NotNull
    private final Restorable<T> restorable;

    @Nullable
    private T originalMemento;

    protected MementoCommand(@NotNull Restorable<T> restorable) {
        this.restorable = restorable;
    }

    @Nullable
    public T getOriginalMemento() {
        return originalMemento;
    }

    /**
     * Executes the command.
     * <p>
     * If the command is being executed for the first time, it creates and stores a memento of the original state.
     * <p>
     * If the command is being executed again (e.g., as part of a redo operation),
     * it restores the state from the previously saved memento to ensure
     * that the {@link #restorable} is in the exact state it was before the original execution.
     */
    @Override
    public boolean execute() {
        if (originalMemento == null) {
            // Create a memento of the current state if this is the first execution
            originalMemento = restorable.scribble$createMemento();
        } else {
            // Restore the first/original state from the saved memento for rollback operations
            restorable.scribble$restore(originalMemento);
        }
        doAction();

        // check if action affected on restartable state
        return !restorable.scribble$createMemento().equals(originalMemento);
    }

    /**
     * Does the command action.
     */
    public abstract void doAction();

    /**
     * Reverts the command by restoring the state from the origin memento.
     * <p>
     * If there is no memento to restore from, this method has no effect and returns {@code false}.
     * </p>
     *
     * @return {@code true} if the state was successfully reverted and differs from the current state,
     * {@code false} if there was no memento or the state was unchanged.
     */
    @Override
    public boolean rollback() {
        if (originalMemento != null) {
            T currentState = restorable.scribble$createMemento();
            restorable.scribble$restore(originalMemento);

            // check if action affected on restartable state
            return !originalMemento.equals(currentState);
        } else {
            return false;
        }
    }
}

