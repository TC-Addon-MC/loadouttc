package com.toancao.loadouttc.compat;

import com.toancao.loadouttc.LoadoutTcClient;
import com.toancao.loadouttc.config.HudConfig;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Curios is published for NeoForge on 1.21.1, not for Fabric. This source therefore talks to the
 * Curios API reflectively, so it compiles on Fabric and works whenever the API classes are present
 * at runtime (for example through Sinytra Connector or a Fabric port of Curios).
 */
public final class CuriosSource implements AccessorySource {
    private Method getCuriosInventory;
    private Method handlerGetCurios;
    private Method stacksGetStacks;
    private Method stacksGetIdentifier;
    private Method stacksGetSlots;
    private Method stacksIsVisible;
    private Method stacksHasCosmetic;
    private Method stacksGetCosmeticStacks;
    private Method inventoryGetStackInSlot;
    private boolean usable;

    public CuriosSource() {
        try {
            Class<?> api = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            getCuriosInventory = api.getMethod("getCuriosInventory", LivingEntity.class);

            Class<?> handler = Class.forName(
                    "top.theillusivec4.curios.api.type.capability.ICuriosItemHandler");
            handlerGetCurios = handler.getMethod("getCurios");

            Class<?> stacks = Class.forName(
                    "top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler");
            stacksGetStacks = stacks.getMethod("getStacks");
            stacksGetIdentifier = stacks.getMethod("getIdentifier");
            stacksGetSlots = stacks.getMethod("getSlots");
            stacksIsVisible = optionalMethod(stacks, "isVisible");
            stacksHasCosmetic = optionalMethod(stacks, "hasCosmetic");
            stacksGetCosmeticStacks = optionalMethod(stacks, "getCosmeticStacks");

            Class<?> inventory = Class.forName(
                    "top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler");
            inventoryGetStackInSlot = inventory.getMethod("getStackInSlot", int.class);
            usable = true;
        } catch (Throwable throwable) {
            usable = false;
            LoadoutTcClient.LOG.debug("Curios API not reachable", throwable);
        }
    }

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
        return usable;
    }

    @Override
    public void collect(Minecraft minecraft, Player player, HudConfig config, boolean includeEmpty,
                        List<HudSlot> out) {
        if (!usable) {
            return;
        }
        try {
            Object handler = invoke(getCuriosInventory, null, player);
            if (handler instanceof Optional<?> optional) {
                handler = optional.orElse(null);
            }
            if (handler == null) {
                return;
            }

            Object raw = invoke(handlerGetCurios, handler);
            if (!(raw instanceof Map<?, ?> map)) {
                return;
            }

            List<Map.Entry<?, ?>> entries = new ArrayList<>(map.entrySet());
            entries.sort(Comparator.comparing(entry -> String.valueOf(entry.getKey())));

            for (Map.Entry<?, ?> entry : entries) {
                Object stacksHandler = entry.getValue();
                if (stacksHandler == null) {
                    continue;
                }
                if (stacksIsVisible != null && Boolean.FALSE.equals(invoke(stacksIsVisible, stacksHandler))) {
                    continue;
                }

                String group = stacksGetIdentifier == null
                        ? String.valueOf(entry.getKey())
                        : String.valueOf(invoke(stacksGetIdentifier, stacksHandler));
                String label = friendly(group);

                Object stacks = invoke(stacksGetStacks, stacksHandler);
                if (stacks == null) {
                    continue;
                }
                int size = ((Number) invoke(stacksGetSlots, stacksHandler)).intValue();

                Object cosmetics = null;
                if (config.showCosmetic && stacksHasCosmetic != null && stacksGetCosmeticStacks != null
                        && Boolean.TRUE.equals(invoke(stacksHasCosmetic, stacksHandler))) {
                    cosmetics = invoke(stacksGetCosmeticStacks, stacksHandler);
                }

                for (int index = 0; index < size; index++) {
                    ItemStack stack = (ItemStack) invoke(inventoryGetStackInSlot, stacks, index);
                    if (cosmetics != null) {
                        ItemStack cosmetic = (ItemStack) invoke(inventoryGetStackInSlot, cosmetics, index);
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
        } catch (Throwable throwable) {
            // Never let a broken Curios install take down the HUD.
            LoadoutTcClient.LOG.debug("Curios read failed", throwable);
        }
    }

    private static Method optionalMethod(Class<?> owner, String name) {
        try {
            return owner.getMethod(name);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object invoke(Method method, Object target, Object... args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException exception) {
            throw exception.getCause() == null ? exception : exception.getCause();
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
