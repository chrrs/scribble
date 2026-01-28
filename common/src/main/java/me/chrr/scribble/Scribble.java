package me.chrr.scribble;

import me.chrr.scribble.book.FileChooser;
import me.chrr.tapestry.config.ReflectedConfig;
import me.chrr.tapestry.config.gui.TapestryConfigScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

@NullMarked
public class Scribble {
    public static final String MOD_ID = "scribble";
    public static final Logger LOGGER = LogManager.getLogger();

    private static @Nullable Platform PLATFORM;
    private static @Nullable ScribbleConfig CONFIG;

    public static void init(Platform platform) {
        PLATFORM = platform;

        CONFIG = ReflectedConfig.load(() -> platform.CONFIG_DIR, ScribbleConfig.class,
                "scribble.client.json", List.of("scribble.json"));

        FileChooser.convertLegacyBooks();
    }

    public static Screen buildConfigScreen(Screen parent) {
        return new TapestryConfigScreen(parent, config());
    }

    public static ScribbleConfig config() {
        return Objects.requireNonNull(CONFIG);
    }

    public static Platform platform() {
        return Objects.requireNonNull(PLATFORM);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public abstract static class Platform {
        public final Path CONFIG_DIR = getConfigDir();
        public final Path BOOK_DIR = getGameDir().resolve("books");

        public final String VERSION = getModVersion();

        protected abstract String getModVersion();

        protected abstract Path getConfigDir();

        protected abstract Path getGameDir();
    }
}
