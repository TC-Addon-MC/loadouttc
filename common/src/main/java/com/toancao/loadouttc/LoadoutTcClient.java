package com.toancao.loadouttc;

import com.mojang.blaze3d.platform.InputConstants;
import com.toancao.loadouttc.compat.HudData;
import com.toancao.loadouttc.compat.HudSlot;
import com.toancao.loadouttc.config.ConfigStore;
import com.toancao.loadouttc.config.HudConfig;
import com.toancao.loadouttc.gui.HudEditorScreen;
import com.toancao.loadouttc.hud.HudRenderer;
import com.toancao.loadouttc.platform.Platform;
import com.toancao.loadouttc.platform.PlatformHolder;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loader-neutral client bootstrap. Each platform module installs its {@link Platform} and then
 * calls {@link #init()}; nothing in here knows which loader is running.
 */
public final class LoadoutTcClient {
    public static final String MOD_ID = "loadouttc";
    public static final Logger LOG = LoggerFactory.getLogger("LoadoutTC");

    private static KeyMapping openEditor;

    private LoadoutTcClient() {
    }

    public static void init() {
        Platform platform = PlatformHolder.get();

        ConfigStore.load();
        HudData.initialise();

        // One key, one job: the rest of the mod's actions are buttons inside the menu it opens.
        openEditor = platform.registerKeyMapping(new KeyMapping(
                "key.loadouttc.editor",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "key.categories.loadouttc"));

        platform.onClientTick(() -> {
            ConfigStore.tick();
            Minecraft client = Minecraft.getInstance();
            while (openEditor.consumeClick()) {
                if (client.screen == null && client.player != null) {
                    client.setScreen(new HudEditorScreen(null));
                }
            }
        });

        platform.onHudRender(graphics -> {
            Minecraft client = Minecraft.getInstance();
            HudConfig config = ConfigStore.get();
            if (!config.enabled || client.player == null || client.options.hideGui
                    || client.screen != null) {
                return;
            }
            // Every known slot: each panel filters with its own selection inside the renderer.
            List<HudSlot> slots = HudData.collectAll(client, config);
            if (slots.isEmpty()) {
                return;
            }
            HudRenderer.render(graphics, slots, config, false, 0, 0);
        });

        platform.onShutdown(ConfigStore::flush);

        LOG.info("Loadout TC ready on {} - press H in game to edit", platform.name());
    }
}
