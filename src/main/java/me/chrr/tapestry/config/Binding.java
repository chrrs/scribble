package me.chrr.tapestry.config;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface Binding<T> {
    static <T> Binding<T> of(Class<T> valueClass, Supplier<T> getter, Consumer<T> setter) {
        return new Binding<>() {
            @Override
            public Class<T> getValueClass() {
                return valueClass;
            }

            @Override
            public T get() {
                return getter.get();
            }

            @Override
            public void set(T value) {
                setter.accept(value);
            }
        };
    }

    Class<T> getValueClass();

    T get();

    void set(T value);
}
