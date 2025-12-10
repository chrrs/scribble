package me.chrr.scribble.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.chrr.scribble.Scribble;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookSignScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BookSignScreen.class)
public abstract class BookSignScreenMixin extends Screen {
    // Dummy constructor to match super class.
    private BookSignScreenMixin() {
        super(null);
    }

    // If we need to center the GUI, we shift the Y of the texture draw call down.
    @ModifyArg(method = "renderBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"), index = 3)
    public int shiftBackgroundY(int y) {
        return Scribble.getBookScreenYOffset(height) + y;
    }

    // If we need to center the GUI, we shift the Y of the close buttons down.
    @WrapOperation(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/BookSignScreen;addRenderableWidget(Lnet/minecraft/client/gui/components/events/GuiEventListener;)Lnet/minecraft/client/gui/components/events/GuiEventListener;"))
    public <T extends GuiEventListener & Renderable & NarratableEntry> T shiftButtonY(BookSignScreen instance, T guiEventListener, Operation<T> original) {
        if (guiEventListener instanceof AbstractWidget widget) {
            widget.setY(widget.getY() + Scribble.getBookScreenYOffset(height));
        }

        return original.call(instance, guiEventListener);
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
}
