package me.chrr.scribble;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class KeyboardUtil {
    private KeyboardUtil() {
    }

    /**
     * Test if a key corresponds to the given key name, respecting keyboard layout.
     * See <a href="https://bugs.mojang.com/browse/MC-121278">MC-121278</a>.
     */
    public static boolean isKey(int keyCode, String keyName) {
        if (keyCode == GLFW.GLFW_KEY_UNKNOWN) {
            return false;
        } else {
            return keyName.equalsIgnoreCase(GLFW.glfwGetKeyName(keyCode, 0));
        }
    }

    public static boolean hasShiftDown() {
        Window window = Minecraft.getInstance().getWindow();
        return InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }
}
