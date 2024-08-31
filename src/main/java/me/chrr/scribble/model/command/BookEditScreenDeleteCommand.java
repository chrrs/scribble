package me.chrr.scribble.model.command;

import me.chrr.scribble.book.RichSelectionManager;
import me.chrr.scribble.model.BookEditScreenMemento;
import me.chrr.scribble.tool.commandmanager.MementoCommand;
import me.chrr.scribble.tool.Restorable;
import net.minecraft.client.util.SelectionManager;

public class BookEditScreenDeleteCommand extends MementoCommand<BookEditScreenMemento> {

    private final RichSelectionManager selectionManager;
    private final SelectionManager.SelectionType selectionType;

    public BookEditScreenDeleteCommand(
            Restorable<BookEditScreenMemento> bookEditScreenMementoRestorable,
            RichSelectionManager selectionManager,
            SelectionManager.SelectionType selectionType
    ) {
        super(bookEditScreenMementoRestorable);
        this.selectionManager = selectionManager;
        this.selectionType = selectionType;
    }

    @Override
    public void doAction() {
        selectionManager.delete(-1, selectionType);
    }
}
