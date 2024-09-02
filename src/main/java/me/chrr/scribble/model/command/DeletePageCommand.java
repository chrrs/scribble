package me.chrr.scribble.model.command;

import me.chrr.scribble.Scribble;
import me.chrr.scribble.book.RichText;
import me.chrr.scribble.tool.commandmanager.Command;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DeletePageCommand implements Command {

    private final List<RichText> richPages;
    private final List<String> pages;
    private final int index;

    private final PagesListener pagesListener;

    @Nullable
    private RichText pageContentToDelete;

    public DeletePageCommand(List<RichText> richPages, List<String> pages, int index, PagesListener pagesListener) {
        this.richPages = richPages;
        this.pages = pages;
        this.index = index;
        this.pagesListener = pagesListener;
    }

    @Override
    public boolean execute() {
        pageContentToDelete = richPages.get(index);

        richPages.remove(index);
        pages.remove(index);

        pagesListener.scribble$onPageRemoved(index);
        return true;
    }

    @Override
    public boolean rollback() {
        if (pageContentToDelete != null) {
            richPages.add(index, pageContentToDelete);
            pages.add(index, pageContentToDelete.getAsFormattedString());
            pagesListener.scribble$onPageAdded(index);
            return true;

        } else {
            Scribble.LOGGER.error("Unable to rollback DeletePageCommand, deletedPage is null");
            return false;
        }
    }
}
