package me.chrr.tapestry.config.gui.widget;

import me.chrr.tapestry.config.Controller;
import me.chrr.tapestry.config.gui.OptionProxy;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NullMarked;

import java.util.*;

@NullMarked
public class EnumOptionWidget<T> extends OptionWidget.Clickable<T> {
    private final List<T> values = new ArrayList<>();
    private final Map<T, Component> nameByValue = new HashMap<>();

    public EnumOptionWidget(OptionProxy<T> optionProxy, Controller.EnumValues<T> enumValues) {
        super(optionProxy);

        for (Controller.EnumValues.Value<T> value : enumValues.options) {
            this.values.add(value.value());
            this.nameByValue.put(value.value(), value.name());
        }
    }

    @Override
    public void onPress(InputWithModifiers input) {
        optionProxy.value = values.get((values.indexOf(optionProxy.value) + 1) % values.size());
    }

    @Override
    protected void renderOptionWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderOptionLabel(graphics, this.getWidth());
        this.renderValueLabel(graphics, 0, this.getWidth(), nameByValue.get(optionProxy.value));
    }
}
