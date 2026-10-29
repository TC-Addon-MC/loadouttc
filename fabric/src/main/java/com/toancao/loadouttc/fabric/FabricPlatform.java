package com.toancao.loadouttc.fabric;

import com.toancao.loadouttc.LoadoutTcClient;
import com.toancao.loadouttc.api.SlotSource;
import com.toancao.loadouttc.compat.AccessoriesSource;
import com.toancao.loadouttc.compat.CuriosSource;
import com.toancao.loadouttc.compat.TrinketsSource;
import com.toancao.loadouttc.platform.HudSink;
import com.toancao.loadouttc.platform.Platform;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.minecraft.client.KeyMapping;

/** The five Fabric APIs the rest of the mod would otherwise depend on. */
final class FabricPlatform implements Platform {

    @Override
    public String name() {
        return "fabric";
    }

    @Override
    public String modVersion() {
        return FabricLoader.getInstance()
                .getModContainer(LoadoutTcClient.MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public KeyMapping registerKeyMapping(KeyMapping mapping) {
        return KeyBindingHelper.registerKeyBinding(mapping);
    }

    @Override
    public void onClientTick(Runnable callback) {
        ClientTickEvents.END_CLIENT_TICK.register(client -> callback.run());
    }

    @Override
    public void onHudRender(HudSink sink) {
        HudRenderCallback.EVENT.register((graphics, tickCounter) -> sink.render(graphics));
    }

    @Override
    public void onShutdown(Runnable callback) {
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> callback.run());
    }

    @Override
    public List<NamedSource> builtinSources() {
        List<NamedSource> sources = new ArrayList<>();
        // Each provider is only constructed when its mod is present, so a missing API is never
        // touched and cannot throw at class-load time.
        if (isModLoaded("accessories")) {
            sources.add(new NamedSource("Accessories", new AccessoriesSource()));
        }
        if (isModLoaded("trinkets")) {
            sources.add(new NamedSource("Trinkets", new TrinketsSource()));
        }
        if (isModLoaded("curios")) {
            sources.add(new NamedSource("Curios", new CuriosSource()));
        }
        return sources;
    }

    @Override
    public List<ModEntrypoint> entrypoints(String key) {
        List<ModEntrypoint> found = new ArrayList<>();
        try {
            for (EntrypointContainer<Object> container : FabricLoader.getInstance()
                    .getEntrypointContainers(key, Object.class)) {
                found.add(new ModEntrypoint(
                        container.getProvider().getMetadata().getId(),
                        container.getEntrypoint()));
            }
        } catch (Throwable throwable) {
            // A malformed key or a mod with a broken entrypoint must not take the HUD down.
            LoadoutTcClient.LOG.warn("Could not read entrypoints for {}", key, throwable);
        }
        return found;
    }
}
