package me.chrr.scribble.history.command;

import me.chrr.scribble.Scribble;
import me.chrr.scribble.book.SynchronizedPageList;
import me.chrr.scribble.book.RichText;
import org.jetbrains.annotations.Nullable;

public class DeletePageCommand implements Command {

    private final SynchronizedPageList pages;
    private final int index;

    private final PagesListener pagesListener;

    @Nullable
    private RichText deletedPage;

    public DeletePageCommand(SynchronizedPageList pages, int index, PagesListener pagesListener) {
        if (index < 0 || index >= pages.size()) {
            throw new IllegalArgumentException("Delete page index is out of pages range");
        }

        this.pages = pages;
        this.index = index;
        this.pagesListener = pagesListener;
    }

    @Override
    public boolean execute() {
        deletedPage = pages.get(index);
        pages.remove(index);
        pagesListener.scribble$onPageRemoved(index);
        return true;
    }

    @Override
    public boolean rollback() {
        if (deletedPage != null) {
            pages.add(index, deletedPage);
            pagesListener.scribble$onPageAdded(index);
            return true;

        } else {
            Scribble.LOGGER.error("Unable to rollback DeletePageCommand, deletedPage is null");
            return false;
        }
    }
}
