package com.toancao.loadouttc.config;

import com.toancao.loadouttc.hud.CooldownStyle;
import com.toancao.loadouttc.hud.DurabilityStyle;
import com.toancao.loadouttc.hud.HudStyle;
import com.toancao.loadouttc.hud.PanelStyle;
import com.toancao.loadouttc.hud.SlotStyle;
import com.toancao.loadouttc.hud.TextStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HudConfig {
    public boolean enabled = true;
    public String style = HudStyle.VANILLA.id();
    public String anchor = HudAnchor.TOP_RIGHT.name();
    public int x = 0;
    public int y = 0;

    public String size = "auto";
    public float scale = 1.0f;
    public float opacity = 1.0f;
    public int iconSize = 18;
    public int gap = 6;
    public int padding = 8;

    public String panelStyle = "none";
    public String slotStyle = "vanilla";
    public String textStyle = "normal";
    public int radius = -1;

    public int panelPadding = -1;
    public int rowGap = -1;
    public int labelGap = -1;
    public int slotInset = -1;
    public int slotBorderWidth = -1;
    public int screenMargin = -1;

    public int iconNudgeX = 0;
    public int iconNudgeY = 0;
    public int labelNudgeX = 0;
    public int labelNudgeY = 0;
    public int gaugeNudgeY = 0;
    public boolean debugGuides = false;
    public String durabilityColor = "auto";
    public String cooldownColor = "auto";
    public String abilityColor = "auto";
    public LayerOptions durabilityOptions = new LayerOptions();
    public LayerOptions cooldownOptions = new LayerOptions();
    public LayerOptions abilityOptions = new LayerOptions();
    public String labelEffect = "none";
    public boolean frameFlow = false;
    /**
     * Icon motion; one at a time. Animation lives here: spin, bob, pulse, shake.
     */
    public String iconEffect = "none";
    /**
     * Icon light; one at a time and independent of the motion, so "spin + glow" is a look the
     * player can actually build instead of two exclusive choices in one list.
     */
    public String iconLight = "none";
    public String effectSpeed = "normal";
    public String effectStrength = "normal";
    /** How far the accent is mixed towards white for every "auto" layer colour. */
    public int effectBlend = 35;

    public static final java.util.Set<String> MOTION_EFFECTS = java.util.Set.of(
            "none", "spin", "spin_3d", "spin_fast", "spin_3d_fast", "bob", "bounce", "sway",
            "pulse", "shake");
    public static final java.util.Set<String> LIGHT_EFFECTS = java.util.Set.of(
            "none", "glow", "flash", "rainbow");

    public boolean labels = false;
    public String labelMode = "slot";
    public boolean groupHeaders = true;
    public boolean emptySlots = false;
    public boolean durability = true;
    public String durabilityStyle = "bar";
    public boolean cooldowns = true;
    public String cooldownStyle = "frame";
    public String abilityStyle = "ring";
    public boolean stackCount = true;
    public boolean animations = true;
    public boolean horizontal = false;
    public String layoutMode = "";
    public int columns = 3;
    public int circleRadius = 48;
    public boolean showCosmetic = true;
    public boolean tooltips = true;

    public String slotFilter = "all";
    public List<String> hiddenSlots = new ArrayList<>();
    public List<String> visibleSlots = new ArrayList<>();
    public List<String> hiddenSources = new ArrayList<>();
    // A fresh install shows the accessory mod's slots only. Vanilla armour and offhand are one
    // click away in Slots, but they are not what this HUD is for.
    public boolean vanillaArmor = false;
    public boolean vanillaOffhand = false;
    public boolean vanillaHotbar = false;
    public boolean vanillaInventory = false;

    public String sortMode = "default";
    public List<String> slotOrder = new ArrayList<>();

    public int maxSlots = 64;
    public int maxLabelChars = 16;
    public String accent = "7C9EFF";
    public String featuredSlot = "";
    public String featuredEffect = "spin";
    public Map<String, SlotOverride> slotOverrides = new LinkedHashMap<>();
    /** Slots unlocked for their own settings in the design screen; sticky per slot. */
    public List<String> unlockedSlots = new ArrayList<>();

    public List<HudCluster> clusters = new ArrayList<>();
    public String activeCluster = "";

    public HudStyle style() { return HudStyle.byId(style); }
    public HudAnchor anchor() { return HudAnchor.byName(anchor); }
    public DurabilityStyle durabilityStyle() { return DurabilityStyle.byId(durabilityStyle); }
    public boolean showsDurability() { return durability && durabilityStyle() != DurabilityStyle.OFF; }
    public CooldownStyle cooldownStyle() { return CooldownStyle.byId(cooldownStyle); }
    public com.toancao.loadouttc.hud.AbilityStyle abilityStyle() { return com.toancao.loadouttc.hud.AbilityStyle.byId(abilityStyle); }
    public boolean showsCooldowns() { return cooldowns && cooldownStyle() != CooldownStyle.OFF; }
    public SlotOverride override(String slotKey) { return slotOverrides == null ? null : slotOverrides.get(slotKey); }
    /**
     * Whether the design screen should open this slot's own settings straight away. A slot that
     * already has overrides counts as unlocked even when the list predates the unlocked flag.
     */
    public boolean isSlotUnlocked(String slotKey) {
        if (slotKey == null) return false;
        if (unlockedSlots != null && unlockedSlots.contains(slotKey)) return true;
        SlotOverride existing = override(slotKey);
        return existing != null && !existing.isEmpty();
    }
    public void setSlotUnlocked(String slotKey, boolean unlocked) {
        if (slotKey == null || slotKey.isBlank()) return;
        if (unlockedSlots == null) unlockedSlots = new ArrayList<>();
        unlockedSlots.remove(slotKey);
        if (unlocked) unlockedSlots.add(slotKey);
    }
    public SlotOverride overrideOrCreate(String slotKey) {
        if (slotOverrides == null) slotOverrides = new LinkedHashMap<>();
        return slotOverrides.computeIfAbsent(slotKey, ignored -> new SlotOverride());
    }
    public CooldownStyle cooldownStyleFor(String slotKey) {
        SlotOverride o = override(slotKey);
        if (o == null || o.cooldown == null || o.cooldown.isBlank()) return cooldownStyle();
        return CooldownStyle.byId(o.cooldown);
    }
    public boolean showsCooldownsFor(String slotKey) {
        SlotOverride o = override(slotKey);
        if (o == null || o.cooldown == null || o.cooldown.isBlank()) return showsCooldowns();
        return CooldownStyle.byId(o.cooldown) != CooldownStyle.OFF;
    }
    public DurabilityStyle durabilityStyleFor(String slotKey) {
        SlotOverride o = override(slotKey);
        if (o == null || o.durability == null || o.durability.isBlank()) return durabilityStyle();
        return DurabilityStyle.byId(o.durability);
    }
    public boolean showsDurabilityFor(String slotKey) {
        SlotOverride o = override(slotKey);
        if (o == null || o.durability == null || o.durability.isBlank()) return showsDurability();
        return DurabilityStyle.byId(o.durability) != DurabilityStyle.OFF;
    }
    /** Motion for this slot: its own override, else the shared value. */
    public String effectFor(String slotKey) {
        SlotOverride o = override(slotKey);
        if (o != null && o.effect != null && !o.effect.isBlank()) return o.effect;
        return iconEffect == null || iconEffect.isBlank() ? "none" : iconEffect;
    }

    /** Light for this slot: its own override, else the shared value. */
    public String lightFor(String slotKey) {
        SlotOverride o = override(slotKey);
        if (o != null && o.light != null && !o.light.isBlank()) return o.light;
        return iconLight == null || iconLight.isBlank() ? "none" : iconLight;
    }

    public boolean anyIconEffectFor(String slotKey) {
        return !"none".equals(effectFor(slotKey)) || !"none".equals(lightFor(slotKey));
    }

    public float effectSpeedFactor() {
        return switch (effectSpeed == null ? "normal" : effectSpeed) {
            case "slow" -> 0.55f;
            case "fast" -> 1.7f;
            default -> 1.0f;
        };
    }

    public float effectStrengthFactor() {
        return switch (effectStrength == null ? "normal" : effectStrength) {
            case "soft" -> 0.6f;
            case "strong" -> 1.35f;
            default -> 1.0f;
        };
    }

    /** The shared "effect colour": accent mixed towards white by the blend setting. */
    public int blended(int color) {
        return com.toancao.loadouttc.hud.StylePainter.blend(color, 0xFFFFFFFF,
                Math.max(0, Math.min(90, effectBlend)) / 100.0f);
    }

    public int accentColor() { return parseColor(accent, 0xFF7C9EFF); }
    public boolean sourceEnabled(String sourceId) { return sourceId != null && (hiddenSources == null || !hiddenSources.contains(sourceId)); }
    public boolean slotVisible(String key) {
        if (key == null || key.isBlank()) return false;
        if ("selected".equals(slotFilter)) return visibleSlots != null && visibleSlots.contains(key);
        return hiddenSlots == null || !hiddenSlots.contains(key);
    }
    public boolean vanillaGroupEnabled(String group) {
        return switch (group == null ? "" : group) {
            case "armor" -> vanillaArmor;
            case "offhand" -> vanillaOffhand;
            case "hotbar" -> vanillaHotbar;
            case "inventory" -> vanillaInventory;
            default -> true;
        };
    }
    public void setSlotVisible(String key, boolean visible) {
        if (key == null || key.isBlank()) return;
        if ("selected".equals(slotFilter)) {
            if (visibleSlots == null) visibleSlots = new ArrayList<>();
            if (visible) { if (!visibleSlots.contains(key)) visibleSlots.add(key); }
            else visibleSlots.remove(key);
            return;
        }
        if (hiddenSlots == null) hiddenSlots = new ArrayList<>();
        if (visible) hiddenSlots.remove(key);
        else if (!hiddenSlots.contains(key)) hiddenSlots.add(key);
    }
    /** Switches a slot off in every filter mode, unlike {@link #setSlotVisible} in "show only". */
    public void hideSlot(String key) {
        if (key == null || key.isBlank()) return;
        if (hiddenSlots == null) hiddenSlots = new ArrayList<>();
        if (!hiddenSlots.contains(key)) hiddenSlots.add(key);
        if (visibleSlots != null) visibleSlots.remove(key);
    }
    public void showAllSlots() { slotFilter = "all"; hiddenSlots = new ArrayList<>(); visibleSlots = new ArrayList<>(); }
    public void showOnly(Collection<String> keys) { slotFilter = "selected"; visibleSlots = keys == null ? new ArrayList<>() : new ArrayList<>(keys); hiddenSlots = new ArrayList<>(); }
    public void switchFilter(Collection<String> currentlyVisible) {
        if ("selected".equals(slotFilter)) { showAllSlots(); return; }
        visibleSlots = currentlyVisible == null ? new ArrayList<>() : new ArrayList<>(currentlyVisible);
        hiddenSlots = new ArrayList<>();
        slotFilter = "selected";
    }
    public void ensureOrder(Collection<String> keys) {
        if (slotOrder == null) slotOrder = new ArrayList<>();
        if (keys == null) return;
        for (String key : keys) if (key != null && !key.isBlank() && !slotOrder.contains(key)) slotOrder.add(key);
    }
    public int orderIndex(String key) { return slotOrder == null ? -1 : slotOrder.indexOf(key); }
    public boolean moveSlot(String key, int delta) {
        if (key == null || slotOrder == null) return false;
        int from = slotOrder.indexOf(key);
        if (from < 0) return false;
        int to = Math.max(0, Math.min(slotOrder.size() - 1, from + delta));
        if (to == from) return false;
        slotOrder.remove(from);
        slotOrder.add(to, key);
        return true;
    }
    public float effectiveScale(double guiScale) {
        float base = switch (size == null ? "auto" : size) {
            case "micro" -> 0.5f;
            case "tiny" -> 0.65f;
            case "small" -> 0.8f;
            case "medium" -> 1.0f;
            case "large" -> 1.25f;
            case "xlarge" -> 1.5f;
            case "huge" -> 2.0f;
            case "giant" -> 3.0f;
            case "custom" -> scale;
            // Auto means "one HUD unit is one GUI unit", so a slot keeps the vanilla hotbar unit
            // at every GUI scale instead of drifting on higher ones. The scale is 0.8 so a full
            // loadout of slots still fits next to the hotbar.
            default -> 0.8f;
        };
        return clamp(base, 0.5f, 3.0f);
    }
    public String sizeLabel() {
        return switch (size == null ? "auto" : size) {
            case "micro" -> "XXS";
            case "tiny" -> "XS";
            case "small" -> "S";
            case "medium" -> "M";
            case "large" -> "L";
            case "xlarge" -> "XL";
            case "huge" -> "2x";
            case "giant" -> "3x";
            case "custom" -> String.format(java.util.Locale.ROOT, "%.2fx", scale);
            default -> "Auto";
        };
    }
    public String sizeDescription(double guiScale) {
        return switch (size == null ? "auto" : size) {
            case "micro" -> "Micro (0.50x)";
            case "tiny" -> "Extra small (0.65x)";
            case "small" -> "Small (0.80x)";
            case "medium" -> "Medium (1.00x)";
            case "large" -> "Large (1.25x)";
            case "xlarge" -> "Extra large (1.50x)";
            case "huge" -> "Double (2.00x)";
            case "giant" -> "Triple (3.00x)";
            case "custom" -> String.format(java.util.Locale.ROOT, "Custom (%.2fx)", scale);
            default -> "Auto - vanilla size (0.80x)";
        };
    }
    public void sanitize() {
        if (!HudStyle.isVisible(style())) style = HudStyle.VANILLA.id();
        style = style().id();
        anchor = anchor().name();
        if (size == null || !size.matches("auto|micro|tiny|small|medium|large|xlarge|huge|giant|custom")) size = "auto";
        scale = clamp(scale, 0.5f, 3.0f);
        opacity = clamp(opacity, 0.2f, 1.0f);
        iconSize = (int) clamp(iconSize, 12, 40);
        if ((iconSize & 1) != 0) iconSize = Math.min(40, iconSize + 1);
        gap = (int) clamp(gap, 0, 16);
        padding = (int) clamp(padding, 0, 20);
        maxSlots = (int) clamp(maxSlots, 1, 128);
        maxLabelChars = (int) clamp(maxLabelChars, 4, 48);
        panelStyle = PanelStyle.byId(panelStyle).id();
        slotStyle = SlotStyle.byId(slotStyle).id();
        textStyle = TextStyle.byId(textStyle).id();
        radius = (int) clamp(radius, -1, 16);
        panelPadding = (int) clamp(panelPadding, -1, 20);
        rowGap = (int) clamp(rowGap, -1, 16);
        labelGap = (int) clamp(labelGap, -1, 16);
        slotInset = (int) clamp(slotInset, -1, 8);
        slotBorderWidth = (int) clamp(slotBorderWidth, -1, 4);
        screenMargin = (int) clamp(screenMargin, -1, 32);
        iconNudgeX = (int) clamp(iconNudgeX, -6, 6);
        iconNudgeY = (int) clamp(iconNudgeY, -6, 6);
        labelNudgeX = (int) clamp(labelNudgeX, -8, 8);
        labelNudgeY = (int) clamp(labelNudgeY, -8, 8);
        gaugeNudgeY = (int) clamp(gaugeNudgeY, -6, 10);
        if (labelEffect == null || !labelEffect.matches("none|glow|fade|bob")) labelEffect = "none";
        if (iconEffect == null || !MOTION_EFFECTS.contains(iconEffect)) iconEffect = "none";
        if (iconLight == null || !LIGHT_EFFECTS.contains(iconLight)) iconLight = "none";
        if (effectSpeed == null || !effectSpeed.matches("slow|normal|fast")) effectSpeed = "normal";
        if (effectStrength == null || !effectStrength.matches("soft|normal|strong")) effectStrength = "normal";
        effectBlend = (int) clamp(effectBlend, 0, 90);
        durabilityColor = tidyColor(durabilityColor);
        cooldownColor = tidyColor(cooldownColor);
        abilityColor = tidyColor(abilityColor);
        if (durabilityOptions == null) durabilityOptions = new LayerOptions();
        if (cooldownOptions == null) cooldownOptions = new LayerOptions();
        if (abilityOptions == null) abilityOptions = new LayerOptions();
        durabilityOptions.sanitize();
        cooldownOptions.sanitize();
        abilityOptions.sanitize();
        PanelStyle panel = PanelStyle.resolve(panelStyle, style());
        if ((panel == PanelStyle.VANILLA || panel == PanelStyle.PARCHMENT) && ("bright".equals(textStyle) || "gold".equals(textStyle))) textStyle = "normal";
        if (SlotStyle.resolve(slotStyle, style()) == SlotStyle.NONE) {
            if ("frame".equals(cooldownStyle)) cooldownStyle = "arc";
            if ("frame".equals(abilityStyle)) abilityStyle = "ring";
        }
        durabilityStyle = durabilityStyle().id();
        cooldownStyle = cooldownStyle().id();
        abilityStyle = com.toancao.loadouttc.hud.AbilityStyle.byId(abilityStyle).id();
        if (labelMode == null || !labelMode.matches("slot|item")) labelMode = "slot";
        if (slotFilter == null || !slotFilter.matches("all|selected")) slotFilter = "all";
        if (sortMode == null || !sortMode.matches("default|name|type|source|custom")) sortMode = "default";
        if (layoutMode == null || layoutMode.isBlank()) layoutMode = horizontal ? "horizontal" : "vertical";
        else if (!layoutMode.matches("vertical|horizontal|grid|circle")) layoutMode = horizontal ? "horizontal" : "vertical";
        horizontal = "horizontal".equals(layoutMode);
        columns = (int) clamp(columns, 1, 8);
        circleRadius = (int) clamp(circleRadius, 16, 200);
        hiddenSlots = cleanKeys(hiddenSlots);
        visibleSlots = cleanKeys(visibleSlots);
        hiddenSources = cleanKeys(hiddenSources);
        slotOrder = cleanKeys(slotOrder);
        unlockedSlots = cleanKeys(unlockedSlots);
        if (clusters == null) clusters = new ArrayList<>();
        clusters.removeIf(java.util.Objects::isNull);
        if (clusters.isEmpty()) { HudCluster main = new HudCluster(); main.id = "main"; main.name = "Main"; clusters.add(main); }
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (HudCluster cluster : clusters) {
            cluster.sanitize();
            if (!ids.add(cluster.id)) { cluster.id = "c" + (ids.size() + 1); ids.add(cluster.id); }
        }
        java.util.Set<String> claimed = new java.util.HashSet<>();
        for (HudCluster cluster : clusters) {
            if (cluster.slots != null && !cluster.slots.isEmpty()) cluster.slots.removeIf(key -> !claimed.add(key));
        }
        boolean anyRest = false;
        for (HudCluster cluster : clusters) anyRest |= cluster.rest;
        if (!anyRest) clusters.get(0).rest = true;
        // Panels hold their own position; the shared anchor is only adopted once, on load.
        for (HudCluster cluster : clusters) {
            if (!cluster.ownPosition) {
                cluster.adoptPosition(this);
            }
        }
        if (activeCluster == null || cluster(activeCluster) == null) {
            activeCluster = clusters.isEmpty() ? "" : clusters.get(0).id;
        }
        ensurePanelScopes();
        // With a single panel the shared fields own the slot selection. A stale per-panel scope
        // would keep overriding what the slot menu writes, so fold it back in and drop it.
        if (clusters.size() <= 1) {
            for (HudCluster cluster : clusters) {
                if (cluster.scope != null) {
                    cluster.scope.applyTo(this);
                    cluster.scope = null;
                }
            }
        }
        if (featuredSlot == null) featuredSlot = "";
        if (featuredEffect == null || featuredEffect.isBlank()) featuredEffect = "spin";
        if (accent == null || !accent.matches("(?i)[0-9a-f]{6}")) accent = "7C9EFF";
        if (slotOverrides == null) slotOverrides = new LinkedHashMap<>();
        slotOverrides.entrySet().removeIf(entry -> {
            if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null) return true;
            entry.getValue().sanitize();
            return entry.getValue().isEmpty();
        });
    }
    private static List<String> cleanKeys(List<String> keys) {
        List<String> clean = new ArrayList<>();
        if (keys == null) return clean;
        for (String key : keys) {
            if (key == null) continue;
            String trimmed = key.trim();
            if (!trimmed.isEmpty() && !clean.contains(trimmed)) clean.add(trimmed);
        }
        return clean;
    }
    private static String tidyColor(String value) {
        if (value == null) return "auto";
        String clean = value.startsWith("#") ? value.substring(1) : value;
        if ("auto".equalsIgnoreCase(clean) || clean.matches("(?i)[0-9a-f]{6}")) return clean.toLowerCase(java.util.Locale.ROOT);
        return "auto";
    }
    public int durabilityTint(int fallback) { return "auto".equals(durabilityColor) ? fallback : parseColor(durabilityColor, fallback); }
    public int cooldownTint(int accent) { return "auto".equals(cooldownColor) ? blended(accent) : parseColor(cooldownColor, accent); }
    public int abilityTint(int accent) { return "auto".equals(abilityColor) ? blended(accent) : parseColor(abilityColor, accent); }
    public static int parseColor(String text, int fallback) {
        if (text == null) return fallback;
        String clean = text.startsWith("#") ? text.substring(1) : text;
        if (!clean.matches("(?i)[0-9a-f]{6}")) return fallback;
        try { return 0xFF000000 | Integer.parseInt(clean, 16); } catch (NumberFormatException ignored) { return fallback; }
    }
    private static float clamp(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }
    public String layoutKind() {
        if (layoutMode != null && layoutMode.matches("vertical|horizontal|grid|circle")) return layoutMode;
        return horizontal ? "horizontal" : "vertical";
    }
    public HudCluster cluster(String id) {
        if (id == null || id.isBlank() || clusters == null) return null;
        for (HudCluster cluster : clusters) if (cluster != null && id.equals(cluster.id)) return cluster;
        return null;
    }
    public HudCluster clusterFor(String slotKey) {
        if (clusters == null || clusters.isEmpty()) return null;
        for (HudCluster cluster : clusters) if (cluster != null && cluster.contains(slotKey)) return cluster;
        for (HudCluster cluster : clusters) if (cluster != null && cluster.rest) return cluster;
        return clusters.get(0);
    }
    public String clusterIdFor(String slotKey) { HudCluster c = clusterFor(slotKey); return c == null ? "" : c.id; }
    public HudCluster addCluster(String name) {
        if (clusters == null) clusters = new ArrayList<>();
        HudCluster cluster = new HudCluster();
        cluster.id = nextClusterId();
        cluster.name = name == null || name.isBlank() ? cluster.id : name;
        cluster.rest = false;
        cluster.ownPosition = true;
        cluster.anchor = HudAnchor.CUSTOM.name();
        int step = 18 * (clusters.size() - 1);
        cluster.x = 24 + step;
        cluster.y = 24 + step;
        clusters.add(cluster);
        ensurePanelScopes();
        return cluster;
    }
    /**
     * New panel placed in a free screen spot. A panel's {@code x}/{@code y} are measured into the
     * screen from its anchor, so offsetting a right/bottom anchored panel by +24 moves it towards
     * its own corner - on top of the panel it was split from. Picking an anchor no other panel
     * uses is the only placement that is guaranteed not to overlap.
     */
    public HudCluster addClusterNear(String name, String sourceId) {
        HudCluster created = addCluster(name);
        HudAnchor spot = freeAnchor();
        if (spot != null) {
            created.anchor = spot.name();
            created.x = 0;
            created.y = 0;
        } else {
            // Every anchor is taken; cascade from the source as a last resort.
            HudCluster source = cluster(sourceId);
            created.anchor = HudAnchor.CUSTOM.name();
            created.x = Math.max(0, (source == null ? x : source.x) + 40);
            created.y = Math.max(0, (source == null ? y : source.y) + 40);
        }
        created.ownPosition = true;
        return created;
    }

    /** First screen anchor no panel is using yet, or {@code null} when all of them are taken. */
    private HudAnchor freeAnchor() {
        HudAnchor[] order = {HudAnchor.TOP_LEFT, HudAnchor.BOTTOM_LEFT, HudAnchor.BOTTOM_RIGHT,
                HudAnchor.TOP_RIGHT, HudAnchor.CENTER, HudAnchor.ABOVE_HOTBAR};
        for (HudAnchor candidate : order) {
            boolean used = false;
            if (clusters != null) {
                for (HudCluster cluster : clusters) {
                    if (cluster != null && HudAnchor.byName(cluster.anchor) == candidate) {
                        used = true;
                        break;
                    }
                }
            }
            if (!used) return candidate;
        }
        return null;
    }

    /** Points the editor at a panel; falls back to the first one. */
    public void setActiveCluster(String id) {
        activeCluster = cluster(id) != null ? id : (clusters.isEmpty() ? "" : clusters.get(0).id);
    }

    private String nextClusterId() {
        for (int i = 1; i < 1000; i++) {
            String id = "c" + i;
            if (cluster(id) == null && !"main".equals(id)) return id;
        }
        return "c" + (System.nanoTime() % 10000);
    }
    public HudCluster split(Collection<String> keys, String name) {
        HudCluster cluster = addCluster(name);
        for (String key : new ArrayList<>(keys == null ? List.<String>of() : keys)) if (key != null && !key.isBlank()) assign(key, cluster.id);
        return cluster;
    }
    public void assign(String slotKey, String clusterId) {
        if (slotKey == null || slotKey.isBlank() || clusters == null) return;
        for (HudCluster cluster : clusters) if (cluster != null && cluster.slots != null) cluster.slots.remove(slotKey);
        HudCluster target = cluster(clusterId);
        if (target != null && !target.rest) target.slots.add(slotKey);
    }
    /**
     * Absorbs one panel into another. The destination receives every slot of the absorbed panel,
     * keeps the "rest" role if the absorbed one had it, and can take over its look ("the absorbed
     * panel becomes the standard"). Returns false when the ids are invalid or equal.
     */
    public boolean mergeInto(String sourceId, String destId, boolean takeSourceLook) {
        if (sourceId == null || destId == null || sourceId.equals(destId)) return false;
        HudCluster source = cluster(sourceId);
        HudCluster dest = cluster(destId);
        if (source == null || dest == null || clusters == null || clusters.size() <= 1) return false;
        if (source.slots != null) {
            for (String key : new ArrayList<>(source.slots)) {
                if (!dest.slots.contains(key)) dest.slots.add(key);
            }
        }
        if (source.rest) dest.rest = true;
        if (takeSourceLook) {
            dest.look = source.look == null ? null : source.look.copy();
            if (dest.look != null) dest.look.clusters = new ArrayList<>();
        }
        clusters.remove(source);
        if (activeCluster != null && activeCluster.equals(sourceId)) activeCluster = "";
        boolean hasRest = false;
        for (HudCluster cluster : clusters) hasRest |= cluster.rest;
        if (!hasRest && !clusters.isEmpty()) clusters.get(0).rest = true;
        return true;
    }

    /**
     * Drops a panel and switches off the slots it was drawing, so deleting a panel never hands its
     * slots to another one. {@code allKeys} is every slot the client knows, needed for the catch-all
     * panel which draws slots it does not own; use {@link #mergeInto} to move slots instead.
     */
    public boolean removeCluster(String id, Collection<String> allKeys) {
        HudCluster target = cluster(id);
        if (target == null || clusters == null || clusters.size() <= 1) return false;
        List<String> removing = new ArrayList<>();
        if (target.slots != null) for (String key : target.slots) if (key != null) removing.add(key);
        if (allKeys != null) {
            for (String key : allKeys) {
                if (key != null && !removing.contains(key) && clusterFor(key) == target) removing.add(key);
            }
        }
        clusters.remove(target);
        if (activeCluster != null && activeCluster.equals(id)) activeCluster = "";
        boolean hasRest = false;
        for (HudCluster cluster : clusters) if (cluster.rest) { hasRest = true; break; }
        if (!hasRest && !clusters.isEmpty()) clusters.get(0).rest = true;
        for (String key : removing) {
            hideSlot(key);
            for (HudCluster other : clusters) {
                if (other != null && other.scope != null) other.scope.hide(key);
            }
        }
        return true;
    }

    public boolean removeCluster(String id) {
        return removeCluster(id, null);
    }
    public HudConfig copy() {
        HudConfig out = new HudConfig();
        out.enabled = enabled;
        out.style = style;
        out.anchor = anchor;
        out.x = x;
        out.y = y;
        out.size = size;
        out.scale = scale;
        out.opacity = opacity;
        out.iconSize = iconSize;
        out.gap = gap;
        out.padding = padding;
        out.panelStyle = panelStyle;
        out.slotStyle = slotStyle;
        out.textStyle = textStyle;
        out.radius = radius;
        out.panelPadding = panelPadding;
        out.rowGap = rowGap;
        out.labelGap = labelGap;
        out.slotInset = slotInset;
        out.slotBorderWidth = slotBorderWidth;
        out.screenMargin = screenMargin;
        out.iconNudgeX = iconNudgeX;
        out.iconNudgeY = iconNudgeY;
        out.labelNudgeX = labelNudgeX;
        out.labelNudgeY = labelNudgeY;
        out.gaugeNudgeY = gaugeNudgeY;
        out.debugGuides = debugGuides;
        out.durabilityColor = durabilityColor;
        out.cooldownColor = cooldownColor;
        out.abilityColor = abilityColor;
        out.durabilityOptions = durabilityOptions == null ? new LayerOptions() : durabilityOptions.copy();
        out.cooldownOptions = cooldownOptions == null ? new LayerOptions() : cooldownOptions.copy();
        out.abilityOptions = abilityOptions == null ? new LayerOptions() : abilityOptions.copy();
        out.labelEffect = labelEffect;
        out.frameFlow = frameFlow;
        out.iconEffect = iconEffect;
        out.iconLight = iconLight;
        out.effectSpeed = effectSpeed;
        out.effectStrength = effectStrength;
        out.effectBlend = effectBlend;
        out.labels = labels;
        out.labelMode = labelMode;
        out.groupHeaders = groupHeaders;
        out.emptySlots = emptySlots;
        out.durability = durability;
        out.durabilityStyle = durabilityStyle;
        out.cooldowns = cooldowns;
        out.cooldownStyle = cooldownStyle;
        out.abilityStyle = abilityStyle;
        out.stackCount = stackCount;
        out.animations = animations;
        out.horizontal = horizontal;
        out.layoutMode = layoutMode;
        out.columns = columns;
        out.circleRadius = circleRadius;
        out.showCosmetic = showCosmetic;
        out.tooltips = tooltips;
        out.slotFilter = slotFilter;
        out.hiddenSlots = new ArrayList<>(hiddenSlots == null ? List.of() : hiddenSlots);
        out.visibleSlots = new ArrayList<>(visibleSlots == null ? List.of() : visibleSlots);
        out.hiddenSources = new ArrayList<>(hiddenSources == null ? List.of() : hiddenSources);
        out.vanillaArmor = vanillaArmor;
        out.vanillaOffhand = vanillaOffhand;
        out.vanillaHotbar = vanillaHotbar;
        out.vanillaInventory = vanillaInventory;
        out.sortMode = sortMode;
        out.slotOrder = new ArrayList<>(slotOrder == null ? List.of() : slotOrder);
        out.maxSlots = maxSlots;
        out.maxLabelChars = maxLabelChars;
        out.accent = accent;
        out.featuredSlot = featuredSlot;
        out.featuredEffect = featuredEffect;
        out.slotOverrides = slotOverrides == null ? new LinkedHashMap<>() : new LinkedHashMap<>(slotOverrides);
        out.unlockedSlots = new ArrayList<>(unlockedSlots == null ? List.of() : unlockedSlots);
        out.clusters = new ArrayList<>();
        if (clusters != null) for (HudCluster cluster : clusters) out.clusters.add(cluster.copy());
        out.activeCluster = activeCluster;
        return out;
    }
    public HudCluster activeClusterRef() { return cluster(activeCluster); }

    /** True when slot selection belongs to the panels instead of the shared config. */
    public boolean perPanelSlots() {
        return clusters != null && clusters.size() > 1;
    }

    /**
     * Slot selection the slot menu edits: the selected panel's own, or {@code null} when a single
     * panel still follows the shared config.
     */
    public SlotScope activeSlotScope() {
        if (!perPanelSlots()) {
            return null;
        }
        HudCluster cluster = activeClusterRef();
        if (cluster == null) {
            return null;
        }
        if (cluster.scope == null) {
            cluster.scope = SlotScope.from(this);
        }
        return cluster.scope;
    }

    /**
     * Panels keep their own slot selection once a config has more than one. A panel that has none
     * yet starts from the shared settings, so splitting never changes what the HUD draws.
     */
    public void ensurePanelScopes() {
        if (!perPanelSlots()) {
            return;
        }
        for (HudCluster cluster : clusters) {
            if (cluster != null && cluster.scope == null) {
                cluster.scope = SlotScope.from(this);
            }
        }
    }

    /** Every panel's own slot selection; empty while the panels still share one. */
    public List<SlotScope> panelScopes() {
        List<SlotScope> out = new ArrayList<>();
        if (!perPanelSlots()) {
            return out;
        }
        for (HudCluster cluster : clusters) {
            if (cluster != null && cluster.scope != null) {
                out.add(cluster.scope);
            }
        }
        return out;
    }

    /**
     * Object the editor should write size/scale into: the selected panel's own look when a panel
     * is targeted, otherwise this config. The look is created on demand, so changing the size of a
     * panel is also the moment it stops following the shared look.
     */
    public HudConfig editTarget() {
        HudCluster cluster = activeClusterRef();
        if (cluster == null) {
            return this;
        }
        if (cluster.look == null) {
            HudConfig snapshot = copy();
            snapshot.clusters = new ArrayList<>();
            snapshot.activeCluster = "";
            snapshot.slotOverrides = new LinkedHashMap<>();
            snapshot.hiddenSlots = new ArrayList<>();
            snapshot.visibleSlots = new ArrayList<>();
            snapshot.slotOrder = new ArrayList<>();
            snapshot.unlockedSlots = new ArrayList<>();
            cluster.look = snapshot;
        }
        return cluster.look;
    }
    public HudAnchor effectiveAnchor() {
        HudCluster cluster = activeClusterRef();
        return cluster != null && cluster.ownPosition ? HudAnchor.byName(cluster.anchor) : anchor();
    }
    public void cycleActiveAnchor() {
        HudCluster cluster = activeClusterRef();
        if (cluster != null) {
            if (!cluster.ownPosition) cluster.adoptPosition(this);
            cluster.anchor = HudAnchor.byName(cluster.anchor).next().name();
            cluster.x = 0;
            cluster.y = 0;
            return;
        }
        anchor = anchor().next().name();
        x = 0;
        y = 0;
    }
}
