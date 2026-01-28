package me.chrr.tapestry.config.gui.widget;

import me.chrr.tapestry.config.gui.OptionProxy;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.util.ARGB;
import net.minecraft.util.CommonColors;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class BooleanOptionWidget extends OptionWidget.Clickable<Boolean> {
    public BooleanOptionWidget(OptionProxy<Boolean> optionProxy) {
        super(optionProxy);
    }

    @Override
    public void onPress(InputWithModifiers input) {
        optionProxy.value = !optionProxy.value;
    }

    @Override
    protected void renderOptionWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        int boxSize = this.getHeight() - (2 + 4) * 2;
        int boxX = this.getRight() - 2 - 4 - boxSize;
        int boxY = this.getY() + 2 + 4;

        int color = CommonColors.WHITE;
        int shadow = ARGB.scaleRGB(color, 0.25F);

        this.renderOptionLabel(graphics, boxX - this.getX());

        graphics.renderOutline(boxX + 1, boxY + 1, boxSize, boxSize, shadow);
        graphics.renderOutline(boxX, boxY, boxSize, boxSize, color);

        if (optionProxy.value) {
            graphics.fill(boxX + 3, boxY + 3, boxX + boxSize - 1, boxY + boxSize - 1, shadow);
            graphics.fill(boxX + 2, boxY + 2, boxX + boxSize - 2, boxY + boxSize - 2, color);
        }
    }
}
