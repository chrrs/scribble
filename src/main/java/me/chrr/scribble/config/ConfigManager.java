package me.chrr.scribble.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

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
        return FabricLoader.getInstance().getConfigDir().resolve("scribble.json");
    }
}