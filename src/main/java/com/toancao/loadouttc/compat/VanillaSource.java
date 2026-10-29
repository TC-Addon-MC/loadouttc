package com.toancao.loadouttc.compat;

import com.toancao.loadouttc.config.HudConfig;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Vanilla equipment: the four armour pieces, the offhand and, opt-in, the hotbar and main
 * inventory. Armour and offhand are on by default so the HUD still reads as "what am I wearing"
 * instead of turning into a second inventory.
 */
public final class VanillaSource implements AccessorySource {
    /** Armour list indices in vanilla order: feet, legs, chest, head. */
    private static final String[][] ARMOUR = {
            {"boots", "Boots"},
            {"leggings", "Leggings"},
            {"chestplate", "Chestplate"},
            {"helmet", "Helmet"}
    };

    @Override
    public String id() {
        return "vanilla";
    }

    @Override
    public String label() {
        return "Vanilla";
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public void collect(Minecraft minecraft, Player player, HudConfig config, boolean includeEmpty,
                        List<HudSlot> out) {
        var inventory = player.getInventory();

        // Head first, feet last: that is the order the armour screen shows.
        for (int i = ARMOUR.length - 1; i >= 0; i--) {
            ItemStack stack = inventory.armor.get(i);
            if (stack.isEmpty() && !includeEmpty) {
                continue;
            }
            out.add(new HudSlot(id(), "armor", "Armor", ARMOUR[i][0], 0, stack));
        }

        ItemStack offhand = player.getOffhandItem();
        if (!offhand.isEmpty() || includeEmpty) {
            out.add(new HudSlot(id(), "offhand", "Offhand", "offhand", 0, offhand));
        }

        // Emit every group; HudData applies the on/off flags so the slot menu can list a group
        // and switch it on later.
        addRange(inventory.items, 0, 9, "hotbar", "Hotbar", "hotbar", includeEmpty, out);
        addRange(inventory.items, 9, 36, "inventory", "Inventory", "inventory", includeEmpty, out);
    }

    private void addRange(List<ItemStack> items, int from, int to, String group, String label,
                          String slotId, boolean includeEmpty, List<HudSlot> out) {
        for (int index = from; index < Math.min(to, items.size()); index++) {
            ItemStack stack = items.get(index);
            if (stack.isEmpty() && !includeEmpty) {
                continue;
            }
            out.add(new HudSlot(id(), group, label, slotId, index - from, stack));
        }
    }
}
