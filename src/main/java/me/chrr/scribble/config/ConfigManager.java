package me.chrr.scribble.config;

import com.google.gson.*;
import me.chrr.scribble.Scribble;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .addSerializationExclusionStrategy(new SkipDeprecatedStrategy())
            .setPrettyPrinting()
            .create();

    private Config config = new Config();

    public Config getConfig() {
        return this.config;
    }

    public void load() throws IOException {
        Path path = this.getConfigPath();
        if (path.toFile().isFile()) {
            this.config = GSON.fromJson(Files.readString(path), Config.class);
            this.config.upgrade();
        }

        this.save();
    }

    public void save() throws IOException {
        Files.writeString(this.getConfigPath(), GSON.toJson(this.config));
    }

    private Path getConfigPath() {
        return Scribble.CONFIG_DIR.resolve("scribble.json");
    }

    /// Exclusion strategy to skip all fields that are annotated with {@link DeprecatedConfigOption}.
    private static class SkipDeprecatedStrategy implements ExclusionStrategy {
        @Override
        public boolean shouldSkipField(FieldAttributes f) {
            return f.getAnnotation(DeprecatedConfigOption.class) != null;
        }

        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
            return false;
        }
    }
}
