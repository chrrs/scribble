package me.chrr.scribble.gui.button;

import me.chrr.scribble.Scribble;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class IconButtonWidget extends AbstractButton {
    private static final Identifier WIDGETS_TEXTURE = Scribble.id("textures/gui/scribble_widgets.png");

    private final Runnable onPress;

    private final int u;
    private final int v;

    public IconButtonWidget(Component tooltip, Runnable onPress, int x, int y, int u, int v, int width, int height) {
        super(x, y, width, height, tooltip);
        this.setTooltip(Tooltip.create(tooltip));
        this.onPress = onPress;

        this.u = u;
        this.v = v;
    }

    @Override
    protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        int v = this.v + (!this.active ? 2 : (this.isHoveredOrFocused() ? 1 : 0)) * this.height;
        graphics.blit(RenderPipelines.GUI_TEXTURED, WIDGETS_TEXTURE, getX(), getY(), this.u, v, this.width, this.height, 128, 128);
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
        onPress.run();
    }
}
