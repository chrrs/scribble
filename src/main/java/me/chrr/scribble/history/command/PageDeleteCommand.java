package me.chrr.scribble.history.command;

import me.chrr.scribble.book.RichText;
import me.chrr.scribble.history.HistoryListener;

public class PageDeleteCommand implements Command {
    private final int page;
    private final RichText content;

    public PageDeleteCommand(int page, RichText content) {
        this.page = page;
        this.content = content;
    }

    @Override
    public void execute(HistoryListener listener) {
        listener.scribble$history$deletePage(page);
    }

    @Override
    public void rollback(HistoryListener listener) {
        listener.scribble$history$insertPage(page, content);
    }
}
