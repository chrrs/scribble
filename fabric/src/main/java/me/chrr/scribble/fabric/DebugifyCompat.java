package me.chrr.scribble.fabric;

import dev.isxander.debugify.api.DebugifyApi;

public class DebugifyCompat implements DebugifyApi {
    @Override
    public String[] getDisabledFixes() {
        return new String[]{"MC-61489"};
    }
}
