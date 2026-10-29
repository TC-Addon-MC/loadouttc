package com.toancao.loadouttc.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Shared backdrop for the full-screen menus. It dims the world (and whatever blur the game already
 * applied) just enough that labels and chips stay readable, and gives every menu the same header
 * band instead of letting content float on the world.
 */
final class GuiBackdrop {
    private GuiBackdrop() {
    }

    static void draw(GuiGraphics graphics, int width, int height) {
        // Slightly translucent so a blurred world still shows through, like a fogged glass pane.
        graphics.fillGradient(0, 0, width, height, UiTheme.BACKDROP_TOP, UiTheme.BACKDROP_BOTTOM);
        // Header band, fading into the content area.
        graphics.fillGradient(0, 0, width, UiTheme.HEADER_H, UiTheme.HEADER_BAND,
                UiTheme.HEADER_BAND_FADE);
    }

    /** Title plus one-line subtitle, at the same spot on every menu. */
    static void header(GuiGraphics graphics, Font font, String title, String subtitle) {
        graphics.drawString(font, title, UiTheme.HEADER_X, UiTheme.HEADER_TITLE_Y, UiTheme.TITLE,
                true);
        if (subtitle != null) {
            graphics.drawString(font, subtitle, UiTheme.HEADER_X, UiTheme.HEADER_SUB_Y,
                    UiTheme.SUBTITLE, false);
        }
    }
}
