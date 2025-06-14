package me.chrr.scribble.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.chrr.scribble.Scribble;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.BookSigningScreen;
import net.minecraft.client.gui.widget.Widget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BookSigningScreen.class)
public abstract class BookSigningScreenMixin extends Screen {
    // Dummy constructor to match super class.
    private BookSigningScreenMixin() {
        super(null);
    }

    // If we need to center the GUI, we shift the Y of the texture draw call down.
    @ModifyArg(method = "renderBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIFFIIII)V"), index = 3)
    public int shiftBackgroundY(int y) {
        return Scribble.getBookScreenYOffset(height) + y;
    }

    // If we need to center the GUI, we shift the Y of the close buttons down.
    @WrapOperation(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/BookSigningScreen;addDrawableChild(Lnet/minecraft/client/gui/Element;)Lnet/minecraft/client/gui/Element;"))
    public <T extends Element & Drawable & Selectable> T shiftButtonY(BookSigningScreen instance, Element element, Operation<T> original) {
        if (element instanceof Widget widget) {
            widget.setY(widget.getY() + Scribble.getBookScreenYOffset(height));
        }

        return original.call(instance, element);
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
