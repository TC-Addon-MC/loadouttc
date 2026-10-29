package com.toancao.loadouttc.hud;

import java.util.Locale;

/**
 * How the "ability in use" timer is drawn. This is not a vanilla concept: mods push the timer
 * through {@code LoadoutTcApi.showAbility(...)}.
 */
public enum AbilityStyle {
    OFF("off", "Off"),
    RING("ring", "Ring"),
    BAR("bar", "Bar"),
    TEXT("text", "Text"),
    FRAME("frame", "Frame");

    private final String id;
    private final String label;

    AbilityStyle(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public static AbilityStyle byId(String id) {
        if (id != null) {
            String key = id.trim().toLowerCase(Locale.ROOT);
            for (AbilityStyle style : values()) {
                if (style.id.equals(key) || style.name().equalsIgnoreCase(key)) {
                    return style;
                }
            }
        }
        return RING;
    }
}
