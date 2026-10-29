package com.toancao.loadouttc.gui;

import com.toancao.loadouttc.hud.StylePainter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Single palette and metric set for every menu screen. HUD looks are themed by the player; the
 * editor chrome is not, so all of it reads from here instead of repeating literals per screen.
 */
final class UiTheme {
    private UiTheme() {
    }

    // ---- Surfaces ----
    static final int BACKDROP_TOP = 0xE00A0E13;
    static final int BACKDROP_BOTTOM = 0xEC0E131A;
    static final int HEADER_BAND = 0xFF161D27;
    static final int HEADER_BAND_FADE = 0x00161D27;

    static final int CARD = 0xFF161B24;
    static final int CARD_HOVER = 0xFF1E2530;
    static final int CARD_OFF = 0xFF11141A;
    static final int OUTLINE = 0xFF2A3140;
    static final int OUTLINE_HOVER = 0xFF3A4353;
    static final int INSET = 0xFF232833;
    /** Preview wells, tooltip plates and any content that sits below the panel level. */
    static final int WELL = 0xFF0B0E14;
    static final int ICON_EMPTY = 0xFF262C36;
    static final int TRACK = 0x33FFFFFF;
    static final int SCRIM = 0x66000000;

    // ---- Text ----
    static final int TITLE = 0xFFF5F7FA;
    static final int SUBTITLE = 0xFF9AA6B8;
    static final int TEXT = 0xFFB8C4D8;
    static final int VALUE = 0xFFD6DEEA;
    static final int BRIGHT = 0xFFE8EEF8;
    static final int MUTED = 0xFF6F7A8C;
    static final int DISABLED = 0xFF5A6372;
    static final int ON_ACCENT = 0xFFFFFFFF;

    // ---- Status ----
    /** Editor chrome accent. Fixed so the menus never turn into the player's HUD colour. */
    static final int ACCENT = 0xFF7C9EFF;
    static final int INFO = ACCENT;
    static final int HINT = 0xFF7CDFFF;
    static final int WARN = 0xFFF59E0B;
    static final int OK = 0xFF4ADE80;
    static final int DANGER = 0xFFF87171;

    // ---- Metrics ----
    static final int HEADER_X = 16;
    static final int HEADER_TITLE_Y = 20;
    static final int HEADER_SUB_Y = 34;
    static final int HEADER_H = 44;

    static final int CHIP_H = 18;
    static final int CHIP_RADIUS = 4;
    static final int CARD_RADIUS = 6;
    static final int WELL_RADIUS = 8;
    static final int TIP_RADIUS = 4;
    static final int TIP_H = 15;

    static int chipText(boolean active, boolean enabled) {
        return !enabled ? DISABLED : active ? ON_ACCENT : TEXT;
    }

    /** Shared chip body: fill, outline and the accent tick that marks the active option. */
    static void chip(GuiGraphics graphics, int x, int y, int w, int h, boolean active,
                     boolean hovered, boolean enabled, int accent) {
        if (!enabled) {
            StylePainter.roundRect(graphics, x, y, w, h, CHIP_RADIUS, CARD_OFF);
            StylePainter.outline(graphics, x, y, w, h, CHIP_RADIUS, INSET);
            return;
        }
        StylePainter.roundRect(graphics, x, y, w, h, CHIP_RADIUS,
                active ? StylePainter.alpha(accent, 0.42f) : hovered ? CARD_HOVER : CARD);
        StylePainter.outline(graphics, x, y, w, h, CHIP_RADIUS,
                active ? accent : hovered ? StylePainter.alpha(accent, 0.6f) : OUTLINE);
        if (active) {
            int bar = Math.max(4, h - 8);
            StylePainter.roundRect(graphics, x + 3, y + (h - bar) / 2, 2, bar, 1, accent);
        }
    }

    static void tooltip(GuiGraphics graphics, Font font, String text, int mouseX, int mouseY,
                        int screenWidth, int accent) {
        int w = font.width(text) + 12;
        int x = Math.min(mouseX + 9, screenWidth - w - 4);
        int y = Math.max(4, mouseY - 16);
        StylePainter.roundRect(graphics, x, y, w, TIP_H, TIP_RADIUS,
                StylePainter.alpha(WELL, 0.94f));
        StylePainter.outline(graphics, x, y, w, TIP_H, TIP_RADIUS, accent);
        graphics.drawString(font, text, x + 6, y + 4, BRIGHT, false);
    }
}
