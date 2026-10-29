package com.toancao.loadouttc.api;

import net.minecraft.world.item.ItemStack;

/**
 * Context handed to a {@link SlotEffect}. {@code progress} runs from 0 to 1 for timed effects and
 * stays at 1 for permanent/featured effects. All methods are called on the client render thread.
 */
public record EffectContext(String slotKey, ItemStack stack, float seconds, float progress,
                            boolean featured, boolean hovered, int accentColor) {
}
