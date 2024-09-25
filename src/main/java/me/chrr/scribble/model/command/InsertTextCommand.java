package me.chrr.scribble.model.command;

import me.chrr.scribble.tool.Restorable;
import me.chrr.scribble.tool.commandmanager.MementoCommand;
import net.minecraft.client.util.SelectionManager;

public class InsertTextCommand<T> extends MementoCommand<T> {

    private final SelectionManager selectionManager;
    private final String text;

    public InsertTextCommand(Restorable<T> mementoRestorable, SelectionManager selectionManager, String text) {
        super(mementoRestorable);
        this.selectionManager = selectionManager;
        this.text = text;
    }

    @Override
    public void doAction() {
        selectionManager.insert(text);
    }
}
