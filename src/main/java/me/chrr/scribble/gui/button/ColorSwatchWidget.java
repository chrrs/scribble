package me.chrr.scribble.gui.button;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

public class ColorSwatchWidget extends AbstractButton {
    private final ChatFormatting color;
    private final Runnable onClick;

    private boolean toggled;

    public ColorSwatchWidget(Component tooltip, ChatFormatting color, Runnable onClick, int x, int y, int width, int height, boolean toggled) {
        super(x, y, width, height, tooltip);
        this.setTooltip(Tooltip.create(tooltip));
        this.color = color;
        this.onClick = onClick;
        this.toggled = toggled;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (isHoveredOrFocused() || toggled) {
            graphics.fill(getX(), getY(), getX() + width, getY() + height, isHoveredOrFocused() ? 0xffffffff : 0xffa0a0a0);
        }

        Integer color = this.color.getColor();
        if (color == null) {
            return;
        }

        color = color | 0xff000000;
        graphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, color);
    }

    @Override
    public boolean shouldTakeFocusAfterInteraction() {
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }

    @Override
    public void onPress(InputWithModifiers inputWithModifiers) {
        this.toggled = true;
        this.onClick.run();
    }

    public ChatFormatting getColor() {
        return color;
    }

    public void setToggled(boolean toggled) {
        this.toggled = toggled;
    }
}
