package me.chrr.scribble.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.chrr.scribble.Scribble;
import me.chrr.scribble.screen.ScribbleLecternScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.LecternMenu;
import net.minecraft.world.inventory.MenuType;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Mixin;

@NullMarked
@Mixin(MenuScreens.class)
public abstract class MenuScreensMixin {
    @WrapMethod(method = "register")
    private static <S extends Screen & MenuAccess<LecternMenu>> void overrideLecternScreen(MenuType<LecternMenu> type, MenuScreens.ScreenConstructor<LecternMenu, S> factory, Operation<Void> original) {
        if (type == MenuType.LECTERN) {
            original.call(type, (MenuScreens.ScreenConstructor<LecternMenu, S>) (menu, inventory, title) -> {
                Minecraft minecraft = Minecraft.getInstance();
                if (!minecraft.hasShiftDown() || !Scribble.config().openVanillaBookScreenOnShift) {
                    //noinspection unchecked: S is technically not the same here, but it doesn't matter.
                    return (S) new ScribbleLecternScreen(menu);
                } else {
                    return factory.create(menu, inventory, title);
                }
            });
        } else {
            original.call(type, factory);
        }
    }
}
