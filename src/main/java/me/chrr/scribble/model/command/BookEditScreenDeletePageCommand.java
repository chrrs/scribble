package me.chrr.scribble.model.command;

import me.chrr.scribble.Scribble;
import me.chrr.scribble.book.RichText;
import me.chrr.scribble.tool.commandmanager.Command;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BookEditScreenDeletePageCommand implements Command {

    private final List<RichText> richPages;
    private final List<String> pages;
    private final int pageIndexToDelete;
    private final PagesChangedCallback callback;

    @Nullable
    private RichText pageContentToDelete;

    public BookEditScreenDeletePageCommand(
            List<RichText> richPages,
            List<String> pages,
            int pageIndexToDelete,
            PagesChangedCallback callback
    ) {
        this.richPages = richPages;
        this.pages = pages;
        this.pageIndexToDelete = pageIndexToDelete;
        this.callback = callback;
    }

    @Override
    public void execute() {
        pageContentToDelete = richPages.get(pageIndexToDelete);

        richPages.remove(pageIndexToDelete);
        pages.remove(pageIndexToDelete);

        int newPageIndex = Math.min(pageIndexToDelete, richPages.size() - 1);
        callback.onPagesChanged(newPageIndex);
    }

    @Override
    public boolean rollback() {
        if (pageContentToDelete != null) {
            richPages.add(pageIndexToDelete, pageContentToDelete);
            pages.add(pageIndexToDelete, pageContentToDelete.getAsFormattedString());
            callback.onPagesChanged(pageIndexToDelete);
            return true;

        } else {
            Scribble.LOGGER.error("Unable to rollback DeletePageCommand, deletedPage is null");
            return false;
        }
    }

    public interface PagesChangedCallback {
        void onPagesChanged(int currentPageIndex);
    }
}
