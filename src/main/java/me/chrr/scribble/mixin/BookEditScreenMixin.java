package me.chrr.scribble.mixin;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import me.chrr.scribble.Scribble;
import me.chrr.scribble.book.RichPageContent;
import me.chrr.scribble.book.RichSelectionManager;
import me.chrr.scribble.book.RichText;
import me.chrr.scribble.gui.FormatButtonWidget;
import net.minecraft.client.font.TextHandler;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import net.minecraft.client.util.SelectionManager;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
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
import java.util.ListIterator;

@Mixin(BookEditScreen.class)
public abstract class BookEditScreenMixin extends Screen {
    //region @Shadow declarations
    @Mutable
    @Shadow
    @Final
    private SelectionManager currentPageSelectionManager;

    @Shadow
    private int currentPage;

    @Shadow
    private boolean dirty;

    @Shadow
    @Final
    private List<String> pages;

    @Shadow
    private boolean signing;

    @Shadow
    protected abstract BookEditScreen.Position absolutePositionToScreenPosition(BookEditScreen.Position position);

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
    protected abstract void invalidatePageContent();
    //endregion

    @Unique
    private final List<RichText> richPages = new ArrayList<>();

    @Unique
    private FormatButtonWidget boldButton;
    @Unique
    private FormatButtonWidget italicButton;
    @Unique
    private FormatButtonWidget underlineButton;
    @Unique
    private FormatButtonWidget strikethroughButton;
    @Unique
    private FormatButtonWidget obfuscatedButton;

    // Dummy constructor to match super
    protected BookEditScreenMixin(Text title) {
        super(title);
    }

    // RichText replacement for BookEditScreen#getLineSelectionRectangle
    @Unique
    private Rect2i getSelectionRectangle(RichText text, TextHandler handler, int selectionStart, int selectionEnd, int lineY, int lineStart) {
        RichText toSelectionStart = text.subText(lineStart, selectionStart);
        RichText toSelectionEnd = text.subText(lineStart, selectionEnd);
        BookEditScreen.Position topLeft = new BookEditScreen.Position((int) handler.getWidth(toSelectionStart), lineY);
        BookEditScreen.Position bottomRight = new BookEditScreen.Position((int) handler.getWidth(toSelectionEnd), lineY + 9);
        return this.getRectFromCorners(topLeft, bottomRight);
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

    @Unique
    private RichSelectionManager getRichSelectionManager() {
        // This is always the case, as we replace it in #init.
        return (RichSelectionManager) this.currentPageSelectionManager;
    }

    @Unique
    private void initButtons() {
        int x = this.width / 2 + 78;
        int y = 16;

        boldButton = addDrawableChild(new FormatButtonWidget(
                Text.translatable("text.scribble.modifier.bold"),
                (toggled) -> this.getRichSelectionManager().toggleModifier(Formatting.BOLD, toggled),
                x, y, 0, 0, 22, 19, false));
        italicButton = addDrawableChild(new FormatButtonWidget(
                Text.translatable("text.scribble.modifier.italic"),
                (toggled) -> this.getRichSelectionManager().toggleModifier(Formatting.ITALIC, toggled),
                x, y + 19, 0, 19, 22, 17, false));
        underlineButton = addDrawableChild(new FormatButtonWidget(
                Text.translatable("text.scribble.modifier.underline"),
                (toggled) -> this.getRichSelectionManager().toggleModifier(Formatting.UNDERLINE, toggled),
                x, y + 36, 0, 36, 22, 17, false));
        strikethroughButton = addDrawableChild(new FormatButtonWidget(
                Text.translatable("text.scribble.modifier.strikethrough"),
                (toggled) -> this.getRichSelectionManager().toggleModifier(Formatting.STRIKETHROUGH, toggled),
                x, y + 53, 0, 53, 22, 17, false));
        obfuscatedButton = addDrawableChild(new FormatButtonWidget(
                Text.translatable("text.scribble.modifier.obfuscated"),
                (toggled) -> this.getRichSelectionManager().toggleModifier(Formatting.OBFUSCATED, toggled),
                x, y + 70, 0, 70, 22, 18, false));
    }

    @Inject(method = "<init>", at = @At(value = "TAIL"))
    public void init(PlayerEntity player, ItemStack itemStack, Hand hand, CallbackInfo ci) {
        // Replace the selection manager with our own
        currentPageSelectionManager = new RichSelectionManager(
                this::getCurrentPageText,
                this::setPageText,
                (string) -> this.pages.set(this.currentPage, string),
                (color, modifiers) -> {
                    boldButton.toggled = modifiers.contains(Formatting.BOLD);
                    italicButton.toggled = modifiers.contains(Formatting.ITALIC);
                    underlineButton.toggled = modifiers.contains(Formatting.UNDERLINE);
                    strikethroughButton.toggled = modifiers.contains(Formatting.STRIKETHROUGH);
                    obfuscatedButton.toggled = modifiers.contains(Formatting.OBFUSCATED);
                },
                this::getClipboard,
                this::setClipboard,
                text -> text.getAsFormattedString().length() < 1024
                        && this.textRenderer.getWrappedLinesHeight(text, 114) <= 128
        );

        // Load the text from NBT
        NbtCompound nbt = itemStack.getNbt();
        if (nbt != null) {
            BookScreen.filterPages(nbt, (string) -> richPages.add(RichText.fromFormattedString(string)));
        }
    }

    @Inject(method = "init", at = @At(value = "HEAD"))
    private void initScreen(CallbackInfo ci) {
        initButtons();
    }

    @Inject(method = "updateButtons", at = @At(value = "HEAD"))
    private void updateButtons(CallbackInfo ci) {
        this.boldButton.visible = !this.signing;
        this.italicButton.visible = !this.signing;
        this.underlineButton.visible = !this.signing;
        this.strikethroughButton.visible = !this.signing;
        this.obfuscatedButton.visible = !this.signing;
    }

    // We cancel any drags outside the width of the book interface.
    // This needs to be here, because in this GUI no buttons can ever be focused.
    @Inject(method = "mouseDragged", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/BookEditScreen;getPageContent()Lnet/minecraft/client/gui/screen/ingame/BookEditScreen$PageContent;", shift = At.Shift.BEFORE), cancellable = true)
    private void mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> cir) {
        if (mouseX < (this.width - 152) / 2.0 || mouseX > (this.width + 152) / 2.0) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    /**
     * @reason We can't be too sure that the `pages` variable is accurate on checking if it's
     * empty, so we check with `richPages` instead.
     * @author chrrrs
     */
    @Overwrite
    private void removeEmptyPages() {
        ListIterator<RichText> listIterator = this.richPages.listIterator(this.richPages.size());
        while (listIterator.hasPrevious() && listIterator.previous().isEmpty()) {
            listIterator.remove();
        }
    }

    @Inject(method = "appendNewPage", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
    private void appendNewPage(CallbackInfo ci) {
        richPages.add(RichText.empty());
    }

    /**
     * @reason This method should not be called, as it is replaced by {@link #setPageText}.
     * @author chrrrs
     */
    @Overwrite
    private void setPageContent(String newContent) {
        Scribble.LOGGER.warn("setPageContent() was called, but ignored.");
    }

    /**
     * The contents of this method are basically a 1 to 1 translation of
     * {@link BookEditScreen#createPageContent}, but edited to work with rich text.
     *
     * @reason To help with rich page editing, we replace this function to always
     * return a {@link RichPageContent} instance. There's too many changes
     * here to just mixin, so we overwrite it instead.
     * @author chrrrs
     */
    @Overwrite
    private BookEditScreen.PageContent createPageContent() {
        RichText text = getCurrentPageText();
        String plainText = text.getPlainText();

        if (text.isEmpty()) {
            return RichPageContent.EMPTY;
        }

        int selectionStart = this.currentPageSelectionManager.getSelectionStart();
        int selectionEnd = this.currentPageSelectionManager.getSelectionEnd();

        IntList lineStarts = new IntArrayList();
        List<RichPageContent.Line> lines = new ArrayList<>();

        MutableBoolean endsWithNewline = new MutableBoolean();
        TextHandler textHandler = this.textRenderer.getTextHandler();

        MutableInt lineNumber = new MutableInt();
        MutableInt charNumber = new MutableInt();
        textHandler.wrapLines(text, 114, Style.EMPTY, (stringVisitable, continued) -> {
            String string = stringVisitable.getString();
            int length = string.length();

            int i = lineNumber.getAndIncrement();
            int start = charNumber.getValue();

            // We need to count the last char, because the visitor cuts it off.
            // This is basically replacing the `retainTrailingWordSplit` parameter.
            boolean newline = false;
            if (plainText.length() > start + length) {
                char lastChar = plainText.charAt(start + length);
                if (lastChar == '\n') {
                    newline = true;
                    length++;
                } else if (lastChar == ' ') {
                    length++;
                }
            }

            endsWithNewline.setValue(newline);
            charNumber.add(length);

            int y = i * 9;
            BookEditScreen.Position position = this.absolutePositionToScreenPosition(new BookEditScreen.Position(0, y));
            lineStarts.add(start);

            lines.add(new RichPageContent.Line(stringVisitable, position.x, position.y));
        });

        int[] lineStartsArray = lineStarts.toIntArray();
        boolean atEnd = selectionStart == plainText.length();

        // We try to find the cursor position.
        BookEditScreen.Position cursorPosition;
        if (atEnd && endsWithNewline.isTrue()) {
            cursorPosition = new BookEditScreen.Position(0, lines.size() * 9);
        } else {
            int i = getLineFromOffset(lineStartsArray, selectionStart);
            int width = this.textRenderer.getWidth(text.subText(lineStartsArray[i], selectionStart));
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
                    int s = (int) textHandler.getWidth(lines.get(i).getStringVisitable());
                    selectionRectangles.add(this.getRectFromCorners(new BookEditScreen.Position(0, y), new BookEditScreen.Position(s, y + 9)));
                }

                selectionRectangles.add(this.getSelectionRectangle(text, textHandler, lineStartsArray[endLine], selEnd, endLine * 9, lineStartsArray[endLine]));
            }
        }

        return new RichPageContent(text, cursorPosition, atEnd, lineStartsArray, lines.toArray(new BookEditScreen.Line[0]), selectionRectangles.toArray(new Rect2i[0]));
    }
}
