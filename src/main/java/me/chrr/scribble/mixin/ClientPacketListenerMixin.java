package me.chrr.scribble.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import me.chrr.scribble.screen.ScribbleBookViewScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@NullMarked
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @WrapOperation(method = "handleOpenBook", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V"))
    public void overrideBookViewScreen(Minecraft instance, Screen screen, Operation<Void> original, @Local BookViewScreen.BookAccess book) {
        if (instance.hasShiftDown()) {
            // FIXME: this is temporary, maybe a config option?
            original.call(instance, screen);
        } else {
            // FIXME: ideally, I'd like to avoid even constructing the original BookViewScreen.
            original.call(instance, new ScribbleBookViewScreen(book));
        }
    }
}
