package me.chrr.scribble.history.command;

import me.chrr.scribble.book.RichText;
import me.chrr.scribble.history.HistoryListener;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class PageDeleteCommand implements Command {
    private final int page;
    private final RichText content;
    private final int navigateDirection;

    public PageDeleteCommand(int page, RichText content, int navigateDirection) {
        this.page = page;
        this.content = content;
        this.navigateDirection = navigateDirection;
    }

    @Override
    public void execute(HistoryListener listener) {
        listener.deletePage(page, navigateDirection);
    }

    @Override
    public void rollback(HistoryListener listener) {
        listener.insertPageAt(page, content);
    }
}
