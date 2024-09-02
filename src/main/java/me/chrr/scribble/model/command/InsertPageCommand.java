package me.chrr.scribble.model.command;

import me.chrr.scribble.book.RichText;
import me.chrr.scribble.tool.commandmanager.Command;

import java.util.List;

public class InsertPageCommand implements Command {

    private final List<RichText> richPages;
    private final List<String> pages;
    private final int index;

    private final PagesListener pagesListener;

    public InsertPageCommand(List<RichText> richPages, List<String> pages, int index, PagesListener pagesListener) {
        this.richPages = richPages;
        this.pages = pages;
        this.index = index;
        this.pagesListener = pagesListener;
    }

    @Override
    public boolean execute() {
        richPages.add(index, RichText.empty());
        pages.add(index, "");

        pagesListener.scribble$onPageAdded(index);
        return true;
    }

    @Override
    public boolean rollback() {
        richPages.remove(index);
        pages.remove(index);

        pagesListener.scribble$onPageRemoved(index);
        return true;
    }
}
