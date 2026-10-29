package com.toancao.loadouttc.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/** One equipped (or empty) accessory slot, normalised across the supported mods. */
public record HudSlot(String source, String group, String groupLabel, String slot, int index,
                      ItemStack stack) {
    /**
     * Stable key for config, overrides and the API.
     *
     * <p>{@code source/slot/index} when the group and the slot share a name (Accessories rings,
     * vanilla hotbar), otherwise {@code source/group/slot/index} — Trinkets reports a group and a
     * slot name separately, and two groups can hold slots with the same name.
     */
    public String key() {
        String base = source + "/";
        if (group != null && !group.isBlank() && !group.equals(slot)) {
            base += group + "/";
        }
        return base + slot + "/" + index;
    }

    public boolean empty() {
        return stack == null || stack.isEmpty();
    }

    public String itemId() {
        return empty() ? "" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }
}
