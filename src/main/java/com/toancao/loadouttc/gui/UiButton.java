package com.toancao.loadouttc.gui;

import com.toancao.loadouttc.util.UiText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/**
 * The editor's button: same dark card, radius and accent as the custom chips, so a menu does not
 * mix grey vanilla widgets with the hand-drawn pickers.
 */
public final class UiButton extends Button {
    private boolean alignLeft;

    private UiButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, Button.DEFAULT_NARRATION);
    }

    static UiButton of(int x, int y, int width, int height, String label, OnPress onPress) {
        return new UiButton(x, y, width, height, Component.literal(label), onPress);
    }

    static UiButton of(int x, int y, int width, int height, String label, OnPress onPress,
                       String tip) {
        UiButton button = of(x, y, width, height, label, onPress);
        button.setTooltip(Tooltip.create(Component.literal(UiText.s(tip))));
        return button;
    }

    UiButton alignLeft() {
        alignLeft = true;
        return this;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean hovered = isHoveredOrFocused();
        UiTheme.chip(graphics, getX(), getY(), getWidth(), getHeight(), false, hovered, active,
                UiTheme.ACCENT);
        int color = !active ? UiTheme.DISABLED : hovered ? UiTheme.ON_ACCENT : UiTheme.TEXT;
        if (alignLeft) {
            Font font = Minecraft.getInstance().font;
            String text = getMessage().getString();
            int max = getWidth() - 12;
            if (font.width(text) > max) {
                text = font.plainSubstrByWidth(text, Math.max(0, max - font.width("\u2026")))
                        + "\u2026";
            }
            graphics.drawString(font, text, getX() + 8, getY() + (getHeight() - 8) / 2, color);
        } else {
            graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(),
                    getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, color);
        }
    }
}
