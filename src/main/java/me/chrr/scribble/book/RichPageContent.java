package me.chrr.scribble.book;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.text.*;

import java.util.Optional;

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
                    .trimToWidth(line.stringVisitable, position.x, Style.EMPTY)
                    .getString().length();
        }
    }

    public static class Line extends BookEditScreen.Line {
        private final StringVisitable stringVisitable;

        public Line(StringVisitable stringVisitable, int x, int y) {
            super(Style.EMPTY, "", x, y);

            this.stringVisitable = stringVisitable;
            this.text = MutableText.of(toTextContent(stringVisitable));
        }

        public StringVisitable getStringVisitable() {
            return stringVisitable;
        }

        // FIXME: Does this really not exist already?
        private static TextContent toTextContent(StringVisitable stringVisitable) {
            return new TextContent() {
                @Override
                public <T> Optional<T> visit(StringVisitable.StyledVisitor<T> visitor, Style style) {
                    return stringVisitable.visit(visitor, style);
                }

                @Override
                public <T> Optional<T> visit(StringVisitable.Visitor<T> visitor) {
                    return stringVisitable.visit(visitor);
                }

                //? if >=1.20.4 {
                @Override
                public Type<?> getType() {
                    // This is not accurate, but this TextContent is never sent to the
                    // server, so it doesn't need to be.
                    return PlainTextContent.TYPE;
                }
                //?}
            };
        }
    }
}
