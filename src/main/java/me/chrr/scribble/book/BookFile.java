package me.chrr.scribble.book;

import net.minecraft.nbt.*;

import java.io.*;
import java.nio.file.Path;
import java.util.Collection;

/**
 * A record of the data saved to a .book file. The .book file is an NBT
 * file containing an 'author' tag and a 'pages' tag.
 *
 * @param author the author of the book file. Usually equal to a username.
 * @param pages  the rich-text pages of the book.
 */
public record BookFile(String author, Collection<RichText> pages) {
    public static BookFile read(Path path) throws IOException {
        //? if >=1.20.4 {
        NbtCompound root = NbtIo.read(path);
        //?} else
        /*NbtCompound root = NbtIo.read(path.toFile());*/

        if (root == null) {
            throw new IOException("could not read book nbt file");
        }

        String author = root.getString("author");
        Collection<RichText> pages = root.getList("pages", NbtElement.STRING_TYPE)
                .stream()
                .map(NbtElement::asString)
                .map(RichText::fromFormattedString)
                .toList();

        return new BookFile(author, pages);
    }

    public void write(Path path) throws IOException {
        NbtList pagesNbt = new NbtList();
        for (RichText page : pages) {
            pagesNbt.add(NbtString.of(page.getAsFormattedString()));
        }

        NbtCompound root = new NbtCompound();
        root.putString("author", author);
        root.put("pages", pagesNbt);

        //? if >=1.20.4 {
        NbtIo.write(root, path);
        //?} else
        /*NbtIo.write(root, path.toFile());*/
    }
}
