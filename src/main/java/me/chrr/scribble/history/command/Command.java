package me.chrr.scribble.history.command;

import me.chrr.scribble.history.HistoryListener;

public interface Command {
    void execute(HistoryListener listener);

    void rollback(HistoryListener listener);
}
