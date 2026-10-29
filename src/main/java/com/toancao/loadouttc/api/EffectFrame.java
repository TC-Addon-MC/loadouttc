package com.toancao.loadouttc.api;

/**
 * A per-frame transform for one HUD slot. Custom effects return this from a {@link SlotEffect}.
 *
 * <p>{@code spinDegrees} is applied around the icon's vertical axis when {@code threeD} is true,
 * otherwise around its screen-space centre. {@code glowColor} is ARGB; use {@code 0} for no glow.
 */
public record EffectFrame(float offsetX, float offsetY, float scale, float spinDegrees,
                          boolean threeD, int glowColor, float glowStrength) {
    public static final EffectFrame NONE = new EffectFrame(0.0f, 0.0f, 1.0f, 0.0f, false, 0, 0.0f);

    public EffectFrame {
        if (scale <= 0.0f) {
            scale = 1.0f;
        }
        glowStrength = Math.max(0.0f, Math.min(1.0f, glowStrength));
    }

    public static EffectFrame spin(float degrees) {
        return new EffectFrame(0, 0, 1, degrees, true, 0, 0);
    }

    public static EffectFrame offset(float x, float y) {
        return new EffectFrame(x, y, 1, 0, false, 0, 0);
    }

    public static EffectFrame scale(float scale) {
        return new EffectFrame(0, 0, scale, 0, false, 0, 0);
    }

    public static EffectFrame glow(int color, float strength) {
        return new EffectFrame(0, 0, 1, 0, false, color, strength);
    }
}
