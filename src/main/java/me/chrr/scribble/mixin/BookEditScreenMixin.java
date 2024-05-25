package me.chrr.scribble.mixin;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import me.chrr.scribble.book.RichPageContent;
import me.chrr.scribble.book.RichSelectionManager;
import me.chrr.scribble.book.RichText;
import net.minecraft.client.font.TextHandler;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.client.util.SelectionManager;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Mixin(BookEditScreen.class)
public abstract class BookEditScreenMixin extends Screen {
    //region @Shadow declarations
    @Shadow
    protected abstract BookEditScreen.Position absolutePositionToScreenPosition(BookEditScreen.Position position);

    @Mutable
    @Shadow
    @Final
    private SelectionManager currentPageSelectionManager;

    @Shadow
    private int currentPage;

    @Shadow
    static int getLineFromOffset(int[] lineStarts, int position) {
        return 0;
    }

    @Shadow
    protected abstract Rect2i getRectFromCorners(BookEditScreen.Position start, BookEditScreen.Position end);

    @Shadow
    protected abstract String getClipboard();

    @Shadow
    protected abstract void setClipboard(String clipboard);

    @Shadow
    private boolean dirty;

    @Shadow
    protected abstract void invalidatePageContent();
    //endregion

    @Unique
    private final List<RichText> richPages = new ArrayList<>(List.of(
            new RichText(List.of(
                    new RichText.Segment("Helloooo es", Formatting.RED, Set.of(Formatting.BOLD)),
                    new RichText.Segment(" world and\nthings that", Formatting.RED, Set.of(Formatting.BOLD, Formatting.ITALIC)),
                    new RichText.Segment("!!!", Formatting.RED, Set.of(Formatting.ITALIC)),
                    new RichText.Segment(" and others that make this text very very very long!!", Formatting.BLUE, Set.of(Formatting.ITALIC))
            ))
    ));

    // Dummy constructor to match super
    protected BookEditScreenMixin(Text title) {
        super(title);
    }

    // RichText replacement for BookEditScreen#getLineSelectionRectangle
    @Unique
    private Rect2i getSelectionRectangle(RichText text, TextHandler handler, int selectionStart, int selectionEnd, int lineY, int lineStart) {
        RichText toSelectionStart = text.subText(lineStart, selectionStart);
        RichText toSelectionEnd = text.subText(lineStart, selectionEnd);
        BookEditScreen.Position position = new BookEditScreen.Position((int) handler.getWidth(toSelectionStart.getAsStringVisitable()), lineY);
        BookEditScreen.Position position2 = new BookEditScreen.Position((int) handler.getWidth(toSelectionEnd.getAsStringVisitable()), lineY + 9);
        return this.getRectFromCorners(position, position2);
    }

    // RichText replacement for BookEditScreen#getCurrentPageContent
    @Unique
    private RichText getCurrentPageText() {
        return this.currentPage >= 0 && this.currentPage < this.richPages.size()
                ? this.richPages.get(this.currentPage)
                : RichText.empty();
    }

    // RichText replacement for BookEditScreen#setPageContent
    @Unique
    private void setPageText(RichText newText) {
        if (this.currentPage >= 0 && this.currentPage < this.richPages.size()) {
            this.richPages.set(this.currentPage, newText);
            this.dirty = true;
            this.invalidatePageContent();
        }
    }

    @Inject(method = "<init>", at = @At(value = "TAIL"))
    public void init(PlayerEntity player, ItemStack itemStack, Hand hand, CallbackInfo ci) {
        currentPageSelectionManager = new RichSelectionManager(
                this::getCurrentPageText,
                this::setPageText,
                this::getClipboard,
                this::setClipboard,
                text -> text.getAsFormattedString().length() < 1024
                        && this.textRenderer.getWrappedLinesHeight(text.getAsStringVisitable(), 114) <= 128
        );
    }

    // We replace the full createPageContent method with our own.
    // The contents of this methods are copied basically 1 to 1 from the original
    // method, with changes to RichText where necessary.
    // FIXME: I feel like we should use less RichText#subText for performance reasons.
    @Inject(method = "createPageContent", at = @At(value = "HEAD"), cancellable = true)
    private void createPageContent(CallbackInfoReturnable<BookEditScreen.PageContent> cir) {
        cir.cancel();

        RichText text = getCurrentPageText();
        String plainText = text.getPlainText();

        if (text.isEmpty()) {
            cir.setReturnValue(RichPageContent.EMPTY);
            return;
        }

        int selectionStart = this.currentPageSelectionManager.getSelectionStart();
        int selectionEnd = this.currentPageSelectionManager.getSelectionEnd();

        IntList lineStarts = new IntArrayList();
        List<RichPageContent.Line> lines = new ArrayList<>();
        List<RichText> lineTexts = new ArrayList<>();

        MutableBoolean endsWithManualNewline = new MutableBoolean();
        TextHandler textHandler = this.textRenderer.getTextHandler();

        // We split the text into lines. On the way, we record the line start positions,
        // the lines themselves, and if the book ends with a manual newline.
        MutableInt lineNumber = new MutableInt();
        textHandler.wrapLines(plainText, 114, Style.EMPTY, true, (style, start, end) -> {
            int i = lineNumber.getAndIncrement();

            RichText line = text.subText(start, end);
            lineTexts.add(line);

            String string = line.getAsFormattedString();
            endsWithManualNewline.setValue(string.endsWith("\n"));

            int y = i * 9;
            BookEditScreen.Position position = this.absolutePositionToScreenPosition(new BookEditScreen.Position(0, y));
            lineStarts.add(start);

            lines.add(new RichPageContent.Line(line, position.x, position.y));
        });

        int[] lineStartsArray = lineStarts.toIntArray();
        boolean atEnd = selectionStart == plainText.length();

        // We try to find the cursor position.
        BookEditScreen.Position cursorPosition;
        if (atEnd && endsWithManualNewline.isTrue()) {
            cursorPosition = new BookEditScreen.Position(0, lines.size() * 9);
        } else {
            int i = getLineFromOffset(lineStartsArray, selectionStart);
            int width = this.textRenderer.getWidth(text.subText(lineStartsArray[i], selectionStart).getAsStringVisitable());
            cursorPosition = new BookEditScreen.Position(width, i * 9);
        }

        // We calculate the selection rectangles.
        List<Rect2i> selectionRectangles = Lists.newArrayList();
        if (selectionStart != selectionEnd) {
            // We reorder the points, as backwards selections are possible.
            int selStart = Math.min(selectionStart, selectionEnd);
            int selEnd = Math.max(selectionStart, selectionEnd);

            int startLine = getLineFromOffset(lineStartsArray, selStart);
            int endLine = getLineFromOffset(lineStartsArray, selEnd);

            // Same-line selections are much simpler.
            if (startLine == endLine) {
                int y = startLine * 9;
                int lineStart = lineStartsArray[startLine];
                selectionRectangles.add(this.getSelectionRectangle(text, textHandler, selStart, selEnd, y, lineStart));
            } else {
                int lineEnd = startLine + 1 > lineStartsArray.length ? plainText.length() : lineStartsArray[startLine + 1];
                selectionRectangles.add(this.getSelectionRectangle(text, textHandler, selStart, lineEnd, startLine * 9, lineStartsArray[startLine]));

                for (int i = startLine + 1; i < endLine; i++) {
                    int y = i * 9;
                    RichText line = lineTexts.get(i);
                    int s = (int) textHandler.getWidth(line.getAsStringVisitable());
                    selectionRectangles.add(this.getRectFromCorners(new BookEditScreen.Position(0, y), new BookEditScreen.Position(s, y + 9)));
                }

                selectionRectangles.add(this.getSelectionRectangle(text, textHandler, lineStartsArray[endLine], selEnd, endLine * 9, lineStartsArray[endLine]));
            }
        }

        cir.setReturnValue(new RichPageContent(text, cursorPosition, atEnd, lineStartsArray, lines.toArray(new BookEditScreen.Line[0]), selectionRectangles.toArray(new Rect2i[0])));
    }
}
