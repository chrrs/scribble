package me.chrr.scribble.text;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Set;

@NullMarked
public enum ChatFormattingUtil {
    ;

    public static boolean isFormat(ChatFormatting formatting) {
        return formatting == ChatFormatting.BOLD ||
                formatting == ChatFormatting.ITALIC ||
                formatting == ChatFormatting.UNDERLINE ||
                formatting == ChatFormatting.STRIKETHROUGH ||
                formatting == ChatFormatting.OBFUSCATED;
    }

    public static @Nullable ChatFormatting getByName(String name) {
        try {
            return ChatFormatting.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static Style toStyle(@Nullable ChatFormatting color, Set<ChatFormatting> modifiers) {
        Style style = Style.EMPTY;
        if (color != null) style = style.applyFormat(color);
        for (ChatFormatting modifier : modifiers) style = style.applyFormat(modifier);
        return style;
    }
}
