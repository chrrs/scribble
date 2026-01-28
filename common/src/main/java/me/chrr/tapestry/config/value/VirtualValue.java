package me.chrr.tapestry.config.value;

import org.jspecify.annotations.NullMarked;

import java.util.function.Consumer;
import java.util.function.Supplier;

@NullMarked
public class VirtualValue<T> extends Value<T> {
    private final Class<T> type;
    private final T defaultValue;
    private final Supplier<T> getter;
    private final Consumer<T> setter;

    public VirtualValue(Class<T> type, T defaultValue, Supplier<T> getter, Consumer<T> setter) {
        this.type = type;
        this.defaultValue = defaultValue;
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public void set(T value) {
        this.setter.accept(value);
    }

    @Override
    public T getDefaultValue() {
        return this.defaultValue;
    }

    @Override
    public T get() {
        return this.getter.get();
    }

    @Override
    public Class<T> getValueType() {
        return this.type;
    }
}
