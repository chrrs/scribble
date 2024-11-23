package me.chrr.scribble.config;

import com.google.gson.annotations.SerializedName;

public class Config {
    public static int VERSION = 2;

    @SerializedName("version")
    public int version = VERSION;

    @SerializedName("copy_formatting_codes")
    public boolean copyFormattingCodes = true;

    @SerializedName("center_book_gui")
    public boolean centerBookGui = true;

    @SerializedName("show_save_load_buttons")
    public boolean showSaveLoadButtons = true;

    public void upgrade() {
        this.version = VERSION;
    }
}
