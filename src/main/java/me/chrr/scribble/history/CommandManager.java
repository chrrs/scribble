package me.chrr.scribble.history;

import me.chrr.scribble.ScribbleConfig;
import me.chrr.scribble.history.command.Command;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;

@NullMarked
public class CommandManager {
    private final HistoryListener listener;

    private final List<Command> commands = new ArrayList<>();
    private int index = 0;

    public CommandManager(HistoryListener listener) {
        this.listener = listener;
    }

    public void push(Command command) {
        while (this.index < this.commands.size()) {
            this.commands.removeLast();
        }

        this.commands.add(command);
        this.index += 1;

        if (this.commands.size() > ScribbleConfig.INSTANCE.editHistorySize) {
            this.commands.removeFirst();
            this.index -= 1;
        }
    }

    public boolean canUndo() {
        return this.index > 0;
    }

    public boolean canRedo() {
        return this.index < this.commands.size();
    }

    public void tryUndo() {
        if (!this.canUndo())
            return;

        this.index -= 1;
        Command command = this.commands.get(this.index);
        command.rollback(this.listener);
    }

    public void tryRedo() {
        if (!this.canRedo())
            return;

        Command command = this.commands.get(this.index);
        command.execute(this.listener);
        this.index += 1;
    }

    public void clear() {
        this.commands.clear();
        this.index = 0;
    }
}
