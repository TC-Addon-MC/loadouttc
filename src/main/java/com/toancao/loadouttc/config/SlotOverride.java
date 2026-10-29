package com.toancao.loadouttc.config;

/**
 * Per-slot overrides. An empty string means "inherit the global setting"; {@code "none"} /
 * {@code "off"} explicitly disables that feature for this slot.
 */
public final class SlotOverride {
    /** Motion: empty = inherit the panel, "none" = explicitly off. */
    public String effect = "";
    /** Light: independent of the motion, same inherit rules. */
    public String light = "";
    public String cooldown = "";
    public String durability = "";

    public boolean isEmpty() {
        return effect.isBlank() && light.isBlank() && cooldown.isBlank()
                && durability.isBlank();
    }

    public void sanitize() {
        effect = clean(effect);
        light = clean(light);
        cooldown = clean(cooldown);
        durability = clean(durability);
        if (!effect.isEmpty() && !HudConfig.MOTION_EFFECTS.contains(effect)) effect = "";
        if (!light.isEmpty() && !HudConfig.LIGHT_EFFECTS.contains(light)) light = "";
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
