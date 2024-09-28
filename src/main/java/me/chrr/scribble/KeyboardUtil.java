package me.chrr.scribble;

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
}
