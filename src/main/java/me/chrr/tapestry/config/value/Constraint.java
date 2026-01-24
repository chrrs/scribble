package me.chrr.tapestry.config.value;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;

@NullMarked
public sealed interface Constraint<T> {
    record Range<T>(T min, T max, @Nullable T step) implements Constraint<T> {
    }

    record Values<T>(List<T> values) implements Constraint<T> {
    }
}
