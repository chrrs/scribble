package me.chrr.scribble.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Colors;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class PageNumberWidget extends ClickableWidget {
    private final TextRenderer textRenderer;
    private final Consumer<Integer> onPageChange;
    private final int anchorX;

    private Text text;
    private Text hoverText;

    private Text beforeCursor;
    private Text afterCursor;

    public long lastSwitchFocusTime = Util.getMeasuringTimeMs();
    private String input = "";

    public PageNumberWidget(Consumer<Integer> onPageChange, int x, int y, TextRenderer textRenderer) {
        super(x, y, 0, textRenderer.fontHeight, Text.empty());
        this.onPageChange = onPageChange;
        this.anchorX = x;

        this.textRenderer = textRenderer;

        this.text = ScreenTexts.EMPTY;
        this.hoverText = ScreenTexts.EMPTY;

        this.beforeCursor = ScreenTexts.EMPTY;
        this.afterCursor = ScreenTexts.EMPTY;
    }

    public void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        if (this.isFocused()) {
            int x = this.getX() + this.width;

            x -= this.textRenderer.getWidth(this.afterCursor);
            context.drawText(this.textRenderer, this.afterCursor, x, this.getY(), Colors.BLACK, false);

            int cursorX = x;
            x -= this.textRenderer.getWidth(this.input);
            context.drawText(this.textRenderer, this.input, x, this.getY(), Colors.BLACK, false);

            x -= this.textRenderer.getWidth(this.beforeCursor);
            context.drawText(this.textRenderer, this.beforeCursor, x, this.getY(), Colors.BLACK, false);

            if ((Util.getMeasuringTimeMs() - this.lastSwitchFocusTime) / 300L % 2L == 0L) {
                context.fill(cursorX, this.getY() - 1, cursorX + 1, this.getY() + 1 + textRenderer.fontHeight, Colors.BLACK);
            }
        } else {
            Text text = this.isHovered() ? this.hoverText : this.text;
            int textWidth = textRenderer.getWidth(text);
            context.drawText(this.textRenderer, text, this.getX() + this.width - textWidth, this.getY(), Colors.BLACK, false);
        }

        //? if >=1.21.9 {
        if (this.isHovered()) {
            context.setCursor(net.minecraft.client.gui.cursor.StandardCursors.IBEAM);
        }
        //?}
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        this.appendDefaultNarrations(builder);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (modifiers != 0)
            return false;
        if (chr < '0' || chr > '9')
            return false;
        if (this.input.length() >= 2 && !(this.input.equals("10") && chr == '0'))
            return false;

        this.input += chr;
        return true;
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);

        if (focused) {
            this.lastSwitchFocusTime = Util.getMeasuringTimeMs();
            this.input = "";
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY/*? if >=1.21.9 {*/, boolean doubleClick /*?}*/) {
        setFocused(true);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isFocused())
            return super.keyPressed(keyCode, scanCode, modifiers);

        if (!this.input.isEmpty() && keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (Screen.hasControlDown()) {
                this.input = "";
            } else {
                this.input = this.input.substring(0, this.input.length() - 1);
            }

            return true;
        } else if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (!this.input.isEmpty()) {
                MinecraftClient.getInstance().getSoundManager()
                        .play(PositionedSoundInstance.master(SoundEvents.ITEM_BOOK_PAGE_TURN, 1.0F));
                this.onPageChange.accept(Integer.parseInt(this.input));
            }

            this.setFocused(false);
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.setFocused(false);
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT) {
            // Mark arrows as handled to prevent focus changes.
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
    }

    public void setPageNumber(int page, int total) {
        this.text = Text.translatable("book.pageIndicator", page, total);
        this.hoverText = Texts.setStyleIfAbsent(this.text.copy(), Style.EMPTY.withUnderline(true));

        // FIXME: surely there's a better way to do this.
        Text indicator = Text.translatable("book.pageIndicator", "--INDICATOR--", total);
        String[] parts = indicator.getString().split("--INDICATOR--", 2);

        Style gray = Style.EMPTY.withColor(Formatting.DARK_GRAY);
        this.beforeCursor = Text.literal(parts[0]).setStyle(gray);
        this.afterCursor = parts.length > 1 ? Text.literal(parts[1]).setStyle(gray) : ScreenTexts.EMPTY;

        this.width = this.textRenderer.getWidth(this.text);
        this.setX(this.anchorX - this.width);
    }
}
