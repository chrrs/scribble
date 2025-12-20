package me.chrr.tapestry.config.gui.widget;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import me.chrr.tapestry.config.Controller;
import me.chrr.tapestry.config.gui.OptionProxy;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.ARGB;
import net.minecraft.util.CommonColors;
import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class SliderOptionWidget<N extends Number> extends OptionWidget<N> {
    private static final int SLIDER_WIDTH = 96;

    private int sliderMinX = 0;
    private int sliderMaxX = 0;
    private boolean isSliding = false;

    public final N min;
    public final N max;
    public final N step;

    public SliderOptionWidget(OptionProxy<N> optionProxy, Controller.Slider<N> controller) {
        super(optionProxy);

        this.min = controller.min;
        this.max = controller.max;
        this.step = controller.step;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.isFocused()) {
            switch (event.key()) {
                case InputConstants.KEY_LEFT -> {
                    optionProxy.value = incrementBySteps(optionProxy.value, -1);
                    return true;
                }
                case InputConstants.KEY_RIGHT -> {
                    optionProxy.value = incrementBySteps(optionProxy.value, 1);
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean isDoubleClick) {
        this.isSliding = event.button() == InputConstants.MOUSE_BUTTON_LEFT
                && event.x() >= sliderMinX && event.x() <= sliderMaxX;
        this.updateSlider(event);
    }

    @Override
    public void onRelease(MouseButtonEvent event) {
        this.isSliding = false;
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double mouseX, double mouseY) {
        this.updateSlider(event);
    }

    protected void updateSlider(MouseButtonEvent event) {
        if (!isSliding || event.button() != InputConstants.MOUSE_BUTTON_LEFT)
            return;

        float progress = Math.clamp(((float) event.x() - sliderMinX) / SLIDER_WIDTH, 0f, 1f);
        optionProxy.value = getValueFromProgress(progress);
    }

    @Override
    protected void renderOptionWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        int sliderWidth = this.isHoveredOrFocused() ? SLIDER_WIDTH : SLIDER_WIDTH / 4;
        int availableWidth = this.getWidth() - sliderWidth - 4;
        this.renderOptionLabel(graphics, availableWidth);
        this.renderValueLabel(graphics, sliderWidth + 4, availableWidth);

        int color = CommonColors.WHITE;
        int shadow = ARGB.scaleRGB(color, 0.25F);

        sliderMaxX = this.getRight() - 2 - 4;
        sliderMinX = sliderMaxX - sliderWidth;
        int sliderY = this.getY() + this.getHeight() / 2;

        int thumbWidth = 2;
        int thumbHeight = this.getHeight() - (2 + 4) * 2;
        float progress = Math.clamp(getProgressFromValue(optionProxy.value), 0f, 1f);
        int thumbX = sliderMinX + (int) (sliderWidth * progress) - thumbWidth / 2;
        int thumbY = this.getY() + 2 + 4;

        graphics.fill(sliderMinX + 1, sliderY + 1, sliderMaxX + 1, sliderY + 2, shadow);
        graphics.fill(sliderMinX, sliderY, sliderMaxX, sliderY + 1, color);

        graphics.fill(thumbX + 1, thumbY + 1, thumbX + thumbWidth + 1, thumbY + thumbHeight + 1, shadow);
        graphics.fill(thumbX, thumbY, thumbX + thumbWidth, thumbY + thumbHeight, color);

        if (isSliding || (this.isHovered() && mouseX >= sliderMinX && mouseX <= sliderMaxX)) {
            graphics.requestCursor(this.isActive() ? CursorTypes.RESIZE_EW : CursorTypes.NOT_ALLOWED);
        }
    }

    protected abstract float getProgressFromValue(N value);

    protected abstract N getValueFromProgress(float progress);

    protected abstract N incrementBySteps(N value, int steps);


    public static class Int extends SliderOptionWidget<Integer> {
        public Int(OptionProxy<Integer> optionProxy, Controller.Slider<Integer> controller) {
            super(optionProxy, controller);
        }

        @Override
        protected float getProgressFromValue(Integer value) {
            return ((float) value - this.min) / ((float) this.max - this.min);
        }

        @Override
        protected Integer getValueFromProgress(float progress) {
            return this.min + (int) (progress * (this.max - this.min)) / step * step;
        }

        @Override
        protected Integer incrementBySteps(Integer value, int steps) {
            return Math.clamp(value + (long) this.step * steps, this.min, this.max);
        }
    }

    public static class Float extends SliderOptionWidget<java.lang.Float> {
        public Float(OptionProxy<java.lang.Float> optionProxy, Controller.Slider<java.lang.Float> controller) {
            super(optionProxy, controller);
        }

        @Override
        protected float getProgressFromValue(java.lang.Float value) {
            return (value - this.min) / (this.max - this.min);
        }

        @Override
        protected java.lang.Float getValueFromProgress(float progress) {
            return this.min + Math.round(progress * (this.max - this.min) / step) * step;
        }

        @Override
        protected java.lang.Float incrementBySteps(java.lang.Float value, int steps) {
            return Math.clamp(value + this.step * steps, this.min, this.max);
        }
    }
}
