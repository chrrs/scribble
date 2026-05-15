package me.chrr.scribble.history.command;

import me.chrr.scribble.book.RichText;
import me.chrr.scribble.gui.edit.RichMultiLineTextField;
import me.chrr.scribble.history.HistoryListener;

import java.util.List;

public class OverflowCommand implements Command {
    private final int page;
    private final RichText before;
    private final List<RichText> after;

    public OverflowCommand(int page, RichText before, List<RichText> after) {
        this.page = page;
        this.before = before;
        this.after = List.copyOf(after);
    }

    @Override
    public void execute(HistoryListener listener) {
        int newPages = after.size() - 1;
        for (int i = 0; i < newPages; i++) listener.insertPageAt(page + 1 + i, null);
        for (int i = 0; i < after.size(); i++) listener.setPageContent(page + i, after.get(i));
        listener.refreshPages();
        setCursor(listener.switchAndFocusPage(page + newPages), after.get(newPages).getLength());
    }

    @Override
    public void rollback(HistoryListener listener) {
        for (int i = after.size() - 1; i > 0; i--) listener.deletePage(page + i, 0);
        listener.setPageContent(page, before);
        listener.refreshPages();
        setCursor(listener.switchAndFocusPage(page), before.getLength());
    }

    private void setCursor(RichMultiLineTextField tf, int pos) {
        tf.cursor = tf.selectCursor = pos;
        tf.onValueChange();
    }
}
