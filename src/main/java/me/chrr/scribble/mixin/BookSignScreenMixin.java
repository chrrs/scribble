package me.chrr.scribble.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.chrr.scribble.SetReturnScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookSignScreen;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@NullMarked
@Mixin(BookSignScreen.class)
public abstract class BookSignScreenMixin implements SetReturnScreen {
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
}
