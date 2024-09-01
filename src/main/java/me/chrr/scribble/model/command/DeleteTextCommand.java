package me.chrr.scribble.model.command;

import me.chrr.scribble.book.RichSelectionManager;
import me.chrr.scribble.tool.Restorable;
import me.chrr.scribble.tool.commandmanager.MementoCommand;

public class DeleteTextCommand<T> extends MementoCommand<T> {

    private final RichSelectionManager selectionManager;
    private final int position;

    public DeleteTextCommand(Restorable<T> mementoRestorable, RichSelectionManager selectionManager, int position) {
        super(mementoRestorable);
        this.selectionManager = selectionManager;
        this.position = position;
    }

    @Override
    public void doAction() {
        selectionManager.delete(position);
    }
}
