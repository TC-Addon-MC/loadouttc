package com.toancao.loadouttc.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * One panel's slot selection: which slots the panel draws, from which sources, in what order.
 *
 * <p>{@link HudConfig} keeps the same fields as the defaults a newly split panel starts from. Once
 * a config has more than one panel, every panel keeps its own copy here, so ticking a slot while
 * editing Panel 1 can never change what Panel 2 shows.
 */
public final class SlotScope {
    public String filter = "all";
    public List<String> hidden = new ArrayList<>();
    public List<String> visible = new ArrayList<>();
    public List<String> hiddenSources = new ArrayList<>();
    // Matches a fresh {@link HudConfig}: accessory slots only.
    public boolean vanillaArmor = false;
    public boolean vanillaOffhand = false;
    public boolean vanillaHotbar = false;
    public boolean vanillaInventory = false;
    public boolean emptySlots = false;
    public String sortMode = "default";
    public List<String> order = new ArrayList<>();

    public static SlotScope from(HudConfig cfg) {
        SlotScope scope = new SlotScope();
        if (cfg == null) {
            return scope;
        }
        scope.filter = cfg.slotFilter;
        scope.hidden = copy(cfg.hiddenSlots);
        scope.visible = copy(cfg.visibleSlots);
        scope.hiddenSources = copy(cfg.hiddenSources);
        scope.vanillaArmor = cfg.vanillaArmor;
        scope.vanillaOffhand = cfg.vanillaOffhand;
        scope.vanillaHotbar = cfg.vanillaHotbar;
        scope.vanillaInventory = cfg.vanillaInventory;
        scope.emptySlots = cfg.emptySlots;
        scope.sortMode = cfg.sortMode;
        scope.order = copy(cfg.slotOrder);
        return scope;
    }

    /** Writes this selection into a resolver view, overriding the shared defaults. */
    public void applyTo(HudConfig view) {
        view.slotFilter = filter == null ? "all" : filter;
        view.hiddenSlots = copy(hidden);
        view.visibleSlots = copy(visible);
        view.hiddenSources = copy(hiddenSources);
        view.vanillaArmor = vanillaArmor;
        view.vanillaOffhand = vanillaOffhand;
        view.vanillaHotbar = vanillaHotbar;
        view.vanillaInventory = vanillaInventory;
        view.emptySlots = emptySlots;
        view.sortMode = sortMode == null ? "default" : sortMode;
        view.slotOrder = copy(order);
    }

    public boolean sourceEnabled(String sourceId) {
        return sourceId != null && !hiddenSources.contains(sourceId);
    }

    public boolean slotVisible(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        if ("selected".equals(filter)) {
            return visible.contains(key);
        }
        return !hidden.contains(key);
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

    public void setGroupEnabled(String group, boolean enabled) {
        switch (group == null ? "" : group) {
            case "armor" -> vanillaArmor = enabled;
            case "offhand" -> vanillaOffhand = enabled;
            case "hotbar" -> vanillaHotbar = enabled;
            case "inventory" -> vanillaInventory = enabled;
            default -> {
            }
        }
    }

    public void setSlotVisible(String key, boolean visible) {
        if (key == null || key.isBlank()) {
            return;
        }
        if ("selected".equals(filter)) {
            if (visible) {
                if (!this.visible.contains(key)) {
                    this.visible.add(key);
                }
            } else {
                this.visible.remove(key);
            }
            return;
        }
        if (visible) {
            hidden.remove(key);
        } else if (!hidden.contains(key)) {
            hidden.add(key);
        }
    }

    /** Switches a slot off in every filter mode, unlike {@link #setSlotVisible} in "show only". */
    public void hide(String key) {
        if (key == null || key.isBlank()) return;
        if (hidden == null) hidden = new ArrayList<>();
        if (!hidden.contains(key)) hidden.add(key);
        if (visible != null) visible.remove(key);
    }

    public void showAll() {
        filter = "all";
        hidden = new ArrayList<>();
        visible = new ArrayList<>();
    }

    public void showOnly(Collection<String> keys) {
        filter = "selected";
        visible = keys == null ? new ArrayList<>() : clean(keys);
        hidden = new ArrayList<>();
    }

    public void switchFilter(Collection<String> currentlyVisible) {
        if ("selected".equals(filter)) {
            showAll();
            return;
        }
        showOnly(currentlyVisible);
    }

    public void toggleSource(String sourceId) {
        if (sourceId == null) {
            return;
        }
        if (hiddenSources.contains(sourceId)) {
            hiddenSources.remove(sourceId);
        } else {
            hiddenSources.add(sourceId);
        }
    }

    public void ensureOrder(Collection<String> keys) {
        if (keys == null) {
            return;
        }
        for (String key : keys) {
            if (key != null && !key.isBlank() && !order.contains(key)) {
                order.add(key);
            }
        }
    }

    public int orderIndex(String key) {
        return order.indexOf(key);
    }

    public boolean moveSlot(String key, int delta) {
        if (key == null) {
            return false;
        }
        int from = order.indexOf(key);
        if (from < 0) {
            return false;
        }
        int to = Math.max(0, Math.min(order.size() - 1, from + delta));
        if (to == from) {
            return false;
        }
        order.remove(from);
        order.add(to, key);
        return true;
    }

    public SlotScope copy() {
        SlotScope out = new SlotScope();
        out.filter = filter;
        out.hidden = copy(hidden);
        out.visible = copy(visible);
        out.hiddenSources = copy(hiddenSources);
        out.vanillaArmor = vanillaArmor;
        out.vanillaOffhand = vanillaOffhand;
        out.vanillaHotbar = vanillaHotbar;
        out.vanillaInventory = vanillaInventory;
        out.emptySlots = emptySlots;
        out.sortMode = sortMode;
        out.order = copy(order);
        return out;
    }

    public void sanitize() {
        if (filter == null || !filter.matches("all|selected")) {
            filter = "all";
        }
        if (sortMode == null || !sortMode.matches("default|name|type|source|custom")) {
            sortMode = "default";
        }
        hidden = clean(hidden);
        visible = clean(visible);
        hiddenSources = clean(hiddenSources);
        order = clean(order);
    }

    private static List<String> copy(List<String> keys) {
        return keys == null ? new ArrayList<>() : new ArrayList<>(keys);
    }

    private static List<String> clean(Collection<String> keys) {
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
