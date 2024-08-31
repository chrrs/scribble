package me.chrr.scribble.model.command;

import me.chrr.scribble.book.RichSelectionManager;
import me.chrr.scribble.model.BookEditScreenMemento;
import me.chrr.scribble.tool.Restorable;
import me.chrr.scribble.tool.commandmanager.MementoCommand;
import net.minecraft.util.Formatting;

import java.util.function.Consumer;

public class BookEditScreenChangeColorForSelectionCommand extends MementoCommand<BookEditScreenMemento> {

    private final RichSelectionManager selectionManager;
    private final Formatting color;
    private final Consumer<Formatting> activeColorConsumer;

    public BookEditScreenChangeColorForSelectionCommand(
            Restorable<BookEditScreenMemento> bookEditScreenMementoRestorable,
            RichSelectionManager selectionManager,
            Formatting color,
            Consumer<Formatting> activeColorConsumer
    ) {
        super(bookEditScreenMementoRestorable);
        this.selectionManager = selectionManager;
        this.color = color;
        this.activeColorConsumer = activeColorConsumer;
    }

    @Override
    public void doAction() {
        activeColorConsumer.accept(color);
        selectionManager.applyColorForSelection(color);
    }
}
