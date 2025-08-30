package me.chrr.scribble.book;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;

/**
 * A record of the data saved to a book file.
 * <p>
 * Because of legacy reasons, book files come in two formats:
 * <li> NBT files (.book): For Scribble <1.6, was a bad idea, but still readable for backwards compatibility.
 * <li> JSON files (.json): For Scribble 1.6+, are a lot easier to work with and make.
 *
 * @param author the author of the book file. Usually equal to a username.
 * @param pages  the rich-text pages of the book.
 */
public record BookFile(String author, Collection<String> pages) {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static BookFile readFile(Path path) throws IOException {
        if (path.toString().endsWith(".book")) {
            return readNbt(path);
        } else if (path.toString().endsWith(".json")) {
            return readJson(path);
        } else {
            throw new IOException("unrecognized file format");
        }
    }

    public void writeJson(Path path) throws IOException {
        String json = GSON.toJson(this);
        Files.write(path, json.getBytes());
    }

    private static BookFile readJson(Path path) throws IOException {
        return GSON.fromJson(Files.newBufferedReader(path), BookFile.class);
    }

    private static BookFile readNbt(Path path) throws IOException {
        NbtCompound root = NbtIo.read(path);

        if (root == null) {
            throw new IOException("could not read book nbt file");
        }

        String author = root.getString("author").orElse("<unknown>");
        Collection<String> pages = root.getList("pages").orElse(new NbtList())
                .stream()
                .map(NbtElement::asString)
                .map(Optional::orElseThrow)
                .toList();

        return new BookFile(author, pages);
    }
}
