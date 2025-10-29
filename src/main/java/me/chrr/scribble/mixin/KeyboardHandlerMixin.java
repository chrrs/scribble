package me.chrr.scribble.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    // Disable the narrator hotkey when editing a book, and while not holding SHIFT.
    @WrapOperation(method = "keyPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/GameNarrator;isActive()Z"))
    public boolean isNarratorActive(GameNarrator instance, Operation<Boolean> original, @Local(argsOnly = true) KeyEvent event) {
        if (minecraft.screen instanceof BookEditScreen && !event.hasShiftDown()) {
            return false;
        } else {
            return original.call(instance);
        }
    }
}
