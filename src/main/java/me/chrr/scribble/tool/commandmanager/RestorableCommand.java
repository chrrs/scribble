package me.chrr.scribble.tool.commandmanager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Class for commands that support undo/redo functionality.
 *
 * <p>This class encapsulates the logic for creating a memento before command execution and restoring
 * the state from the memento when the command is undone.</p>
 *
 * @param <T> The type of the memento used to capture the state of the restorable object.
 */
public abstract class RestorableCommand<T> implements Command {

    @NotNull
    private final Restorable<T> restorable;

    @Nullable
    private T memento;

    /**
     * Constructs a new RestorableCommand instance.
     *
     * @param restorable The restorable object to be managed by this command.
     */
    protected RestorableCommand(@NotNull Restorable<T> restorable) {
        this.restorable = restorable;
    }

    /**
     * Executes the command, creating a memento of the current state.
     */
    @Override
    public void execute() {
        this.memento = restorable.scribble$createMemento();
    }

    /**
     * Undoes the command by restoring the state from the previously created memento.
     * <p>
     * Do nothing if nothing to undo.
     */
    @Override
    public boolean undo() {
        if (memento != null) {
            restorable.scribble$restore(memento);
            return true;
        } else {
            return false;
        }
    }
}

