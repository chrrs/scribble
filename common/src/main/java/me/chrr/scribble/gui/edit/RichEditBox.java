package me.chrr.scribble.gui.edit;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import me.chrr.scribble.gui.TextArea;
import me.chrr.scribble.history.command.Command;
import me.chrr.scribble.history.command.EditCommand;
import me.chrr.scribble.text.StyleFlag;
import me.chrr.scribble.text.StyledText;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.CommonColors;
import net.minecraft.util.Util;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@NullMarked
public class RichEditBox extends MultiLineEditBox implements TextArea<StyledText> {
    private final @Nullable Runnable onInvalidateStyle;
    private final @Nullable Consumer<Command> onHistoryPush;

    public Style style = Style.EMPTY;

    private RichEditBox(Font font, int x, int y, int width, int height,
                        Component placeholder, Component message, int textColor, boolean textShadow, int cursorColor,
                        boolean hasBackground, boolean hasOverlay,
                        @Nullable Runnable onInvalidateStyle, @Nullable Consumer<Command> onHistoryPush) {
        super(font, x, y, width, height, placeholder, message, textColor, textShadow, cursorColor, hasBackground, hasOverlay);

        this.onInvalidateStyle = onInvalidateStyle;
        this.onHistoryPush = onHistoryPush;

        this.textField = new RichMultiLineTextField(
                font, width - this.totalInnerPadding(),
                () -> style,
                (style) -> {
                    this.style = style;
                    this.notifyInvalidateStyle();
                });
    }

    private void notifyInvalidateStyle() {
        if (this.onInvalidateStyle != null) {
            this.onInvalidateStyle.run();
        }
    }

    private void pushHistory(EditCommand command) {
        if (this.onHistoryPush != null) {
            this.onHistoryPush.accept(command);
        }
    }

    public void setRichValueListener(Consumer<StyledText> valueListener) {
        this.getRichTextField().setRichValueListener(valueListener);
    }

    public void applyStyle(Function<Style, Style> modifier) {
        RichMultiLineTextField textField = this.getRichTextField();

        if (textField.hasSelection()) {
            EditCommand command = new EditCommand(this, (box) -> box.applySelection(modifier));
            command.executeEdit(textField);
            this.pushHistory(command);
        } else {
            modifier.apply(this.style);
        }
    }

    private int getCursorIndicatorColor() {
        TextColor color = this.style.getColor();

        if (color == null) {
            return CommonColors.BLACK;
        } else {
            return 0xff000000 | color.getValue();
        }
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTicks) {
        StyledText text = getRichTextField().getRichText();

        // Draw the placeholder text if there's no content.
        if (text.isEmpty() && !this.isFocused()) {
            graphics.textWithWordWrap(this.font, this.placeholder, this.getInnerLeft(), this.getInnerTop(), this.width - this.totalInnerPadding(), 0xcce0e0e0);
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
                    StyledText lineText = text.subText(line.beginIndex(), line.endIndex());
                    graphics.text(this.font, lineText.getAsMutableComponent(), x, y, this.textColor, this.textShadow);

                    StyledText beforeCursor = text.subText(line.beginIndex(), cursor);
                    lastX = x + this.font.width(beforeCursor);

                    if (!hasDrawnCursor) {
                        graphics.fill(lastX, y - 1, lastX + 1, y + 1 + this.font.lineHeight, this.getCursorIndicatorColor());
                        hasDrawnCursor = true;
                    }
                }
            } else {
                // Otherwise, just draw the line normally.
                if (visible) {
                    StyledText lineText = text.subText(line.beginIndex(), line.endIndex());
                    graphics.text(this.font, lineText.getAsMutableComponent(), x, y, this.textColor, this.textShadow);
                    lastX = x + this.font.width(lineText) - 1;
                }

                lastY = y;
            }

            y += this.font.lineHeight;
        }

        // If we haven't drawn the cursor yet, it should be a '_' at the last draw position.
        if (blink && !cursorInText) {
            if (this.withinContentAreaTopBottom(lastY, lastY + this.font.lineHeight)) {
                graphics.text(this.font, "_", lastX + 1, lastY, this.getCursorIndicatorColor(), this.textShadow);
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
            StyleFlag flag = switch (event.key()) {
                case GLFW.GLFW_KEY_B -> StyleFlag.Bold;
                case GLFW.GLFW_KEY_I -> StyleFlag.Italic;
                case GLFW.GLFW_KEY_U -> StyleFlag.Underline;
                case GLFW.GLFW_KEY_MINUS -> StyleFlag.Strikethrough;
                case GLFW.GLFW_KEY_K -> StyleFlag.Obfuscated;
                default -> null;
            };

            if (flag != null) {
                this.applyStyle((style) -> flag.apply(style, !flag.isPresent(this.style)));
                return true;
            }
        }

        // Wrap the operation with an edit command if it edits the text.
        if (event.isCut() || event.isPaste() ||
                List.of(GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER,
                        GLFW.GLFW_KEY_BACKSPACE, GLFW.GLFW_KEY_DELETE).contains(event.key())) {
            EditCommand command = new EditCommand(this,
                    (textField) -> textField.keyPressed(event));
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
        return (RichMultiLineTextField) this.textField;
    }

    @Override
    public void setText(StyledText text) {
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

        public Builder onInvalidateFormat(Runnable onInvalidateFormat) {
            this.onInvalidateFormat = onInvalidateFormat;
            return this;
        }

        public Builder onHistoryPush(Consumer<Command> onHistoryPush) {
            this.onHistoryPush = onHistoryPush;
            return this;
        }

        @Override
        public MultiLineEditBox build(Font font, int width, int height, Component message) {
            return new RichEditBox(font,
                    this.x, this.y, width, height,
                    this.placeholder, message, this.textColor,
                    this.textShadow, this.cursorColor, this.showBackground,
                    this.showDecorations, this.onInvalidateFormat, this.onHistoryPush);
        }
    }
}
