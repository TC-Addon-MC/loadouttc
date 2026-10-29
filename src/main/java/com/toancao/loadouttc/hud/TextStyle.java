package com.toancao.loadouttc.hud;

import java.util.Locale;

/** Label treatment. Independent from the panel material and the slot shape. */
public enum TextStyle {
    AUTO("auto", "Auto"),
    NORMAL("normal", "Normal"),
    UPPER("upper", "Upper"),
    BRIGHT("bright", "Bright"),
    GOLD("gold", "Gold"),
    /** White type with a 1px dark outline: readable on any background. */
    OUTLINE("outline", "Outline");

    private final String id;
    private final String label;

    TextStyle(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public static TextStyle byId(String id) {
        if (id != null) {
            String key = id.trim().toLowerCase(Locale.ROOT);
            for (TextStyle style : values()) {
                if (style.id.equals(key) || style.name().equalsIgnoreCase(key)) {
                    return style;
                }
            }
        }
        return AUTO;
    }

    public static TextStyle resolve(String id, HudStyle preset) {
        TextStyle style = byId(id);
        return style == AUTO ? fromPreset(preset) : style;
    }

    private static TextStyle fromPreset(HudStyle preset) {
        return switch (preset) {
            case FANTASY_RPG, ARCANE -> GOLD;
            case OUTLINE -> UPPER;
            case BARE, MINIMAL -> BRIGHT;
            case TERMINAL, GROUPED, SCIFI_TACTICAL, BRUTALIST, ARCADE -> UPPER;
            default -> NORMAL;
        };
    }
}
