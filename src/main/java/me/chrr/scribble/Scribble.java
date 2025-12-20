package me.chrr.scribble;

import me.chrr.scribble.book.FileChooser;
import me.chrr.tapestry.config.gui.TapestryConfigScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

@NullMarked
public class Scribble {
    public static final String MOD_ID = "scribble";
    public static final Logger LOGGER = LogManager.getLogger();

    private static @Nullable Platform PLATFORM;

    public static void init(Platform platform) {
        PLATFORM = platform;
        ScribbleConfig.INSTANCE.ensureLoaded();

        try {
            FileChooser.convertLegacyBooks();
        } catch (IOException e) {
            LOGGER.error("failed to convert legacy NBT-based book files to JSON", e);
        }
    }

    public static Screen buildConfigScreen(Screen parent) {
        return new TapestryConfigScreen(ScribbleConfig.INSTANCE, parent);
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
