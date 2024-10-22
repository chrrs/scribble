package me.chrr.scribble.gui;

import me.chrr.scribble.Scribble;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

//? if >=1.21.2
import net.minecraft.client.render.RenderLayer;

/**
 * A toggle-able button that's used in the book edit screen for toggling
 * text modifiers. It always uses `gui/scribble_widgets.png` as texture.
 */
public class ModifierButtonWidget extends ClickableWidget {
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
    //$ renderWidget
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        context.getMatrices().push();

        // If the button is hovered or focussed, we want it to be in front, so we shift the Z.
        if (this.isSelected()) {
            context.getMatrices().translate(0, 0, 1);
        }

        int u = this.u + (this.isSelected() ? 22 : 0) + (this.toggled ? 44 : 0);

        //? if >=1.21.2 {
        context.drawTexture(RenderLayer::getGuiTextured, WIDGETS_TEXTURE, getX(), getY(), u, v, width, height + 1, 128, 128);
         //?} else
        /*context.drawTexture(WIDGETS_TEXTURE, getX(), getY(), u, v, width, height + 1, 128, 128);*/

        context.getMatrices().pop();
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        this.appendDefaultNarrations(builder);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        this.toggled = !this.toggled;
        onToggle.accept(this.toggled);
    }

    public void toggle() {
        this.toggled = !this.toggled;
        onToggle.accept(this.toggled);
    }
}
