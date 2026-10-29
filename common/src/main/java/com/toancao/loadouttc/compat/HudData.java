package com.toancao.loadouttc.compat;

import com.toancao.loadouttc.LoadoutTcClient;
import com.toancao.loadouttc.api.SlotSource;
import com.toancao.loadouttc.config.HudConfig;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.toancao.loadouttc.platform.Platform;
import com.toancao.loadouttc.platform.PlatformHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Registry of every slot source, plus the filtering that decides what the HUD draws. */
public final class HudData {
    private static final Map<String, AccessorySource> SOURCES = new LinkedHashMap<>();
    private static boolean initialised;
    /** Provider output for the current client tick; see {@code raw(...)}. */
    private static List<HudSlot> rawCache;
    private static long rawTick = Long.MIN_VALUE;
    private static Player rawPlayer;

    private HudData() {
    }

    /** A source that is currently installed, as shown in the slot menu tab bar. */
    public record SourceInfo(String id, String label) {
    }

    public static synchronized void initialise() {
        if (initialised) {
            return;
        }
        initialised = true;

        registerInternal(new VanillaSource());

        if (PlatformHolder.get().isModLoaded("accessories")) {
            tryRegister(new AccessoriesSource(), "Accessories");
        }
        if (PlatformHolder.get().isModLoaded("trinkets")) {
            tryRegister(new TrinketsSource(), "Trinkets");
        }
        if (PlatformHolder.get().isModLoaded("curios")) {
            tryRegister(new CuriosSource(), "Curios");
        }

        registerEntrypoints();

        LoadoutTcClient.LOG.info("Slot sources: {}", SOURCES.isEmpty() ? "none"
                : String.join(", ", SOURCES.keySet()));
    }

    /**
     * Sources that declare the {@link SlotSource#ENTRYPOINT} entrypoint in their own mod metadata.
     * Nothing on this side has to name them, so a new equipment mod can support itself by shipping
     * one class and three lines of JSON.
     */
    private static void registerEntrypoints() {
        try {
            for (Platform.ModEntrypoint container : PlatformHolder.get()
                    .entrypoints(SlotSource.ENTRYPOINT)) {
                String modId = container.modId();
                try {
                    Object entry = container.value();
                    if (entry instanceof SlotSource source) {
                        if (source.available()) {
                            registerInternal(new SlotSourceAdapter(source));
                        }
                    } else if (entry instanceof AccessorySource source) {
                        if (source.available()) {
                            registerInternal(source);
                        }
                    } else if (entry != null) {
                        LoadoutTcClient.LOG.warn(
                                "{} entrypoint from {} is not a SlotSource",
                                SlotSource.ENTRYPOINT, modId);
                    }
                } catch (Throwable throwable) {
                    LoadoutTcClient.LOG.warn("Slot source from {} disabled", modId, throwable);
                }
            }
        } catch (Throwable throwable) {
            LoadoutTcClient.LOG.warn("Could not read slot source entrypoints", throwable);
        }
    }

    private static void tryRegister(AccessorySource source, String name) {
        try {
            if (source.available()) {
                registerInternal(source);
            }
        } catch (Throwable throwable) {
            LoadoutTcClient.LOG.warn("{} integration disabled", name, throwable);
        }
    }

    private static void registerInternal(AccessorySource source) {
        if (source == null || source.id() == null || source.id().isBlank()) {
            return;
        }
        SOURCES.put(source.id(), source);
    }

    /**
     * Registers (or replaces) a slot source. Intended for other mods; the built-in providers call
     * this through {@link #initialise()}. Returns false for a blank id or a null source.
     */
    public static synchronized boolean registerSource(AccessorySource source) {
        initialise();
        if (source == null || source.id() == null || source.id().isBlank()) {
            return false;
        }
        SOURCES.put(source.id(), source);
        rawCache = null;
        return true;
    }

    /** Removes a source. Built-in providers can be removed too; they come back on next launch. */
    public static synchronized boolean unregisterSource(String id) {
        initialise();
        rawCache = null;
        return id != null && SOURCES.remove(id) != null;
    }

    /** Every installed source, in tab order: vanilla first, then mod providers. */
    public static List<SourceInfo> sources() {
        initialise();
        List<SourceInfo> out = new ArrayList<>();
        for (AccessorySource source : SOURCES.values()) {
            out.add(new SourceInfo(source.id(), source.label()));
        }
        return out;
    }

    public static boolean hasSource(String id) {
        initialise();
        return id != null && SOURCES.containsKey(id);
    }

    /** True when at least one equipment mod (not vanilla) is installed. */
    public static boolean hasProvider() {
        initialise();
        return SOURCES.keySet().stream().anyMatch(id -> !"vanilla".equals(id));
    }

    /**
     * Slots the HUD should draw: source toggles, the slot filter and empty-slot preference applied.
     * Sorting and the {@code maxSlots} cap happen in the renderer, after this.
     */
    public static List<HudSlot> collect(Minecraft minecraft, HudConfig config) {
        initialise();
        List<HudSlot> out = new ArrayList<>();
        Player player = minecraft.player;
        if (player == null) {
            return out;
        }
        for (HudSlot slot : raw(minecraft, config)) {
            if (!config.sourceEnabled(slot.source()) || !config.slotVisible(slot.key())) {
                continue;
            }
            if ("vanilla".equals(slot.source()) && !config.vanillaGroupEnabled(slot.group())) {
                continue;
            }
            if (slot.empty() && !config.emptySlots) {
                continue;
            }
            out.add(slot);
        }
        return out;
    }

    /** Cached provider output. Walking every inventory and capability once per tick is enough. */
    private static List<HudSlot> raw(Minecraft minecraft, HudConfig config) {
        Player player = minecraft.player;
        if (player == null) {
            return List.of();
        }
        long tick = minecraft.level == null ? -1L : minecraft.level.getGameTime();
        if (rawCache != null && tick == rawTick && rawPlayer == player) {
            return rawCache;
        }
        List<HudSlot> raw = new ArrayList<>();
        for (AccessorySource source : SOURCES.values()) {
            try {
                // Always enumerate empties: the cache is shared with the slot menu, which needs
                // them, and the HUD filters them out below.
                source.collect(minecraft, player, config, true, raw);
            } catch (Throwable throwable) {
                LoadoutTcClient.LOG.debug("Source {} failed", source.id(), throwable);
            }
        }
        rawCache = raw;
        rawTick = tick;
        rawPlayer = player;
        return raw;
    }

    /**
     * Every known slot, including empties and ones currently filtered out. This is what the slot
     * menu lists, so a hidden slot can be switched back on.
     */
    public static List<HudSlot> collectAll(Minecraft minecraft, HudConfig config) {
        initialise();
        List<HudSlot> out = new ArrayList<>();
        if (minecraft.player == null) {
            return out;
        }
        out.addAll(raw(minecraft, config));
        return out;
    }

    /** Placeholder slots so the editor is usable with nothing equipped. */
    public static List<HudSlot> demoSlots() {
        List<HudSlot> demo = new ArrayList<>();
        demo.add(new HudSlot("demo", "ring", "Ring", "ring", 0,
                new ItemStack(Items.DIAMOND)));
        demo.add(new HudSlot("demo", "necklace", "Necklace", "necklace", 0,
                new ItemStack(Items.EMERALD)));
        demo.add(new HudSlot("demo", "hands", "Hands", "hands", 0,
                new ItemStack(Items.GOLDEN_SWORD)));
        demo.add(new HudSlot("demo", "belt", "Belt", "belt", 0,
                new ItemStack(Items.CLOCK)));
        return demo;
    }
}
