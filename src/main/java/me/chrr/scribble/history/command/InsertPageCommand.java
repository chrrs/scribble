package me.chrr.scribble.history.command;

import me.chrr.scribble.book.SynchronizedPageList;
import me.chrr.scribble.book.RichText;

public class InsertPageCommand implements Command {

    private final SynchronizedPageList pages;
    private final int index;

    private final PagesListener pagesListener;

    public InsertPageCommand(SynchronizedPageList pages, int index, PagesListener pagesListener) {
        if (index < 0 || index > pages.size()) {
            throw new IllegalArgumentException("Insert page index is out of pages range");
        }

        this.pages = pages;
        this.index = index;
        this.pagesListener = pagesListener;
    }

    @Override
    public boolean execute() {
        pages.add(index, RichText.empty());
        pagesListener.scribble$onPageAdded(index);
        return true;
    }

    @Override
    public boolean rollback() {
        pages.remove(index);
        pagesListener.scribble$onPageRemoved(index);
        return true;
    }
}
