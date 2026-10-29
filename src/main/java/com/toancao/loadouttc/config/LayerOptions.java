package com.toancao.loadouttc.config;

/**
 * Extra knobs shared by the three feedback layers (durability, cooldown, ability): thickness,
 * opacity, text mode, pulse, direction and colour.
 *
 * <p>Nested in the JSON so new options can be added without touching the flat config or breaking
 * old saves.
 */
public final class LayerOptions {
    /** Bar / arc / frame thickness in pixels, 1..4. */
    public int thickness = 2;
    /** Overall opacity of the layer, 30..100 percent. */
    public int opacity = 100;
    /** seconds | int | percent */
    public String text = "seconds";
    /** Pulse when the timer is nearly done. */
    public boolean pulse = true;
    /** Draw the arc / bar / frame from the opposite end. */
    public boolean reverse = false;
    /** Draw the faint background track behind the layer. */
    public boolean track = true;

    public void sanitize() {
        thickness = Math.max(1, Math.min(4, thickness));
        opacity = Math.max(30, Math.min(100, opacity));
        if (text == null || !text.matches("seconds|int|percent")) {
            text = "seconds";
        }
    }

    /** Independent copy, used when a cluster keeps its own look. */
    public LayerOptions copy() {
        LayerOptions out = new LayerOptions();
        out.thickness = thickness;
        out.opacity = opacity;
        out.text = text;
        out.pulse = pulse;
        out.reverse = reverse;
        out.track = track;
        return out;
    }

    public String textKind() {
        return text == null ? "seconds" : text;
    }

    public float alpha() {
        return Math.max(0.3f, Math.min(1.0f, opacity / 100.0f));
    }

    /** "6.2s" stays, "int" drops the decimal, "percent" shows the remaining share. */
    public String format(float remaining, String secondsText) {
        return switch (textKind()) {
            case "int" -> {
                String digits = secondsText.replaceAll("[^0-9.]", "");
                try {
                    yield Math.max(1, (int) Math.ceil(Float.parseFloat(digits))) + "s";
                } catch (NumberFormatException ignored) {
                    yield secondsText;
                }
            }
            case "percent" -> Math.round(Math.max(0.0f, Math.min(1.0f, remaining)) * 100) + "%";
            default -> secondsText;
        };
    }
}
