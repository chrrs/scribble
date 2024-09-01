package me.chrr.scribble.model.command;

import me.chrr.scribble.tool.Restorable;
import me.chrr.scribble.tool.commandmanager.MementoCommand;

public class ActionCommand<T> extends MementoCommand<T> {

    private final Action action;

    public ActionCommand(Restorable<T> mementoRestorable, Action action) {
        super(mementoRestorable);
        this.action = action;
    }

    @Override
    public void doAction() {
        action.perform();
    }

    public interface Action {
        void perform();
    }
}
