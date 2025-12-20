package me.chrr.tapestry.config.gui;

import me.chrr.tapestry.config.Controller;
import me.chrr.tapestry.config.Option;
import me.chrr.tapestry.config.gui.widget.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NullMarked;

import java.util.List;

@NullMarked
public class OptionList extends ContainerObjectSelectionList<OptionList.Entry> {
    public OptionList(Minecraft minecraft, int width, int height, int y) {
        super(minecraft, width, height, y, 25);

        this.centerListVertically = false;
    }

    public void addHeader(Component text) {
        int padding = this.children().isEmpty() ? 8 : 16;
        int height = padding + OptionList.this.minecraft.font.lineHeight;
        this.addEntry(new HeaderEntry(text), height);
    }

    public <T> OptionProxy<T> addOption(Option<?, T> option) {
        OptionProxy<T> proxy = new OptionProxy<>(option);
        this.addEntry(new OptionEntry<>(proxy));
        return proxy;
    }

    @Override
    public int getRowWidth() {
        return 310;
    }

    private static <T> OptionWidget<T> getWidgetForProxy(OptionProxy<T> optionProxy) {
        Class<T> valueClass = optionProxy.option.displayBinding.getValueClass();

        // FIXME: very dirty and manual replacement for an interface method, but I don't
        //        want widgets in server-side code.
        if (valueClass == boolean.class || valueClass == Boolean.class) {
            return unsafeCast(new BooleanOptionWidget(unsafeCast(optionProxy)));
        } else if ((valueClass == int.class || valueClass == Integer.class) &&
                optionProxy.option.controller instanceof Controller.Slider<?> slider) {
            return unsafeCast(new SliderOptionWidget.Int(unsafeCast(optionProxy), unsafeCast(slider)));
        } else if ((valueClass == float.class || valueClass == Float.class) &&
                optionProxy.option.controller instanceof Controller.Slider<?> slider) {
            return unsafeCast(new SliderOptionWidget.Float(unsafeCast(optionProxy), unsafeCast(slider)));
        } else if (optionProxy.option.controller instanceof Controller.EnumValues<?> enumValues) {
            return new EnumOptionWidget<>(optionProxy, unsafeCast(enumValues));
        }

        return new ReadOnlyOptionWidget<>(optionProxy);
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
        private final Button reset;

        public OptionEntry(OptionProxy<T> optionProxy) {
            this.widget = getWidgetForProxy(optionProxy);
            this.optionProxy = optionProxy;

            this.reset = Button.builder(Component.literal("✘"), (button) -> optionProxy.reset())
                    .tooltip(Tooltip.create(Component.literal("Reset to default value"))).build();
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
