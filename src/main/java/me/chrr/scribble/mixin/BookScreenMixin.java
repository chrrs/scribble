package me.chrr.scribble.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.chrr.scribble.Scribble;
import me.chrr.scribble.book.BookFile;
import me.chrr.scribble.book.FileChooser;
import me.chrr.scribble.book.RichText;
import me.chrr.scribble.config.Config;
import me.chrr.scribble.gui.button.IconButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(BookScreen.class)
public abstract class BookScreenMixin extends Screen {
    @Shadow
    private BookScreen.Contents contents;

    // Dummy constructor to match super class.
    private BookScreenMixin() {
        super(null);
    }

    // Add save book button.
    @Inject(method = "init", at = @At(value = "TAIL"))
    public void initButtons(CallbackInfo info) {
        if (Scribble.CONFIG_MANAGER.getConfig().showActionButtons == Config.ShowActionButtons.ALWAYS) {
            int x = this.width / 2 - 78 - 7 - 12;
            int y = Scribble.getBookScreenYOffset(height) + 12 + 4;

            Runnable saveBook = () -> FileChooser.chooseBook(true, (path) -> {
                try {
                    List<String> pages = this.contents.pages().stream()
                            .map(RichText::fromStringVisitableLossy)
                            .map(RichText::getAsFormattedString)
                            .toList();

                    BookFile bookFile = new BookFile("<written book>", pages);
                    bookFile.writeJson(path);
                } catch (Exception e) {
                    Scribble.LOGGER.error("could not save book to file", e);
                }
            });

            addDrawableChild(new IconButtonWidget(
                    Text.translatable("text.scribble.action.save_book_to_file"), saveBook,
                    x, y, 48, 90, 12, 12));
        }
    }

    // If we need to center the GUI, we shift the Y of the texture draw call down.
    @ModifyArg(method = "renderBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIFFIIII)V"), index = 3)
    public int shiftBackgroundY(int y) {
        return Scribble.getBookScreenYOffset(height) + y;
    }

    // If we need to center the GUI, we shift the Y of the close buttons down.
    @WrapOperation(method = "addCloseButton", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/BookScreen;addDrawableChild(Lnet/minecraft/client/gui/Element;)Lnet/minecraft/client/gui/Element;"))
    public <T extends Element & Drawable & Selectable> T shiftCloseButtonY(BookScreen instance, Element element, Operation<T> original) {
        if (element instanceof Widget widget) {
            widget.setY(widget.getY() + Scribble.getBookScreenYOffset(height));
        }

        return original.call(instance, element);
    }

    // If we need to center the GUI, we shift the Y of the page buttons down.
    @WrapOperation(method = "addPageButtons", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/BookScreen;addDrawableChild(Lnet/minecraft/client/gui/Element;)Lnet/minecraft/client/gui/Element;"))
    public <T extends Element & Drawable & Selectable> T shiftPageButtonY(BookScreen instance, Element element, Operation<T> original) {
        if (element instanceof Widget widget) {
            widget.setY(widget.getY() + Scribble.getBookScreenYOffset(height));
        }

        return original.call(instance, element);
    }

    // If we need to center the GUI, modify the coordinates to check.
    @ModifyVariable(method = "getTextStyleAt", at = @At(value = "HEAD"), ordinal = 1, argsOnly = true)
    public double shiftTextStyleY(double y) {
        return y - Scribble.getBookScreenYOffset(height);
    }

    // When rendering, we translate the matrices of the draw context to draw the text further down if needed.
    // Note that this happens after the parent screen render, so only the text in the book is shifted.
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;render(Lnet/minecraft/client/gui/DrawContext;IIF)V", shift = At.Shift.AFTER))
    public void translateRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(0f, Scribble.getBookScreenYOffset(height));
    }

    // At the end of rendering, we need to pop those matrices we pushed.
    @Inject(method = "render", at = @At(value = "RETURN"))
    public void popRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        context.getMatrices().popMatrix();
    }
}
