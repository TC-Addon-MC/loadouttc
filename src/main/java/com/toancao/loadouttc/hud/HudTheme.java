package com.toancao.loadouttc.hud;

import com.toancao.loadouttc.config.HudConfig;

/**
 * Resolved design tokens. Everything comes from three independent axes — panel material, slot
 * shape and label treatment — plus the accent, the panel radius and the opacity. There is no preset
 * palette any more: changing one axis cannot drag in another style's colours.
 */
public final class HudTheme {
    public int accent = 0xFF7C9EFF;
    public boolean dark = true;

    public int panel;
    public int panelBorder;
    public int panelSheen;
    public int slot;
    public int slotBorder;
    public int slotHover;
    public int text = 0xFFF4F7FC;
    public int muted = 0xFFA9B4C4;
    public int shadow;
    public int glow;

    public int gaugeFull = 0xFF3FBF5F;
    public int gaugeMid = 0xFFE0B020;
    public int gaugeLow = 0xFFD93A3A;

    public int radiusPanel;
    public int radiusSlot;

    public PanelStyle panelKind = PanelStyle.DARK;
    public SlotStyle slotKind = SlotStyle.ROUNDED;
    public TextStyle textKind = TextStyle.NORMAL;
    /** 0 means "use muted"; otherwise an override for the label colour. */
    public int labelColor;

    public boolean panelVisible = true;
    public boolean plateAlways = true;
    public boolean plateOnHover;
    public boolean bevel;
    public boolean hardShadow;
    public boolean uppercase;
    public boolean alwaysHeaders;
    public boolean numberRows;
    public int topBar;
    public boolean focusRing = true;
    public int borderWidth = 1;

    public int minPad = 8;
    public int slotInset = 3;
    public int minGap = 4;
    public int labelGap = 6;

    private HudTheme() {
    }

    /** Builds the theme from the four config axes. */
    public static HudTheme of(HudConfig cfg) {
        HudStyle legacy = cfg.style();
        HudTheme theme = build(PanelStyle.resolve(cfg.panelStyle, legacy),
                SlotStyle.resolve(cfg.slotStyle, legacy),
                TextStyle.resolve(cfg.textStyle, legacy),
                cfg.accentColor(), cfg.radius, cfg.opacity);
        if (cfg.slotBorderWidth >= 0) {
            theme.borderWidth = cfg.slotBorderWidth;
        }
        return theme;
    }

    /** Legacy entry point: maps a preset id to the three axes. */
    public static HudTheme of(HudStyle preset, int accent) {
        return build(PanelStyle.resolve("auto", preset),
                SlotStyle.resolve("auto", preset),
                TextStyle.resolve("auto", preset), accent, -1, 1.0f);
    }

    private static HudTheme build(PanelStyle panel, SlotStyle slot, TextStyle text, int accent,
                                  int radius, float opacity) {
        HudTheme t = new HudTheme();
        t.accent = accent;
        t.panelKind = panel;
        t.slotKind = slot;
        t.textKind = text;

        t.applyPanel();
        t.applySlot();
        t.applyText();

        if (radius >= 0) {
            t.radiusPanel = radius;
        }
        return t.faded(opacity);
    }

    // ---- Axes ----
    private void applyPanel() {
        topBar = 0;
        panelVisible = true;
        panelSheen = 0;
        shadow = 0x33000000;
        gaugeFull = 0xFF3FBF5F;
        gaugeMid = 0xFFE0B020;
        gaugeLow = 0xFFD93A3A;

        switch (panelKind) {
            case NONE -> {
                panelVisible = false;
                dark = true;
                minPad = 2;
                radiusPanel = 0;
                shadow = 0;
            }
            case VANILLA -> {
                dark = false;
                panel = 0xFFC6C6C6;
                panelBorder = 0xFF000000;
                minPad = 6;
                radiusPanel = 0;
                shadow = 0x1A000000;
            }
            case DARK -> {
                dark = true;
                panel = 0xF00D1117;
                panelBorder = 0xFF2A3542;
                minPad = 8;
                if (radiusPanel == 0) {
                    radiusPanel = 3;
                }
            }
            case GLASS -> {
                dark = true;
                panel = 0xC8121620;
                panelBorder = 0x33FFFFFF;
                panelSheen = 0x26FFFFFF;
                minPad = 8;
                if (radiusPanel < 10) {
                    radiusPanel = 14;
                }
            }
            case WOOD -> {
                dark = true;
                panel = 0xFF1C1613;
                panelBorder = 0xFFC2A24F;
                minPad = 8;
                radiusPanel = 0;
                shadow = 0x55000000;
                topBar = 16;
                gaugeFull = 0xFFD9A93C;
            }
            case CARBON -> {
                dark = true;
                panel = 0xFF0E0E11;
                panelBorder = 0xFF26262B;
                minPad = 8;
                shadow = 0x40000000;
            }
            case PARCHMENT -> {
                dark = false;
                panel = 0xFFEFE6D6;
                panelBorder = 0xFFC9B99A;
                minPad = 8;
                radiusPanel = 0;
                shadow = 0x1A000000;
            }
            case ARCANE -> {
                dark = true;
                panel = 0xFF1A1030;
                panelBorder = 0xFF6B4CB8;
                panelSheen = 0x2AA78BFA;
                minPad = 8;
                if (radiusPanel < 8) {
                    radiusPanel = 10;
                }
                shadow = 0x44000000;
                gaugeFull = 0xFFB388FF;
            }
        }
    }

    private void applySlot() {
        focusRing = true;
        plateOnHover = false;

        switch (slotKind) {
            case NONE -> {
                plateAlways = false;
                slotInset = 2;
            }
            case VANILLA -> {
                plateAlways = true;
                slot = 0xFF8B8B8B;
                slotBorder = 0xFF373737;
                slotHover = 0xFFA8A8A8;
                radiusSlot = 0;
                slotInset = 2;
            }
            case STEEL -> {
                plateAlways = true;
                borderWidth = 2;
                if (dark) {
                    slot = 0xFF0B0F15;
                    slotBorder = 0xFFA7BCCD;
                    slotHover = 0xFF141C26;
                } else {
                    slot = 0xFFE9EEF3;
                    slotBorder = 0xFF6E7F8C;
                    slotHover = 0xFFF3F7FA;
                }
                radiusSlot = 2;
                slotInset = 4;
            }
            case HEX -> {
                plateAlways = true;
                borderWidth = 2;
                slot = dark ? 0xFF23272E : 0xFFF3E9D7;
                slotBorder = dark ? 0xFFC3CBD6 : 0xFF7A6A4E;
                slotHover = dark ? 0xFF2C323A : 0xFFE8DCC4;
                radiusSlot = 0;
                slotInset = 4;
                focusRing = false;
            }
            case CUT -> {
                plateAlways = true;
                borderWidth = 2;
                slot = dark ? 0xFF23272E : 0xFFF3E9D7;
                slotBorder = dark ? 0xFFC3CBD6 : 0xFF7A6A4E;
                slotHover = dark ? 0xFF2C323A : 0xFFE8DCC4;
                radiusSlot = 0;
                slotInset = 3;
                focusRing = false;
            }
            case INSET -> {
                plateAlways = true;
                if (dark) {
                    slot = 0xFF0A0A0C;
                    slotBorder = 0xFF2E2E36;
                    slotHover = 0xFF14141A;
                } else {
                    slot = 0xFFD9C9AC;
                    slotBorder = 0xFF8C7A5C;
                    slotHover = 0xFFE5D8C0;
                }
                radiusSlot = 0;
                slotInset = 3;
            }

            case ROUNDED -> {
                plateAlways = true;
                slot = dark ? 0x14FFFFFF : 0x14000000;
                slotBorder = dark ? 0x22FFFFFF : 0x22000000;
                slotHover = dark ? 0x24FFFFFF : 0x1F000000;
                radiusSlot = 8;
                slotInset = 3;
            }
            case TRIANGLE -> {
                plateAlways = true;
                borderWidth = 2;
                slot = dark ? 0xFF23272E : 0xFFF3E9D7;
                slotBorder = dark ? 0xFFC3CBD6 : 0xFF7A6A4E;
                slotHover = dark ? 0xFF2C323A : 0xFFE8DCC4;
                radiusSlot = 0;
                slotInset = 5;
            }
            case ELLIPSE -> {
                plateAlways = true;
                borderWidth = 2;
                slot = dark ? 0xFF23272E : 0xFFF3E9D7;
                slotBorder = dark ? 0xFFC3CBD6 : 0xFF7A6A4E;
                slotHover = dark ? 0xFF2C323A : 0xFFE8DCC4;
                radiusSlot = 0;
                slotInset = 4;
            }
            case CIRCLE -> {
                plateAlways = true;
                slot = dark ? 0x18FFFFFF : 0x18000000;
                slotBorder = dark ? 0x33FFFFFF : 0x33000000;
                slotHover = dark ? 0x2AFFFFFF : 0x26000000;
                radiusSlot = 99;
                slotInset = 4;
            }
        }
    }

    private void applyText() {
        if (dark) {
            text = 0xFFF4F7FC;
            muted = 0xFFA9B4C4;
        } else {
            text = 0xFF2B2A28;
            muted = 0xFF5F5B55;
        }
        uppercase = false;
        labelColor = 0;
        switch (textKind) {
            case UPPER -> uppercase = true;
            case BRIGHT -> labelColor = text;
            case GOLD -> labelColor = dark ? 0xFFFFE373 : 0xFF8A6A18;
            case OUTLINE -> labelColor = 0xFFFFFFFF;
            default -> {
            }
        }
    }

    // ---- Queries ----

    public boolean drawsPlate(boolean hover) {
        return plateAlways || (plateOnHover && hover);
    }

    /** Applies the configured opacity to every surface token exactly once. */
    public HudTheme faded(float opacity) {
        HudTheme t = new HudTheme();
        t.accent = accent;
        t.dark = dark;
        t.panel = StylePainter.alpha(panel, opacity);
        t.panelBorder = StylePainter.alpha(panelBorder, opacity);
        t.panelSheen = StylePainter.alpha(panelSheen, opacity);
        t.slot = StylePainter.alpha(slot, opacity);
        t.slotBorder = StylePainter.alpha(slotBorder, opacity);
        t.slotHover = StylePainter.alpha(slotHover, opacity);
        t.text = StylePainter.alpha(text, opacity);
        t.muted = StylePainter.alpha(muted, opacity);
        t.shadow = StylePainter.alpha(shadow, opacity);
        t.glow = StylePainter.alpha(glow, opacity);
        t.gaugeFull = StylePainter.alpha(gaugeFull, opacity);
        t.gaugeMid = StylePainter.alpha(gaugeMid, opacity);
        t.gaugeLow = StylePainter.alpha(gaugeLow, opacity);
        t.radiusPanel = radiusPanel;
        t.radiusSlot = radiusSlot;
        t.panelKind = panelKind;
        t.slotKind = slotKind;
        t.textKind = textKind;
        t.labelColor = labelColor == 0 ? 0 : StylePainter.alpha(labelColor, opacity);
        t.panelVisible = panelVisible;
        t.plateAlways = plateAlways;
        t.plateOnHover = plateOnHover;
        t.bevel = bevel;
        t.hardShadow = hardShadow;
        t.uppercase = uppercase;
        t.alwaysHeaders = alwaysHeaders;
        t.numberRows = numberRows;
        t.topBar = topBar;
        t.focusRing = focusRing;
        t.borderWidth = borderWidth;
        t.minPad = minPad;
        t.slotInset = slotInset;
        t.minGap = minGap;
        t.labelGap = labelGap;
        return t;
    }
}
