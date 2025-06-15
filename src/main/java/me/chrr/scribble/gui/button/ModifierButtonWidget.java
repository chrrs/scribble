package me.chrr.scribble.gui.button;

import me.chrr.scribble.Scribble;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

/**
 * A toggle-able button that's used in the book edit screen for toggling
 * text modifiers. It always uses `gui/scribble_widgets.png` as texture.
 */
public class ModifierButtonWidget extends PressableWidget {
    private static final Identifier WIDGETS_TEXTURE = Scribble.id("textures/gui/scribble_widgets.png");

    private final int u;
    private final int v;

    public boolean toggled;
    private final Consumer<Boolean> onToggle;

    public ModifierButtonWidget(Text tooltip, Consumer<Boolean> onToggle, int x, int y, int u, int v, int width, int height, boolean toggled) {
        super(x, y, width, height, tooltip);
        this.setTooltip(Tooltip.of(tooltip));

        this.u = u;
        this.v = v;
        this.toggled = toggled;
        this.onToggle = onToggle;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        // If the button is hovered or focused, we want it to be in front, so we shift the Z.
        if (this.isSelected()) {
            // FIXME: shift Z +1.
        }

        int u = this.u + (this.isSelected() ? 22 : 0) + (this.toggled ? 44 : 0);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, WIDGETS_TEXTURE, getX(), getY(), u, v, width, height + 1, 128, 128);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        this.appendDefaultNarrations(builder);
    }

    @Override
    public void onPress() {
        this.toggled = !this.toggled;
        onToggle.accept(this.toggled);
    }
}
