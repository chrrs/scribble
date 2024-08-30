package me.chrr.scribble.model.command;

import me.chrr.scribble.book.RichText;
import me.chrr.scribble.model.memento.RichSelectionManagerMemento;
import me.chrr.scribble.tool.commandmanager.Restorable;
import me.chrr.scribble.tool.commandmanager.MementoCommand;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Deprecated
public class RichSelectionManagerApplyFormattingToSelectionCommand extends MementoCommand<RichSelectionManagerMemento> {

    private final Supplier<RichText> textGetter;
    private final Consumer<RichText> textSetter;

    public final int selectionStart;
    public final int selectionEnd;

    @Nullable
    private final Formatting newColor;
    private final Set<Formatting> addModifiers;
    private final Set<Formatting> removeModifiers;

    public RichSelectionManagerApplyFormattingToSelectionCommand(
            @NotNull Restorable<RichSelectionManagerMemento> restorable,
            Supplier<RichText> textGetter,
            Consumer<RichText> textSetter,
            int selectionStart,
            int selectionEnd,
            @Nullable Formatting newColor,
            Set<Formatting> addModifiers,
            Set<Formatting> removeModifiers
    ) {
        super(restorable);
        this.textGetter = textGetter;
        this.textSetter = textSetter;
        this.selectionStart = selectionStart;
        this.selectionEnd = selectionEnd;
        this.newColor = newColor;
        this.addModifiers = addModifiers;
        this.removeModifiers = removeModifiers;
    }

    @Override
    public void doo() {

        int start = Math.min(this.selectionStart, this.selectionEnd);
        int end = Math.max(this.selectionStart, this.selectionEnd);

        RichText text = this.textGetter.get().applyFormatting(start, end, newColor, addModifiers, removeModifiers);
        this.textSetter.accept(text);
    }
}
