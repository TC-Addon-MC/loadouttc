package com.toancao.loadouttc.gui;

import com.toancao.loadouttc.compat.HudData;
import com.toancao.loadouttc.compat.HudSlot;
import com.toancao.loadouttc.config.ConfigStore;
import com.toancao.loadouttc.config.HudAnchor;
import com.toancao.loadouttc.config.HudConfig;
import com.toancao.loadouttc.hud.HudRenderer;
import com.toancao.loadouttc.hud.StylePainter;
import com.toancao.loadouttc.util.UiText;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

/**
 * In-game editor: pick the panel, drag it, scroll to resize, right-click a slot to target it.
 * Everything visual lives in the design screen; this screen keeps position, size and preview.
 */
public final class HudEditorScreen extends Screen {
    private final Screen parent;
    private boolean backgroundDrawn;
    private boolean chromeHidden;
    private long chromeHiddenAt;
    private String selectedSlot;
    private int dragOffsetX;
    private int dragOffsetY;
    private String dragClusterId;

    private Button sizeButton;
    private Button panelButton;
    private Button slotsButton;

    public HudEditorScreen(Screen parent) {
        super(Component.literal(UiText.s("Loadout TC")));
        this.parent = parent;
    }

    @Override
    protected void init() {
        UiText.clear();
        HudConfig cfg = ConfigStore.get();
        selectedSlot = HudRenderer.selectedKey();

        boolean narrow = width < 430;
        int gap = width < 560 ? 3 : 4;
        int panelW = narrow ? 58 : 84;
        int designW = narrow ? 52 : 68;
        int slotsW = narrow ? 54 : 82;
        int posW = narrow ? 62 : 92;
        int sizeW = narrow ? 58 : 80;
        int resetW = narrow ? 46 : 56;

        int bottomCount = narrow ? 5 : 6;
        int fixed = panelW + designW + slotsW + posW + sizeW + (narrow ? 0 : resetW)
                + gap * (bottomCount - 1);
        int startX = Math.max(8, (width - fixed) / 2);
        int y = height - 50;

        panelButton = addRenderableWidget(UiButton.of(startX, y, panelW, 18, panelLabel(cfg),
                button -> openPanelMenu(), "Choose which panel to edit"));

        addRenderableWidget(UiButton.of(startX + panelW + gap, y, designW, 18,
                UiText.s("Design"), button -> openDesign(),
                "Panel, slot, text, bars and motion"));

        slotsButton = addRenderableWidget(UiButton.of(startX + panelW + designW + gap * 2, y,
                slotsW, 18, narrow ? UiText.s("Slots") : slotsLabel(),
                button -> {
                    if (minecraft != null) {
                        minecraft.setScreen(new SlotsScreen(this));
                    }
                }, "Choose which slots the HUD shows"));

        int posX = startX + panelW + designW + slotsW + gap * 3;
        addRenderableWidget(UiButton.of(posX, y, posW, 18,
                narrow ? UiText.s("Pos")
                        : UiText.s("Pos") + ": " + shortAnchor(cfg.effectiveAnchor()),
                button -> {
                    cfg.cycleActiveAnchor();
                    button.setMessage(Component.literal(narrow ? UiText.s("Pos")
                            : UiText.s("Pos") + ": " + shortAnchor(cfg.effectiveAnchor())));
                    ConfigStore.save();
                }, "Cycle the screen anchor"));

        sizeButton = addRenderableWidget(UiButton.of(posX + posW + gap, y, sizeW, 18,
                narrow ? UiText.s("Size") : UiText.s("Size") + ": " + cfg.sizeLabel(),
                button -> cycleSize(button), "Cycle Auto / S / M / L / Custom"));

        if (!narrow) {
            addRenderableWidget(UiButton.of(posX + posW + sizeW + gap, y, resetW, 18,
                    UiText.s("Reset"), button -> {
                        ConfigStore.replace(new HudConfig());
                        ConfigStore.save();
                        if (minecraft != null) {
                            minecraft.setScreen(new HudEditorScreen(parent));
                        }
                    }, "Reset every setting"));
        }

        int topWidth = 62;
        int right = width - 8;
        addRenderableWidget(UiButton.of(right - topWidth, 6, topWidth, 18, UiText.s("Done"),
                button -> onClose(), "Save and close"));
        right -= topWidth + 4;
        addRenderableWidget(UiButton.of(right - topWidth, 6, topWidth, 18,
                UiText.s("Preview"), button -> toggleChrome(), "Hide the editor chrome"));
        if (narrow) {
            right -= topWidth + 4;
            addRenderableWidget(UiButton.of(right - 46, 6, 46, 18, UiText.s("Reset"), button -> {
                ConfigStore.replace(new HudConfig());
                ConfigStore.save();
                if (minecraft != null) {
                    minecraft.setScreen(new HudEditorScreen(parent));
                }
            }, "Reset every setting"));
        }
    }

    private String panelLabel(HudConfig cfg) {
        var active = cfg.activeClusterRef();
        String name = active == null ? UiText.s("No panel") : active.name;
        return UiText.s("Panel") + ": " + name + " \u25be";
    }

    /** "Slots 5/12" for the panel being edited, so the button shows its state at a glance. */
    private String slotsLabel() {
        HudConfig cfg = ConfigStore.get();
        var active = cfg.activeClusterRef();
        HudConfig view = active == null ? cfg : active.resolve(cfg);
        var all = HudData.collectAll(minecraft, cfg);
        boolean split = active != null && cfg.perPanelSlots();
        int shown = 0;
        for (var slot : all) {
            // Split configs draw a slot in one panel only, so the count must follow ownership too.
            if (split && cfg.clusterFor(slot.key()) != active) {
                continue;
            }
            if (view.sourceEnabled(slot.source()) && view.slotVisible(slot.key())
                    && (!"vanilla".equals(slot.source()) || view.vanillaGroupEnabled(slot.group()))
                    && (!slot.empty() || view.emptySlots)) {
                shown++;
            }
        }
        return UiText.s("Slots") + " " + Math.min(shown, view.maxSlots) + "/" + all.size();
    }

    private record OverlayLine(int y, String text, int color) {
    }

    /** Dark plate behind the corner text, so it stays readable over sky, snow or lava. */
    private void drawOverlay(GuiGraphics graphics, List<OverlayLine> lines) {
        int maxWidth = width - 16;
        List<OverlayLine> fitted = new ArrayList<>();
        int maxW = 0;
        for (OverlayLine line : lines) {
            String text = fitLine(line.text(), maxWidth);
            maxW = Math.max(maxW, font.width(text));
            fitted.add(new OverlayLine(line.y(), text, line.color()));
        }
        int lastY = fitted.get(fitted.size() - 1).y();
        int plateW = Math.min(maxW + 10, width - 8);
        StylePainter.roundRect(graphics, 4, 4, plateW, lastY + 6, 4,
                StylePainter.alpha(UiTheme.WELL, 0.82f));
        for (OverlayLine line : fitted) {
            graphics.drawString(font, line.text(), 8, line.y(), line.color(), true);
        }
    }

    private String fitLine(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String cut = font.plainSubstrByWidth(text, maxWidth - font.width("\u2026"));
        return cut + "\u2026";
    }

    /** Outlines the panel the toolbar edits, so clicking a preview panel is obvious. */
    private void drawActivePanelOutline(GuiGraphics graphics, HudConfig cfg) {
        if (cfg.clusters == null || cfg.clusters.size() <= 1) {
            return;
        }
        int[] rect = HudRenderer.bounds(cfg.activeCluster);
        if (rect[2] <= 0 || rect[3] <= 0) {
            return;
        }
        StylePainter.outline(graphics, rect[0] - 2, rect[1] - 2, rect[2] + 4, rect[3] + 4, 5,
                UiTheme.ACCENT);
    }

    /** Clicking a panel in the preview makes it the one the toolbar edits. */
    private void activatePanel(String id) {
        if (id == null || id.isBlank()) {
            return;
        }
        boolean editing = ConfigStore.editingCluster();
        if (editing) {
            ConfigStore.endEdit(true);
        }
        HudConfig cfg = ConfigStore.get();
        cfg.setActiveCluster(id);
        if (editing) {
            ConfigStore.beginEdit(id);
        }
        ConfigStore.save();
        cfg = ConfigStore.get();
        if (panelButton != null) {
            panelButton.setMessage(Component.literal(panelLabel(cfg)));
        }
        if (slotsButton != null) {
            slotsButton.setMessage(Component.literal(slotsLabel()));
        }
        if (sizeButton != null) {
            sizeButton.setMessage(Component.literal(
                    UiText.s("Size") + ": " + cfg.editTarget().sizeLabel()));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        HudConfig cfg = ConfigStore.get();
        var activePanel = cfg.activeClusterRef();
        // The panel being edited decides what the preview draws; the renderer filters per panel.
        HudConfig view = activePanel == null ? cfg : activePanel.resolve(cfg);
        List<HudSlot> input = HudData.collectAll(minecraft, cfg);
        int total = input.size();
        if (total == 0) {
            input = HudData.demoSlots();
        }
        var slots = HudRenderer.prepare(input, view);
        HudRenderer.setSelection(selectedSlot);

        if (chromeHidden) {
            HudRenderer.render(graphics, input, cfg, true, mouseX, mouseY);
            if (System.currentTimeMillis() - chromeHiddenAt < 3000L) {
                graphics.drawString(font, UiText.s("Tab: show settings"), 8, 8,
                        StylePainter.alpha(UiTheme.ON_ACCENT, 0.8f), true);
            }
            return;
        }

        renderBackground(graphics, mouseX, mouseY, partialTick);
        backgroundDrawn = true;

        int panelSlots = 0;
        if (activePanel != null) {
            for (var slot : slots) if (cfg.clusterFor(slot.key()) == activePanel) panelSlots++;
        }
        String info = com.toancao.loadouttc.hud.PanelStyle.byId(view.panelStyle).label()
                + " / " + com.toancao.loadouttc.hud.SlotStyle.byId(view.slotStyle).label()
                + " / " + com.toancao.loadouttc.hud.TextStyle.byId(view.textStyle).label()
                + "  \u00b7  " + view.sizeDescription(minecraft.getWindow().getGuiScale())
                + "  \u00b7  " + Math.round(view.opacity * 100.0f) + "%"
                + (activePanel == null ? ""
                : "  \u00b7  " + UiText.s("Panel") + ": " + activePanel.name
                + "  \u00b7  " + UiText.f("%s slots", panelSlots));

        List<OverlayLine> overlay = new ArrayList<>();
        overlay.add(new OverlayLine(8, "Loadout TC", UiTheme.TITLE));
        overlay.add(new OverlayLine(24, info, UiTheme.INFO));

        String notice = null;
        int noticeColor = UiTheme.SUBTITLE;
        if (!HudData.hasProvider() && slots.isEmpty()) {
            notice = UiText.s("No equipment mod - vanilla slots only");
            noticeColor = UiTheme.WARN;
        } else if (slots.isEmpty() && total == 0) {
            notice = UiText.s("Nothing equipped - showing preview items");
        } else if (slots.isEmpty() && !view.emptySlots) {
            notice = UiText.s("Nothing equipped - turn on Empty in Slots");
        } else if (slots.isEmpty()) {
            notice = UiText.s("All slots hidden - open Slots");
            noticeColor = UiTheme.WARN;
        }
        if (notice != null) {
            overlay.add(new OverlayLine(38, notice, noticeColor));
        }

        int overrides = cfg.slotOverrides == null ? 0 : cfg.slotOverrides.size();
        int line = 50;
        if (overrides > 0) {
            overlay.add(new OverlayLine(line, UiText.f("%s slot override(s)", overrides),
                    UiTheme.SUBTITLE));
            line += 12;
        }
        if (activePanel != null && cfg.clusters.size() > 1 && panelSlots == 0) {
            overlay.add(new OverlayLine(line,
                    UiText.f("%s has no slots - tick them in Slots", activePanel.name),
                    UiTheme.WARN));
            line += 12;
        }
        if (selectedSlot != null) {
            overlay.add(new OverlayLine(line, UiText.s("Target") + ": " + shortSlot(selectedSlot)
                    + "  \u00b7  " + UiText.s("Delete clears it"), UiTheme.HINT));
        }
        drawOverlay(graphics, overlay);

        HudRenderer.render(graphics, input, cfg, true, mouseX, mouseY);
        drawActivePanelOutline(graphics, cfg);

        try {
            super.render(graphics, mouseX, mouseY, partialTick);
        } finally {
            backgroundDrawn = false;
        }
    }

    private HudConfig config() {
        return ConfigStore.get();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (backgroundDrawn) {
            return;
        }
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1) {
            String key = HudRenderer.slotKeyAt((int) mouseX, (int) mouseY);
            if (key != null) {
                selectedSlot = key.equals(selectedSlot) ? null : key;
                HudRenderer.setSelection(selectedSlot);
                return true;
            }
        }
        if (!chromeHidden && super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        HudConfig cfg = ConfigStore.get();
        String hit = HudRenderer.clusterAt((int) mouseX, (int) mouseY);
        int[] bounds = hit == null ? HudRenderer.bounds() : HudRenderer.bounds(hit);
        if (mouseX >= bounds[0] && mouseX < bounds[0] + bounds[2]
                && mouseY >= bounds[1] && mouseY < bounds[1] + bounds[3]) {
            var cluster = hit == null ? null : cfg.cluster(hit);
            if (cluster == null) {
                return false;
            }
            if (!cluster.id.equals(cfg.activeCluster)) {
                activatePanel(cluster.id);
                cfg = ConfigStore.get();
                cluster = cfg.cluster(hit);
                if (cluster == null) {
                    return false;
                }
            }
            dragOffsetX = (int) mouseX - bounds[0];
            dragOffsetY = (int) mouseY - bounds[1];
            dragClusterId = cluster.id;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX,
                                double dragY) {
        if (dragClusterId != null) {
            var cluster = ConfigStore.get().cluster(dragClusterId);
            if (cluster != null) {
                // A plain click only switches panels; the anchor goes custom once it moves.
                cluster.anchor = HudAnchor.CUSTOM.name();
                cluster.x = Mth.clamp((int) Math.round(mouseX) - dragOffsetX, 0,
                        Math.max(0, width - 16));
                cluster.y = Mth.clamp((int) Math.round(mouseY) - dragOffsetY, 0,
                        Math.max(0, height - 16));
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragClusterId != null) {
            dragClusterId = null;
            ConfigStore.save();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        HudConfig target = ConfigStore.get().editTarget();
        if (hasControlDown()) {
            target.iconSize = Mth.clamp(target.iconSize + (int) Math.signum(scrollY) * 2, 12, 40);
        } else if (hasShiftDown()) {
            target.opacity = Mth.clamp(target.opacity + (float) scrollY * 0.05f, 0.2f, 1.0f);
        } else {
            if (!"custom".equals(target.size)) {
                target.scale = target.effectiveScale(minecraft.getWindow().getGuiScale());
                target.size = "custom";
            }
            target.scale = Mth.clamp(target.scale + (float) scrollY * 0.05f, 0.5f, 3.0f);
            if (sizeButton != null) {
                sizeButton.setMessage(Component.literal(UiText.s("Size") + ": "
                        + target.sizeLabel()));
            }
        }
        ConfigStore.save();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_DELETE && selectedSlot != null) {
            HudConfig cfg = ConfigStore.get();
            if (cfg.slotOverrides != null) {
                cfg.slotOverrides.remove(selectedSlot);
            }
            ConfigStore.save();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            chromeHidden = !chromeHidden;
            chromeHiddenAt = System.currentTimeMillis();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_H) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        ConfigStore.flush();
        // Closing the editor drops the focus ring too, so the live HUD goes back to a clean look.
        HudRenderer.setSelection(null);
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void toggleChrome() {
        chromeHidden = !chromeHidden;
        chromeHiddenAt = System.currentTimeMillis();
    }

    /** Panel button: pick which panel the editor works on (and manage the list from there). */
    private void openPanelMenu() {
        if (minecraft == null) {
            return;
        }
        HudRenderer.setSelection(selectedSlot);
        minecraft.setScreen(new PanelsScreen(this));
    }

    /** Design goes straight to the look of the panel already selected in the toolbar. */
    private void openDesign() {
        if (minecraft == null) {
            return;
        }
        HudRenderer.setSelection(selectedSlot);
        ConfigStore.endEdit(true);
        HudConfig cfg = ConfigStore.get();
        String id = cfg.activeCluster;
        if (id == null || id.isBlank() || cfg.cluster(id) == null) {
            id = cfg.clusters == null || cfg.clusters.isEmpty() ? "" : cfg.clusters.get(0).id;
        }
        cfg.setActiveCluster(id);
        if (!id.isBlank()) {
            ConfigStore.beginEdit(id);
        }
        minecraft.setScreen(new StylePickerScreen(ConfigStore.get(),
                () -> minecraft.setScreen(new HudEditorScreen(parent)),
                () -> minecraft.setScreen(new PanelsScreen(parent))));
    }

    private void cycleSize(Button button) {
        HudConfig target = ConfigStore.get().editTarget();
        String next = switch (target.size == null ? "auto" : target.size) {
            case "small" -> "medium";
            case "medium" -> "large";
            case "large" -> "xlarge";
            case "xlarge" -> "huge";
            case "huge" -> "giant";
            case "giant" -> "custom";
            case "custom" -> "auto";
            default -> "small";
        };
        if ("custom".equals(next)) {
            target.scale = target.effectiveScale(minecraft.getWindow().getGuiScale());
        }
        target.size = next;
        ConfigStore.save();
        button.setMessage(Component.literal(UiText.s("Size") + ": " + target.sizeLabel()));
    }

    private static String shortSlot(String key) {
        String[] parts = key.split("/");
        if (parts.length >= 3) {
            return parts[parts.length - 2] + "/" + parts[parts.length - 1];
        }
        return key;
    }

    private static String shortAnchor(HudAnchor anchor) {
        return switch (anchor) {
            case TOP_LEFT -> "TL";
            case TOP_RIGHT -> "TR";
            case BOTTOM_LEFT -> "BL";
            case BOTTOM_RIGHT -> "BR";
            case ABOVE_HOTBAR -> "Bar";
            case CENTER -> "Mid";
            case CUSTOM -> "Free";
        };
    }
}
