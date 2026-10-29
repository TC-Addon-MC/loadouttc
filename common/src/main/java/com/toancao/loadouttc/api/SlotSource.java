package com.toancao.loadouttc.api;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/**
 * A slot source other mods can contribute, using only types from this package.
 *
 * <p>Register it with {@link LoadoutTcApi#registerSource(SlotSource)}; its tab then appears in the
 * slot menu and its keys look like {@code yourmod/ring/0}.
 */
public interface SlotSource {
    /**
     * Fabric entrypoint key. A mod that declares this entrypoint in its own {@code fabric.mod.json}
     * is picked up automatically at launch, with no change on the Loadout TC side:
     * {@code "entrypoints": {"loadouttc:slot_source": ["com.example.MySource"]}}.
     */
    String ENTRYPOINT = "loadouttc:slot_source";

    String id();

    /** Human-readable name shown as the slot menu tab; defaults to the id. */
    default String label() {
        return id();
    }

    boolean available();

    /**
     * Emits this source's slots in display order.
     *
     * @param includeEmpty when true, also emit slots whose stack is empty (ghost frames)
     */
    void collect(Minecraft minecraft, Player player, boolean includeEmpty, List<ApiSlot> out);
}
