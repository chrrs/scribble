package me.chrr.scribble.gui.button;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class ColorSwatchWidget extends AbstractButton {
    private final TextColor color;
    private final Runnable onClick;

    private boolean toggled = false;

    public ColorSwatchWidget(Component tooltip, TextColor color, Runnable onClick, int x, int y, int width, int height) {
        super(x, y, width, height, tooltip);
        this.setTooltip(Tooltip.create(tooltip));
        this.color = color;
        this.onClick = onClick;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (isHoveredOrFocused() || toggled) {
            graphics.fill(getX(), getY(), getX() + width, getY() + height, isHoveredOrFocused() ? 0xffffffff : 0xffa0a0a0);
        }

        graphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, color.getValue() | 0xff000000);
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

    public TextColor getColor() {
        return color;
    }

    public void setToggled(boolean toggled) {
        this.toggled = toggled;
    }
}
