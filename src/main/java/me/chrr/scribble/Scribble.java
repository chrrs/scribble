package me.chrr.scribble;

import me.chrr.scribble.book.FileChooser;
import me.chrr.scribble.config.Config;
import me.chrr.scribble.config.ConfigManager;
import me.chrr.scribble.config.YACLConfigScreenFactory;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

@NullMarked
public class Scribble {
    public static final String MOD_ID = "scribble";
    public static final Logger LOGGER = LogManager.getLogger();

    private static final ConfigManager CONFIG_MANAGER = new ConfigManager();
    private static @Nullable Platform PLATFORM;

    public static void init(Platform platform) {
        PLATFORM = platform;

        try {
            CONFIG_MANAGER.load();
        } catch (IOException e) {
            LOGGER.error("failed to load config", e);
        }

        try {
            FileChooser.convertLegacyBooks();
        } catch (IOException e) {
            LOGGER.error("failed to convert legacy NBT-based book files to JSON", e);
        }
    }

    public static Screen buildConfigScreen(Screen parent) {
        return YACLConfigScreenFactory.create(CONFIG_MANAGER, parent);
    }

    public static Platform platform() {
        return Optional.ofNullable(PLATFORM).orElseThrow();
    }

    public static Config config() {
        return CONFIG_MANAGER.getConfig();
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public abstract static class Platform {
        public final Path CONFIG_DIR = getConfigDir();
        public final Path BOOK_DIR = getGameDir().resolve("books");

        public final String VERSION = getModVersion();
        public final boolean HAS_YACL = isModLoaded("yet_another_config_lib_v3");

        protected abstract boolean isModLoaded(String modId);

        protected abstract String getModVersion();

        protected abstract Path getConfigDir();

        protected abstract Path getGameDir();
    }
}
