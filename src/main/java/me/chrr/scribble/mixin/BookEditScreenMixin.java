package me.chrr.scribble.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.chrr.scribble.Scribble;
import me.chrr.scribble.book.BookFile;
import me.chrr.scribble.book.FileChooser;
import me.chrr.scribble.book.RichText;
import me.chrr.scribble.gui.button.ColorSwatchWidget;
import me.chrr.scribble.gui.button.IconButtonWidget;
import me.chrr.scribble.gui.button.ModifierButtonWidget;
import me.chrr.scribble.gui.edit.RichEditBox;
import me.chrr.scribble.gui.edit.RichEditBoxWidget;
import me.chrr.scribble.history.CommandManager;
import me.chrr.scribble.history.HistoryListener;
import me.chrr.scribble.history.command.EditCommand;
import me.chrr.scribble.history.command.PageDeleteCommand;
import me.chrr.scribble.history.command.PageInsertCommand;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

@Mixin(BookEditScreen.class)
public abstract class BookEditScreenMixin extends Screen implements HistoryListener {
    //region Constants
    @Unique
    private static final Formatting[] scribble$COLORS = new Formatting[]{
            Formatting.BLACK, Formatting.DARK_GRAY,
            Formatting.GRAY, Formatting.WHITE,
            Formatting.DARK_RED, Formatting.RED,
            Formatting.GOLD, Formatting.YELLOW,
            Formatting.DARK_GREEN, Formatting.GREEN,
            Formatting.DARK_AQUA, Formatting.AQUA,
            Formatting.DARK_BLUE, Formatting.BLUE,
            Formatting.DARK_PURPLE, Formatting.LIGHT_PURPLE,
    };
    //endregion

    //region @Shadow declarations
    @Shadow
    private EditBoxWidget editBox;

    @Shadow
    @Final
    private List<String> pages;

    @Shadow
    private int currentPage;

    @Shadow
    @Final
    private PlayerEntity player;

    @Shadow
    protected abstract void updatePage();

    @Shadow
    protected abstract void updatePreviousPageButtonVisibility();
    //endregion

    //region Variables
    @Unique
    private ModifierButtonWidget scribble$boldButton;
    @Unique
    private ModifierButtonWidget scribble$italicButton;
    @Unique
    private ModifierButtonWidget scribble$underlineButton;
    @Unique
    private ModifierButtonWidget scribble$strikethroughButton;
    @Unique
    private ModifierButtonWidget scribble$obfuscatedButton;

    @Unique
    private List<ColorSwatchWidget> scribble$colorSwatches = List.of();

    @Unique
    private IconButtonWidget scribble$deletePageButton;
    @Unique
    private IconButtonWidget scribble$insertPageButton;

    @Unique
    private IconButtonWidget scribble$undoButton;
    @Unique
    private IconButtonWidget scribble$redoButton;

    @Unique
    private boolean scribble$dirty = false;
    @Unique
    private final CommandManager scribble$commandManager = new CommandManager(this);
    //endregion

    // Dummy constructor to match super class.
    private BookEditScreenMixin() {
        super(null);
    }

    @Redirect(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/EditBoxWidget;builder()Lnet/minecraft/client/gui/widget/EditBoxWidget$Builder;"))
    public EditBoxWidget.Builder buildEditBoxWidget() {
        return new RichEditBoxWidget.Builder()
                .onInvalidateFormat(this::scribble$invalidateFormattingButtons)
                .onHistoryPush(this::scribble$pushEditCommand);
    }

    @Unique
    private RichEditBoxWidget scribble$getRichEditBoxWidget() {
        return (RichEditBoxWidget) this.editBox;
    }

    //region Formatting Buttons
    @Inject(method = "init", at = @At(value = "TAIL"))
    public void initFormattingButtons(CallbackInfo ci) {
        int x = this.width / 2 + 78;
        int y = Scribble.getBookScreenYOffset(height) + 12;

        // Modifier buttons
        scribble$boldButton = scribble$addModifierButton(
                Formatting.BOLD, Text.translatable("text.scribble.modifier.bold"),
                x, y, 0, 0, 22, 19);
        scribble$italicButton = scribble$addModifierButton(
                Formatting.ITALIC, Text.translatable("text.scribble.modifier.italic"),
                x, y + 19, 0, 19, 22, 17);
        scribble$underlineButton = scribble$addModifierButton(
                Formatting.UNDERLINE, Text.translatable("text.scribble.modifier.underline"),
                x, y + 36, 0, 36, 22, 17);
        scribble$strikethroughButton = scribble$addModifierButton(
                Formatting.STRIKETHROUGH, Text.translatable("text.scribble.modifier.strikethrough"),
                x, y + 53, 0, 53, 22, 17);
        scribble$obfuscatedButton = scribble$addModifierButton(
                Formatting.OBFUSCATED, Text.translatable("text.scribble.modifier.obfuscated"),
                x, y + 70, 0, 70, 22, 18);

        // Color swatches
        RichEditBoxWidget editBox = scribble$getRichEditBoxWidget();
        scribble$colorSwatches = new ArrayList<>(scribble$COLORS.length);
        for (int i = 0; i < scribble$COLORS.length; i++) {
            Formatting color = scribble$COLORS[i];

            int dx = (i % 2) * 8;
            int dy = (i / 2) * 8;

            ColorSwatchWidget swatch = new ColorSwatchWidget(
                    Text.translatable("text.scribble.color." + color.getName()), color,
                    () -> editBox.applyFormatting(color, true),
                    x + 3 + dx, y + 95 + dy, 8, 8,
                    editBox.color == color
            );

            ColorSwatchWidget widget = addDrawableChild(swatch);
            scribble$colorSwatches.add(widget);
        }

        scribble$invalidateFormattingButtons();
    }

    @Unique
    private ModifierButtonWidget scribble$addModifierButton(Formatting modifier, Text tooltip, int x, int y, int u, int v, int width, int height) {
        RichEditBoxWidget editBox = scribble$getRichEditBoxWidget();
        ModifierButtonWidget button = new ModifierButtonWidget(
                tooltip, (toggled) -> editBox.applyFormatting(modifier, toggled),
                x, y, u, v, width, height,
                editBox.modifiers.contains(modifier));
        return addDrawableChild(button);
    }

    // Un-focus modifier and swatch buttons after they've been clicked, so you can continue typing.
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);

        // FIXME: is there not a better way to do this?
        Element focused = getFocused();
        if (focused instanceof ModifierButtonWidget || focused instanceof ColorSwatchWidget) {
            this.setFocused(editBox);
        }

        return handled;
    }

    @Unique
    private void scribble$invalidateFormattingButtons() {
        RichEditBoxWidget editBox = scribble$getRichEditBoxWidget();

        // Sometimes, these buttons get invalidated on screen initialize, when the buttons don't exist yet.
        // We can just return in that case.
        if (scribble$boldButton == null) {
            return;
        }

        scribble$boldButton.toggled = editBox.modifiers.contains(Formatting.BOLD);
        scribble$italicButton.toggled = editBox.modifiers.contains(Formatting.ITALIC);
        scribble$underlineButton.toggled = editBox.modifiers.contains(Formatting.UNDERLINE);
        scribble$strikethroughButton.toggled = editBox.modifiers.contains(Formatting.STRIKETHROUGH);
        scribble$obfuscatedButton.toggled = editBox.modifiers.contains(Formatting.OBFUSCATED);

        scribble$setSwatchColor(editBox.color);
    }

    @Unique
    private void scribble$setSwatchColor(Formatting color) {
        for (ColorSwatchWidget swatch : scribble$colorSwatches) {
            swatch.setToggled(swatch.getColor() == color);
        }
    }
    //endregion

    //region Page Buttons
    @Inject(method = "init", at = @At(value = "HEAD"))
    public void initPageButtons(CallbackInfo ci) {
        int x = this.width / 2 - 96;
        int y = Scribble.getBookScreenYOffset(height) + 12;

        scribble$deletePageButton = addDrawableChild(new IconButtonWidget(
                Text.translatable("text.scribble.action.delete_page"),
                this::scribble$deletePage,
                x + 78, y + 148, 0, 90, 12, 12));
        scribble$insertPageButton = addDrawableChild(new IconButtonWidget(
                Text.translatable("text.scribble.action.insert_new_page"),
                this::scribble$insertPage,
                x + 94, y + 148, 12, 90, 12, 12));
    }

    @Inject(method = "updatePreviousPageButtonVisibility", at = @At(value = "HEAD"))
    public void invalidatePageButtons(CallbackInfo ci) {
        scribble$deletePageButton.visible = this.pages.size() > 1;
        scribble$insertPageButton.visible = this.pages.size() < 100;
    }

    @Unique
    private void scribble$deletePage() {
        if (this.pages.size() > 1) {
            // See scribble$history$deletePage for implementation.
            PageDeleteCommand command = new PageDeleteCommand(this.currentPage,
                    this.scribble$getRichEditBoxWidget().getRichEditBox().getRichText());
            command.execute(this);

            scribble$commandManager.push(command);
            this.scribble$dirty = true;
        }
    }

    @Unique
    private void scribble$insertPage() {
        if (this.pages.size() < 100) {
            // See scribble$history$insertPage for implementation.
            PageInsertCommand command = new PageInsertCommand(this.currentPage);
            command.execute(this);

            scribble$commandManager.push(command);
            this.scribble$dirty = true;
        }
    }
    //endregion

    //region Action Buttons
    @Inject(method = "init", at = @At(value = "TAIL"))
    public void initActionButtons(CallbackInfo ci) {
        int ax = this.width / 2 - 78 - 7 - 12;
        int ay = Scribble.getBookScreenYOffset(height) + 12 + 4;

        scribble$undoButton = addDrawableChild(new IconButtonWidget(
                Text.translatable("text.scribble.action.undo"),
                () -> {
                    scribble$commandManager.tryUndo();
                    this.scribble$invalidateActionButtons();
                },
                ax, ay, 24, 90, 12, 12));
        scribble$redoButton = addDrawableChild(new IconButtonWidget(
                Text.translatable("text.scribble.action.redo"),
                () -> {
                    scribble$commandManager.tryRedo();
                    this.scribble$invalidateActionButtons();
                },
                ax, ay + 12, 36, 90, 12, 12));

        addDrawableChild(new IconButtonWidget(
                Text.translatable("text.scribble.action.save_book_to_file"),
                () -> FileChooser.chooseBook(true, this::scribble$saveTo),
                ax, ay + 12 * 2 + 4, 48, 90, 12, 12));
        addDrawableChild(new IconButtonWidget(
                Text.translatable("text.scribble.action.load_book_from_file"),
                () -> this.scribble$confirmIf(true, "overwrite_warning",
                        () -> FileChooser.chooseBook(false, this::scribble$loadFrom)),
                ax, ay + 12 * 3 + 4, 60, 90, 12, 12));

        scribble$invalidateActionButtons();
    }

    @Unique
    private void scribble$invalidateActionButtons() {
        scribble$undoButton.active = scribble$commandManager.canUndo();
        scribble$redoButton.active = scribble$commandManager.canRedo();
    }

    @Inject(method = "keyPressed", at = @At(value = "HEAD"), cancellable = true)
    public void onActionKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (Screen.hasControlDown() && !Screen.hasAltDown()) {
            boolean shift = Screen.hasShiftDown();
            if ((keyCode == GLFW.GLFW_KEY_Z && !shift && scribble$undoButton.active)) {
                scribble$undoButton.onPress();
                cir.setReturnValue(true);
            } else if (((keyCode == GLFW.GLFW_KEY_Z && shift) || (keyCode == GLFW.GLFW_KEY_Y && !shift)) && scribble$redoButton.active) {
                scribble$redoButton.onPress();
                cir.setReturnValue(true);
            }
        }
    }
    //endregion

    //region History
    @Override
    public void scribble$history$switchPage(int page) {
        if (page < 0 || page >= this.pages.size())
            return;

        this.currentPage = page;
        this.updatePage();
        this.updatePreviousPageButtonVisibility();
    }

    @Override
    public void scribble$history$setFormat(@Nullable Formatting color, Set<Formatting> modifiers) {
        RichEditBoxWidget editBox = this.scribble$getRichEditBoxWidget();
        editBox.color = color;
        editBox.modifiers = new HashSet<>(modifiers);
        this.scribble$invalidateFormattingButtons();
    }

    @Override
    public void scribble$history$insertPage(int page, @Nullable RichText content) {
        this.scribble$history$switchPage(page);

        String text = "";
        if (content != null)
            text = content.getAsFormattedString();

        this.pages.add(page, text);
        this.updatePage();
        this.updatePreviousPageButtonVisibility();
    }

    @Override
    public void scribble$history$deletePage(int page) {
        this.scribble$history$switchPage(page);

        this.pages.remove(page);
        if (this.currentPage == this.pages.size()) {
            this.currentPage -= 1;
        }

        this.updatePage();
        this.updatePreviousPageButtonVisibility();
    }

    @Override
    public RichEditBox scribble$history$getRichEditBox() {
        return this.scribble$getRichEditBoxWidget().getRichEditBox();
    }

    @Unique
    private void scribble$pushEditCommand(EditCommand command) {
        RichEditBoxWidget editBox = this.scribble$getRichEditBoxWidget();

        command.page = this.currentPage;
        command.color = editBox.color;
        command.modifiers = editBox.modifiers;

        this.scribble$commandManager.push(command);
        this.scribble$invalidateActionButtons();
    }
    //endregion

    //region Skip to first/last page
    // When shift is held down, skip to the last page.
    @Inject(method = "openNextPage", at = @At(value = "HEAD"), cancellable = true)
    public void openNextPage(CallbackInfo ci) {
        int lastPage = this.pages.size() - 1;
        if (this.currentPage < lastPage && Screen.hasShiftDown()) {
            this.currentPage = lastPage;
            this.updatePage();
            this.updatePreviousPageButtonVisibility();
            ci.cancel();
        }
    }

    // When shift is held down, skip to the first page.
    @Inject(method = "openPreviousPage", at = @At(value = "HEAD"), cancellable = true)
    public void openPreviousPage(CallbackInfo ci) {
        if (Screen.hasShiftDown()) {
            this.currentPage = 0;
            this.updatePage();
            this.updatePreviousPageButtonVisibility();
            ci.cancel();
        }
    }
    //endregion

    //region Saving and Loading
    @Unique
    private void scribble$saveTo(Path path) {
        try {
            BookFile bookFile = new BookFile(this.player.getGameProfile().getName(), this.pages);
            bookFile.writeJson(path);
        } catch (Exception e) {
            Scribble.LOGGER.error("could not save book to file", e);
        }
    }

    @Unique
    private void scribble$loadFrom(Path path) {
        try {
            BookFile bookFile = BookFile.readFile(path);

            this.pages.clear();
            this.pages.addAll(bookFile.pages());
            this.currentPage = 0;

            this.updatePage();
            this.updatePreviousPageButtonVisibility();
            this.scribble$dirty = true;
        } catch (Exception e) {
            Scribble.LOGGER.error("could not load book from file", e);
        }
    }
    //endregion

    //region Overwrite / Close confirmations
    // We want to keep track of if the book has been edited (i.e. if it's "dirty").
    @ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/EditBoxWidget;setChangeListener(Ljava/util/function/Consumer;)V"), index = 0)
    public Consumer<String> modifyChangeListener(Consumer<String> changeListener) {
        return (text) -> {
            if (!text.equals(this.pages.get(this.currentPage))) {
                this.scribble$dirty = true;
            }

            changeListener.accept(text);
        };
    }

    // Show a confirmation dialog if passed condition is true, otherwise run the runnable immediately.
    @Unique
    public void scribble$confirmIf(boolean condition, String name, Runnable runnable) {
        if (condition && this.client != null) {
            this.client.setScreen(new ConfirmScreen(
                    confirmed -> {
                        this.client.setScreen(this);
                        if (confirmed) runnable.run();
                    },
                    Text.translatable("text.scribble." + name + ".title"),
                    Text.translatable("text.scribble." + name + ".description")
            ));
        } else {
            runnable.run();
        }
    }

    // Show a confirmation dialog when trying to exit the screen if the book has been edited
    @Override
    public void close() {
        this.scribble$confirmIf(this.scribble$dirty, "quit_without_saving", super::close);
    }
    //endregion

    //region GUI Centering
    // If we need to center the GUI, we shift the Y of the texture draw call down.
    @ModifyArg(method = "renderBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIFFIIII)V"), index = 3)
    public int shiftBackgroundY(int y) {
        return Scribble.getBookScreenYOffset(height) + y;
    }

    // If we need to center the GUI, we shift the Y of the buttons down.
    @WrapOperation(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/BookEditScreen;addDrawableChild(Lnet/minecraft/client/gui/Element;)Lnet/minecraft/client/gui/Element;"))
    public <T extends Element & Drawable & Selectable> T shiftButtonY(BookEditScreen instance, Element element, Operation<T> original) {
        if (element instanceof Widget widget) {
            widget.setY(widget.getY() + Scribble.getBookScreenYOffset(height));
        }

        return original.call(instance, element);
    }

    // If we need to center the GUI, we shift the Y of the page number down.
    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIIZ)V"), index = 3)
    public int shiftPageNumberY(int y) {
        return Scribble.getBookScreenYOffset(height) + y;
    }
    //endregion

    //region Bug fixes
    // We cancel any drags outside the width of the book interface.
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (mouseX < (this.width - 152) / 2.0 || mouseX > (this.width + 152) / 2.0) {
            return true;
        } else {
            return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
    }
    //endregion
}
