package me.chrr.scribble.util;

import net.minecraft.ChatFormatting;
import org.jspecify.annotations.Nullable;

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
}
