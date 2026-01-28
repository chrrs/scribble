package me.chrr.scribble.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import me.chrr.scribble.Scribble;
import me.chrr.scribble.screen.ScribbleBookEditScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WritableBookContent;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@NullMarked
@Mixin(value = LocalPlayer.class, priority = 500)
public abstract class LocalPlayerMixin {
    @WrapOperation(method = "openItemGui", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V"))
    public void overrideBookViewScreen(Minecraft instance, Screen screen, Operation<Void> original, @Local(argsOnly = true) ItemStack itemStack, @Local(argsOnly = true) InteractionHand hand, @Local WritableBookContent book) {
        if (instance.hasShiftDown() && Scribble.config().openVanillaBookScreenOnShift.get()) {
            original.call(instance, screen);
        } else {
            // FIXME: ideally, I'd like to avoid even constructing the original BookEditScreen.
            original.call(instance, new ScribbleBookEditScreen((LocalPlayer) (Object) this, itemStack, hand, book));
        }
    }
}
