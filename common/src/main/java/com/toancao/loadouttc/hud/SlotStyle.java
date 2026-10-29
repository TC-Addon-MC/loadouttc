package com.toancao.loadouttc.hud;

import java.util.Locale;

/** Slot shape. Independent from the panel material and the label treatment. */
public enum SlotStyle {
    AUTO("auto", "Auto"),
    NONE("none", "None"),
    VANILLA("vanilla", "Vanilla"),
    STEEL("steel", "Steel"),
    HEX("hex", "Hex"),
    CUT("cut", "Cut"),
    INSET("inset", "Inset"),
    ROUNDED("rounded", "Rounded"),
    CIRCLE("circle", "Circle"),
    TRIANGLE("triangle", "Triangle"),
    ELLIPSE("ellipse", "Ellipse");

    private final String id;
    private final String label;

    SlotStyle(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public static SlotStyle byId(String id) {
        if (id != null) {
            String key = id.trim().toLowerCase(Locale.ROOT);
            if ("carved".equals(key)) {
                return INSET;
            }
            for (SlotStyle style : values()) {
                if (style.id.equals(key) || style.name().equalsIgnoreCase(key)) {
                    return style;
                }
            }
        }
        return AUTO;
    }

    public static SlotStyle resolve(String id, HudStyle preset) {
        SlotStyle style = byId(id);
        return style == AUTO ? fromPreset(preset) : style;
    }

    private static SlotStyle fromPreset(HudStyle preset) {
        return switch (preset) {
            case VANILLA, PIXEL_VANILLA -> VANILLA;
            case STEEL -> STEEL;
            case HEX -> HEX;
            case FANTASY_RPG -> INSET;
            case PARCHMENT -> INSET;
            case ARCANE -> HEX;
            case CARBON, OUTLINE -> ROUNDED;
            case BARE, MINIMAL -> NONE;
            default -> ROUNDED;
        };
    }
}
