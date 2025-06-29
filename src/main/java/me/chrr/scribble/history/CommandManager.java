package me.chrr.scribble.history;

import me.chrr.scribble.Scribble;
import me.chrr.scribble.history.command.Command;

import java.util.ArrayList;
import java.util.List;

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

        if (this.commands.size() > Scribble.CONFIG_MANAGER.getConfig().editHistorySize) {
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
}
