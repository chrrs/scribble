package me.chrr.scribble.gui.button;

import me.chrr.scribble.Scribble;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

/**
 * A toggle-able button that's used in the book edit screen for toggling
 * text modifiers. It always uses `gui/scribble_widgets.png` as texture.
 */
public class ModifierButtonWidget extends AbstractButton {
    private static final ResourceLocation WIDGETS_TEXTURE = Scribble.resource("textures/gui/scribble_widgets.png");

    private final int u;
    private final int v;

    public boolean toggled;
    private final Consumer<Boolean> onToggle;

    public ModifierButtonWidget(Component tooltip, Consumer<Boolean> onToggle, int x, int y, int u, int v, int width, int height, boolean toggled) {
        super(x, y, width, height, tooltip);
        this.setTooltip(Tooltip.create(tooltip));

        this.u = u;
        this.v = v;
        this.toggled = toggled;
        this.onToggle = onToggle;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // If the button is hovered or focused, we want it to be in front, so we shift the Z.
        if (this.isHoveredOrFocused()) {
            // FIXME: shift Z +1.
        }

        int u = this.u + (this.isHoveredOrFocused() ? 22 : 0) + (this.toggled ? 44 : 0);
        graphics.blit(RenderPipelines.GUI_TEXTURED, WIDGETS_TEXTURE, getX(), getY(), u, v, width, height + 1, 128, 128);
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
        this.toggled = !this.toggled;
        onToggle.accept(this.toggled);
    }
}
