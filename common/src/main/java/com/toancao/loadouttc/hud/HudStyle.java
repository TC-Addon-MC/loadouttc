package com.toancao.loadouttc.hud;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The visual languages the HUD can wear. Each one is a self-contained look (panel, slots,
 * typography) rather than just a colour swap; all of them share the layout rules described in
 * {@link HudTheme}.
 *
 * <p>Sources of inspiration are listed in the README; they include Notion's card lists,
 * Apple's Liquid Glass, glassmorphism 2.0, neumorphism, bento grids, the macOS dock,
 * Android's Dynamic Island, Material 3 Expressive, terminal UIs and vanilla Minecraft.
 */
public enum HudStyle {
    // ---- Game-native family (visible) ----
    VANILLA("vanilla", "Vanilla",
            "The real Minecraft inventory bevel, nothing else.", Kind.LIST, true),
    STEEL("steel", "Steel Slots",
            "Blue-steel frames, white focus ring, dark glass.", Kind.LIST, false),
    HEX("hex", "Shield Slots",
            "Hexagonal wells with the gauge inside the plate.", Kind.LIST, false),
    GROUPED("grouped", "Grouped",
            "Colored group headers, boxed slots, always labelled.", Kind.LIST, false),
    COMPACT("compact", "Compact",
            "Icon plus a right-aligned durability readout.", Kind.LIST, false),

    // ---- Styles kept out of the gallery ----
    NOTION("notion", "Notion Card",
            "Warm paper, hairline border, quiet labels.", Kind.LIST, true),
    LIQUID_GLASS("liquid_glass", "Liquid Glass",
            "Layered translucency, light border, soft glow.", Kind.LIST, false),
    GLASSMORPHISM("glassmorphism", "Glassmorphism",
            "Frosted navy glass with a cold cyan rim.", Kind.LIST, false),
    NEUMORPHISM("neumorphism", "Neumorphism",
            "Soft raised clay, dual shadows, inset slots.", Kind.LIST, true),
    BENTO("bento", "Bento Grid",
            "Apple-style cards in a tidy two-column grid.", Kind.GRID, false),
    DYNAMIC_ISLAND("dynamic_island", "Dynamic Island",
            "Black pill, round slots, tiny status dot.", Kind.LIST, false),
    DOCK("dock", "macOS Dock",
            "A floating frosted strip that magnifies on hover.", Kind.DOCK, false),
    MATERIAL_EXPRESSIVE("material_expressive", "Material Expressive",
            "Bold tinted pills and springy shapes.", Kind.LIST, false),
    TERMINAL("terminal", "Terminal",
            "Phosphor green, scanlines, bracketed slots.", Kind.LIST, false),
    PIXEL_VANILLA("pixel_vanilla", "Pixel Vanilla",
            "Minecraft-native bevels, square corners.", Kind.LIST, true),

    // ---- Transparent / no-background family ----
    BARE("bare", "Bare Icons",
            "No panel, no slots: only the item icons.", Kind.LIST, false),
    MINIMAL("minimal", "Minimal Line",
            "No panel; a single hairline under each icon.", Kind.LIST, false),
    CHIPS("chips", "Floating Chips",
            "No panel; one small translucent chip per item.", Kind.LIST, false),
    STICKER("sticker", "Sticker Pop",
            "No panel; every icon is a die-cut sticker.", Kind.LIST, false),

    // ---- Modern / expressive family ----
    CLAY("clay", "Claymorphism",
            "Puffy pastel clay, soft dual depth.", Kind.LIST, true),
    NEON("neon", "Neon Grid",
            "Magenta and cyan glow on near-black glass.", Kind.LIST, false),
    BRUTALIST("brutalist", "Brutalist",
            "Hard black edges, white paper, raw shadow.", Kind.LIST, true),
    EDITORIAL("editorial", "Editorial Ink",
            "Deep ink panel, cream type, red rules.", Kind.LIST, false),
    FROST("frost", "Frost Light",
            "Light frosted glass with a cool white rim.", Kind.LIST, true),
    ARCADE("arcade", "Arcade CRT",
            "Purple cabinet, neon marquee, pixel energy.", Kind.LIST, false),

    // ---- Game-native family ----
    FANTASY_RPG("fantasy_rpg", "Fantasy RPG",
            "Gilded double frame, torch-lit wood, gold type.", Kind.LIST, false),
    CARBON("carbon", "Carbon",
            "Matte black plates, soft rounded slots, crisp white type.", Kind.LIST, false),
    PARCHMENT("parchment", "Parchment",
            "Warm paper, carved light slots, ink text.", Kind.LIST, true),
    ARCANE("arcane", "Arcane",
            "Violet glass, hex wells, gold runes.", Kind.LIST, false),
    OUTLINE("outline", "Outline",
            "No panel; rounded outlines and spaced uppercase labels.", Kind.LIST, false),
    SCIFI_TACTICAL("scifi_tactical", "Sci-Fi Tactical",
            "Cyan HUD, corner ticks, deep-space glass.", Kind.LIST, false);

    public enum Kind {
        LIST,
        GRID,
        DOCK
    }

    /** Loadable for existing configs, hidden from the gallery: each duplicates a stronger sibling. */
    private static final Set<HudStyle> HIDDEN = EnumSet.of(
            NOTION, GLASSMORPHISM, NEUMORPHISM, BENTO, DYNAMIC_ISLAND, DOCK,
            MATERIAL_EXPRESSIVE, TERMINAL, PIXEL_VANILLA, MINIMAL, CHIPS, STICKER, CLAY,
            ARCADE, EDITORIAL, FROST, NEON, BRUTALIST, SCIFI_TACTICAL, GROUPED, COMPACT);

    public static List<HudStyle> visibleStyles() {
        return java.util.Arrays.stream(values()).filter(style -> !HIDDEN.contains(style)).toList();
    }

    public static boolean isVisible(HudStyle style) {
        return style != null && !HIDDEN.contains(style);
    }

    private final String id;
    private final String title;
    private final String description;
    private final Kind kind;
    private final boolean light;

    HudStyle(String id, String title, String description, Kind kind, boolean light) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.kind = kind;
        this.light = light;
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    /** Fits the editor's small buttons. */
    public String shortTitle() {
        return switch (this) {
            case VANILLA -> "Vanilla";
            case STEEL -> "Steel";
            case HEX -> "Shield";
            case GROUPED -> "Group";
            case COMPACT -> "Compact";
            case NOTION -> "Notion";
            case LIQUID_GLASS -> "Liquid";
            case GLASSMORPHISM -> "Glass";
            case NEUMORPHISM -> "Clay";
            case BENTO -> "Bento";
            case DYNAMIC_ISLAND -> "Island";
            case DOCK -> "Dock";
            case MATERIAL_EXPRESSIVE -> "Material";
            case TERMINAL -> "Term";
            case PIXEL_VANILLA -> "Pixel";
            case BARE -> "Bare";
            case MINIMAL -> "Line";
            case CHIPS -> "Chips";
            case STICKER -> "Sticker";
            case CLAY -> "Clay";
            case NEON -> "Neon";
            case BRUTALIST -> "Raw";
            case EDITORIAL -> "Ink";
            case FROST -> "Frost";
            case ARCADE -> "Arcade";
            case FANTASY_RPG -> "Fantasy";
            case CARBON -> "Carbon";
            case PARCHMENT -> "Paper";
            case ARCANE -> "Arcane";
            case OUTLINE -> "Outline";
            case SCIFI_TACTICAL -> "SciFi";
        };
    }

    public Kind kind() {
        return kind;
    }

    public boolean light() {
        return light;
    }

    public int textColor() {
        return switch (this) {
            case CLAY -> 0xFF3B3150;
            case EDITORIAL -> 0xFFF4E9D8;
            case FROST -> 0xFF1E2A38;
            case ARCADE -> 0xFFFFF3C4;
            case VANILLA -> 0xFF3F3F3F;
            case GROUPED -> 0xFFF2F4F8;
            case COMPACT -> 0xFFEDEDED;
            case FANTASY_RPG -> 0xFFFFE373;
            case SCIFI_TACTICAL -> 0xFFE6FBFF;
            default -> light ? 0xFF232323 : 0xFFF6F8FB;
        };
    }

    public int mutedColor() {
        return switch (this) {
            case CLAY -> 0xFF7E6F96;
            case EDITORIAL -> 0xFFBFAE9A;
            case FROST -> 0xFF5A6B7D;
            case ARCADE -> 0xFFC9A7FF;
            case VANILLA -> 0xFF3F3F3F;
            case GROUPED -> 0xFFAEB6C2;
            case COMPACT -> 0xFFB9B9B9;
            case FANTASY_RPG -> 0xFFC9A227;
            case SCIFI_TACTICAL -> 0xFF8FD8E6;
            default -> light ? 0xFF787774 : 0xFF9AA6B8;
        };
    }

    public int radius() {
        return switch (this) {
            case NOTION -> 7;
            case LIQUID_GLASS -> 16;
            case GLASSMORPHISM -> 12;
            case NEUMORPHISM -> 12;
            case BENTO -> 12;
            case DYNAMIC_ISLAND -> 99;
            case DOCK -> 10;
            case MATERIAL_EXPRESSIVE -> 18;
            case TERMINAL, PIXEL_VANILLA, BARE, MINIMAL, BRUTALIST, EDITORIAL -> 0;
            case CHIPS, STICKER -> 9;
            case CLAY -> 14;
            case NEON -> 8;
            case FROST -> 14;
            case ARCADE -> 4;
            case VANILLA, HEX -> 0;
            case STEEL, COMPACT -> 2;
            case GROUPED -> 5;
            case FANTASY_RPG, PARCHMENT -> 0;
            case CARBON -> 8;
            case ARCANE -> 10;
            case OUTLINE -> 5;
            case SCIFI_TACTICAL -> 4;
        };
    }

    public static HudStyle byId(String id) {
        if (id != null) {
            String normalised = id.trim().toLowerCase(Locale.ROOT);
            for (HudStyle style : values()) {
                if (style.id.equals(normalised) || style.name().equalsIgnoreCase(normalised)) {
                    return style;
                }
            }
        }
        return VANILLA;
    }
}
