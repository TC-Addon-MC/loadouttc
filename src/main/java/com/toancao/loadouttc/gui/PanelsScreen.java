package com.toancao.loadouttc.gui;

import com.toancao.loadouttc.compat.HudData;
import com.toancao.loadouttc.compat.HudSlot;
import com.toancao.loadouttc.config.ConfigStore;
import com.toancao.loadouttc.config.HudCluster;
import com.toancao.loadouttc.config.HudConfig;
import com.toancao.loadouttc.util.UiText;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Asks which panel the design screen should edit, and manages the panel list. Kept separate from
 * the design screen so "which panel" is a decision, not a row buried in a scrolling settings list.
 */
public final class PanelsScreen extends Screen {
    private static final int ROW = 24;
    private static final int BUTTON = 20;

    private final Screen parent;
    private int scroll;

    public PanelsScreen(Screen parent) {
        super(Component.literal(UiText.s("Panels")));
        this.parent = parent;
    }

    @Override
    protected void init() {
        UiText.clear();
        HudConfig cfg = ConfigStore.get();

        int w = Math.min(268, width - 32);
        int x = (width - w) / 2;
        int y = 54;
        int visible = Math.max(3, (height - 132) / ROW);
        int total = cfg.clusters.size();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, total - visible)));


        for (int i = scroll; i < total && i < scroll + visible; i++) {
            HudCluster cluster = cfg.clusters.get(i);
            String id = cluster.id;
            addRenderableWidget(UiButton.of(x, y, w - 76, BUTTON, label(cluster),
                    button -> open(id), "Edit this panel's look and layout").alignLeft());

            addRenderableWidget(UiButton.of(x + w - 72, y, 24, BUTTON,
                    cluster.enabled ? UiText.s("On") : UiText.s("Off"),
                    button -> {
                        cluster.enabled = !cluster.enabled;
                        ConfigStore.save();
                        rebuildWidgets();
                    }, "Show or hide this panel"));

            if (cluster.look != null) {
                addRenderableWidget(UiButton.of(x + w - 46, y, 20, BUTTON, "\u21ba",
                        button -> {
                            cluster.look = null;
                            ConfigStore.save();
                            rebuildWidgets();
                        }, "Use the shared look again"));
            }

            if (total > 1) {
                addRenderableWidget(UiButton.of(x + w - 22, y, 20, BUTTON, "\u00d7",
                        button -> {
                            // Commit any open panel edit first: the hidden-slot list lives on the
                            // real config, not on the per-panel editing copy.
                            ConfigStore.endEdit(true);
                            HudConfig live = ConfigStore.get();
                            live.removeCluster(id, knownKeys(live, cluster));
                            ConfigStore.save();
                            rebuildWidgets();
                        }, "Delete this panel and the slots it shows"));
            }
            y += ROW;
        }

        if (total > visible) {
            int arrowsY = y;
            addRenderableWidget(UiButton.of(x, arrowsY, 26, BUTTON, "\u25b2", button -> {
                scroll = Math.max(0, scroll - 1);
                rebuildWidgets();
            }));
            addRenderableWidget(UiButton.of(x + 28, arrowsY, 26, BUTTON, "\u25bc", button -> {
                scroll++;
                rebuildWidgets();
            }));
            y += ROW;
        }

        addRenderableWidget(UiButton.of(x, y + 4, w, BUTTON, "+ " + UiText.s("Split panel"),
                button -> addPanel(),
                "Creates a panel, then opens the slot menu to pick its slots"));

        addRenderableWidget(UiButton.of(x, y + 4 + BUTTON + 4, w, BUTTON,
                UiText.s("Merge panels"), button -> {
                    if (minecraft != null) {
                        minecraft.setScreen(new MergeScreen(this));
                    }
                }, "Move one panel's slots into another"));

        addRenderableWidget(UiButton.of(x, height - 40, w, BUTTON, UiText.s("Done"),
                button -> onClose()));
    }

    private String label(HudCluster cluster) {
        StringBuilder out = new StringBuilder(cluster.name);
        if (cluster.rest) {
            out.append("  \u00b7  ").append(UiText.s("Rest"));
        }
        out.append("  \u00b7  ").append(UiText.f("%s slots", drawn(cluster)));
        if (!cluster.enabled) {
            out.append("  (").append(UiText.s("Off")).append(')');
        }
        return out.toString();
    }

    /** What the HUD draws for this panel: its own filters plus the slots it owns or catches. */
    private int drawn(HudCluster cluster) {
        if (minecraft == null || minecraft.player == null) {
            return cluster.slots == null ? 0 : cluster.slots.size();
        }
        HudConfig cfg = ConfigStore.get();
        HudConfig view = cluster.resolve(cfg);
        int count = 0;
        for (HudSlot slot : HudData.collectAll(minecraft, cfg)) {
            if (cfg.clusterFor(slot.key()) != cluster) {
                continue;
            }
            if (view.sourceEnabled(slot.source()) && view.slotVisible(slot.key())
                    && (!"vanilla".equals(slot.source()) || view.vanillaGroupEnabled(slot.group()))
                    && (!slot.empty() || view.emptySlots)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Every slot key the client knows. Deleting a panel needs them so it can switch off exactly the
     * slots that panel was drawing, including the ones it only caught through the catch-all role.
     */
    private List<String> knownKeys(HudConfig cfg, HudCluster cluster) {
        List<String> keys = new ArrayList<>();
        if (minecraft == null || minecraft.player == null) {
            if (cluster.slots != null) keys.addAll(cluster.slots);
            return keys;
        }
        for (HudSlot slot : HudData.collectAll(minecraft, cfg)) {
            keys.add(slot.key());
        }
        return keys;
    }

    /** Split flow: create the panel, then go straight to the slot list to fill it. */
    private void addPanel() {
        HudConfig cfg = ConfigStore.get();
        String source = cfg.activeCluster.isBlank() ? cfg.clusters.get(0).id : cfg.activeCluster;
        HudCluster created = cfg.addClusterNear(
                UiText.s("Panel") + " " + (cfg.clusters.size() + 1), source);
        ConfigStore.save();
        if (minecraft != null) {
            minecraft.setScreen(new SlotsScreen(this, created.id));
        }
    }

    private void open(String clusterId) {
        ConfigStore.endEdit(true);
        HudConfig cfg = ConfigStore.get();
        cfg.activeCluster = clusterId == null ? "" : clusterId;
        if (!cfg.activeCluster.isBlank()) {
            ConfigStore.beginEdit(cfg.activeCluster);
        }
        if (minecraft != null) {
            minecraft.setScreen(new StylePickerScreen(ConfigStore.get(),
                    () -> minecraft.setScreen(parent instanceof HudEditorScreen ? parent
                            : new HudEditorScreen(parent)),
                    () -> minecraft.setScreen(new PanelsScreen(parent))));
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Math.max(0, scroll - (int) Math.signum(scrollY));
        rebuildWidgets();
        return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        GuiBackdrop.draw(graphics, width, height);
        GuiBackdrop.header(graphics, font, UiText.s("Panels"),
                UiText.s("Pick a panel to design."));
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        ConfigStore.endEdit(true);
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
