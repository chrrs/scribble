package me.chrr.scribble.book.bookeditscreencommand;

import me.chrr.scribble.book.RichSelectionManager;
import me.chrr.scribble.tool.commandmanager.Restorable;
import me.chrr.scribble.tool.commandmanager.RestorableCommand;
import net.minecraft.util.Formatting;

/**
 * Was replaced with {@link RichSelectionManagerApplyFormattingToSelectionCommand}
 */
@Deprecated
public class BookEditScreenToggleModifierCommand extends RestorableCommand<BookEditScreenMemento> {

    private final RichSelectionManager selectionManager;
    private final Formatting modifier;
    private final boolean toggled;

    public BookEditScreenToggleModifierCommand(
            Restorable<BookEditScreenMemento> bookEditScreenMementoRestorable,
            RichSelectionManager selectionManager,
            Formatting modifier,
            boolean toggled
    ) {
        super(bookEditScreenMementoRestorable);
        this.selectionManager = selectionManager;
        this.modifier = modifier;
        this.toggled = toggled;
    }

    @Override
    public void execute() {
        super.execute();
        selectionManager.toggleModifier(modifier, toggled);
    }
}
