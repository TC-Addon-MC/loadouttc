package com.toancao.loadouttc.hud;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Axis;
import com.toancao.loadouttc.api.EffectFrame;
import com.toancao.loadouttc.compat.HudSlot;
import com.toancao.loadouttc.config.HudAnchor;
import com.toancao.loadouttc.config.HudCluster;
import com.toancao.loadouttc.config.HudConfig;
import com.toancao.loadouttc.config.LayerOptions;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Lays out and draws the HUD.
 *
 * <h2>Why this is structured in passes</h2>
 * Each frame runs the same passes over every cell — plate, icon, overlay, label, focus — instead of
 * finishing one cell at a time. That keeps the stacking order global and identical for every style:
 * no cell can paint its plate or glow over a sibling's label or border, and count, cooldown and
 * durability always sit above the 3D item model (which renders at z=150).
 *
 * <h2>Why nothing overflows</h2>
 * Each style declares how much chrome it needs via {@link HudTheme#slotInset}. The layout reserves
 * that box, and {@link StylePainter} is contractually forbidden to paint outside it. Labels get
 * their own measured column, so hiding them removes the space instead of leaving a gap.
 */
public final class HudRenderer {
    private static final int MARGIN = 8;
    private static final int HOTBAR_HEIGHT = 22;
    private static final int LINE = 9;
    private static final int Z_OVERLAY = 200;
    /** Where a slot's name sits relative to its plate: every layout but the ring uses BELOW. */
    public static final int LABEL_BELOW = 0;
    public static final int LABEL_ABOVE = 1;
    public static final int LABEL_RIGHT = 2;
    public static final int LABEL_LEFT = 3;
    /** Gap between stacked panels in the design preview. */
    private static final int PREVIEW_GAP = 8;
    /** Used only when a config carries no panels at all. */
    private static final HudCluster FALLBACK_CLUSTER = new HudCluster();

    private static final Map<String, Long> FIRST_SEEN = new HashMap<>();
    private static final Map<Item, CooldownTracker> COOLDOWNS = new HashMap<>();
    private static final List<SlotBounds> SLOT_BOUNDS = new ArrayList<>();
    /** Screen rectangle per cluster id, refreshed every frame. */
    private static final Map<String, int[]> CLUSTER_BOUNDS = new java.util.LinkedHashMap<>();

    private static int panelX;
    private static int panelY;
    private static int panelW;
    private static int panelH;
    private static String hoveredKey;
    private static String selectedKey;
    /** Preview mode shows a fake active cooldown so every CD style is visible while designing. */
    private static boolean previewMode;
    /** Only the bar tabs ask for the fake layers; other tabs preview the bare HUD. */
    private static boolean previewLayerDemos;
    /** Frozen unless the motion tab asks for a live demo, so a style preview never blinks. */
    private static boolean previewDemoAnimated;
    /** Frozen clock for those fake layers, so a style demo never blinks on an untouched slot. */
    private static final long DEMO_CLOCK = 1000L;
    private static String previewCooldownKey;
    private static String previewAbilityKey;

    private HudRenderer() {
    }

    public static void render(GuiGraphics graphics, List<HudSlot> input, HudConfig cfg,
                              boolean editor, int mouseX, int mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        long now = System.currentTimeMillis();
        hoveredKey = editor ? findHovered(mouseX, mouseY) : null;
        SLOT_BOUNDS.clear();
        CLUSTER_BOUNDS.clear();

        int bottomReserve = editor ? 60 : 0;
        int margin = cfg.screenMargin >= 0 ? cfg.screenMargin : MARGIN;
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();

        boolean first = true;
        // A config that was mutated through the API may not have been sanitised yet; drawing one
        // implicit panel is exactly the pre-cluster behaviour.
        List<HudCluster> panels = cfg.clusters == null || cfg.clusters.isEmpty()
                ? List.of(FALLBACK_CLUSTER) : cfg.clusters;
        for (HudCluster cluster : panels) {
            if (cluster == null || !cluster.enabled) {
                continue;
            }
            HudConfig view = cluster.resolve(cfg);
            List<HudSlot> slots = slotsFor(input, cfg, cluster, view);
            if (slots.isEmpty()) {
                continue;
            }

            HudStyle style = view.style();
            HudTheme theme = HudTheme.of(view);
            Layout layout = buildLayout(minecraft, style, theme, slots, view);

            float scale = view.effectiveScale(minecraft.getWindow().getGuiScale());
            int availableHeight = Math.max(48, screenHeight - bottomReserve - (editor ? 8 : 20));
            int availableWidth = Math.max(48, screenWidth - margin * 2);
            float fit = Math.min(1.0f, Math.min(availableWidth / (float) layout.width,
                    availableHeight / (float) layout.height));
            scale *= fit;

            int width = Math.round(layout.width * scale);
            int height = Math.round(layout.height * scale);
            int[] origin = origin(view, screenWidth, screenHeight, width, height, bottomReserve,
                    margin);
            int panelX = origin[0];
            int panelY = origin[1];

            CLUSTER_BOUNDS.put(cluster.id, new int[]{panelX, panelY, width, height});
            if (first) {
                HudRenderer.panelX = panelX;
                HudRenderer.panelY = panelY;
                HudRenderer.panelW = width;
                HudRenderer.panelH = height;
                first = false;
            }

            for (Cell cell : layout.cells) {
                cell.hovered = cell.slot.key().equals(hoveredKey);
                cell.featured = !view.featuredSlot.isEmpty()
                        && view.featuredSlot.equals(cell.slot.key());
                cell.selected = selectedKey != null && selectedKey.equals(cell.slot.key());
                float alpha = view.animations ? appearAlpha(cell.slot.key(), now) : 1.0f;
                cell.slide = Math.round((1.0f - alpha) * 4.0f);
            }

            graphics.pose().pushPose();
            try {
                graphics.pose().translate(panelX, panelY, 0);
                graphics.pose().scale(scale, scale, 1);

                drawScene(graphics, minecraft, layout, style, theme, view, now);
            } finally {
                graphics.pose().popPose();
            }

            for (Cell cell : layout.cells) {
                SLOT_BOUNDS.add(new SlotBounds(cell.slot.key(),
                        panelX + Math.round(cell.x * scale),
                        panelY + Math.round(cell.y * scale),
                        Math.round(cell.width * scale),
                        Math.round(cell.height * scale),
                        cell.slot.stack()));
            }

            Set<String> seen = new HashSet<>();
            for (Cell cell : layout.cells) {
                seen.add(cell.slot.key());
            }
            FIRST_SEEN.keySet().removeIf(key -> !seen.contains(key));
        }

        if (first) {
            return;
        }

        if (editor) {
            for (int[] rect : CLUSTER_BOUNDS.values()) {
                graphics.renderOutline(rect[0] - 2, rect[1] - 2, rect[2] + 4, rect[3] + 4,
                        0xAAFFFFFF);
                graphics.renderOutline(rect[0] - 3, rect[1] - 3, rect[2] + 6, rect[3] + 6,
                        0x557C9EFF);
            }
            if (cfg.tooltips && hoveredKey != null) {
                for (SlotBounds bounds : SLOT_BOUNDS) {
                    if (bounds.key.equals(hoveredKey) && bounds.stack != null
                            && !bounds.stack.isEmpty()) {
                        graphics.renderTooltip(minecraft.font, bounds.stack, mouseX, mouseY);
                        break;
                    }
                }
            }
        }
    }

    /** Slots a cluster draws: the global filter first, then cluster membership. */
    private static List<HudSlot> slotsFor(List<HudSlot> input, HudConfig cfg, HudCluster cluster,
                                          HudConfig view) {
        List<HudSlot> prepared = prepare(input, view);
        if (cfg.clusters.size() <= 1) {
            return prepared;
        }
        List<HudSlot> out = new ArrayList<>(prepared.size());
        for (HudSlot slot : prepared) {
            if (cfg.clusterFor(slot.key()) == cluster) {
                out.add(slot);
            }
        }
        return out;
    }

    /**
     * Draws the real HUD, unchanged, inside an arbitrary rectangle. Used by the design screen so
     * the preview is the same code path as the game overlay - never a hand-drawn mock.
     */
    public record PreviewSlot(String key, int x, int y, int w, int h) {
    }

    public static List<PreviewSlot> renderInRect(GuiGraphics graphics, List<HudSlot> slots,
                                                 HudConfig cfg, int x, int y, int w, int h) {
        return renderInRect(graphics, slots, cfg, null, x, y, w, h);
    }

    /**
     * {@code soloCluster} draws just that panel and uses the config as given, so the design screen
     * previews the look being edited instead of the panel's last saved snapshot. {@code null}
     * stacks every panel.
     */
    public static List<PreviewSlot> renderInRect(GuiGraphics graphics, List<HudSlot> slots,
                                                 HudConfig cfg, String soloCluster,
                                                 int x, int y, int w, int h) {
        if (w <= 8 || h <= 8) {
            return List.of();
        }
        Minecraft minecraft = Minecraft.getInstance();

        List<HudCluster> clusters = new ArrayList<>();
        List<HudConfig> views = new ArrayList<>();
        List<Layout> layouts = new ArrayList<>();
        int totalW = 0;
        int totalH = 0;
        for (HudCluster cluster : cfg.clusters) {
            if (cluster == null || !cluster.enabled) {
                continue;
            }
            if (soloCluster != null && !soloCluster.equals(cluster.id)) {
                continue;
            }
            HudConfig view = soloCluster != null ? cfg : cluster.resolve(cfg);
            List<HudSlot> drawable = slotsFor(slots, cfg, cluster, view);
            if (drawable.isEmpty()) {
                continue;
            }
            Layout layout = buildLayout(minecraft, view.style(), HudTheme.of(view), drawable, view);
            totalW = Math.max(totalW, layout.width);
            totalH += layout.height + (layouts.isEmpty() ? 0 : PREVIEW_GAP);
            clusters.add(cluster);
            views.add(view);
            layouts.add(layout);
        }
        if (layouts.isEmpty()) {
            return List.of();
        }

        float scale = Math.min(1.0f, Math.min((w - 6) / (float) Math.max(1, totalW),
                (h - 6) / (float) Math.max(1, totalH)));
        int blockW = Math.round(totalW * scale);
        int blockH = Math.round(totalH * scale);
        int blockX = x + (w - blockW) / 2;
        int blockY = y + (h - blockH) / 2;

        long now = System.currentTimeMillis();
        previewMode = true;
        List<PreviewSlot> bounds = new ArrayList<>();
        int cursorY = 0;
        try {
            for (int i = 0; i < layouts.size(); i++) {
                Layout layout = layouts.get(i);
                HudConfig view = views.get(i);
                for (Cell cell : layout.cells) {
                    cell.hovered = false;
                    cell.featured = false;
                    cell.selected = false;
                    cell.slide = 0;
                }
                // The fake layers belong to the slot being edited; with none selected they sit on
                // the first two slots so every style still has a home.
                String demoKey = null;
                for (Cell cell : layout.cells) {
                    if (selectedKey != null && selectedKey.equals(cell.slot.key())) {
                        demoKey = selectedKey;
                        break;
                    }
                }
                previewCooldownKey = demoKey != null ? demoKey
                        : !layout.cells.isEmpty() ? layout.cells.get(0).slot.key() : null;
                previewAbilityKey = demoKey != null ? demoKey
                        : layout.cells.size() > 1 ? layout.cells.get(1).slot.key()
                        : previewCooldownKey;

                int py = blockY + Math.round(cursorY * scale);
                graphics.pose().pushPose();
                try {
                    graphics.pose().translate(blockX, py, 0);
                    graphics.pose().scale(scale, scale, 1);
                    drawScene(graphics, minecraft, layout, view.style(), HudTheme.of(view), view,
                            now);
                } finally {
                    graphics.pose().popPose();
                }
                for (Cell cell : layout.cells) {
                    bounds.add(new PreviewSlot(cell.slot.key(),
                            blockX + Math.round(cell.slotX * scale),
                            py + Math.round(cell.slotY * scale),
                            Math.round(cell.slotSide * scale),
                            Math.round(cell.slotSide * scale)));
                }
                cursorY += layout.height + PREVIEW_GAP;
            }
        } finally {
            previewMode = false;
        }
        return bounds;
    }

    public static String selectedKey() {
        return selectedKey;
    }

    /** Combined size of every panel, for the design screen readout. */
    public static int[] measure(List<HudSlot> slots, HudConfig cfg) {
        return measure(slots, cfg, null);
    }

    /** Size of one panel in {@code soloCluster} mode, or of every panel when it is null. */
    public static int[] measure(List<HudSlot> slots, HudConfig cfg, String soloCluster) {
        // The preview must show exactly what the HUD shows, including ghost frames only when
        // the Empty switch is on.
        Minecraft minecraft = Minecraft.getInstance();
        int width = 0;
        int height = 0;
        for (HudCluster cluster : cfg.clusters) {
            if (cluster == null || !cluster.enabled) {
                continue;
            }
            if (soloCluster != null && !soloCluster.equals(cluster.id)) {
                continue;
            }
            HudConfig view = soloCluster != null ? cfg : cluster.resolve(cfg);
            List<HudSlot> drawable = slotsFor(slots, cfg, cluster, view);
            if (drawable.isEmpty()) {
                continue;
            }
            Layout layout = buildLayout(minecraft, view.style(), HudTheme.of(view), drawable, view);
            width = Math.max(width, layout.width);
            height += layout.height + (height == 0 ? 0 : PREVIEW_GAP);
        }
        return new int[]{width, height};
    }

    /** Draws the whole HUD in local coordinates; the caller owns translate/scale. */
    private static void drawScene(GuiGraphics graphics, Minecraft minecraft, Layout layout,
                                  HudStyle style, HudTheme theme, HudConfig cfg, long now) {
        StylePainter.panel(style, theme, graphics, 0, 0, layout.width, layout.height,
                cfg.opacity);

            for (GroupBox box : layout.groups) {
                StylePainter.roundRect(graphics, box.x, box.y, box.width, box.height, 6,
                        0xE0101014);
                StylePainter.outline(graphics, box.x, box.y, box.width, box.height, 6,
                        StylePainter.alpha(box.tint, 0.8f));
                graphics.drawString(minecraft.font,
                        box.label, box.x + 6, box.y + 5,
                        StylePainter.alpha(box.tint, 1.0f), false);
            }
            if (theme.topBar > 0) {
                String title = com.toancao.loadouttc.util.UiText.s("Accessories");
                int titleWidth = minecraft.font.width(title);
                graphics.drawString(minecraft.font, title, (layout.width - titleWidth) / 2, 4,
                        theme.text, true);
                int ruleY = Math.max(12, theme.topBar - 3);
                graphics.fill(5, ruleY, layout.width - 5, ruleY + 1,
                        StylePainter.alpha(theme.accent, 0.45f));
            }
            for (Header header : layout.headers) {
                StylePainter.header(style, theme, graphics, minecraft.font, header.label, header.x,
                        header.y, header.width, cfg.opacity, header.tint);
            }

            // 1. All plates first: a plate can never land on a neighbouring label.
            for (Cell cell : layout.cells) {
                boolean underline = !hasGauge(cell.slot, cfg);
                StylePainter.slot(style, theme, graphics, plateX(cell), plateY(cell),
                        plateW(cell), plateH(cell), cell.hovered, underline, cfg.opacity,
                        cell.tint);
            }
            // 2. Icons and their effects.
            for (Cell cell : layout.cells) {
                drawIcon(graphics, minecraft, theme, cell, cfg);
            }
            // 3. Count / cooldown / durability, above the 3D model.
            for (Cell cell : layout.cells) {
                drawOverlays(graphics, minecraft, style, theme, cell, cfg, now);
            }
            // 4. Labels last among content so nothing can cover them.
            for (Cell cell : layout.cells) {
                drawLabel(graphics, minecraft, theme, cell, cfg, now);
            }
            // 5. Focus rings on top of everything.
            for (Cell cell : layout.cells) {
                drawFocus(graphics, style, theme, cell);
            }
            if (cfg.debugGuides) {
                drawGuides(graphics, minecraft, layout, cfg);
            }    }
    /** Screen-space rectangle of the first panel, for callers that only need one. */
    public static int[] bounds() {
        return new int[]{panelX, panelY, panelW, panelH};
    }

    /** Screen-space rectangle of one cluster, or an empty rect when it is not drawn. */
    public static int[] bounds(String clusterId) {
        int[] rect = CLUSTER_BOUNDS.get(clusterId);
        return rect == null ? new int[]{0, 0, 0, 0} : rect.clone();
    }

    /** Id of the panel under the cursor, or null. Used by the editor to drag one panel. */
    public static String clusterAt(int mouseX, int mouseY) {
        for (Map.Entry<String, int[]> entry : CLUSTER_BOUNDS.entrySet()) {
            int[] rect = entry.getValue();
            if (rect != null && mouseX >= rect[0] && mouseX < rect[0] + rect[2]
                    && mouseY >= rect[1] && mouseY < rect[1] + rect[3]) {
                return entry.getKey();
            }
        }
        return null;
    }

    /** Screen rectangle of one slot, or an empty rect when it is not drawn. */
    public static int[] slotBounds(String slotKey) {
        for (SlotBounds bounds : SLOT_BOUNDS) {
            if (bounds.key.equals(slotKey)) {
                return new int[]{bounds.x, bounds.y, bounds.w, bounds.h};
            }
        }
        return new int[]{0, 0, 0, 0};
    }

    /** Union of every drawn panel, so an integration can avoid the whole HUD. */
    public static int[] hudBounds() {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (int[] rect : CLUSTER_BOUNDS.values()) {
            if (rect == null || rect[2] <= 0) {
                continue;
            }
            minX = Math.min(minX, rect[0]);
            minY = Math.min(minY, rect[1]);
            maxX = Math.max(maxX, rect[0] + rect[2]);
            maxY = Math.max(maxY, rect[1] + rect[3]);
        }
        if (minX == Integer.MAX_VALUE) {
            return new int[]{0, 0, 0, 0};
        }
        return new int[]{minX, minY, maxX - minX, maxY - minY};
    }

    /** The slot currently selected in the editor, drawn with a white/cyan focus ring. */
    public static void setSelection(String slotKey) {
        selectedKey = slotKey;
    }

    /**
     * Asks the design preview for the fake cooldown/ability layers. They are only meaningful while
     * the bar settings are on screen; every other tab previews the bare HUD the player really has.
     * They stay frozen unless the motion tab asks for a live demo.
     */
    public static void setPreviewDemos(boolean enabled, boolean animated) {
        previewLayerDemos = enabled;
        previewDemoAnimated = animated;
    }

    // ---- Layout ----
    private static Layout buildLayout(Minecraft minecraft, HudStyle style, HudTheme theme,
                                      List<HudSlot> slots, HudConfig cfg) {
        Font font = minecraft.font;
        int icon = cfg.iconSize;
        int pad = cfg.panelPadding >= 0 ? cfg.panelPadding
                : Math.max(4, theme.minPad);
        int gap = cfg.rowGap >= 0 ? cfg.rowGap : Math.max(2, theme.minGap);
        int labelGap = cfg.labelGap >= 0 ? cfg.labelGap : theme.labelGap;
        boolean labels = cfg.labels || theme.numberRows;

        int maxReserve = 0;
        int maxRingDepth = 0;
        int maxFrameDepth = 0;
        int durText = 0;
        boolean anyCooldownBar = false;
        // A timed layer claims plate space only while something is actually ticking, otherwise
        // every HUD would carry a dead gap for an ability the player never has.
        abilityLayerLive = previewLayerDemos || hasAbilityTimer(slots);
        for (HudSlot slot : slots) {
            boolean gauge = cfg.showsDurabilityFor(slot.key()) && !theme.numberRows;
            if (gauge) {
                DurabilityStyle ds = cfg.durabilityStyleFor(slot.key());
                maxReserve = Math.max(maxReserve,
                        durabilityBarHeight(ds, cfg.durabilityOptions));
                ItemStack stack = slot.stack();
                if (stack != null && stack.isDamageableItem() && stack.getMaxDamage() > 0) {
                    durText = Math.max(durText, durabilityTextWidth(font, ds, stack));
                }
            }
            int ringDepth = 0;
            if (gauge && cfg.durabilityStyleFor(slot.key()) == DurabilityStyle.RING) {
                ringDepth += barThickness(cfg.durabilityOptions) + 1;
            }
            if (cfg.showsCooldownsFor(slot.key())) {
                CooldownStyle cd = cfg.cooldownStyleFor(slot.key());
                if (cd == CooldownStyle.ARC) {
                    ringDepth += barThickness(cfg.cooldownOptions) + 1;
                }
                if (cd == CooldownStyle.FRAME) {
                    maxFrameDepth = Math.max(maxFrameDepth, barThickness(cfg.cooldownOptions));
                }
                anyCooldownBar |= cd == CooldownStyle.BAR;
            }
            if (abilityRingFor(cfg)) {
                ringDepth += barThickness(cfg.abilityOptions) + 1;
            }
            if (abilityLayerLive && cfg.abilityStyle() == AbilityStyle.FRAME) {
                maxFrameDepth = Math.max(maxFrameDepth, barThickness(cfg.abilityOptions));
            }
            maxRingDepth = Math.max(maxRingDepth, ringDepth);
        }
        // Bars stack under the plate, one line each, so no layer can ever land on another. The
        // layout reserves the whole stack and the name moves below it.
        int coolBarH = anyCooldownBar ? barThickness(cfg.cooldownOptions) : 0;
        int abilBarH = abilityBarFor(cfg) ? barThickness(cfg.abilityOptions) : 0;
        int barStack = maxReserve + (coolBarH > 0 ? coolBarH + 1 : 0)
                + (abilBarH > 0 ? abilBarH + 1 : 0);
        // The strip also owns the 3 px lead gap between the plate and the first row.
        int barsExtra = barStack > 0 ? barStack + 3 : 0;
        int inset = cfg.slotInset >= 0 ? cfg.slotInset : Math.max(theme.slotInset, 0);
        inset = Math.max(inset, Math.min(4, theme.borderWidth) + 1);
        // A frame band is painted inward from the plate edge, so the icon has to start past it or
        // a thick timer would eat the item art.
        int frameInset = maxFrameDepth > 0 ? maxFrameDepth + 1 : 0;
        inset = Math.max(inset, frameInset);
        // A ring has to pass around the icon, never over it: the band grows by the real thickness
        // of every ring that this HUD actually draws.
        int ringInset = maxRingDepth == 0 ? 0 : maxRingDepth + 3;
        inset = Math.max(inset, ringInset);
        // Auto means "the same box as a vanilla hotbar slot": 16 px of item inside a 20 px cell.
        // S / M / L / Custom keep the player's icon size and inset.
        if ("auto".equals(cfg.size == null ? "auto" : cfg.size)) {
            icon = Math.min(icon, 16);
            inset = Math.max(2, Math.max(ringInset, frameInset));
        }
        int slotSide = icon + inset * 2;
        int labelWidth = labels ? labelWidth(font, slots, cfg, theme) : 0;
        int lineH = LINE;

        // Dock and dynamic island are rows by nature; Grid/Circle still win when chosen.
        String layoutKind = cfg.layoutKind();
        if ("vertical".equals(layoutKind) && (style.kind() == HudStyle.Kind.DOCK
                || style == HudStyle.DYNAMIC_ISLAND)) {
            layoutKind = "horizontal";
        }

        List<Cell> cells = new ArrayList<>();
        List<Header> headers = new ArrayList<>();
        List<GroupBox> groups = new ArrayList<>();
        int width;
        int height;

        if (style.kind() == HudStyle.Kind.GRID) {
            int count = slots.size();
            int columns = count <= 1 ? 1 : count <= 4 ? 2 : 3;
            int gridPad = Math.max(4, inset);
            int labelBlock = labels ? labelBlock(lineH) : 0;
            int cardW = Math.max(Math.max(slotSide, labels ? labelWidth : 0), durText + 6)
                    + gridPad * 2;
            int cardH = gridPad * 2 + slotSide + labelBlock + barsExtra;

            for (int i = 0; i < count; i++) {
                int column = i % columns;
                int row = i / columns;
                int x = pad + column * (cardW + gap);
                int y = pad + row * (cardH + gap);
                Cell cell = new Cell(slots.get(i), x, y, cardW, cardH);
                cell.slotX = x + (cardW - slotSide) / 2;
                cell.slotY = y + gridPad;
                cell.slotSide = slotSide;
                cell.label = truncate(font, labelText(slots.get(i), cfg, theme),
                        cfg.maxLabelChars, cardW - gridPad * 2);
                cell.labelX = x + (cardW - font.width(cell.label)) / 2;
                cell.durY = cell.slotY + slotSide + 3;
                cell.labelBelow = true;
                cells.add(cell);
            }
            width = pad * 2 + columns * cardW + (columns - 1) * gap;
            height = pad * 2 + ((count + columns - 1) / columns) * cardH
                    + (((count + columns - 1) / columns) - 1) * gap;
        } else if ("grid".equals(layoutKind)) {
            int labelBlock = labels ? labelBlock(lineH) : 0;
            int cellW = Math.max(Math.max(slotSide, labels ? labelWidth : 0) + 4, durText + 6);
            int cellH = slotSide + labelBlock + barsExtra;
            int count = slots.size();
            int columns = Math.max(1, Math.min(cfg.columns, count));
            int rows = (count + columns - 1) / columns;
            for (int i = 0; i < count; i++) {
                int column = i % columns;
                int row = i / columns;
                int x = pad + column * (cellW + gap);
                int y = pad + row * (cellH + gap);
                Cell cell = new Cell(slots.get(i), x, y, cellW, cellH);
                cell.slotX = x + (cellW - slotSide) / 2;
                cell.slotY = y;
                cell.slotSide = slotSide;
                cell.label = truncate(font, labelText(slots.get(i), cfg, theme),
                        cfg.maxLabelChars, cellW - 4);
                cell.labelX = x + (cellW - font.width(cell.label)) / 2;
                cell.durY = y + slotSide + 3;
                cell.labelBelow = true;
                cells.add(cell);
            }
            width = pad * 2 + columns * cellW + (columns - 1) * gap;
            height = pad * 2 + rows * cellH + (rows - 1) * gap;
        } else if ("circle".equals(layoutKind)) {
            int count = slots.size();
            int radius = count <= 1 ? 0 : Math.max(12, cfg.circleRadius);
            // Names leave the ring on the outside: the two ends of the circle name themselves
            // above/below, everything else beside its plate. Centring every name under its own
            // plate can never work on a ring - eight names need more room than the circle's
            // circumference has, so neighbours always ran together.
            int belowOffset = slotSide + (barStack > 0 ? barsExtra : 3);
            int[] xs = new int[count];
            int[] ys = new int[count];
            int[] sides = new int[count];
            String[] texts = new String[count];
            int[] textWidths = new int[count];
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            for (int i = 0; i < count; i++) {
                double angle = -Math.PI / 2.0 + i * (Math.PI * 2.0 / count);
                int dx = count <= 1 ? 0 : (int) Math.round(Math.cos(angle) * radius);
                int dy = count <= 1 ? 0 : (int) Math.round(Math.sin(angle) * radius);
                xs[i] = dx - slotSide / 2;
                ys[i] = dy - slotSide / 2;
                sides[i] = labels ? ringLabelSide(dx, dy, count, slotSide) : LABEL_BELOW;
                texts[i] = truncate(font, labelText(slots.get(i), cfg, theme),
                        cfg.maxLabelChars, Math.max(slotSide, labelWidth));
                textWidths[i] = font.width(texts[i]);

                // The plate, the gauge stack and the name all share the panel bounds, so a name
                // is never clamped back over a neighbour's plate.
                int left = xs[i];
                int top = ys[i];
                int right = xs[i] + slotSide;
                int bottom = ys[i] + slotSide + barsExtra;
                if (durText > 0) {
                    left = Math.min(left, xs[i] + (slotSide - durText) / 2);
                    right = Math.max(right, xs[i] + (slotSide + durText) / 2);
                }
                int textX = xs[i] + (slotSide - textWidths[i]) / 2;
                if (labels) {
                    switch (sides[i]) {
                        case LABEL_ABOVE -> {
                            left = Math.min(left, textX);
                            right = Math.max(right, textX + textWidths[i]);
                            top = Math.min(top, ys[i] - lineH - 2);
                        }
                        case LABEL_RIGHT -> right = Math.max(right,
                                xs[i] + slotSide + labelGap + textWidths[i]);
                        case LABEL_LEFT -> left = Math.min(left,
                                xs[i] - labelGap - textWidths[i]);
                        default -> {
                            left = Math.min(left, textX);
                            right = Math.max(right, textX + textWidths[i]);
                            bottom = Math.max(bottom, ys[i] + belowOffset + lineH);
                        }
                    }
                }
                minX = Math.min(minX, left);
                minY = Math.min(minY, top);
                maxX = Math.max(maxX, right);
                maxY = Math.max(maxY, bottom);
            }
            int shiftX = pad - minX;
            // A name above the ring needs room under a panel title bar as well.
            int shiftY = pad + theme.topBar - minY;
            int cellH = slotSide + (labels ? labelBlock(lineH) : 0) + barsExtra;
            for (int i = 0; i < count; i++) {
                int x = xs[i] + shiftX;
                int y = ys[i] + shiftY;
                Cell cell = new Cell(slots.get(i), x, y, slotSide, cellH);
                cell.slotX = x;
                cell.slotY = y;
                cell.slotSide = slotSide;
                cell.label = texts[i];
                cell.labelX = x + (slotSide - textWidths[i]) / 2;
                cell.labelSide = sides[i];
                cell.labelBelow = sides[i] == LABEL_BELOW;
                cell.durY = y + slotSide + 3;
                cells.add(cell);
            }
            width = pad * 2 + (maxX - minX);
            height = pad * 2 + theme.topBar + (maxY - minY);
        } else if ("horizontal".equals(layoutKind)) {
            int labelBlock = labels ? labelBlock(lineH) : 0;
            int cellW = Math.max(Math.max(slotSide, labels ? labelWidth : 0) + 4, durText + 6);
            int cellH = slotSide + labelBlock + barsExtra;
            int x = pad;
            for (HudSlot slot : slots) {
                Cell cell = new Cell(slot, x, pad, cellW, cellH);
                cell.slotX = x + (cellW - slotSide) / 2;
                cell.slotY = pad;
                cell.slotSide = slotSide;
                cell.label = truncate(font, labelText(slot, cfg, theme), cfg.maxLabelChars,
                        cellW - 4);
                cell.labelX = x + (cellW - font.width(cell.label)) / 2;
                cell.durY = pad + slotSide + 3;
                cell.labelBelow = true;
                cells.add(cell);
                x += cellW + gap;
            }
            width = pad * 2 + slots.size() * cellW + (slots.size() - 1) * gap;
            height = cellH + pad * 2;
        } else if (theme.alwaysHeaders) {
            // One framed box per source, with a small grid inside, like an accessories tab split
            // into Accessories / Seals / Artifacts.
            int cols = 2;
            int cellW = Math.max(slotSide, durText + 4);
            int cellH = slotSide + (labels ? labelBlock(lineH) : 0) + barsExtra;
            int boxPad = 6;
            int headerH = 15;
            int boxGap = 8;
            int boxW = cols * cellW + (cols - 1) * gap + boxPad * 2;
            int cursor = pad;

            java.util.LinkedHashMap<String, java.util.List<HudSlot>> bySource =
                    new java.util.LinkedHashMap<>();
            for (HudSlot slot : slots) {
                bySource.computeIfAbsent(slot.source(), ignored -> new ArrayList<>()).add(slot);
            }

            for (var entry : bySource.entrySet()) {
                int tint = groupTint(entry.getKey(), theme.accent);
                String label = prettySource(entry.getKey());
                List<HudSlot> members = entry.getValue();
                int rows = (members.size() + cols - 1) / cols;
                int boxH = boxPad * 2 + headerH + rows * cellH + (rows - 1) * gap;
                groups.add(new GroupBox(pad, cursor, boxW, boxH, tint, label));

                for (int i = 0; i < members.size(); i++) {
                    HudSlot slot = members.get(i);
                    int col = i % cols;
                    int row = i / cols;
                    int cellX = pad + boxPad + col * (cellW + gap);
                    int cellY = cursor + boxPad + headerH + row * (cellH + gap);
                    Cell cell = new Cell(slot, cellX, cellY, cellW, cellH);
                    cell.tint = tint;
                    cell.slotX = cellX;
                    cell.slotY = cellY;
                    cell.slotSide = slotSide;
                    cell.label = truncate(font, labelText(slot, cfg, theme),
                            cfg.maxLabelChars, cellW);
                    cell.labelX = cellX + (cellW - font.width(cell.label)) / 2;
                    cell.labelY = cellY + slotSide + 2;
                    cell.durY = cellY + slotSide + 3;
                    cell.labelBelow = true;
                    cells.add(cell);
                }
                cursor += boxH + boxGap;
            }

            width = pad * 2 + boxW;
            height = Math.max(8, cursor - boxGap + pad);        } else {
            int rowW = slotSide + (labels ? labelGap + labelWidth : 0);
            int rowH = slotSide + barsExtra;
            int cursor = pad + theme.topBar;
            String lastGroup = null;
            int contentWidth = rowW;

            for (HudSlot slot : slots) {
                // Headers are keyed on the label; name/custom sorting interleaves groups.
                String groupName = slot.groupLabel() == null ? "" : slot.groupLabel();
                boolean grouped = !"name".equals(cfg.sortMode) && !"custom".equals(cfg.sortMode);
                boolean newGroup = cfg.groupHeaders && labels && "item".equals(cfg.labelMode)
                        && grouped && !groupName.equals(lastGroup) && !groupName.isBlank();
                if (newGroup) {
                    String label = groupName;
                    int headerWidth = font.width(label) + 16;
                    contentWidth = Math.max(contentWidth, headerWidth);
                    headers.add(new Header(label, pad, cursor, headerWidth,
                            groupTint(slot.source(), theme.accent)));
                    cursor += 14;
                    lastGroup = groupName;
                }
                Cell cell = new Cell(slot, pad, cursor, rowW, rowH);
                cell.tint = groupTint(slot.source(), theme.accent);
                cell.slotX = pad;
                cell.slotY = cursor;
                cell.slotSide = slotSide;
                cell.label = truncate(font, labelText(slot, cfg, theme), cfg.maxLabelChars,
                        labelWidth);
                cell.labelX = pad + slotSide + labelGap;
                cell.labelY = cursor + (slotSide - 8) / 2;
                cell.durY = cursor + slotSide + 3;
                cells.add(cell);
                cursor += rowH + gap;
            }

            if (durText > 0) {
                // The readout is centred on the slot, so half of the overflow sits on each side.
                contentWidth = Math.max(contentWidth, (slotSide + durText) / 2 + 2);
            }
            for (Cell cell : cells) {
                cell.width = contentWidth;
            }
            if (theme.numberRows) {
                for (Cell cell : cells) {
                    cell.labelX = cell.x + contentWidth - font.width(cell.label);
                }
            }
            for (Header header : headers) {
                header.width = contentWidth;
            }
            width = contentWidth + pad * 2;
            height = cursor - gap + pad;
        }

        // Single source of truth for the anchors: icon centre, label baseline and gauge all derive
        // from the slot box, then the user nudges are applied once.
        for (Cell cell : cells) {
            cell.iconSize = icon;
            cell.iconX = cell.slotX + (cell.slotSide - icon) / 2 + cfg.iconNudgeX;
            cell.iconY = cell.slotY + (cell.slotSide - icon) / 2 + cfg.iconNudgeY;
            cell.durY += cfg.gaugeNudgeY;
            // Below-the-plate names all start at the same offset (the bottom of the gauge stack),
            // so two slots never print their names on different lines just because one of them
            // happens to carry a gauge and the other does not.
            int underGauge = cell.durY + barStack;
            switch (cell.labelSide) {
                case LABEL_ABOVE -> cell.labelY = cell.slotY - lineH - 2;
                case LABEL_RIGHT -> {
                    cell.labelX = cell.slotX + cell.slotSide + labelGap;
                    cell.labelY = cell.slotY + (cell.slotSide - 8) / 2;
                }
                case LABEL_LEFT -> {
                    cell.labelX = cell.slotX - labelGap - font.width(cell.label);
                    cell.labelY = cell.slotY + (cell.slotSide - 8) / 2;
                }
                default -> cell.labelY = cell.labelBelow ? underGauge
                        : cell.iconY + (icon - 8) / 2;
            }
            cell.labelX += cfg.labelNudgeX;
            cell.labelY += cfg.labelNudgeY;
            // A name measured inside the bounds can still be pushed out by a nudge; keep it in.
            if (labels) {
                int maxX = Math.max(pad, width - pad - font.width(cell.label));
                cell.labelX = Math.max(pad, Math.min(cell.labelX, maxX));
            }
        }

        // The plate is the slot chrome, not the whole row: a list row owns a label column that must
        // stay transparent. Only the bento grid treats the entire card as one plate.
        for (Cell cell : cells) {
            cell.plateX = cell.slotX;
            cell.plateY = cell.slotY;
            cell.plateW = cell.slotSide;
            cell.plateH = cell.slotSide;
        }
        if (style.kind() == HudStyle.Kind.GRID) {
            for (Cell cell : cells) {
                cell.plateX = cell.x;
                cell.plateY = cell.y;
                cell.plateW = cell.width;
                cell.plateH = cell.height;
            }
        }

        return new Layout(width, Math.max(8, height), cells, headers, groups);
    }
    private static int plateW(Cell cell) {
        return cell.plateW;
    }

    private static int plateH(Cell cell) {
        return cell.plateH;
    }

    private static int plateX(Cell cell) {
        return cell.plateX;
    }

    private static int plateY(Cell cell) {
        return cell.plateY + cell.slide;
    }

    // ---- Passes ----
    private static void drawIcon(GuiGraphics graphics, Minecraft minecraft, HudTheme theme,
                                 Cell cell, HudConfig cfg) {
        int iconSize = cell.iconSize;
        int iconX = cell.iconX;
        int iconY = cell.iconY + cell.slide;
        EffectFrame effect = HudEffects.frame(cell.slot.key(), cell.slot.stack(), cell.featured,
                cell.hovered, cfg);

        if (effect.glowColor() != 0 && effect.glowStrength() > 0.0f) {
            // Layered halo: light falls off around the icon instead of reading as one flat box.
            float strength = Math.min(1.0f, effect.glowStrength());
            int spread = Math.max(1, cell.slotSide / 10);
            for (int layer = 3; layer >= 1; layer--) {
                int grow = spread * layer;
                StylePainter.roundRect(graphics, cell.slotX - grow, cell.slotY + cell.slide - grow,
                        cell.slotSide + grow * 2, cell.slotSide + grow * 2, 6 + grow,
                        StylePainter.alpha(effect.glowColor(), strength * 0.26f / layer));
            }
        }

        graphics.pose().pushPose();
        try {
            float centerX = iconX + iconSize / 2.0f;
            float centerY = iconY + iconSize / 2.0f;
            float scale = effect.scale();
            if (Math.abs(scale - 1.0f) > 0.001f) {
                graphics.pose().translate(centerX, centerY, 0);
                graphics.pose().scale(scale, scale, 1);
                graphics.pose().translate(-centerX, -centerY, 0);
            }
            graphics.pose().translate(effect.offsetX(), effect.offsetY(), 0);

            if (cell.slot.empty()) {
                drawGhost(graphics, theme, iconX, iconY, iconSize);
            } else if (effect.threeD()) {
                drawItem3D(graphics, minecraft, cell.slot.stack(), iconX, iconY, iconSize,
                        effect.spinDegrees());
            } else if (effect.spinDegrees() != 0.0f) {
                graphics.pose().pushPose();
                try {
                    graphics.pose().translate(centerX, centerY, 0);
                    graphics.pose().mulPose(Axis.ZP.rotationDegrees(effect.spinDegrees()));
                    graphics.pose().translate(-centerX, -centerY, 0);
                    drawItem(graphics, cell.slot.stack(), iconX, iconY, iconSize, 1.0f);
                } finally {
                    graphics.pose().popPose();
                }
            } else {
                float gain = 1.0f;
                if (cell.hovered && cfg.animations) {
                    int inset = (cell.slotSide - iconSize) / 2;
                    gain = 1.0f + Math.min(0.18f, 1.4f * inset / Math.max(1, iconSize));
                }
                drawItem(graphics, cell.slot.stack(), iconX, iconY, iconSize, gain);
            }
        } finally {
            graphics.pose().popPose();
        }
    }

    private static void drawOverlays(GuiGraphics graphics, Minecraft minecraft, HudStyle style,
                                     HudTheme theme, Cell cell, HudConfig cfg, long now) {
        if (cell.slot.empty()) {
            return;
        }
        String key = cell.slot.key();
        boolean demoCooldown = previewMode && previewLayerDemos && key.equals(previewCooldownKey);
        long demoTime = !previewDemoAnimated ? DEMO_CLOCK : now;

        AbilityTimers.Remaining ability = AbilityTimers.remaining(key);
        boolean demoAbility = false;
        if (ability == null && previewMode && previewLayerDemos && key.equals(previewAbilityKey)) {
            ability = new AbilityTimers.Remaining(0.62f, 6.2f, 0);
            demoAbility = true;
        }

        CooldownStyle cooldownStyle = cfg.showsCooldownsFor(key)
                ? cfg.cooldownStyleFor(key) : CooldownStyle.OFF;
        DurabilityStyle durabilityStyle = cfg.showsDurabilityFor(key) && !theme.numberRows
                ? cfg.durabilityStyleFor(key) : DurabilityStyle.OFF;
        int durabilityH = durabilityBarHeight(durabilityStyle, cfg.durabilityOptions);
        int cooldownH = cooldownStyle == CooldownStyle.BAR
                ? barThickness(cfg.cooldownOptions) : 0;

        // Rows grow downward under the plate; an absent layer closes its gap instead of leaving a
        // hole, and the reserve in the layout is a maximum so nothing can spill into the name.
        int durabilityY = cell.durY + cell.slide;
        int cooldownY = durabilityY + (durabilityH > 0 ? durabilityH + 1 : 0);
        int abilityY = cooldownY + (cooldownH > 0 ? cooldownH + 1 : 0);

        // Rings are packed from the plate edge inward: cooldown, then ability, then durability.
        // Each one claims its own thickness plus a pixel of air, so two bands can never share a
        // pixel. The order follows the settings, not what happens to be ticking right now, so a
        // ring never jumps when a timer starts.
        int cooldownRing = -1;
        int abilityRing = -1;
        int durabilityRing = -1;
        int ringCursor = 0;
        if (cooldownStyle == CooldownStyle.ARC) {
            cooldownRing = ringCursor;
            ringCursor += barThickness(cfg.cooldownOptions) + 1;
        }
        if (abilityRingFor(cfg)) {
            abilityRing = ringCursor;
            ringCursor += barThickness(cfg.abilityOptions) + 1;
        }
        if (durabilityStyle == DurabilityStyle.RING) {
            durabilityRing = ringCursor;
        }

        graphics.pose().pushPose();
        try {
            graphics.pose().translate(0, 0, Z_OVERLAY);

            int cooldownTint = cfg.cooldownTint(theme.accent);
            if (cooldownStyle == CooldownStyle.FRAME) {
                float remaining = demoCooldown ? 0.62f
                        : cooldownFraction(minecraft, cell.slot.stack());
                if (remaining > 0.0f) {
                    StylePainter.frameProgress(graphics, cell.slotX, cell.slotY + cell.slide,
                            cell.slotSide, cell.slotSide, remaining,
                            StylePainter.alpha(cooldownTint, cfg.cooldownOptions.alpha()),
                            cfg.cooldownOptions.track
                                    ? StylePainter.alpha(theme.muted, 0.20f) : 0,
                            theme.radiusSlot, theme.slotKind,
                            cfg.frameFlow, demoCooldown ? demoTime : now,
                            cfg.cooldownOptions.reverse,
                            barThickness(cfg.cooldownOptions));
                }
            } else if (cooldownStyle != CooldownStyle.OFF) {
                drawCooldown(graphics, minecraft, cell, theme, cooldownStyle,
                        demoCooldown ? demoTime : now, demoCooldown, cooldownTint,
                        cfg.cooldownOptions, cooldownRing, cooldownY);
            }

            if (ability != null) {
                drawAbility(graphics, minecraft, cell, theme, cfg, ability,
                        demoAbility ? demoTime : now, abilityRing, abilityY);
            }
            if (durabilityH > 0 && cell.slot.stack().isDamageableItem()) {
                drawDurability(graphics, minecraft, cell, theme, durabilityStyle, cfg, now,
                        durabilityY, durabilityRing);
            }
            // Count sits on top of every other overlay: it is the last thing the eye needs.
            if (cfg.stackCount && cell.slot.stack().getCount() > 1) {
                String count = String.valueOf(cell.slot.stack().getCount());
                graphics.drawString(minecraft.font, count,
                        cell.slotX + cell.slotSide - minecraft.font.width(count) - 1,
                        cell.slotY + cell.slide + cell.slotSide - 9, 0xFFFFFFFF, true);
            }
        } finally {
            graphics.pose().popPose();
        }
    }

    private static float cooldownFraction(Minecraft minecraft, ItemStack stack) {
        if (minecraft.player == null) {
            return -1.0f;
        }
        var cooldowns = minecraft.player.getCooldowns();
        Item item = stack.getItem();
        if (!cooldowns.isOnCooldown(item)) {
            return -1.0f;
        }
        return Mth.clamp(cooldowns.getCooldownPercent(item, 0.0f), 0.0f, 1.0f);
    }

    private static void drawAbility(GuiGraphics graphics, Minecraft minecraft, Cell cell,
                                    HudTheme theme, HudConfig cfg,
                                    AbilityTimers.Remaining ability, long now,
                                    int ringOffset, int barY) {
        AbilityStyle style = cfg.abilityStyle();
        if (style == AbilityStyle.OFF) {
            return;
        }
        LayerOptions opts = cfg.abilityOptions;
        int color = ability.color() != 0 ? ability.color() : cfg.abilityTint(theme.accent);
        float alpha = opts.alpha();
        int fade = opts.pulse && ability.fraction() <= 0.2f
                ? (int) (0.65f * 255) + (int) (0.35f * 255 * (0.5f + 0.5f
                * (float) Math.sin(now / 90.0))) : 255;
        fade = Math.round(fade * alpha);
        int tint = (fade << 24) | (color & 0xFFFFFF);
        String text = opts.format(ability.fraction(),
                String.format(java.util.Locale.ROOT, "%.1fs", ability.seconds()));
        int thickness = barThickness(opts);
        int centerX = cell.slotX + cell.slotSide / 2;
        int centerY = cell.slotY + cell.slide + cell.slotSide / 2;

        switch (style) {
            case RING -> {
                float radius = ringRadius(cell, theme, Math.max(0, ringOffset), thickness);
                drawArc(graphics, centerX, centerY, radius,
                        ability.fraction(), tint,
                        opts.track ? StylePainter.alpha(0x33FFFFFF, 1.0f) : 0,
                        thickness, opts.reverse);
                // Ability-only trait: four pulsing marks orbiting the ring.
                int halo = opts.pulse ? (int) (0.45f * fade) << 24 | (color & 0xFFFFFF) : 0;
                for (int i = 0; i < 4; i++) {
                    double angle = now / 420.0 + i * (Math.PI / 2.0);
                    int px = Math.round(centerX + (float) Math.cos(angle) * (radius + thickness));
                    int py = Math.round(centerY + (float) Math.sin(angle) * (radius + thickness));
                    graphics.fill(px, py, px + 1, py + 1, halo);
                }
            }
            case BAR -> {
                // Ability-only trait: a double line. The thin second row keeps ability bars
                // visually separate from the single hatched cooldown line.
                int x = cell.slotX + 1;
                int w = Math.max(1, cell.slotSide - 2);
                if (opts.track) {
                    graphics.fill(x, barY, x + w, barY + thickness,
                            StylePainter.alpha(0x66000000, 1.0f));
                }
                int filled = Math.round(w * ability.fraction());
                int fx = opts.reverse ? x + w - filled : x;
                if (filled > 0) {
                    graphics.fill(fx, barY, fx + filled, barY + 1, tint);
                    if (thickness > 1) {
                        graphics.fill(fx, barY + 1, fx + filled, barY + thickness,
                                StylePainter.alpha(tint, 0.45f));
                    }
                }
            }
            case TEXT -> {
                // An opt-in readout: it paints its own backing box on the icon, the same way the
                // cooldown text style does, so it never collides with the rows under the plate.
                int textWidth = minecraft.font.width(text);
                StylePainter.roundRect(graphics, cell.iconX - 1, cell.iconY + cell.slide - 1,
                        cell.iconSize + 2, cell.iconSize + 2, 4,
                        StylePainter.alpha(0xCC000000, 0.60f));
                graphics.drawString(minecraft.font, text,
                        centerX - textWidth / 2,
                        cell.iconY + cell.slide + (cell.iconSize - 8) / 2, tint, true);
            }
            case FRAME -> {
                StylePainter.frameProgress(graphics, cell.slotX, cell.slotY + cell.slide,
                        cell.slotSide, cell.slotSide, ability.fraction(), tint,
                        opts.track ? StylePainter.alpha(theme.muted, 0.18f) : 0,
                        theme.radiusSlot, theme.slotKind,
                        cfg.frameFlow, now, opts.reverse, thickness);
            }
            default -> {
            }
        }
    }

    private static void drawLabel(GuiGraphics graphics, Minecraft minecraft, HudTheme theme,
                                  Cell cell, HudConfig cfg, long now) {
        if (!cfg.labels && !theme.numberRows) {
            return;
        }
        int color = theme.numberRows ? readoutColor(cell.slot, theme)
                : theme.labelColor != 0 ? theme.labelColor : theme.muted;
        int x = cell.labelX;
        int y = cell.labelY + cell.slide;
        float wave = 0.5f + 0.5f * (float) Math.sin(now / 300.0);

        switch (cfg.labelEffect == null ? "none" : cfg.labelEffect) {
            case "glow" -> color = StylePainter.blend(color, theme.accent, 0.25f + 0.45f * wave);
            case "fade" -> color = StylePainter.alpha(color, 0.55f + 0.45f * wave);
            case "bob" -> y += Math.round((float) Math.sin(now / 260.0) * 1.2f);
            default -> {
            }
        }

        graphics.pose().pushPose();
        try {
            graphics.pose().translate(0, 0, Z_OVERLAY);
            if (theme.textKind == TextStyle.OUTLINE) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        if (dx != 0 || dy != 0) {
                            graphics.drawString(minecraft.font, cell.label, x + dx, y + dy,
                                    0xCC000000, false);
                        }
                    }
                }
                graphics.drawString(minecraft.font, cell.label, x, y, 0xFFFFFFFF, false);
            } else {
                graphics.drawString(minecraft.font, cell.label, x, y, color, theme.dark);
            }
        } finally {
            graphics.pose().popPose();
        }
    }

    /** Cyan slot box, yellow icon box, green text bounds, magenta gauge, white centre cross. */
    private static void drawGuides(GuiGraphics graphics, Minecraft minecraft, Layout layout,
                                   HudConfig cfg) {
        for (Cell cell : layout.cells) {
            graphics.renderOutline(cell.slotX, cell.slotY, cell.slotSide, cell.slotSide,
                    0xCC00E5FF);
            graphics.renderOutline(cell.iconX, cell.iconY, cell.iconSize, cell.iconSize,
                    0xCCFFD54A);
            int centerX = cell.iconX + cell.iconSize / 2;
            int centerY = cell.iconY + cell.iconSize / 2;
            graphics.fill(centerX, cell.slotY, centerX + 1, cell.slotY + cell.slotSide,
                    0x99FFFFFF);
            graphics.fill(cell.slotX, centerY, cell.slotX + cell.slotSide, centerY + 1,
                    0x99FFFFFF);
            if (!cell.label.isEmpty()) {
                int width = minecraft.font.width(cell.label);
                graphics.renderOutline(cell.labelX, cell.labelY, width, 8, 0xCC4ADE80);
            }
            graphics.fill(cell.iconX, cell.durY, cell.iconX + cell.iconSize, cell.durY + 1,
                    0xCCFF4AD8);
        }
    }

    private static void drawFocus(GuiGraphics graphics, HudStyle style, HudTheme theme,
                                  Cell cell) {
        if (!cell.hovered && !cell.featured && !cell.selected) {
            return;
        }
        if (!theme.focusRing) {
            // Hex plates need a hex ring; a square around them reads as a bug.
            int x = cell.plateX;
            int y = cell.plateY + cell.slide;
            if (cell.hovered) {
                StylePainter.hexOutline(graphics, x - 1, y - 1, cell.plateW + 2, cell.plateH + 2,
                        StylePainter.alpha(theme.accent, 0.55f));
            }
            if (cell.selected) {
                StylePainter.hexOutline(graphics, x - 2, y - 2, cell.plateW + 4, cell.plateH + 4,
                        StylePainter.alpha(0xFFFFFFFF, 0.95f));
            }
            return;
        }
        int x = cell.plateX;
        int y = cell.plateY + cell.slide;
        int w = cell.plateW;
        int h = cell.plateH;
        int r = theme.radiusSlot;
        if (cell.hovered && !cell.selected) {
            StylePainter.outline(graphics, x - 1, y - 1, w + 2, h + 2, r + 1,
                    StylePainter.alpha(theme.accent, 0.45f));
        }
        if (cell.featured) {
            StylePainter.outline(graphics, x - 2, y - 2, w + 4, h + 4, r + 2,
                    StylePainter.alpha(theme.accent, 0.85f));
        }
        if (cell.selected) {
            StylePainter.outline(graphics, x - 2, y - 2, w + 4, h + 4, r + 2,
                    StylePainter.alpha(0xFFFFFFFF, 0.95f));
            StylePainter.outline(graphics, x - 3, y - 3, w + 6, h + 6, r + 3,
                    StylePainter.alpha(0xFF7CDFFF, 0.85f));
        }
    }

    // ---- Item rendering ----
    private static void drawGhost(GuiGraphics graphics, HudTheme theme, int x, int y, int size) {
        StylePainter.outline(graphics, x + 3, y + 3, size - 6, size - 6, 3,
                StylePainter.alpha(theme.muted, 0.35f));
        int line = StylePainter.alpha(theme.muted, 0.55f);
        graphics.fill(x + size / 2, y + 6, x + size / 2 + 1, y + size - 6, line);
        graphics.fill(x + 6, y + size / 2, x + size - 6, y + size / 2 + 1, line);
    }

    private static void drawItem(GuiGraphics graphics, ItemStack stack, int x, int y, int size,
                                 float gain) {
        float scale = size / 16.0f * gain;
        if (gain > 1.0001f) {
            x -= Math.round((16 * scale - size) / 2);
            y -= Math.round((16 * scale - size) / 2);
        }
        if (Math.abs(scale - 1.0f) < 0.001f) {
            graphics.renderItem(stack, x, y);
            return;
        }
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale(scale, scale, 1);
            graphics.renderItem(stack, 0, 0);
        } finally {
            graphics.pose().popPose();
        }
    }

    /** True 3D item render used by the spin effects. */
    private static void drawItem3D(GuiGraphics graphics, Minecraft minecraft, ItemStack stack,
                                   int x, int y, int size, float angle) {
        BakedModel model = minecraft.getItemRenderer().getModel(stack, minecraft.level,
                minecraft.player, 0);
        boolean flatLighting = !model.usesBlockLight();
        if (flatLighting) {
            Lighting.setupForFlatItems();
        } else {
            Lighting.setupFor3DItems();
        }
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(x + size / 2.0f, y + size / 2.0f, 150.0f);
            graphics.pose().scale(size, -size, size);
            graphics.pose().mulPose(Axis.XP.rotationDegrees(18.0f));
            graphics.pose().mulPose(Axis.YP.rotationDegrees(angle));
            minecraft.getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, 0xF000F0,
                    OverlayTexture.NO_OVERLAY, graphics.pose(), graphics.bufferSource(),
                    minecraft.level, 0);
            graphics.flush();
        } finally {
            graphics.pose().popPose();
            if (flatLighting) {
                Lighting.setupFor3DItems();
            }
        }
    }

    // ---- Cooldown ----
    private static void drawCooldown(GuiGraphics graphics, Minecraft minecraft, Cell cell,
                                     HudTheme theme, CooldownStyle style,
                                     long now, boolean demo, int tintColor, LayerOptions opts,
                                     int ringOffset, int barY) {
        ItemStack stack = cell.slot.stack();
        int iconX = cell.iconX;
        int iconY = cell.iconY + cell.slide;
        int size = cell.iconSize;
        float remaining;
        String text;
        if (demo) {
            remaining = 0.62f;
            text = "6.2s";
        } else {
            if (minecraft.player == null) {
                return;
            }
            var cooldowns = minecraft.player.getCooldowns();
            Item item = stack.getItem();
            if (!cooldowns.isOnCooldown(item)) {
                COOLDOWNS.remove(item);
                return;
            }
            remaining = Mth.clamp(cooldowns.getCooldownPercent(item, 0.0f), 0.0f, 1.0f);
            if (remaining <= 0.0f) {
                return;
            }
            CooldownTracker tracker = COOLDOWNS.computeIfAbsent(item,
                    ignored -> new CooldownTracker(now, remaining));
            tracker.sample(now, remaining);
            text = tracker.text(remaining);
        }
        float pulse = opts.pulse && remaining <= 0.2f
                ? 0.62f + 0.38f * (0.5f + 0.5f * (float) Math.sin(now / 100.0)) : 1.0f;
        int accent = StylePainter.alpha(tintColor, pulse * opts.alpha());
        text = opts.format(remaining, text);
        int thickness = barThickness(opts);
        // The number always sits on the plate's bottom edge, the way vanilla shows a cooldown, so a
        // ring can enclose the pet without a dark box painted over it.
        int textX = cell.slotX + (cell.slotSide - minecraft.font.width(text)) / 2;
        int textY = cell.slotY + cell.slide + cell.slotSide - 9;

        switch (style) {
            case ARC -> {
                drawArc(graphics, cell.slotX + cell.slotSide / 2,
                        cell.slotY + cell.slide + cell.slotSide / 2,
                        ringRadius(cell, theme, Math.max(0, ringOffset), thickness),
                        remaining, accent,
                        opts.track ? StylePainter.alpha(0x33FFFFFF, 1.0f) : 0,
                        thickness, opts.reverse);
                graphics.drawString(minecraft.font, text, textX, textY, 0xFFFFFFFF, true);
            }
            case TEXT -> {
                StylePainter.roundRect(graphics, iconX - 1, iconY - 1, size + 2, size + 2, 4,
                        StylePainter.alpha(0xCC000000, 0.60f));
                graphics.drawString(minecraft.font, text, textX, textY, accent, true);
            }
            case BAR -> {
                // Own row in the bar stack under the plate, the same width as the durability gauge
                // so the two read as one tidy column. Diagonal hatching is the cooldown-only trait:
                // durability stays a calm solid line.
                int x = cell.slotX + 1;
                int w = Math.max(1, cell.slotSide - 2);
                if (opts.track) {
                    graphics.fill(x, barY, x + w, barY + thickness,
                            StylePainter.alpha(0x99000000, 1.0f));
                }
                int filled = Math.round(w * remaining);
                if (filled > 0) {
                    int fx = opts.reverse ? x + w - filled : x;
                    graphics.fill(fx, barY, fx + filled, barY + thickness, accent);
                    hatch(graphics, fx, barY, filled, thickness, accent, now);
                }
            }
            case OFF -> {
            }
        }
    }

    /** Shared ring primitive: 32 marks; thickness and direction are configurable. */
    private static void drawArc(GuiGraphics graphics, int cx, int cy, float radius,
                                float fraction, int color, int track) {
        drawArc(graphics, cx, cy, radius, fraction, color, track, 2, false);
    }

    private static void drawArc(GuiGraphics graphics, int cx, int cy, float radius,
                                float fraction, int color, int track, int thickness,
                                boolean reverse) {
        int segments = 32;
        int size = Math.max(1, Math.min(4, thickness));
        int filled = Math.round(Math.max(0.0f, Math.min(1.0f, fraction)) * segments);
        for (int i = 0; i < segments; i++) {
            double angle = -Math.PI / 2.0 + i * (Math.PI * 2.0 / segments);
            int px = Math.round(cx + (float) Math.cos(angle) * radius);
            int py = Math.round(cy + (float) Math.sin(angle) * radius);
            boolean lit = reverse ? i >= segments - filled : i < filled;
            graphics.fill(px, py, px + size, py + size, lit ? color : track);
        }
    }

    /**
     * Scrolling diagonal hatch, clipped to the filled part of a cooldown bar. This is the trait that
     * separates a cooldown from a durability gauge: cooldowns move, durability does not.
     */
    private static void hatch(GuiGraphics graphics, int x, int y, int w, int h, int color,
                              long now) {
        if (w <= 0) {
            return;
        }
        int step = 3;
        int offset = (int) ((now / 90L) % step);
        int light = StylePainter.blend(color, 0xFFFFFFFF, 0.35f);
        for (int i = -h; i < w + h; i += step) {
            for (int row = 0; row < h; row++) {
                int px = x + i + row + offset;
                if (px < x || px >= x + w) {
                    continue;
                }
                graphics.fill(px, y + row, px + 1, y + row + 1, light);
            }
        }
    }

    // ---- Durability ----
    private static void drawDurability(GuiGraphics graphics, Minecraft minecraft, Cell cell,
                                       HudTheme theme, DurabilityStyle style, HudConfig cfg,
                                       long now, int barY, int ringOffset) {
        ItemStack stack = cell.slot.stack();
        float fraction = Mth.clamp(1.0f - stack.getDamageValue()
                / (float) Math.max(1, stack.getMaxDamage()), 0.0f, 1.0f);
        int healthy = cfg.durabilityTint(theme.gaugeFull);
        int color = fraction > 0.5f ? healthy : fraction > 0.2f ? theme.gaugeMid : theme.gaugeLow;
        float pulse = fraction <= 0.2f
                ? 0.62f + 0.38f * (0.5f + 0.5f * (float) Math.sin(now / 110.0)) : 1.0f;
        // Full bars are information the player already has; keep them quiet.
        float presence = fraction >= 0.999f ? 0.45f : 1.0f;
        LayerOptions opts = cfg.durabilityOptions;
        float alpha = opts.alpha();
        int fillColor = StylePainter.alpha(color, pulse * presence * alpha);
        int backColor = opts.track ? StylePainter.alpha(theme.muted, 0.22f * presence * alpha) : 0;
        int thickness = barThickness(opts);
        // Every gauge shares the same column: the width of the plate, one row under it.
        int barX = cell.slotX + 1;
        int barW = Math.max(1, cell.slotSide - 2);

        switch (style) {
            case OFF -> {
            }
            case BAR -> {
                if (opts.track) {
                    StylePainter.roundRect(graphics, barX, barY, barW, thickness, 1, backColor);
                }
                int filled = Math.round(barW * fraction);
                int fx = opts.reverse ? barX + barW - filled : barX;
                if (filled > 0) {
                    StylePainter.roundRect(graphics, fx, barY, filled, thickness, 1, fillColor);
                }
            }
            case SEGMENTS -> {
                int cells = 5;
                int gap = 1;
                int cellWidth = Math.max(1, (barW - (cells - 1) * gap) / cells);
                int filled = Math.min(cells, (int) Math.ceil(fraction * cells));
                for (int i = 0; i < cells; i++) {
                    int lit = opts.reverse ? cells - 1 - i : i;
                    int cx = barX + i * (cellWidth + gap);
                    graphics.fill(cx, barY, cx + cellWidth, barY + Math.max(3, thickness),
                            lit < filled ? fillColor : backColor);
                }
            }
            case PIPS -> {
                int dots = 6;
                int dot = Math.max(2, thickness);
                int gap = Math.max(1, (barW - dots * dot) / Math.max(1, dots - 1));
                int filled = Math.round(fraction * dots);
                for (int i = 0; i < dots; i++) {
                    int lit = opts.reverse ? dots - 1 - i : i;
                    int cx = barX + i * (dot + gap);
                    graphics.fill(cx, barY, cx + dot, barY + dot,
                            lit < filled ? fillColor : backColor);
                }
            }
            case TEXT -> {
                String text = Math.round(fraction * 100.0f) + "%";
                graphics.drawString(minecraft.font, text,
                        barX + (barW - minecraft.font.width(text)) / 2, barY, fillColor, true);
            }
            case VALUE -> {
                int max = Math.max(1, stack.getMaxDamage());
                String text = (max - stack.getDamageValue()) + "/" + max;
                graphics.drawString(minecraft.font, text,
                        barX + (barW - minecraft.font.width(text)) / 2, barY, fillColor, true);
            }
            case RING -> {
                drawArc(graphics, cell.slotX + cell.slotSide / 2,
                        cell.slotY + cell.slotSide / 2 + cell.slide,
                        ringRadius(cell, theme, Math.max(0, ringOffset), thickness), fraction, fillColor, backColor,
                        thickness, opts.reverse);
            }
            case BATTERY -> {
                int h = 3 + thickness;
                StylePainter.outline(graphics, barX, barY, barW, h, 1, backColor);
                int filled = Math.round((barW - 2) * fraction);
                if (filled > 0) {
                    int fx = opts.reverse ? barX + barW - 1 - filled : barX + 1;
                    graphics.fill(fx, barY + 1, fx + filled, barY + h - 1, fillColor);
                }
            }
        }
    }

    // ---- Queries ----
    /** First slot key in the current layout, used when an effect is enabled without a pin. */
    public static String firstSlotKey() {
        return SLOT_BOUNDS.isEmpty() ? null : SLOT_BOUNDS.get(0).key();
    }

    /** Key of the slot under the mouse, or {@code null}. Used by the editor's right-click. */
    public static String slotKeyAt(int mouseX, int mouseY) {
        for (SlotBounds bounds : SLOT_BOUNDS) {
            if (mouseX >= bounds.x && mouseX < bounds.x + bounds.w
                    && mouseY >= bounds.y && mouseY < bounds.y + bounds.h) {
                return bounds.key;
            }
        }
        return null;
    }

    private static boolean hasGauge(HudSlot slot, HudConfig cfg) {
        return cfg.showsDurabilityFor(slot.key()) && !slot.empty()
                && slot.stack().isDamageableItem();
    }

    /**
     * The one path from raw slots to drawable slots: apply the slot filter of the given config
     * view (source toggles, shown/hidden slots, vanilla groups, empty preference), then the sort
     * order, then the {@code maxSlots} cap. Used by the HUD, the editor preview and the measuring
     * pass so all three always agree. Callers hand in every known slot, not a pre-filtered list:
     * each panel filters with its own settings.
     */
    public static List<HudSlot> prepare(List<HudSlot> input, HudConfig cfg) {
        List<HudSlot> slots = new ArrayList<>();
        if (input == null) {
            return slots;
        }
        for (HudSlot slot : input) {
            if (slot == null) {
                continue;
            }
            if (!cfg.sourceEnabled(slot.source()) || !cfg.slotVisible(slot.key())) {
                continue;
            }
            if ("vanilla".equals(slot.source()) && !cfg.vanillaGroupEnabled(slot.group())) {
                continue;
            }
            if (slot.empty() && !cfg.emptySlots) {
                continue;
            }
            slots.add(slot);
        }
        Comparator<HudSlot> order = comparator(cfg);
        if (order != null) {
            slots.sort(order);
        }
        if (slots.size() > cfg.maxSlots) {
            slots = new ArrayList<>(slots.subList(0, cfg.maxSlots));
        }
        return slots;
    }

    /** Ordered copy of the given slots, using the same rule the HUD draws with. */
    public static List<HudSlot> sortSlots(List<HudSlot> slots, HudConfig cfg) {
        List<HudSlot> copy = new ArrayList<>(slots);
        Comparator<HudSlot> order = comparator(cfg);
        if (order != null) {
            copy.sort(order);
        }
        return copy;
    }

    /** Same rule, for callers holding a panel's own sort settings instead of a whole config. */
    public static List<HudSlot> sortSlots(List<HudSlot> slots, String sortMode,
                                          List<String> slotOrder) {
        List<HudSlot> copy = new ArrayList<>(slots);
        Comparator<HudSlot> order = comparator(sortMode, key -> {
            int index = slotOrder == null ? -1 : slotOrder.indexOf(key);
            return index;
        });
        if (order != null) {
            copy.sort(order);
        }
        return copy;
    }

    /**
     * Sort order for the visible rows. {@code null} keeps the provider order, which is also the
     * order the accessory mod itself uses.
     */
    private static Comparator<HudSlot> comparator(HudConfig cfg) {
        return comparator(cfg.sortMode, cfg::orderIndex);
    }

    private static Comparator<HudSlot> comparator(String sortMode,
                                                  java.util.function.ToIntFunction<String>
                                                          orderIndex) {
        Comparator<HudSlot> byKey = Comparator.comparing(HudSlot::key);
        Comparator<HudSlot> byName = Comparator
                .comparing((HudSlot slot) -> slot.empty()
                        ? "\uffff"
                        : slot.stack().getHoverName().getString()
                                .toLowerCase(java.util.Locale.ROOT))
                .thenComparing(HudSlot::key);
        Comparator<HudSlot> byType = Comparator
                .comparing((HudSlot slot) -> slot.groupLabel() == null ? ""
                        : slot.groupLabel().toLowerCase(java.util.Locale.ROOT))
                .thenComparing(HudSlot::group)
                .thenComparingInt(HudRenderer::bodyRank)
                .thenComparingInt(HudSlot::index)
                .thenComparing(byKey);
        Comparator<HudSlot> bySource = Comparator
                .comparingInt((HudSlot slot) -> sourceRank(slot.source()))
                .thenComparing(HudSlot::source)
                .thenComparing(byType);
        return switch (sortMode == null ? "default" : sortMode) {
            case "name" -> byName;
            case "type" -> byType;
            case "source" -> bySource;
            case "custom" -> Comparator
                    .comparingInt((HudSlot slot) -> {
                        int index = orderIndex.applyAsInt(slot.key());
                        return index < 0 ? Integer.MAX_VALUE : index;
                    })
                    .thenComparing(byKey);
            default -> null;
        };
    }

    /** Keeps armour in head-to-feet order instead of alphabetical when sorting by type. */
    private static int bodyRank(HudSlot slot) {
        return switch (slot.slot() == null ? "" : slot.slot()) {
            case "helmet" -> 0;
            case "chestplate" -> 1;
            case "leggings" -> 2;
            case "boots" -> 3;
            default -> 0;
        };
    }

    private static int sourceRank(String source) {
        return switch (source == null ? "" : source) {
            case "vanilla" -> 0;
            case "accessories" -> 1;
            case "trinkets" -> 2;
            case "curios" -> 3;
            default -> 4;
        };
    }

    private static String labelText(HudSlot slot, HudConfig cfg, HudTheme theme) {
        String text;
        if (theme.numberRows) {
            text = slot.empty() ? "" : durabilityText(slot.stack());
        } else if (slot.empty()) {
            text = "Empty";
        } else if ("item".equals(cfg.labelMode)) {
            text = slot.stack().getHoverName().getString();
        } else {
            text = slotLabel(slot);
        }
        return theme.uppercase ? text.toUpperCase(java.util.Locale.ROOT) : text;
    }

    /** Armour pieces share the "Armor" group, so their own name is the useful label. */
    private static String slotLabel(HudSlot slot) {
        if ("armor".equals(slot.group()) && slot.slot() != null && !slot.slot().isBlank()) {
            String id = slot.slot();
            return Character.toUpperCase(id.charAt(0)) + id.substring(1);
        }
        if ("offhand".equals(slot.group())) {
            return "Offhand";
        }
        return slot.groupLabel() == null || slot.groupLabel().isBlank()
                ? slot.group() : slot.groupLabel();
    }

    private static String durabilityText(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.isDamageableItem()) {
            return "";
        }
        int max = Math.max(1, stack.getMaxDamage());
        return (max - stack.getDamageValue()) + "/" + max;
    }

    private static int readoutColor(HudSlot slot, HudTheme theme) {
        ItemStack stack = slot.stack();
        if (stack == null || stack.isEmpty() || !stack.isDamageableItem()) {
            return theme.muted;
        }
        float fraction = 1.0f - stack.getDamageValue()
                / (float) Math.max(1, stack.getMaxDamage());
        return fraction > 0.5f ? theme.gaugeFull
                : fraction > 0.2f ? theme.gaugeMid : theme.gaugeLow;
    }

    private static String prettySource(String source) {
        if (source == null || source.isBlank()) {
            return "Items";
        }
        return source.substring(0, 1).toUpperCase(java.util.Locale.ROOT)
                + source.substring(1);
    }

    private static int groupTint(String source, int accent) {
        return switch (source == null ? "" : source) {
            case "accessories" -> 0xFFB5794F;
            case "trinkets" -> 0xFFC24A5A;
            case "curios" -> 0xFF4E8FD1;
            default -> accent;
        };
    }

    /**
     * Height of the durability row in the bar stack. Rings and the plate-inset styles own no row:
     * they are drawn around or inside the slot instead.
     */
    private static int durabilityBarHeight(DurabilityStyle style, LayerOptions opts) {
        int thickness = barThickness(opts);
        return switch (style) {
            case OFF, RING -> 0;
            case BAR -> thickness;
            case SEGMENTS, PIPS -> Math.max(3, thickness);
            case BATTERY -> 3 + thickness;
            case TEXT, VALUE -> 9;
        };
    }

    private static int barThickness(LayerOptions opts) {
        return Math.max(1, Math.min(4, opts == null ? 2 : opts.thickness));
    }

    /**
     * A configured layer only owns its ring band or bar row while it has something to show. The
     * ability layer flips this on for the whole panel so one timer starting cannot resize slots
     * one by one.
     */
    private static boolean abilityLayerLive;

    private static boolean hasAbilityTimer(List<HudSlot> slots) {
        for (HudSlot slot : slots) {
            if (AbilityTimers.remaining(slot.key()) != null) {
                return true;
            }
        }
        return false;
    }

    private static boolean abilityRingFor(HudConfig cfg) {
        return abilityLayerLive && cfg.abilityStyle() == AbilityStyle.RING;
    }

    private static boolean abilityBarFor(HudConfig cfg) {
        return abilityLayerLive && cfg.abilityStyle() == AbilityStyle.BAR;
    }

    /**
     * Radius of a feedback ring. Marks grow outward by their thickness, so the band starts this
     * far in and ends at the plate edge minus its border minus the rings already packed there.
     * The slot inset grows with the whole band, so a ring encloses the icon instead of crossing it.
     */
    private static float ringRadius(Cell cell, HudTheme theme, float offset, int thickness) {
        int border = Math.max(1, Math.min(2, theme.borderWidth));
        float outer = cell.slotSide / 2.0f - border;
        int size = Math.max(1, Math.min(4, thickness));
        return Math.max(cell.iconSize / 2.0f + 1.0f, outer - offset - size);
    }

    /**
     * Room for one name line. The name starts three pixels under the plate whether or not a gauge
     * sits there, so every label in a row shares one offset instead of following its own gauge.
     */
    private static int labelBlock(int lineH) {
        return 3 + lineH;
    }

    /**
     * Which side of its plate a ring slot's name takes. Only the two ends of the ring name
     * themselves above/below; every other slot names itself on the outside, where a name never
     * crosses the plates that follow it around the circle.
     */
    public static int ringLabelSide(int dx, int dy, int count, int slotSide) {
        if (count <= 1) {
            return LABEL_BELOW;
        }
        if (Math.abs(dx) * 2 <= Math.max(1, slotSide)) {
            return dy < 0 ? LABEL_ABOVE : LABEL_BELOW;
        }
        return dx > 0 ? LABEL_RIGHT : LABEL_LEFT;
    }

    /** Widest text the TEXT / VALUE durability styles can draw, so layout reserves room for it. */
    private static int durabilityTextWidth(Font font, DurabilityStyle style, ItemStack stack) {        return switch (style) {
            case TEXT -> font.width("100%");
            case VALUE -> {
                // Use max/max: the longest string this slot can ever print, so the panel never
                // resizes while durability ticks down.
                int max = Math.max(1, stack.getMaxDamage());
                yield font.width(max + "/" + max);
            }
            default -> 0;
        };
    }

    private static int labelWidth(Font font, List<HudSlot> slots, HudConfig cfg,
                                  HudTheme theme) {        int max = 0;
        for (HudSlot slot : slots) {
            String shown = truncate(font, labelText(slot, cfg, theme), cfg.maxLabelChars, 9999);
            max = Math.max(max, font.width(shown));
        }
        return max;
    }

    private static String truncate(Font font, String text, int limit, int pixelWidth) {
        String value = text;
        if (value.length() > limit) {
            value = value.substring(0, Math.max(1, limit - 1)) + "\u2026";
        }
        if (pixelWidth > 0 && font.width(value) > pixelWidth) {
            while (value.length() > 1 && font.width(value + "\u2026") > pixelWidth) {
                value = value.substring(0, value.length() - 1);
            }
            value = value + "\u2026";
        }
        return value;
    }

    private static float appearAlpha(String key, long now) {
        long start = FIRST_SEEN.computeIfAbsent(key, ignored -> now);
        return Mth.clamp((now - start) / 170.0f, 0.0f, 1.0f);
    }

    private static int[] origin(HudConfig cfg, int screenWidth, int screenHeight, int width,
                                int height, int bottomReserve, int margin) {
        int x;
        int y;
        switch (cfg.anchor()) {
            case TOP_LEFT -> {
                x = margin + cfg.x;
                y = margin + cfg.y;
            }
            case BOTTOM_LEFT -> {
                x = margin + cfg.x;
                y = screenHeight - HOTBAR_HEIGHT - margin - height - cfg.y;
            }
            case BOTTOM_RIGHT -> {
                x = screenWidth - width - margin - cfg.x;
                y = screenHeight - HOTBAR_HEIGHT - margin - height - cfg.y;
            }
            case ABOVE_HOTBAR -> {
                x = (screenWidth - width) / 2 + cfg.x;
                y = screenHeight - HOTBAR_HEIGHT - height - 4 - cfg.y;
            }
            case CENTER -> {
                x = (screenWidth - width) / 2 + cfg.x;
                y = (screenHeight - height) / 2 + cfg.y;
            }
            case CUSTOM -> {
                x = cfg.x;
                y = cfg.y;
            }
            default -> {
                x = screenWidth - width - margin - cfg.x;
                y = margin + cfg.y;
            }
        }
        x = Mth.clamp(x, margin, Math.max(margin, screenWidth - width - margin));
        // A dragged panel may use the whole screen; only automatic anchors keep clear of the
        // editor toolbar at the bottom.
        int reserve = cfg.anchor() == HudAnchor.CUSTOM ? 0 : bottomReserve;
        int maxY = Math.max(margin, screenHeight - reserve - height - margin);
        y = Mth.clamp(y, margin, Math.max(margin, maxY));
        return new int[]{x, y};
    }

    private static String findHovered(int mouseX, int mouseY) {
        for (SlotBounds bounds : SLOT_BOUNDS) {
            if (mouseX >= bounds.x && mouseX < bounds.x + bounds.w
                    && mouseY >= bounds.y && mouseY < bounds.y + bounds.h) {
                return bounds.key;
            }
        }
        return null;
    }

    private record Layout(int width, int height, List<Cell> cells, List<Header> headers,
                          List<GroupBox> groups) {
    }

    private record GroupBox(int x, int y, int width, int height, int tint, String label) {
    }

    private static final class Cell {
        private final HudSlot slot;
        private final int x;
        private final int y;
        private int width;
        private final int height;
        private int slotX;
        private int slotY;
        private int slotSide;
        private int iconSize;
        private int iconX;
        private int iconY;
        private int labelX;
        private int labelY;
        private int labelSide = LABEL_BELOW;
        private int durY;
        private boolean labelBelow;
        private int plateX;
        private int plateY;
        private int plateW;
        private int plateH;
        private String label = "";
        private int slide;
        private boolean hovered;
        private boolean featured;
        private boolean selected;
        private int tint;

        private Cell(HudSlot slot, int x, int y, int width, int height) {
            this.slot = slot;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    private static final class Header {
        private final String label;
        private final int x;
        private final int y;
        private final int tint;
        private int width;

        private Header(String label, int x, int y, int width, int tint) {
            this.label = label;
            this.x = x;
            this.y = y;
            this.width = width;
            this.tint = tint;
        }
    }

    /**
     * Vanilla only exposes the remaining fraction, not the original duration. We estimate the
     * duration from the fraction drop over time, which turns the percent into a readable
     * "3.2s" countdown after a couple of ticks. Falls back to "%" when the estimate is not ready.
     */
    private static final class CooldownTracker {
        private long firstSeen;
        private long lastSeen;
        private float firstPercent;
        private float lastRemaining;
        private float totalSeconds;

        private CooldownTracker(long now, float remaining) {
            reset(now, remaining);
        }

        private void reset(long now, float remaining) {
            firstSeen = now;
            lastSeen = now;
            firstPercent = remaining;
            lastRemaining = remaining;
            totalSeconds = 0.0f;
        }

        private void sample(long now, float remaining) {
            if (now - lastSeen > 400L || remaining > lastRemaining + 0.15f) {
                reset(now, remaining);
                return;
            }
            lastSeen = now;
            lastRemaining = remaining;
            float drop = firstPercent - remaining;
            if (drop > 0.03f && now - firstSeen > 150L) {
                float estimate = (now - firstSeen) / 1000.0f / drop;
                if (estimate > 0.15f && estimate < 3600.0f) {
                    totalSeconds = totalSeconds <= 0.0f
                            ? estimate
                            : totalSeconds * 0.65f + estimate * 0.35f;
                }
            }
        }

        private String text(float remaining) {
            float seconds = totalSeconds * remaining;
            if (totalSeconds > 0.0f && seconds >= 0.05f) {
                return String.format(java.util.Locale.ROOT, "%.1fs", seconds);
            }
            return Math.round(remaining * 100.0f) + "%";
        }
    }

    private record SlotBounds(String key, int x, int y, int w, int h, ItemStack stack) {
    }
}
