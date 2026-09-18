package me.chrr.scribble.util;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class KeyboardUtil {
    private KeyboardUtil() {
    }

    //? if >=26.3 {
    public static final int KEY_PAGE_UP = org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_PAGEUP;
    public static final int KEY_PAGE_DOWN = org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_PAGEDOWN;
    public static final int KEY_MINUS = org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_MINUS;
    public static final int KEY_ENTER = org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_RETURN;
    public static final int KEY_KP_ENTER = org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_KP_ENTER;
    public static final int KEY_BACKSPACE = org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_BACKSPACE;
    public static final int KEY_DELETE = org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_DELETE;
    public static final int KEY_ESCAPE = org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_ESCAPE;
    public static final int KEY_LEFT = org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_LEFT;
    public static final int KEY_RIGHT = org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_RIGHT;
    public static final int KEY_B = org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_B;
    public static final int KEY_I = org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_I;
    public static final int KEY_K = org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_K;
    public static final int KEY_U = org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_U;
    //? } else {
    /*public static final int KEY_PAGE_UP = org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_UP;
    public static final int KEY_PAGE_DOWN = org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_DOWN;
    public static final int KEY_MINUS = org.lwjgl.glfw.GLFW.GLFW_KEY_MINUS;
    public static final int KEY_ENTER = org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;
    public static final int KEY_KP_ENTER = org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER;
    public static final int KEY_BACKSPACE = org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE;
    public static final int KEY_DELETE = org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE;
    public static final int KEY_ESCAPE = org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
    public static final int KEY_LEFT = org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT;
    public static final int KEY_RIGHT = org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT;
    public static final int KEY_B = org.lwjgl.glfw.GLFW.GLFW_KEY_B;
    public static final int KEY_I = org.lwjgl.glfw.GLFW.GLFW_KEY_I;
    public static final int KEY_K = org.lwjgl.glfw.GLFW.GLFW_KEY_K;
    public static final int KEY_U = org.lwjgl.glfw.GLFW.GLFW_KEY_U;
    *///? }

    /**
     * Test if a key corresponds to the given key name, respecting keyboard layout.
     * See <a href="https://bugs.mojang.com/browse/MC-121278">MC-121278</a>.
     */
    public static boolean isKey(int keyCode, String keyName) {
        //? >=26.3 {
        if (keyCode == org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_UNKNOWN) {
            return false;
        } else {
            return keyName.equalsIgnoreCase(org.lwjgl.sdl.SDLKeyboard.SDL_GetScancodeName(keyCode));
        }
        //? } else {
        /*if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN) {
            return false;
        } else {
            return keyName.equalsIgnoreCase(org.lwjgl.glfw.GLFW.glfwGetKeyName(keyCode, 0));
        }
        *///? }
    }
}
