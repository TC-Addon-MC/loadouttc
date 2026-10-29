package com.toancao.loadouttc.gui;

import com.toancao.loadouttc.compat.HudData;
import com.toancao.loadouttc.compat.HudSlot;
import com.toancao.loadouttc.config.ConfigStore;
import com.toancao.loadouttc.config.HudCluster;
import com.toancao.loadouttc.config.HudConfig;
import com.toancao.loadouttc.config.SlotScope;
import com.toancao.loadouttc.hud.HudRenderer;
import com.toancao.loadouttc.hud.StylePainter;
import com.toancao.loadouttc.util.UiText;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Slot picker: one compact chrome row, then group blocks. Every vanilla group keeps its cards on
 * their own lines, and a group that is switched off for the HUD shows only its heading, so it can
 * be switched back on.
 */
public final class SlotsScreen extends Screen {
    private static final int PAD = 16;
    private static final int CHIP_H = 18;
    private static final int CELL_W = 68;
    private static final int CELL_H = 56;
    private static final int GAP = 6;
    private static final int HEADER_H = 13;

    private static final String[] GROUP_IDS = {"armor", "offhand", "hotbar", "inventory"};

    private final Screen parent;
    private final HudConfig config;
    /**
     * Slot selection of the panel being edited; {@code null} while a single panel still follows
     * the shared settings. Every read and write below goes through the helpers at the bottom so
     * the two cases stay in one place.
     */
    private final SlotScope scope;
    /** Panel that receives the ticked slots; empty = the user picks it in the footer. */
    private final String assignTo;

    private final List<Hit> hits = new ArrayList<>();
    private final List<Tip> tips = new ArrayList<>();
    private String tab = "vanilla";
    /** UI-only filter: all | armor | offhand | hotbar | inventory. */
    private String groupFilter = "all";
    private double scroll;
    private int contentHeight;
    private int gridTop;
    private int gridBottom;
    private int scrollTrackTop;
    private int scrollTrackHeight;
    private int scrollThumbHeight;
    private boolean draggingScrollbar;
    private double scrollGrab;
    private String moveTarget = "";

    /** Slots collected once per frame. */
    private List<HudSlot> cached;

    public SlotsScreen(Screen parent) {
        this(parent, "");
    }

    public SlotsScreen(Screen parent, String assignTo) {
        super(Component.translatable(UiText.key("Loadout TC - Slots")));
        this.parent = parent;
        this.config = ConfigStore.get();
        this.assignTo = assignTo == null ? "" : assignTo;
        if (!this.assignTo.isBlank() && config.cluster(this.assignTo) != null) {
            moveTarget = this.assignTo;
            // The screen edits the panel it was opened for, not whatever the editor picked last.
            config.activeCluster = this.assignTo;
        }
        this.scope = config.activeSlotScope();
    }

    @Override
    protected void init() {
        super.init();
        UiText.clear();
    }

    // ---- Data ----

    private List<HudSlot> allSlots() {
        if (cached != null) {
            return cached;
        }
        if (minecraft == null || minecraft.player == null) {
            return List.of();
        }
        cached = HudData.collectAll(minecraft, config);
        return cached;
    }

    private void ensureTab() {
        List<HudData.SourceInfo> sources = HudData.sources();
        if (sources.isEmpty()) {
            tab = "vanilla";
            return;
        }
        for (HudData.SourceInfo source : sources) {
            if (source.id().equals(tab)) {
                return;
            }
        }
        tab = sources.get(0).id();
        scroll = 0;
    }

    private List<HudSlot> rows() {
        List<HudSlot> out = new ArrayList<>();
        for (HudSlot slot : allSlots()) {
            if (tab.equals(slot.source())) {
                out.add(slot);
            }
        }
        return HudRenderer.sortSlots(out, sortMode(), scope == null ? config.slotOrder : scope.order);
    }

    /** True when this slot is drawn by the panel being edited. */
    private boolean effective(HudSlot slot) {
        return sourceEnabled(slot.source()) && slotVisible(slot.key())
                && (!"vanilla".equals(slot.source()) || groupEnabled(slot.group()))
                && ownedHere(slot);
    }

    /** Single-panel configs draw everything they filter in; split configs own their slots. */
    private boolean ownedHere(HudSlot slot) {
        return scope == null || config.activeCluster.equals(config.clusterIdFor(slot.key()));
    }

    // ---- Panel scope ----
    // A config with several panels keeps slot selection per panel; a single panel still edits the
    // shared settings. These helpers are the only place that knows the difference.

    private boolean sourceEnabled(String sourceId) {
        return scope == null ? config.sourceEnabled(sourceId) : scope.sourceEnabled(sourceId);
    }

    private boolean slotVisible(String key) {
        return scope == null ? config.slotVisible(key) : scope.slotVisible(key);
    }

    private boolean groupEnabled(String group) {
        return scope == null ? config.vanillaGroupEnabled(group) : scope.vanillaGroupEnabled(group);
    }

    private boolean emptySlots() {
        return scope == null ? config.emptySlots : scope.emptySlots;
    }

    private String sortMode() {
        return scope == null ? config.sortMode : scope.sortMode;
    }

    private String filterMode() {
        return scope == null ? config.slotFilter : scope.filter;
    }

    private void toggleSource(String sourceId) {
        if (scope == null) {
            if (config.hiddenSources == null) {
                config.hiddenSources = new ArrayList<>();
            }
            if (config.hiddenSources.contains(sourceId)) {
                config.hiddenSources.remove(sourceId);
            } else {
                config.hiddenSources.add(sourceId);
            }
        } else {
            scope.toggleSource(sourceId);
        }
    }

    private void setGroupEnabled(String group, boolean enabled) {
        if (scope == null) {
            switch (group == null ? "" : group) {
                case "armor" -> config.vanillaArmor = enabled;
                case "offhand" -> config.vanillaOffhand = enabled;
                case "hotbar" -> config.vanillaHotbar = enabled;
                case "inventory" -> config.vanillaInventory = enabled;
                default -> {
                }
            }
        } else {
            scope.setGroupEnabled(group, enabled);
        }
    }

    private void setEmptySlots(boolean enabled) {
        if (scope == null) {
            config.emptySlots = enabled;
        } else {
            scope.emptySlots = enabled;
        }
    }

    private void setFilter() {
        if (scope == null) {
            config.switchFilter(currentVisibleKeys());
        } else {
            scope.switchFilter(currentVisibleKeys());
        }
    }

    private void setSlotVisible(String key, boolean visible) {
        if (scope == null) {
            config.setSlotVisible(key, visible);
        } else {
            scope.setSlotVisible(key, visible);
        }
    }

    private int maxSlots() {
        var active = config.activeClusterRef();
        return active == null ? config.maxSlots : active.resolve(config).maxSlots;
    }

    private int totalFor(String source) {
        int total = 0;
        for (HudSlot slot : allSlots()) {
            if (source.equals(slot.source())) {
                total++;
            }
        }
        return total;
    }

    private int shownFor(String source) {
        int shown = 0;
        for (HudSlot slot : allSlots()) {
            if (source.equals(slot.source()) && effective(slot)) {
                shown++;
            }
        }
        return shown;
    }

    private int onHud() {
        int count = 0;
        for (HudSlot slot : allSlots()) {
            if (effective(slot) && (!slot.empty() || emptySlots())) {
                count++;
            }
        }
        return count;
    }

    /** One block per group; a disabled group keeps its heading and loses all of its cards. */
    private List<Block> blocks() {
        List<HudSlot> all = rows();
        List<Block> out = new ArrayList<>();
        if ("vanilla".equals(tab)) {
            String[] labels = {UiText.s("Armor"), UiText.s("Offhand"), UiText.s("Hotbar"),
                    UiText.s("Inventory")};
            for (int i = 0; i < GROUP_IDS.length; i++) {
                String id = GROUP_IDS[i];
                if (!"all".equals(groupFilter) && !groupFilter.equals(id)) {
                    continue;
                }
                List<HudSlot> list = new ArrayList<>();
                for (HudSlot slot : all) {
                    if (id.equals(slot.group())) {
                        list.add(slot);
                    }
                }
                out.add(new Block(id, labels[i], list, groupEnabled(id), true));
            }
        } else {
            String label = tab;
            for (HudData.SourceInfo source : HudData.sources()) {
                if (source.id().equals(tab)) {
                    label = source.label();
                    break;
                }
            }
            out.add(new Block(tab, label, all, sourceEnabled(tab), false));
        }
        return out;
    }

    // ---- Render ----

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        cached = null;
        hits.clear();
        tips.clear();
        ensureTab();
        renderBackground(graphics, mouseX, mouseY, partialTick);
        GuiBackdrop.draw(graphics, width, height);

        int contentTop = drawChrome(graphics, mouseX, mouseY);
        gridTop = contentTop + 2;
        gridBottom = Math.max(gridTop + CELL_H, height - 44);
        drawBlocks(graphics, mouseX, mouseY);
        // Footer band: separates the buttons from the list instead of letting the two blend.
        graphics.fill(0, height - 44, width, height, 0xCC0B0E14);
        graphics.fill(0, height - 45, width, height - 44, UiTheme.OUTLINE);
        drawScrollbar(graphics);
        drawFooter(graphics, mouseX, mouseY);
        drawHoverTip(graphics, mouseX, mouseY);
    }

    /** A 6 px track with a solid thumb, so the list length is obvious at a glance. */
    private void drawScrollbar(GuiGraphics graphics) {
        int viewport = gridBottom - gridTop;
        int maxScroll = Math.max(0, contentHeight - viewport);
        if (maxScroll <= 0) {
            scrollThumbHeight = 0;
            return;
        }
        int trackX = width - 8;
        scrollTrackTop = gridTop;
        scrollTrackHeight = viewport;
        StylePainter.roundRect(graphics, trackX, gridTop, 6, viewport, 3, UiTheme.INSET);
        StylePainter.outline(graphics, trackX, gridTop, 6, viewport, 3, UiTheme.OUTLINE);
        int thumbH = Math.max(24, (int) (viewport * (viewport / (double) contentHeight)));
        scrollThumbHeight = thumbH;
        int thumbY = gridTop + (int) ((viewport - thumbH) * (scroll / maxScroll));
        StylePainter.roundRect(graphics, trackX + 1, thumbY, 4, thumbH, 2, UiTheme.ACCENT);
        StylePainter.outline(graphics, trackX + 1, thumbY, 4, thumbH, 2,
                StylePainter.alpha(UiTheme.BRIGHT, 0.45f));
    }

    private boolean scrollbarHit(double mouseX, double mouseY) {
        return scrollThumbHeight > 0 && mouseX >= width - 12 && mouseX <= width - 2
                && mouseY >= scrollTrackTop && mouseY < scrollTrackTop + scrollTrackHeight;
    }

    private int scrollFromMouse(double mouseY, double grab) {
        int span = Math.max(1, scrollTrackHeight - scrollThumbHeight);
        double max = Math.max(0, contentHeight - (gridBottom - gridTop));
        double ratio = (mouseY - scrollTrackTop - grab) / span;
        return (int) Math.round(Math.max(0, Math.min(1, ratio)) * max);
    }

    /** One row of tabs on the left and controls on the right (two rows only when they do not fit). */
    private int drawChrome(GuiGraphics graphics, int mouseX, int mouseY) {
        int y = 10;
        int groupW = font.width(UiText.s("Group") + ": " + groupLabel()) + 16;
        int half = Math.max(font.width(UiText.s("Checked")), font.width(UiText.s("All"))) + 14;
        int modeW = half * 2 + 2;
        int emptyW = font.width(UiText.s("Empty")) + 16;
        int sourceW = font.width(UiText.s("Source")) + 16;
        int sortW = font.width(sortLabel()) + 16;
        int doneW = 46;
        int controlsW = groupW + 6 + modeW + 6 + emptyW + 6 + sourceW + 6 + sortW + 6 + doneW;

        int tabsW = 0;
        int gap = 0;
        for (HudData.SourceInfo source : HudData.sources()) {
            tabsW += font.width(tabLabel(source)) + 16 + gap;
            gap = 4;
        }
        String title = UiText.s("Slots");
        if (scope != null) {
            HudCluster active = config.activeClusterRef();
            if (active != null) {
                title += "  \u00b7  " + active.name;
            }
        }
        int titleW = font.width(title) + 12;
        boolean twoRows = titleW + tabsW + 12 + controlsW > width - PAD * 2;
        if (twoRows && titleW + tabsW > width - PAD * 2) {
            twoRows = true;
        }

        int tabsRight = twoRows ? width - PAD : width - PAD - controlsW - 12;
        int afterTabs = drawTabs(graphics, mouseX, mouseY, PAD + titleW, y, tabsRight);
        graphics.drawString(font, title, PAD, y + 5, UiTheme.TITLE, true);

        int controlY = twoRows ? Math.max(afterTabs, y + CHIP_H + 4) : y;
        int x = width - PAD - controlsW;
        if (twoRows) {
            x = width - PAD - controlsW;
        }

        headerButton(graphics, mouseX, mouseY, x, controlY, groupW, CHIP_H,
                UiText.s("Group") + ": " + groupLabel(), !"all".equals(groupFilter),
                this::cycleGroup);
        tip(x, controlY, groupW, CHIP_H, UiText.s("Filter one group"));
        x += groupW + 6;

        boolean checked = "selected".equals(filterMode());
        headerButton(graphics, mouseX, mouseY, x, controlY, half, CHIP_H, UiText.s("All"),
                !checked, () -> {
                    if (checked) {
                        setFilter();
                        save();
                    }
                });
        headerButton(graphics, mouseX, mouseY, x + half + 2, controlY, half, CHIP_H,
                UiText.s("Checked"), checked, () -> {
                    if (!checked) {
                        setFilter();
                        save();
                    }
                });
        x += modeW + 6;

        headerButton(graphics, mouseX, mouseY, x, controlY, emptyW, CHIP_H, UiText.s("Empty"),
                emptySlots(), () -> {
                    setEmptySlots(!emptySlots());
                    save();
                });
        tip(x, controlY, emptyW, CHIP_H, UiText.s("Show empty slots"));
        x += emptyW + 6;

        headerButton(graphics, mouseX, mouseY, x, controlY, sourceW, CHIP_H, UiText.s("Source"),
                sourceEnabled(tab), () -> {
                    toggleSource(tab);
                    save();
                });
        tip(x, controlY, sourceW, CHIP_H, UiText.s("Source on the HUD"));
        x += sourceW + 6;

        headerButton(graphics, mouseX, mouseY, x, controlY, sortW, CHIP_H, sortLabel(), false,
                this::cycleSort);
        tip(x, controlY, sortW, CHIP_H, UiText.s("Sort order"));
        x += sortW + 6;

        headerButton(graphics, mouseX, mouseY, x, controlY, doneW, CHIP_H, UiText.s("Done"),
                false, this::onClose);

        return twoRows ? controlY + CHIP_H + 4 : y + CHIP_H + 4;
    }

    private int drawTabs(GuiGraphics graphics, int mouseX, int mouseY, int startX, int y,
                         int maxRight) {
        int x = startX;
        int currentY = y;
        for (HudData.SourceInfo source : HudData.sources()) {
            String label = tabLabel(source);
            int w = font.width(label) + 16;
            if (x + w > maxRight) {
                x = PAD;
                currentY += CHIP_H + 4;
            }
            String id = source.id();
            headerButton(graphics, mouseX, mouseY, x, currentY, w, CHIP_H, label,
                    id.equals(tab), () -> {
                        tab = id;
                        scroll = 0;
                    });
            tip(x, currentY, w, CHIP_H, UiText.f("Show %s slots", source.label()));
            x += w + 4;
        }
        return currentY + CHIP_H;
    }

    private String tabLabel(HudData.SourceInfo source) {
        return source.label() + "  " + shownFor(source.id()) + "/" + totalFor(source.id());
    }

    private String sortLabel() {
        return UiText.s("Sort") + ": " + UiText.s(switch (sortMode() == null
                ? "default" : sortMode()) {
            case "name" -> "Name";
            case "type" -> "Type";
            case "source" -> "Source";
            case "custom" -> "Custom";
            default -> "Default";
        });
    }

    private String groupLabel() {
        return switch (groupFilter) {
            case "armor" -> UiText.s("Armor");
            case "offhand" -> UiText.s("Offhand");
            case "hotbar" -> UiText.s("Hotbar");
            case "inventory" -> UiText.s("Inventory");
            default -> UiText.s("All");
        };
    }

    private void cycleGroup() {
        groupFilter = switch (groupFilter) {
            case "all" -> "armor";
            case "armor" -> "offhand";
            case "offhand" -> "hotbar";
            case "hotbar" -> "inventory";
            default -> "all";
        };
        scroll = 0;
    }

    /** Group blocks: a heading, then the cards of that group on their own lines. */
    private void drawBlocks(GuiGraphics graphics, int mouseX, int mouseY) {
        if (minecraft == null || minecraft.player == null) {
            graphics.drawString(font, UiText.s("Join a world to see slots."), PAD, gridTop + 4,
                    UiTheme.WARN, false);
            contentHeight = 0;
            return;
        }
        List<Block> blocks = blocks();
        int columns = Math.max(2, (width - PAD * 2 + GAP) / (CELL_W + GAP));
        int total = 0;
        for (Block block : blocks) {
            total += blockHeight(block, columns);
        }
        contentHeight = total;
        int viewport = gridBottom - gridTop;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, contentHeight - viewport)));

        graphics.enableScissor(0, gridTop, width, gridBottom);
        try {
            int y = gridTop - (int) scroll;
            for (Block block : blocks) {
                int height = blockHeight(block, columns);
                if (y + height >= gridTop && y <= gridBottom) {
                    drawBlock(graphics, block, y, columns, mouseX, mouseY);
                }
                y += height;
            }
        } finally {
            graphics.disableScissor();
        }
    }

    private int blockHeight(Block block, int columns) {
        int cards = block.enabled ? (block.slots.size() + columns - 1) / columns : 0;
        return HEADER_H + 2 + cards * (CELL_H + GAP) + 4;
    }

    private void drawBlock(GuiGraphics graphics, Block block, int y, int columns, int mouseX,
                           int mouseY) {
        int accent = UiTheme.ACCENT;
        graphics.drawString(font, block.title, PAD, y + 3,
                block.enabled ? accent : UiTheme.MUTED, false);
        if (block.vanilla) {
            int chipW = 34;
            int chipX = width - PAD - chipW;
            headerButton(graphics, mouseX, mouseY, chipX, y, chipW, 12,
                    block.enabled ? UiText.s("On") : UiText.s("Off"), block.enabled, () -> {
                        setGroupEnabled(block.id, !block.enabled);
                        save();
                    });
            tip(chipX, y, chipW, 12, UiText.s("Group on the HUD"));
        }
        if (!block.enabled) {
            return;
        }
        int cardY = y + HEADER_H + 2;
        for (int i = 0; i < block.slots.size(); i++) {
            int column = i % columns;
            int row = i / columns;
            int x = PAD + column * (CELL_W + GAP);
            int cy = cardY + row * (CELL_H + GAP);
            drawSlotCard(graphics, block.slots.get(i), x, cy, mouseX, mouseY);
        }
    }

    private void drawSlotCard(GuiGraphics graphics, HudSlot slot, int x, int y, int mouseX,
                              int mouseY) {
        boolean ticked = slotVisible(slot.key()) && ownedHere(slot);
        boolean blocked = !sourceEnabled(slot.source())
                || ("vanilla".equals(slot.source()) && !groupEnabled(slot.group()));
        boolean visible = ticked && !blocked;
        boolean hovered = mouseX >= x && mouseX < x + CELL_W && mouseY >= y
                && mouseY < y + CELL_H && mouseY >= gridTop && mouseY < gridBottom;
        int accent = UiTheme.ACCENT;
        StylePainter.roundRect(graphics, x, y, CELL_W, CELL_H, UiTheme.CARD_RADIUS,
                visible ? StylePainter.alpha(accent, 0.20f) : hovered ? UiTheme.CARD_HOVER
                        : UiTheme.CARD);
        StylePainter.outline(graphics, x, y, CELL_W, CELL_H, UiTheme.CARD_RADIUS,
                visible ? accent : hovered ? accent : UiTheme.OUTLINE);

        int boxX = x + 5;
        int boxY = y + 5;
        StylePainter.roundRect(graphics, boxX, boxY, 9, 9, 2,
                visible ? accent : UiTheme.INSET);
        if (visible) {
            graphics.fill(boxX + 2, boxY + 4, boxX + 4, boxY + 7, UiTheme.WELL);
            graphics.fill(boxX + 4, boxY + 2, boxX + 7, boxY + 6, UiTheme.WELL);
        }

        boolean customSort = "custom".equals(sortMode());
        if (config.clusters.size() > 1 && !(hovered && customSort)) {
            graphics.fill(x + CELL_W - 11, y + 6, x + CELL_W - 5, y + 12, UiTheme.INFO);
        }

        ItemStack stack = slot.stack();
        int iconX = x + (CELL_W - 16) / 2;
        if (stack != null && !stack.isEmpty()) {
            graphics.renderItem(stack, iconX, y + 8);
            if (stack.getCount() > 1) {
                String count = String.valueOf(stack.getCount());
                graphics.drawString(font, count, x + CELL_W - font.width(count) - 4, y + 20,
                        UiTheme.ON_ACCENT, true);
            }
        } else {
            graphics.fill(iconX + 3, y + 11, iconX + 13, y + 21, UiTheme.ICON_EMPTY);
        }

        String name = UiText.s(slotName(slot));
        int nameY = y + 28;
        graphics.drawString(font, fit(name, CELL_W - 8), x + 4, nameY,
                visible ? UiTheme.BRIGHT : UiTheme.MUTED, false);
        String sub = stack == null || stack.isEmpty() ? UiText.s("Empty")
                : stack.getHoverName().getString();
        graphics.drawString(font, fit(sub, CELL_W - 8), x + 4, nameY + 10, UiTheme.MUTED, false);

        if (stack != null && stack.isDamageableItem() && stack.getMaxDamage() > 0) {
            int left = stack.getMaxDamage() - stack.getDamageValue();
            float fraction = left / (float) stack.getMaxDamage();
            int colour = fraction > 0.5f ? UiTheme.OK : fraction > 0.2f ? UiTheme.WARN
                    : UiTheme.DANGER;
            graphics.fill(x + 5, y + CELL_H - 8, x + CELL_W - 5, y + CELL_H - 6, UiTheme.INSET);
            graphics.fill(x + 5, y + CELL_H - 8,
                    x + 5 + Math.round((CELL_W - 10) * fraction), y + CELL_H - 6, colour);
        }
        if (blocked) {
            graphics.fill(x, y, x + CELL_W, y + CELL_H, UiTheme.SCRIM);
        }

        hits.add(new Hit(x, y, CELL_W, CELL_H, () -> toggleSlot(slot)));
        if (hovered) {
            String state = blocked
                    ? (!sourceEnabled(slot.source()) ? UiText.s("src off")
                    : UiText.s("group off")) : visible ? UiText.s("On") : UiText.s("Off");
            String cluster = config.clusters.size() > 1
                    ? "  ·  " + clusterLabel(config.clusterIdFor(slot.key())) : "";
            tip(x, y, CELL_W, CELL_H,
                    UiText.f("Toggle %s", slotName(slot)) + "  ·  " + state + cluster);
            if (customSort) {
                arrow(graphics, x + CELL_W - 28, y + 5, "\u25b2", slot.key(), -1, mouseX, mouseY);
                arrow(graphics, x + CELL_W - 15, y + 5, "\u25bc", slot.key(), 1, mouseX, mouseY);
            }
        }
    }

    private void arrow(GuiGraphics graphics, int x, int y, String glyph, String key, int delta,
                       int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + 12 && mouseY >= y && mouseY < y + 12;
        StylePainter.roundRect(graphics, x, y, 12, 12, 3,
                hovered ? UiTheme.CARD_HOVER : UiTheme.INSET);
        StylePainter.outline(graphics, x, y, 12, 12, 3, hovered ? UiTheme.ACCENT : UiTheme.OUTLINE);
        graphics.drawString(font, glyph, x + 3, y + 2,
                hovered ? UiTheme.ON_ACCENT : UiTheme.TEXT, false);
        hits.add(new Hit(x, y, 12, 12, () -> move(key, delta)));
    }

    private void drawFooter(GuiGraphics graphics, int mouseX, int mouseY) {
        int y = height - 36;
        int x = PAD;
        x = footerChip(graphics, x, y, UiText.s("Check all"), false, mouseX, mouseY,
                () -> setTab(true), UiText.s("Tick all"));
        x = footerChip(graphics, x, y, UiText.s("Clear"), false, mouseX, mouseY,
                () -> setTab(false), UiText.s("Untick all"));

        if (config.clusters.size() > 1) {
            int moveW = font.width(UiText.s("Move checked")) + 16;
            int toW = font.width(UiText.s("To") + ": " + clusterLabel(moveTarget)) + 16;
            int moveX = width - PAD - moveW;
            int toX = moveX - 6 - toW;
            if (toX > x + 10) {
                footerChipAt(graphics, toX, y, toW,
                        UiText.s("To") + ": " + clusterLabel(moveTarget), false, mouseX, mouseY,
                        this::cycleMoveTarget, UiText.s("Target panel"));
                footerChipAt(graphics, moveX, y, moveW, UiText.s("Move checked"), false, mouseX,
                        mouseY, this::moveChecked,
                        UiText.s("Move ticked slots"));
            }
        } else {
            int shown = onHud();
            int cap = maxSlots();
            String hint = shown > cap
                    ? UiText.f("HUD shows %s of %s - raise Max slots", cap, shown)
                    : UiText.f("%s slots on the HUD", shown);
            int hintX = width - PAD - font.width(hint);
            if (hintX > x + 8) {
                graphics.drawString(font, hint, hintX, y + 5,
                        shown > cap ? UiTheme.WARN : UiTheme.MUTED, false);
            }
        }
    }

    private void headerButton(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, int w,
                              int h, String label, boolean active, Runnable action) {
        boolean hovered = isHovered(mouseX, mouseY, x, y, w, h);
        UiTheme.chip(graphics, x, y, w, h, active, hovered, true, UiTheme.ACCENT);
        graphics.drawString(font, label, x + 8, y + (h - 8) / 2 + 1,
                UiTheme.chipText(active, true), false);
        hits.add(new Hit(x, y, w, h, action));
    }

    private int footerChip(GuiGraphics graphics, int x, int y, String label, boolean active,
                           int mouseX, int mouseY, Runnable action, String tip) {
        int w = font.width(label) + 16;
        return footerChipAt(graphics, x, y, w, label, active, mouseX, mouseY, action, tip);
    }

    private int footerChipAt(GuiGraphics graphics, int x, int y, int w, String label,
                             boolean active, int mouseX, int mouseY, Runnable action, String tip) {
        headerButton(graphics, mouseX, mouseY, x, y, w, CHIP_H, label, active, action);
        tip(x, y, w, CHIP_H, tip);
        return x + w + 4;
    }

    private String clusterLabel(String id) {
        if (id == null || id.isBlank()) {
            return UiText.s("Rest");
        }
        HudCluster cluster = config.cluster(id);
        return cluster == null ? UiText.s("Rest") : cluster.name;
    }

    private void tip(int x, int y, int w, int h, String text) {
        tips.add(new Tip(x, y, w, h, UiText.s(text)));
    }

    private void drawHoverTip(GuiGraphics graphics, int mouseX, int mouseY) {
        for (int i = tips.size() - 1; i >= 0; i--) {
            Tip tip = tips.get(i);
            if (mouseX >= tip.x && mouseX < tip.x + tip.w && mouseY >= tip.y
                    && mouseY < tip.y + tip.h) {
                UiTheme.tooltip(graphics, font, tip.text, mouseX, mouseY, width,
                        UiTheme.ACCENT);
                return;
            }
        }
    }

    private static String fit(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, Math.max(1, max - 1)) + "\u2026";
    }

    private boolean isHovered(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    // ---- Actions ----

    private void toggleSlot(HudSlot slot) {
        if (effective(slot)) {
            // Off means "this panel does not draw it"; ownership stays so it cannot pop up in
            // another panel as a side effect.
            setSlotVisible(slot.key(), false);
        } else {
            // On means "this panel draws it", which needs ownership as well.
            if (scope != null) {
                config.assign(slot.key(), config.activeCluster);
            }
            setSlotVisible(slot.key(), true);
        }
        save();
    }

    private Set<String> currentVisibleKeys() {
        Set<String> keys = new LinkedHashSet<>();
        for (HudSlot slot : allSlots()) {
            if (effective(slot)) {
                keys.add(slot.key());
            }
        }
        return keys;
    }

    private void setTab(boolean visible) {
        Set<String> keys = new LinkedHashSet<>();
        for (Block block : blocks()) {
            if (!block.enabled) {
                continue;
            }
            for (HudSlot slot : block.slots) {
                keys.add(slot.key());
            }
        }
        for (String key : keys) {
            if (visible && scope != null) {
                config.assign(key, config.activeCluster);
            }
            setSlotVisible(key, visible);
        }
        save();
    }

    private void cycleSort() {
        String next = switch (sortMode() == null ? "default" : sortMode()) {
            case "default" -> "name";
            case "name" -> "type";
            case "type" -> "source";
            case "source" -> "custom";
            default -> "default";
        };
        if (scope == null) {
            config.sortMode = next;
            if ("custom".equals(next)) {
                config.ensureOrder(baseOrder());
            }
        } else {
            scope.sortMode = next;
            if ("custom".equals(next)) {
                scope.ensureOrder(baseOrder());
            }
        }
        save();
    }

    private List<String> baseOrder() {
        List<String> keys = new ArrayList<>();
        for (HudSlot slot : allSlots()) {
            keys.add(slot.key());
        }
        return keys;
    }

    private void move(String key, int delta) {
        if (scope == null) {
            config.ensureOrder(baseOrder());
            config.sortMode = "custom";
            config.moveSlot(key, delta);
        } else {
            scope.ensureOrder(baseOrder());
            scope.sortMode = "custom";
            scope.moveSlot(key, delta);
        }
        save();
    }

    private void cycleMoveTarget() {
        int index = -1;
        for (int i = 0; i < config.clusters.size(); i++) {
            if (config.clusters.get(i).id.equals(moveTarget)) {
                index = i;
                break;
            }
        }
        moveTarget = index + 1 >= config.clusters.size() ? ""
                : config.clusters.get(index + 1).id;
    }

    private void moveChecked() {
        HudCluster target = config.cluster(moveTarget);
        for (Block block : blocks()) {
            if (!block.enabled) {
                continue;
            }
            for (HudSlot slot : block.slots) {
                if (effective(slot)) {
                    config.assign(slot.key(), moveTarget);
                    if (target != null && target.scope != null) {
                        target.scope.setSlotVisible(slot.key(), true);
                    }
                }
            }
        }
        save();
    }

    private static String groupName(HudSlot slot) {
        if (slot.groupLabel() != null && !slot.groupLabel().isBlank()) {
            return slot.groupLabel();
        }
        return slot.group() == null ? "Slots" : slot.group();
    }

    private static String slotName(HudSlot slot) {
        if ("armor".equals(slot.group())) {
            String id = slot.slot() == null ? "" : slot.slot();
            return id.isEmpty() ? "Armor" : Character.toUpperCase(id.charAt(0)) + id.substring(1);
        }
        if ("offhand".equals(slot.group())) {
            return "Offhand";
        }
        return groupName(slot) + " " + (slot.index() + 1);
    }

    private void save() {
        ConfigStore.save();
    }

    // ---- Input ----

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && scrollbarHit(mouseX, mouseY)) {
            int thumbY = gridTop + (int) ((scrollTrackHeight - scrollThumbHeight)
                    * (scroll / Math.max(1e-6, contentHeight - (gridBottom - gridTop))));
            boolean onThumb = mouseY >= thumbY && mouseY < thumbY + scrollThumbHeight;
            scrollGrab = onThumb ? mouseY - thumbY : scrollThumbHeight / 2.0;
            draggingScrollbar = true;
            scroll = scrollFromMouse(mouseY, scrollGrab);
            return true;
        }
        if (button == 0) {
            for (int i = hits.size() - 1; i >= 0; i--) {
                Hit hit = hits.get(i);
                if (mouseX >= hit.x && mouseX < hit.x + hit.w
                        && mouseY >= hit.y && mouseY < hit.y + hit.h) {
                    hit.action.run();
                    cached = null;
                    scroll = Math.max(0, scroll);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX,
                                double dragY) {
        if (draggingScrollbar) {
            scroll = scrollFromMouse(mouseY, scrollGrab);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingScrollbar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseY >= gridTop && mouseY <= gridBottom) {
            int max = Math.max(0, contentHeight - (gridBottom - gridTop));
            scroll = Math.max(0, Math.min(max, scroll - scrollY * 24.0));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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

    private record Block(String id, String title, List<HudSlot> slots, boolean enabled,
                         boolean vanilla) {
    }

    private record Hit(int x, int y, int w, int h, Runnable action) {
    }

    private record Tip(int x, int y, int w, int h, String text) {
    }
}
