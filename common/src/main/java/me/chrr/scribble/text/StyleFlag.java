package me.chrr.scribble.text;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;
import org.jspecify.annotations.NullMarked;

import java.util.EnumSet;

@NullMarked
public enum StyleFlag {
    Bold(ChatFormatting.BOLD),
    Italic(ChatFormatting.ITALIC),
    Underline(ChatFormatting.UNDERLINE),
    Obfuscated(ChatFormatting.OBFUSCATED),
    Strikethrough(ChatFormatting.STRIKETHROUGH);

    private final ChatFormatting legacyFormat;

    StyleFlag(ChatFormatting legacyFormat) {
        this.legacyFormat = legacyFormat;
    }

    public static EnumSet<StyleFlag> fromStyle(Style style) {
        EnumSet<StyleFlag> flags = EnumSet.noneOf(StyleFlag.class);

        for (StyleFlag flag : StyleFlag.values())
            if (flag.isPresent(style))
                flags.add(flag);

        return flags;
    }

    public static Style retainFlags(Style style, EnumSet<StyleFlag> flags) {
        for (StyleFlag flag : EnumSet.complementOf(flags))
            style = flag.apply(style, false);
        return style;
    }

    public boolean isPresent(Style style) {
        if (this == StyleFlag.Bold) return style.isBold();
        if (this == StyleFlag.Italic) return style.isItalic();
        if (this == StyleFlag.Underline) return style.isUnderlined();
        if (this == StyleFlag.Obfuscated) return style.isObfuscated();
        if (this == StyleFlag.Strikethrough) return style.isStrikethrough();
        return false;
    }

    public Style apply(Style style, boolean active) {
        if (this == StyleFlag.Bold) return style.withBold(active ? true : null);
        if (this == StyleFlag.Italic) return style.withItalic(active ? true : null);
        if (this == StyleFlag.Underline) return style.withUnderlined(active ? true : null);
        if (this == StyleFlag.Obfuscated) return style.withObfuscated(active ? true : null);
        if (this == StyleFlag.Strikethrough) return style.withStrikethrough(active ? true : null);
        return style;
    }

    public ChatFormatting getLegacyFormat() {
        return legacyFormat;
    }
}
