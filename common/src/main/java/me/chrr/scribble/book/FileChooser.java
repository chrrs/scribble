package me.chrr.scribble.book;

import me.chrr.scribble.Scribble;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.NullMarked;
import org.lwjgl.system.MemoryStack;

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
    @SuppressWarnings("resource")
    public static void chooseFile(boolean save, Consumer<Path> pathConsumer) {
        new Thread(() -> {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                String defaultPath = createAndGetBookDirectory().toAbsolutePath() + File.separator;

                //? >=26.3 {
                long window = Minecraft.getInstance().getWindow().handle();
                org.lwjgl.sdl.SDL_DialogFileCallbackI fileCallback = (_, filelist, _) -> {
                    if (filelist == 0L) {
                        Scribble.LOGGER.error("failed to choose path: {}", org.lwjgl.sdl.SDLError.SDL_GetError());
                        return;
                    }

                    long str = org.lwjgl.system.MemoryUtil.memGetAddress(filelist);
                    if (str == 0L) {
                        // No file was selected.
                        return;
                    }

                    try {
                        Path p = Path.of(org.lwjgl.system.MemoryUtil.memUTF8(str));
                        Minecraft.getInstance().execute(() -> pathConsumer.accept(p));
                    } catch (InvalidPathException e) {
                        Scribble.LOGGER.error("failed to choose path", e);
                    }
                };
                //? } else {
                /*org.lwjgl.PointerBuffer filter = null;
                String path;
                 *///? }

                // Depending on the arguments, we open either a save or open file dialog.
                if (save) {
                    //? >=26.3 {
                    org.lwjgl.sdl.SDL_DialogFileFilter.Buffer filters = org.lwjgl.sdl.SDL_DialogFileFilter.create(1);
                    filters.get(0).name(stack.UTF8("Scribble Book (.json)")).pattern(stack.UTF8("json"));

                    org.lwjgl.sdl.SDLDialog.SDL_ShowSaveFileDialog(fileCallback, 0L, window, filters, defaultPath);
                    //? } else {
                    /*filter = stack.mallocPointer(1);
                    filter.put(stack.UTF8("*.json"));
                    filter.flip();

                    path = org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_saveFileDialog(
                            net.minecraft.locale.Language.getInstance().getOrDefault("text.scribble.action.save_book_to_file"), defaultPath,
                            filter, "Scribble Book (.json)");
                    *///?}
                } else {
                    //? >=26.3 {
                    org.lwjgl.sdl.SDL_DialogFileFilter.Buffer filters = org.lwjgl.sdl.SDL_DialogFileFilter.create(1);
                    filters.get(0).name(stack.UTF8("Scribble Book (.book, .json)")).pattern(stack.UTF8("json;book"));

                    org.lwjgl.sdl.SDLDialog.SDL_ShowOpenFileDialog(fileCallback, 0L, window, filters, defaultPath, false);
                    //? } else {
                    /*if (org.lwjgl.system.Platform.get() != org.lwjgl.system.Platform.MACOSX) {
                        // We only want to select Scribble book and JSON files.
                        filter = stack.mallocPointer(2);
                        filter.put(stack.UTF8("*.book"));
                        filter.put(stack.UTF8("*.json"));
                        filter.flip();
                    }

                    path = org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_openFileDialog(
                            net.minecraft.locale.Language.getInstance().getOrDefault("text.scribble.action.load_book_from_file"), defaultPath,
                            filter, "Scribble Book (.book, .json)", false);
                    *///?}
                }

                //? <26.3 {
                /*// If the returned path is null, the user closed the file dialog.
                if (path == null) {
                    return;
                }

                try {
                    Path p = Path.of(path);
                    Minecraft.getInstance().execute(() -> pathConsumer.accept(p));
                } catch (InvalidPathException e) {
                    Scribble.LOGGER.error("failed to choose path", e);
                }
                *///?}
            }
        }, "File chooser").start();
    }

    /**
     * Get the directory `.minecraft/books`, creating it if it does not exist yet.
     *
     * @return the book directory.
     */
    private static Path createAndGetBookDirectory() {
        Path bookDir = Scribble.BOOK_DIR;

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
        Path rootDir = Scribble.BOOK_DIR;
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
