package com.toancao.loadouttc;

import com.toancao.loadouttc.compat.HudData;
import com.toancao.loadouttc.compat.HudSlot;
import com.toancao.loadouttc.config.ConfigStore;
import com.toancao.loadouttc.config.HudConfig;
import com.toancao.loadouttc.gui.HudEditorScreen;
import com.toancao.loadouttc.hud.HudRenderer;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LoadoutTcClient implements ClientModInitializer {
    public static final String MOD_ID = "loadouttc";
    public static final Logger LOG = LoggerFactory.getLogger("LoadoutTC");

    private static KeyMapping openEditor;

    @Override
    public void onInitializeClient() {
        ConfigStore.load();
        HudData.initialise();

        // One key, one job: the rest of the mod's actions are buttons inside the menu it opens.
        openEditor = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.loadouttc.editor",
                com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "key.categories.loadouttc"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ConfigStore.tick();
            while (openEditor.consumeClick()) {
                if (client.screen == null && client.player != null) {
                    client.setScreen(new HudEditorScreen(null));
                }
            }
        });

        HudRenderCallback.EVENT.register((graphics, tickCounter) -> {
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

        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STOPPING
                .register(client -> ConfigStore.flush());

        LOG.info("Loadout TC ready - press H in game to edit");
    }
}
