package me.chrr.tapestry.config;

import com.google.gson.*;
import me.chrr.tapestry.config.value.Value;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@NullMarked
public enum ConfigIo {
    ;

    private static final Logger LOGGER = LogManager.getLogger("Tapestry/ConfigIo");

    public static final Gson GSON = new GsonBuilder()
            .setStrictness(Strictness.LENIENT)
            .setFormattingStyle(FormattingStyle.PRETTY)
            .create();

    public static void loadFromPathOrSaveDefault(Config config, Path file, List<Path> aliases) {
        if (Files.exists(file)) {
            loadFromPath(config, file);
            return;
        } else {
            for (Path alias : aliases) {
                if (!Files.isRegularFile(alias))
                    continue;

                loadFromPath(config, alias);
                saveToPath(config, file);

                try {
                    Files.delete(alias);
                } catch (IOException e) {
                    LOGGER.error("Failed to delete old config file at '{}'", alias, e);
                }

                LOGGER.info("Migrated config from '{}' to '{}'", alias, file);
                return;
            }
        }

        LOGGER.info("No config file found, saving default config to '{}'", file);
        saveToPath(config, file);
    }

    public static void loadFromPath(Config config, Path path) {
        try {
            JsonObject object = JsonParser.parseString(Files.readString(path)).getAsJsonObject();

            UpgradeRewriter upgradeRewriter = config.getUpgradeRewriter();
            if (upgradeRewriter != null && object.has("version")) {
                int version = object.get("version").getAsInt();
                upgradeRewriter.upgrade(version, object);
                object.addProperty("version", upgradeRewriter.getLatestVersion());
                LOGGER.info("Upgraded config at '{}' to version {}", path, upgradeRewriter.getLatestVersion());

                String json = GSON.toJson(object);
                Files.writeString(path, json);
            }

            for (Option<?> option : config.getOptions()) {
                if (option.serializedName != null && object.has(option.serializedName)) {
                    readIntoValue(option.value, object.get(option.serializedName));
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load config from '{}'", path, e);
        }
    }

    public static <T> void readIntoValue(Value<T> value, JsonElement element) {
        value.set(GSON.fromJson(element, value.getValueType()));
    }

    public static void saveToPath(Config config, Path path) {
        JsonObject object = new JsonObject();

        UpgradeRewriter upgradeRewriter = config.getUpgradeRewriter();
        if (upgradeRewriter != null) {
            object.addProperty("version", upgradeRewriter.getLatestVersion());
        }

        for (Option<?> option : config.getOptions()) {
            if (option.serializedName != null) {
                object.add(option.serializedName, GSON.toJsonTree(option.value.get(), option.value.getValueType()));
            }
        }

        try {
            String json = GSON.toJson(object);
            Files.writeString(path, json);
        } catch (IOException e) {
            LOGGER.error("Failed to save config to '{}'", path, e);
        }
    }

    public interface UpgradeRewriter {
        void upgrade(int fromVersion, JsonObject config);

        int getLatestVersion();
    }
}
