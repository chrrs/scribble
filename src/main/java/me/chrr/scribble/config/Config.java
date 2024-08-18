package me.chrr.scribble.config;

import com.google.gson.annotations.SerializedName;

public class Config {
    public static int VERSION = 1;

    @SerializedName("version")
    public int version = VERSION;

    public void upgrade() {
        this.version = VERSION;
    }
}
