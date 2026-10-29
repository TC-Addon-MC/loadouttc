package com.toancao.loadouttc.hud;

import java.util.Locale;

/** How an active item cooldown is shown on a HUD slot. */
public enum CooldownStyle {
    ARC("arc", "Arc"),
    TEXT("text", "Txt"),
    BAR("bar", "Bar"),
    /** Progress traced along the slot's own frame; follows hex / rounded / square shapes. */
    FRAME("frame", "Frame"),
    OFF("off", "Off");

    private final String id;
    private final String shortLabel;

    CooldownStyle(String id, String shortLabel) {
        this.id = id;
        this.shortLabel = shortLabel;
    }

    public String id() {
        return id;
    }

    public String shortLabel() {
        return shortLabel;
    }

    public CooldownStyle next() {
        CooldownStyle[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static CooldownStyle byId(String id) {
        if (id != null) {
            String key = id.trim().toLowerCase(Locale.ROOT);
            for (CooldownStyle style : values()) {
                if (style.id.equals(key) || style.name().equalsIgnoreCase(key)) {
                    return style;
                }
            }
        }
        return ARC;
    }
}
