package com.toancao.loadouttc.compat;

import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import com.toancao.loadouttc.config.HudConfig;
import io.wispforest.accessories.api.slot.SlotType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Reads the equipped inventory exposed by wisp-forest's Accessories. */
public final class AccessoriesSource implements AccessorySource {
    @Override
    public String id() {
        return "accessories";
    }

    @Override
    public String label() {
        return "Accessories";
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public void collect(Minecraft minecraft, Player player, HudConfig config, boolean includeEmpty,
                        List<HudSlot> out) {
        AccessoriesCapability capability = AccessoriesCapability.getOptionally(player).orElse(null);
        if (capability == null) {
            return;
        }

        List<AccessoriesContainer> containers = new ArrayList<>(capability.getContainers().values());
        containers.sort(Comparator
                .comparingInt((AccessoriesContainer container) -> order(container))
                .thenComparing(AccessoriesContainer::getSlotName));

        for (AccessoriesContainer container : containers) {
            String group = container.getSlotName();
            String label = groupLabel(container);
            int size = Math.max(0, Math.min(container.getSize(),
                    container.getAccessories().getContainerSize()));
            for (int index = 0; index < size; index++) {
                ItemStack primary = container.getAccessories().getItem(index);
                ItemStack cosmetic = container.getCosmeticAccessories().getItem(index);
                ItemStack stack = config.showCosmetic && !cosmetic.isEmpty() ? cosmetic : primary;
                if (stack.isEmpty() && !includeEmpty) {
                    continue;
                }
                out.add(new HudSlot(id(), group, label, group, index, stack));
            }
        }
    }

    private static int order(AccessoriesContainer container) {
        SlotType type = container.slotType();
        return type == null ? Integer.MAX_VALUE : type.order();
    }

    private static String groupLabel(AccessoriesContainer container) {
        try {
            SlotType type = container.slotType();
            if (type != null && type.translation() != null) {
                String key = type.translation();
                String translated = Component.translatable(key).getString();
                if (!translated.isBlank() && !translated.equals(key)
                        && !translated.startsWith("accessories.")) {
                    return translated;
                }
            }
        } catch (RuntimeException ignored) {
            // Slot type data may not be ready during the first frames of a world join.
        }
        return friendly(container.getSlotName());
    }

    private static String friendly(String id) {
        if (id == null || id.isBlank()) {
            return "Accessory";
        }
        String text = id.replace('_', ' ').replace('/', ' ').replace(':', ' ').trim();
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
