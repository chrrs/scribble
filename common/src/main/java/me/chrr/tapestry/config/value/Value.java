package me.chrr.tapestry.config.value;

import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;

@NullMarked
public abstract class Value<T> implements Supplier<T> {
    public Function<T, Component> textProvider = (v) -> Component.literal(v.toString());
    public @Nullable Constraint<T> constraint;

    public boolean didSetTextProvider = false;

    public Value<T> range(T min, T max) {
        this.constraint = new Constraint.Range<>(min, max, null);
        return this;
    }

    public Value<T> range(T min, T max, T step) {
        this.constraint = new Constraint.Range<>(min, max, step);
        return this;
    }

    public Value<T> textProvider(Function<T, Component> f) {
        this.didSetTextProvider = true;
        this.textProvider = f;
        return this;
    }

    public abstract void set(T value);

    public abstract T getDefaultValue();

    public abstract Class<T> getValueType();
}
