package com.toancao.loadouttc.gui;

import com.toancao.loadouttc.compat.HudData;
import com.toancao.loadouttc.compat.HudSlot;
import com.toancao.loadouttc.config.ConfigStore;
import com.toancao.loadouttc.config.HudCluster;
import com.toancao.loadouttc.config.HudConfig;
import com.toancao.loadouttc.config.LayerOptions;
import com.toancao.loadouttc.config.SlotOverride;
import com.toancao.loadouttc.hud.CooldownStyle;
import com.toancao.loadouttc.hud.DurabilityStyle;
import com.toancao.loadouttc.hud.HudEffects;
import com.toancao.loadouttc.hud.HudRenderer;
import com.toancao.loadouttc.hud.HudStyle;
import com.toancao.loadouttc.hud.HudTheme;
import com.toancao.loadouttc.hud.PanelStyle;
import com.toancao.loadouttc.hud.SlotStyle;
import com.toancao.loadouttc.hud.StylePainter;
import com.toancao.loadouttc.hud.TextStyle;
import com.toancao.loadouttc.util.UiText;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The whole design surface: a scrollable list of sections (preset, panel, slot, text, spacing,
 * content, layout, accent) plus a sticky live preview that renders the real HUD.
 *
 * <p>The preview has a target switch: <b>All</b> edits the global defaults, <b>Slot</b> writes an
 * override for the slot you click in the preview.
 */
public final class StylePickerScreen extends Screen {
    private static final int MARGIN = UiTheme.HEADER_X;
    private static final int CHIP_H = UiTheme.CHIP_H;
    private static final int CHIP_GAP = 4;
    private static final int SECTION_GAP = 14;
    private static final int HEADER = UiTheme.HEADER_H;
    /** One tab per settings group, so the list stays short instead of one long scroll. */
    private static final String[] TABS = {"Look", "Layout", "Spacing", "Bars", "Motion", "Help"};
    /** A single slot is being edited: its list replaces the tabs, so it reads as its own mode. */
    private static final String SLOT_TAB = "Slot";

    private static final String[] COLOR_LABELS = {
            "Red", "Orange", "Yellow", "Green", "Cyan", "Blue", "Purple", "Pink", "White"
    };
    private static final String[] COLOR_IDS = {
            "ff5555", "ff9f43", "ffd93b", "55d96b", "4fd6e3", "6aa9ff", "b48bff", "ff8fc7",
            "ffffff"
    };
    private static final int[] COLOR_ARGB = {
            0xFFFF5555, 0xFFFF9F43, 0xFFFFD93B, 0xFF55D96B, 0xFF4FD6E3, 0xFF6AA9FF,
            0xFFB48BFF, 0xFFFF8FC7, 0xFFFFFFFF
    };
    /**
     * One-line hover tips, keyed by the English chip label. A label is only listed when it means
     * the same thing everywhere it appears ("Auto" is not, so it gets no tip instead of a lie).
     */
    private static final java.util.Map<String, String> TIPS = java.util.Map.ofEntries(
            java.util.Map.entry("Vertical", "One column"),
            java.util.Map.entry("Horizontal", "One row"),
            java.util.Map.entry("Grid", "Rows x columns"),
            java.util.Map.entry("Circle", "Ring of slots"),
            java.util.Map.entry("Columns", "Slots per row"),
            java.util.Map.entry("Ring radius", "Ring size"),
            java.util.Map.entry("Count", "Stack counts"),
            java.util.Map.entry("Max slots", "Draw limit"),
            java.util.Map.entry("Slot name", "Slot title"),
            java.util.Map.entry("Item name", "Item title"),
            java.util.Map.entry("Hidden", "No label"),
            java.util.Map.entry("Name width", "Max characters"),
            java.util.Map.entry("S", "0.80x"),
            java.util.Map.entry("M", "1.00x"),
            java.util.Map.entry("L", "1.25x"),
            java.util.Map.entry("Custom", "Your own scale"),
            java.util.Map.entry("Border", "Outline width"),
            java.util.Map.entry("Scale", "Panel scale"),
            java.util.Map.entry("Panel padding", "Space inside panel"),
            java.util.Map.entry("Row gap", "Space between rows"),
            java.util.Map.entry("Label gap", "Icon to label"),
            java.util.Map.entry("Slot inset", "Icon inside the slot"),
            java.util.Map.entry("Screen edge", "Gap to screen edge"),
            java.util.Map.entry("Icon size", "Icon pixel size"),
            java.util.Map.entry("Icon X", "Shift icon sideways"),
            java.util.Map.entry("Icon Y", "Shift icon up or down"),
            java.util.Map.entry("Label X", "Shift label sideways"),
            java.util.Map.entry("Label Y", "Shift label up or down"),
            java.util.Map.entry("Gauge Y", "Shift gauge up or down"),
            java.util.Map.entry("Reset spacing", "Back to theme values"),
            java.util.Map.entry("Bar", "Line under the slot"),
            java.util.Map.entry("Segments", "Five cells"),
            java.util.Map.entry("Pips", "Six dots"),
            java.util.Map.entry("Percent", "Show as percent"),
            java.util.Map.entry("Value", "Current / max"),
            java.util.Map.entry("Ring", "Encloses the icon"),
            java.util.Map.entry("Battery", "Battery outline"),
            java.util.Map.entry("Off", "Nothing drawn"),
            java.util.Map.entry("Thickness", "Gauge thickness"),
            java.util.Map.entry("Opacity", "Layer opacity"),
            java.util.Map.entry("Track", "Line behind fill"),
            java.util.Map.entry("Blink", "Blink when low"),
            java.util.Map.entry("Reverse", "Fill backwards"),
            java.util.Map.entry("Arc", "Circular sweep"),
            java.util.Map.entry("Text", "Number readout"),
            java.util.Map.entry("Frame", "Border ring"),
            java.util.Map.entry("Seconds", "Show seconds"),
            java.util.Map.entry("Int", "Whole seconds"),
            java.util.Map.entry("Spin", "Rotate the icon"),
            java.util.Map.entry("3D", "Tilted spin"),
            java.util.Map.entry("Fast", "Quicker spin"),
            java.util.Map.entry("3D+", "Quick tilted spin"),
            java.util.Map.entry("Bob", "Gentle bounce"),
            java.util.Map.entry("Pulse", "Size pulse"),
            java.util.Map.entry("Shake", "Quick shiver"),
            java.util.Map.entry("Flash", "Blink on and off"),
            java.util.Map.entry("RGB", "Rainbow cycle"),
            java.util.Map.entry("Still", "No motion"),
            java.util.Map.entry("Glow", "Soft glow"),
            java.util.Map.entry("Fade", "Fade in and out"),
            java.util.Map.entry("Flow", "Moving frame"),
            java.util.Map.entry("Show guides", "Alignment outlines"),
            java.util.Map.entry("Default", "The stock blue"),
            java.util.Map.entry("Same as panel", "Inherit the panel value"),
            java.util.Map.entry("Icon motion", "How the icon moves"),
            java.util.Map.entry("Icon light", "Glow around the icon"),
            java.util.Map.entry("Speed", "How fast it moves"),
            java.util.Map.entry("Strength", "How bright the glow is"),
            java.util.Map.entry("Slow", "Half speed"),
            java.util.Map.entry("Normal", "Natural speed"),
            java.util.Map.entry("Quick", "Almost double speed"),
            java.util.Map.entry("Soft", "Gentle glow"),
            java.util.Map.entry("Strong", "Loud glow"),
            java.util.Map.entry("Effect blend", "White mixed into the effect colour"),
            java.util.Map.entry("Bounce", "Hop in place"),
            java.util.Map.entry("Sway", "Rock side to side"),
            java.util.Map.entry("Reset slot", "Clear this slot's overrides"));

    /** Motion group: one transform at a time, so two spins can never fight over the icon. */
    private static final String[] MOTION_IDS = {
            "none", "spin", "spin_3d", "spin_fast", "spin_3d_fast", "bob", "bounce", "sway",
            "pulse", "shake"
    };
    private static final String[] MOTION_LABELS = {
            "Off", "Spin", "3D", "Fast", "3D+", "Bob", "Bounce", "Sway", "Pulse", "Shake"
    };
    /** Light group: a separate layer, so a moving icon can also glow. */
    private static final String[] LIGHT_IDS = {"none", "glow", "flash", "rainbow"};
    private static final String[] LIGHT_LABELS = {"Off", "Glow", "Flash", "RGB"};
    private static final String[] SPEED_IDS = {"slow", "normal", "fast"};
    private static final String[] SPEED_LABELS = {"Slow", "Normal", "Quick"};
    private static final String[] STRENGTH_IDS = {"soft", "normal", "strong"};
    private static final String[] STRENGTH_LABELS = {"Soft", "Normal", "Strong"};


    private HudConfig config;
    private final Runnable onBack;
    private final Runnable onPanels;
    private final List<Section> sections = new ArrayList<>();
    private final List<HudSlot> previewSlots = new ArrayList<>();

    private List<HudRenderer.PreviewSlot> previewBounds = List.of();
    private boolean targetAll = true;
    private String targetSlot;
    /** Last slot the player targetted, so "One slot" reopens the same one instead of the first. */
    private String lastSlot;
    /** Per-slot editing stays locked until the player unlocks it, so a click never edits by accident. */
    private boolean slotUnlocked;
    private int lockChipX;
    private int lockChipY;
    private int lockChipW;
    private String lockChipTip = "Edit this slot on its own";
    private String tab = TABS[0];

    private double scroll;
    private int contentHeight;
    private boolean draggingScrollbar;
    private int scrollTrackTop;
    private int scrollTrackHeight;
    private int scrollThumbHeight;
    private double scrollGrab;

    private int contentX;
    private int contentRight;
    private int clipTop;
    private int clipBottom;
    private int previewX;
    private int previewY;
    private int previewW;
    private int previewH;
    private boolean sideLayout;

    private boolean backgroundDrawn;

    public StylePickerScreen(HudConfig config, Runnable onBack) {
        this(config, onBack, onBack);
    }

    public StylePickerScreen(HudConfig config, Runnable onBack, Runnable onPanels) {
        super(Component.literal(UiText.s("HUD Design")));
        this.config = config;
        this.onBack = onBack;
        this.onPanels = onPanels;
    }

    // ---- Structure ----

    @Override
    protected void init() {
        UiText.clear();
        if (config.activeCluster.isBlank() && !config.clusters.isEmpty()) {
            config.activeCluster = config.clusters.get(0).id;
        }
        if (!ConfigStore.editingCluster()
                || !config.activeCluster.equals(ConfigStore.editingClusterId())) {
            ConfigStore.beginEdit(config.activeCluster);
            this.config = ConfigStore.get();
        }
        sections.clear();
        previewSlots.clear();
        // The renderer filters per panel, so it needs every slot; the slot filter is applied there.
        previewSlots.addAll(HudData.collectAll(minecraft, config));
        // Demo items only stand in when the player really has nothing; an empty result caused by
        // the slot filter must stay empty, otherwise the preview lies about what is hidden.
        if (previewSlots.isEmpty()) {
            previewSlots.addAll(HudData.demoSlots());
        }
        targetSlot = HudRenderer.selectedKey();
        targetAll = targetSlot == null;
        if (targetSlot != null) {
            lastSlot = targetSlot;
            slotUnlocked = config.isSlotUnlocked(targetSlot);
        }

        buildSections();

        addRenderableWidget(UiButton.of(width - MARGIN - 70, UiTheme.HEADER_TITLE_Y - 5, 70, 18,
                UiText.s("Done"), button -> onClose(), "Save and close"));
    }

    private void buildSections() {
        if (slotMode()) {
            buildSlotSections();
            return;
        }
        HudTheme theme = HudTheme.of(config);
        // ---- Look ----
        Section panel = section("Look", "Panel");
        PanelStyle[] panelOrder = {PanelStyle.VANILLA, PanelStyle.DARK, PanelStyle.GLASS,
                PanelStyle.WOOD, PanelStyle.CARBON, PanelStyle.PARCHMENT, PanelStyle.ARCANE,
                PanelStyle.NONE};
        for (PanelStyle style : panelOrder) {
            panel.chip(style.label(),
                    () -> style.id().equals(config.panelStyle)
                            || ("auto".equals(config.panelStyle)
                            && PanelStyle.resolve("auto", config.style()) == style),
                    panelSwatch(style), () -> {
                        config.panelStyle = style.id();
                        save();
                    });
        }

        Section slot = section("Look", "Slot");
        SlotStyle[] slotOrder = {SlotStyle.VANILLA, SlotStyle.STEEL, SlotStyle.HEX,
                SlotStyle.CUT, SlotStyle.INSET, SlotStyle.ROUNDED, SlotStyle.CIRCLE,
                SlotStyle.TRIANGLE, SlotStyle.ELLIPSE, SlotStyle.NONE};
        for (SlotStyle style : slotOrder) {
            slot.chip(style.label(),
                    () -> style.id().equals(config.slotStyle)
                            || ("auto".equals(config.slotStyle)
                            && SlotStyle.resolve("auto", config.style()) == style),
                    slotSwatch(style), () -> {
                        config.slotStyle = style.id();
                        save();
                    });
        }

        Section text = section("Look", "Text");
        TextStyle[] textOrder = {TextStyle.NORMAL, TextStyle.UPPER, TextStyle.BRIGHT,
                TextStyle.GOLD, TextStyle.OUTLINE};
        for (TextStyle style : textOrder) {
            text.chipIf(style.label(),
                    () -> style.id().equals(config.textStyle)
                            || ("auto".equals(config.textStyle)
                            && TextStyle.resolve("auto", config.style()) == style),
                    0, () -> {
                        config.textStyle = style.id();
                        save();
                    },
                    () -> !(style == TextStyle.BRIGHT || style == TextStyle.GOLD)
                            || !lightPanel(),
                    "Not on a light panel");
        }

        Section radius = section("Look", "Radius / border");
        radius.chip("Auto", () -> config.radius < 0, 0, () -> {
            config.radius = -1;
            save();
        });
        for (int value : new int[]{0, 2, 4, 6, 8, 12, 16}) {
            radius.chip(String.valueOf(value), () -> config.radius == value, 0, () -> {
                config.radius = value;
                save();
            });
        }

        radius.number("Border", () -> config.slotBorderWidth,
                value -> config.slotBorderWidth = value, theme.borderWidth, 0, 4);

        // Accent shared by the HUD, the effect layers and the editor highlights.
        Section accent = section("Look", "Accent colour");
        accent.chip("Default", () -> "7c9eff".equalsIgnoreCase(config.accent), 0xFF7C9EFF, () -> {
            config.accent = "7C9EFF";
            save();
        });
        for (int i = 0; i < COLOR_IDS.length; i++) {
            String id = COLOR_IDS[i];
            accent.chip(COLOR_LABELS[i], () -> id.equalsIgnoreCase(config.accent), COLOR_ARGB[i],
                    () -> {
                        config.accent = id;
                        save();
                    });
        }
        // One shared wash for every effect: cooldown, ability and the icon glow all read the same.
        accent.fixedNumber("Effect blend", () -> config.effectBlend,
                value -> config.effectBlend = value, 0, 90, 5);

        // ---- Layout ----
        Section arrange = section("Layout", "Arrange");
        String[][] layouts = {{"vertical", "Vertical"}, {"horizontal", "Horizontal"},
                {"grid", "Grid"}, {"circle", "Circle"}};
        for (String[] layout : layouts) {
            String id = layout[0];
            arrange.chip(layout[1], () -> id.equals(config.layoutKind()), 0, () -> {
                config.layoutMode = id;
                config.horizontal = "horizontal".equals(id);
                save();
            });
        }
        arrange.chip("Count", () -> config.stackCount, 0, () -> {
            config.stackCount = !config.stackCount;
            save();
        });
        // Both rows always show; the one that cannot apply is greyed out, so a setting never
        // moves between screens depending on another chip.
        arrange.fixedNumberIf("Columns", () -> config.columns,
                value -> config.columns = value, 1, 8,
                () -> "grid".equals(config.layoutKind()), "Only for the grid layout");
        arrange.fixedNumberIf("Ring radius", () -> config.circleRadius,
                value -> config.circleRadius = value, 16, 200, 8,
                () -> "circle".equals(config.layoutKind()), "Only for the circle layout");

        Section labels = section("Layout", "Labels");
        labels.chip("Slot name", () -> config.labels && !"item".equals(config.labelMode), 0, () -> {
            config.labels = true;
            config.labelMode = "slot";
            save();
        });
        labels.chip("Item name", () -> config.labels && "item".equals(config.labelMode), 0, () -> {
            config.labels = true;
            config.labelMode = "item";
            save();
        });
        labels.chip("Hidden", () -> !config.labels, 0, () -> {
            config.labels = false;
            save();
        });
        labels.fixedNumberIf("Name width", () -> config.maxLabelChars,
                value -> config.maxLabelChars = value, 4, 24, 1, () -> config.labels,
                "Names are off");

        // ---- Slots ----
        // Which slots appear is the slot menu's job; this keeps only the HUD-side cap.
        Section menu = section("Layout", "Display");
        menu.fixedNumber("Max slots", () -> config.maxSlots,
                value -> config.maxSlots = value, 8, 128, 8);

        // ---- Spacing ----
        // Size and spacing belong to the panel's own look, so each panel can be laid out on its
        // own scale without touching the others.
        // Size belongs to the panel too: editing it while a panel is selected writes into that
        // panel's look, so each panel can be scaled independently.
        Section size = section("Spacing", "Size");
        String[][] sizeOptions = {{"auto", "Auto"}, {"micro", "XXS"}, {"tiny", "XS"},
                {"small", "S"}, {"medium", "M"},
                {"large", "L"}, {"xlarge", "XL"}, {"huge", "2x"}, {"giant", "3x"},
                {"custom", "Custom"}};
        for (String[] option : sizeOptions) {
            String id = option[0];
            size.chip(option[1], () -> id.equals(config.size), 0, () -> {
                if ("custom".equals(id) && !"custom".equals(config.size) && minecraft != null) {
                    config.scale = config.effectiveScale(minecraft.getWindow().getGuiScale());
                }
                config.size = id;
                save();
            });
        }
        size.fixedNumberIf("Scale", () -> Math.round(config.scale * 100.0f),
                value -> config.scale = value / 100.0f, 50, 300, 5,
                () -> "custom".equals(config.size), "Only for a custom size");

        Section spacing = section("Spacing", "Sizes");
        spacing.number("Panel padding", () -> config.panelPadding,
                value -> config.panelPadding = value, Math.max(4, theme.minPad), 0, 20);
        spacing.number("Row gap", () -> config.rowGap,
                value -> config.rowGap = value, Math.max(2, theme.minGap), 0, 16);
        spacing.number("Label gap", () -> config.labelGap,
                value -> config.labelGap = value, theme.labelGap, 0, 16);
        spacing.number("Slot inset", () -> config.slotInset,
                value -> config.slotInset = value, theme.slotInset, 0, 8);
        spacing.number("Screen edge", () -> config.screenMargin,
                value -> config.screenMargin = value, 8, 0, 32);
        spacing.fixedNumber("Icon size", () -> config.iconSize,
                value -> config.iconSize = value, 12, 40, 2);

        Section align = section("Spacing", "Align");
        align.fixedNumber("Icon X", () -> config.iconNudgeX,
                value -> config.iconNudgeX = value, -6, 6);
        align.fixedNumber("Icon Y", () -> config.iconNudgeY,
                value -> config.iconNudgeY = value, -6, 6);
        align.fixedNumber("Label X", () -> config.labelNudgeX,
                value -> config.labelNudgeX = value, -8, 8);
        align.fixedNumber("Label Y", () -> config.labelNudgeY,
                value -> config.labelNudgeY = value, -8, 8);
        align.fixedNumber("Gauge Y", () -> config.gaugeNudgeY,
                value -> config.gaugeNudgeY = value, -6, 10);
        align.chip("Reset spacing", () -> false, 0, () -> {
            config.panelPadding = -1;
            config.rowGap = -1;
            config.labelGap = -1;
            config.slotInset = -1;
            config.screenMargin = -1;
            config.iconSize = 18;
            config.iconNudgeX = 0;
            config.iconNudgeY = 0;
            config.labelNudgeX = 0;
            config.labelNudgeY = 0;
            config.gaugeNudgeY = 0;
            save();
        });

        // ---- Bars ----
        Section durability = section("Bars", "Durability");
        addDurabilityChips(durability);

        addColors(section("Bars", "Durability colour"),
                () -> config.durabilityColor, value -> config.durabilityColor = value);
        layerDetail(section("Bars", "Durability detail"), config.durabilityOptions, false);

        Section cooldown = section("Bars", "Cooldown");
        addCooldownChips(cooldown);

        addColors(section("Bars", "Cooldown colour"),
                () -> config.cooldownColor, value -> config.cooldownColor = value);
        layerDetail(section("Bars", "Cooldown detail"), config.cooldownOptions, true);
        layerText(section("Bars", "Cooldown text"), config.cooldownOptions);

        Section ability = section("Bars", "Ability (from mods)");
        com.toancao.loadouttc.hud.AbilityStyle[] abilityOrder = {
                com.toancao.loadouttc.hud.AbilityStyle.RING,
                com.toancao.loadouttc.hud.AbilityStyle.BAR,
                com.toancao.loadouttc.hud.AbilityStyle.TEXT,
                com.toancao.loadouttc.hud.AbilityStyle.FRAME,
                com.toancao.loadouttc.hud.AbilityStyle.OFF};
        for (com.toancao.loadouttc.hud.AbilityStyle style : abilityOrder) {
            ability.chipIf(style.label(),
                    () -> config.abilityStyle().id().equals(style.id()), 0, () -> {
                        config.abilityStyle = style.id();
                        save();
                    },
                    () -> style != com.toancao.loadouttc.hud.AbilityStyle.FRAME
                            || hasSlotBorder(),
                    "Needs a visible slot border");
        }

        addColors(section("Bars", "Ability colour"),
                () -> config.abilityColor, value -> config.abilityColor = value);
        layerDetail(section("Bars", "Ability detail"), config.abilityOptions, true);
        layerText(section("Bars", "Ability text"), config.abilityOptions);

        // ---- Motion ----
        // Two independent groups: the icon can move and glow at the same time, but never two
        // transforms at once.
        Section motionIcon = section("Motion", "Icon motion");
        addMotionChips(motionIcon);

        Section motionLight = section("Motion", "Icon light");
        addLightChips(motionLight);

        Section speed = section("Motion", "Speed");
        addChoiceChips(speed, SPEED_IDS, SPEED_LABELS,
                () -> config.effectSpeed == null ? "normal" : config.effectSpeed,
                value -> config.effectSpeed = value, this::anyEffect,
                "Pick an effect first");

        Section strength = section("Motion", "Strength");
        addChoiceChips(strength, STRENGTH_IDS, STRENGTH_LABELS,
                () -> config.effectStrength == null ? "normal" : config.effectStrength,
                value -> config.effectStrength = value, this::anyEffect,
                "Pick an effect first");

        Section nameMotion = section("Motion", "Name motion");
        String[] labelIds = {"none", "glow", "fade", "bob"};
        String[] labelMotion = {"Still", "Glow", "Fade", "Bob"};
        for (int i = 0; i < labelIds.length; i++) {
            String id = labelIds[i];
            nameMotion.chip(labelMotion[i], () -> id.equals(config.labelEffect), 0, () -> {
                config.labelEffect = id;
                save();
            });
        }

        Section flow = section("Motion", "Frame");
        flow.chipIf("Flow", () -> config.frameFlow, 0, () -> {
            config.frameFlow = !config.frameFlow;
            save();
        }, this::hasFrameLayer, "No layer uses the frame style");

        // ---- Help ----
        // Resetting the whole config lives in the editor toolbar, next to Pos/Size, so it is not
        // repeated here.
        Section help = section("Help", "Guides");
        help.chip("Show guides", () -> config.debugGuides, 0, () -> {
            config.debugGuides = !config.debugGuides;
            save();
        });
    }

    /**
     * Editing one slot shows only what a per-slot override can change, plus an explicit way back
     * to the panel value. The same chips write an override here and the global value elsewhere.
     */
    private void buildSlotSections() {
        Section durability = section(SLOT_TAB, "Durability");
        durability.chip("Same as panel", this::inheritsDurability, 0, this::inheritDurability);
        addDurabilityChips(durability);

        Section cooldown = section(SLOT_TAB, "Cooldown");
        cooldown.chip("Same as panel", this::inheritsCooldown, 0, this::inheritCooldown);
        addCooldownChips(cooldown);

        Section motion = section(SLOT_TAB, "Icon motion");
        motion.chip("Same as panel", this::inheritsMotion, 0, this::inheritMotion);
        addMotionChips(motion);

        Section light = section(SLOT_TAB, "Icon light");
        light.chip("Same as panel", this::inheritsLight, 0, this::inheritLight);
        addLightChips(light);

        Section reset = section(SLOT_TAB, "This slot");
        reset.chip("Reset slot", () -> false, 0, () -> {
            clearOverride();
            rebuildSections();
        });
    }

    private void addDurabilityChips(Section section) {
        DurabilityStyle[] order = {DurabilityStyle.BAR, DurabilityStyle.SEGMENTS,
                DurabilityStyle.PIPS, DurabilityStyle.TEXT, DurabilityStyle.VALUE,
                DurabilityStyle.RING, DurabilityStyle.BATTERY, DurabilityStyle.OFF};
        for (DurabilityStyle style : order) {
            section.chip(durabilityLabel(style), () -> durabilityActive(style), 0,
                    () -> setDurability(style));
        }
    }

    private void addCooldownChips(Section section) {
        CooldownStyle[] order = {CooldownStyle.ARC, CooldownStyle.TEXT, CooldownStyle.BAR,
                CooldownStyle.FRAME, CooldownStyle.OFF};
        for (CooldownStyle style : order) {
            section.chipIf(cooldownLabel(style), () -> cooldownActive(style), 0,
                    () -> setCooldown(style),
                    () -> style != CooldownStyle.FRAME || hasSlotBorder(),
                    "Needs a visible slot border");
        }
    }

    private void addMotionChips(Section section) {
        for (int i = 0; i < MOTION_IDS.length; i++) {
            String id = MOTION_IDS[i];
            section.chip(MOTION_LABELS[i], () -> motionActive(id), 0, () -> setMotion(id));
        }
    }

    private void addLightChips(Section section) {
        for (int i = 0; i < LIGHT_IDS.length; i++) {
            String id = LIGHT_IDS[i];
            section.chip(LIGHT_LABELS[i], () -> lightActive(id), 0, () -> setLight(id));
        }
    }

    private void addChoiceChips(Section section, String[] ids, String[] labels,
                                java.util.function.Supplier<String> get,
                                java.util.function.Consumer<String> set,
                                BooleanSupplier enabled, String why) {
        for (int i = 0; i < ids.length; i++) {
            String id = ids[i];
            section.chipIf(labels[i], () -> id.equals(get.get()), 0, () -> {
                set.accept(id);
                save();
            }, enabled, why);
        }
    }

    private static String durabilityLabel(DurabilityStyle style) {
        return switch (style) {
            case BAR -> "Bar";
            case SEGMENTS -> "Segments";
            case PIPS -> "Pips";
            case TEXT -> "Percent";
            case VALUE -> "Value";
            case RING -> "Ring";
            case BATTERY -> "Battery";
            case OFF -> "Off";
        };
    }

    private static String cooldownLabel(CooldownStyle style) {
        return switch (style) {
            case ARC -> "Arc";
            case TEXT -> "Text";
            case BAR -> "Bar";
            case FRAME -> "Frame";
            case OFF -> "Off";
        };
    }

    // ---- Per-slot vs global ----

    private String target() {
        if (targetSlot != null) {
            return targetSlot;
        }
        return previewBounds.isEmpty() ? null : previewBounds.get(0).key();
    }

    private boolean global() {
        return targetAll;
    }

    private boolean durabilityActive(DurabilityStyle style) {
        if (global()) {
            return config.showsDurability() && config.durabilityStyle() == style
                    || (!config.durability && style == DurabilityStyle.OFF);
        }
        SlotOverride override = config.override(target());
        String id = override == null ? "" : override.durability;
        return style.id().equals(id);
    }

    private void setDurability(DurabilityStyle style) {
        if (global()) {
            config.durability = style != DurabilityStyle.OFF;
            config.durabilityStyle = style.id();
        } else if (target() != null) {
            config.overrideOrCreate(target()).durability = style.id();
        }
        save();
    }

    private boolean cooldownActive(CooldownStyle style) {
        if (global()) {
            return config.cooldowns && config.cooldownStyle() == style
                    || (!config.cooldowns && style == CooldownStyle.OFF);
        }
        SlotOverride override = config.override(target());
        String id = override == null ? "" : override.cooldown;
        return style.id().equals(id);
    }

    private void setCooldown(CooldownStyle style) {
        if (global()) {
            config.cooldowns = style != CooldownStyle.OFF;
            config.cooldownStyle = style.id();
        } else if (target() != null) {
            config.overrideOrCreate(target()).cooldown = style.id();
        }
        save();
    }

    private boolean motionActive(String id) {
        return id.equals(global() ? panelMotion() : config.effectFor(target()));
    }

    private void setMotion(String id) {
        if (global()) {
            config.iconEffect = id;
        } else if (target() != null) {
            config.overrideOrCreate(target()).effect = id;
        }
        save();
    }

    private boolean lightActive(String id) {
        return id.equals(global() ? panelLight() : config.lightFor(target()));
    }

    private void setLight(String id) {
        if (global()) {
            config.iconLight = id;
        } else if (target() != null) {
            config.overrideOrCreate(target()).light = id;
        }
        save();
    }

    private String panelMotion() {
        String id = config.iconEffect == null || config.iconEffect.isBlank()
                ? "none" : config.iconEffect;
        return HudConfig.MOTION_EFFECTS.contains(id) ? id : "none";
    }

    private String panelLight() {
        String id = config.iconLight == null || config.iconLight.isBlank()
                ? "none" : config.iconLight;
        return HudConfig.LIGHT_EFFECTS.contains(id) ? id : "none";
    }

    /** The speed and strength rows only mean something once an icon effect is actually on. */
    private boolean anyEffect() {
        if (global()) {
            return !"none".equals(panelMotion()) || !"none".equals(panelLight());
        }
        return config.anyIconEffectFor(target());
    }

    private boolean inheritsMotion() {
        SlotOverride override = overrideOf();
        return override == null || override.effect == null || override.effect.isBlank();
    }

    private void inheritMotion() {
        SlotOverride override = overrideOf();
        if (override != null) {
            override.effect = "";
            save();
        }
    }

    private boolean inheritsLight() {
        SlotOverride override = overrideOf();
        return override == null || override.light == null || override.light.isBlank();
    }

    private void inheritLight() {
        SlotOverride override = overrideOf();
        if (override != null) {
            override.light = "";
            save();
        }
    }

    private void save() {
        ConfigStore.save();
    }

    // ---- Render ----

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        hits.clear();
        renderBackground(graphics, mouseX, mouseY, partialTick);
        backgroundDrawn = true;
        GuiBackdrop.draw(graphics, width, height);

        graphics.drawString(font, UiText.s("HUD Design"), MARGIN, UiTheme.HEADER_TITLE_Y,
                UiTheme.TITLE, true);

        sideLayout = width >= 560;
        previewW = sideLayout ? Math.max(160, Math.min(210, width / 5)) : width - MARGIN * 2;
        previewH = sideLayout ? Math.max(150, Math.min(220, height / 3))
                : (height < 400 ? 92 : 120);
        // A single slot is selected: keep room under the well for the unlock chip, otherwise it
        // would have to float over the preview or the target chips.
        int lockPad = targetAll ? 0 : CHIP_H + 6;
        int previewTop = sideLayout ? HEADER + 30 : MARGIN;
        previewH = Math.max(60, Math.min(previewH, height - previewTop - MARGIN - lockPad));
        previewX = sideLayout ? width - MARGIN - previewW : MARGIN;
        previewY = sideLayout ? previewTop : height - MARGIN - previewH - lockPad;
        contentX = MARGIN;
        contentRight = sideLayout ? previewX - 14 : width - MARGIN;
        clipTop = HEADER + 2 + tabRows() * 18 + 4;
        clipBottom = sideLayout ? height - MARGIN : Math.max(clipTop + 40, previewY - 44);
        // Clamp before layout so the hit boxes and the drawn chips always agree.
        scroll = Math.max(0, Math.min(scroll,
                Math.max(0, contentHeight - (clipBottom - clipTop))));

        var active = config.activeClusterRef();
        if (active != null) {
            int headerRight = width - MARGIN - 70 - 4;
            int available = Math.max(60, Math.min(contentRight - MARGIN, headerRight - MARGIN));
            graphics.drawString(font, fit(UiText.s("Panel") + ": " + active.name, available),
                    MARGIN, UiTheme.HEADER_SUB_Y, UiTheme.SUBTITLE, false);
        }
        drawTabs(graphics, mouseX, mouseY);

        drawPreview(graphics, mouseX, mouseY);

        graphics.enableScissor(contentX, clipTop, contentRight, clipBottom);
        int y = clipTop - (int) scroll;
        for (Section section : sections) {
            if (!section.group.equals(activeGroup())) {
                continue;
            }
            // Headings get an accent tick and a brighter line so they read as titles, not rows.
            graphics.fill(contentX, y + 1, contentX + 2, y + 8, UiTheme.ACCENT);
            graphics.drawString(font, section.label, contentX + 7, y, UiTheme.TITLE, true);
            y += 14;
            int x = contentX;
            for (Entry entry : section.entries) {
                if (entry instanceof Chip chip) {
                    chip.width = font.width(chip.label) + 14 + (chip.swatch != 0 ? 11 : 0);
                    chip.tip = TIPS.get(chip.raw);
                    if (x + chip.width > contentRight) {
                        x = contentX;
                        y += CHIP_H + CHIP_GAP;
                    }
                    chip.x = x;
                    chip.y = y;
                    drawChip(graphics, chip, mouseX, mouseY);
                    x += chip.width + CHIP_GAP;
                } else if (entry instanceof Number number) {
                    number.tip = TIPS.get(number.raw);
                    if (x != contentX) {
                        x = contentX;
                        y += CHIP_H + CHIP_GAP;
                    }
                    number.y = y;
                    drawNumber(graphics, number, mouseX, mouseY);
                    y += CHIP_H + CHIP_GAP;
                    x = contentX;
                }
            }
            y += CHIP_H + SECTION_GAP;
        }
        graphics.disableScissor();
        if (settingsLocked()) {
            drawLockScrim(graphics);
        }
        drawHoverTip(graphics, mouseX, mouseY);

        contentHeight = y + (int) scroll - clipTop + SECTION_GAP;
        int viewport = clipBottom - clipTop;
        double maxScroll = Math.max(0, contentHeight - viewport);
        if (scroll > maxScroll) {
            scroll = maxScroll;
        }
        drawScrollbar(graphics, viewport, maxScroll);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawHoverTip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (lockChipW > 0 && mouseX >= lockChipX && mouseX < lockChipX + lockChipW
                && mouseY >= lockChipY && mouseY < lockChipY + CHIP_H) {
            tipBox(graphics, lockChipTip, mouseX, mouseY);
            return;
        }
        if (settingsLocked()) {
            // The whole list is greyed out behind the padlock: say why instead of staying silent.
            if (mouseX >= contentX && mouseX < contentRight && mouseY >= clipTop
                    && mouseY < clipBottom) {
                tipBox(graphics, "Unlock this slot to change its settings", mouseX, mouseY);
            }
            return;
        }
        for (Section section : sections) {
            if (!section.group.equals(activeGroup())) {
                continue;
            }
            for (Entry entry : section.entries) {
                if (entry instanceof Chip chip && mouseX >= chip.x && mouseX < chip.x + chip.width
                        && mouseY >= chip.y && mouseY < chip.y + CHIP_H) {
                    // A dead chip explains itself instead of swallowing the hover.
                    String text = chip.enabled() ? chip.tip : chip.why;
                    if (text != null) tipBox(graphics, text, mouseX, mouseY);
                    return;
                }
                // Only the stepper controls own a tip; the empty middle of the row does not.
                if (entry instanceof Number number
                        && mouseY >= number.y && mouseY < number.y + CHIP_H) {
                    boolean overAuto = number.autoAllowed
                            && mouseX >= number.autoX && mouseX < number.autoX + number.autoW;
                    boolean overValue = mouseX >= number.valueX
                            && mouseX < number.valueX + 42;
                    boolean overStep = (mouseX >= number.minusX && mouseX < number.minusX + 16)
                            || (mouseX >= number.plusX && mouseX < number.plusX + 16);
                    if (!overAuto && !overValue && !overStep) {
                        continue;
                    }
                    String text = number.enabled() ? number.tip : number.why;
                    if (text != null) tipBox(graphics, text, mouseX, mouseY);
                    return;
                }
            }
        }
    }

    /** One slot is selected but its own settings are still locked: the list is read-only. */
    private boolean settingsLocked() {
        return !targetAll && !slotUnlocked;
    }

    /** A slot is unlocked, so the list shows only what a per-slot override can change. */
    private boolean slotMode() {
        return !targetAll && slotUnlocked;
    }

    private String activeGroup() {
        return slotMode() ? UiText.group(SLOT_TAB) : tab;
    }

    private void rebuildSections() {
        sections.clear();
        buildSections();
        scroll = 0;
    }

    /**
     * Unlock opens this slot's own list. Locking is the reset: the slot drops its overrides and
     * follows the panel again, and unlocking later starts from the panel values.
     */
    private void toggleUnlock() {
        String key = target();
        if (slotUnlocked) {
            clearOverride();
            if (key != null) config.setSlotUnlocked(key, false);
            slotUnlocked = false;
        } else {
            slotUnlocked = true;
            if (key != null) config.setSlotUnlocked(key, true);
        }
        save();
        rebuildSections();
    }

    private SlotOverride overrideOf() {
        String key = target();
        return key == null ? null : config.override(key);
    }

    private void clearOverride() {
        String key = target();
        if (key != null && config.slotOverrides != null) {
            config.slotOverrides.remove(key);
            save();
        }
    }

    private boolean inheritsDurability() {
        SlotOverride override = overrideOf();
        return override == null || override.durability == null || override.durability.isBlank();
    }

    private void inheritDurability() {
        SlotOverride override = overrideOf();
        if (override != null) {
            override.durability = "";
            save();
        }
    }

    private boolean inheritsCooldown() {
        SlotOverride override = overrideOf();
        return override == null || override.cooldown == null || override.cooldown.isBlank();
    }

    private void inheritCooldown() {
        SlotOverride override = overrideOf();
        if (override != null) {
            override.cooldown = "";
            save();
        }
    }

    private void drawLockScrim(GuiGraphics graphics) {
        graphics.fill(contentX - 2, clipTop, contentRight + 2, clipBottom, 0xB30B0E14);
        int cx = (contentX + contentRight) / 2;
        int cy = (clipTop + clipBottom) / 2 - 6;
        drawLockIcon(graphics, cx - 4, cy - 26, UiTheme.SUBTITLE);
        graphics.drawCenteredString(font, UiText.s("Slot settings locked"), cx, cy - 8,
                UiTheme.TITLE);
        graphics.drawCenteredString(font, UiText.s("Unlock to edit this slot"), cx, cy + 5,
                UiTheme.SUBTITLE);
    }

    /** Small padlock: shackle on top, body below, keyhole punched through. */
    private void drawLockIcon(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x + 3, y, x + 6, y + 1, color);
        graphics.fill(x + 2, y + 1, x + 3, y + 5, color);
        graphics.fill(x + 6, y + 1, x + 7, y + 5, color);
        StylePainter.roundRect(graphics, x, y + 4, 9, 8, 2, color);
        graphics.fill(x + 4, y + 6, x + 5, y + 9, UiTheme.WELL);
    }

    private void tipBox(GuiGraphics graphics, String rawText, int mouseX, int mouseY) {
        UiTheme.tooltip(graphics, font, UiText.s(rawText), mouseX, mouseY, width,
                UiTheme.ACCENT);
    }

    private void drawScrollbar(GuiGraphics graphics, int viewport, double maxScroll) {
        if (maxScroll <= 0) {
            scrollThumbHeight = 0;
            return;
        }
        int trackX = contentRight + 4;
        int trackW = 6;
        scrollTrackTop = clipTop;
        scrollTrackHeight = viewport;
        StylePainter.roundRect(graphics, trackX, clipTop, trackW, viewport, 3, UiTheme.INSET);
        StylePainter.outline(graphics, trackX, clipTop, trackW, viewport, 3, UiTheme.OUTLINE);
        int thumbH = Math.max(24, (int) (viewport * (viewport / (double) contentHeight)));
        scrollThumbHeight = thumbH;
        int thumbY = clipTop + (int) ((viewport - thumbH) * (scroll / maxScroll));
        StylePainter.roundRect(graphics, trackX + 1, thumbY, trackW - 2, thumbH, 2,
                UiTheme.ACCENT);
        StylePainter.outline(graphics, trackX + 1, thumbY, trackW - 2, thumbH, 2,
                StylePainter.alpha(UiTheme.BRIGHT, 0.45f));
    }

    /** The track is 6 px wide but the grab area is 10 px, so it is actually usable. */
    private boolean scrollbarHit(double mouseX, double mouseY) {
        return scrollThumbHeight > 0 && mouseX >= contentRight + 2
                && mouseX <= contentRight + 12
                && mouseY >= scrollTrackTop && mouseY < scrollTrackTop + scrollTrackHeight;
    }

    private double maxScroll() {
        return Math.max(0, contentHeight - (clipBottom - clipTop));
    }

    private double scrollFromMouse(double mouseY, double grab) {
        int span = Math.max(1, scrollTrackHeight - scrollThumbHeight);
        double ratio = (mouseY - scrollTrackTop - grab) / span;
        return Math.max(0, Math.min(1, ratio)) * maxScroll();
    }

    private double scrollThumbY() {
        int span = Math.max(1, scrollTrackHeight - scrollThumbHeight);
        return scrollTrackTop + span * (scroll / Math.max(1e-6, maxScroll()));
    }

    /** Tab rows needed at the current width; long translations wrap instead of running off-screen. */
    private int tabRows() {
        if (slotMode()) {
            return 1;
        }
        int rows = 1;
        int x = contentX;
        for (String id : TABS) {
            int w = font.width(UiText.group(id)) + 10;
            if (x > contentX && x + w > contentRight) {
                rows++;
                x = contentX;
            }
            x += w + 2;
        }
        return rows;
    }

    /** Flat tab bar: one group per tab, so the visible list never becomes a long scroll. */
    private void drawTabs(GuiGraphics graphics, int mouseX, int mouseY) {
        if (slotMode()) {
            // Per-slot mode has no tabs: one line names the slot the whole list is editing.
            String name = targetSlot == null ? "" : "  \u00b7  " + shortSlot(targetSlot);
            graphics.drawString(font,
                    fit(UiText.s("Slot settings") + name, contentRight - contentX),
                    contentX, HEADER + 4, UiTheme.TITLE, true);
            return;
        }
        int y = HEADER + 2;
        int x = contentX;
        for (String id : TABS) {
            String label = UiText.group(id);
            int w = font.width(label) + 10;
            if (x > contentX && x + w > contentRight) {
                x = contentX;
                y += 18;
            }
            boolean active = id.equals(tab);
            boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + 16;
            graphics.drawString(font, label, x + 5, y + 3,
                    active ? UiTheme.TITLE : hovered ? UiTheme.BRIGHT : UiTheme.SUBTITLE, active);
            if (active) {
                graphics.fill(x + 5, y + 13, x + w - 5, y + 15, UiTheme.ACCENT);
            } else if (hovered) {
                graphics.fill(x + 5, y + 14, x + w - 5, y + 15, UiTheme.OUTLINE_HOVER);
            }
            hits.add(new Hit(x, y, w, 16, () -> {
                tab = id;
                scroll = 0;
            }));
            x += w + 2;
        }
    }

    private void drawPreview(GuiGraphics graphics, int mouseX, int mouseY) {
        // Only the panel being designed is drawn, so a look change never reads as "both panels".
        String solo = config.cluster(config.activeCluster) == null ? null : config.activeCluster;
        int[] size = HudRenderer.measure(previewSlots, config, solo);
        // The demo cooldown/ability only exist to explain the bar and motion chips; on every other
        // tab the preview shows exactly what the player will see in game. They stay still unless
        // the motion tab is open, so browsing styles never makes an untouched slot blink.
        boolean barsTab = slotMode() || "Bars".equals(tab);
        boolean motionTab = "Motion".equals(tab);
        String caption = UiText.s("Preview") + "  \u00b7  " + size[0] + "\u00d7" + size[1];
        if (barsTab || motionTab) {
            caption += "  \u00b7  " + UiText.s("Demo");
        }
        if (!targetAll && targetSlot != null) {
            caption += "  \u00b7  " + shortSlot(targetSlot);
        }
        graphics.drawString(font, fit(caption, previewW), previewX, previewY - 36, UiTheme.INFO,
                false);
        drawTargetChips(graphics, mouseX, mouseY);

        StylePainter.roundRect(graphics, previewX, previewY, previewW, previewH,
                UiTheme.WELL_RADIUS, UiTheme.WELL);
        StylePainter.outline(graphics, previewX, previewY, previewW, previewH,
                UiTheme.WELL_RADIUS, UiTheme.OUTLINE);
        // Centre the real panel inside the well so the preview never hugs one corner.
        int availW = previewW - 16;
        int availH = previewH - 16;
        float fitScale = Math.min(1.0f, Math.min(availW / (float) Math.max(1, size[0]),
                availH / (float) Math.max(1, size[1])));
        int drawW = Math.max(1, Math.round(size[0] * fitScale));
        int drawH = Math.max(1, Math.round(size[1] * fitScale));
        int drawX = previewX + (previewW - drawW) / 2;
        int drawY = previewY + (previewH - drawH) / 2;
        HudRenderer.setPreviewDemos(barsTab || motionTab, motionTab);
        previewBounds = HudRenderer.renderInRect(graphics, previewSlots, config, solo,
                drawX, drawY, drawW, drawH);
        for (HudRenderer.PreviewSlot slot : previewBounds) {
            hits.add(new Hit(slot.x(), slot.y(), slot.w(), slot.h(),
                    () -> setTargetSlot(slot.key())));
        }

        if (!targetAll) {
            if (targetSlot == null) {
                setTargetSlot(rememberedSlot());
            } else {
                HudRenderer.setSelection(targetSlot);
            }
        }
        drawLockChip(graphics, mouseX, mouseY);
    }

    /** The slot to reopen when the player goes back to "One slot". */
    private String rememberedSlot() {
        if (lastSlot != null) {
            for (HudRenderer.PreviewSlot slot : previewBounds) {
                if (slot.key().equals(lastSlot)) return lastSlot;
            }
        }
        return previewBounds.isEmpty() ? null : previewBounds.get(0).key();
    }

    /**
     * Points editing at one slot or at the whole panel. The unlock state belongs to the slot, so
     * coming back to an unlocked slot reopens its own settings with its own values.
     */
    private void setTargetSlot(String key) {
        boolean wasSlotMode = slotMode();
        targetSlot = key;
        targetAll = key == null;
        if (key != null) lastSlot = key;
        slotUnlocked = key != null && config.isSlotUnlocked(key);
        HudRenderer.setSelection(key);
        if (wasSlotMode != slotMode()) rebuildSections();
    }

    /** "Unlock slot" under the preview: the only way into per-slot settings. */
    private void drawLockChip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (targetAll) {
            lockChipW = 0;
            return;
        }
        String label = UiText.s(slotUnlocked ? "Slot unlocked" : "Unlock slot");
        int w = font.width(label) + 26;
        int x = previewX;
        int y = previewY + previewH + 6;
        boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + CHIP_H;
        UiTheme.chip(graphics, x, y, w, CHIP_H, slotUnlocked, hovered, true, UiTheme.ACCENT);
        drawLockIcon(graphics, x + 7, y + 5, slotUnlocked ? UiTheme.ON_ACCENT : UiTheme.ACCENT);
        graphics.drawString(font, label, x + 20, y + 5,
                UiTheme.chipText(slotUnlocked, true), false);
        hits.add(new Hit(x, y, w, CHIP_H, this::toggleUnlock));
        lockChipX = x;
        lockChipY = y;
        lockChipW = w;
        lockChipTip = slotUnlocked ? "Close and use the panel values" : "Edit this slot on its own";
    }

    private void drawTargetChips(GuiGraphics graphics, int mouseX, int mouseY) {
        int y = previewY - 18;
        int x = previewX;
        String[] labels = {UiText.s("All slots"), UiText.s("One slot")};
        for (int i = 0; i < 2; i++) {
            final boolean allTargets = i == 0;
            boolean active = allTargets == targetAll;
            int w = font.width(labels[i]) + 14;
            boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + CHIP_H;
            UiTheme.chip(graphics, x, y, w, CHIP_H, active, hovered, true, UiTheme.ACCENT);
            graphics.drawString(font, labels[i], x + 7, y + 5,
                    UiTheme.chipText(active, true), false);
            hits.add(new Hit(x, y, w, CHIP_H, () -> selectTarget(allTargets)));
            x += w + 4;
        }
    }

    /** Switches the FX scope; the unlock state of the chosen slot is kept either way. */
    private void selectTarget(boolean all) {
        setTargetSlot(all ? null : rememberedSlot());
    }
    /** Everything clickable this frame, registered while drawing. */
    private final List<Hit> hits = new ArrayList<>();

    private record Hit(int x, int y, int w, int h, Runnable action) {
    }

    private void drawChip(GuiGraphics graphics, Chip chip, int mouseX, int mouseY) {
        if (chip.enabled() && chip.width > 0 && chip.y + CHIP_H > clipTop
                && chip.y < clipBottom && !settingsLocked()) {
            hits.add(new Hit(chip.x, chip.y, chip.width, CHIP_H, chip.action));
        }
        if (!chip.enabled()) {
            UiTheme.chip(graphics, chip.x, chip.y, chip.width, CHIP_H, false, false, false,
                    UiTheme.ACCENT);
            graphics.drawString(font, chip.label, chip.x + 7, chip.y + 5,
                    UiTheme.DISABLED, false);
            return;
        }
        boolean active = chip.active.getAsBoolean();
        boolean hovered = mouseX >= chip.x && mouseX < chip.x + chip.width
                && mouseY >= chip.y && mouseY < chip.y + CHIP_H && chip.width > 0;

        UiTheme.chip(graphics, chip.x, chip.y, chip.width, CHIP_H, active, hovered, true,
                UiTheme.ACCENT);
        int textX = chip.x + 7;
        if (chip.swatch != 0) {
            int swatchX = chip.x + 9;
            int swatchY = chip.y + 5;
            StylePainter.roundRect(graphics, swatchX, swatchY, 8, 8, 2, chip.swatch);
            StylePainter.outline(graphics, swatchX, swatchY, 8, 8, 2,
                    StylePainter.alpha(UiTheme.ON_ACCENT, 0.33f));
            textX = swatchX + 12;
        }
        graphics.drawString(font, chip.label, textX, chip.y + 5,
                UiTheme.chipText(active, true), false);
    }

    private void drawNumber(GuiGraphics graphics, Number number, int mouseX, int mouseY) {
        int value = number.get.getAsInt();
        boolean auto = !number.signed && value < 0;
        int shown = auto ? number.auto : value;
        boolean enabled = number.enabled();

        // Stepper geometry: [-][value][+] hugging the right edge as one group, then Auto. The Auto
        // chip is sized from its own text, so a longer translation never spills out of it.
        int autoW = Math.max(34, Math.max(font.width(UiText.s("Auto")),
                font.width(UiText.s("Set"))) + 12);
        int plusX = contentRight - 16;
        int valueX = plusX - 4 - 42;
        int minusX = valueX - 4 - 16;
        int autoX = minusX - 6 - autoW;
        number.autoX = autoX;
        number.autoW = autoW;
        number.minusX = minusX;
        number.plusX = plusX;
        number.valueX = valueX;

        boolean onScreen = number.y + CHIP_H > clipTop && number.y < clipBottom;
        boolean live = onScreen && enabled && !settingsLocked();
        if (live && number.autoAllowed) {
            hits.add(new Hit(autoX, number.y, autoW, CHIP_H, () -> {
                number.set.accept(-1);
                save();
            }));
        }
        if (live) {
            hits.add(new Hit(minusX, number.y, 16, CHIP_H, () -> {
                number.set.accept(Math.max(number.min, shown - number.step));
                save();
            }));
            hits.add(new Hit(plusX, number.y, 16, CHIP_H, () -> {
                number.set.accept(Math.min(number.max, shown + number.step));
                save();
            }));
        }

        graphics.drawString(font, fit(number.label, Math.max(24, autoX - contentX - 8)),
                contentX, number.y + 5, enabled ? UiTheme.TEXT : UiTheme.DISABLED, false);

        if (number.autoAllowed) {
            drawMini(graphics, autoX, number.y, autoW, auto ? "Auto" : "Set", auto, enabled,
                    mouseX, mouseY, autoX + autoW, number.y + CHIP_H);
        }
        // The value sits in its own well so the two step buttons read as one control.
        StylePainter.roundRect(graphics, valueX, number.y, 42, CHIP_H, UiTheme.CHIP_RADIUS,
                UiTheme.INSET);
        drawMini(graphics, minusX, number.y, 16, "-", false, enabled, mouseX, mouseY,
                minusX + 16, number.y + CHIP_H);
        drawMini(graphics, plusX, number.y, 16, "+", false, enabled, mouseX, mouseY,
                plusX + 16, number.y + CHIP_H);

        String text = (number.signed && shown > 0 ? "+" : "") + shown + "px";
        graphics.drawString(font, text, valueX + (42 - font.width(text)) / 2, number.y + 5,
                !enabled ? UiTheme.DISABLED : auto ? UiTheme.INFO : UiTheme.TITLE, false);
    }

    private void drawMini(GuiGraphics graphics, int x, int y, int w, String rawLabel,
                          boolean active, boolean enabled, int mouseX, int mouseY, int right,
                          int bottom) {
        String label = UiText.s(rawLabel);
        boolean hovered = enabled && mouseX >= x && mouseX < right && mouseY >= y
                && mouseY < bottom;
        UiTheme.chip(graphics, x, y, w, CHIP_H, active, hovered, enabled, UiTheme.ACCENT);
        graphics.drawString(font, label, x + (w - font.width(label)) / 2, y + 5,
                UiTheme.chipText(active, enabled), false);
    }

    // ---- Input ----

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (scrollbarHit(mouseX, mouseY)) {
            double thumbY = scrollThumbY();
            boolean onThumb = mouseY >= thumbY && mouseY < thumbY + scrollThumbHeight;
            scrollGrab = onThumb ? mouseY - thumbY : scrollThumbHeight / 2.0;
            draggingScrollbar = true;
            scroll = scrollFromMouse(mouseY, scrollGrab);
            return true;
        }
        // One list, built while drawing: what you see is what you can click.
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit hit = hits.get(i);
            if (mouseX >= hit.x() && mouseX < hit.x() + hit.w()
                    && mouseY >= hit.y() && mouseY < hit.y() + hit.h()) {
                hit.action().run();
                return true;
            }
        }
        return false;
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
        int viewport = clipBottom - clipTop;
        double maxScroll = Math.max(0, contentHeight - viewport);
        scroll = Math.max(0, Math.min(maxScroll, scroll - scrollY * 18.0));
        return true;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (backgroundDrawn) {
            return;
        }
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        leave(onBack);
    }

    /** Commits the panel look, then goes wherever the button asked. */
    private void leave(Runnable target) {
        ConfigStore.endEdit(true);
        ConfigStore.flush();
        // The focus ring is an editing aid; leaving the menu must not leave a ring on the live HUD.
        HudRenderer.setSelection(null);
        HudRenderer.setPreviewDemos(false, false);
        target.run();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---- Model ----

    /** One palette row: Auto plus the shared colours, so every feedback layer can differ. */
    private void addColors(Section section, java.util.function.Supplier<String> get,
                           java.util.function.Consumer<String> set) {
        section.chip("Auto", () -> "auto".equalsIgnoreCase(get.get()), 0, () -> {
            set.accept("auto");
            save();
        });
        for (int i = 0; i < COLOR_IDS.length; i++) {
            String id = COLOR_IDS[i];
            section.chip(COLOR_LABELS[i], () -> id.equalsIgnoreCase(get.get()), COLOR_ARGB[i],
                    () -> {
                        set.accept(id);
                        save();
                    });
        }
    }

    /** Thickness / opacity / track (+ pulse and reverse for the timed layers). */
    private void layerDetail(Section detail, LayerOptions opts, boolean motion) {
        detail.fixedNumber("Thickness", () -> opts.thickness, value -> opts.thickness = value,
                1, 4);
        detail.fixedNumber("Opacity", () -> opts.opacity, value -> opts.opacity = value,
                30, 100, 10);
        detail.chip("Track", () -> opts.track, 0, () -> {
            opts.track = !opts.track;
            save();
        });
        if (motion) {
            detail.chip("Blink", () -> opts.pulse, 0, () -> {
                opts.pulse = !opts.pulse;
                save();
            });
            detail.chip("Reverse", () -> opts.reverse, 0, () -> {
                opts.reverse = !opts.reverse;
                save();
            });
        }
    }

    private void layerText(Section section, LayerOptions opts) {
        String[][] kinds = {{"seconds", "Seconds"}, {"int", "Int"}, {"percent", "Percent"}};
        for (String[] kind : kinds) {
            String id = kind[0];
            section.chip(kind[1], () -> id.equals(opts.textKind()), 0, () -> {
                opts.text = id;
                save();
            });
        }
    }

    private Section section(String group, String label) {
        Section section = new Section(group, label);
        sections.add(section);
        return section;
    }

    private static String shortSlot(String key) {
        String[] parts = key.split("/");
        if (parts.length >= 3) {
            return parts[parts.length - 2] + "/" + parts[parts.length - 1];
        }
        return key;
    }

    private String fit(String text, int width) {
        if (font.width(text) <= width) {
            return text;
        }
        String value = text;
        while (value.length() > 1 && font.width(value + "\u2026") > width) {
            value = value.substring(0, value.length() - 1);
        }
        return value + "\u2026";
    }

    private boolean lightPanel() {
        PanelStyle panel = PanelStyle.resolve(config.panelStyle, config.style());
        return panel == PanelStyle.VANILLA || panel == PanelStyle.PARCHMENT;
    }

    private boolean hasSlotBorder() {
        return SlotStyle.resolve(config.slotStyle, config.style()) != SlotStyle.NONE;
    }

    /** The flowing border only has somewhere to flow when a layer actually uses the frame style. */
    private boolean hasFrameLayer() {
        return (config.cooldowns && config.cooldownStyle() == CooldownStyle.FRAME)
                || config.abilityStyle() == com.toancao.loadouttc.hud.AbilityStyle.FRAME;
    }

    private static int panelSwatch(PanelStyle style) {
        return switch (style) {
            case NONE -> 0x00000000;
            case VANILLA -> 0xFFC6C6C6;
            case DARK -> 0xFF0D1117;
            case GLASS -> 0xCC121620;
            case WOOD -> 0xFF2A1E15;
            default -> 0x33FFFFFF;
        };
    }

    private static int slotSwatch(SlotStyle style) {
        return switch (style) {
            case NONE -> 0x00000000;
            case VANILLA -> 0xFF8B8B8B;
            case STEEL -> 0xFF0B0F15;
            case HEX -> 0xFF23272E;
            case INSET -> 0xFF050403;
            case ROUNDED -> 0x33FFFFFF;
            case CIRCLE -> 0x44FFFFFF;
            default -> 0x22FFFFFF;
        };
    }

    private interface Entry {
    }

    private static final class Chip implements Entry {
        private final String label;
        /** English text, used to look up the tooltip and the translation. */
        private final String raw;
        private final BooleanSupplier active;
        private final int swatch;
        private final Runnable action;
        private final BooleanSupplier enabled;
        /** Shown instead of the tip while the chip is greyed out, so a dead chip explains itself. */
        private final String why;
        private String tip;
        private int x;
        private int y;
        private int width;

        private Chip(String raw, BooleanSupplier active, int swatch, Runnable action,
                     BooleanSupplier enabled, String why) {
            this.raw = raw;
            this.label = UiText.s(raw);
            this.active = active;
            this.swatch = swatch;
            this.action = action;
            this.enabled = enabled;
            this.why = why;
        }

        private boolean enabled() {
            return enabled == null || enabled.getAsBoolean();
        }
    }

    private static final class Number implements Entry {
        private final String label;
        /** English text, used to look up the tooltip and the translation. */
        private final String raw;
        private final IntSupplier get;
        private final IntConsumer set;
        private final int auto;
        private final int min;
        private final int max;
        private final boolean autoAllowed;
        private final boolean signed;
        private final int step;
        /** A row that cannot apply right now stays visible but greyed out. */
        private final BooleanSupplier enabled;
        /** Shown instead of the tip while the row is greyed out. */
        private final String why;
        private String tip;
        private int y;
        private int autoX;
        private int autoW;
        private int minusX;
        private int plusX;
        private int valueX;

        private Number(String raw, IntSupplier get, IntConsumer set, int auto, int min,
                       int max, boolean autoAllowed, boolean signed, int step,
                       BooleanSupplier enabled, String why) {
            this.raw = raw;
            this.label = UiText.s(raw);
            this.get = get;
            this.set = set;
            this.auto = auto;
            this.min = min;
            this.max = max;
            this.autoAllowed = autoAllowed;
            this.signed = signed;
            this.step = step;
            this.enabled = enabled;
            this.why = why;
        }

        private boolean enabled() {
            return enabled == null || enabled.getAsBoolean();
        }
    }

    private static final class Section {
        private final String group;
        private final String label;
        private final List<Entry> entries = new ArrayList<>();

        private Section(String group, String label) {
            this.group = UiText.group(group);
            this.label = UiText.s(label);
        }

        private void chip(String label, BooleanSupplier active, int swatch, Runnable action) {
            entries.add(new Chip(label, active, swatch, action, null, null));
        }

        private void chipIf(String label, BooleanSupplier active, int swatch, Runnable action,
                            BooleanSupplier enabled) {
            entries.add(new Chip(label, active, swatch, action, enabled, null));
        }

        private void chipIf(String label, BooleanSupplier active, int swatch, Runnable action,
                            BooleanSupplier enabled, String why) {
            entries.add(new Chip(label, active, swatch, action, enabled, why));
        }

        private void number(String label, IntSupplier get, IntConsumer set, int auto, int min,
                            int max) {
            entries.add(new Number(label, get, set, auto, min, max, true, false, 1, null, null));
        }

        private void fixedNumber(String label, IntSupplier get, IntConsumer set, int min,
                                 int max) {
            entries.add(new Number(label, get, set, 0, min, max, false, true, 1, null, null));
        }

        private void fixedNumber(String label, IntSupplier get, IntConsumer set, int min,
                                 int max, int step) {
            entries.add(new Number(label, get, set, 0, min, max, false, true, step, null, null));
        }

        private void fixedNumberIf(String label, IntSupplier get, IntConsumer set, int min,
                                   int max, BooleanSupplier enabled) {
            entries.add(new Number(label, get, set, 0, min, max, false, true, 1, enabled, null));
        }

        private void fixedNumberIf(String label, IntSupplier get, IntConsumer set, int min,
                                   int max, BooleanSupplier enabled, String why) {
            entries.add(new Number(label, get, set, 0, min, max, false, true, 1, enabled, why));
        }

        private void fixedNumberIf(String label, IntSupplier get, IntConsumer set, int min,
                                   int max, int step, BooleanSupplier enabled) {
            entries.add(new Number(label, get, set, 0, min, max, false, true, step, enabled, null));
        }

        private void fixedNumberIf(String label, IntSupplier get, IntConsumer set, int min,
                                   int max, int step, BooleanSupplier enabled, String why) {
            entries.add(new Number(label, get, set, 0, min, max, false, true, step, enabled, why));
        }
    }
}
