package me.chrr.scribble.book;

import me.chrr.scribble.Scribble;
import net.minecraft.util.Language;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Platform;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.function.Consumer;

public class FileChooser {
    private FileChooser() {
    }

    public static void chooseBook(boolean save, Consumer<Path> pathConsumer) {
        new Thread(() -> {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                // We only want to select Scribble book files.
                PointerBuffer filter = stack.mallocPointer(1);
                filter.put(stack.UTF8("*.book"));
                filter.flip();

                String defaultPath = createAndGetBookDirectory() + "/";

                // Depending on the arguments, we open either a save or open file dialog.
                String path;
                if (save) {
                    path = TinyFileDialogs.tinyfd_saveFileDialog(
                            Language.getInstance().get("text.scribble.action.save_book_to_file"), defaultPath,
                            filter, "Scribble Book (.book)");
                } else {
                    // FIXME: For macOS, we don't set a file filter on open.
                    //        - https://github.com/chrrs/scribble/issues/11
                    //        - https://github.com/LWJGL/lwjgl3/issues/921
                    if (Platform.get() == Platform.MACOSX) {
                        filter = null;
                    }

                    path = TinyFileDialogs.tinyfd_openFileDialog(
                            Language.getInstance().get("text.scribble.action.load_book_from_file"), defaultPath,
                            filter, "Scribble Book (.book)", false);
                }

                // If the returned path is null, the user closed the file dialog.
                if (path == null) {
                    return;
                }

                try {
                    pathConsumer.accept(Path.of(path));
                } catch (InvalidPathException e) {
                    Scribble.LOGGER.error("failed to choose path", e);
                }
            }
        }, "File chooser").start();
    }

    public static Path createAndGetBookDirectory() {
        //? if fabric {
        Path gameDir = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir();
        //?} elif neoforge {
        /*Path gameDir = net.neoforged.fml.loading.FMLPaths.GAMEDIR.get();
         *///?} elif forge
        /*Path gameDir = net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get();*/

        Path booksDir = gameDir.resolve("books");

        try {
            if (!Files.exists(booksDir)) {
                Files.createDirectory(booksDir);
            }
        } catch (Exception ignored) {
            Scribble.LOGGER.warn("couldn't create the default books directory");
        }

        return booksDir;
    }
}
