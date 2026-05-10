package me.chrr.scribble.book;

import me.chrr.scribble.history.HistoryListener;
import me.chrr.scribble.history.command.OverflowCommand;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Style;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

@NullMarked
public class TextOverflowHandler {
    private static final int PAGE_WIDTH = 114;
    private static final int LINE_LIMIT = 14;
    
    private final HistoryListener listener;
    private final Font font;
    private final Consumer<OverflowCommand> onCommand;

    public TextOverflowHandler(HistoryListener listener, Font font, Consumer<OverflowCommand> onCommand) {
        this.listener = listener;
        this.font = font;
        this.onCommand = onCommand;
    }

    public boolean insertWithOverflow(int page, RichText text, int cursor, RichText insert,
                                      @Nullable ChatFormatting color, Set<ChatFormatting> modifiers) {
        int maxPages = 100 - listener.getTotalPages() + 1;
        if (cursor != text.getLength() || maxPages < 1) return false;
        
        // Truncate insert to max pages × ~400 chars/page (conservative estimate)
        insert = insert.subText(0, Math.min(insert.getLength(), maxPages * 400));

        // Combine text + insert first, then split with word-wrapping
        RichText combined = text.insert(cursor, insert);
        
        List<RichText> pages = new ArrayList<>();
        RichText remaining = combined;
        for (; remaining.getLength() > 0 && pages.size() < maxPages; ) {
            int len = findFittingLength(remaining);
            pages.add(remaining.subText(0, len));
            remaining = remaining.subText(len, remaining.getLength());
        }
        
        // Reject only if no overflow pages were created (at page limit with no room)
        // Allow truncation when overflow pages were successfully added
        if (remaining.getLength() > 0 && pages.size() == 1) {
            return false;
        }

        OverflowCommand cmd = new OverflowCommand(page, listener.getPageContent(page), pages);
        cmd.execute(listener);
        onCommand.accept(cmd);
        return true;
    }

    private int findFittingLength(RichText text) {
        if (!wouldOverflow(text)) return text.getLength();
        
        // Binary search for max chars that fit on one page
        int lo = 1, hi = text.getLength(), best = 1;
        while (lo <= hi) {
            int mid = (lo + hi) / 2;
            if (!wouldOverflow(text.subText(0, mid))) { best = mid; lo = mid + 1; }
            else hi = mid - 1;
        }
        
        // Find last space in the fitted text for word wrapping
        String plain = text.subText(0, best).getPlainText();
        int lastNewline = plain.lastIndexOf('\n');
        int searchStart = Math.max(0, lastNewline); // Only look for space after last newline
        int lastSpace = plain.lastIndexOf(' ', best - 1);
        
        // Only use space if it's on the last visual line (after last newline)
        if (lastSpace > searchStart) {
            return lastSpace + 1;
        }
        return best;
    }

    private boolean wouldOverflow(RichText text) {
        return font.getSplitter().splitLines(text, PAGE_WIDTH, Style.EMPTY).size() > LINE_LIMIT;
    }
}
