package me.chrr.tapestry.config.value;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class TrackedValue<T> extends Value<T> {
    private final Class<T> type;
    private final T defaultValue;
    public T value;

    public TrackedValue(Class<T> type, T defaultValue) {
        this.type = type;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    @Override
    public void set(T value) {
        this.value = value;
    }

    @Override
    public T getDefaultValue() {
        return this.defaultValue;
    }

    @Override
    public T get() {
        return this.value;
    }

    @Override
    public Class<T> getValueType() {
        return this.type;
    }
}
