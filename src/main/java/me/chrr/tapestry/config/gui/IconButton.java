package me.chrr.tapestry.config.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.CommonColors;
import org.jspecify.annotations.NullMarked;

import java.util.function.Consumer;

@NullMarked
public class IconButton extends AbstractButton {
    private final Consumer<IconButton> onPress;
    private final Identifier icon;

    public IconButton(int x, int y, int width, int height, Component tooltip, Identifier icon, Consumer<IconButton> onPress) {
        super(x, y, width, height, tooltip);
        this.onPress = onPress;
        this.icon = icon;

        setTooltip(Tooltip.create(tooltip));
    }

    @Override
    public void onPress(InputWithModifiers input) {
        this.onPress.accept(this);
    }

    @Override
    protected void renderContents(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderDefaultSprite(guiGraphics);

        int color = this.isActive() ? CommonColors.WHITE : CommonColors.LIGHT_GRAY;
        int shadow = ARGB.scaleRGB(color, 0.25F);

        int x = getX() + getWidth() / 2 - 16 / 2;
        int y = getY() + getHeight() / 2 - 16 / 2;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, icon, x + 1, y + 1, 0, 0, 16, 16, 16, 16, shadow);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, icon, x, y, 0, 0, 16, 16, 16, 16, color);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
