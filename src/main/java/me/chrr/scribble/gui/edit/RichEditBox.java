package me.chrr.scribble.gui.edit;

import me.chrr.scribble.Scribble;
import me.chrr.scribble.book.RichText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.EditBox;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CursorMovement;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class RichEditBox extends EditBox {
    private final Supplier<Pair<Formatting, Set<Formatting>>> formatSupplier;
    private final BiConsumer<@Nullable Formatting, Set<Formatting>> formatListener;

    private RichText richText;

    public RichEditBox(
            TextRenderer textRenderer, int width,
            Supplier<Pair<Formatting, Set<Formatting>>> formatSupplier,
            BiConsumer<@Nullable Formatting, Set<Formatting>> formatListener
    ) {
        super(textRenderer, width);

        this.formatSupplier = formatSupplier;
        this.formatListener = formatListener;
    }

    @Override
    public void setChangeListener(Consumer<String> changeListener) {
        super.setChangeListener((text) -> changeListener.accept(getText()));
    }

    public void sendUpdateFormat() {
        if (this.formatListener != null) {
            Substring selection = this.getSelection();
            Pair<@Nullable Formatting, Set<Formatting>> format = this.richText.getCommonFormat(selection.beginIndex(), selection.endIndex());
            this.formatListener.accept(format.getLeft(), format.getRight());
        }
    }

    public void applyFormatting(Formatting formatting, boolean active) {
        Substring selection = this.getSelection();
        int start = selection.beginIndex();
        int end = selection.endIndex();

        RichText result;
        if (formatting.isModifier()) {
            if (active) {
                result = this.richText.applyFormatting(start, end, null, Set.of(formatting), Set.of());
            } else {
                result = this.richText.applyFormatting(start, end, null, Set.of(), Set.of(formatting));
            }
        } else {
            result = this.richText.applyFormatting(start, end, formatting, Set.of(), Set.of());
        }

        if (!this.exceedsMaxLines(result)) {
            this.richText = result;
            this.text = richText.getPlainText();
            this.onChange();
            this.sendUpdateFormat();
        }
    }

    @Override
    public void setText(String text, boolean bl) {
        String truncated = this.truncateForReplacement(text);
        RichText richText = RichText.fromFormattedString(truncated);

        if (bl || !this.exceedsMaxLines(richText)) {
            this.richText = richText;
            this.text = richText.getPlainText();

            this.cursor = this.text.length();
            this.selectionEnd = this.cursor;

            this.onChange();
            this.sendUpdateFormat();
        }
    }

    @Override
    public String getText() {
        return richText.getAsFormattedString();
    }

    public void setRichTextWithoutUpdating(RichText richText) {
        this.richText = richText;
        this.text = richText.getPlainText();
    }

    @Override
    public void replaceSelection(String string) {
        // We consider the RESET formatting code to be void, as it messes with books.
        string = string.replaceAll(Formatting.RESET.toString(), "");

        if (string.isEmpty() && !this.hasSelection()) {
            return;
        }

        // If the string contains formatting codes, we keep them in. Otherwise,
        // we just type in the current color and modifiers.
        Pair<Formatting, Set<Formatting>> style = this.formatSupplier.get();
        RichText replacement = Formatting.strip(string).equals(string)
                ? new RichText(string, style.getLeft(), style.getRight())
                : RichText.fromFormattedString(string);

        Substring substring = this.getSelection();
        int start = substring.beginIndex();
        int end = substring.endIndex();

        RichText result = this.hasSelection()
                ? this.richText.replace(start, end, replacement)
                : this.richText.insert(start, replacement);

        if (!this.exceedsMaxLines(result)) {
            this.richText = result;
            this.text = richText.getPlainText();

            this.cursor = start + replacement.getLength();
            this.selectionEnd = this.cursor;

            this.onChange();
            this.sendUpdateFormat();
        }
    }

    @Override
    public void moveCursorLine(int offset) {
        if (offset != 0) {
            int cursorX = this.textRenderer.getWidth(this.richText.subText(this.getCurrentLine().beginIndex(), this.cursor)) + 2;

            Substring substring = this.getOffsetLine(offset);
            int col = this.textRenderer.trimToWidth(this.richText.subText(substring.beginIndex(), substring.endIndex()), cursorX).getString().length();
            this.moveCursor(CursorMovement.ABSOLUTE, substring.beginIndex() + col);
        }
    }

    @Override
    public void moveCursor(double x, double y) {
        int cursorX = MathHelper.floor(x);
        int line = MathHelper.floor(y / (double) textRenderer.fontHeight);

        Substring substring = this.lines.get(MathHelper.clamp(line, 0, this.lines.size() - 1));
        int col = this.textRenderer.trimToWidth(this.richText.subText(substring.beginIndex(), substring.endIndex()), cursorX).getString().length();
        this.moveCursor(CursorMovement.ABSOLUTE, substring.beginIndex() + col);
    }

    @Override
    public void moveCursor(CursorMovement movement, int amount) {
        super.moveCursor(movement, amount);
        this.sendUpdateFormat();
    }

    @Override
    public boolean handleSpecialKey(int keyCode) {
        // Override copy/cut/paste to remove formatting codes if the config option is set or SHIFT is held down.
        boolean keepFormatting = Scribble.CONFIG_MANAGER.getConfig().copyFormattingCodes ^ Screen.hasShiftDown();
        boolean ctrlNoAlt = Screen.hasControlDown() && !Screen.hasAltDown();
        if (ctrlNoAlt && (keyCode == GLFW.GLFW_KEY_C || keyCode == GLFW.GLFW_KEY_X)) {
            String text = this.getSelectedText();
            if (!keepFormatting) text = Formatting.strip(text);
            MinecraftClient.getInstance().keyboard.setClipboard(text);
            if (keyCode == GLFW.GLFW_KEY_X) this.replaceSelection("");
            return true;
        } else if (ctrlNoAlt && keyCode == GLFW.GLFW_KEY_V) {
            String text = MinecraftClient.getInstance().keyboard.getClipboard();
            if (!keepFormatting) text = Formatting.strip(text);
            this.replaceSelection(text);
            return true;
        }

        // FIXME: vanilla bug? cursor update callback isn't called on select all.
        if (Screen.isSelectAll(keyCode)) {
            boolean handled = super.handleSpecialKey(keyCode);
            this.sendUpdateFormat();
            return handled;
        }

        return super.handleSpecialKey(keyCode);
    }

    @Override
    protected void rewrap() {
        this.lines.clear();

        if (this.text.isEmpty()) {
            this.lines.add(new Substring(0, 0));
            return;
        }

        MutableInt current = new MutableInt();
        this.textRenderer.getTextHandler().wrapLines(this.richText, this.width, Style.EMPTY, (line, continued) -> {
            String content = line.getString();

            int start = current.getValue();
            int end = start + content.length();

            this.lines.add(new Substring(start, end));

            // If we wrapped on a whitespace character, we need to take that into account.
            if (this.text.length() > end) {
                char c = this.text.charAt(end);
                end += (c == '\n' || c == ' ') ? 1 : 0;
            }

            current.setValue(end);
        });
    }

    @Override
    public String getSelectedText() {
        Substring substring = this.getSelection();
        return this.richText.subText(substring.beginIndex(), substring.endIndex()).getAsFormattedString();
    }

    private boolean exceedsMaxLines(RichText text) {
        return this.hasMaxLines() && this.textRenderer.getTextHandler().wrapLines(text, this.width, Style.EMPTY).size() + (text.getPlainText().endsWith("\n") ? 1 : 0) > this.maxLines;
    }

    public RichText getRichText() {
        return richText;
    }
}
