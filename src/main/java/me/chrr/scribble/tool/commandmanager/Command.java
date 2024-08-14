package me.chrr.scribble.tool.commandmanager;

public interface Command {

    void execute();

    void undo();
}
