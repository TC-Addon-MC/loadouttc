package com.toancao.loadouttc.platform;

import java.nio.file.Path;
import java.util.List;
import net.minecraft.client.KeyMapping;

/**
 * Everything {@code common/src} needs from the mod loader, and nothing else.
 *
 * <p>Each platform module installs one implementation into {@link PlatformHolder} before
 * {@code LoadoutTcClient.init()} runs. Keep this interface small: every method added here has to
 * be implemented twice, and a large surface is how a "common" source set stops being common.
 */
public interface Platform {

    /** {@code "fabric"} or {@code "neoforge"} — used for diagnostics and provider selection. */
    String name();

    /** Mod version as the loader reports it, e.g. {@code 1.0.0+1.21.1}. */
    String modVersion();

    boolean isModLoaded(String modId);

    Path configDir();

    /** Hands the key to the loader so it appears in the vanilla controls screen. */
    KeyMapping registerKeyMapping(KeyMapping mapping);

    void onClientTick(Runnable callback);

    /** Draws the HUD. Coordinates are handled by the renderer, so only the surface is needed. */
    void onHudRender(HudSink sink);

    void onShutdown(Runnable callback);

    /**
     * Objects that another mod registered under {@code key}. Fabric has real entrypoints; other
     * loaders return an empty list and their mods call {@code LoadoutTcApi} directly instead.
     */
    List<ModEntrypoint> entrypoints(String key);

    /** One object published under an entrypoint key, plus the mod that published it. */
    record ModEntrypoint(String modId, Object value) {
    }
}
