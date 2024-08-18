package me.chrr.scribble.book.command;

import me.chrr.scribble.book.RichSelectionManager;
import me.chrr.scribble.tool.commandmanager.Restorable;
import me.chrr.scribble.tool.commandmanager.RestorableCommand;

public class BookEditScreenPasteCommand extends RestorableCommand<BookEditScreenMemento> {

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
    public void doo() {
        if (ignoreFormatting) {
            selectionManager.pasteWithoutFormatting();
        } else {
            selectionManager.paste();
        }
    }
}
