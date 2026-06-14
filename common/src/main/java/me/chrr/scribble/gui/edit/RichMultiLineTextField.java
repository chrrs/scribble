package me.chrr.scribble.gui.edit;

import me.chrr.scribble.Scribble;
import me.chrr.scribble.text.StyledText;
import me.chrr.scribble.util.KeyboardUtil;
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

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@NullMarked
public class RichMultiLineTextField extends MultilineTextField {
    // These assignments are used, as super() in the constructor calls functions before they are assigned.
    @SuppressWarnings("UnusedAssignment")
    private @Nullable Supplier<Style> styleSupplier = null;
    @SuppressWarnings("UnusedAssignment")
    private @Nullable Consumer<Style> styleListener = null;

    private StyledText styledText = StyledText.EMPTY;

    public RichMultiLineTextField(Font font, int width, Supplier<Style> styleSupplier, Consumer<Style> styleListener) {
        super(font, width);

        this.styleSupplier = styleSupplier;
        this.styleListener = styleListener;
    }

    @Override
    public void setValueListener(Consumer<String> valueListener) {
        super.setValueListener((_) -> valueListener.accept(value()));
    }

    public void setRichValueListener(Consumer<StyledText> valueListener) {
        super.setValueListener((_) -> valueListener.accept(getRichText()));
    }

    public void sendUpdateFormat() {
        if (this.styleListener != null) {
            StringView selection = this.getSelected();
            this.styleListener.accept(this.styledText.getCommonStyle(selection.beginIndex(), selection.endIndex()));
        }
    }

    public void applySelection(Function<Style, Style> modifier) {
        StringView selection = this.getSelected();
        int start = selection.beginIndex();
        int end = selection.endIndex();

        StyledText result = this.styledText.apply(start, end, modifier);

        if (!this.overflowsLineLimit(result)) {
            this.styledText = result;
            this.value = styledText.getPlainText();
            this.onValueChange();
            this.sendUpdateFormat();
        }
    }

    @Override
    public void setValue(String text, boolean allowOverflow) {
        String truncated = this.truncateFullText(text);
        StyledText styledText = StyledText.fromFormattedString(truncated);
        this.setValue(styledText, allowOverflow);
    }

    public void setValue(StyledText styledText, boolean allowOverflow) {
        if (allowOverflow || !this.overflowsLineLimit(styledText)) {
            this.setValueWithoutUpdating(styledText);

            this.cursor = this.value.length();
            this.selectCursor = this.cursor;

            this.onValueChange();
            this.sendUpdateFormat();
        }
    }

    public void setValueWithoutUpdating(StyledText styledText) {
        this.styledText = styledText;
        this.value = styledText.getPlainText();
    }

    public void resetCursor(boolean update) {
        if (this.cursor == this.value.length() && this.selectCursor == this.cursor)
            return;

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
        assert this.styleSupplier != null;
        StyledText replacement = ChatFormatting.stripFormatting(string).equals(string)
                ? new StyledText(string, this.styleSupplier.get())
                : StyledText.fromFormattedString(string);

        StringView substring = this.getSelected();
        int start = substring.beginIndex();
        int end = substring.endIndex();

        StyledText result = this.hasSelection()
                ? this.styledText.replace(start, end, replacement)
                : this.styledText.insert(start, replacement);

        if (!this.overflowsLineLimit(result)) {
            this.styledText = result;
            this.value = styledText.getPlainText();

            this.cursor = start + replacement.getLength();
            this.selectCursor = this.cursor;

            this.onValueChange();
            this.sendUpdateFormat();
        }
    }

    @Override
    public void seekCursorLine(int offset) {
        if (offset != 0) {
            int cursorX = this.font.width(this.styledText.subText(this.getCursorLineView().beginIndex(), this.cursor)) + 2;

            StringView substring = this.getCursorLineView(offset);
            int col = this.font.substrByWidth(this.styledText.subText(substring.beginIndex(), substring.endIndex()), cursorX).getString().length();
            this.seekCursor(Whence.ABSOLUTE, substring.beginIndex() + col);
        }
    }

    @Override
    public void seekCursorToPoint(double x, double y) {
        int cursorX = Mth.floor(x);
        int line = Mth.floor(y / (double) font.lineHeight);

        StringView substring = this.displayLines.get(Mth.clamp(line, 0, this.displayLines.size() - 1));
        StyledText lineText = this.styledText.subText(substring.beginIndex(), substring.endIndex());
        int col = this.font.substrByWidth(lineText, cursorX).getString().length();

        int lineLength = substring.endIndex() - substring.beginIndex();
        if (col >= lineLength) {
            this.seekCursor(Whence.ABSOLUTE, substring.endIndex());
        } else {
            int width = this.font.width(lineText.subText(0, col));
            int overshot = cursorX - width;

            StyledText nextChar = lineText.subText(col, col + 1);
            int charWidth = this.font.width(nextChar);
            int offset = (overshot * 2 >= charWidth) ? 1 : 0;
            this.seekCursor(Whence.ABSOLUTE, substring.beginIndex() + col + offset);
        }
    }

    @Override
    public void seekCursor(Whence whence, int amount) {
        int cursorBefore = this.cursor;
        int selectBefore = this.selectCursor;

        super.seekCursor(whence, amount);

        if (cursorBefore != this.cursor || selectBefore != this.selectCursor)
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
        this.font.getSplitter().splitLines(this.styledText, this.width, Style.EMPTY, (line, _) -> {
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
        return this.styledText.subText(substring.beginIndex(), substring.endIndex()).getAsFormattedStringLossy();
    }

    public StyledText getRichText() {
        return styledText;
    }

    @Override
    public String value() {
        return styledText.getAsFormattedStringLossy();
    }

    private boolean overflowsLineLimit(StyledText text) {
        return this.hasLineLimit() && this.font.getSplitter().splitLines(text, this.width, Style.EMPTY).size() > this.lineLimit;
    }
}
