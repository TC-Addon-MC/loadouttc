package com.toancao.loadouttc.hud;

import java.util.Locale;

/** Background material. Independent from the slot shape and the label treatment. */
public enum PanelStyle {
    AUTO("auto", "Auto"),
    NONE("none", "None"),
    VANILLA("vanilla", "Vanilla"),
    DARK("dark", "Dark"),
    GLASS("glass", "Glass"),
    WOOD("wood", "Wood"),
    CARBON("carbon", "Carbon"),
    PARCHMENT("parchment", "Parchment"),
    ARCANE("arcane", "Arcane");

    private final String id;
    private final String label;

    PanelStyle(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public PanelStyle next() {
        PanelStyle[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static PanelStyle byId(String id) {
        if (id != null) {
            String key = id.trim().toLowerCase(Locale.ROOT);
            for (PanelStyle style : values()) {
                if (style.id.equals(key) || style.name().equalsIgnoreCase(key)) {
                    return style;
                }
            }
        }
        return AUTO;
    }

    public static PanelStyle resolve(String id, HudStyle preset) {
        PanelStyle style = byId(id);
        return style == AUTO ? fromPreset(preset) : style;
    }

    private static PanelStyle fromPreset(HudStyle preset) {
        return switch (preset) {
            case VANILLA -> VANILLA;
            case FANTASY_RPG -> WOOD;
            case BARE, MINIMAL -> NONE;
            case LIQUID_GLASS, GLASSMORPHISM, FROST, NEUMORPHISM -> GLASS;
            case CARBON -> CARBON;
            case PARCHMENT -> PARCHMENT;
            case ARCANE -> ARCANE;
            case OUTLINE -> NONE;
            default -> DARK;
        };
    }
}
