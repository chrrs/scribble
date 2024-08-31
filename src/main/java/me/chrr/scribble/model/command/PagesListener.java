package me.chrr.scribble.model.command;

public interface PagesListener {

    void scribble$onPageAdded(int index);

    void scribble$onPageRemoved(int index);
}
