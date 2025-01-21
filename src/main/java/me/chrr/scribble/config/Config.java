package me.chrr.scribble.config;

public class Config {
    public static Config DEFAULT = new Config();

    public int version = 3;
    public boolean copyFormattingCodes = true;
    public boolean centerBookGui = true;
    public ShowActionButtons showActionButtons = ShowActionButtons.WHEN_EDITING;
    public int editHistorySize = 32;

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
