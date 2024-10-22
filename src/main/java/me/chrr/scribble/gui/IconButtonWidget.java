package me.chrr.scribble.gui;

import me.chrr.scribble.Scribble;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

//? if >=1.21.2
import net.minecraft.client.render.RenderLayer;

public class IconButtonWidget extends ClickableWidget {
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
    //$ renderWidget
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        int u = isSelected() ? this.u + this.width : this.u;

        //? if >=1.21.2 {
        context.drawTexture(RenderLayer::getGuiTextured, WIDGETS_TEXTURE, getX(), getY(), u, this.v, this.width, this.height, 128, 128);
         //?} else
        /*context.drawTexture(WIDGETS_TEXTURE, getX(), getY(), u, this.v, this.width, this.height, 128, 128);*/
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        this.appendDefaultNarrations(builder);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        this.onPress.run();
    }
}
