package me.chrr.scribble.gui.button;

import me.chrr.scribble.Scribble;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.input.AbstractInput;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class IconButtonWidget extends PressableWidget {
    private static final Identifier WIDGETS_TEXTURE = Scribble.id("textures/gui/scribble_widgets.png");

    private final Runnable onPress;

    private final int u;
    private final int v;

    public IconButtonWidget(Text tooltip, Runnable onPress, int x, int y, int u, int v, int width, int height) {
        super(x, y, width, height, tooltip);
        this.setTooltip(Tooltip.of(tooltip));
        this.onPress = onPress;

        this.u = u;
        this.v = v;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        int v = this.v + (!this.active ? 2 : (this.isSelected() ? 1 : 0)) * this.height;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, WIDGETS_TEXTURE, getX(), getY(), this.u, v, this.width, this.height, 128, 128);
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
        this.onPress.run();
    }
}
