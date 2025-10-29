package me.chrr.scribble.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.chrr.scribble.Scribble;
import me.chrr.scribble.book.BookFile;
import me.chrr.scribble.book.FileChooser;
import me.chrr.scribble.book.RichText;
import me.chrr.scribble.config.Config;
import me.chrr.scribble.gui.PageNumberWidget;
import me.chrr.scribble.gui.button.IconButtonWidget;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(BookViewScreen.class)
public abstract class BookViewScreenMixin extends Screen {
    //region @Shadow declarations
    @Shadow
    private BookViewScreen.BookAccess bookAccess;

    @Shadow
    private int currentPage;

    @Shadow
    public abstract boolean setPage(int index);
    //endregion

    @Unique
    private PageNumberWidget scribble$pageNumberWidget;

    // Dummy constructor to match super class.
    private BookViewScreenMixin() {
        super(null);
    }

    //region Action Buttons
    @Inject(method = "init", at = @At(value = "TAIL"))
    public void initButtons(CallbackInfo info) {
        if (Scribble.CONFIG_MANAGER.getConfig().showActionButtons == Config.ShowActionButtons.ALWAYS) {
            int x = this.width / 2 - 78 - 7 - 12;
            int y = Scribble.getBookScreenYOffset(height) + 12 + 4;

            Runnable saveBook = () -> FileChooser.chooseBook(true, (path) -> {
                try {
                    List<String> pages = this.bookAccess.pages().stream()
                            .map(RichText::fromFormattedTextLossy)
                            .map(RichText::getAsFormattedString)
                            .toList();

                    BookFile bookFile = new BookFile("<written book>", pages);
                    bookFile.writeJson(path);
                } catch (Exception e) {
                    Scribble.LOGGER.error("could not save book to file", e);
                }
            });

            addRenderableWidget(new IconButtonWidget(
                    Component.translatable("text.scribble.action.save_book_to_file"), saveBook,
                    x, y, 48, 90, 12, 12));
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
                        (page) -> setPage(page - 1),
                        x + 192 - 44, y + 18, this.font));
    }

    @Inject(method = "updateButtonVisibility", at = @At(value = "HEAD"))
    public void updatePageNumber(CallbackInfo ci) {
        this.scribble$pageNumberWidget.setPageNumber(this.currentPage + 1, this.bookAccess.getPageCount());
    }

    @WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"))
    public boolean drawIndicatorText(GuiGraphics instance, Font font, Component component, int i, int j, int k, boolean bl) {
        // Do nothing: this is replaced by scribble$pageNumberWidget.
        return false;
    }
    //endregion

    //region GUI Centering
    // If we need to center the GUI, we shift the Y of the texture draw call down.
    @ModifyArg(method = "renderBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/ResourceLocation;IIFFIIII)V"), index = 3)
    public int shiftBackgroundY(int y) {
        return Scribble.getBookScreenYOffset(height) + y;
    }

    // If we need to center the GUI, we shift the Y of the close buttons down.
    @WrapOperation(method = "createMenuControls", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/BookViewScreen;addRenderableWidget(Lnet/minecraft/client/gui/components/events/GuiEventListener;)Lnet/minecraft/client/gui/components/events/GuiEventListener;"))
    public <T extends GuiEventListener & Renderable & NarratableEntry> T shiftCloseButtonY(BookViewScreen instance, T guiEventListener, Operation<T> original) {
        if (guiEventListener instanceof AbstractWidget widget) {
            widget.setY(widget.getY() + Scribble.getBookScreenYOffset(height));
        }

        return original.call(instance, guiEventListener);
    }

    // If we need to center the GUI, we shift the Y of the page buttons down.
    @WrapOperation(method = "createPageControlButtons", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/BookViewScreen;addRenderableWidget(Lnet/minecraft/client/gui/components/events/GuiEventListener;)Lnet/minecraft/client/gui/components/events/GuiEventListener;"))
    public <T extends GuiEventListener & Renderable & NarratableEntry> T shiftPageButtonY(BookViewScreen instance, T guiEventListener, Operation<T> original) {
        if (guiEventListener instanceof AbstractWidget widget) {
            widget.setY(widget.getY() + Scribble.getBookScreenYOffset(height));
        }

        return original.call(instance, guiEventListener);
    }

    // If we need to center the GUI, modify the coordinates to check.
    @ModifyVariable(method = "getClickedComponentStyleAt", at = @At(value = "HEAD"), ordinal = 1, argsOnly = true)
    public double shiftTextStyleY(double y) {
        return y - Scribble.getBookScreenYOffset(height);
    }

    // When rendering, we translate the matrices of the draw context to draw the text further down if needed.
    // Note that this happens after the parent screen render, so only the text in the book is shifted.
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", shift = At.Shift.AFTER))
    public void translateRender(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(0f, Scribble.getBookScreenYOffset(height));
    }

    // At the end of rendering, we need to pop those matrices we pushed.
    @Inject(method = "render", at = @At(value = "RETURN"))
    public void popRender(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        graphics.pose().popMatrix();
    }
    //endregion
}
