package me.chrr.scribble.history.command;

import me.chrr.scribble.Scribble;
import me.chrr.scribble.book.PageManager;
import me.chrr.scribble.book.RichText;
import org.jetbrains.annotations.Nullable;

public class DeletePageCommand implements Command {

    private final PageManager pageManager;
    private final int index;

    @Nullable
    private RichText deletedPage;

    public DeletePageCommand(PageManager pageManager, int index) {
        this.pageManager = pageManager;
        this.index = index;
    }

    @Override
    public boolean execute() {
        if (pageManager.canRemovePage(index)) {
            deletedPage = pageManager.removePage(index);
            return deletedPage != null;
        } else {
            Scribble.LOGGER.error("Unable to delete a page: page manage can't remove a page with index={}", index);
        }
        return false;

//        TODO - Previous Implementation
//        deletedPage = pages.get(index);
//        pages.remove(index);
//        pagesListener.scribble$onPageRemoved(index);
    }

    @Override
    public boolean rollback() {
        if (canRollback()) {
            return pageManager.insertPage(index, deletedPage);
        }
        return false;

//        TODO - Previous Implementation
//        pages.add(index, deletedPage);
//        pagesListener.scribble$onPageAdded(index);
    }

    private boolean canRollback() {
        if (deletedPage == null) {
            Scribble.LOGGER.error("Unable to rollback a page deleting: deletedPage is null");
            return false;
        }

        if (!pageManager.canAddPage(index)) {
            Scribble.LOGGER.error("Unable to rollback a page deleting: page manage can't add a page with index={}",
                    index);
            return false;
        }

        return true;
    }
}
