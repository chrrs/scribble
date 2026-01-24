package me.chrr.tapestry.config.gui;

import me.chrr.tapestry.config.Option;
import me.chrr.tapestry.config.gui.widget.*;
import me.chrr.tapestry.config.value.Constraint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NullMarked;

import java.util.List;

@NullMarked
public class OptionList extends ContainerObjectSelectionList<OptionList.Entry> {
    private final boolean showHeaderSeparator;

    public OptionList(Minecraft minecraft, boolean showHeaderSeparator, int width, int height, int y) {
        super(minecraft, width, height, y, 25);

        this.showHeaderSeparator = showHeaderSeparator;
        this.centerListVertically = false;
    }

    public void addHeader(Component text) {
        int padding = this.children().isEmpty() ? 8 : 16;
        int height = padding + OptionList.this.minecraft.font.lineHeight;
        this.addEntry(new HeaderEntry(text), height);
    }

    public <T> OptionProxy<T> addOption(Option<T> option) {
        OptionProxy<T> proxy = new OptionProxy<>(option);
        this.addEntry(new OptionEntry<>(proxy));
        return proxy;
    }

    @Override
    public int getRowWidth() {
        return 310;
    }

    @Override
    protected void renderListSeparators(GuiGraphics guiGraphics) {
        if (this.showHeaderSeparator) {
            Identifier headerSeparator = this.minecraft.level == null ? Screen.HEADER_SEPARATOR : Screen.INWORLD_HEADER_SEPARATOR;
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, headerSeparator, this.getX(), this.getY() - 2, 0f, 0f, this.getWidth(), 2, 32, 2);
        }

        Identifier footerSeparator = this.minecraft.level == null ? Screen.FOOTER_SEPARATOR : Screen.INWORLD_FOOTER_SEPARATOR;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, footerSeparator, this.getX(), this.getBottom(), 0f, 0f, this.getWidth(), 2, 32, 2);
    }

    private static <T> OptionWidget<T> getWidgetForProxy(OptionProxy<T> optionProxy) {
        Class<T> valueClass = optionProxy.option.value.getValueType();

        if (valueClass == boolean.class || valueClass == Boolean.class) {
            return unsafeCast(new BooleanOptionWidget(unsafeCast(optionProxy)));
        } else if (optionProxy.option.value.constraint instanceof Constraint.Range<T> range && range.step() != null) {
            if ((valueClass == int.class || valueClass == Integer.class)) {
                return unsafeCast(new SliderOptionWidget.Int(unsafeCast(optionProxy), unsafeCast(range)));
            } else if ((valueClass == float.class || valueClass == Float.class)) {
                return unsafeCast(new SliderOptionWidget.Float(unsafeCast(optionProxy), unsafeCast(range)));
            } else {
                throw new IllegalArgumentException("Range constraint can't be applied to a value of type " + valueClass);
            }
        } else if (optionProxy.option.value.constraint instanceof Constraint.Values<T>(List<T> values)) {
            return new EnumOptionWidget<>(optionProxy, values);
        } else {
            return new ReadOnlyOptionWidget<>(optionProxy);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T, V> T unsafeCast(V value) {
        return (T) value;
    }

    protected abstract static class Entry extends ContainerObjectSelectionList.Entry<Entry> {
    }

    protected class HeaderEntry extends Entry {
        private final StringWidget widget;

        public HeaderEntry(Component text) {
            this.widget = new StringWidget(text, minecraft.font);
        }

        public List<? extends NarratableEntry> narratables() {
            return List.of(this.widget);
        }

        public void renderContent(GuiGraphics graphics, int x, int y, boolean bl, float delta) {
            this.widget.setPosition(this.getContentX() + 2, this.getContentBottom() - minecraft.font.lineHeight);
            this.widget.render(graphics, x, y, delta);
        }

        public List<? extends GuiEventListener> children() {
            return List.of(this.widget);
        }
    }

    protected static class OptionEntry<T> extends Entry {
        private final OptionWidget<T> widget;
        private final OptionProxy<T> optionProxy;
        private final IconButton reset;

        public OptionEntry(OptionProxy<T> optionProxy) {
            this.widget = getWidgetForProxy(optionProxy);
            this.optionProxy = optionProxy;

            this.reset = new IconButton(0, 0, 0, 0,
                    Component.translatable("text.tapestry.config.reset"),
                    Identifier.fromNamespaceAndPath("tapestry-config", "textures/gui/reset.png"),
                    (button) -> optionProxy.reset());
        }

        public List<? extends NarratableEntry> narratables() {
            return List.of(this.widget, this.reset);
        }

        public void renderContent(GuiGraphics graphics, int x, int y, boolean isHovering, float delta) {
            this.widget.setPosition(this.getContentX(), this.getContentY());
            this.widget.setSize(this.getContentWidth() - this.getContentHeight() - 4, this.getContentHeight());
            this.widget.render(graphics, x, y, delta);

            this.reset.active = this.optionProxy.isChanged();
            this.reset.setPosition(this.getContentRight() - this.getContentHeight(), this.getContentY());
            this.reset.setSize(this.getContentHeight(), this.getContentHeight());
            this.reset.render(graphics, x, y, delta);
        }

        public List<? extends GuiEventListener> children() {
            return List.of(this.widget, this.reset);
        }
    }
}
