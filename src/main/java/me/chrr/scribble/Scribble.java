package me.chrr.scribble;

import me.chrr.scribble.config.ConfigManager;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class Scribble {
    public static final String MOD_ID = "scribble";
    public static Logger LOGGER = LogManager.getLogger();

    public static final ConfigManager CONFIG_MANAGER = new ConfigManager();

    public static void init() {
        try {
            CONFIG_MANAGER.load();
        } catch (IOException e) {
            LOGGER.error("failed to load config", e);
        }
    }

    public static int getBookScreenYOffset(int screenHeight) {
        return Scribble.CONFIG_MANAGER.getConfig().centerBookGui ? (screenHeight - 192) / 3 : 0;
    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
}
