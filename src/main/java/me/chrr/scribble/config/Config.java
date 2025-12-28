package me.chrr.scribble.config;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class Config {
    public static final Config DEFAULT = new Config();

    public int version = 3;
    public boolean copyFormattingCodes = true;
    public boolean centerBookGui = true;
    public boolean showFormattingButtons = true;
    public ShowActionButtons showActionButtons = ShowActionButtons.WHEN_EDITING;
    public int editHistorySize = 32;
    public int pagesToShow = 1;
    public boolean openVanillaBookScreenOnShift = false;

    @DeprecatedConfigOption
    private boolean showSaveLoadButtons = true;

    public void upgrade() {
        if (this.version < 3) {
            // `show_save_load_buttons` was removed.
            this.showActionButtons = this.showSaveLoadButtons
                    ? ShowActionButtons.WHEN_EDITING
                    : ShowActionButtons.NEVER;
        }

        this.version = DEFAULT.version;
    }

    public enum ShowActionButtons {
        ALWAYS,
        WHEN_EDITING,
        NEVER,
    }
}
