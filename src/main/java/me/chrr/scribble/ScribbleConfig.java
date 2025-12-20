package me.chrr.scribble;

import com.google.gson.JsonObject;
import me.chrr.tapestry.config.Binding;
import me.chrr.tapestry.config.reflect.NamingStrategy;
import me.chrr.tapestry.config.reflect.ReflectedConfig;
import me.chrr.tapestry.config.reflect.annotation.*;
import org.jspecify.annotations.NullMarked;

import java.util.List;

@NullMarked
@TranslateDisplayNames(prefix = "config.scribble")
@SerializeName.Strategy(NamingStrategy.SNAKE_CASE)
public class ScribbleConfig extends ReflectedConfig {
    public static final ScribbleConfig INSTANCE = load(() -> Scribble.platform().CONFIG_DIR,
            ScribbleConfig.class, "scribble.client.json", List.of("scribble.json"));


    @Header("appearance")
    @Rebind.Display("doublePageViewing")
    @DisplayName("double_page_viewing")
    public int pagesToShow = 1;
    public boolean centerBookGui = true;
    public boolean showFormattingButtons = true;
    public ShowActionButtons showActionButtons = ShowActionButtons.WHEN_EDITING;

    @Header("behaviour")
    public boolean copyFormattingCodes = true;
    @Slider.Int(min = 8, max = 128)
    public int editHistorySize = 32;

    @Header("miscellaneous")
    public boolean openVanillaBookScreenOnShift = false;


    @SuppressWarnings("unused")
    private final transient Binding<Boolean> doublePageViewing = Binding.of(Boolean.class,
            () -> this.pagesToShow > 1, (value) -> this.pagesToShow = value ? 2 : 1);

    @UpgradeRewriter(currentVersion = 3)
    public static void upgrade(int fromVersion, JsonObject config) {
        if (fromVersion < 3) {
            // 'show_save_load_buttons' was removed.
            config.addProperty("show_action_buttons",
                    config.get("show_save_load_buttons").getAsBoolean() ? "WHEN_EDITING" : "NEVER");
        }
    }


    public enum ShowActionButtons {
        ALWAYS,
        WHEN_EDITING,
        NEVER,
    }
}
