package me.chrr.scribble.model.command;

import me.chrr.scribble.tool.Restorable;
import me.chrr.scribble.tool.commandmanager.MementoCommand;
import net.minecraft.client.util.SelectionManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DeleteTextCommand<T> extends MementoCommand<T> {

    private final SelectionManager selectionManager;
    private final int offset;

    @Nullable
    private final SelectionManager.SelectionType selectionType;

    public DeleteTextCommand(
            Restorable<T> mementoRestorable,
            SelectionManager selectionManager,
            int offset,
            @Nullable SelectionManager.SelectionType selectionType
    ) {
        super(mementoRestorable);
        this.selectionManager = selectionManager;
        this.offset = offset;
        this.selectionType = selectionType;
    }

    public DeleteTextCommand(@NotNull Restorable<T> restorable, SelectionManager selectionManager) {
        this(restorable, selectionManager, 0, null);
    }

    @Override
    public void doAction() {
        if (selectionType != null) {
            selectionManager.delete(offset, selectionType);
        } else {
            selectionManager.delete(offset);
        }
    }
}
