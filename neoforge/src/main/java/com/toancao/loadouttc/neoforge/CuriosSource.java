package com.toancao.loadouttc.neoforge;

import com.toancao.loadouttc.compat.AccessorySource;
import com.toancao.loadouttc.compat.HudSlot;
import com.toancao.loadouttc.config.HudConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

/**
 * Curios through its real API.
 *
 * <p>The Fabric module keeps a reflective copy of this class because Curios has no Fabric build
 * for 1.21.1. Here the API is a compile-time dependency, so the reflection is gone: a rename in
 * Curios now fails the build instead of failing silently at runtime.
 */
public final class CuriosSource implements AccessorySource {

    @Override
    public String id() {
        return "curios";
    }

    @Override
    public String label() {
        return "Curios";
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public void collect(Minecraft minecraft, Player player, HudConfig config, boolean includeEmpty,
                        List<HudSlot> out) {
        ICuriosItemHandler handler = CuriosApi.getCuriosInventory(player).orElse(null);
        if (handler == null) {
            return;
        }

        List<Map.Entry<String, ICurioStacksHandler>> entries =
                new ArrayList<>(handler.getCurios().entrySet());
        entries.sort(Map.Entry.comparingByKey());

        for (Map.Entry<String, ICurioStacksHandler> entry : entries) {
            ICurioStacksHandler stacksHandler = entry.getValue();
            if (stacksHandler == null || !stacksHandler.isVisible()) {
                continue;
            }

            String group = entry.getKey();
            String label = friendly(group);

            IDynamicStackHandler stacks = stacksHandler.getStacks();
            if (stacks == null) {
                continue;
            }
            int size = stacksHandler.getSlots();
            IDynamicStackHandler cosmetics = config.showCosmetic && stacksHandler.hasCosmetic()
                    ? stacksHandler.getCosmeticStacks() : null;

            for (int index = 0; index < size; index++) {
                ItemStack stack = stacks.getStackInSlot(index);
                if (cosmetics != null) {
                    ItemStack cosmetic = cosmetics.getStackInSlot(index);
                    if (cosmetic != null && !cosmetic.isEmpty()) {
                        stack = cosmetic;
                    }
                }
                if (stack == null || (stack.isEmpty() && !includeEmpty)) {
                    continue;
                }
                out.add(new HudSlot(id(), group, label, group, index, stack));
            }
        }
    }

    private static String friendly(String id) {
        if (id == null || id.isBlank()) {
            return "Curio";
        }
        String text = id.replace('_', ' ').replace('/', ' ').trim();
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
