package com.toancao.loadouttc.gui;

import com.toancao.loadouttc.config.ConfigStore;
import com.toancao.loadouttc.config.HudCluster;
import com.toancao.loadouttc.config.HudConfig;
import com.toancao.loadouttc.util.UiText;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Merges two panels: the destination receives every slot of the tail panel, and the tail's look
 * becomes the standard for the result.
 */
public final class MergeScreen extends Screen {
    private static final int BUTTON = 20;
    private static final int ROW = 26;

    private final Screen parent;
    private final HudConfig config = ConfigStore.get();
    private String destId = "";
    private String sourceId = "";

    public MergeScreen(Screen parent) {
        super(Component.literal(UiText.s("Merge panels")));
        this.parent = parent;
    }

    @Override
    protected void init() {
        UiText.clear();
        List<HudCluster> panels = config.clusters;
        int w = Math.min(280, width - 32);
        int x = (width - w) / 2;
        int y = 72;

        if (panels.size() < 2) {
            addRenderableWidget(UiButton.of(x, y, w, BUTTON, UiText.s("Done"),
                    b -> onClose()));
            return;
        }
        if (config.cluster(destId) == null) {
            destId = panels.get(0).id;
        }
        if (config.cluster(sourceId) == null || sourceId.equals(destId)) {
            sourceId = firstOther(destId);
        }

        addRenderableWidget(UiButton.of(x, y, w, BUTTON,
                UiText.s("Destination") + ": " + name(destId), b -> {
                    destId = nextPanel(destId, null);
                    if (destId.equals(sourceId)) {
                        sourceId = firstOther(destId);
                    }
                    rebuildWidgets();
                }).alignLeft());
        y += ROW;

        addRenderableWidget(UiButton.of(x, y, w, BUTTON,
                UiText.s("Tail (absorbed)") + ": " + name(sourceId), b -> {
                    sourceId = nextPanel(sourceId, destId);
                    rebuildWidgets();
                }).alignLeft());
        y += ROW + 6;

        addRenderableWidget(UiButton.of(x, y, w, BUTTON, UiText.s("Merge"),
                b -> conditionallyMerge()));
        y += ROW;

        addRenderableWidget(UiButton.of(x, y, w, BUTTON, UiText.s("Cancel"),
                b -> onClose()));
    }

    private void conditionallyMerge() {
        if (config.mergeInto(sourceId, destId, true)) {
            ConfigStore.save();
        }
        onClose();
    }

    private String firstOther(String exclude) {
        for (HudCluster panel : config.clusters) {
            if (!panel.id.equals(exclude)) {
                return panel.id;
            }
        }
        return exclude;
    }

    private String nextPanel(String current, String exclude) {
        List<String> ids = new ArrayList<>();
        for (HudCluster panel : config.clusters) {
            if (exclude == null || !panel.id.equals(exclude)) {
                ids.add(panel.id);
            }
        }
        if (ids.isEmpty()) {
            return current;
        }
        int index = ids.indexOf(current);
        return ids.get((index + 1) % ids.size());
    }

    private String name(String id) {
        HudCluster panel = config.cluster(id);
        return panel == null ? "" : panel.name;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        GuiBackdrop.draw(graphics, width, height);
        GuiBackdrop.header(graphics, font, UiText.s("Merge panels"), null);
        if (config.clusters.size() < 2) {
            graphics.drawString(font, UiText.s("Need at least two panels"),
                    UiTheme.HEADER_X, UiTheme.HEADER_SUB_Y, UiTheme.WARN, false);
        } else {
            graphics.drawString(font,
                    UiText.s("Slots move to the destination; the tail's look becomes the standard."),
                    UiTheme.HEADER_X, UiTheme.HEADER_SUB_Y, UiTheme.SUBTITLE, false);
            graphics.drawString(font,
                    UiText.f("Destination: %s slots, tail: %s slots",
                            owned(destId), owned(sourceId)),
                    UiTheme.HEADER_X, UiTheme.HEADER_SUB_Y + 14, UiTheme.INFO, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private int owned(String id) {
        HudCluster panel = config.cluster(id);
        if (panel == null) {
            return 0;
        }
        return panel.slots == null ? 0 : panel.slots.size();
    }

    @Override
    public void onClose() {
        ConfigStore.flush();
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
