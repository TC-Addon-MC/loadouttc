package com.toancao.loadouttc.api;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/**
 * One slot a mod contributes through {@link SlotSource}. Same fields as the internal slot record,
 * but this one lives in the API package so integrations never import mod internals.
 */
public record ApiSlot(String source, String group, String groupLabel, String slot, int index,
                      ItemStack stack) {
    public ApiSlot {
        stack = stack == null ? ItemStack.EMPTY : stack;
    }

    /**
     * Config/API key: {@code source/slot/index} when group and slot share a name, otherwise
     * {@code source/group/slot/index}. Must match what the HUD stores for its own slots.
     */
    public String key() {
        String base = source + "/";
        if (group != null && !group.isBlank() && !group.equals(slot)) {
            base += group + "/";
        }
        return base + slot + "/" + index;
    }

    public boolean empty() {
        return stack.isEmpty();
    }

    public String itemId() {
        return empty() ? "" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }
}
