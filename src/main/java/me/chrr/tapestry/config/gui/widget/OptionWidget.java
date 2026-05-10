package me.chrr.tapestry.config.gui.widget;

import me.chrr.tapestry.config.gui.OptionProxy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.WidgetTooltipHolder;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.NullMarked;

@NullMarked
public abstract class OptionWidget<T> extends AbstractWidget {
    private static final WidgetSprites SPRITES = new WidgetSprites(
            Identifier.withDefaultNamespace("widget/button"),
            Identifier.withDefaultNamespace("widget/button_disabled"),
            Identifier.withDefaultNamespace("widget/button_highlighted")
    );

    public final OptionProxy<T> optionProxy;
    public final WidgetTooltipHolder customTooltip;

    public OptionWidget(OptionProxy<T> optionProxy) {
        super(0, 0, 0, 0, optionProxy.option.displayName);

        this.optionProxy = optionProxy;
        this.customTooltip = new WidgetTooltipHolder();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }

    protected void renderOptionLabel(GuiGraphics graphics, int availableWidth) {
        ActiveTextCollector textCollector = graphics.textRendererForWidget(this, GuiGraphics.HoveredTextEffects.NONE);
        textCollector.acceptScrolling(this.getMessage(),
                this.getX() + 2 + 4, this.getX() + 2 + 4, this.getX() + availableWidth - 2 - 4,
                this.getY() + 2, this.getBottom() - 2);
    }

    protected void renderValueLabel(GuiGraphics graphics, int rightOffset, int availableWidth) {
        this.renderValueLabel(graphics, rightOffset, availableWidth, optionProxy.option.value.textProvider.apply(optionProxy.value));
    }

    protected void renderValueLabel(GuiGraphics graphics, int rightOffset, int availableWidth, Component value) {
        ActiveTextCollector textCollector = graphics.textRendererForWidget(this, GuiGraphics.HoveredTextEffects.NONE);
        textCollector.acceptScrolling(value,
                this.getRight() - rightOffset - 2 - 4,
                this.getRight() - rightOffset - availableWidth + 2 + 4,
                this.getRight() - rightOffset - 2 - 4,
                this.getY() + 2, this.getBottom() - 2);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED, SPRITES.get(this.active, this.isHoveredOrFocused()),
                this.getX(), this.getY(), this.getWidth(), this.getHeight(),
                ARGB.white(this.alpha));
        this.renderOptionWidget(graphics, mouseX, mouseY, partialTick);
    }

    protected abstract void renderOptionWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta);

    public static abstract class Clickable<T> extends OptionWidget<T> {
        public Clickable(OptionProxy<T> optionProxy) {
            super(optionProxy);
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean isDoubleClick) {
            this.onPress(event);
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            if (!this.isActive()) {
                return false;
            } else if (event.isSelection()) {
                this.playDownSound(Minecraft.getInstance().getSoundManager());
                this.onPress(event);
                return true;
            } else {
                return false;
            }
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
            this.handleCursor(graphics);
        }

        public abstract void onPress(InputWithModifiers input);
    }
}
