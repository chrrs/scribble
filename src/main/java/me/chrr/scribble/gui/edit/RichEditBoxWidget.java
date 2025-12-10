package me.chrr.scribble.gui.edit;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import me.chrr.scribble.book.RichText;
import me.chrr.scribble.history.command.EditCommand;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
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
import net.minecraft.util.Tuple;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class RichEditBoxWidget extends MultiLineEditBox {
    @Nullable
    private final Runnable onInvalidateFormat;
    @Nullable
    private final Consumer<EditCommand> onHistoryPush;

    @Nullable
    public ChatFormatting color = ChatFormatting.BLACK;
    public Set<ChatFormatting> modifiers = new HashSet<>();

    private RichEditBoxWidget(Font font, int x, int y, int width, int height,
                              Component placeholder, Component message, int textColor, boolean textShadow, int cursorColor,
                              boolean hasBackground, boolean hasOverlay,
                              @Nullable Runnable onInvalidateFormat, @Nullable Consumer<EditCommand> onHistoryPush) {
        super(font, x, y, width, height, placeholder, message, textColor, textShadow, cursorColor, hasBackground, hasOverlay);
        this.onInvalidateFormat = onInvalidateFormat;
        this.onHistoryPush = onHistoryPush;

        this.textField = new RichMultiLineTextField(
                font, width - this.totalInnerPadding(),
                () -> new Tuple<>(Optional.ofNullable(color).orElse(ChatFormatting.BLACK), modifiers),
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

    public void applyFormatting(ChatFormatting formatting, boolean active) {
        RichMultiLineTextField editBox = this.getRichTextField();

        if (editBox.hasSelection()) {
            EditCommand command = new EditCommand(editBox, (box) -> box.applyFormatting(formatting, active));
            command.executeEdit(editBox);
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
            graphics.drawWordWrap(this.font, this.placeholder, this.getInnerLeft(), this.getInnerTop(), this.width - this.totalInnerPadding(), -857677600);
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

                        graphics.textHighlight(x + start, y, x + end, y + this.font.lineHeight);
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
            EditCommand command = new EditCommand(this.getRichTextField(),
                    (editBox) -> editBox.insertText(event.codepointAsString()));
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
                this.applyFormatting(modifier, !this.modifiers.contains(modifier));
                return true;
            }
        }

        // Wrap the operation with an edit command if it edits the text.
        if (event.isCut() || event.isPaste() ||
                List.of(GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER,
                        GLFW.GLFW_KEY_BACKSPACE, GLFW.GLFW_KEY_DELETE).contains(event.key())) {
            EditCommand command = new EditCommand(this.getRichTextField(),
                    (editBox) -> editBox.keyPressed(event));
            command.executeEdit(this.getRichTextField());
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
        return (RichMultiLineTextField) textField;
    }

    public static class Builder extends MultiLineEditBox.Builder {
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
        public @NotNull MultiLineEditBox build(Font font, int width, int height, Component message) {
            return new RichEditBoxWidget(font,
                    this.x, this.y, width, height,
                    this.placeholder, message, this.textColor,
                    this.textShadow, this.cursorColor, this.showBackground,
                    this.showDecorations, this.onInvalidateFormat, this.onHistoryPush);
        }
    }
}
