package me.chrr.scribble.mixin;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import me.chrr.scribble.KeyboardUtil;
import me.chrr.scribble.Scribble;
import me.chrr.scribble.book.*;
import me.chrr.scribble.gui.ColorSwatchWidget;
import me.chrr.scribble.gui.IconButtonWidget;
import me.chrr.scribble.gui.ModifierButtonWidget;
import me.chrr.scribble.history.BookEditScreenMemento;
import me.chrr.scribble.history.command.*;
import me.chrr.scribble.history.Restorable;
import me.chrr.scribble.history.command.Command;
import me.chrr.scribble.history.CommandManager;
import net.minecraft.client.font.TextHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.navigation.GuiNavigationPath;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.client.gui.widget.Widget;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Path;
import java.util.*;

//? if >=1.21.2
import net.minecraft.component.type.WritableBookContentComponent;

@Mixin(BookEditScreen.class)
public abstract class BookEditScreenMixin extends Screen implements PagesListener, Restorable<BookEditScreenMemento> {

    @Unique
    private static final int MAX_PAGES_NUMBER = 100;

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

    @Unique
    private static final Formatting DEFAULT_COLOR = Formatting.BLACK;

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
    private final CommandManager commandManager = new CommandManager(
            Scribble.CONFIG_MANAGER.getConfig().editHistorySize
    );

    @Unique
    @Nullable
    private Formatting activeColor = DEFAULT_COLOR;

    @Unique
    @NotNull
    private Set<Formatting> activeModifiers = new HashSet<>();

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
    @NotNull
    private List<ColorSwatchWidget> colorSwatches = List.of();

    @Unique
    private IconButtonWidget deletePageButton;
    @Unique
    private IconButtonWidget insertPageButton;

    @Unique
    private IconButtonWidget saveBookButton;
    @Unique
    private IconButtonWidget loadBookButton;

    // Dummy constructor to match super class.
    private BookEditScreenMixin() {
        super(null);
    }

    @Unique
    private String getRawClipboard() {
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
        }

        // also update original page list to keep it in sync with richPages
        if (this.currentPage >= 0 && this.currentPage < this.pages.size()) {
            this.pages.set(this.currentPage, newText.getAsFormattedString());
        }

        this.dirty = true;
        this.invalidatePageContent();
    }

    @Unique
    private RichSelectionManager getRichSelectionManager() {
        // This is always the case, as we replace it in #init.
        return (RichSelectionManager) this.currentPageSelectionManager;
    }

    @Unique
    private void initButtons() {
        int x = this.width / 2 + 78;
        int y = Scribble.getBookScreenYOffset(height) + 12;

        // Modifier buttons
        boldButton = addModifierButton(
                Formatting.BOLD, Text.translatable("text.scribble.modifier.bold"),
                x, y, 0, 0, 22, 19
        );
        italicButton = addModifierButton(
                Formatting.ITALIC, Text.translatable("text.scribble.modifier.italic"),
                x, y + 19, 0, 19, 22, 17
        );
        underlineButton = addModifierButton(
                Formatting.UNDERLINE, Text.translatable("text.scribble.modifier.underline"),
                x, y + 36, 0, 36, 22, 17
        );
        strikethroughButton = addModifierButton(
                Formatting.STRIKETHROUGH, Text.translatable("text.scribble.modifier.strikethrough"),
                x, y + 53, 0, 53, 22, 17
        );
        obfuscatedButton = addModifierButton(
                Formatting.OBFUSCATED, Text.translatable("text.scribble.modifier.obfuscated"),
                x, y + 70, 0, 70, 22, 18
        );

        // Color swatches
        colorSwatches = new ArrayList<>(COLORS.length);
        for (int i = 0; i < COLORS.length; i++) {
            Formatting color = COLORS[i];

            int dx = (i % 2) * 8;
            int dy = (i / 2) * 8;

            ColorSwatchWidget swatch = new ColorSwatchWidget(
                    Text.translatable("text.scribble.color." + color.getName()), color,
                    () -> changeActiveColor(color),
                    x + 3 + dx, y + 95 + dy, 8, 8
            );
            swatch.setToggled(activeColor == color);

            ColorSwatchWidget widget = addDrawableChild(swatch);
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

    @Unique
    private ModifierButtonWidget addModifierButton(Formatting modifier, Text tooltip, int x, int y, int u, int v, int width, int height) {
        ModifierButtonWidget button = new ModifierButtonWidget(
                tooltip,
                (toggled) -> toggleActiveModifier(modifier, toggled),
                x, y, u, v, width, height,
                activeModifiers.contains(modifier)
        );

        return addDrawableChild(button);
    }

    @Inject(method = "<init>", at = @At(value = "TAIL"))
    //? if <1.21.2 {
    /*public void init(PlayerEntity player, ItemStack stack, Hand hand, CallbackInfo ci) {
     *///?} else
    public void init(PlayerEntity player, ItemStack stack, Hand hand, WritableBookContentComponent writableBookContent, CallbackInfo ci) {
        // Replace the selection manager with our own
        currentPageSelectionManager = new RichSelectionManager(
                this::getCurrentPageText,
                this::setPageText,
                this::onCursorFormattingChanged,
                this::getRawClipboard,
                this::setClipboard,
                text -> text.getAsFormattedString().length() < 1024
                        && this.textRenderer.getWrappedLinesHeight(text, 114) <= 128,
                () -> Optional.ofNullable(this.activeColor).orElse(DEFAULT_COLOR),
                () -> this.activeModifiers
        );

        // Load the pages into richPages
        for (String page : this.pages) {
            this.richPages.add(RichText.fromFormattedString(page));
        }

        // After loading the pages, we update cursor formatting.
        getRichSelectionManager().notifyCursorFormattingChanged();
    }

    @Unique
    private void changeActiveColor(@NotNull Formatting newColor) {
        if (newColor == activeColor) {
            // Apply color for selection only if(or):
            // - new color is different
            // - cursor color is not undefined (multiple colors text selected e.g.)
            // Otherwise - ignore
            return;
        }

        Command command = new ActionCommand<>(this, () -> {
            activeColor = newColor;
            invalidateFormattingButtons();
            getRichSelectionManager().applyColorForSelection(newColor);
        });
        commandManager.execute(command);
    }

    @Unique
    public void toggleActiveModifier(Formatting modifier, boolean toggled) {
        Command command = new ActionCommand<>(this, () -> {
            if (toggled) {
                activeModifiers.add(modifier);
            } else {
                activeModifiers.remove(modifier);
            }

            invalidateFormattingButtons();
            getRichSelectionManager().toggleModifierForSelection(modifier, toggled);
        });
        commandManager.execute(command);
    }

    @Inject(method = "init", at = @At(value = "HEAD"))
    private void initScreen(CallbackInfo ci) {
        initButtons();
    }

    @Inject(method = "updateButtons", at = @At(value = "HEAD"))
    private void invalidateControlButtons(CallbackInfo ci) {
        Optional.ofNullable(boldButton).ifPresent(button -> button.visible = !this.signing);
        Optional.ofNullable(italicButton).ifPresent(button -> button.visible = !this.signing);
        Optional.ofNullable(underlineButton).ifPresent(button -> button.visible = !this.signing);
        Optional.ofNullable(strikethroughButton).ifPresent(button -> button.visible = !this.signing);
        Optional.ofNullable(obfuscatedButton).ifPresent(button -> button.visible = !this.signing);

        for (ColorSwatchWidget swatch : colorSwatches) {
            swatch.visible = !signing;
        }

        Optional.ofNullable(deletePageButton).ifPresent(button -> button.visible = !signing && richPages.size() > 1);
        Optional.ofNullable(insertPageButton).ifPresent(button ->
                button.visible = !signing && richPages.size() < MAX_PAGES_NUMBER
        );


        boolean showSaveLoadButtons = Scribble.CONFIG_MANAGER.getConfig().showSaveLoadButtons;
        Optional.ofNullable(saveBookButton).ifPresent(button -> button.visible = !signing && showSaveLoadButtons);
        Optional.ofNullable(loadBookButton).ifPresent(button -> button.visible = !signing && showSaveLoadButtons);
    }

    @Unique
    private void invalidateFormattingButtons() {
        Optional.ofNullable(boldButton).ifPresent(button -> button.toggled = activeModifiers.contains(Formatting.BOLD));
        Optional.ofNullable(italicButton).ifPresent(button -> button.toggled = activeModifiers.contains(Formatting.ITALIC));
        Optional.ofNullable(underlineButton).ifPresent(button -> button.toggled = activeModifiers.contains(Formatting.UNDERLINE));
        Optional.ofNullable(strikethroughButton).ifPresent(button -> button.toggled = activeModifiers.contains(Formatting.STRIKETHROUGH));
        Optional.ofNullable(obfuscatedButton).ifPresent(button -> button.toggled = activeModifiers.contains(Formatting.OBFUSCATED));
        setSwatchColor(activeColor);
    }

    @Unique
    private void setSwatchColor(Formatting color) {
        for (ColorSwatchWidget swatch : colorSwatches) {
            swatch.setToggled(swatch.getColor() == color);
        }
    }

    @Unique
    private void onCursorFormattingChanged(@Nullable Formatting color, Set<Formatting> modifiers) {
        this.activeColor = color;
        this.activeModifiers = modifiers;

        invalidateFormattingButtons();
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
            commandManager.clear();

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
        Command command = new DeletePageCommand(richPages, currentPage, this);
        commandManager.execute(command);
    }

    @Unique
    private void insertPage() {
        if (this.richPages.size() < MAX_PAGES_NUMBER) {
            Command command = new InsertPageCommand(richPages, currentPage, this);
            commandManager.execute(command);
        }
    }

    // If we need to center the GUI, we shift the Y of the texture draw call down.
    // For 1.20.1, this draw call happens in render, so we don't need to do it separately.
    //? if >=1.20.2 {
    //? if >=1.21.2 {
    @ModifyArg(method = "renderBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Ljava/util/function/Function;Lnet/minecraft/util/Identifier;IIFFIIII)V"), index = 3)
    //?} else
    /*@ModifyArg(method = "renderBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lnet/minecraft/util/Identifier;IIIIII)V"), index = 2)*/
    public int shiftBackgroundY(int y) {
        return Scribble.getBookScreenYOffset(height) + y;
    }
    //?}

    // If we need to center the GUI, we shift the Y of the button dimensions down.
    @Redirect(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/BookEditScreen;addDrawableChild(Lnet/minecraft/client/gui/Element;)Lnet/minecraft/client/gui/Element;"))
    public <T extends Element & Drawable & Selectable> T shiftButtonY(BookEditScreen screen, T element) {
        if (element instanceof Widget widget) {
            widget.setY(widget.getY() + Scribble.getBookScreenYOffset(height));
        }

        return addDrawableChild(element);
    }

    // If we need to center the GUI, we shift any mouse clicks down.
    @ModifyArg(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/BookEditScreen;screenPositionToAbsolutePosition(Lnet/minecraft/client/gui/screen/ingame/BookEditScreen$Position;)Lnet/minecraft/client/gui/screen/ingame/BookEditScreen$Position;"))
    public BookEditScreen.Position shiftMouseClicks(BookEditScreen.Position position) {
        return new BookEditScreen.Position(position.x, position.y - Scribble.getBookScreenYOffset(height));
    }

    // If we need to center the GUI, we shift any mouse drags down.
    @ModifyArg(method = "mouseDragged", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/BookEditScreen;screenPositionToAbsolutePosition(Lnet/minecraft/client/gui/screen/ingame/BookEditScreen$Position;)Lnet/minecraft/client/gui/screen/ingame/BookEditScreen$Position;"))
    public BookEditScreen.Position shiftMouseDrags(BookEditScreen.Position position) {
        return new BookEditScreen.Position(position.x, position.y - Scribble.getBookScreenYOffset(height));
    }

    // When rendering, we translate the matrices of the draw context to draw the text further down if needed.
    // Note that this happens after the parent screen render, so only the text in the book is shifted.
    //? if >=1.20.2 {
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;render(Lnet/minecraft/client/gui/DrawContext;IIF)V", shift = At.Shift.AFTER))
    //?} else
    /*@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/BookEditScreen;renderBackground(Lnet/minecraft/client/gui/DrawContext;)V", shift = At.Shift.AFTER))*/
    public void translateRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        context.getMatrices().push();
        context.getMatrices().translate(0f, Scribble.getBookScreenYOffset(height), 0f);
    }

    // At the end of rendering, we need to pop those matrices we pushed.
    //? if >=1.20.2 {
    @Inject(method = "render", at = @At(value = "RETURN"))
    //?} else
    /*@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;render(Lnet/minecraft/client/gui/DrawContext;IIF)V"))*/
    public void popRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        context.getMatrices().pop();
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
    @Inject(method = "mouseDragged", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/BookEditScreen;getPageContent()Lnet/minecraft/client/gui/screen/ingame/BookEditScreen$PageContent;"), cancellable = true)
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

    @Redirect(method = "appendNewPage", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
    private boolean appendNewPage(List<String> page, Object empty) {
        // FIXME: It feels slightly confusing that we pass in richPages here, but at the same time
        //        use PagesListener to add plain-text pages. It makes it hard to follow.
        Command command = new InsertPageCommand(richPages, richPages.size(), this);
        commandManager.execute(command);
        return true;
    }

    @Override
    public void scribble$onPageAdded(int pageAddedIndex) {
        // sync with plain text(native) page list
        pages.add(pageAddedIndex, richPages.get(pageAddedIndex).getAsFormattedString());

        currentPage = pageAddedIndex;
        dirty = true;
        updateButtons();
        changePage();
    }

    @Override
    public void scribble$onPageRemoved(int pageRemovedIndex) {
        // sync with plain text(native) page list
        pages.remove(pageRemovedIndex);

        if (pageRemovedIndex < currentPage) {
            // a page before opened was removed
            // move the index to the left by 1 to keep the same page opened
            currentPage = Math.max(0, pageRemovedIndex - 1);
        } else if (currentPage >= richPages.size()) {
            // the last page was opened before removing
            currentPage = Math.max(0, richPages.size() - 1);
        }

        dirty = true;
        updateButtons();
        changePage();
    }

    /**
     * NOTE: This method is marked public, while the original method is private. Some mods
     * will try to access-modify `setPageContent` to be public, and thus crash the
     * game. While this will still produce incompatibilities, we at least try to
     * not crash.
     *
     * @reason This method should not be called, as it is replaced by {@link #setPageText}.
     * @author chrrrs
     */
    @Overwrite
    @SuppressWarnings("visibility")
    public void setPageContent(String newContent) {
        Scribble.LOGGER.warn("setPageContent() was called, but ignored.");
    }

    // NOTE: There are two "insert" calls in the original method. One is editing the book title, which
    //       takes a char, the other one is editing the page, which takes a String. We're targeting the
    //       second one.
    @Redirect(method = "charTyped", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/SelectionManager;insert(Ljava/lang/String;)V"))
    private void charTypedEditMode(SelectionManager instance, String string) {
        Command command = new ActionCommand<>(this, () -> this.getRichSelectionManager().insert(string));
        commandManager.execute(command);
    }

    @Inject(method = "keyPressedEditMode", at = @At(value = "HEAD"), cancellable = true)
    private void keyPressedEditMode(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        // Override COPY and CUT shortcuts
        if (hasControlDown() && !hasAltDown() && (keyCode == GLFW.GLFW_KEY_C || keyCode == GLFW.GLFW_KEY_X)) {
            // Copy formatting when the config option is set and the SHIFT key is not held down.
            boolean shouldCopyFormatting = Scribble.CONFIG_MANAGER.getConfig().copyFormattingCodes && !hasShiftDown();

            // Put the selected text on the clipboard with or without formatting.
            String selectedText = getRichSelectionManager().getSelectedFormattedText();
            setClipboard(shouldCopyFormatting ? selectedText : Formatting.strip(selectedText));

            // Delete the selected text if we're cutting.
            if (keyCode == GLFW.GLFW_KEY_X) {
                Command command = new ActionCommand<>(this, () -> this.getRichSelectionManager().delete(0));
                commandManager.execute(command);
            }

            cir.setReturnValue(true);
            cir.cancel();
            return;
        }

        // Override PASTE and PASTE-without-formatting shortcut
        if (hasControlDown() && !hasAltDown() && keyCode == GLFW.GLFW_KEY_V) {
            // Get the text from the clipboard with or without formatting.
            String textToPaste = hasShiftDown() ? Formatting.strip(getRawClipboard()) : getRawClipboard();

            // Paste the text onto the page.
            Command command = new ActionCommand<>(this, () -> this.getRichSelectionManager().insert(textToPaste));
            commandManager.execute(command);

            cir.setReturnValue(true);
            cir.cancel();
            return;
        }

        // Undo on Ctrl-Z
        if (!hasShiftDown() && hasControlDown() && !hasAltDown() && KeyboardUtil.isKey(keyCode, "Z")) {
            commandManager.tryUndo();
            cir.setReturnValue(true);
            cir.cancel();
            return;
        }

        // Redo on Ctrl-Shift-Z and Ctrl-Y
        if (hasControlDown() && !hasAltDown() && ((hasShiftDown() && KeyboardUtil.isKey(keyCode, "Z")) || (!hasShiftDown() && KeyboardUtil.isKey(keyCode, "Y")))) {
            commandManager.tryRedo();
            cir.setReturnValue(true);
            cir.cancel();
            return;
        }

        // Override DELETE action
        if (keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            SelectionManager.SelectionType selectionType = Screen.hasControlDown()
                    ? SelectionManager.SelectionType.WORD
                    : SelectionManager.SelectionType.CHARACTER;

            // Delete after the cursor when holding DELETE, otherwise before.
            int offset = keyCode == GLFW.GLFW_KEY_DELETE ? 1 : -1;
            Command command = new ActionCommand<>(this, () -> this.getRichSelectionManager().delete(offset, selectionType));
            commandManager.execute(command);

            cir.setReturnValue(true);
            cir.cancel();
            return;
        }

        // We inject some hotkeys for toggling formatting options.
        if (hasControlDown() && !hasShiftDown() && !hasAltDown()) {
            if (keyCode == GLFW.GLFW_KEY_B) {
                Optional.ofNullable(boldButton).ifPresent(ModifierButtonWidget::toggle);
            } else if (keyCode == GLFW.GLFW_KEY_I) {
                Optional.ofNullable(italicButton).ifPresent(ModifierButtonWidget::toggle);
            } else if (keyCode == GLFW.GLFW_KEY_U) {
                Optional.ofNullable(underlineButton).ifPresent(ModifierButtonWidget::toggle);
            } else if (keyCode == GLFW.GLFW_KEY_MINUS) {
                Optional.ofNullable(strikethroughButton).ifPresent(ModifierButtonWidget::toggle);
            } else if (keyCode == GLFW.GLFW_KEY_K) {
                Optional.ofNullable(obfuscatedButton).ifPresent(ModifierButtonWidget::toggle);
            } else {
                return;
            }

            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @ModifyArg(method = "drawCursor", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;IIIZ)I"), index = 4)
    private int modifyEndCursorColor(int constant) {
        return activeColor == null || activeColor.getColorValue() == null ? constant : activeColor.getColorValue();
    }

    @ModifyArg(method = "drawCursor", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"), index = 4)
    private int modifyLineCursorColor(int constant) {
        return modifyEndCursorColor(constant) | 0xff000000;
    }

    // Don't switch focus when asked to switch focus. This is a workaround for
    // MC-262268 / #30 where widgets would flash when trying to switch focus.
    @Override
    protected void switchFocus(GuiNavigationPath path) {
        this.blur();
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


    @Override
    public BookEditScreenMemento scribble$createMemento() {
        RichSelectionManager selectionManager = this.getRichSelectionManager();
        return new BookEditScreenMemento(
                currentPage,
                selectionManager.selectionStart,
                selectionManager.selectionEnd,
                getCurrentPageText(),
                activeColor,
                Set.copyOf(activeModifiers)
        );
    }

    @Override
    public void scribble$restore(BookEditScreenMemento memento) {
        // restore opened page index
        currentPage = memento.pageIndex();
        updateButtons();
        changePage();

        // restore page content
        setPageText(memento.currentPageRichText());

        // restore text selection / cursor position
        RichSelectionManager selectionManager = this.getRichSelectionManager();
        selectionManager.setSelection(memento.selectionStart(), memento.selectionEnd());

        activeColor = memento.color();
        activeModifiers = new HashSet<>(memento.modifiers()); // to be sure it's mutable
        invalidateFormattingButtons();
    }
}
