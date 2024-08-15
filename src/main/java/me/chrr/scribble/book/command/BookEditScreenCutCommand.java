package me.chrr.scribble.book.command;

import me.chrr.scribble.tool.commandmanager.Restorable;
import me.chrr.scribble.tool.commandmanager.RestorableCommand;
import net.minecraft.client.util.SelectionManager;

public class BookEditScreenCutCommand extends RestorableCommand<BookEditScreenMemento> {

    private final SelectionManager selectionManager;

    public BookEditScreenCutCommand(
            Restorable<BookEditScreenMemento> bookEditScreenMementoRestorable,
            SelectionManager selectionManager
    ) {
        super(bookEditScreenMementoRestorable);
        this.selectionManager = selectionManager;
    }

    @Override
    public void doo() {
        selectionManager.cut();
    }
}
