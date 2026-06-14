package me.chrr.scribble.history.command;

import me.chrr.scribble.text.StyledText;
import me.chrr.scribble.history.HistoryListener;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class PageDeleteCommand implements Command {
    private final int page;
    private final StyledText content;

    public PageDeleteCommand(int page, StyledText content) {
        this.page = page;
        this.content = content;
    }

    @Override
    public void execute(HistoryListener listener) {
        listener.deletePage(page);
    }

    @Override
    public void rollback(HistoryListener listener) {
        listener.insertPageAt(page, content);
    }
}
