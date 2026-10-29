package com.toancao.loadouttc.neoforge;

import com.toancao.loadouttc.LoadoutTcClient;
import com.toancao.loadouttc.gui.HudEditorScreen;
import com.toancao.loadouttc.platform.HudSink;
import com.toancao.loadouttc.platform.Platform;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.GameShuttingDownEvent;

/** NeoForge side of the {@link Platform} seam. */
final class NeoForgePlatform implements Platform {

    private final ModContainer container;

    private KeyMapping keyMapping;
    private Runnable tickCallback;
    private Runnable shutdownCallback;
    private HudSink hudSink;

    NeoForgePlatform(ModContainer container) {
        this.container = container;

        // NeoForge replaces Mod Menu with a built-in extension point: the mod list gets a
        // "Config" button that opens the same editor the key opens.
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (modContainer, parent) -> new HudEditorScreen(parent));
    }

    @Override
    public String name() {
        return "neoforge";
    }

    @Override
    public String modVersion() {
        return container.getModInfo().getVersion().toString();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public KeyMapping registerKeyMapping(KeyMapping mapping) {
        // Deferred: the event below fires later, when NeoForge is ready to accept key mappings.
        this.keyMapping = mapping;
        return mapping;
    }

    @Override
    public void onClientTick(Runnable callback) {
        this.tickCallback = callback;
    }

    @Override
    public void onHudRender(HudSink sink) {
        this.hudSink = sink;
    }

    @Override
    public void onShutdown(Runnable callback) {
        this.shutdownCallback = callback;
    }

    @Override
    public List<ModEntrypoint> entrypoints(String key) {
        // NeoForge has no entrypoint system; its mods call LoadoutTcApi directly instead.
        return Collections.emptyList();
    }

    @Override
    public List<NamedSource> builtinSources() {
        // Phase 3: native Curios, then Accessories.
        return Collections.emptyList();
    }

    // ---- event handlers ----

    void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        if (keyMapping != null) {
            event.register(keyMapping);
        }
    }

    void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        if (hudSink == null) {
            return;
        }
        // Right above the hotbar: high enough not to be covered by it, low enough that chat,
        // the scoreboard and the title screen still draw on top.
        event.registerAbove(VanillaGuiLayers.HOTBAR,
                ResourceLocation.fromNamespaceAndPath(LoadoutTcClient.MOD_ID, "hud"),
                (graphics, deltaTracker) -> hudSink.render(graphics));
    }

    void handleClientTick(ClientTickEvent.Post event) {
        if (tickCallback != null) {
            tickCallback.run();
        }
    }

    void handleGameShuttingDown(GameShuttingDownEvent event) {
        if (shutdownCallback != null) {
            shutdownCallback.run();
        }
    }
}
