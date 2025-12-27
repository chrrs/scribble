package me.chrr.scribble.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import me.chrr.scribble.Scribble;
import me.chrr.scribble.screen.ScribbleLecternScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.LecternMenu;
import net.minecraft.world.inventory.MenuType;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MenuScreens.class)
public abstract class MenuScreensMixin {
    @WrapOperation(method = "create", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/MenuScreens;getConstructor(Lnet/minecraft/world/inventory/MenuType;)Lnet/minecraft/client/gui/screens/MenuScreens$ScreenConstructor;"))
    private static MenuScreens.@Nullable ScreenConstructor<?, ?> overrideLecternScreen(MenuType<LecternMenu> menuType, Operation<MenuScreens.ScreenConstructor<?, ?>> original, @Local(argsOnly = true) Minecraft minecraft) {
        if (menuType == MenuType.LECTERN) {
            if (!minecraft.hasShiftDown() || !Scribble.config().openVanillaBookScreenOnShift) {
                return (MenuScreens.ScreenConstructor<LecternMenu, ScribbleLecternScreen>)
                        (menu, inventory, title) -> new ScribbleLecternScreen(menu);
            }
        }

        return original.call(menuType);
    }
}
