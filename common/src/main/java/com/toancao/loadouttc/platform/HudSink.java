package com.toancao.loadouttc.platform;

import java.util.Objects;
import net.minecraft.client.gui.GuiGraphics;

/** A draw callback for the HUD, independent of which loader is asking for it. */
@FunctionalInterface
public interface HudSink {
    void render(GuiGraphics graphics);

    /** Runs two sinks in order; used by platforms that can register more than one layer. */
    static HudSink then(HudSink first, HudSink second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        return graphics -> {
            first.render(graphics);
            second.render(graphics);
        };
    }
}
