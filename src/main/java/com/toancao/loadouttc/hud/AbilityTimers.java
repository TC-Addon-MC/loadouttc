package com.toancao.loadouttc.hud;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Timers for "ability in use" feedback. Vanilla only exposes item cooldowns; this is the hook for
 * other mods to say "this slot is channelling an ability for N seconds".
 */
public final class AbilityTimers {
    private static final Map<String, Entry> ACTIVE = new ConcurrentHashMap<>();

    private AbilityTimers() {
    }

    /** Starts (or restarts) a timer on one slot. {@code color = 0} uses the configured accent. */
    public static void show(String slotKey, long durationMillis, int color) {
        if (slotKey == null || slotKey.isBlank() || durationMillis <= 0L) {
            return;
        }
        long now = System.currentTimeMillis();
        ACTIVE.put(slotKey, new Entry(now, now + durationMillis, color));
    }

    public static void hide(String slotKey) {
        if (slotKey != null) {
            ACTIVE.remove(slotKey);
        }
    }

    public static void clear() {
        ACTIVE.clear();
    }

    /** Remaining fraction + seconds, or {@code null} when nothing is running. */
    public static Remaining remaining(String slotKey) {
        Entry entry = slotKey == null ? null : ACTIVE.get(slotKey);
        if (entry == null) {
            return null;
        }
        long now = System.currentTimeMillis();
        if (now >= entry.end) {
            ACTIVE.remove(slotKey, entry);
            return null;
        }
        long total = Math.max(1L, entry.end - entry.start);
        float fraction = (entry.end - now) / (float) total;
        float seconds = (entry.end - now) / 1000.0f;
        return new Remaining(fraction, seconds, entry.color);
    }

    public record Remaining(float fraction, float seconds, int color) {
    }

    private record Entry(long start, long end, int color) {
    }
}
