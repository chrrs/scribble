package me.chrr.scribble.history.command;

import me.chrr.scribble.Scribble;
import me.chrr.scribble.book.PageManager;
import me.chrr.scribble.book.RichText;

public class InsertPageCommand implements Command {

    private final PageManager pageManager;
    private final int index;

    public InsertPageCommand(PageManager pageManager, int index) {
        this.pageManager = pageManager;
        this.index = index;
    }

    @Override
    public boolean execute() {
        if (pageManager.canAddPage(index)) {
            pageManager.insertPage(index, RichText.empty());
            return true;
        } else {
            Scribble.LOGGER.error("Unable to insert a page: page manage can't add a page with index={}", index);
        }
        return false;

//        TODO - Previous Implementation
//        pages.add(index, RichText.empty());
//        pagesListener.scribble$onPageAdded(index);
    }

    @Override
    public boolean rollback() {
        if (pageManager.canRemovePage(index)) {
            RichText removedPage = pageManager.removePage(index);
            return removedPage != null;
        } else {
            Scribble.LOGGER.error("Unable to rollback a page insertion: page manage can't remove a page with index={}",
                    index);
        }
        return false;

//        TODO - Previous Implementation
//        pages.remove(index);
//        pagesListener.scribble$onPageRemoved(index);
    }
}
