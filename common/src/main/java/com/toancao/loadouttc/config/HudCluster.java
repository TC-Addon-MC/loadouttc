package com.toancao.loadouttc.config;

import java.util.ArrayList;
import java.util.List;

/**
 * One HUD panel. The HUD can draw several at once - hotbar gear in one corner, accessories in
 * another - and every panel owns its position, plus optionally a full copy of the look.
 *
 * <p>{@link #slots} is the membership list. A panel marked {@link #rest} also draws every visible
 * slot that no other panel claims, which is what makes a fresh config behave like the old
 * single-panel HUD; splitting is then only a matter of moving slots out of it.
 */
public final class HudCluster {
    public String id = "main";
    public String name = "Main";
    public boolean enabled = true;
    /** Slots this panel owns on top of whatever it catches. */
    public List<String> slots = new ArrayList<>();
    /** True = also draws every visible slot no panel claims. Normally exactly one panel has it. */
    public boolean rest = true;

    /** False = follow the global anchor and offset. */
    public boolean ownPosition = false;
    public String anchor = HudAnchor.TOP_RIGHT.name();
    public int x = 0;
    public int y = 0;

    /**
     * Full look snapshot; {@code null} means "follow the global settings". A snapshot is used
     * instead of a dozen nullable fields so adding a new style option never silently stops being
     * per-panel.
     */
    public HudConfig look;

    /**
     * This panel's own slot selection; {@code null} means "follow the shared defaults". Set as soon
     * as a config splits into several panels, so editing slots for one panel never edits another.
     */
    public SlotScope scope;

    /** The config the renderer should use for this panel. */
    public HudConfig resolve(HudConfig base) {
        HudConfig view = look == null ? base : look.copy();
        if (view == base) {
            // Position is per panel, so even a panel without its own look needs its own copy.
            view = base.copy();
        }
        inheritBehaviour(view, base);
        // A lone panel edits the shared slot selection; a scope from a split-out panel must not
        // shadow what the slot menu writes into the config.
        if (scope != null && base.perPanelSlots()) {
            scope.applyTo(view);
        }
        // A panel's position is always its own: no shared anchor, no "move all" side effect.
        view.anchor = HudAnchor.byName(anchor).name();
        view.x = x;
        view.y = y;
        return view;
    }

    /**
     * Behaviour that must never be frozen inside a look: which slots are shown, how they are
     * ordered, which sources are on, the accent colour and the featured slot. Only the visual
     * fields stay per panel; everything here always comes from the live config.
     */
    private static void inheritBehaviour(HudConfig view, HudConfig base) {
        view.clusters = base.clusters;
        view.activeCluster = base.activeCluster;
        view.slotOverrides = base.slotOverrides;
        view.unlockedSlots = base.unlockedSlots;
        view.enabled = base.enabled;
        view.slotFilter = base.slotFilter;
        view.hiddenSlots = base.hiddenSlots;
        view.visibleSlots = base.visibleSlots;
        view.hiddenSources = base.hiddenSources;
        view.vanillaArmor = base.vanillaArmor;
        view.vanillaOffhand = base.vanillaOffhand;
        view.vanillaHotbar = base.vanillaHotbar;
        view.vanillaInventory = base.vanillaInventory;
        view.emptySlots = base.emptySlots;
        view.sortMode = base.sortMode;
        view.slotOrder = base.slotOrder;
        view.featuredSlot = base.featuredSlot;
        view.featuredEffect = base.featuredEffect;
        view.accent = base.accent;
        view.tooltips = base.tooltips;
    }

    /** Copies the current global look into this panel. */
    public void adoptLook(HudConfig base) {
        look = base.copy();
        look.clusters = new ArrayList<>();
    }

    public void adoptPosition(HudConfig base) {
        ownPosition = true;
        anchor = base.anchor;
        x = base.x;
        y = base.y;
    }

    /** True when this panel takes the unclaimed slots. */
    public boolean catchAll() {
        return rest;
    }

    public boolean contains(String slotKey) {
        return slotKey != null && slots != null && slots.contains(slotKey);
    }

    /** Deep copy, so two panels can never share a look or a slot list. */
    public HudCluster copy() {
        HudCluster out = new HudCluster();
        out.id = id;
        out.name = name;
        out.enabled = enabled;
        out.slots = new ArrayList<>(slots == null ? List.of() : slots);
        out.rest = rest;
        out.ownPosition = ownPosition;
        out.anchor = anchor;
        out.x = x;
        out.y = y;
        if (look != null) {
            out.look = look.copy();
            out.look.clusters = new ArrayList<>();
        }
        if (scope != null) {
            out.scope = scope.copy();
        }
        return out;
    }

    public void sanitize() {
        if (id == null || !id.matches("[a-z0-9_]{1,16}")) {
            id = "c" + Math.abs(System.nanoTime() % 100000);
        }
        if (name == null || name.isBlank()) {
            name = id;
        }
        if (name.length() > 20) {
            name = name.substring(0, 20);
        }
        slots = clean(slots);
        anchor = HudAnchor.byName(anchor).name();
        if (look != null) {
            look.sanitize();
            look.clusters = new ArrayList<>();
            look.activeCluster = "";
        }
        if (scope != null) {
            scope.sanitize();
        }
    }

    private static List<String> clean(List<String> keys) {
        List<String> out = new ArrayList<>();
        if (keys == null) {
            return out;
        }
        for (String key : keys) {
            if (key == null) {
                continue;
            }
            String trimmed = key.trim();
            if (!trimmed.isEmpty() && !out.contains(trimmed)) {
                out.add(trimmed);
            }
        }
        return out;
    }
}
