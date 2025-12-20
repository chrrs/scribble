package me.chrr.tapestry.config;

import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

@NullMarked
public class Option<T, D> {
    public final String serializeName;
    public final Component displayName;

    public final D defaultDisplayValue;

    public final Binding<T> serializeBinding;
    public final Binding<D> displayBinding;

    public @Nullable Component header = null;
    public @Nullable Controller controller = null;


    public Option(String serializeName, Component displayName, D defaultDisplayValue, Binding<T> serializeBinding, Binding<D> displayBinding) {
        this.serializeName = serializeName;
        this.displayName = displayName;
        this.defaultDisplayValue = defaultDisplayValue;
        this.serializeBinding = serializeBinding;
        this.displayBinding = displayBinding;
    }

    public static <T> Option<T, T> of(String name, T defaultValue, Binding<T> binding) {
        return new Option<>(name, Component.literal(name), defaultValue, binding, binding);
    }

    public <E> Option<T, E> mapDisplayBinding(Class<E> toClass, Function<D, E> aToB, Function<E, D> bToA) {
        Binding<E> displayBinding = Binding.of(toClass,
                () -> aToB.apply(this.displayBinding.get()),
                (value) -> this.displayBinding.set(bToA.apply(value)));
        Option<T, E> option = new Option<>(this.serializeName, this.displayName, aToB.apply(this.defaultDisplayValue),
                this.serializeBinding, displayBinding);
        option.header = this.header;
        return option;
    }
}
