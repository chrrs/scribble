package me.chrr.scribble;

import me.chrr.scribble.book.FileChooser;
import me.chrr.scribble.config.ConfigManager;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;

public class Scribble {
    public static final String MOD_ID = "scribble";
    public static Logger LOGGER = LogManager.getLogger();

    public static final ConfigManager CONFIG_MANAGER = new ConfigManager();

    public static Path CONFIG_DIR = null;
    public static Path BOOK_DIR = null;

    public static void init() {
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

    public static int getBookScreenYOffset(int screenHeight) {
        return Scribble.CONFIG_MANAGER.getConfig().centerBookGui ? (screenHeight - 192) / 3 : 0;
    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
}
