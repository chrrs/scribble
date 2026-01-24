package me.chrr.tapestry.config;

import me.chrr.tapestry.config.value.Value;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class Option<T> {
    public @Nullable String serializedName;
    public Value<T> value;

    public Component displayName = Component.empty();
    public @Nullable Component header;
    public boolean hidden;

    public Option(Value<T> value) {
        this.value = value;
    }
}
