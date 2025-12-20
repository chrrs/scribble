package me.chrr.tapestry.config;

import com.google.gson.*;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@NullMarked
public enum ConfigIo {
    ;

    public static final Gson GSON = new GsonBuilder()
            .setStrictness(Strictness.LENIENT)
            .setFormattingStyle(FormattingStyle.PRETTY)
            .create();

    public static void loadFromPathOrSaveDefault(Config config, Path file, List<Path> aliases) throws IOException {
        if (Files.exists(file)) {
            loadFromPath(config, file);
            return;
        } else {
            for (Path alias : aliases) {
                if (!Files.isRegularFile(alias))
                    continue;

                loadFromPath(config, alias);
                saveToPath(config, file);

                Files.delete(alias);
                config.getLogger().info("Migrated config from '{}'", alias);
                return;
            }
        }

        config.getLogger().info("No config file found, saving default config");
        saveToPath(config, file);
    }

    public static void loadFromPath(Config config, Path path) throws IOException {
        JsonObject object = JsonParser.parseString(Files.readString(path)).getAsJsonObject();

        UpgradeRewriter upgradeRewriter = config.getUpgradeRewriter();
        if (upgradeRewriter != null && object.has("version")) {
            int version = object.get("version").getAsInt();
            upgradeRewriter.upgrade(version, object);
            object.addProperty("version", upgradeRewriter.getLatestVersion());
            config.getLogger().info("Upgraded config to version {}", upgradeRewriter.getLatestVersion());
        }

        for (Option<?, ?> option : config.getOptions()) {
            if (object.has(option.serializeName)) {
                setBindingJsonElement(option.serializeBinding, object.get(option.serializeName));
            }
        }
    }

    private static <T> void setBindingJsonElement(Binding<T> binding, JsonElement element) {
        binding.set(GSON.fromJson(element, binding.getValueClass()));
    }

    public static void saveToPath(Config config, Path path) throws IOException {
        JsonObject object = new JsonObject();

        UpgradeRewriter upgradeRewriter = config.getUpgradeRewriter();
        if (upgradeRewriter != null) {
            object.addProperty("version", upgradeRewriter.getLatestVersion());
        }

        for (Option<?, ?> option : config.getOptions()) {
            object.add(option.serializeName, GSON.toJsonTree(option.serializeBinding.get(), option.serializeBinding.getValueClass()));
        }

        String json = GSON.toJson(object);
        Files.write(path, json.getBytes());
    }

    public interface UpgradeRewriter {
        void upgrade(int fromVersion, JsonObject config);

        int getLatestVersion();
    }
}
