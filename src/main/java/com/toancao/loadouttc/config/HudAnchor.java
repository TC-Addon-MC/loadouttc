package com.toancao.loadouttc.config;

import java.util.Locale;

/** Screen anchors understood by the HUD layout. */
public enum HudAnchor {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    ABOVE_HOTBAR,
    CENTER,
    CUSTOM;

    public static HudAnchor byName(String name) {
        if (name == null || name.isBlank()) {
            return TOP_RIGHT;
        }
        try {
            return valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return TOP_RIGHT;
        }
    }

    public HudAnchor next() {
        HudAnchor[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public String translationKey() {
        return "loadouttc.anchor." + name().toLowerCase(Locale.ROOT);
    }
}
