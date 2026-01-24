package me.chrr.tapestry.config.gui;

import me.chrr.tapestry.config.Option;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class OptionProxy<T> {
    public final Option<T> option;
    public T value;

    public OptionProxy(Option<T> option) {
        this.option = option;
        this.value = option.value.get();
    }

    public void apply() {
        this.option.value.set(this.value);
    }

    public void reset() {
        this.value = this.option.value.getDefaultValue();
    }

    public boolean isChanged() {
        return this.value != this.option.value.getDefaultValue();
    }
}
