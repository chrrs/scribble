package me.chrr.scribble.history.command;

/**
 * The interface for operation that can be executed and rolled back.
 */
public interface Command {

    /**
     * Executes the main action associated with this command.
     *
     * @return {@code true} if the execution resulted in a change of state; {@code false} otherwise.
     */
    boolean execute();

    /**
     * Reverses any changes made during the execution of this command.
     *
     * @return {@code true} if the rollback was successful; {@code false} otherwise.
     */
    boolean rollback();
}
