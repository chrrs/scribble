package me.chrr.scribble.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.chrr.scribble.Scribble;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.LecternScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LecternScreen.class)
public abstract class LecternScreenMixin extends Screen {
    // Dummy constructor to match super class.
    private LecternScreenMixin() {
        super(null);
    }

    // If we need to center the GUI, we shift the Y of the close buttons down.
    @WrapOperation(method = "createMenuControls", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/LecternScreen;addRenderableWidget(Lnet/minecraft/client/gui/components/events/GuiEventListener;)Lnet/minecraft/client/gui/components/events/GuiEventListener;"))
    public <T extends GuiEventListener & Renderable & NarratableEntry> T shiftCloseButtonY(LecternScreen instance, T guiEventListener, Operation<T> original) {
        if (guiEventListener instanceof AbstractWidget widget) {
            widget.setY(widget.getY() + Scribble.getBookScreenYOffset(height));
        }

        return original.call(instance, guiEventListener);
    }
}
