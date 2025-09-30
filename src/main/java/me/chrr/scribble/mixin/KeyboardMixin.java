package me.chrr.scribble.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.NarratorManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Keyboard.class)
public class KeyboardMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    // Disable the narrator hotkey when editing a book, and while not holding SHIFT.
    @WrapOperation(method = "onKey", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/NarratorManager;isActive()Z"))
    public boolean isNarratorActive(NarratorManager instance, Operation<Boolean> original, @Local(argsOnly = true) KeyInput input) {
        if (client.currentScreen instanceof BookEditScreen && !input.hasShift()) {
            return false;
        } else {
            return original.call(instance);
        }
    }
}
