package me.chrr.scribble.mixin;

import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Keyboard.class)
public class KeyboardMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    // Disable the narrator hotkey when editing a book.
    // We mix into the constant, because there's not really many other good places
    // that we can mixin into to change the condition.
    @ModifyConstant(method = "onKey", constant = @Constant(intValue = GLFW.GLFW_KEY_B))
    public int getNarratorKey(int constant) {
        // We'll change the key needed to activate the narrator to
        // a known non-existent key if we're currently editing a book.
        return client.currentScreen instanceof BookEditScreen
                ? GLFW.GLFW_KEY_LAST + 1 : GLFW.GLFW_KEY_B;
    }
}
