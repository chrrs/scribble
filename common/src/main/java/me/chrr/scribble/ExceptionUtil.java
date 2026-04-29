package me.chrr.scribble;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
                out.append(Component.literal("caused by: ").withStyle(ChatFormatting.GRAY));
            }

            String header = cause.getClass().getName() + ": " + cause.getMessage();
            out.append(Component.literal(header).withStyle(ChatFormatting.GRAY));

            Optional<String> source = Arrays.stream(cause.getStackTrace())
                    .map(StackTraceElement::toString)
                    .filter(s -> s.contains("me.chrr."))
                    .findFirst();
            source.ifPresent(it -> out.append(Component.literal("\nat " + it).withStyle(ChatFormatting.DARK_GRAY)));

            cause = cause.getCause();
        }

        return out;
    }
}
