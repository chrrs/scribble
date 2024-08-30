package me.chrr.scribble.model.command;

import me.chrr.scribble.book.RichSelectionManager;
import me.chrr.scribble.model.memento.BookEditScreenMemento;
import me.chrr.scribble.tool.commandmanager.MementoCommand;
import me.chrr.scribble.tool.commandmanager.Restorable;
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
    public void doo() {
        activeColorConsumer.accept(color);
        selectionManager.applyColorForSelection(color);
    }
}
