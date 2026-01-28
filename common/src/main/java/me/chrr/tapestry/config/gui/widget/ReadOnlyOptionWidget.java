package me.chrr.tapestry.config.gui.widget;

import me.chrr.tapestry.config.gui.OptionProxy;
import net.minecraft.client.gui.GuiGraphics;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class ReadOnlyOptionWidget<T> extends OptionWidget<T> {
    public ReadOnlyOptionWidget(OptionProxy<T> optionProxy) {
        super(optionProxy);
        this.active = false;
    }

    @Override
    protected void renderOptionWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderOptionLabel(graphics, this.getWidth());
        this.renderValueLabel(graphics, 0, this.getWidth());
        this.handleCursor(graphics);
    }
}
