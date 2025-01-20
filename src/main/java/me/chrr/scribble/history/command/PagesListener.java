package me.chrr.scribble.history.command;

public interface PagesListener {

    void scribble$onPageAdded(int index);

    void scribble$onPageRemoved(int index);
}
