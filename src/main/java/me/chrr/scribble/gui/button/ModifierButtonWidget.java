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

import java.util.function.Consumer;

/**
 * A toggle-able button that's used in the book edit screen for toggling
 * text modifiers. It always uses `gui/scribble_widgets.png` as texture.
 */
@NullMarked
public class ModifierButtonWidget extends AbstractButton {
    private static final Identifier WIDGETS_TEXTURE = Scribble.id("textures/gui/scribble_widgets.png");

    private final int u;
    private final int v;

    public boolean toggled = false;
    private final Consumer<Boolean> onToggle;

    public ModifierButtonWidget(Component tooltip, Consumer<Boolean> onToggle, int x, int y, int u, int v, int width, int height) {
        super(x, y, width, height, tooltip);
        this.setTooltip(Tooltip.create(tooltip));

        this.u = u;
        this.v = v;
        this.onToggle = onToggle;
    }

    @Override
    protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // If the button is hovered or focused, we want it to be in front, so we slightly increase the height.
        int offset = this.isHoveredOrFocused() ? 1 : 0;

        int u = this.u + (this.isHoveredOrFocused() ? 22 : 0) + (this.toggled ? 44 : 0);
        graphics.blit(RenderPipelines.GUI_TEXTURED, WIDGETS_TEXTURE, getX(), getY(), u, v, width, height + offset, 128, 128);
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
