package com.toancao.loadouttc.compat;

import com.toancao.loadouttc.api.ApiSlot;
import com.toancao.loadouttc.api.SlotSource;
import com.toancao.loadouttc.config.HudConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/** Bridges the public {@link SlotSource} onto the internal provider interface. */
public final class SlotSourceAdapter implements AccessorySource {
    private final SlotSource source;

    public SlotSourceAdapter(SlotSource source) {
        this.source = source;
    }

    @Override
    public String id() {
        return source.id();
    }

    @Override
    public String label() {
        return source.label();
    }

    @Override
    public boolean available() {
        return source.available();
    }

    @Override
    public void collect(Minecraft minecraft, Player player, HudConfig config,
                        boolean includeEmpty, List<HudSlot> out) {
        List<ApiSlot> slots = new ArrayList<>();
        source.collect(minecraft, player, includeEmpty, slots);
        for (ApiSlot slot : slots) {
            if (slot == null) {
                continue;
            }
            out.add(new HudSlot(slot.source(), slot.group(), slot.groupLabel(),
                    slot.slot(), slot.index(), slot.stack()));
        }
    }
}
