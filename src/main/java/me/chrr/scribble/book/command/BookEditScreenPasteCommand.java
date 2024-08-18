package me.chrr.scribble.book.command;

import me.chrr.scribble.tool.commandmanager.RestorableCommand;
import me.chrr.scribble.tool.commandmanager.Restorable;
import net.minecraft.client.util.SelectionManager;

public class BookEditScreenPasteCommand extends RestorableCommand<BookEditScreenMemento> {

    private final SelectionManager selectionManager;

    public BookEditScreenPasteCommand(
            Restorable<BookEditScreenMemento> bookEditScreenMementoRestorable,
            SelectionManager selectionManager
    ) {
        super(bookEditScreenMementoRestorable);
        this.selectionManager = selectionManager;
    }


    @Override
    public void doo() {
        selectionManager.paste();
    }
}