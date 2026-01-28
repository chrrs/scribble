package me.chrr.tapestry.config;

import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

public interface Config {
    Collection<Option<?>> getOptions();

    Component getTitle();

    ConfigIo.@Nullable UpgradeRewriter getUpgradeRewriter();

    void save();
}
