package com.toancao.loadouttc.neoforge;

import com.toancao.loadouttc.LoadoutTcClient;
import com.toancao.loadouttc.platform.PlatformHolder;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

/**
 * NeoForge entrypoint. Mod-bus events are wired here because they must be registered from the
 * mod constructor, while the game-bus ones can be added as soon as the callbacks exist.
 */
@Mod(value = LoadoutTcClient.MOD_ID, dist = Dist.CLIENT)
public final class LoadoutTcNeoForge {

    public LoadoutTcNeoForge(IEventBus modBus, ModContainer container) {
        NeoForgePlatform platform = new NeoForgePlatform(container);
        PlatformHolder.install(platform);

        // install() runs first so the core can ask the platform for keys, ticks and draw hooks.
        LoadoutTcClient.init();

        modBus.addListener(platform::onRegisterKeyMappings);
        modBus.addListener(platform::onRegisterGuiLayers);

        NeoForge.EVENT_BUS.addListener(platform::handleClientTick);
        NeoForge.EVENT_BUS.addListener(platform::handleGameShuttingDown);
    }
}
