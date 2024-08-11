package me.chrr.scribble.mixin;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import me.chrr.scribble.Scribble;
import me.chrr.scribble.book.*;
import me.chrr.scribble.gui.ColorSwatchWidget;
import me.chrr.scribble.gui.IconButtonWidget;
import me.chrr.scribble.gui.ModifierButtonWidget;
import net.minecraft.client.font.TextHandler;
import net.minecraft.client.gui.screen.ConfirmScreen;
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
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

@Mixin(BookEditScreen.class)
public abstract class BookEditScreenMixin extends Screen {
    @Unique
    private static final Formatting[] COLORS = new Formatting[]{
            Formatting.BLACK, Formatting.DARK_GRAY,
            Formatting.GRAY, Formatting.WHITE,
            Formatting.DARK_RED, Formatting.RED,
            Formatting.GOLD, Formatting.YELLOW,
            Formatting.DARK_GREEN, Formatting.GREEN,
            Formatting.DARK_AQUA, Formatting.AQUA,
            Formatting.DARK_BLUE, Formatting.BLUE,
            Formatting.DARK_PURPLE, Formatting.LIGHT_PURPLE,
    };

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
    @Final
    private PlayerEntity player;

    @Shadow
    protected abstract BookEditScreen.Position absolutePositionToScreenPosition(BookEditScreen.Position position);

    @Shadow
    static int getLineFromOffset(int[] lineStarts, int position) {
        return 0;
    }

    @Shadow
    protected abstract Rect2i getRectFromCorners(BookEditScreen.Position start, BookEditScreen.Position end);

    @Shadow
    protected abstract void setClipboard(String clipboard);

    @Shadow
    protected abstract void invalidatePageContent();

    @Shadow
    protected abstract void changePage();

    @Shadow
    protected abstract void updateButtons();
    //endregion

    // List of text on the pages of the book. This replaces the usual
    // `pages` variable in BookEditScreen.
    @Unique
    private final List<RichText> richPages = new ArrayList<>();

    @Unique
    private ModifierButtonWidget boldButton;
    @Unique
    private ModifierButtonWidget italicButton;
    @Unique
    private ModifierButtonWidget underlineButton;
    @Unique
    private ModifierButtonWidget strikethroughButton;
    @Unique
    private ModifierButtonWidget obfuscatedButton;

    @Unique
    private List<ColorSwatchWidget> colorSwatches;

    @Unique
    private IconButtonWidget deletePageButton;
    @Unique
    private IconButtonWidget insertPageButton;

    @Unique
    private IconButtonWidget saveBookButton;
    @Unique
    private IconButtonWidget loadBookButton;

    // Dummy constructor to match super class. The mixin derives from
    // `Screen` so we don't have to shadow as many methods.
    // This should never be called.
    protected BookEditScreenMixin(Text title) {
        super(title);
    }


    @Unique
    private String getClipboard() {
        // the original logic of BookEditScreen.getClipboard without Formatting.strip() call
        // to keep text styling modifiers in copied text
        return this.client != null ? client.keyboard.getClipboard().replaceAll("\\r", "") : "";
    }

    /**
     * RichText-based replacement for BookEditScreen#getLineSelectionRectangle
     */
    @Unique
    private Rect2i getSelectionRectangle(RichText text, TextHandler handler, int selectionStart, int selectionEnd, int lineY, int lineStart) {
        RichText toSelectionStart = text.subText(lineStart, selectionStart);
        RichText toSelectionEnd = text.subText(lineStart, selectionEnd);
        BookEditScreen.Position topLeft = new BookEditScreen.Position((int) handler.getWidth(toSelectionStart), lineY);
        BookEditScreen.Position bottomRight = new BookEditScreen.Position((int) handler.getWidth(toSelectionEnd), lineY + 9);
        return this.getRectFromCorners(topLeft, bottomRight);
    }

    /**
     * RichText-based replacement for BookEditScreen#getCurrentPageContent
     */
    @Unique
    private RichText getCurrentPageText() {
        return this.currentPage >= 0 && this.currentPage < this.richPages.size()
                ? this.richPages.get(this.currentPage)
                : RichText.empty();
    }

    /**
     * RichText replacement for BookEditScreen#setPageContent.
     */
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
        int y = 12;

        if (Scribble.shouldCenter) {
            y += (height - 192) / 3;
        }

        // Modifier buttons
        // They're all toggled off by default, this is fixed in #initScreen.
        boldButton = addDrawableChild(new ModifierButtonWidget(
                Text.translatable("text.scribble.modifier.bold"),
                (toggled) -> this.getRichSelectionManager().toggleModifier(Formatting.BOLD, toggled),
                x, y, 0, 0, 22, 19, false));
        italicButton = addDrawableChild(new ModifierButtonWidget(
                Text.translatable("text.scribble.modifier.italic"),
                (toggled) -> this.getRichSelectionManager().toggleModifier(Formatting.ITALIC, toggled),
                x, y + 19, 0, 19, 22, 17, false));
        underlineButton = addDrawableChild(new ModifierButtonWidget(
                Text.translatable("text.scribble.modifier.underline"),
                (toggled) -> this.getRichSelectionManager().toggleModifier(Formatting.UNDERLINE, toggled),
                x, y + 36, 0, 36, 22, 17, false));
        strikethroughButton = addDrawableChild(new ModifierButtonWidget(
                Text.translatable("text.scribble.modifier.strikethrough"),
                (toggled) -> this.getRichSelectionManager().toggleModifier(Formatting.STRIKETHROUGH, toggled),
                x, y + 53, 0, 53, 22, 17, false));
        obfuscatedButton = addDrawableChild(new ModifierButtonWidget(
                Text.translatable("text.scribble.modifier.obfuscated"),
                (toggled) -> this.getRichSelectionManager().toggleModifier(Formatting.OBFUSCATED, toggled),
                x, y + 70, 0, 70, 22, 18, false));

        // Color swatches
        colorSwatches = new ArrayList<>(COLORS.length);
        for (int i = 0; i < COLORS.length; i++) {
            Formatting color = COLORS[i];

            int dx = (i % 2) * 8;
            int dy = (i / 2) * 8;

            ColorSwatchWidget widget = addDrawableChild(new ColorSwatchWidget(
                    Text.translatable("text.scribble.color." + color.getName()), color,
                    () -> {
                        this.getRichSelectionManager().setColor(color);
                        this.setSwatchColor(color);
                    }, x + 3 + dx, y + 95 + dy, 8, 8
            ));

            colorSwatches.add(widget);
        }

        // Page buttons
        int px = this.width / 2 - 96;
        deletePageButton = addDrawableChild(new IconButtonWidget(
                Text.translatable("text.scribble.action.delete_page"),
                this::deletePage,
                px + 78, y + 148, 0, 90, 11, 12));
        insertPageButton = addDrawableChild(new IconButtonWidget(
                Text.translatable("text.scribble.action.insert_new_page"),
                this::insertPage,
                px + 94, y + 148, 22, 90, 11, 12));

        // Save / Load buttons
        int fx = this.width / 2 - 78 - 22;
        saveBookButton = addDrawableChild(new IconButtonWidget(
                Text.translatable("text.scribble.action.save_book_to_file"),
                () -> FileChooser.chooseBook(true, this::saveTo),
                fx, y, 44, 91, 18, 18));
        loadBookButton = addDrawableChild(new IconButtonWidget(
                Text.translatable("text.scribble.action.load_book_from_file"),
                () -> this.confirmOverwrite(() -> FileChooser.chooseBook(false, this::loadFrom)),
                fx, y + 18 + 2, 44, 109, 18, 18));
    }

    @Inject(method = "<init>", at = @At(value = "TAIL"))
    public void init(PlayerEntity player, ItemStack itemStack, Hand hand, CallbackInfo ci) {
        // Replace the selection manager with our own
        currentPageSelectionManager = new RichSelectionManager(
                this::getCurrentPageText,
                this::setPageText,
                (string) -> this.pages.set(this.currentPage, string),
                this::updateState,
                this::getClipboard,
                this::setClipboard,
                text -> text.getAsFormattedString().length() < 1024
                        && this.textRenderer.getWrappedLinesHeight(text, 114) <= 128
        );

        // Load the pages into richPages
        for (String page : this.pages) {
            this.richPages.add(RichText.fromFormattedString(page));
        }
    }

    @Inject(method = "init", at = @At(value = "HEAD"))
    private void initScreen(CallbackInfo ci) {
        initButtons();

        // We need to update the states of all the buttons again.
        this.getRichSelectionManager().updateSelectionFormatting();
    }

    @Inject(method = "updateButtons", at = @At(value = "HEAD"))
    private void updateButtons(CallbackInfo ci) {
        this.boldButton.visible = !this.signing;
        this.italicButton.visible = !this.signing;
        this.underlineButton.visible = !this.signing;
        this.strikethroughButton.visible = !this.signing;
        this.obfuscatedButton.visible = !this.signing;

        for (ColorSwatchWidget swatch : colorSwatches) {
            swatch.visible = !this.signing;
        }

        this.deletePageButton.visible = !this.signing && this.richPages.size() > 1;
        this.insertPageButton.visible = !this.signing;

        this.saveBookButton.visible = !this.signing;
        this.loadBookButton.visible = !this.signing;
    }

    @Unique
    private void updateState(@Nullable Formatting color, Set<Formatting> modifiers) {
        boldButton.toggled = modifiers.contains(Formatting.BOLD);
        italicButton.toggled = modifiers.contains(Formatting.ITALIC);
        underlineButton.toggled = modifiers.contains(Formatting.UNDERLINE);
        strikethroughButton.toggled = modifiers.contains(Formatting.STRIKETHROUGH);
        obfuscatedButton.toggled = modifiers.contains(Formatting.OBFUSCATED);
        setSwatchColor(color);
    }

    @Unique
    private void setSwatchColor(Formatting color) {
        for (ColorSwatchWidget swatch : colorSwatches) {
            swatch.setToggled(swatch.getColor() == color);
        }
    }

    /**
     * If the book is not empty, ask for confirmation before executing a function.
     *
     * @param callback function to call if player accepted overwriting.
     */
    @Unique
    private void confirmOverwrite(Runnable callback) {
        if (!richPages.stream().allMatch(RichText::isEmpty)) {
            if (client == null) {
                return;
            }

            client.setScreen(new ConfirmScreen(
                    confirmed -> {
                        if (confirmed) {
                            callback.run();
                        }

                        client.setScreen(this);
                    },
                    Text.translatable("text.scribble.overwrite_warning.title"),
                    Text.translatable("text.scribble.overwrite_warning.description")
            ));
        } else {
            callback.run();
        }
    }

    @Unique
    private void saveTo(Path path) {
        if (client == null) {
            return;
        }

        try {
            BookFile bookFile = new BookFile(this.player.getGameProfile().getName(), List.copyOf(richPages));
            bookFile.write(path);
        } catch (Exception e) {
            Scribble.LOGGER.error("could not save book to file", e);
        }
    }

    @Unique
    private void loadFrom(Path path) {
        try {
            BookFile bookFile = BookFile.read(path);

            this.richPages.clear();
            this.pages.clear();

            // Loading an empty book file would set the total amount of pages to 0.
            // We work around this by just inserting a new empty page.
            if (bookFile.pages().isEmpty()) {
                this.currentPage = 0;
                this.insertPage();
                return;
            }

            for (RichText page : bookFile.pages()) {
                this.richPages.add(page);
                this.pages.add(page.getAsFormattedString());
            }

            this.currentPage = 0;
            this.dirty = true;
            this.updateButtons();
            this.changePage();
        } catch (Exception e) {
            Scribble.LOGGER.error("could not load book from file", e);
        }
    }

    @Unique
    private void deletePage() {
        this.richPages.remove(this.currentPage);
        this.pages.remove(this.currentPage);
        this.dirty = true;

        this.currentPage = Math.min(this.currentPage, this.richPages.size() - 1);
        this.updateButtons();
        this.changePage();
    }

    @Unique
    private void insertPage() {
        if (this.richPages.size() < 100) {
            this.richPages.add(this.currentPage, RichText.empty());
            this.pages.add(this.currentPage, "");
            this.dirty = true;

            this.updateButtons();
            this.changePage();
        }
    }

    /**
     * Ask for confirmation before closing without saving.
     */
    @Override
    public void close() {
        if (this.dirty && this.client != null) {
            this.client.setScreen(new ConfirmScreen(
                    confirmed -> {
                        if (confirmed) {
                            super.close();
                        } else {
                            this.client.setScreen(this);
                        }
                    },
                    Text.translatable("text.scribble.quit_without_saving.title"),
                    Text.translatable("text.scribble.quit_without_saving.description")
            ));
        } else {
            super.close();
        }
    }

    // When shift is held down, skip to the last page.
    @Inject(method = "openNextPage", at = @At(value = "HEAD"), cancellable = true)
    public void openNextPage(CallbackInfo ci) {
        int lastPage = this.richPages.size() - 1;
        if (this.currentPage < lastPage && Screen.hasShiftDown()) {
            this.currentPage = lastPage;
            this.updateButtons();
            this.changePage();
            ci.cancel();
        }
    }

    // When shift is held down, skip to the first page.
    @Inject(method = "openPreviousPage", at = @At(value = "HEAD"), cancellable = true)
    public void openPreviousPage(CallbackInfo ci) {
        if (Screen.hasShiftDown()) {
            this.currentPage = 0;
            this.updateButtons();
            this.changePage();
            ci.cancel();
        }
    }

    // When asking for the current page content, we return the plain text.
    // This method is only actively used when double-clicking to select a word.
    @Redirect(method = "getCurrentPageContent", at = @At(value = "INVOKE", target = "Ljava/util/List;get(I)Ljava/lang/Object;"))
    public Object getCurrentPageContent(List<String> pages, int page) {
        return this.richPages.get(page).getPlainText();
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

    @Inject(method = "removeEmptyPages", at = @At(value = "TAIL"))
    private void removeEmptyPages(CallbackInfo ci) {
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

    @Inject(method = "keyPressedEditMode", at = @At(value = "HEAD"), cancellable = true)
    private void keyPressedEditMode(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        // We inject some hotkeys for toggling formatting options.
        if (hasControlDown() && !hasShiftDown() && !hasAltDown()) {
            if (keyCode == GLFW.GLFW_KEY_B) {
                this.boldButton.toggle();
            } else if (keyCode == GLFW.GLFW_KEY_I) {
                this.italicButton.toggle();
            } else if (keyCode == GLFW.GLFW_KEY_U) {
                this.underlineButton.toggle();
            } else if (keyCode == GLFW.GLFW_KEY_MINUS) {
                this.strikethroughButton.toggle();
            } else if (keyCode == GLFW.GLFW_KEY_K) {
                this.obfuscatedButton.toggle();
            } else {
                return;
            }

            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @ModifyArg(method = "drawCursor", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;IIIZ)I"), index = 4)
    private int modifyEndCursorColor(int constant) {
        Formatting color = this.getRichSelectionManager().getColor();
        return color == null || color.getColorValue() == null
                ? constant : color.getColorValue();
    }

    @ModifyArg(method = "drawCursor", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"), index = 4)
    private int modifyLineCursorColor(int constant) {
        return modifyEndCursorColor(constant) | 0xff000000;
    }

    /**
     * The contents of this method are basically a 1 to 1 translation of
     * BookEditScreen#createPageContent, but edited to work with rich text.
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
