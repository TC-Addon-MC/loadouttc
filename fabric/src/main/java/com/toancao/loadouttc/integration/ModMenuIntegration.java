package com.toancao.loadouttc.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.toancao.loadouttc.gui.HudEditorScreen;

/** Adds a config button to Mod Menu. Only loaded when Mod Menu is installed. */
public final class ModMenuIntegration implements com.terraformersmc.modmenu.api.ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return HudEditorScreen::new;
    }
}
