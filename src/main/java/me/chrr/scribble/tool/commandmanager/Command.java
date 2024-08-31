package me.chrr.scribble.tool.commandmanager;

/**
 * The interface for operation that can be executed and rolled back.
 */
public interface Command {

    void execute();

    boolean rollback();
}
