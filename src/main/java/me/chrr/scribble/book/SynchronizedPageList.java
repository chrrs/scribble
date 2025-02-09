package me.chrr.scribble.book;

import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * A class responsible for synchronizing a list of formatted strings ({@link SynchronizedPageList#pages})
 * with a list of {@link RichText} objects ({@link SynchronizedPageList#richPages}).
 * <p>
 * This class ensures that any modification to one list is reflected in the other,
 * maintaining consistency between the two.
 */
public class SynchronizedPageList {

    private final List<RichText> richPages;
    private List<String> pages;

    private static List<RichText> createRichPages(List<String> pages) {
        return pages.stream().map(RichText::fromFormattedString).toList();
    }

    public SynchronizedPageList(List<String> pages, List<RichText> richPages) {
        this.pages = pages;
        this.richPages = richPages;
    }

    public SynchronizedPageList() {
        // use ArrayList to keep collections mutable
        this(new ArrayList<>(), new ArrayList<>());
    }

    /**
     * Populates the {@link SynchronizedPageList#pages}` and {@link SynchronizedPageList#richPages} lists
     * with the provided strings.
     * The {@link SynchronizedPageList#pages} reference link is updated.
     * The {@link SynchronizedPageList#richPages} reference is remains the same. The list is cleared and recreated from the strings.
     *
     * @param pages The list of formatted strings to populate the `pages` and `richPages` lists.
     */
    public void populate(List<String> pages) {
        this.pages = pages;

        this.richPages.clear();
        this.richPages.addAll(createRichPages(pages));
    }

    public RichText set(int index, RichText richText) {
        RichText result = richPages.set(index, richText);
        pages.set(index, richText.getAsFormattedString());
        return result;
    }

    public boolean add(RichText richText) {
        richPages.add(richText);
        pages.add(richText.getAsFormattedString());
        return true;
    }

    public void add(int index, RichText richText) {
        richPages.add(index, richText);
        pages.add(index, richText.getAsFormattedString());
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean addAll(@NotNull Collection<? extends RichText> collection) {
        boolean result = richPages.addAll(collection);
        collection.forEach((richText) -> pages.add(richText.getAsFormattedString()));
        return result;
    }

    @SuppressWarnings("UnusedReturnValue")
    public RichText remove(int index) {
        RichText removed = richPages.remove(index);
        pages.remove(index);
        return removed;
    }

    public void clear() {
        richPages.clear();
        pages.clear();
    }

    public RichText get(int index) {
        return richPages.get(index);
    }

    public List<RichText> getRichPages() {
        // return immutable copy of richPages
        return List.copyOf(richPages);
    }

    public List<String> getPages() {
        // return immutable copy of pages
        return List.copyOf(pages);
    }

    public int size() {
        return richPages.size();
    }

    public boolean isEmpty() {
        return richPages.isEmpty();
    }
}
