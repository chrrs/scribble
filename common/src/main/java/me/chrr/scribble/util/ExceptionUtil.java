package me.chrr.scribble.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.jspecify.annotations.NullMarked;

import java.util.Arrays;
import java.util.Optional;

@NullMarked
public class ExceptionUtil {
    private ExceptionUtil() {
    }

    public static Component getExceptionSummary(Throwable throwable) {
        MutableComponent out = Component.empty();

        boolean first = true;
        Throwable cause = throwable;
        while (cause != null) {
            if (first) {
                first = false;
            } else {
                out.append("\n\n");
                out.append(Component.literal("caused by: ")
                        .withStyle(Style.EMPTY.withColor(TextColor.GRAY)));
            }

            String header = cause.getClass().getName() + ": " + cause.getMessage();
            out.append(Component.literal(header)
                    .withStyle(Style.EMPTY.withColor(TextColor.GRAY)));

            Optional<String> source = Arrays.stream(cause.getStackTrace())
                    .map(StackTraceElement::toString)
                    .filter(s -> s.contains("me.chrr."))
                    .findFirst();
            source.ifPresent(it -> out.append(Component.literal("\nat " + it)
                    .withStyle(Style.EMPTY.withColor(TextColor.DARK_GRAY))));

            cause = cause.getCause();
        }

        return out;
    }
}
