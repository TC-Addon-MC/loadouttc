package com.toancao.loadouttc.hud;

import com.toancao.loadouttc.config.HudConfig;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Paints the chrome for a resolved {@link HudTheme}.
 *
 * <p>There is no switch over "style" any more: the panel material, the slot shape and the label
 * treatment are independent axes ({@link PanelStyle}, {@link SlotStyle}, {@link TextStyle}). A
 * preset only fills those three in; the user can then change any one of them.
 *
 * <p>Every routine receives an explicit rectangle and is contractually forbidden to paint outside
 * it, so no style can collide with a neighbour.
 */
public final class StylePainter {
    private StylePainter() {
    }

    // ---- Panel ----
    public static void panel(HudStyle style, GuiGraphics g, int x, int y, int w, int h,
                             HudConfig cfg) {
        panel(style, HudTheme.of(style, cfg.accentColor()).faded(cfg.opacity), g, x, y, w, h,
                cfg.opacity);
    }

    public static void panel(HudStyle style, HudTheme t, GuiGraphics g, int x, int y, int w,
                             int h, float opacity) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int r = Math.min(t.radiusPanel, Math.min(w, h) / 2);

        if (t.hardShadow) {
            g.fill(x + 4, y + 4, x + w + 4, y + h + 4, alpha(0xFF000000, opacity));
        } else if ((t.shadow >>> 24) != 0) {
            softShadow(g, x, y, w, h, r, t.shadow);
        }

        if (!t.panelVisible) {
            return;
        }

        roundRect(g, x, y, w, h, r, t.panel);
        if ((t.panelBorder >>> 24) != 0) {
            outline(g, x, y, w, h, r, t.panelBorder);
        }
        if ((t.panelSheen >>> 24) != 0 && h > 8 && w > 2 * r + 4) {
            fill(g, x + r, y + 1, x + w - r, y + 2, t.panelSheen);
        }

        switch (t.panelKind) {
            case VANILLA -> {
                fill(g, x, y, x + w, y + 1, alpha(0xFF000000, opacity));
                fill(g, x, y + h - 1, x + w, y + h, alpha(0xFF000000, opacity));
                fill(g, x, y, x + 1, y + h, alpha(0xFF000000, opacity));
                fill(g, x + w - 1, y, x + w, y + h, alpha(0xFF000000, opacity));
                fill(g, x + 1, y + 1, x + w - 1, y + 2, alpha(0xFFFFFFFF, opacity));
                fill(g, x + 1, y + 1, x + 2, y + h - 1, alpha(0xFFFFFFFF, opacity));
                fill(g, x + 1, y + h - 2, x + w - 1, y + h - 1,
                        alpha(0xFF555555, opacity));
                fill(g, x + w - 2, y + 1, x + w - 1, y + h - 1,
                        alpha(0xFF555555, opacity));
            }
            case DARK -> {
                outline(g, x + 1, y + 1, w - 2, h - 2, Math.max(0, r - 1),
                        alpha(0xFF161D26, opacity));
            }
            case GLASS -> {
                int third = Math.max(1, h / 3);
                for (int i = 0; i < third; i++) {
                    float fade = 1.0f - i / (float) third;
                    fill(g, x + 1, y + 1 + i, x + w - 1, y + 2 + i,
                            alpha(0xFFFFFFFF, 0.045f * fade * opacity));
                }
            }
            case WOOD -> {
                for (int i = 0; i < h; i++) {
                    float f = i / (float) Math.max(1, h - 1);
                    fill(g, x, y + i, x + w, y + i + 1, blend(0xFF1C1613, 0xFF0A0604, f));
                }
                int gilt = alpha(0xFFC2A24F, opacity);
                fill(g, x, y, x + w, y + 2, gilt);
                fill(g, x, y + h - 2, x + w, y + h, gilt);
                fill(g, x, y, x + 2, y + h, gilt);
                fill(g, x + w - 2, y, x + w, y + h, gilt);
                outline(g, x + 3, y + 3, w - 6, h - 6, 0, alpha(0xFF4A3525, opacity));
                for (int cx = 0; cx < 2; cx++) {
                    for (int cy = 0; cy < 2; cy++) {
                        int nx = cx == 0 ? x + 4 : x + w - 6;
                        int ny = cy == 0 ? y + 4 : y + h - 6;
                        fill(g, nx, ny, nx + 2, ny + 2, alpha(0xFFC2A24F, opacity));
                    }
                }
            }
            default -> {
            }
        }
    }

    // ---- Slot ----
    public static void slot(HudStyle style, GuiGraphics g, int x, int y, int size, boolean hover,
                            HudConfig cfg) {
        slot(style, HudTheme.of(style, cfg.accentColor()).faded(cfg.opacity), g, x, y, size, size,
                hover, cfg.opacity);
    }

    public static void slot(HudStyle style, HudTheme t, GuiGraphics g, int x, int y, int w,
                            int h, boolean hover, float opacity) {
        slot(style, t, g, x, y, w, h, hover, true, opacity, t.accent);
    }

    public static void slot(HudStyle style, HudTheme t, GuiGraphics g, int x, int y, int w,
                            int h, boolean hover, boolean underline, float opacity) {
        slot(style, t, g, x, y, w, h, hover, underline, opacity, t.accent);
    }

    public static void slot(HudStyle style, HudTheme t, GuiGraphics g, int x, int y, int w,
                            int h, boolean hover, boolean underline, float opacity, int tint) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int r = Math.min(t.radiusSlot, Math.min(w, h) / 2);
        boolean plate = t.plateAlways || (hover && t.plateOnHover);

        if (t.slotKind == SlotStyle.NONE) {
            if (hover) {
                outline(g, x, y, w, h, 3, alpha(0xFFFFFFFF, 0.45f * opacity));
            }
            return;
        }

        if (t.slotKind == SlotStyle.TRIANGLE || t.slotKind == SlotStyle.ELLIPSE) {
            boolean triangle = t.slotKind == SlotStyle.TRIANGLE;
            int bw = Math.max(1, Math.min(4, t.borderWidth));
            int border = alpha(t.slotBorder, opacity);
            int fillColor = hover ? alpha(t.slotHover, opacity) : alpha(t.slot, opacity);
            if (triangle) {
                triangle(g, x, y, w, h, border);
                triangle(g, x + bw, y + bw + 1, w - 2 * bw, h - bw - 1, fillColor);
            } else {
                ellipse(g, x, y, w, h, border);
                ellipse(g, x + bw, y + bw, w - 2 * bw, h - 2 * bw, fillColor);
            }
            return;
        }
        if (t.slotKind == SlotStyle.HEX || t.slotKind == SlotStyle.CUT) {
            int bw = Math.max(1, Math.min(4, t.borderWidth));
            int cut = t.slotKind == SlotStyle.CUT
                    ? Math.max(2, Math.min(w, h) / 6)
                    : Math.max(3, Math.min(w, h) / 3);
            hex(g, x, y, w, h, cut, alpha(t.slotBorder, opacity));
            hex(g, x + bw, y + bw, w - 2 * bw, h - 2 * bw, Math.max(1, cut - bw),
                    hover ? alpha(t.slotHover, opacity) : alpha(t.slot, opacity));
            return;
        }

        if (plate) {
            roundRect(g, x, y, w, h, r, hover && (t.slotHover >>> 24) != 0
                    ? t.slotHover : t.slot);
            if ((t.slotBorder >>> 24) != 0) {
                int bw = Math.max(0, Math.min(4, t.borderWidth));
                for (int i = 0; i < bw; i++) {
                    outline(g, x + i, y + i, w - 2 * i, h - 2 * i, Math.max(0, r - i),
                            t.slotBorder);
                }
            }
        }

        switch (t.slotKind) {
            case VANILLA -> {
                fill(g, x, y, x + w, y + 1, alpha(0xFF373737, opacity));
                fill(g, x, y, x + 1, y + h, alpha(0xFF373737, opacity));
                fill(g, x, y + h - 1, x + w, y + h, alpha(0xFF373737, opacity));
                fill(g, x + w - 1, y, x + w, y + h, alpha(0xFF373737, opacity));
                fill(g, x + 1, y + 1, x + w - 1, y + 2, alpha(0xFF6E6E6E, opacity));
                fill(g, x + 1, y + 1, x + 2, y + h - 1, alpha(0xFF6E6E6E, opacity));
                fill(g, x + 1, y + h - 2, x + w - 1, y + h - 1,
                        alpha(0xFFB5B5B5, opacity));
                fill(g, x + w - 2, y + 1, x + w - 1, y + h - 1,
                        alpha(0xFFB5B5B5, opacity));
            }
            case STEEL -> {
                outline(g, x, y, w, h, 2, alpha(0xFF22303D, opacity));
                outline(g, x + 1, y + 1, w - 2, h - 2, 1, alpha(t.slotBorder, opacity));
                fill(g, x + 2, y + 2, x + w - 2, y + 3,
                        alpha(0xFFD7E4EE, 0.45f * opacity));
                fill(g, x + 2, y + h - 3, x + w - 2, y + h - 2,
                        alpha(0xFF05080C, 0.55f * opacity));
                if (hover) {
                    outline(g, x - 2, y - 2, w + 4, h + 4, 3,
                            alpha(0xFFFFFFFF, 0.95f * opacity));
                    outline(g, x - 3, y - 3, w + 6, h + 6, 4, alpha(0x44FFFFFF, opacity));
                }
            }
            case INSET -> {
                // Recessed well; the bevel follows whatever slot colour the panel produced, so it
                // reads as a dark well on dark panels and a carved well on paper.
                int shadowEdge = alpha(t.slotBorder, opacity);
                int lightEdge = alpha(blend(t.slot, 0xFFFFFFFF, 0.55f), opacity);
                fill(g, x, y, x + w, y + 1, shadowEdge);
                fill(g, x, y, x + 1, y + h, shadowEdge);
                fill(g, x, y + h - 1, x + w, y + h, lightEdge);
                fill(g, x + w - 1, y, x + w, y + h, lightEdge);
                if (hover) {
                    outline(g, x, y, w, h, 0, alpha(t.accent, 0.75f * opacity));
                }
            }

            case ROUNDED, CIRCLE -> {
                if (w > 2 * r + 4) {
                    fill(g, x + r, y + 1, x + w - r, y + 2,
                            alpha(0xFFFFFFFF, (hover ? 0.18f : 0.08f) * opacity));
                }
                if (hover) {
                    outline(g, x - 1, y - 1, w + 2, h + 2, r + 1,
                            alpha(t.accent, 0.55f * opacity));
                }
            }
            default -> {
            }
        }
    }

    // ---- Labels ----
    public static void header(HudStyle style, GuiGraphics g, Font font, String text, int x, int y,
                              int width, HudConfig cfg) {
        header(style, HudTheme.of(style, cfg.accentColor()).faded(cfg.opacity), g, font, text, x, y,
                width, cfg.opacity);
    }

    public static void header(HudStyle style, HudTheme t, GuiGraphics g, Font font, String text,
                              int x, int y, int width, float opacity) {
        header(style, t, g, font, text, x, y, width, opacity, t.accent);
    }

    public static void header(HudStyle style, HudTheme t, GuiGraphics g, Font font, String text,
                              int x, int y, int width, float opacity, int tint) {
        String label = t.uppercase ? text.toUpperCase(Locale.ROOT) : text;
        int color = t.labelColor != 0 ? alpha(t.labelColor, opacity) : t.muted;
        g.drawString(font, label, x, y, color, t.dark);

        int lineX = x + font.width(label) + 6;
        if (lineX < x + width) {
            // Baseline, not mid-glyph: a rule through the letters is what made headers look off.
            fill(g, lineX, y + 8, x + width, y + 9, alpha(t.muted, 0.35f * opacity));
        }
    }

    // ---- Shared palette helpers ----
    public static int countColor(HudStyle style) {
        return style.light() ? 0xFF2A2A2A : 0xFFFFFFFF;
    }

    public static int durabilityBack(HudStyle style) {
        return style.light() ? 0xFFB9B4AC : 0xFF101010;
    }

    public static int accent(HudStyle style, HudConfig cfg) {
        return cfg.accentColor();
    }

    // ---- Primitives ----
    public static void softShadow(GuiGraphics g, int x, int y, int w, int h, int radius,
                                  int color) {
        roundRect(g, x - 2, y - 1, w + 4, h + 5, radius + 2, alpha(color, 0.16f));
        roundRect(g, x - 1, y, w + 2, h + 3, radius + 1, alpha(color, 0.22f));
    }

    public static void roundRect(GuiGraphics g, int x, int y, int w, int h, int radius, int color) {
        if (w <= 0 || h <= 0 || (color >>> 24) == 0) {
            return;
        }
        int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
        if (r == 0) {
            g.fill(x, y, x + w, y + h, color);
            return;
        }
        g.fill(x + r, y, x + w - r, y + h, color);
        g.fill(x, y + r, x + r, y + h - r, color);
        g.fill(x + w - r, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            int dy = r - i;
            int inset = r - (int) Math.round(Math.sqrt(Math.max(0, r * r - dy * dy)));
            int rowY = y + i;
            g.fill(x + inset, rowY, x + w - inset, rowY + 1, color);
            g.fill(x + inset, y + h - 1 - i, x + w - inset, y + h - i, color);
        }
    }

    public static void outline(GuiGraphics g, int x, int y, int w, int h, int radius, int color) {
        if (w <= 0 || h <= 0 || (color >>> 24) == 0) {
            return;
        }
        if (radius <= 0) {
            g.fill(x, y, x + w, y + 1, color);
            g.fill(x, y + h - 1, x + w, y + h, color);
            g.fill(x, y, x + 1, y + h, color);
            g.fill(x + w - 1, y, x + w, y + h, color);
            return;
        }
        int r = Math.min(radius, Math.min(w, h) / 2);
        for (int i = 0; i < h; i++) {
            int inset = 0;
            if (i < r) {
                int dy = r - i;
                inset = r - (int) Math.round(Math.sqrt(Math.max(0, r * r - dy * dy)));
            } else if (i >= h - r) {
                int dy = r - (h - 1 - i);
                inset = r - (int) Math.round(Math.sqrt(Math.max(0, r * r - dy * dy)));
            }
            int left = x + inset;
            int right = x + w - inset;
            if (i == 0 || i == h - 1) {
                g.fill(left, y + i, right, y + i + 1, color);
            } else {
                g.fill(left, y + i, left + 1, y + i + 1, color);
                g.fill(right - 1, y + i, right, y + i + 1, color);
            }
        }
    }

    /**
     * Traces {@code fraction} of the slot border on the plate outline, so the cooldown / ability
     * timer *is* the frame instead of a ring drawn on top of it. The band grows inward by
     * {@code thickness} px, one nested outline per pixel, so every shape keeps its own edge.
     */
    public static void frameProgress(GuiGraphics g, int x, int y, int w, int h, float fraction,
                                     int color, int track, int radius, SlotStyle shape) {
        frameProgress(g, x, y, w, h, fraction, color, track, radius, shape, false, 0L, false, 1);
    }

    public static void frameProgress(GuiGraphics g, int x, int y, int w, int h, float fraction,
                                     int color, int track, int radius, SlotStyle shape,
                                     boolean flow, long now) {
        frameProgress(g, x, y, w, h, fraction, color, track, radius, shape, flow, now, false, 1);
    }

    public static void frameProgress(GuiGraphics g, int x, int y, int w, int h, float fraction,
                                     int color, int track, int radius, SlotStyle shape,
                                     boolean flow, long now, boolean reverse) {
        frameProgress(g, x, y, w, h, fraction, color, track, radius, shape, flow, now, reverse, 1);
    }

    public static void frameProgress(GuiGraphics g, int x, int y, int w, int h, float fraction,
                                     int color, int track, int radius, SlotStyle shape,
                                     boolean flow, long now, boolean reverse, int thickness) {
        if (w <= 0 || h <= 0) {
            return;
        }
        float share = Math.max(0.0f, Math.min(1.0f, fraction));
        int band = Math.max(1, Math.min(4, thickness));
        int bright = flow ? blend(color, 0xFFFFFFFF, 0.65f) : 0;
        for (int layer = 0; layer < band; layer++) {
            int bw = w - layer * 2;
            int bh = h - layer * 2;
            if (bw <= 0 || bh <= 0) {
                break;
            }
            java.util.List<int[]> points = perimeter(x + layer, y + layer, bw, bh,
                    Math.max(0, radius - layer), shape);
            int total = points.size();
            int filled = Math.round(total * share);
            for (int i = 0; i < total; i++) {
                int[] point = points.get(i);
                boolean lit = reverse ? i >= total - filled : i < filled;
                g.fill(point[0], point[1], point[0] + 1, point[1] + 1,
                        lit ? color : track);
            }
            if (flow && total > 0) {
                int head = (int) ((now / 12L) % total);
                for (int i = 0; i < 3; i++) {
                    int[] point = points.get((head + i) % total);
                    g.fill(point[0], point[1], point[0] + 1, point[1] + 1, bright);
                }
            }
        }
    }

    private static java.util.List<int[]> perimeter(int x, int y, int w, int h, int radius,
                                                   SlotStyle shape) {
        java.util.List<int[]> points = new java.util.ArrayList<>();

        int topInset = shapeInset(0, w, h, radius, shape);
        for (int i = topInset; i < w - topInset; i++) {
            points.add(new int[]{x + i, y});
        }
        for (int row = 1; row < h; row++) {
            int inset = shapeInset(row, w, h, radius, shape);
            points.add(new int[]{x + w - inset - 1, y + row});
        }
        int bottomInset = shapeInset(h - 1, w, h, radius, shape);
        for (int i = w - bottomInset - 2; i >= bottomInset; i--) {
            points.add(new int[]{x + i, y + h - 1});
        }
        for (int row = h - 2; row >= 1; row--) {
            int inset = shapeInset(row, w, h, radius, shape);
            points.add(new int[]{x + inset, y + row});
        }
        return points;
    }

    private static int shapeInset(int row, int w, int h, int radius, SlotStyle shape) {
        int min = Math.min(w, h);
        if (shape == SlotStyle.HEX) {
            return cutInset(row, h, Math.max(3, min / 3));
        }
        if (shape == SlotStyle.CUT) {
            return cutInset(row, h, Math.max(2, min / 6));
        }
        if (shape == SlotStyle.TRIANGLE) {
            return Math.round((w / 2.0f) * (1.0f - row / (float) Math.max(1, h - 1)));
        }
        if (shape == SlotStyle.ELLIPSE) {
            float ry = h / 2.0f;
            float dy = (row + 0.5f - ry) / Math.max(0.001f, ry);
            float t = 1.0f - dy * dy;
            return t <= 0.0f ? w / 2 : Math.round((w / 2.0f) * (1.0f - (float) Math.sqrt(t)));
        }
        int r = Math.max(0, Math.min(radius, min / 2));
        if (r <= 0) {
            return 0;
        }
        if (row < r) {
            int dy = r - row;
            return r - (int) Math.round(Math.sqrt(Math.max(0, r * r - dy * dy)));
        }
        if (row >= h - r) {
            int dy = r - (h - 1 - row);
            return r - (int) Math.round(Math.sqrt(Math.max(0, r * r - dy * dy)));
        }
        return 0;
    }

    private static int cutInset(int row, int h, int cut) {
        if (row < cut) {
            return cut - row;
        }
        if (row >= h - cut) {
            return row - (h - cut) + 1;
        }
        return 0;
    }
    /** One-pixel hexagon border, used for focus rings on hex slots. */
    public static void hexOutline(GuiGraphics g, int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0 || (color >>> 24) == 0) {
            return;
        }
        int cut = Math.max(3, Math.min(w, h) / 4);
        for (int i = 0; i < h; i++) {
            int inset;
            if (i < cut) {
                inset = cut - i;
            } else if (i >= h - cut) {
                inset = i - (h - cut) + 1;
            } else {
                inset = 0;
            }
            inset = Math.min(inset, Math.max(0, w / 2 - 1));
            g.fill(x + inset, y + i, x + inset + 1, y + i + 1, color);
            g.fill(x + w - inset - 1, y + i, x + w - inset, y + i + 1, color);
            if (i == 0 || i == h - 1) {
                g.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            }
        }
    }

    private static void triangle(GuiGraphics g, int x, int y, int w, int h, int color) {
        int centerX = x + w / 2;
        for (int row = 1; row < h; row++) {
            int half = Math.round((w / 2.0f) * (row / (float) Math.max(1, h - 1)));
            g.fill(centerX - half, y + row, centerX + half, y + row + 1, color);
        }
    }

    private static void ellipse(GuiGraphics g, int x, int y, int w, int h, int color) {
        float rx = w / 2.0f;
        float ry = h / 2.0f;
        int centerX = x + w / 2;
        for (int row = 0; row < h; row++) {
            float dy = (row + 0.5f - ry) / Math.max(0.001f, ry);
            float t = 1.0f - dy * dy;
            if (t <= 0.0f) {
                continue;
            }
            int half = Math.round(rx * (float) Math.sqrt(t));
            g.fill(centerX - half, y + row, centerX + half, y + row + 1, color);
        }
    }

    /** Filled hexagon: vertical sides with 45-degree top and bottom corners. */
    private static void hex(GuiGraphics g, int x, int y, int w, int h, int cut, int color) {
        if (w <= 0 || h <= 0 || (color >>> 24) == 0) {
            return;
        }
        for (int i = 0; i < h; i++) {
            int inset;
            if (i < cut) {
                inset = cut - i;
            } else if (i >= h - cut) {
                inset = i - (h - cut) + 1;
            } else {
                inset = 0;
            }
            inset = Math.min(inset, Math.max(0, w / 2 - 1));
            g.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
        }
    }

    private static void fill(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        if (x2 <= x1 || y2 <= y1 || (color >>> 24) == 0) {
            return;
        }
        g.fill(x1, y1, x2, y2, color);
    }

    public static int alpha(int color, float opacity) {
        int a = (int) (((color >>> 24) & 0xFF) * clamp(opacity, 0.0f, 1.0f));
        return (a << 24) | (color & 0xFFFFFF);
    }

    public static int blend(int from, int to, float t) {
        float f = clamp(t, 0.0f, 1.0f);
        int a = (int) (((from >>> 24) & 0xFF) * (1 - f) + ((to >>> 24) & 0xFF) * f);
        int r = (int) (((from >> 16) & 0xFF) * (1 - f) + ((to >> 16) & 0xFF) * f);
        int gg = (int) (((from >> 8) & 0xFF) * (1 - f) + ((to >> 8) & 0xFF) * f);
        int b = (int) ((from & 0xFF) * (1 - f) + (to & 0xFF) * f);
        return (a << 24) | (r << 16) | (gg << 8) | b;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
