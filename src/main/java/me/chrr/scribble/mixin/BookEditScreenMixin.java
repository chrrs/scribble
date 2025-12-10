package me.chrr.scribble.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.chrr.scribble.KeyboardUtil;
import me.chrr.scribble.Scribble;
import me.chrr.scribble.book.BookFile;
import me.chrr.scribble.book.FileChooser;
import me.chrr.scribble.book.RichText;
import me.chrr.scribble.config.Config;
import me.chrr.scribble.gui.PageNumberWidget;
import me.chrr.scribble.gui.button.ColorSwatchWidget;
import me.chrr.scribble.gui.button.IconButtonWidget;
import me.chrr.scribble.gui.button.ModifierButtonWidget;
import me.chrr.scribble.gui.edit.RichMultiLineTextField;
import me.chrr.scribble.gui.edit.RichEditBoxWidget;
import me.chrr.scribble.history.CommandManager;
import me.chrr.scribble.history.HistoryListener;
import me.chrr.scribble.history.command.EditCommand;
import me.chrr.scribble.history.command.PageDeleteCommand;
import me.chrr.scribble.history.command.PageInsertCommand;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
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
    private static final ChatFormatting[] scribble$COLORS = new ChatFormatting[]{
            ChatFormatting.BLACK, ChatFormatting.DARK_GRAY,
            ChatFormatting.GRAY, ChatFormatting.WHITE,
            ChatFormatting.DARK_RED, ChatFormatting.RED,
            ChatFormatting.GOLD, ChatFormatting.YELLOW,
            ChatFormatting.DARK_GREEN, ChatFormatting.GREEN,
            ChatFormatting.DARK_AQUA, ChatFormatting.AQUA,
            ChatFormatting.DARK_BLUE, ChatFormatting.BLUE,
            ChatFormatting.DARK_PURPLE, ChatFormatting.LIGHT_PURPLE,
    };
    //endregion

    //region @Shadow declarations
    @Shadow
    private MultiLineEditBox page;

    @Shadow
    @Final
    private List<String> pages;

    @Shadow
    private int currentPage;

    @Shadow
    @Final
    private Player owner;

    @Shadow
    protected abstract void updatePageContent();

    @Shadow
    protected abstract void updateButtonVisibility();
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
    private PageNumberWidget scribble$pageNumberWidget;

    @Unique
    private boolean scribble$dirty = false;
    @Unique
    private final CommandManager scribble$commandManager = new CommandManager(this);
    //endregion

    // Dummy constructor to match super class.
    private BookEditScreenMixin() {
        super(null);
    }

    @Redirect(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/MultiLineEditBox;builder()Lnet/minecraft/client/gui/components/MultiLineEditBox$Builder;"))
    public MultiLineEditBox.Builder buildEditBoxWidget() {
        return new RichEditBoxWidget.Builder()
                .onInvalidateFormat(this::scribble$invalidateFormattingButtons)
                .onHistoryPush(this::scribble$pushEditCommand);
    }

    @Unique
    private RichEditBoxWidget scribble$getRichEditBoxWidget() {
        return (RichEditBoxWidget) this.page;
    }

    //region Formatting Buttons
    @Inject(method = "init", at = @At(value = "TAIL"))
    public void initFormattingButtons(CallbackInfo ci) {
        int x = this.width / 2 + 78;
        int y = Scribble.getBookScreenYOffset(height) + 12;

        // Modifier buttons
        scribble$boldButton = scribble$addModifierButton(
                ChatFormatting.BOLD, Component.translatable("text.scribble.modifier.bold"),
                x, y, 0, 0, 22, 19);
        scribble$italicButton = scribble$addModifierButton(
                ChatFormatting.ITALIC, Component.translatable("text.scribble.modifier.italic"),
                x, y + 19, 0, 19, 22, 17);
        scribble$underlineButton = scribble$addModifierButton(
                ChatFormatting.UNDERLINE, Component.translatable("text.scribble.modifier.underline"),
                x, y + 36, 0, 36, 22, 17);
        scribble$strikethroughButton = scribble$addModifierButton(
                ChatFormatting.STRIKETHROUGH, Component.translatable("text.scribble.modifier.strikethrough"),
                x, y + 53, 0, 53, 22, 17);
        scribble$obfuscatedButton = scribble$addModifierButton(
                ChatFormatting.OBFUSCATED, Component.translatable("text.scribble.modifier.obfuscated"),
                x, y + 70, 0, 70, 22, 18);

        // Color swatches
        RichEditBoxWidget editBox = scribble$getRichEditBoxWidget();
        scribble$colorSwatches = new ArrayList<>(scribble$COLORS.length);
        for (int i = 0; i < scribble$COLORS.length; i++) {
            ChatFormatting color = scribble$COLORS[i];

            int dx = (i % 2) * 8;
            int dy = (i / 2) * 8;

            ColorSwatchWidget swatch = new ColorSwatchWidget(
                    Component.translatable("text.scribble.color." + color.getName()), color,
                    () -> editBox.applyFormatting(color, true),
                    x + 3 + dx, y + 95 + dy, 8, 8,
                    editBox.color == color
            );

            ColorSwatchWidget widget = addRenderableWidget(swatch);
            scribble$colorSwatches.add(widget);
        }

        scribble$invalidateFormattingButtons();
    }

    @Unique
    private ModifierButtonWidget scribble$addModifierButton(ChatFormatting modifier, Component tooltip, int x, int y, int u, int v, int width, int height) {
        RichEditBoxWidget editBox = scribble$getRichEditBoxWidget();
        ModifierButtonWidget button = new ModifierButtonWidget(
                tooltip, (toggled) -> editBox.applyFormatting(modifier, toggled),
                x, y, u, v, width, height,
                editBox.modifiers.contains(modifier));
        return addRenderableWidget(button);
    }

    @Unique
    private void scribble$invalidateFormattingButtons() {
        RichEditBoxWidget editBox = scribble$getRichEditBoxWidget();

        // Sometimes, these buttons get invalidated on screen initialize, when the buttons don't exist yet.
        // We can just return in that case.
        if (scribble$boldButton == null) {
            return;
        }

        scribble$boldButton.toggled = editBox.modifiers.contains(ChatFormatting.BOLD);
        scribble$italicButton.toggled = editBox.modifiers.contains(ChatFormatting.ITALIC);
        scribble$underlineButton.toggled = editBox.modifiers.contains(ChatFormatting.UNDERLINE);
        scribble$strikethroughButton.toggled = editBox.modifiers.contains(ChatFormatting.STRIKETHROUGH);
        scribble$obfuscatedButton.toggled = editBox.modifiers.contains(ChatFormatting.OBFUSCATED);

        scribble$setSwatchColor(editBox.color);
    }

    @Unique
    private void scribble$setSwatchColor(ChatFormatting color) {
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

        scribble$deletePageButton = addRenderableWidget(new IconButtonWidget(
                Component.translatable("text.scribble.action.delete_page"),
                this::scribble$deletePage,
                x + 94, y + 148, 0, 90, 12, 12));
        scribble$insertPageButton = addRenderableWidget(new IconButtonWidget(
                Component.translatable("text.scribble.action.insert_new_page"),
                this::scribble$insertPage,
                x + 78, y + 148, 12, 90, 12, 12));
    }

    @Inject(method = "updateButtonVisibility", at = @At(value = "HEAD"))
    public void invalidatePageButtons(CallbackInfo ci) {
        scribble$deletePageButton.visible = this.pages.size() > 1;
        scribble$insertPageButton.visible = this.pages.size() < 100;
    }

    @Unique
    private void scribble$deletePage() {
        if (this.pages.size() > 1) {
            // See scribble$history$deletePage for implementation.
            PageDeleteCommand command = new PageDeleteCommand(this.currentPage,
                    this.scribble$getRichEditBoxWidget().getRichTextField().getRichText());
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

        scribble$undoButton = addRenderableWidget(new IconButtonWidget(
                Component.translatable("text.scribble.action.undo"),
                () -> {
                    scribble$commandManager.tryUndo();
                    this.scribble$invalidateActionButtons();
                },
                ax, ay, 24, 90, 12, 12));
        scribble$redoButton = addRenderableWidget(new IconButtonWidget(
                Component.translatable("text.scribble.action.redo"),
                () -> {
                    scribble$commandManager.tryRedo();
                    this.scribble$invalidateActionButtons();
                },
                ax, ay + 12, 36, 90, 12, 12));

        if (Scribble.CONFIG_MANAGER.getConfig().showActionButtons != Config.ShowActionButtons.NEVER) {
            addRenderableWidget(new IconButtonWidget(
                    Component.translatable("text.scribble.action.save_book_to_file"),
                    () -> FileChooser.chooseBook(true, this::scribble$saveTo),
                    ax, ay + 12 * 2 + 4, 48, 90, 12, 12));
            addRenderableWidget(new IconButtonWidget(
                    Component.translatable("text.scribble.action.load_book_from_file"),
                    () -> this.scribble$confirmIf(true, "overwrite_warning",
                            () -> FileChooser.chooseBook(false, this::scribble$loadFrom)),
                    ax, ay + 12 * 3 + 4, 60, 90, 12, 12));
        }

        scribble$invalidateActionButtons();
    }

    @Unique
    private void scribble$invalidateActionButtons() {
        boolean show = Scribble.CONFIG_MANAGER.getConfig().showActionButtons != Config.ShowActionButtons.NEVER;
        scribble$undoButton.visible = show;
        scribble$redoButton.visible = show;

        scribble$undoButton.active = scribble$commandManager.canUndo();
        scribble$redoButton.active = scribble$commandManager.canRedo();
    }

    @Inject(method = "keyPressed", at = @At(value = "HEAD"), cancellable = true)
    public void onActionKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (event.hasControlDown() && !event.hasAltDown()) {
            if ((KeyboardUtil.isKey(event.key(), "Z") && !event.hasShiftDown() && scribble$undoButton.active)) {
                scribble$undoButton.onPress(event);
                cir.setReturnValue(true);
            } else if (((KeyboardUtil.isKey(event.key(), "Z") && event.hasShiftDown()) || (KeyboardUtil.isKey(event.key(), "Y") && !event.hasShiftDown())) && scribble$redoButton.active) {
                scribble$redoButton.onPress(event);
                cir.setReturnValue(true);
            }
        }
    }
    //endregion

    //region Page Number Widget
    @Inject(method = "init", at = @At(value = "HEAD"))
    public void initPageNumberWidget(CallbackInfo ci) {
        int x = (this.width - 192) / 2;
        int y = Scribble.getBookScreenYOffset(height);

        this.scribble$pageNumberWidget = addRenderableWidget(
                new PageNumberWidget(
                        (page) -> {
                            this.currentPage = Math.clamp(page - 1, 0, this.pages.size() - 1);
                            this.updatePageContent();
                            this.updateButtonVisibility();
                            this.setFocused(this.page);
                        },
                        x + 192 - 44, y + 18, this.font));
    }

    @Inject(method = "updatePageContent", at = @At(value = "HEAD"))
    public void updatePageNumber(CallbackInfo ci) {
        this.scribble$pageNumberWidget.setPageNumber(this.currentPage + 1, this.pages.size());
    }

    @WrapWithCondition(method = "visitText", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/ActiveTextCollector;accept(Lnet/minecraft/client/gui/TextAlignment;IILnet/minecraft/network/chat/Component;)V"))
    public boolean drawIndicatorText(ActiveTextCollector instance, TextAlignment textAlignment, int i, int j, Component component) {
        // Do nothing: this is replaced by scribble$pageNumberWidget.
        return false;
    }
    //endregion

    //region History
    @Override
    public void scribble$history$switchPage(int page) {
        if (page < 0 || page >= this.pages.size())
            return;

        this.currentPage = page;
        this.updatePageContent();
        this.updateButtonVisibility();
    }

    @Override
    public void scribble$history$setFormat(@Nullable ChatFormatting color, Set<ChatFormatting> modifiers) {
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
        this.updatePageContent();
        this.updateButtonVisibility();
    }

    @Override
    public void scribble$history$deletePage(int page) {
        this.scribble$history$switchPage(page);

        this.pages.remove(page);
        if (this.currentPage == this.pages.size()) {
            this.currentPage -= 1;
        }

        this.updatePageContent();
        this.updateButtonVisibility();
    }

    @Override
    public RichMultiLineTextField scribble$history$getRichEditBox() {
        return this.scribble$getRichEditBoxWidget().getRichTextField();
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
    @Inject(method = "pageForward", at = @At(value = "HEAD"), cancellable = true)
    public void pageForward(CallbackInfo ci) {
        int lastPage = this.pages.size() - 1;
        if (this.currentPage < lastPage && KeyboardUtil.hasShiftDown()) {
            this.currentPage = lastPage;
            this.updatePageContent();
            this.updateButtonVisibility();
            ci.cancel();
        }
    }

    // When shift is held down, skip to the first page.
    @Inject(method = "pageBack", at = @At(value = "HEAD"), cancellable = true)
    public void pageBack(CallbackInfo ci) {
        if (KeyboardUtil.hasShiftDown()) {
            this.currentPage = 0;
            this.updatePageContent();
            this.updateButtonVisibility();
            ci.cancel();
        }
    }
    //endregion

    //region Saving and Loading
    @Unique
    private void scribble$saveTo(Path path) {
        try {
            BookFile bookFile = new BookFile(this.owner.getName().getString(), this.pages);
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

            this.updatePageContent();
            this.updateButtonVisibility();
            this.scribble$dirty = true;
        } catch (Exception e) {
            Scribble.LOGGER.error("could not load book from file", e);
        }
    }
    //endregion

    //region Overwrite / Close confirmations
    // We want to keep track of if the book has been edited (i.e. if it's "dirty").
    @ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/MultiLineEditBox;setValueListener(Ljava/util/function/Consumer;)V"), index = 0)
    public Consumer<String> modifyValueListener(Consumer<String> valueListener) {
        return (text) -> {
            if (!text.equals(this.pages.get(this.currentPage))) {
                this.scribble$dirty = true;
            }

            valueListener.accept(text);
        };
    }

    // Show a confirmation dialog if passed condition is true, otherwise run the runnable immediately.
    @Unique
    public void scribble$confirmIf(boolean condition, String name, Runnable runnable) {
        if (condition && this.minecraft != null) {
            this.minecraft.setScreen(new ConfirmScreen(
                    confirmed -> {
                        this.minecraft.setScreen(this);
                        if (confirmed) runnable.run();
                    },
                    Component.translatable("text.scribble." + name + ".title"),
                    Component.translatable("text.scribble." + name + ".description")
            ));
        } else {
            runnable.run();
        }
    }

    // Show a confirmation dialog when trying to exit the screen if the book has been edited
    @Override
    public void onClose() {
        this.scribble$confirmIf(this.scribble$dirty, "quit_without_saving", super::onClose);
    }
    //endregion

    //region GUI Centering
    // If we need to center the GUI, we shift the Y of the texture draw call down.
    @ModifyArg(method = "renderBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"), index = 3)
    public int shiftBackgroundY(int y) {
        return Scribble.getBookScreenYOffset(height) + y;
    }

    // If we need to center the GUI, we shift the Y of the buttons down.
    @WrapOperation(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/BookEditScreen;addRenderableWidget(Lnet/minecraft/client/gui/components/events/GuiEventListener;)Lnet/minecraft/client/gui/components/events/GuiEventListener;"))
    public <T extends GuiEventListener & Renderable & NarratableEntry> T shiftButtonY(BookEditScreen instance, T guiEventListener, Operation<T> original) {
        if (guiEventListener instanceof AbstractWidget widget) {
            widget.setY(widget.getY() + Scribble.getBookScreenYOffset(height));
        }

        return original.call(instance, guiEventListener);
    }
    //endregion

    //region Bug fixes
    // We cancel any drags outside the width of the book interface.
    @Override
    public boolean mouseDragged(MouseButtonEvent event, double offsetX, double offsetY) {
        if (event.x() < (this.width - 152) / 2.0 || event.x() > (this.width + 152) / 2.0) {
            return true;
        } else {
            return super.mouseDragged(event, offsetX, offsetY);
        }
    }
    //endregion
}
