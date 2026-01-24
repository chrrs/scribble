package me.chrr.scribble.book;

import me.chrr.scribble.Scribble;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import org.jspecify.annotations.NullMarked;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Platform;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Consumer;

@NullMarked
public class FileChooser {
    private FileChooser() {
    }

    /**
     * Show a file open or save dialog, and send the result to the attached consumer.
     *
     * @param save         if the dialog should be a save dialog instead of an open dialog.
     * @param pathConsumer the callback to call when a path is successfully chosen.
     */
    public static void chooseFile(boolean save, Consumer<Path> pathConsumer) {
        new Thread(() -> {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer filter = null;
                String defaultPath = createAndGetBookDirectory().toAbsolutePath() + File.separator;

                // Depending on the arguments, we open either a save or open file dialog.
                String path;
                if (save) {
                    // We want to save JSON files.
                    filter = stack.mallocPointer(1);
                    filter.put(stack.UTF8("*.json"));
                    filter.flip();

                    path = TinyFileDialogs.tinyfd_saveFileDialog(
                            Language.getInstance().getOrDefault("text.scribble.action.save_book_to_file"), defaultPath,
                            filter, "Scribble Book (.json)");
                } else {
                    // FIXME: For macOS, we don't set a file filter on open.
                    //        - https://github.com/chrrs/scribble/issues/11
                    //        - https://github.com/LWJGL/lwjgl3/issues/921
                    if (Platform.get() != Platform.MACOSX) {
                        // We only want to select Scribble book and JSON files.
                        filter = stack.mallocPointer(2);
                        filter.put(stack.UTF8("*.book"));
                        filter.put(stack.UTF8("*.json"));
                        filter.flip();
                    }

                    path = TinyFileDialogs.tinyfd_openFileDialog(
                            Language.getInstance().getOrDefault("text.scribble.action.load_book_from_file"), defaultPath,
                            filter, "Scribble Book (.book, .json)", false);
                }

                // If the returned path is null, the user closed the file dialog.
                if (path == null) {
                    return;
                }

                try {
                    Path p = Path.of(path);
                    Minecraft.getInstance().execute(() -> pathConsumer.accept(p));
                } catch (InvalidPathException e) {
                    Scribble.LOGGER.error("failed to choose path", e);
                }
            }
        }, "File chooser").start();
    }

    /**
     * Get the directory `.minecraft/books`, creating it if it does not exist yet.
     *
     * @return the book directory.
     */
    private static Path createAndGetBookDirectory() {
        Path bookDir = Scribble.platform().BOOK_DIR;

        try {
            if (!Files.exists(bookDir)) {
                Files.createDirectory(bookDir);
            }
        } catch (Exception ignored) {
            Scribble.LOGGER.warn("couldn't create the default book directory");
        }

        return bookDir;
    }

    /**
     * Walk the default book directory, and convert all 'legacy style' book files to the
     * new JSON format. All old files are copied to the '_legacy' directory.
     */
    public static void convertLegacyBooks() {
        Path rootDir = Scribble.platform().BOOK_DIR;
        Path legacyDir = rootDir.resolve("_legacy");

        if (!rootDir.toFile().isDirectory()) {
            return;
        }

        try {
            Files.walkFileTree(rootDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (dir.getFileName().toString().equals("_legacy")) {
                        return FileVisitResult.SKIP_SUBTREE;
                    } else {
                        return FileVisitResult.CONTINUE;
                    }
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith(".book")) {
                        Scribble.LOGGER.info("converting legacy NBT-based book file at {} to JSON.", file);

                        Path relativePath = rootDir.relativize(file);
                        Path legacyPath = legacyDir.resolve(relativePath);

                        String fileName = file.getFileName().toString();
                        fileName = fileName.substring(0, fileName.length() - 5);

                        Path jsonPath = file.resolveSibling(fileName + ".json");

                        try {
                            BookFile book = BookFile.readFile(file);
                            book.writeJson(jsonPath);

                            Files.createDirectories(legacyPath.getParent());
                            Files.move(file, legacyPath);
                        } catch (Exception e) {
                            Scribble.LOGGER.error("failed to convert legacy NBT-based book file at {} to JSON.", file, e);
                        }

                    }

                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            Scribble.LOGGER.error("failed to convert legacy NBT-based book files to JSON", e);
        }
    }
}
