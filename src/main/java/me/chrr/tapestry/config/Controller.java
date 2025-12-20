package me.chrr.tapestry.config;

import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NullMarked;

import java.util.List;

@NullMarked
public sealed class Controller {
    public static final class EnumValues<T> extends Controller {
        public final List<Value<T>> options;

        public EnumValues(List<Value<T>> options) {
            this.options = options;
        }

        public record Value<T>(T value, Component name) {
        }
    }

    public static final class Slider<N extends Number> extends Controller {
        public final N min;
        public final N max;
        public final N step;

        public Slider(N min, N max, N step) {
            this.min = min;
            this.max = max;
            this.step = step;
        }
    }
}
