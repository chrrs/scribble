package me.chrr.scribble.gui;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ARGB;
import net.minecraft.util.CommonColors;
import net.minecraft.util.Util;
import org.jspecify.annotations.NullMarked;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

@NullMarked
public class PageNumberWidget extends AbstractWidget {
    private final Font font;
    private final Consumer<Integer> onPageChange;
    private final int anchorX;

    private boolean dimmed = false;

    private Component text;
    private Component hoverText;

    private Component beforeCursor;
    private Component afterCursor;

    public long lastSwitchFocusTime = Util.getMillis();
    private String input = "";

    public PageNumberWidget(Consumer<Integer> onPageChange, int x, int y, Font font) {
        super(x, y, 0, font.lineHeight, Component.empty());
        this.onPageChange = onPageChange;
        this.anchorX = x;

        this.font = font;

        this.text = Component.empty();
        this.hoverText = Component.empty();

        this.beforeCursor = Component.empty();
        this.afterCursor = Component.empty();
    }

    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float deltaTicks) {
        if (this.isFocused()) {
            int x = this.getX() + this.width;

            x -= this.font.width(this.afterCursor);
            graphics.drawString(this.font, this.afterCursor, x, this.getY(), CommonColors.BLACK, false);

            int cursorX = x;
            x -= this.font.width(this.input);
            graphics.drawString(this.font, this.input, x, this.getY(), CommonColors.BLACK, false);

            x -= this.font.width(this.beforeCursor);
            graphics.drawString(this.font, this.beforeCursor, x, this.getY(), CommonColors.BLACK, false);

            if ((Util.getMillis() - this.lastSwitchFocusTime) / 300L % 2L == 0L) {
                graphics.fill(cursorX, this.getY() - 1, cursorX + 1, this.getY() + 1 + font.lineHeight, CommonColors.BLACK);
            }
        } else {
            int color = ARGB.color(this.dimmed ? 0.3f : 1f, CommonColors.BLACK);

            Component text = this.isHovered() ? this.hoverText : this.text;
            int textWidth = font.width(text);
            graphics.drawString(this.font, text, this.getX() + this.width - textWidth, this.getY(), color, false);
        }

        if (this.isHovered()) {
            graphics.requestCursor(CursorTypes.IBEAM);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (event.modifiers() != 0)
            return false;
        if (event.codepoint() < '0' || event.codepoint() > '9')
            return false;
        if (this.input.length() >= 2 && !(this.input.equals("10") && event.codepoint() == '0'))
            return false;

        this.input += event.codepointAsString();
        return true;
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);

        if (focused) {
            this.lastSwitchFocusTime = Util.getMillis();
            this.input = "";
        }
    }

    @Override
    public void onClick(MouseButtonEvent mouseButtonEvent, boolean bl) {
        setFocused(true);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!isFocused())
            return super.keyPressed(event);

        if (!this.input.isEmpty() && event.key() == GLFW.GLFW_KEY_BACKSPACE) {
            if (event.hasControlDown()) {
                this.input = "";
            } else {
                this.input = this.input.substring(0, this.input.length() - 1);
            }

            return true;
        } else if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
            if (!this.input.isEmpty()) {
                Minecraft.getInstance().getSoundManager()
                        .play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
                this.onPageChange.accept(Integer.parseInt(this.input));
            }

            this.setFocused(false);
            return true;
        } else if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.setFocused(false);
            return true;
        } else if (event.key() == GLFW.GLFW_KEY_LEFT || event.key() == GLFW.GLFW_KEY_RIGHT) {
            // Mark arrows as handled to prevent focus changes.
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
    }

    public void setPageNumber(int page, int total) {
        this.text = Component.translatable("book.pageIndicator", page, total);
        this.hoverText = ComponentUtils.mergeStyles(this.text.copy(), Style.EMPTY.withUnderlined(true));

        // FIXME: surely there's a better way to do this.
        Component indicator = Component.translatable("book.pageIndicator", "--INDICATOR--", total);
        String[] parts = indicator.getString().split("--INDICATOR--", 2);

        Style gray = Style.EMPTY.withColor(ChatFormatting.DARK_GRAY);
        this.beforeCursor = Component.literal(parts[0]).setStyle(gray);
        this.afterCursor = parts.length > 1 ? Component.literal(parts[1]).setStyle(gray) : Component.empty();

        this.width = this.font.width(this.text);
        this.setX(this.anchorX - this.width);
    }

    public void setDimmed(boolean dimmed) {
        this.dimmed = dimmed;
    }
}
