package me.chrr.scribble;

import me.chrr.scribble.config.ConfigManager;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class Scribble implements ClientModInitializer {
    public static final String MOD_ID = "scribble";
    public static Logger LOGGER = LogManager.getLogger();

    public static final ConfigManager CONFIG_MANAGER = new ConfigManager();

    @Override
    public void onInitializeClient() {
        try {
            CONFIG_MANAGER.load();
        } catch (IOException e) {
            LOGGER.error("failed to load config", e);
        }
    }

    public static Identifier id(String path) {
        //? if >=1.21 {
        return Identifier.of(MOD_ID, path);
        //?} else
        /*return new Identifier(MOD_ID, path);*/
    }
}
