package me.chrr.scribble.book.bookeditscreencommand;

import me.chrr.scribble.book.RichSelectionManager;
import me.chrr.scribble.tool.commandmanager.Restorable;
import me.chrr.scribble.tool.commandmanager.RestorableCommand;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

/**
 * Was replaced with {@link RichSelectionManagerApplyFormattingToSelectionCommand}
 */
@Deprecated
public class BookEditScreenSetColorCommand extends RestorableCommand<BookEditScreenMemento> {

    private final RichSelectionManager selectionManager;

    @Nullable
    private final Formatting color;

    public BookEditScreenSetColorCommand(
            Restorable<BookEditScreenMemento> bookEditScreenMementoRestorable,
            RichSelectionManager selectionManager,
            @Nullable
            Formatting color
    ) {
        super(bookEditScreenMementoRestorable);
        this.selectionManager = selectionManager;
        this.color = color;
    }

    @Override
    public void execute() {
        super.execute();
        selectionManager.setColor(color);
    }
}
