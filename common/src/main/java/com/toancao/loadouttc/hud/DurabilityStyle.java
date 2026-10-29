package com.toancao.loadouttc.hud;

import java.util.Locale;

/** How the remaining durability of a slot is drawn. */
public enum DurabilityStyle {
    /** Thin continuous line under the icon. */
    BAR("bar", "Bar"),
    /** Five discrete cells. */
    SEGMENTS("segments", "Seg"),
    /** Six dots. */
    PIPS("pips", "Pip"),
    /** Coloured percentage, needs a full text line. */
    TEXT("text", "Txt"),
    /** Current / max readout, the compact "76/77" look. */
    VALUE("value", "Val"),
    /** Arc around the slot; lives in the plate padding instead of a row below it. */
    RING("ring", "Ring"),
    /** Small battery outline. */
    BATTERY("battery", "Bat"),
    OFF("off", "Off");

    private final String id;
    private final String shortLabel;

    DurabilityStyle(String id, String shortLabel) {
        this.id = id;
        this.shortLabel = shortLabel;
    }

    public String id() {
        return id;
    }

    public String shortLabel() {
        return shortLabel;
    }

    public DurabilityStyle next() {
        DurabilityStyle[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static DurabilityStyle byId(String id) {
        if (id != null) {
            String key = id.trim().toLowerCase(Locale.ROOT);
            for (DurabilityStyle style : values()) {
                if (style.id.equals(key) || style.name().equalsIgnoreCase(key)) {
                    return style;
                }
            }
        }
        return BAR;
    }
}
