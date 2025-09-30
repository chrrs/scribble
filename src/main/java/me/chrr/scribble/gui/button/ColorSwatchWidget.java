package me.chrr.scribble.gui.button;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.input.AbstractInput;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ColorSwatchWidget extends PressableWidget {
    private final Formatting color;
    private final Runnable onClick;

    private boolean toggled;

    public ColorSwatchWidget(Text tooltip, Formatting color, Runnable onClick, int x, int y, int width, int height, boolean toggled) {
        super(x, y, width, height, tooltip);
        this.setTooltip(Tooltip.of(tooltip));
        this.color = color;
        this.onClick = onClick;
        this.toggled = toggled;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        if (isSelected() || toggled) {
            context.fill(getX(), getY(), getX() + width, getY() + height, isSelected() ? 0xffffffff : 0xffa0a0a0);
        }

        Integer color = this.color.getColorValue();
        if (color == null) {
            return;
        }

        color = color | 0xff000000;
        context.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, color);
    }

    @Override
    public boolean isClickable() {
        return false;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        this.appendDefaultNarrations(builder);
    }

    @Override
    public void onPress(AbstractInput input) {
        this.toggled = true;
        this.onClick.run();
    }

    public Formatting getColor() {
        return color;
    }

    public void setToggled(boolean toggled) {
        this.toggled = toggled;
    }
}
