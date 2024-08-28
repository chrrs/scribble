package me.chrr.scribble.model.command;

import me.chrr.scribble.model.memento.BookEditScreenMemento;
import me.chrr.scribble.tool.commandmanager.Restorable;
import me.chrr.scribble.tool.commandmanager.MementoCommand;
import net.minecraft.client.util.SelectionManager;

public class BookEditScreenInsertCommand extends MementoCommand<BookEditScreenMemento> {

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
    public void doo() {
        selectionManager.insert(character);
    }
}
