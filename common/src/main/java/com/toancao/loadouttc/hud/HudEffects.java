package com.toancao.loadouttc.hud;

import com.toancao.loadouttc.api.EffectContext;
import com.toancao.loadouttc.api.EffectFrame;
import com.toancao.loadouttc.api.SlotEffect;
import com.toancao.loadouttc.config.HudConfig;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * Built-in and third-party slot effects. Other mods register effects through
 * {@code LoadoutTcApi.registerEffect} and trigger them with {@code playEffect}.
 */
public final class HudEffects {
    private static final ConcurrentMap<String, SlotEffect> REGISTRY = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, TimedEffect> TIMED = new ConcurrentHashMap<>();

    static {
        register("none", context -> EffectFrame.NONE);
        register("hover", context -> new EffectFrame(0, 0, 1.05f, 0, false,
                context.accentColor(), 0.12f));
        // 2D spin rotates the icon inside its slot; 3D spins the model like a coin.
        register("spin", context -> new EffectFrame(0, 0, 1,
                context.seconds() * 360.0f % 360.0f, false, 0, 0));
        register("spin_3d", context -> new EffectFrame(0, 0, 1,
                context.seconds() * 300.0f % 360.0f, true, 0, 0));
        register("spin_fast", context -> new EffectFrame(0, 0, 1,
                context.seconds() * 720.0f % 360.0f, false, 0, 0));
        register("spin_3d_fast", context -> new EffectFrame(0, 0, 1,
                context.seconds() * 720.0f % 360.0f, true, 0, 0));
        register("bob", context -> new EffectFrame(0,
                (float) Math.sin(context.seconds() * 3.0f) * 2.5f, 1, 0, false, 0, 0));
        register("pulse", context -> {
            float wave = (float) Math.sin(context.seconds() * 6.0f);
            return new EffectFrame(0, 0, 1.0f + wave * 0.08f, 0, false,
                    context.accentColor(), 0.35f + wave * 0.15f);
        });
        register("shake", context -> new EffectFrame(
                (float) Math.sin(context.seconds() * 50.0f) * 1.5f,
                (float) Math.cos(context.seconds() * 43.0f) * 0.75f,
                1, 0, false, 0, 0));
        // Rocking on the screen axis, so it reads as "swaying" next to the full turns of spin.
        register("sway", context -> new EffectFrame(0, 0, 1,
                (float) Math.sin(context.seconds() * 2.2f) * 14.0f, false, 0, 0));
        register("bounce", context -> {
            float wave = Math.abs((float) Math.sin(context.seconds() * 2.6f));
            return new EffectFrame(0, -wave * 2.5f, 1.0f + wave * 0.04f, 0, false, 0, 0);
        });
        register("flash", context -> {
            float wave = 0.5f + 0.5f * (float) Math.sin(context.seconds() * 8.0f);
            return new EffectFrame(0, 0, 1, 0, false, context.accentColor(), wave);
        });
        // Constant halo: the light group's quiet option, so a still icon can still read as "on".
        register("glow", context -> new EffectFrame(0, 0, 1, 0, false,
                context.accentColor(), 0.55f));
        register("rainbow", context -> {
            float hue = (context.seconds() * 0.35f) % 1.0f;
            int color = 0xFF000000 | Mth.hsvToRgb(hue, 0.72f, 1.0f);
            return new EffectFrame(0, 0, 1, 0, false, color, 0.65f);
        });
    }

    private HudEffects() {
    }

    public static boolean register(String id, SlotEffect effect) {
        if (id == null || id.isBlank() || effect == null) {
            return false;
        }
        REGISTRY.put(id, effect);
        return true;
    }

    public static boolean unregister(String id) {
        return id != null && REGISTRY.remove(id) != null;
    }

    public static Set<String> ids() {
        return new TreeSet<>(REGISTRY.keySet());
    }

    public static boolean play(String slotKey, String effectId, long durationMillis, int color) {
        if (slotKey == null || slotKey.isBlank() || effectId == null
                || !REGISTRY.containsKey(effectId)) {
            return false;
        }
        long now = System.currentTimeMillis();
        long end = durationMillis <= 0L ? Long.MAX_VALUE : now + durationMillis;
        TIMED.put(slotKey, new TimedEffect(effectId, now, end, color));
        return true;
    }

    public static void clear(String slotKey) {
        if (slotKey != null) {
            TIMED.remove(slotKey);
        }
    }

    public static void clearAll() {
        TIMED.clear();
    }

    public static EffectFrame frame(String slotKey, ItemStack stack, boolean featured,
                                    boolean hovered, HudConfig config) {
        long now = System.currentTimeMillis();
        TimedEffect timed = TIMED.get(slotKey);
        if (timed != null && now >= timed.end) {
            TIMED.remove(slotKey, timed);
            timed = null;
        }

        String motionId;
        String lightId;
        float seconds;
        float progress;
        int color = config.blended(config.accentColor());
        if (timed != null) {
            motionId = timed.effectId;
            lightId = "none";
            seconds = (now - timed.start) / 1000.0f;
            progress = timed.end == Long.MAX_VALUE ? 1.0f
                    : Math.min(1.0f, (now - timed.start) / (float) (timed.end - timed.start));
            if (timed.color != 0) {
                color = timed.color;
            }
        } else {
            String motion = config.effectFor(slotKey);
            String light = config.lightFor(slotKey);
            boolean configured = !"none".equals(motion) || !"none".equals(light);
            if (configured) {
                motionId = motion;
                lightId = light;
            } else if (featured) {
                motionId = config.featuredEffect;
                lightId = "none";
            } else if (hovered) {
                motionId = "hover";
                lightId = "none";
            } else {
                return EffectFrame.NONE;
            }
            // Epoch seconds lose too much precision in a float (~128 s per step at 1.7e9) and
            // slow spins freeze. Sum in double, convert to float last.
            seconds = (clock(now) + phaseOf(slotKey)) * config.effectSpeedFactor();
            progress = 1.0f;
        }

        EffectContext context = new EffectContext(slotKey, stack, seconds, progress, featured,
                hovered, color);
        return combine(apply(motionId, context), apply(lightId, context),
                config.effectStrengthFactor());
    }

    private static EffectFrame apply(String effectId, EffectContext context) {
        SlotEffect effect = effectId == null || "none".equals(effectId)
                ? null : REGISTRY.get(effectId);
        return effect == null ? EffectFrame.NONE : effect.apply(context);
    }

    /**
     * Motion owns the transform, light owns the halo. A motion that glows on its own (pulse) keeps
     * its glow only when it is stronger than the light layer's, so "pulse + glow" never doubles up.
     */
    private static EffectFrame combine(EffectFrame motion, EffectFrame light, float strength) {
        int glowColor = 0;
        float glow = 0.0f;
        if (light.glowColor() != 0 && light.glowStrength() > 0.0f) {
            glowColor = light.glowColor();
            glow = light.glowStrength();
        }
        if (motion.glowColor() != 0 && motion.glowStrength() > glow) {
            glowColor = motion.glowColor();
            glow = motion.glowStrength();
        }
        return new EffectFrame(motion.offsetX(), motion.offsetY(), motion.scale(),
                motion.spinDegrees(), motion.threeD(), glowColor,
                Math.min(1.0f, glow * strength));
    }

    /** Wrapped wall clock in seconds; epoch seconds lose precision in a float. */
    private static float clock(long now) {
        return (float) ((now / 1000.0) % 3600.0);
    }

    /**
     * Stable offset per slot so two slots with the same effect never pulse or spin in lockstep.
     * Timed effects are exempt: they must start at zero when they are triggered.
     */
    private static float phaseOf(String slotKey) {
        if (slotKey == null || slotKey.isEmpty()) {
            return 0.0f;
        }
        return (Math.floorMod(slotKey.hashCode(), 600)) / 1000.0f;
    }

    private record TimedEffect(String effectId, long start, long end, int color) {
    }
}
