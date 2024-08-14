package me.chrr.scribble.book.bookeditscreencommand;

import me.chrr.scribble.tool.commandmanager.Restorable;
import me.chrr.scribble.tool.commandmanager.RestorableCommand;
import net.minecraft.client.util.SelectionManager;

public class BookEditScreenInsertCommand extends RestorableCommand<BookEditScreenMemento> {

    private final SelectionManager selectionManager;
    private final char character;

    public BookEditScreenInsertCommand(
            Restorable<BookEditScreenMemento> bookEditScreenMementoRestorable,
            SelectionManager selectionManager,
            char character
    ) {
        super(bookEditScreenMementoRestorable);
        this.selectionManager = selectionManager;
        this.character = character;
    }

    @Override
    public void execute() {
        super.execute();
        selectionManager.insert(character);
    }
}
