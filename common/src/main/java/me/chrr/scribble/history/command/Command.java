package me.chrr.scribble.history.command;

import me.chrr.scribble.history.HistoryListener;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface Command {
    void execute(HistoryListener listener);

    void rollback(HistoryListener listener);
}
