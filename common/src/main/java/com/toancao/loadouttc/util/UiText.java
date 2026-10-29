package com.toancao.loadouttc.util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.resources.language.I18n;

/**
 * Translates the fixed UI labels. Every label is written in English in the code and looked up as
 * {@code loadouttc.ui.<slug>}; a missing key keeps the English text, so adding a language never
 * changes how the menus behave.
 */
public final class UiText {
    private static final Map<String, String> CACHE = new HashMap<>();

    private UiText() {
    }

    /** Localised label, or the English text when the current language has no entry. */
    public static String s(String english) {
        if (english == null || english.isEmpty()) {
            return english;
        }
        String cached = CACHE.get(english);
        if (cached != null) {
            return cached;
        }
        String key = key(english);
        String value = I18n.exists(key) ? I18n.get(key) : english;
        CACHE.put(english, value);
        return value;
    }

    /**
     * Same lookup for a template with {@code %s} placeholders, e.g. {@code "Show %s slots"}.
     * The placeholders are dropped from the key, so the key stays readable.
     */
    public static String f(String english, Object... args) {
        String key = key(english);
        if (I18n.exists(key)) {
            String template = I18n.get(key);
            // A plain label can share the slug of a template ("Slots" vs "%s slots", both
            // "loadouttc.ui.slots"). Using that label would silently drop the count, so fall
            // back to the English template whenever the placeholder is missing.
            if (!english.contains("%s") || template.contains("%s")) {
                return I18n.get(key, args);
            }
        }
        return String.format(english, args);
    }

    /** Drops the cache so a language change is picked up the next time a menu opens. */
    public static void clear() {
        CACHE.clear();
    }

    /**
     * Section headers share a word with some chip labels ("Slots"), so they get their own
     * namespace and can stay upper case in every language.
     */
    public static String group(String english) {
        String key = "loadouttc.ui.group." + slug(english);
        return I18n.exists(key) ? I18n.get(key) : english;
    }

    /** {@code "Radius / border"} becomes {@code loadouttc.ui.radius_border}. */
    public static String key(String english) {
        return "loadouttc.ui." + slug(english);
    }

    private static String slug(String english) {
        return english.replaceAll("%[sd]", " ")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }
}
