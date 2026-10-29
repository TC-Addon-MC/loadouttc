package com.toancao.loadouttc.compat;

import com.toancao.loadouttc.config.HudConfig;
import dev.emi.trinkets.api.SlotType;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Reads the equipped inventory exposed by Trinkets. */
public final class TrinketsSource implements AccessorySource {
    @Override
    public String id() {
        return "trinkets";
    }

    @Override
    public String label() {
        return "Trinkets";
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public void collect(Minecraft minecraft, Player player, HudConfig config, boolean includeEmpty,
                        List<HudSlot> out) {
        TrinketComponent component = TrinketsApi.getTrinketComponent(player).orElse(null);
        if (component == null) {
            return;
        }

        List<Map.Entry<String, Map<String, TrinketInventory>>> groups =
                new ArrayList<>(component.getInventory().entrySet());
        groups.sort(Comparator.comparing(Map.Entry::getKey));

        for (Map.Entry<String, Map<String, TrinketInventory>> groupEntry : groups) {
            List<Map.Entry<String, TrinketInventory>> inventories =
                    new ArrayList<>(groupEntry.getValue().entrySet());
            inventories.sort(Comparator
                    .comparingInt((Map.Entry<String, TrinketInventory> entry) -> order(entry.getValue()))
                    .thenComparing(Map.Entry::getKey));

            for (Map.Entry<String, TrinketInventory> entry : inventories) {
                TrinketInventory inventory = entry.getValue();
                SlotType type = inventory.getSlotType();
                String group = type == null
                        ? groupEntry.getKey()
                        : type.getGroup() + "/" + type.getName();
                String label = label(groupEntry.getKey(), type);

                int size = Math.max(0, inventory.getContainerSize());
                for (int index = 0; index < size; index++) {
                    ItemStack stack = inventory.getItem(index);
                    if (stack.isEmpty() && !includeEmpty) {
                        continue;
                    }
                    out.add(new HudSlot(id(), group, label, group, index, stack));
                }
            }
        }
    }

    private static int order(TrinketInventory inventory) {
        SlotType type = inventory.getSlotType();
        return type == null ? Integer.MAX_VALUE : type.getOrder();
    }

    private static String label(String group, SlotType type) {
        if (type != null && type.getTranslation() != null) {
            String text = type.getTranslation().getString();
            if (!text.isBlank()) {
                return text;
            }
        }
        return group;
    }
}
