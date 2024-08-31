package me.chrr.scribble.model.command;

import me.chrr.scribble.book.RichSelectionManager;
import me.chrr.scribble.model.BookEditScreenMemento;
import me.chrr.scribble.tool.Restorable;
import me.chrr.scribble.tool.commandmanager.MementoCommand;

public class BookEditScreenPasteCommand extends MementoCommand<BookEditScreenMemento> {

    private final RichSelectionManager selectionManager;
    private final boolean ignoreFormatting;

    public BookEditScreenPasteCommand(
            Restorable<BookEditScreenMemento> bookEditScreenMementoRestorable,
            RichSelectionManager selectionManager,
            boolean ignoreFormatting
    ) {
        super(bookEditScreenMementoRestorable);
        this.selectionManager = selectionManager;
        this.ignoreFormatting = ignoreFormatting;
    }

    @Override
    public void doAction() {
        if (ignoreFormatting) {
            selectionManager.pasteWithoutFormatting();
        } else {
            selectionManager.paste();
        }
    }
}
