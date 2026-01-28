package me.chrr.scribble.gui.edit;

import com.mojang.datafixers.util.Pair;
import me.chrr.scribble.KeyboardUtil;
import me.chrr.scribble.Scribble;
import me.chrr.scribble.book.RichText;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.gui.components.Whence;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

@NullMarked
public class RichMultiLineTextField extends MultilineTextField {
    // These assignments are used, as super() in the constructor calls functions before they are assigned.
    @SuppressWarnings("UnusedAssignment")
    private @Nullable Supplier<Pair<ChatFormatting, Set<ChatFormatting>>> formatSupplier = null;
    @SuppressWarnings("UnusedAssignment")
    private @Nullable BiConsumer<@Nullable ChatFormatting, Set<ChatFormatting>> formatListener = null;

    private RichText richText = RichText.EMPTY;

    public RichMultiLineTextField(
            Font font, int width,
            Supplier<Pair<ChatFormatting, Set<ChatFormatting>>> formatSupplier,
            BiConsumer<@Nullable ChatFormatting, Set<ChatFormatting>> formatListener
    ) {
        super(font, width);

        this.formatSupplier = formatSupplier;
        this.formatListener = formatListener;
    }

    @Override
    public void setValueListener(Consumer<String> valueListener) {
        super.setValueListener((text) -> valueListener.accept(value()));
    }

    public void setRichValueListener(Consumer<RichText> valueListener) {
        super.setValueListener((text) -> valueListener.accept(getRichText()));
    }

    public void sendUpdateFormat() {
        if (this.formatListener != null) {
            StringView selection = this.getSelected();
            Pair<@Nullable ChatFormatting, Set<ChatFormatting>> format = this.richText.getCommonFormat(selection.beginIndex(), selection.endIndex());
            this.formatListener.accept(format.getFirst(), format.getSecond());
        }
    }

    public void applyFormatting(ChatFormatting formatting, boolean active) {
        StringView selection = this.getSelected();
        int start = selection.beginIndex();
        int end = selection.endIndex();

        RichText result;
        if (formatting.isFormat()) {
            if (active) {
                result = this.richText.applyFormatting(start, end, null, Set.of(formatting), Set.of());
            } else {
                result = this.richText.applyFormatting(start, end, null, Set.of(), Set.of(formatting));
            }
        } else {
            result = this.richText.applyFormatting(start, end, formatting, Set.of(), Set.of());
        }

        if (!this.overflowsLineLimit(result)) {
            this.richText = result;
            this.value = richText.getPlainText();
            this.onValueChange();
            this.sendUpdateFormat();
        }
    }

    @Override
    public void setValue(String text, boolean allowOverflow) {
        String truncated = this.truncateFullText(text);
        RichText richText = RichText.fromFormattedString(truncated);
        this.setValue(richText, allowOverflow);
    }

    public void setValue(RichText richText, boolean allowOverflow) {
        if (allowOverflow || !this.overflowsLineLimit(richText)) {
            this.setValueWithoutUpdating(richText);

            this.cursor = this.value.length();
            this.selectCursor = this.cursor;

            this.onValueChange();
            this.sendUpdateFormat();
        }
    }

    public void setValueWithoutUpdating(RichText richText) {
        this.richText = richText;
        this.value = richText.getPlainText();
    }

    public void resetCursor(boolean update) {
        this.cursor = this.value.length();
        this.selectCursor = this.cursor;

        if (update)
            this.sendUpdateFormat();
    }

    @Override
    public void insertText(String string) {
        // We consider the RESET formatting code to be void, as it messes with books.
        string = string.replaceAll(ChatFormatting.RESET.toString(), "");

        if (string.isEmpty() && !this.hasSelection()) {
            return;
        }

        // If the string contains formatting codes, we keep them in. Otherwise,
        // we just type in the current color and modifiers.
        assert this.formatSupplier != null;
        Pair<ChatFormatting, Set<ChatFormatting>> style = this.formatSupplier.get();
        RichText replacement = ChatFormatting.stripFormatting(string).equals(string)
                ? new RichText(string, style.getFirst(), style.getSecond())
                : RichText.fromFormattedString(string);

        StringView substring = this.getSelected();
        int start = substring.beginIndex();
        int end = substring.endIndex();

        RichText result = this.hasSelection()
                ? this.richText.replace(start, end, replacement)
                : this.richText.insert(start, replacement);

        if (!this.overflowsLineLimit(result)) {
            this.richText = result;
            this.value = richText.getPlainText();

            this.cursor = start + replacement.getLength();
            this.selectCursor = this.cursor;

            this.onValueChange();
            this.sendUpdateFormat();
        }
    }

    @Override
    public void seekCursorLine(int offset) {
        if (offset != 0) {
            int cursorX = this.font.width(this.richText.subText(this.getCursorLineView().beginIndex(), this.cursor)) + 2;

            StringView substring = this.getCursorLineView(offset);
            int col = this.font.substrByWidth(this.richText.subText(substring.beginIndex(), substring.endIndex()), cursorX).getString().length();
            this.seekCursor(Whence.ABSOLUTE, substring.beginIndex() + col);
        }
    }

    @Override
    public void seekCursorToPoint(double x, double y) {
        int cursorX = Mth.floor(x);
        int line = Mth.floor(y / (double) font.lineHeight);

        StringView substring = this.displayLines.get(Mth.clamp(line, 0, this.displayLines.size() - 1));
        RichText lineText = this.richText.subText(substring.beginIndex(), substring.endIndex());
        int col = this.font.substrByWidth(lineText, cursorX).getString().length();

        if (col >= substring.endIndex() - 1) {
            this.seekCursor(Whence.ABSOLUTE, substring.beginIndex() + col);
        } else {
            int width = this.font.width(lineText.subText(0, col));
            int overshot = cursorX - width;

            RichText nextChar = lineText.subText(col, col + 1);
            int charWidth = this.font.width(nextChar);
            int offset = (overshot * 2 >= charWidth) ? 1 : 0;
            this.seekCursor(Whence.ABSOLUTE, substring.beginIndex() + col + offset);
        }
    }

    @Override
    public void seekCursor(Whence whence, int amount) {
        super.seekCursor(whence, amount);
        this.sendUpdateFormat();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // Override copy/cut/paste to remove formatting codes if the config option is set or SHIFT is held down.
        boolean keepFormatting = Scribble.CONFIG.copyFormattingCodes.get() ^ event.hasShiftDown();
        boolean ctrlNoAlt = event.hasControlDown() && !event.hasAltDown();
        if (ctrlNoAlt && (KeyboardUtil.isKey(event.key(), "C") || KeyboardUtil.isKey(event.key(), "X"))) {
            String text = this.getSelectedText();
            if (!keepFormatting) text = ChatFormatting.stripFormatting(text);
            Minecraft.getInstance().keyboardHandler.setClipboard(text);
            if (KeyboardUtil.isKey(event.key(), "X")) this.insertText("");
            return true;
        } else if (ctrlNoAlt && KeyboardUtil.isKey(event.key(), "V")) {
            String text = Minecraft.getInstance().keyboardHandler.getClipboard();
            if (!keepFormatting) text = ChatFormatting.stripFormatting(text);
            this.insertText(text);
            return true;
        }

        // FIXME: vanilla bug? cursor update callback isn't called on select all.
        if (event.isSelectAll()) {
            boolean handled = super.keyPressed(event);
            this.sendUpdateFormat();
            return handled;
        }

        return super.keyPressed(event);
    }

    @Override
    protected void reflowDisplayLines() {
        this.displayLines.clear();

        if (this.value.isEmpty()) {
            this.displayLines.add(new StringView(0, 0));
            return;
        }

        MutableInt current = new MutableInt();
        this.font.getSplitter().splitLines(this.richText, this.width, Style.EMPTY, (line, continued) -> {
            String content = line.getString();

            int start = current.get().intValue();
            int end = start + content.length();

            this.displayLines.add(new StringView(start, end));

            // If we wrapped on a whitespace character, we need to take that into account.
            if (this.value.length() > end) {
                char c = this.value.charAt(end);
                end += (c == '\n' || c == ' ') ? 1 : 0;
            }

            current.setValue(end);
        });
    }

    @Override
    public String getSelectedText() {
        StringView substring = this.getSelected();
        return this.richText.subText(substring.beginIndex(), substring.endIndex()).getAsFormattedString();
    }

    public RichText getRichText() {
        return richText;
    }

    @Override
    public String value() {
        return richText.getAsFormattedString();
    }

    private boolean overflowsLineLimit(RichText text) {
        return this.hasLineLimit() && this.font.getSplitter().splitLines(text, this.width, Style.EMPTY).size() > this.lineLimit;
    }
}
