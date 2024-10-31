package me.chrr.scribble.mixin;

import me.chrr.scribble.Scribble;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.LecternScreen;
import net.minecraft.client.gui.widget.Widget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LecternScreen.class)
public abstract class LecternScreenMixin extends Screen {
    // Dummy constructor to match super class.
    private LecternScreenMixin() {
        super(null);
    }

    // If we need to center the GUI, we shift the Y of the close buttons down.
    @Redirect(method = "addCloseButton", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/LecternScreen;addDrawableChild(Lnet/minecraft/client/gui/Element;)Lnet/minecraft/client/gui/Element;"))
    public <T extends Element & Drawable & Selectable> T shiftCloseButtonY(LecternScreen instance, T element) {
        if (element instanceof Widget widget) {
            widget.setY(widget.getY() + Scribble.getBookScreenYOffset(height));
        }

        return addDrawableChild(element);
    }
}
