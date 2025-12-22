package me.chrr.tapestry.config;

import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import java.util.List;

public interface Config {
    Logger getLogger();

    List<Option<?, ?>> getOptions();

    ConfigIo.@Nullable UpgradeRewriter getUpgradeRewriter();

    Component getTitle();

    void save();


    default void ensureLoaded() {
        // ... empty, if this class is loaded that means the config is loaded.
    }
}
