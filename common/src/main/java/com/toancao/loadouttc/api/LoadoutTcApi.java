package com.toancao.loadouttc.api;

import com.toancao.loadouttc.LoadoutTcClient;
import com.toancao.loadouttc.compat.AccessorySource;
import com.toancao.loadouttc.compat.HudData;
import com.toancao.loadouttc.compat.HudSlot;
import com.toancao.loadouttc.config.ConfigStore;
import com.toancao.loadouttc.config.HudCluster;
import com.toancao.loadouttc.hud.HudRenderer;
import com.toancao.loadouttc.config.HudConfig;
import com.toancao.loadouttc.config.LayerOptions;
import com.toancao.loadouttc.config.SlotOverride;
import com.toancao.loadouttc.config.SlotScope;
import com.toancao.loadouttc.gui.HudEditorScreen;
import com.toancao.loadouttc.gui.SlotsScreen;
import com.toancao.loadouttc.hud.AbilityStyle;
import com.toancao.loadouttc.hud.AbilityTimers;
import com.toancao.loadouttc.hud.CooldownStyle;
import com.toancao.loadouttc.hud.DurabilityStyle;
import com.toancao.loadouttc.hud.HudEffects;
import com.toancao.loadouttc.hud.HudStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Public, client-side API for Loadout TC.
 *
 * <p>Other mods can change the active style/size/durability style, register new slot effects, or
 * trigger a built-in effect on a specific slot. Slot keys come from {@link #slotKeys()} and look
 * like {@code accessories/ring/0} or {@code trinkets/chest/necklace/0}.
 *
 * <p>All calls should be made on the client thread. Nothing here is required for the HUD to work;
 * the API is optional sugar for integrations and pack authors.
 */
public final class LoadoutTcApi {
    private static final Set<String> SIZES = Set.of("auto", "micro", "tiny", "small", "medium",
            "large", "xlarge", "huge", "giant", "custom");

    private LoadoutTcApi() {
    }

    // ---- Effects ----
    /** Registers (or replaces) a custom slot effect. */
    public static boolean registerEffect(String id, SlotEffect effect) {
        return HudEffects.register(id, effect);
    }

    public static boolean unregisterEffect(String id) {
        return HudEffects.unregister(id);
    }

    /** Every effect id currently available, including built-ins and mod-provided ones. */
    public static Set<String> effects() {
        return HudEffects.ids();
    }

    /**
     * Plays an effect on one slot.
     *
     * @param durationMillis duration in milliseconds; {@code 0} keeps it until cleared
     * @param color          ARGB tint/glow override, or {@code 0} to use the HUD accent
     */
    public static boolean playEffect(String slotKey, String effectId, long durationMillis,
                                     int color) {
        return HudEffects.play(slotKey, effectId, durationMillis, color);
    }

    public static boolean playEffect(String slotKey, String effectId, long durationMillis) {
        return playEffect(slotKey, effectId, durationMillis, 0);
    }

    public static void clearEffect(String slotKey) {
        HudEffects.clear(slotKey);
    }

    public static void clearAllEffects() {
        HudEffects.clearAll();
    }

    /** Plays the current featured effect on the featured slot. */
    public static boolean triggerFeaturedEffect(long durationMillis) {
        HudConfig config = ConfigStore.get();
        if (config.featuredSlot == null || config.featuredSlot.isBlank()) {
            return false;
        }
        return HudEffects.play(config.featuredSlot, config.featuredEffect, durationMillis, 0);
    }

    public static boolean triggerFeaturedEffect() {
        return triggerFeaturedEffect(2000L);
    }

    // ---- Slot info ----
    /**
     * One slot as exposed to other mods. {@code cooldownRemaining} and {@code abilityRemaining}
     * are fractions from 0 (ready) to 1 (just started); {@code abilitySeconds} is the remaining
     * time of the {@link #showAbility} timer. {@code damage}/{@code maxDamage} are 0 for
     * non-damageable items.
     */
    public record SlotView(String key, String source, String group, String label, int index,
                           String itemId, boolean empty, boolean visible, ItemStack stack,
                           int count, int damage, int maxDamage, float cooldownRemaining,
                           float abilityRemaining, float abilitySeconds) {
        public boolean damageable() {
            return maxDamage > 0;
        }

        public boolean onCooldown() {
            return cooldownRemaining > 0.0f;
        }
    }

    /** Keys currently drawn by the HUD, in display order. */
    public static List<String> slotKeys() {
        return visibleSlots().stream().map(SlotView::key).toList();
    }

    private static List<SlotView> views(boolean onlyVisible) {
        Minecraft minecraft = Minecraft.getInstance();
        HudConfig config = ConfigStore.get();
        if (minecraft.player == null) {
            return List.of();
        }
        List<HudSlot> slots = onlyVisible
                ? drawnOrder(minecraft, config)
                : cappedOrder(minecraft, config);
        List<SlotView> out = new ArrayList<>();
        for (HudSlot slot : slots) {
            boolean visible = drawnByPanel(config, slot);
            if (onlyVisible && !visible) {
                continue;
            }
            ItemStack stack = slot.stack() == null ? ItemStack.EMPTY : slot.stack();
            int count = stack.isEmpty() ? 0 : stack.getCount();
            boolean damageable = stack.isDamageableItem();
            AbilityTimers.Remaining ability = AbilityTimers.remaining(slot.key());
            out.add(new SlotView(slot.key(), slot.source(), slot.group(), slot.groupLabel(),
                    slot.index(), slot.itemId(), slot.empty(), visible, stack, count,
                    damageable ? stack.getDamageValue() : 0,
                    damageable ? stack.getMaxDamage() : 0,
                    cooldownRemaining(minecraft, stack),
                    ability == null ? 0.0f : ability.fraction(),
                    ability == null ? 0.0f : ability.seconds()));
        }
        return out;
    }

    /** Shared order for {@link #slots()}: the whole config sorted as one list. */
    private static List<HudSlot> cappedOrder(Minecraft minecraft, HudConfig config) {
        List<HudSlot> slots = HudRenderer.sortSlots(
                HudData.collectAll(minecraft, config), config);
        if (!config.perPanelSlots() && slots.size() > config.maxSlots) {
            slots = new ArrayList<>(slots.subList(0, config.maxSlots));
        }
        return slots;
    }

    /**
     * Draw order for {@link #visibleSlots()}: panels in their own order, each sorted with the
     * panel's resolved view, exactly like the renderer stacks them. A single panel is just the
     * shared sort. Hidden panels and slots past a panel's cap are left out.
     */
    private static List<HudSlot> drawnOrder(Minecraft minecraft, HudConfig config) {
        List<HudSlot> all = HudData.collectAll(minecraft, config);
        List<HudSlot> out = new ArrayList<>();
        if (!config.perPanelSlots()) {
            List<HudSlot> drawn = new ArrayList<>();
            for (HudSlot slot : all) {
                if (drawnByPanel(config, slot)) {
                    drawn.add(slot);
                }
            }
            List<HudSlot> sorted = HudRenderer.sortSlots(drawn, config);
            out.addAll(sorted.size() > config.maxSlots
                    ? sorted.subList(0, config.maxSlots) : sorted);
            return out;
        }
        for (HudCluster cluster : config.clusters) {
            if (cluster == null || !cluster.enabled) {
                continue;
            }
            HudConfig view = cluster.resolve(config);
            List<HudSlot> own = new ArrayList<>();
            for (HudSlot slot : all) {
                if (config.clusterFor(slot.key()) == cluster && drawnByPanel(config, slot)) {
                    own.add(slot);
                }
            }
            List<HudSlot> sorted = HudRenderer.sortSlots(own, view);
            int cap = Math.max(0, view.maxSlots);
            out.addAll(sorted.size() > cap ? sorted.subList(0, cap) : sorted);
        }
        return out;
    }

    private static float cooldownRemaining(Minecraft minecraft, ItemStack stack) {
        if (minecraft.player == null || stack == null || stack.isEmpty()) {
            return 0.0f;
        }
        var cooldowns = minecraft.player.getCooldowns();
        Item item = stack.getItem();
        if (!cooldowns.isOnCooldown(item)) {
            return 0.0f;
        }
        return Mth.clamp(cooldowns.getCooldownPercent(item, 0.0f), 0.0f, 1.0f);
    }

    /** Mirrors the renderer: a panel draws a slot when its own resolved view keeps it. */
    private static boolean drawnByPanel(HudConfig config, HudSlot slot) {
        HudConfig view = config;
        if (config.perPanelSlots()) {
            HudCluster cluster = config.clusterFor(slot.key());
            if (cluster == null || !cluster.enabled) {
                return false;
            }
            view = cluster.resolve(config);
        }
        return view.sourceEnabled(slot.source()) && view.slotVisible(slot.key())
                && (!"vanilla".equals(slot.source()) || view.vanillaGroupEnabled(slot.group()))
                && (!slot.empty() || view.emptySlots);
    }

    /** Every known slot, including hidden and empty ones. */
    public static List<SlotView> slots() {
        return views(false);
    }

    /** Slots that the HUD is actually drawing. */
    public static List<SlotView> visibleSlots() {
        return views(true);
    }

    /** Installed source ids, vanilla first. */
    public static List<String> sources() {
        return HudData.sources().stream().map(HudData.SourceInfo::id).toList();
    }

    public static boolean hasSource(String id) {
        return HudData.hasSource(id);
    }

    // ---- Slot visibility ----
    /** The shared default selection; panels with their own selection can differ. */
    public static boolean isSlotVisible(String key) {
        return ConfigStore.get().slotVisible(key);
    }

    /** Toggles one slot in the active filter mode. Applies to every panel. */
    public static void setSlotVisible(String key, boolean visible) {
        HudConfig config = ConfigStore.get();
        config.setSlotVisible(key, visible);
        for (SlotScope scope : config.panelScopes()) {
            scope.setSlotVisible(key, visible);
        }
        ConfigStore.save();
    }

    /** Switches to {@code selected} mode and keeps only these keys, in every panel. */
    public static void showOnlySlots(Collection<String> keys) {
        HudConfig config = ConfigStore.get();
        config.showOnly(keys);
        for (SlotScope scope : config.panelScopes()) {
            scope.showOnly(keys);
        }
        ConfigStore.save();
    }

    public static void showAllSlots() {
        HudConfig config = ConfigStore.get();
        config.showAllSlots();
        for (SlotScope scope : config.panelScopes()) {
            scope.showAll();
        }
        ConfigStore.save();
    }

    /** {@code all} or {@code selected}. */
    public static void setSlotFilter(String mode) {
        HudConfig config = ConfigStore.get();
        if ("selected".equalsIgnoreCase(mode)) {
            Minecraft minecraft = Minecraft.getInstance();
            List<String> keys = new ArrayList<>();
            if (minecraft.player != null) {
                for (HudSlot slot : HudData.collectAll(minecraft, config)) {
                    if (config.sourceEnabled(slot.source()) && config.slotVisible(slot.key())
                            && (!"vanilla".equals(slot.source())
                            || config.vanillaGroupEnabled(slot.group()))) {
                        keys.add(slot.key());
                    }
                }
            }
            config.showOnly(keys);
            for (SlotScope scope : config.panelScopes()) {
                scope.showOnly(keys);
            }
        } else if ("all".equalsIgnoreCase(mode)) {
            config.showAllSlots();
            for (SlotScope scope : config.panelScopes()) {
                scope.showAll();
            }
        } else {
            return;
        }
        ConfigStore.save();
    }

    public static String slotFilter() {
        return ConfigStore.get().slotFilter;
    }

    public static void setEmptySlots(boolean enabled) {
        HudConfig config = ConfigStore.get();
        config.emptySlots = enabled;
        for (SlotScope scope : config.panelScopes()) {
            scope.emptySlots = enabled;
        }
        ConfigStore.save();
    }

    // ---- Sorting ----
    /** {@code default}, {@code name}, {@code type}, {@code source} or {@code custom}. */
    public static void setSortMode(String mode) {
        HudConfig config = ConfigStore.get();
        if (mode == null || !mode.matches("default|name|type|source|custom")) {
            return;
        }
        List<String> keys = new ArrayList<>();
        if ("custom".equals(mode)) {
            // Seed the manual order from provider order, so untouched slots keep their natural
            // position instead of being alphabetised by a previous mode.
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) {
                keys.addAll(HudData.collectAll(minecraft, config).stream()
                        .map(HudSlot::key).toList());
                config.ensureOrder(keys);
            }
        }
        config.sortMode = mode;
        for (SlotScope scope : config.panelScopes()) {
            if ("custom".equals(mode)) {
                scope.ensureOrder(keys);
            }
            scope.sortMode = mode;
        }
        ConfigStore.save();
    }

    public static String sortMode() {
        return ConfigStore.get().sortMode;
    }

    /** Replaces the manual order and switches to {@code custom}. */
    public static void setSlotOrder(List<String> keys) {
        HudConfig config = ConfigStore.get();
        config.slotOrder = keys == null ? new ArrayList<>() : new ArrayList<>(keys);
        config.sortMode = "custom";
        for (SlotScope scope : config.panelScopes()) {
            scope.order = keys == null ? new ArrayList<>() : new ArrayList<>(keys);
            scope.sortMode = "custom";
        }
        ConfigStore.save();
    }

    /** Moves one slot up or down inside the manual order. */
    public static void moveSlot(String key, int delta) {
        HudConfig config = ConfigStore.get();
        Minecraft minecraft = Minecraft.getInstance();
        List<String> keys = new ArrayList<>();
        if (minecraft.player != null) {
            keys.addAll(HudData.collectAll(minecraft, config).stream()
                    .map(HudSlot::key).toList());
            config.ensureOrder(keys);
        }
        boolean moved = config.moveSlot(key, delta);
        for (SlotScope scope : config.panelScopes()) {
            scope.ensureOrder(keys);
            moved |= scope.moveSlot(key, delta);
        }
        if (moved) {
            config.sortMode = "custom";
            for (SlotScope scope : config.panelScopes()) {
                scope.sortMode = "custom";
            }
            ConfigStore.save();
        }
    }

    // ---- Sources ----
    public static void setSourceEnabled(String sourceId, boolean enabled) {
        HudConfig config = ConfigStore.get();
        if (config.hiddenSources == null) {
            config.hiddenSources = new ArrayList<>();
        }
        if (enabled) {
            config.hiddenSources.remove(sourceId);
        } else if (!config.hiddenSources.contains(sourceId)) {
            config.hiddenSources.add(sourceId);
        }
        for (SlotScope scope : config.panelScopes()) {
            if (enabled) {
                scope.hiddenSources.remove(sourceId);
            } else if (!scope.hiddenSources.contains(sourceId)) {
                scope.hiddenSources.add(sourceId);
            }
        }
        ConfigStore.save();
    }

    public static boolean isSourceEnabled(String sourceId) {
        return ConfigStore.get().sourceEnabled(sourceId);
    }

    /** {@code armor}, {@code offhand}, {@code hotbar} or {@code inventory}. */
    public static void setVanillaGroup(String group, boolean enabled) {
        HudConfig config = ConfigStore.get();
        switch (group == null ? "" : group) {
            case "armor" -> config.vanillaArmor = enabled;
            case "offhand" -> config.vanillaOffhand = enabled;
            case "hotbar" -> config.vanillaHotbar = enabled;
            case "inventory" -> config.vanillaInventory = enabled;
            default -> {
                return;
            }
        }
        for (SlotScope scope : config.panelScopes()) {
            scope.setGroupEnabled(group, enabled);
        }
        ConfigStore.save();
    }

    /** Lets another mod contribute slots; its tab appears automatically. */
    public static boolean registerSource(AccessorySource source) {
        return HudData.registerSource(source);
    }

    /**
     * Same as above, but the integration only uses types from this package. Prefer this overload:
     * the {@link AccessorySource} one exposes internal classes and can change without notice.
     * A mod can also skip this call entirely and declare the {@link SlotSource#ENTRYPOINT}
     * entrypoint in its own {@code fabric.mod.json}; it is then registered at launch.
     */
    public static boolean registerSource(SlotSource source) {
        if (source == null || source.id() == null || source.id().isBlank()) {
            return false;
        }
        return HudData.registerSource(new com.toancao.loadouttc.compat.SlotSourceAdapter(source));
    }

    public static boolean unregisterSource(String id) {
        return HudData.unregisterSource(id);
    }

    /** Source id + display name, in the order the slot menu shows them. */
    public record SourceView(String id, String label) {
    }

    public static List<SourceView> sourceViews() {
        return HudData.sources().stream()
                .map(source -> new SourceView(source.id(), source.label()))
                .toList();
    }

    public static void openSlots() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.screen == null) {
            minecraft.setScreen(new SlotsScreen(null));
        }
    }

    /** Pins the spinning/featured effect to one slot; pass an empty string to clear it. */
    public static void setFeaturedSlot(String slotKey) {
        HudConfig config = ConfigStore.get();
        config.featuredSlot = slotKey == null ? "" : slotKey;
        ConfigStore.save();
    }

    public static String featuredSlot() {
        return ConfigStore.get().featuredSlot;
    }

    public static void setFeaturedEffect(String effectId) {
        HudConfig config = ConfigStore.get();
        if (effectId != null && HudEffects.ids().contains(effectId)) {
            config.featuredEffect = effectId;
            ConfigStore.save();
        }
    }

    // ---- Per-layer detail ----
    // Each feedback layer has its own options object. Colours use setXxxColor above; these cover
    // thickness, opacity, text format and motion, so integrations can match any HUD style.

    public static void setCooldownThickness(int pixels) {
        cooldown().thickness = clampInt(pixels, 1, 4);
        ConfigStore.save();
    }

    public static void setCooldownOpacity(int percent) {
        cooldown().opacity = clampInt(percent, 30, 100);
        ConfigStore.save();
    }

    /** {@code seconds}, {@code int} or {@code percent}. */
    public static void setCooldownText(String kind) {
        setText(cooldown(), kind);
    }

    public static void setCooldownPulse(boolean enabled) {
        cooldown().pulse = enabled;
        ConfigStore.save();
    }

    public static void setCooldownReverse(boolean enabled) {
        cooldown().reverse = enabled;
        ConfigStore.save();
    }

    public static void setCooldownTrack(boolean enabled) {
        cooldown().track = enabled;
        ConfigStore.save();
    }

    public static void setAbilityThickness(int pixels) {
        ability().thickness = clampInt(pixels, 1, 4);
        ConfigStore.save();
    }

    public static void setAbilityOpacity(int percent) {
        ability().opacity = clampInt(percent, 30, 100);
        ConfigStore.save();
    }

    /** {@code seconds}, {@code int} or {@code percent}. */
    public static void setAbilityText(String kind) {
        setText(ability(), kind);
    }

    public static void setAbilityPulse(boolean enabled) {
        ability().pulse = enabled;
        ConfigStore.save();
    }

    public static void setAbilityReverse(boolean enabled) {
        ability().reverse = enabled;
        ConfigStore.save();
    }

    public static void setAbilityTrack(boolean enabled) {
        ability().track = enabled;
        ConfigStore.save();
    }

    public static void setDurabilityThickness(int pixels) {
        durability().thickness = clampInt(pixels, 1, 4);
        ConfigStore.save();
    }

    public static void setDurabilityOpacity(int percent) {
        durability().opacity = clampInt(percent, 30, 100);
        ConfigStore.save();
    }

    public static void setDurabilityTrack(boolean enabled) {
        durability().track = enabled;
        ConfigStore.save();
    }

    private static LayerOptions cooldown() {
        HudConfig config = ConfigStore.get();
        if (config.cooldownOptions == null) {
            config.cooldownOptions = new LayerOptions();
        }
        return config.cooldownOptions;
    }

    private static LayerOptions ability() {
        HudConfig config = ConfigStore.get();
        if (config.abilityOptions == null) {
            config.abilityOptions = new LayerOptions();
        }
        return config.abilityOptions;
    }

    private static LayerOptions durability() {
        HudConfig config = ConfigStore.get();
        if (config.durabilityOptions == null) {
            config.durabilityOptions = new LayerOptions();
        }
        return config.durabilityOptions;
    }

    private static void setText(LayerOptions options, String kind) {
        if (kind == null || !kind.matches("seconds|int|percent")) {
            return;
        }
        options.text = kind;
        ConfigStore.save();
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    // ---- Per-slot settings ----
    /** Sets a permanent effect on one slot; other slots keep their own settings. */
    public static void setSlotEffect(String slotKey, String effectId) {
        if (slotKey == null || slotKey.isBlank() || effectId == null
                || !HudEffects.ids().contains(effectId)) {
            return;
        }
        HudConfig config = ConfigStore.get();
        SlotOverride override = config.overrideOrCreate(slotKey);
        if (HudConfig.LIGHT_EFFECTS.contains(effectId)) {
            override.light = effectId;
        } else {
            override.effect = effectId;
        }
        ConfigStore.save();
    }

    public static void setSlotCooldownStyle(String slotKey, String styleId) {
        if (slotKey == null || slotKey.isBlank()) {
            return;
        }
        HudConfig config = ConfigStore.get();
        config.overrideOrCreate(slotKey).cooldown = CooldownStyle.byId(styleId).id();
        ConfigStore.save();
    }

    public static void setSlotDurabilityStyle(String slotKey, String styleId) {
        if (slotKey == null || slotKey.isBlank()) {
            return;
        }
        HudConfig config = ConfigStore.get();
        config.overrideOrCreate(slotKey).durability = DurabilityStyle.byId(styleId).id();
        ConfigStore.save();
    }

    public static void clearSlotOverrides(String slotKey) {
        HudConfig config = ConfigStore.get();
        if (config.slotOverrides != null) {
            config.slotOverrides.remove(slotKey);
            ConfigStore.save();
        }
    }

    // ---- Look & feel ----
    public static List<String> styles() {
        return java.util.Arrays.stream(HudStyle.values()).map(HudStyle::id).toList();
    }

    public static void setStyle(String styleId) {
        HudConfig config = ConfigStore.get();
        config.style = HudStyle.byId(styleId).id();
        ConfigStore.save();
    }

    public static void setSize(String size) {
        String id = size == null ? "auto" : size.toLowerCase(Locale.ROOT);
        if (!SIZES.contains(id)) {
            return;
        }
        HudConfig config = ConfigStore.get();
        config.size = id;
        ConfigStore.save();
    }

    public static void setCustomScale(float scale) {
        HudConfig config = ConfigStore.get();
        config.size = "custom";
        config.scale = Math.max(0.5f, Math.min(3.0f, scale));
        ConfigStore.save();
    }

    public static void setDurabilityStyle(String styleId) {
        DurabilityStyle style = DurabilityStyle.byId(styleId);
        HudConfig config = ConfigStore.get();
        config.durabilityStyle = style.id();
        config.durability = style != DurabilityStyle.OFF;
        ConfigStore.save();
    }

    public static void setCooldownStyle(String styleId) {
        CooldownStyle style = CooldownStyle.byId(styleId);
        HudConfig config = ConfigStore.get();
        config.cooldownStyle = style.id();
        config.cooldowns = style != CooldownStyle.OFF;
        ConfigStore.save();
    }

    public static List<String> cooldownStyles() {
        return java.util.Arrays.stream(CooldownStyle.values()).map(CooldownStyle::id).toList();
    }

    /** {@code vertical}, {@code horizontal}, {@code grid} or {@code circle}. */
    public static void setLayoutMode(String mode) {
        if (mode == null || !mode.matches("vertical|horizontal|grid|circle")) {
            return;
        }
        HudConfig config = ConfigStore.get();
        config.layoutMode = mode;
        config.horizontal = "horizontal".equals(mode);
        ConfigStore.save();
    }

    public static String layoutMode() {
        return ConfigStore.get().layoutKind();
    }

    /** Columns used by the grid layout, 1..8. */
    public static void setColumns(int columns) {
        HudConfig config = ConfigStore.get();
        config.columns = Math.max(1, Math.min(8, columns));
        ConfigStore.save();
    }

    /** Radius in GUI pixels used by the circle layout, 16..200. */
    public static void setCircleRadius(int radius) {
        HudConfig config = ConfigStore.get();
        config.circleRadius = Math.max(16, Math.min(200, radius));
        ConfigStore.save();
    }

    public static void setEnabled(boolean enabled) {
        HudConfig config = ConfigStore.get();
        config.enabled = enabled;
        ConfigStore.save();
    }

    public static void setAccent(int argb) {
        HudConfig config = ConfigStore.get();
        config.accent = String.format(Locale.ROOT, "%06X", argb & 0xFFFFFF);
        ConfigStore.save();
    }

    public static void openEditor() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.screen == null) {
            minecraft.setScreen(new HudEditorScreen(null));
        }
    }

    /** Reloads {@code loadouttc.json}; useful after another mod edits the file. */
    public static void reload() {
        ConfigStore.load();
    }

    // ---- Ability timer (the part vanilla has no equivalent for) ----
    /**
     * Shows "ability in use" for one slot. The HUD draws a countdown in the style set by
     * {@link #setAbilityStyle(String)}. Call it again to restart, or {@link #hideAbility(String)}.
     */
    public static void showAbility(String slotKey, long durationMillis) {
        AbilityTimers.show(slotKey, durationMillis, 0);
    }

    /** Same as above with an explicit ARGB colour; 0 falls back to the configured accent. */
    public static void showAbility(String slotKey, long durationMillis, int argb) {
        AbilityTimers.show(slotKey, durationMillis, argb);
    }

    public static void hideAbility(String slotKey) {
        AbilityTimers.hide(slotKey);
    }

    public static void clearAbilities() {
        AbilityTimers.clear();
    }

    /** {@code off | ring | bar | text | frame}. */
    public static void setAbilityStyle(String styleId) {
        HudConfig config = ConfigStore.get();
        config.abilityStyle = AbilityStyle.byId(styleId).id();
        ConfigStore.save();
    }

    // ---- Panels (clusters) ----
    // A cluster is one independent HUD panel: its own slots, its own position, optionally its own
    // look. A fresh config has a single catch-all cluster, which behaves exactly like the old
    // one-panel HUD, so existing integrations do not need to change anything.

    /** Ids of every panel, in draw order. */
    public static List<String> clusters() {
        List<String> out = new ArrayList<>();
        for (HudCluster cluster : ConfigStore.get().clusters) {
            out.add(cluster.id);
        }
        return out;
    }

    /** Panel that currently draws a slot key, or an empty string. */
    public static String clusterOf(String slotKey) {
        return ConfigStore.get().clusterIdFor(slotKey);
    }

    /** Slots explicitly owned by a panel; empty means it is the catch-all panel. */
    public static List<String> clusterSlots(String clusterId) {
        HudCluster cluster = ConfigStore.get().cluster(clusterId);
        return cluster == null ? List.of() : List.copyOf(cluster.slots);
    }

    /** Creates an empty panel and returns its id, or an empty string when it could not be added. */
    public static String createCluster(String name) {
        HudCluster cluster = ConfigStore.get().addCluster(name);
        if (cluster == null) {
            return "";
        }
        ConfigStore.save();
        return cluster.id;
    }

    /** Moves slots into a new panel in one call: the "split" action of the editor. */
    public static String splitToCluster(String name, Collection<String> slotKeys) {
        HudConfig config = ConfigStore.get();
        HudCluster cluster = config.split(slotKeys, name);
        ConfigStore.save();
        return cluster == null ? "" : cluster.id;
    }

    /** Merges one panel into another; the absorbed panel's look can become the standard. */
    public static boolean mergeClusters(String sourceId, String destId, boolean takeSourceLook) {
        if (!ConfigStore.get().mergeInto(sourceId, destId, takeSourceLook)) {
            return false;
        }
        ConfigStore.save();
        return true;
    }

    /** Removes a panel and switches off the slots it was drawing instead of merging them away. */
    public static boolean removeCluster(String clusterId) {
        HudConfig config = ConfigStore.get();
        HudCluster cluster = config.cluster(clusterId);
        if (cluster == null) {
            return false;
        }
        List<String> keys = new ArrayList<>();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null && minecraft.player != null) {
            for (HudSlot slot : HudData.collectAll(minecraft, config)) {
                keys.add(slot.key());
            }
        } else if (cluster.slots != null) {
            keys.addAll(cluster.slots);
        }
        if (!config.removeCluster(clusterId, keys)) {
            return false;
        }
        ConfigStore.save();
        return true;
    }

    public static boolean renameCluster(String clusterId, String name) {
        HudCluster cluster = ConfigStore.get().cluster(clusterId);
        if (cluster == null || name == null || name.isBlank()) {
            return false;
        }
        cluster.name = name.length() > 20 ? name.substring(0, 20) : name;
        ConfigStore.save();
        return true;
    }

    public static void setClusterEnabled(String clusterId, boolean enabled) {
        HudCluster cluster = ConfigStore.get().cluster(clusterId);
        if (cluster != null) {
            cluster.enabled = enabled;
            ConfigStore.save();
        }
    }

    /** Puts one slot into a panel; a blank id leaves it to the catch-all. */
    public static void assignSlotToCluster(String slotKey, String clusterId) {
        ConfigStore.get().assign(slotKey, clusterId);
        ConfigStore.save();
    }

    /** Screen rectangle {@code x, y, w, h} of a panel, or all zeroes when it is not drawn. */
    public static int[] clusterBounds(String clusterId) {
        return HudRenderer.bounds(clusterId);
    }

    /** Union of every panel. Use it to place another overlay without overlapping the HUD. */
    public static int[] hudBounds() {
        return HudRenderer.hudBounds();
    }

    /** Screen rectangle of one slot, or all zeroes when it is not drawn. */
    public static int[] slotBounds(String slotKey) {
        return HudRenderer.slotBounds(slotKey);
    }

    /** Mod version as Fabric reports it, e.g. {@code 1.0.0+1.21.1}. */
    public static String version() {
        return com.toancao.loadouttc.platform.PlatformHolder.get().modVersion();
    }

    /**
     * API contract version. It only goes up when a method changes meaning or is removed, so an
     * integration can call {@code if (LoadoutTcApi.apiVersion() >= 3)} before using a new method.
     * Version 4 added the {@link SlotSource}/{@link ApiSlot} types and the extra
     * {@link SlotView} fields.
     */
    public static int apiVersion() {
        return 4;
    }

    static {
        LoadoutTcClient.LOG.debug("Loadout TC API loaded");
    }
}
