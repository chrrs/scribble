package me.chrr.scribble;

import me.chrr.scribble.book.FileChooser;
import me.chrr.tapestry.base.Tapestry;
import me.chrr.tapestry.config.ReflectedConfig;
import me.chrr.tapestry.config.gui.TapestryConfigScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NullMarked;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

@NullMarked
public class Scribble {
    public static final String MOD_ID = "scribble";
    public static final String VERSION = Objects.requireNonNull(Tapestry.PLATFORM_METHODS.getModVersion(MOD_ID));
    
    public static final Logger LOGGER = LogManager.getLogger();

    public static final Path BOOK_DIR = Tapestry.PLATFORM_METHODS.getGameDirectory().resolve("books");
    public static ScribbleConfig CONFIG = ReflectedConfig.load(ScribbleConfig.class, "scribble.client.json", List.of("scribble.json"));

    public static void init() {
        FileChooser.convertLegacyBooks();
    }

    public static Screen buildConfigScreen(Screen parent) {
        return new TapestryConfigScreen(parent, CONFIG);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
