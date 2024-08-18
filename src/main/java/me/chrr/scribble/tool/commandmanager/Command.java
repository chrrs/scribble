package me.chrr.scribble.tool.commandmanager;

/**
 * The interface for operation that can be executed and undone.
 */
public interface Command {

    void execute();

    boolean undo();
}
