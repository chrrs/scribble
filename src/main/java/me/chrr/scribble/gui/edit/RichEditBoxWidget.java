package me.chrr.scribble.gui.edit;

import me.chrr.scribble.book.RichText;
import me.chrr.scribble.history.command.EditCommand;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.EditBox;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class RichEditBoxWidget extends EditBoxWidget {
    @Nullable
    private final Runnable onInvalidateFormat;
    @Nullable
    private final Consumer<EditCommand> onHistoryPush;

    @Nullable
    public Formatting color = Formatting.BLACK;
    public Set<Formatting> modifiers = new HashSet<>();

    private RichEditBoxWidget(TextRenderer textRenderer, int x, int y, int width, int height,
                              Text placeholder, Text message, int textColor, boolean textShadow, int cursorColor,
                              boolean hasBackground, boolean hasOverlay,
                              @Nullable Runnable onInvalidateFormat, @Nullable Consumer<EditCommand> onHistoryPush) {
        super(textRenderer, x, y, width, height, placeholder, message, textColor, textShadow, cursorColor, hasBackground, hasOverlay);
        this.onInvalidateFormat = onInvalidateFormat;
        this.onHistoryPush = onHistoryPush;

        this.editBox = new RichEditBox(
                textRenderer, width - this.getPadding(),
                () -> new Pair<>(Optional.ofNullable(color).orElse(Formatting.BLACK), modifiers),
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

    public void applyFormatting(Formatting formatting, boolean active) {
        RichEditBox editBox = this.getRichEditBox();

        if (editBox.hasSelection()) {
            EditCommand command = new EditCommand(editBox, (box) -> box.applyFormatting(formatting, active));
            command.executeEdit(editBox);
            this.pushHistory(command);
        } else {
            if (formatting.isModifier()) {
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
            return Colors.BLACK;
        } else {
            //noinspection DataFlowIssue: the color variable is never a modifier.
            return 0xff000000 | this.color.getColorValue();
        }
    }

    @Override
    protected void renderContents(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        RichText text = getRichEditBox().getRichText();

        // Draw the placeholder text if there's no content.
        if (text.isEmpty() && !this.isFocused()) {
            context.drawWrappedTextWithShadow(this.textRenderer, this.placeholder, this.getTextX(), this.getTextY(), this.width - this.getPadding(), -857677600);
            return;
        }

        int cursor = this.editBox.getCursor();
        boolean blink = this.isFocused() && (Util.getMeasuringTimeMs() - this.lastSwitchFocusTime) / 300L % 2L == 0L;
        boolean cursorInText = cursor < text.getLength();

        int lastX = 0;
        int lastY = 0;

        int y = this.getTextY();
        for (EditBox.Substring line : this.editBox.getLines()) {
            boolean visible = this.isVisible(y, y + textRenderer.fontHeight);

            int x = this.getTextX();
            if (blink && cursorInText && cursor >= line.beginIndex() && cursor < line.endIndex()) {
                // If the cursor is in the current line, draw the first and second half separately.
                // FIXME: vanilla bug: MC-298732 (cursor isn't visible at the end of a line).
                if (visible) {
                    RichText beforeCursor = text.subText(line.beginIndex(), cursor);
                    RichText afterCursor = text.subText(cursor, line.endIndex());

                    context.drawText(this.textRenderer, beforeCursor.getAsMutableText(), x, y, this.textColor, this.textShadow);
                    lastX = x + this.textRenderer.getWidth(beforeCursor);

                    context.fill(lastX, y - 1, lastX + 1, y + 1 + textRenderer.fontHeight, this.getCursorColor());

                    context.drawText(this.textRenderer, afterCursor.getAsMutableText(), lastX, y, this.textColor, this.textShadow);
                }
            } else {
                // Otherwise, just draw the line normally.
                if (visible) {
                    RichText lineText = text.subText(line.beginIndex(), line.endIndex());
                    context.drawText(this.textRenderer, lineText.getAsMutableText(), x, y, this.textColor, this.textShadow);
                    lastX = x + this.textRenderer.getWidth(lineText) - 1;
                }

                lastY = y;
            }

            y += textRenderer.fontHeight;
        }

        // If we haven't drawn the cursor yet, it should be a '_' at the last draw position.
        if (blink && !cursorInText) {
            if (this.isVisible(lastY, lastY + textRenderer.fontHeight)) {
                context.drawText(this.textRenderer, "_", lastX, lastY, this.getCursorColor(), this.textShadow);
            }
        }

        // If we have a selection, we want to draw it.
        if (this.editBox.hasSelection()) {
            EditBox.Substring selection = this.editBox.getSelection();
            int x = this.getTextX();
            y = this.getTextY();

            // Loop through the lines, and draw selection boxes for each line.
            for (EditBox.Substring line : this.editBox.getLines()) {
                if (selection.beginIndex() <= line.endIndex()) {
                    if (line.beginIndex() > selection.endIndex()) {
                        break;
                    }

                    if (this.isVisible(y, y + textRenderer.fontHeight)) {
                        int start = this.textRenderer.getWidth(text.subText(line.beginIndex(), Math.max(selection.beginIndex(), line.beginIndex())));

                        int end = selection.endIndex() > line.endIndex()
                                ? this.width - this.getTextMargin()
                                : this.textRenderer.getWidth(text.subText(line.beginIndex(), selection.endIndex()));

                        //? if <1.21.7 {
                        /*this.drawSelection(context, x + start, y, x + end, y + textRenderer.fontHeight);
                        *///?} else
                        context.drawSelection(x + start, y, x + end, y + textRenderer.fontHeight);
                    }
                }

                y += textRenderer.fontHeight;
            }

        }
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (this.visible && this.isFocused() && StringHelper.isValidChar(chr)) {
            EditCommand command = new EditCommand(this.getRichEditBox(),
                    (editBox) -> editBox.replaceSelection(Character.toString(chr)));
            command.executeEdit(this.getRichEditBox());
            this.pushHistory(command);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Respond to common hotkeys for toggling modifiers, such as Ctrl-B for bold.
        if (Screen.hasControlDown() && !Screen.hasShiftDown() && !Screen.hasAltDown()) {
            Formatting modifier = switch (keyCode) {
                case GLFW.GLFW_KEY_B -> Formatting.BOLD;
                case GLFW.GLFW_KEY_I -> Formatting.ITALIC;
                case GLFW.GLFW_KEY_U -> Formatting.UNDERLINE;
                case GLFW.GLFW_KEY_MINUS -> Formatting.STRIKETHROUGH;
                case GLFW.GLFW_KEY_K -> Formatting.OBFUSCATED;
                default -> null;
            };

            if (modifier != null) {
                this.applyFormatting(modifier, !this.modifiers.contains(modifier));
                return true;
            }
        }

        // Wrap the operation with an edit command if it edits the text.
        if (Screen.isCut(keyCode) || Screen.isPaste(keyCode) ||
                List.of(GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER,
                        GLFW.GLFW_KEY_BACKSPACE, GLFW.GLFW_KEY_DELETE).contains(keyCode)) {
            EditCommand command = new EditCommand(this.getRichEditBox(),
                    (editBox) -> editBox.handleSpecialKey(keyCode));
            command.executeEdit(this.getRichEditBox());
            this.pushHistory(command);
            return true;
        }


        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void appendClickableNarrations(NarrationMessageBuilder builder) {
        // Make sure the narrator narrates the plain text, not the formatting codes.
        builder.put(NarrationPart.TITLE, Text.translatable("gui.narrate.editBox",
                this.getMessage(), getRichEditBox().getRichText().getPlainText()));
    }

    public RichEditBox getRichEditBox() {
        return (RichEditBox) editBox;
    }

    public static class Builder extends EditBoxWidget.Builder {
        @Nullable
        private Runnable onInvalidateFormat = null;
        @Nullable
        private Consumer<EditCommand> onHistoryPush = null;

        public Builder onInvalidateFormat(Runnable onInvalidateFormat) {
            this.onInvalidateFormat = onInvalidateFormat;
            return this;
        }

        public Builder onHistoryPush(Consumer<EditCommand> onHistoryPush) {
            this.onHistoryPush = onHistoryPush;
            return this;
        }

        @Override
        public EditBoxWidget build(TextRenderer textRenderer, int width, int height, Text message) {
            return new RichEditBoxWidget(textRenderer,
                    this.x, this.y, width, height,
                    this.placeholder, message, this.textColor,
                    this.textShadow, this.cursorColor, this.hasBackground,
                    this.hasOverlay, this.onInvalidateFormat, this.onHistoryPush);
        }
    }
}
