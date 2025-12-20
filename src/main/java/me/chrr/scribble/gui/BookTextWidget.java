package me.chrr.scribble.gui;

import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.*;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.function.Consumer;

// FIXME: is there a non-interactable-but-clickable abstract widget class I can use here?
//        AbstractStringWidget is a thing, though I'm not sure it does what I need to.
@NullMarked
public class BookTextWidget implements TextArea<Component> {
    public static final Style PAGE_TEXT_STYLE = Style.EMPTY.withoutShadow().withColor(0xff000000);

    private List<FormattedCharSequence> lines = List.of();
    private Component text = Component.empty();

    private boolean visible = true;
    private boolean hovered = false;

    private final int x;
    private final int y;
    private final int width;
    private final int height;

    private final Font font;
    private final Consumer<ClickEvent> handleClickEvent;

    public BookTextWidget(int x, int y, int width, int height, Font font, Consumer<ClickEvent> handleClickEvent) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        this.font = font;
        this.handleClickEvent = handleClickEvent;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        if (!visible)
            return;

        this.hovered = guiGraphics.containsPointInScissor(mouseX, mouseY)
                && this.areCoordinatesInRectangle(mouseX, mouseY);
        this.visitText(guiGraphics.textRenderer(GuiGraphics.HoveredTextEffects.TOOLTIP_AND_CURSOR));
    }

    private void visitText(ActiveTextCollector activeTextCollector) {
        int lines = Math.min(this.height / this.font.lineHeight, this.lines.size());
        for (int i = 0; i < lines; ++i) {
            activeTextCollector.accept(this.x, this.y + i * font.lineHeight, this.lines.get(i));
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean dbl) {
        if (event.button() == 0) {
            ActiveTextCollector.ClickableStyleFinder clickableStyleFinder
                    = new ActiveTextCollector.ClickableStyleFinder(this.font, (int) event.x(), (int) event.y());
            this.visitText(clickableStyleFinder);

            Style style = clickableStyleFinder.result();
            if (style != null && style.getClickEvent() != null) {
                this.handleClickEvent.accept(style.getClickEvent());
                return true;
            }
        }

        return TextArea.super.mouseClicked(event, dbl);
    }

    @Override
    public void setText(Component text) {
        this.text = text;

        FormattedText formattedText = ComponentUtils.mergeStyles(text, PAGE_TEXT_STYLE);
        this.lines = this.font.split(formattedText, this.width);
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public NarrationPriority narrationPriority() {
        return this.hovered ? NarrationPriority.HOVERED : NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE, this.text);
    }

    private boolean areCoordinatesInRectangle(double x, double y) {
        return x >= this.x && y >= this.y && x < this.x + this.width && y < this.y + this.height;
    }

    @Override
    public boolean isMouseOver(double x, double y) {
        return this.isActive() && this.areCoordinatesInRectangle(x, y);
    }

    @Override
    public ScreenRectangle getRectangle() {
        return new ScreenRectangle(this.x, this.y, this.width, this.height);
    }

    @Override
    public boolean shouldTakeFocusAfterInteraction() {
        return false;
    }

    @Override
    public boolean isActive() {
        return this.visible;
    }

    @Override
    public void setFocused(boolean bl) {
        // no-op: it's unfocusable.
    }

    @Override
    public boolean isFocused() {
        return false;
    }
}
