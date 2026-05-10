package me.chrr.scribble.gui.edit;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.mojang.datafixers.util.Pair;
import me.chrr.scribble.Scribble;
import me.chrr.scribble.ScribbleConfig;
import me.chrr.scribble.book.RichText;
import me.chrr.scribble.gui.TextArea;
import me.chrr.scribble.history.command.Command;
import me.chrr.scribble.history.command.EditCommand;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.util.Util;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

@NullMarked
public class RichEditBox extends MultiLineEditBox implements TextArea<RichText> {
    private final @Nullable Runnable onInvalidateFormat;
    private final @Nullable Consumer<Command> onHistoryPush;
    private final @Nullable OverflowHandler overflowHandler;

    public @Nullable ChatFormatting color = ChatFormatting.BLACK;
    public Set<ChatFormatting> modifiers = new HashSet<>();

    private RichEditBox(Font font, int x, int y, int width, int height,
                        Component placeholder, Component message, int textColor, boolean textShadow, int cursorColor,
                        boolean hasBackground, boolean hasOverlay,
                        @Nullable Runnable onInvalidateFormat, @Nullable Consumer<Command> onHistoryPush,
                        @Nullable OverflowHandler overflowHandler) {
        super(font, x, y, width, height, placeholder, message, textColor, textShadow, cursorColor, hasBackground, hasOverlay);

        this.onInvalidateFormat = onInvalidateFormat;
        this.onHistoryPush = onHistoryPush;
        this.overflowHandler = overflowHandler;

        this.textField = new RichMultiLineTextField(
                font, width - this.totalInnerPadding(),
                () -> new Pair<>(Optional.ofNullable(color).orElse(ChatFormatting.BLACK), modifiers),
                (color, modifiers) -> {
                    this.color = color;
                    this.modifiers = new HashSet<>(modifiers);
                    this.notifyInvalidateFormat();
                });
    }

    private void notifyInvalidateFormat() {
        if (this.onInvalidateFormat != null) {
            this.onInvalidateFormat.run();
        }
    }

    private void pushHistory(EditCommand command) {
        if (this.onHistoryPush != null) {
            this.onHistoryPush.accept(command);
        }
    }

    public void setRichValueListener(Consumer<RichText> valueListener) {
        this.getRichTextField().setRichValueListener(valueListener);
    }

    public void applyFormat(ChatFormatting formatting, boolean active) {
        RichMultiLineTextField textField = this.getRichTextField();

        if (textField.hasSelection()) {
            EditCommand command = new EditCommand(this, (box) -> box.applyFormatting(formatting, active));
            command.executeEdit(textField);
            this.pushHistory(command);
        } else {
            if (formatting.isFormat()) {
                if (active) {
                    this.modifiers.add(formatting);
                } else {
                    this.modifiers.remove(formatting);
                }
            } else {
                this.color = formatting;
            }

            this.notifyInvalidateFormat();
        }
    }

    private int getCursorColor() {
        if (this.color == null) {
            return CommonColors.BLACK;
        } else {
            //noinspection DataFlowIssue: the color variable is never a modifier.
            return 0xff000000 | this.color.getColor();
        }
    }

    @Override
    protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float deltaTicks) {
        RichText text = getRichTextField().getRichText();

        // Draw the placeholder text if there's no content.
        if (text.isEmpty() && !this.isFocused()) {
            graphics.drawWordWrap(this.font, this.placeholder, this.getInnerLeft(), this.getInnerTop(), this.width - this.totalInnerPadding(), 0xcce0e0e0);
            return;
        }

        int cursor = this.textField.cursor();
        boolean blink = this.isFocused() && (Util.getMillis() - this.focusedTime) / 300L % 2L == 0L;
        boolean cursorInText = cursor < text.getLength();

        int lastX = 0;
        int lastY = 0;

        int y = this.getInnerTop();
        boolean hasDrawnCursor = false;
        for (MultilineTextField.StringView line : this.textField.iterateLines()) {
            boolean visible = this.withinContentAreaTopBottom(y, y + font.lineHeight);

            int x = this.getInnerLeft();
            if (blink && cursorInText && cursor >= line.beginIndex() && cursor <= line.endIndex()) {
                if (visible) {
                    // AD-HOC: Draw the entire line in one call. Vanilla does this differently, I don't know why
                    RichText lineText = text.subText(line.beginIndex(), line.endIndex());
                    graphics.drawString(this.font, lineText.getAsMutableComponent(), x, y, this.textColor, this.textShadow);

                    RichText beforeCursor = text.subText(line.beginIndex(), cursor);
                    lastX = x + this.font.width(beforeCursor);

                    if (!hasDrawnCursor) {
                        graphics.fill(lastX, y - 1, lastX + 1, y + 1 + this.font.lineHeight, this.getCursorColor());
                        hasDrawnCursor = true;
                    }
                }
            } else {
                // Otherwise, just draw the line normally.
                if (visible) {
                    RichText lineText = text.subText(line.beginIndex(), line.endIndex());
                    graphics.drawString(this.font, lineText.getAsMutableComponent(), x, y, this.textColor, this.textShadow);
                    lastX = x + this.font.width(lineText) - 1;
                }

                lastY = y;
            }

            y += this.font.lineHeight;
        }

        // If we haven't drawn the cursor yet, it should be a '_' at the last draw position.
        if (blink && !cursorInText) {
            if (this.withinContentAreaTopBottom(lastY, lastY + this.font.lineHeight)) {
                graphics.drawString(this.font, "_", lastX + 1, lastY, this.getCursorColor(), this.textShadow);
            }
        }

        // If we have a selection, we want to draw it.
        if (this.textField.hasSelection()) {
            MultilineTextField.StringView selection = this.textField.getSelected();
            int x = this.getInnerLeft();
            y = this.getInnerTop();

            // Loop through the lines, and draw selection boxes for each line.
            for (MultilineTextField.StringView line : this.textField.iterateLines()) {
                if (selection.beginIndex() <= line.endIndex()) {
                    if (line.beginIndex() > selection.endIndex()) {
                        break;
                    }

                    if (this.withinContentAreaTopBottom(y, y + this.font.lineHeight)) {
                        int start = this.font.width(text.subText(line.beginIndex(), Math.max(selection.beginIndex(), line.beginIndex())));

                        int end = selection.endIndex() > line.endIndex()
                                ? this.width - this.innerPadding()
                                : this.font.width(text.subText(line.beginIndex(), selection.endIndex()));

                        graphics.textHighlight(x + start, y, x + end, y + this.font.lineHeight, true);
                    }
                }

                y += this.font.lineHeight;
            }
        }

        // Switch the cursor to an I-beam when we're hovering.
        if (this.isHovered()) {
            graphics.requestCursor(CursorTypes.IBEAM);
        }
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (this.visible && this.isFocused() && event.isAllowedChatCharacter()) {
            RichMultiLineTextField tf = this.getRichTextField();
            RichText currentText = tf.getRichText();
            int cursor = tf.cursor;
            
            // Create the text to insert
            RichText insert = new RichText(event.codepointAsString(),
                    Optional.ofNullable(color).orElse(ChatFormatting.BLACK), modifiers);
            
            // Check if this would overflow
            RichText result = tf.hasSelection()
                    ? currentText.replace(tf.getSelected().beginIndex(), tf.getSelected().endIndex(), insert)
                    : currentText.insert(cursor, insert);
            
            boolean wouldOverflow = tf.hasLineLimit() && tf.font.getSplitter()
                    .splitLines(result, tf.width, net.minecraft.network.chat.Style.EMPTY).size() > tf.lineLimit;
            
            // If would overflow and we have an overflow handler, try to handle it
            if (wouldOverflow && this.overflowHandler != null && cursor == currentText.getLength() && !tf.hasSelection()) {
                if (this.overflowHandler.handleOverflow(currentText, cursor, insert, color, modifiers)) {
                    return true;
                }
            }
            
            // Normal behavior
            EditCommand command = new EditCommand(this,
                    (textField) -> textField.insertText(event.codepointAsString()));
            command.executeEdit(this.getRichTextField());
            this.pushHistory(command);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // Respond to common hotkeys for toggling modifiers, such as Ctrl-B for bold.
        if (event.hasControlDown() && !event.hasShiftDown() && !event.hasAltDown()) {
            ChatFormatting modifier = switch (event.key()) {
                case GLFW.GLFW_KEY_B -> ChatFormatting.BOLD;
                case GLFW.GLFW_KEY_I -> ChatFormatting.ITALIC;
                case GLFW.GLFW_KEY_U -> ChatFormatting.UNDERLINE;
                case GLFW.GLFW_KEY_MINUS -> ChatFormatting.STRIKETHROUGH;
                case GLFW.GLFW_KEY_K -> ChatFormatting.OBFUSCATED;
                default -> null;
            };

            if (modifier != null) {
                this.applyFormat(modifier, !this.modifiers.contains(modifier));
                return true;
            }
        }

        RichMultiLineTextField tf = this.getRichTextField();
        RichText currentText = tf.getRichText();
        int cursor = tf.cursor;
        boolean isAtEndOfText = cursor == currentText.getLength() && !tf.hasSelection();

        // Handle Enter at end of full page - create new page if overflow handler exists
        if ((event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) && isAtEndOfText) {
            boolean pageFull = tf.hasLineLimit() && tf.font.getSplitter()
                    .splitLines(currentText, tf.width, net.minecraft.network.chat.Style.EMPTY).size() >= tf.lineLimit;
            if (pageFull) {
                if (this.overflowHandler != null && this.overflowHandler.handleEnterAtEnd()) {
                    return true;
                }
                return true; // Block enter on full page if overflow disabled
            }
        }

        // Handle Backspace on empty page - delete page if overflow handler exists
        if (event.key() == GLFW.GLFW_KEY_BACKSPACE && currentText.isEmpty() && !tf.hasSelection()) {
            if (this.overflowHandler != null && this.overflowHandler.handleBackspaceOnEmpty()) {
                return true;
            }
        }

        // Handle Paste with different behaviors based on config
        if (event.isPaste()) {
            String clipboardText = Minecraft.getInstance().keyboardHandler.getClipboard().replace("\r", "");
            boolean keepFormatting = Scribble.config().copyFormattingCodes.get() ^ event.hasShiftDown();
            if (!keepFormatting) clipboardText = ChatFormatting.stripFormatting(clipboardText);
            
            RichText insert = ChatFormatting.stripFormatting(clipboardText).equals(clipboardText)
                    ? new RichText(clipboardText, Optional.ofNullable(color).orElse(ChatFormatting.BLACK), modifiers)
                    : RichText.fromFormattedString(clipboardText);

            int start = tf.hasSelection() ? tf.getSelected().beginIndex() : cursor;
            int end = tf.hasSelection() ? tf.getSelected().endIndex() : cursor;
            RichText result = tf.hasSelection() ? currentText.replace(start, end, insert) : currentText.insert(cursor, insert);
            boolean wouldOverflow = tf.hasLineLimit() && tf.font.getSplitter()
                    .splitLines(result, tf.width, net.minecraft.network.chat.Style.EMPTY).size() > tf.lineLimit;

            if (wouldOverflow) {
                ScribbleConfig.PasteBehavior behavior = Scribble.config().pasteBehavior.get();
                if (behavior == ScribbleConfig.PasteBehavior.FIT_PAGE) {
                    int lo = 0, hi = insert.getLength(), best = 0;
                    while (lo <= hi) {
                        int mid = (lo + hi) / 2;
                        RichText partial = insert.subText(0, mid);
                        RichText partialResult = tf.hasSelection() ? currentText.replace(start, end, partial) : currentText.insert(cursor, partial);
                        if (tf.font.getSplitter().splitLines(partialResult, tf.width, net.minecraft.network.chat.Style.EMPTY).size() <= tf.lineLimit) {
                            best = mid; lo = mid + 1;
                        } else hi = mid - 1;
                    }
                    if (best > 0) {
                        String truncated = insert.subText(0, best).getAsFormattedString();
                        EditCommand cmd = new EditCommand(this, t -> t.insertText(truncated));
                        cmd.executeEdit(tf);
                        this.pushHistory(cmd);
                    }
                    return true;
                } else if (behavior == ScribbleConfig.PasteBehavior.OVERFLOW && this.overflowHandler != null && isAtEndOfText) {
                    if (this.overflowHandler.handleOverflow(currentText, cursor, insert, color, modifiers)) return true;
                }
            }
            
            EditCommand command = new EditCommand(this, t -> t.keyPressed(event));
            command.executeEdit(tf);
            this.pushHistory(command);
            return true;
        }

        // Wrap the operation with an edit command if it edits the text.
        if (event.isCut() || List.of(GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER,
                GLFW.GLFW_KEY_BACKSPACE, GLFW.GLFW_KEY_DELETE).contains(event.key())) {
            EditCommand command = new EditCommand(this, t -> t.keyPressed(event));
            command.executeEdit(tf);
            this.pushHistory(command);
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        // Make sure the narrator narrates the plain text, not the formatting codes.
        narrationElementOutput.add(NarratedElementType.TITLE, Component.translatable("gui.narrate.editBox",
                this.getMessage(), getRichTextField().getRichText().getPlainText()));
    }

    public RichMultiLineTextField getRichTextField() {
        return (RichMultiLineTextField) this.textField;
    }

    @Override
    public void setText(RichText text) {
        this.getRichTextField().setValue(text, true);
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public static class Builder extends MultiLineEditBox.Builder {
        @Nullable
        private Runnable onInvalidateFormat = null;
        @Nullable
        private Consumer<Command> onHistoryPush = null;
        @Nullable
        private OverflowHandler overflowHandler = null;

        public Builder onInvalidateFormat(Runnable onInvalidateFormat) {
            this.onInvalidateFormat = onInvalidateFormat;
            return this;
        }

        public Builder onHistoryPush(Consumer<Command> onHistoryPush) {
            this.onHistoryPush = onHistoryPush;
            return this;
        }

        public Builder onOverflow(OverflowHandler overflowHandler) {
            this.overflowHandler = overflowHandler;
            return this;
        }

        @Override
        public MultiLineEditBox build(Font font, int width, int height, Component message) {
            return new RichEditBox(font,
                    this.x, this.y, width, height,
                    this.placeholder, message, this.textColor,
                    this.textShadow, this.cursorColor, this.showBackground,
                    this.showDecorations, this.onInvalidateFormat, this.onHistoryPush,
                    this.overflowHandler);
        }
    }
}
