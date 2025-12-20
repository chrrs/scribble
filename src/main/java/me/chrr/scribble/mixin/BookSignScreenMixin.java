package me.chrr.scribble.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.chrr.scribble.ScribbleConfig;
import me.chrr.scribble.SetReturnScreen;
import me.chrr.scribble.screen.ScribbleBookScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookSignScreen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@NullMarked
@Mixin(BookSignScreen.class)
public abstract class BookSignScreenMixin extends Screen implements SetReturnScreen {
    private BookSignScreenMixin(Component title) {
        super(title);
    }

    //region Return Screen
    @Unique
    public @Nullable Screen scribble$returnScreen = null;

    @Override
    public void scribble$setReturnScreen(Screen screen) {
        this.scribble$returnScreen = screen;
    }

    @WrapOperation(method = "method_71541", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V"))
    public void redirectReturnScreen(Minecraft instance, Screen screen, Operation<Void> original) {
        original.call(instance, this.scribble$returnScreen != null ? this.scribble$returnScreen : screen);
    }
    //endregion

    //region Centering
    // If we need to center the GUI, we shift the Y of the texture draw call down.
    @ModifyArg(method = "renderBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"), index = 3)
    public int shiftBackgroundY(int y) {
        return scribble$getYOffset() + y;
    }

    // If we need to center the GUI, we shift the Y of the close buttons down.
    @WrapOperation(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/BookSignScreen;addRenderableWidget(Lnet/minecraft/client/gui/components/events/GuiEventListener;)Lnet/minecraft/client/gui/components/events/GuiEventListener;"))
    public <T extends GuiEventListener & Renderable & NarratableEntry> T shiftButtonY(BookSignScreen instance, T guiEventListener, Operation<T> original) {
        if (guiEventListener instanceof AbstractWidget widget) {
            widget.setY(widget.getY() + scribble$getYOffset());
        }

        return original.call(instance, guiEventListener);
    }

    // When rendering, we translate the matrices of the draw context to draw the text further down if needed.
    // Note that this happens after the parent screen render, so only the text in the book is shifted.
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", shift = At.Shift.AFTER))
    public void translateRender(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(0f, scribble$getYOffset());
    }

    // At the end of rendering, we need to pop those matrices we pushed.
    @Inject(method = "render", at = @At(value = "RETURN"))
    public void popRender(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        graphics.pose().popMatrix();
    }

    @Unique
    private int scribble$getYOffset() {
        if (ScribbleConfig.INSTANCE.centerBookGui) {
            // See ScribbleBookScreen#getBackgroundY().
            return this.height / 3 - ScribbleBookScreen.getMenuHeight() / 3;
        } else {
            return 0;
        }
    }
    //endregion
}
