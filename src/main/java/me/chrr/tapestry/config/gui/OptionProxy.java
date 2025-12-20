package me.chrr.tapestry.config.gui;

import me.chrr.tapestry.config.Option;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class OptionProxy<T> {
    public final Option<?, T> option;
    public T value;

    public OptionProxy(Option<?, T> option) {
        this.option = option;
        this.value = option.displayBinding.get();
    }

    public void apply() {
        this.option.displayBinding.set(this.value);
    }

    public void reset() {
        this.value = this.option.defaultDisplayValue;
    }

    public boolean isChanged() {
        return this.value != this.option.defaultDisplayValue;
    }
}
