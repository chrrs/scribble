package me.chrr.tapestry.config.reflect;

import java.util.ArrayList;
import java.util.List;

public enum NamingStrategy {
    KEEP,
    SNAKE_CASE,
    CAMEL_CASE,
    SPACED_PASCAL;

    public String transform(String name) {
        return switch (this) {
            case KEEP -> name;
            case SNAKE_CASE -> String.join("_", splitName(name)).toLowerCase();
            case CAMEL_CASE -> {
                StringBuilder builder = new StringBuilder();
                List<String> words = splitName(name);
                builder.append(words.getFirst());
                words.stream().skip(1).forEach((word) -> builder.append(capitalize(word)));
                yield builder.toString();
            }
            case SPACED_PASCAL -> String.join(" ", splitName(name).stream().map(NamingStrategy::capitalize).toList());
        };
    }

    private static List<String> splitName(String name) {
        List<String> words = new ArrayList<>();
        StringBuilder currentWord = new StringBuilder();

        boolean[] inWord = new boolean[]{false};
        name.codePoints().forEach(codePoint -> {
            if (Character.isUpperCase(codePoint) && inWord[0]) {
                words.add(currentWord.toString());
                currentWord.setLength(0);
                currentWord.appendCodePoint(codePoint);
                inWord[0] = false;
            } else {
                currentWord.appendCodePoint(codePoint);
                inWord[0] = true;
            }
        });

        words.add(currentWord.toString());
        return words;
    }

    private static String capitalize(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
