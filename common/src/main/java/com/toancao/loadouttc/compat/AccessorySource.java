package com.toancao.loadouttc.compat;

import com.toancao.loadouttc.config.HudConfig;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/** Read-only view over one slot source. Implementations are only loaded when they can work. */
public interface AccessorySource {
    String id();

    /** Human-readable name used for the slot menu tab; defaults to the id. */
    default String label() {
        return id();
    }

    boolean available();

    /**
     * Emits this source's slots in display order.
     *
     * @param includeEmpty when true, also emit slots whose stack is empty (ghost frames)
     */
    void collect(Minecraft minecraft, Player player, HudConfig config, boolean includeEmpty,
                 List<HudSlot> out);
}
