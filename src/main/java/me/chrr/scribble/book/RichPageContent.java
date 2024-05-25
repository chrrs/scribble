package me.chrr.scribble.book;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;

public class RichPageContent extends BookEditScreen.PageContent {
    public static final BookEditScreen.PageContent EMPTY = new RichPageContent(
            RichText.empty(), new BookEditScreen.Position(0, 0), true, new int[]{0},
            new BookEditScreen.Line[]{new Line(RichText.empty(), 0, 0)},
            new Rect2i[0]
    );

    private final RichText text;

    public RichPageContent(RichText text, BookEditScreen.Position position, boolean atEnd, int[] lineStarts, BookEditScreen.Line[] lines, Rect2i[] selectionRectangles) {
        super("", position, atEnd, lineStarts, lines, selectionRectangles);
        this.text = text;
    }

    @Override
    public int getCursorPosition(TextRenderer renderer, BookEditScreen.Position position) {
        int i = position.y / 9;
        if (i < 0) {
            return 0;
        } else if (i >= this.lines.length) {
            return this.text.getLength();
        } else {
            Line line = (Line) this.lines[i];
            return this.lineStarts[i] + renderer.getTextHandler()
                    .trimToWidth(line.richText, position.x, Style.EMPTY)
                    .getString().length();
        }
    }

    public static class Line extends BookEditScreen.Line {
        private final RichText richText;

        public Line(RichText richText, int x, int y) {
            super(Style.EMPTY, "", x, y);

            this.richText = richText;
            this.text = MutableText.of(richText);
        }

        public RichText getRichText() {
            return richText;
        }
    }
}
