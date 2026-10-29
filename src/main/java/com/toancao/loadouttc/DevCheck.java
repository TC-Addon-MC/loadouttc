package com.toancao.loadouttc;

import com.toancao.loadouttc.compat.HudSlot;
import com.toancao.loadouttc.config.HudAnchor;
import com.toancao.loadouttc.config.HudConfig;
import com.toancao.loadouttc.config.HudCluster;
import com.toancao.loadouttc.config.LayerOptions;
import com.toancao.loadouttc.hud.HudRenderer;
import java.util.ArrayList;
import java.util.List;

/** Temporary headless check for the slot filter / sort logic. Not shipped in the jar. */
public final class DevCheck {
    private static int failures;

    public static void main(String[] args) {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        keys();
        uiText();
        clusters();
        layout();
        layers();
        sizes();
        effects();
        freshDefaults();
        apiSurface();
        defaults();
        filterAll();
        filterSelected();
        switchModes();
        lonePanel();
        ringLabels();
        sorting();
        prepare();
        order();
        panelPlacement();
        System.out.println(failures == 0 ? "ALL OK" : failures + " FAILURES");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void clusters() {
        HudConfig cfg = new HudConfig();
        cfg.sanitize();
        check("one default cluster", cfg.clusters.size() == 1);
        check("default cluster is catch-all", cfg.clusters.get(0).catchAll());

        String ring = "accessories/ring/0";
        String helmet = "vanilla/armor/helmet/0";
        check("catch-all claims unassigned", "main".equals(cfg.clusterIdFor(ring)));

        com.toancao.loadouttc.config.HudCluster split =
                cfg.split(java.util.List.of(ring), "Rings");
        check("split adds cluster", cfg.clusters.size() == 2);
        check("split owns its slot", split.id.equals(cfg.clusterIdFor(ring)));
        check("other slots stay in catch-all", "main".equals(cfg.clusterIdFor(helmet)));
        check("split lists the key", split.contains(ring));

        cfg.assign(ring, "main");
        check("assign back to rest", "main".equals(cfg.clusterIdFor(ring)));
        check("assign removes from split", !split.contains(ring));

        cfg.assign(ring, split.id);
        cfg.assign(helmet, split.id);
        check("one owner per slot", split.slots.size() == 2);

        com.toancao.loadouttc.config.HudConfig copy = cfg.copy();
        copy.clusters.get(0).name = "Renamed";
        check("copy keeps lists apart", cfg.clusters.get(0).name.equals("Main"));

        cfg.removeCluster(split.id);
        check("delete hides the panel's slots", !cfg.slotVisible(ring) && !cfg.slotVisible(helmet));
        check("delete keeps one panel", cfg.clusters.size() == 1);
        com.toancao.loadouttc.config.HudConfig survivor = cfg.clusters.get(0).resolve(cfg);
        check("delete stops the survivor drawing them",
                !survivor.slotVisible(ring) && !survivor.slotVisible(helmet));

        com.toancao.loadouttc.config.HudConfig wide = new com.toancao.loadouttc.config.HudConfig();
        wide.sanitize();
        wide.split(java.util.List.of("a/1"), "Extra");
        wide.removeCluster("main", java.util.List.of("a/1", "b/2"));
        check("catch-all delete hides leftovers", !wide.slotVisible("b/2") && wide.slotVisible("a/1"));
        check("catch-all role moves on", wide.clusters.size() == 1 && wide.clusters.get(0).rest);

        com.toancao.loadouttc.config.HudCluster styled = cfg.clusters.get(0);
        styled.adoptLook(cfg);
        styled.look.panelStyle = "dark";
        styled.look.opacity = 0.5f;
        HudConfig view = styled.resolve(cfg);
        check("override view panel", "dark".equals(view.panelStyle));
        check("override view opacity", Math.abs(view.opacity - 0.5f) < 0.01f);
        check("override leaves global", "none".equals(cfg.panelStyle));
        check("override keeps slot owner", view.clusterFor(ring) != null);

        HudConfig saved = new HudConfig();
        saved.sanitize();
        String panel = saved.split(java.util.List.of("accessories/ring/0"), "Rings").id;
        String json = new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(saved);
        HudConfig back = new com.google.gson.Gson().fromJson(json, HudConfig.class);
        back.sanitize();
        check("json keeps both panels", back.clusters.size() == 2);
        check("json keeps ownership", panel.equals(back.clusterIdFor("accessories/ring/0")));
        check("json keeps rest panel", back.cluster("main").rest);

        HudConfig locking = new HudConfig();
        locking.sanitize();
        locking.setSlotUnlocked(ring, true);
        check("unlock sticks to its slot", locking.isSlotUnlocked(ring));
        check("other slots stay locked", !locking.isSlotUnlocked(helmet));
        check("unlock survives a copy", locking.copy().isSlotUnlocked(ring));
        locking.setSlotUnlocked(ring, false);
        check("lock clears the slot", !locking.isSlotUnlocked(ring));

        HudConfig pos = new HudConfig();
        pos.sanitize();
        HudCluster extra = pos.addCluster("Extra");
        check("new panel has its own spot", extra.ownPosition && extra.x > 0 && extra.y > 0);
        String sharedBefore = pos.anchor;
        extra.ownPosition = true;
        extra.anchor = HudAnchor.CUSTOM.name();
        pos.activeCluster = extra.id;
        pos.cycleActiveAnchor();
        check("panel cycles alone", extra.ownPosition
                && !HudAnchor.CUSTOM.name().equals(extra.anchor));
        check("panel anchor leaves the shared anchor alone", sharedBefore.equals(pos.anchor));

        // A panel with its own look must still follow the shared anchor until it is moved.
        HudConfig shared = new HudConfig();
        shared.sanitize();
        HudCluster firstPanel = shared.clusters.get(0);
        String firstAnchor = firstPanel.anchor;
        HudCluster styledPanel = shared.addCluster("Styled");
        styledPanel.adoptLook(shared);
        styledPanel.anchor = HudAnchor.TOP_LEFT.name();
        shared.anchor = HudAnchor.BOTTOM_LEFT.name();
        check("panel keeps its own anchor",
                HudAnchor.TOP_LEFT.name().equals(styledPanel.resolve(shared).anchor));
        check("base anchor does not move other panels",
                firstAnchor.equals(firstPanel.resolve(shared).anchor));

        // Slot selection is per panel: what is ticked for one panel never reaches another.
        HudConfig live = new HudConfig();
        live.sanitize();
        HudCluster livePanel = live.addCluster("P");
        livePanel.adoptLook(live);
        livePanel.look.panelStyle = "dark";
        live.accent = "FF8800";
        livePanel.scope.sortMode = "name";
        livePanel.scope.emptySlots = true;
        livePanel.scope.setSlotVisible("vanilla/armor/helmet/0", false);
        HudConfig liveView = livePanel.resolve(live);
        HudConfig otherView = live.clusters.get(0).resolve(live);
        check("panel keeps its own look", "dark".equals(liveView.panelStyle));
        check("panel keeps its own sort", "name".equals(liveView.sortMode));
        check("panel keeps its own empty", liveView.emptySlots);
        check("panel keeps its own hidden slot",
                !liveView.slotVisible("vanilla/armor/helmet/0"));
        check("other panel keeps the default sort", "default".equals(otherView.sortMode));
        check("other panel keeps empty off", !otherView.emptySlots);
        check("other panel still shows the slot",
                otherView.slotVisible("vanilla/armor/helmet/0"));
        check("panel follows live accent", "FF8800".equals(liveView.accent));
        check("panel keeps its own slots owner", liveView.clusterFor("vanilla/armor/helmet/0") != null);

        // Two panels, two branches: nothing visual or positional crosses over.
        HudConfig two = new HudConfig();
        two.sanitize();
        HudCluster left = two.clusters.get(0);
        HudCluster right = two.addCluster("B");
        left.adoptLook(two);
        right.adoptLook(two);
        left.look.panelStyle = "dark";
        right.look.panelStyle = "glass";
        left.look.size = "large";
        right.look.size = "small";
        left.anchor = HudAnchor.TOP_LEFT.name();
        left.x = 10;
        left.y = 20;
        right.anchor = HudAnchor.BOTTOM_RIGHT.name();
        right.x = 30;
        right.y = 40;
        check("panel A keeps its look", "dark".equals(left.resolve(two).panelStyle));
        check("panel B keeps its look", "glass".equals(right.resolve(two).panelStyle));
        check("panel A keeps its size", "large".equals(left.resolve(two).size));
        check("panel B keeps its size", "small".equals(right.resolve(two).size));
        check("panel A keeps its position", left.resolve(two).x == 10 && left.resolve(two).y == 20);
        check("panel B keeps its position", right.resolve(two).x == 30 && right.resolve(two).y == 40);


        HudConfig merged = new HudConfig();
        merged.sanitize();
        HudCluster mainPanel = merged.clusters.get(0);
        HudCluster splitPanel = merged.split(java.util.List.of("k1", "k2"), "Split");
        check("merge moves slots", merged.mergeInto(splitPanel.id, mainPanel.id, false));
        check("merge keeps slots", mainPanel.contains("k1") && mainPanel.contains("k2"));
        check("merge removes the tail", merged.cluster(splitPanel.id) == null);
        check("merge keeps a rest panel", mainPanel.rest);

        HudConfig tailed = new HudConfig();
        tailed.sanitize();
        HudCluster head = tailed.split(java.util.List.of("h"), "Head");
        HudCluster tail = tailed.split(java.util.List.of("t"), "Tail");
        tail.look = tailed.copy();
        tail.look.clusters = new java.util.ArrayList<>();
        tail.look.panelStyle = "dark";
        check("merge takes the tail look",
                tailed.mergeInto(tail.id, head.id, true) && "dark".equals(head.look.panelStyle));

        // Size follows the branch: a panel with its own look keeps its own scale.
        HudConfig sized = new HudConfig();
        sized.sanitize();
        HudCluster scaled = sized.addCluster("Scaled");
        scaled.adoptLook(sized);
        scaled.look.size = "large";
        sized.size = "small";
        check("panel keeps its own size", "large".equals(scaled.resolve(sized).size));
        HudCluster plain = sized.addCluster("Plain");
        check("panel without look follows size", "small".equals(plain.resolve(sized).size));

        HudConfig editing = new HudConfig();
        editing.sanitize();
        HudCluster target = editing.addCluster("Target");
        editing.activeCluster = target.id;
        editing.editTarget().size = "large";
        check("editTarget creates the panel look", target.look != null
                && "large".equals(target.look.size));
        check("editTarget leaves the shared size", !"large".equals(editing.size));
    }

    private static void uiText() {
        check("ui key plain", "loadouttc.ui.panel"
                .equals(com.toancao.loadouttc.util.UiText.key("Panel")));
        check("ui key punctuation", "loadouttc.ui.radius_border"
                .equals(com.toancao.loadouttc.util.UiText.key("Radius / border")));
        check("ui key brackets", "loadouttc.ui.ability_from_mods"
                .equals(com.toancao.loadouttc.util.UiText.key("Ability (from mods)")));
        check("ui key placeholder", "loadouttc.ui.slots_on_the_hud"
                .equals(com.toancao.loadouttc.util.UiText.key("%s slots on the HUD")));
        check("ui key digits", "loadouttc.ui.1_4_px_bar_height_or_ring_stroke"
                .equals(com.toancao.loadouttc.util.UiText.key("1-4 px: bar height or ring stroke")));
        check("ui key uppercase", "loadouttc.ui.accessories"
                .equals(com.toancao.loadouttc.util.UiText.key("Accessories")));
    }

    private static HudSlot slot(String source, String group, String slotId, int index, boolean empty) {
        String label = group.isEmpty() ? slotId
                : Character.toUpperCase(group.charAt(0)) + group.substring(1);
        return new HudSlot(source, group, label, slotId, index,
                empty ? net.minecraft.world.item.ItemStack.EMPTY
                        : new net.minecraft.world.item.ItemStack(
                                net.minecraft.world.item.Items.DIAMOND, index + 1));
    }

    private static List<HudSlot> sample() {
        List<HudSlot> out = new ArrayList<>();
        out.add(slot("vanilla", "armor", "helmet", 0, false));
        out.add(slot("vanilla", "armor", "boots", 0, false));
        out.add(slot("accessories", "ring", "ring", 0, false));
        out.add(slot("accessories", "ring", "ring", 1, true));
        out.add(slot("trinkets", "chest", "necklace", 0, false));
        return out;
    }

    private static void freshDefaults() {
        HudConfig cfg = new HudConfig();
        String panel = cfg.panelStyle;
        String slot = cfg.slotStyle;
        String text = cfg.textStyle;
        String ability = cfg.abilityStyle;
        String cooldown = cfg.cooldownStyle;
        boolean armor = cfg.vanillaArmor;
        boolean empty = cfg.emptySlots;
        String layout = cfg.layoutKind();
        cfg.sanitize();
        check("default panel survives", panel.equals(cfg.panelStyle));
        check("default slot survives", slot.equals(cfg.slotStyle));
        check("default text survives", text.equals(cfg.textStyle));
        check("default cooldown survives", cooldown.equals(cfg.cooldownStyle));
        check("default ability survives", ability.equals(cfg.abilityStyle));
        check("default ability is ring", "ring".equals(cfg.abilityStyle));
        check("default armour on", armor == cfg.vanillaArmor);
        check("default empty off", empty == cfg.emptySlots);
        check("default layout survives", layout.equals(cfg.layoutKind()));
        check("default frame cooldown allowed", "vanilla".equals(cfg.slotStyle));
        check("fresh default hides vanilla gear",
                !cfg.vanillaArmor && !cfg.vanillaOffhand
                        && !cfg.vanillaHotbar && !cfg.vanillaInventory);
        List<HudSlot> fresh = HudRenderer.prepare(sample(), cfg);
        check("fresh default keeps mod slots",
                fresh.stream().anyMatch(entry -> "accessories".equals(entry.source())));
        check("fresh default drops player inventory",
                fresh.stream().noneMatch(entry -> "vanilla".equals(entry.source())));
        check("fresh scope hides vanilla gear", !new com.toancao.loadouttc.config.SlotScope()
                .vanillaGroupEnabled("armor"));
    }

    private static void apiSurface() {
        com.toancao.loadouttc.api.ApiSlot ring = new com.toancao.loadouttc.api.ApiSlot(
                "accessories", "ring", "Ring", "ring", 0,
                net.minecraft.world.item.ItemStack.EMPTY);
        check("api slot key collapses a shared group", "accessories/ring/0".equals(ring.key()));
        com.toancao.loadouttc.api.ApiSlot necklace = new com.toancao.loadouttc.api.ApiSlot(
                "trinkets", "chest", "Chest", "necklace", 1,
                net.minecraft.world.item.ItemStack.EMPTY);
        check("api slot key keeps a distinct group",
                "trinkets/chest/necklace/1".equals(necklace.key()));
        check("api slot reports empty", ring.empty() && ring.itemId().isEmpty());
        check("api contract version", com.toancao.loadouttc.api.LoadoutTcApi.apiVersion() >= 4);
        check("api source entrypoint id",
                "loadouttc:slot_source".equals(com.toancao.loadouttc.api.SlotSource.ENTRYPOINT));

        List<com.toancao.loadouttc.compat.HudSlot> adapted = new ArrayList<>();
        new com.toancao.loadouttc.compat.SlotSourceAdapter(
                new com.toancao.loadouttc.api.SlotSource() {
                    public String id() { return "demo"; }
                    public boolean available() { return true; }
                    public void collect(net.minecraft.client.Minecraft minecraft,
                                        net.minecraft.world.entity.player.Player player,
                                        boolean includeEmpty,
                                        List<com.toancao.loadouttc.api.ApiSlot> out) {
                        out.add(new com.toancao.loadouttc.api.ApiSlot(
                                "demo", "charm", "Charm", "charm", 0,
                                net.minecraft.world.item.ItemStack.EMPTY));
                    }
                }).collect(null, null, null, true, adapted);
        check("api source adapts to a real slot",
                adapted.size() == 1 && "demo/charm/0".equals(adapted.get(0).key()));
    }

    private static void layers() {
        LayerOptions defaults = new LayerOptions();
        check("layer thickness default", defaults.thickness == 2);
        check("layer opacity default", defaults.opacity == 100);
        check("layer text default", "seconds".equals(defaults.textKind()));
        check("layer pulse default", defaults.pulse);
        check("layer alpha", Math.abs(defaults.alpha() - 1.0f) < 0.001f);

        LayerOptions bad = new LayerOptions();
        bad.thickness = 99;
        bad.opacity = -5;
        bad.text = "banana";
        bad.sanitize();
        check("thickness clamped", bad.thickness == 4);
        check("opacity clamped", bad.opacity == 30);
        check("text falls back", "seconds".equals(bad.textKind()));

        LayerOptions percent = new LayerOptions();
        percent.text = "percent";
        check("percent text", "62%".equals(percent.format(0.62f, "6.2s")));
        percent.text = "int";
        check("int text rounds up", "7s".equals(percent.format(0.62f, "6.2s")));
        percent.text = "seconds";
        check("seconds text kept", "6.2s".equals(percent.format(0.62f, "6.2s")));

        HudConfig cfg = new HudConfig();
        cfg.cooldownOptions = null;
        cfg.abilityOptions = null;
        cfg.durabilityOptions = null;
        cfg.sanitize();
        check("null options replaced", cfg.cooldownOptions != null && cfg.abilityOptions != null
                && cfg.durabilityOptions != null);
    }

    private static void effects() {
        HudConfig fresh = new HudConfig();
        fresh.sanitize();
        check("motion off by default", "none".equals(fresh.iconEffect));
        check("light off by default", "none".equals(fresh.iconLight));
        check("speed normal", "normal".equals(fresh.effectSpeed));
        check("strength normal", "normal".equals(fresh.effectStrength));
        check("blend default", fresh.effectBlend == 35);

        HudConfig bad = new HudConfig();
        bad.iconEffect = "glow";
        bad.iconLight = "spin";
        bad.effectSpeed = "banana";
        bad.effectStrength = null;
        bad.effectBlend = 400;
        bad.sanitize();
        check("motion stays in its group", "none".equals(bad.iconEffect));
        check("light stays in its group", "none".equals(bad.iconLight));
        check("bad speed falls back", "normal".equals(bad.effectSpeed));
        check("null strength falls back", "normal".equals(bad.effectStrength));
        check("blend clamped", bad.effectBlend == 90);
        check("sway is motion only", HudConfig.MOTION_EFFECTS.contains("sway")
                && !HudConfig.LIGHT_EFFECTS.contains("sway"));
        check("bounce is motion only", HudConfig.MOTION_EFFECTS.contains("bounce")
                && !HudConfig.LIGHT_EFFECTS.contains("bounce"));
        check("glow is light only", HudConfig.LIGHT_EFFECTS.contains("glow")
                && !HudConfig.MOTION_EFFECTS.contains("glow"));

        check("override wins over panel", "bob".equals(
                bothWithOverride().effectFor("accessories/ring/0")));
        check("panel value used elsewhere", "shake".equals(
                bothWithOverride().effectFor("accessories/ring/1")));
        check("light reads its own field", "glow".equals(
                bothWithOverride().lightFor("accessories/ring/0")));
        check("effect on counts either group", bothWithOverride().anyIconEffectFor("x")
                && bothWithOverride().anyIconEffectFor("accessories/ring/0"));

        HudConfig frame = new HudConfig();
        frame.iconLight = "glow";
        com.toancao.loadouttc.api.EffectFrame lit = com.toancao.loadouttc.hud.HudEffects.frame(
                "accessories/ring/0", net.minecraft.world.item.ItemStack.EMPTY, false, false, frame);
        check("light alone glows", lit.glowColor() != 0 && lit.glowStrength() > 0.4f);
        frame.effectStrength = "soft";
        com.toancao.loadouttc.api.EffectFrame dim = com.toancao.loadouttc.hud.HudEffects.frame(
                "accessories/ring/0", net.minecraft.world.item.ItemStack.EMPTY, false, false, frame);
        check("strength scales the glow", dim.glowStrength() < lit.glowStrength());
        frame.iconLight = "none";
        frame.iconEffect = "none";
        check("no effect draws nothing", com.toancao.loadouttc.hud.HudEffects.frame(
                "accessories/ring/0", net.minecraft.world.item.ItemStack.EMPTY, false, false,
                frame) == com.toancao.loadouttc.api.EffectFrame.NONE);

        HudConfig wash = new HudConfig();
        wash.effectBlend = 0;
        check("no wash keeps the accent", wash.blended(0xFF112233) == 0xFF112233);
        wash.effectBlend = 90;
        check("wash lightens towards white", (wash.blended(0xFF112233) & 0xFF) > 0x77);
        check("cooldown auto uses the wash",
                wash.cooldownTint(0xFF112233) == wash.blended(0xFF112233));
        check("ability auto uses the wash",
                wash.abilityTint(0xFF112233) == wash.blended(0xFF112233));
        wash.cooldownColor = "FF0000";
        check("a picked colour beats the wash", wash.cooldownTint(0xFF112233) == 0xFFFF0000);

        HudConfig kept = new HudConfig();
        kept.iconEffect = "bob";
        kept.iconLight = "glow";
        kept.effectBlend = 60;
        HudConfig copy = kept.copy();
        check("copy keeps motion", "bob".equals(copy.iconEffect));
        check("copy keeps light", "glow".equals(copy.iconLight));
        check("copy keeps blend", copy.effectBlend == 60);
    }

    private static HudConfig bothWithOverride() {
        HudConfig cfg = new HudConfig();
        cfg.iconEffect = "shake";
        cfg.iconLight = "glow";
        cfg.overrideOrCreate("accessories/ring/0").effect = "bob";
        cfg.sanitize();
        return cfg;
    }

    private static void layout() {
        HudConfig legacy = new HudConfig();
        legacy.horizontal = true;
        legacy.sanitize();
        check("legacy horizontal maps to layout", "horizontal".equals(legacy.layoutMode));
        check("layoutKind reads it", "horizontal".equals(legacy.layoutKind()));

        HudConfig fresh = new HudConfig();
        fresh.sanitize();
        check("fresh config is vertical", "vertical".equals(fresh.layoutKind()));

        HudConfig grid = new HudConfig();
        grid.layoutMode = "grid";
        grid.columns = 99;
        grid.circleRadius = 9999;
        grid.sanitize();
        check("columns clamped", grid.columns == 8);
        check("radius clamped", grid.circleRadius == 200);

        HudConfig bad = new HudConfig();
        bad.layoutMode = "diagonal";
        bad.sanitize();
        check("unknown layout falls back", "vertical".equals(bad.layoutKind()));
    }

    private static void keys() {
        // Accessories rings: group and slot share a name, so the middle segment is dropped.
        check("accessories key", slot("accessories", "ring", "ring", 0, false).key()
                .equals("accessories/ring/0"));
        // Trinkets passes the full type path for both fields, so the key is unchanged from 1.0.
        check("trinkets key", slot("trinkets", "chest/necklace", "chest/necklace", 0, false).key()
                .equals("trinkets/chest/necklace/0"));
        // A group that differs from the slot keeps both segments (vanilla armour).
        check("group prefix kept", slot("vanilla", "armor", "helmet", 0, false).key()
                .equals("vanilla/armor/helmet/0"));
        // Vanilla armour: the group is "armor", the slot is the piece.
        check("vanilla armor key", slot("vanilla", "armor", "helmet", 0, false).key()
                .equals("vanilla/armor/helmet/0"));
        // Vanilla hotbar: group and slot are both "hotbar", so the middle segment is dropped.
        check("vanilla hotbar key", slot("vanilla", "hotbar", "hotbar", 3, false).key()
                .equals("vanilla/hotbar/3"));
        check("key is stable", slot("a", "b", "b", 1, false).key()
                .equals(slot("a", "b", "b", 1, true).key()));
    }

    private static void sizes() {
        HudConfig cfg = new HudConfig();
        check("auto is vanilla size", Math.abs(cfg.effectiveScale(3) - 1.0f) < 0.001f);
        cfg.size = "xlarge";
        check("xlarge is 1.5x", Math.abs(cfg.effectiveScale(3) - 1.5f) < 0.001f);
        cfg.size = "huge";
        check("huge is 2x", Math.abs(cfg.effectiveScale(3) - 2.0f) < 0.001f);
        cfg.size = "giant";
        check("giant is 3x", Math.abs(cfg.effectiveScale(3) - 3.0f) < 0.001f);
        check("giant label", "3x".equals(cfg.sizeLabel()));
        cfg.size = "custom";
        cfg.scale = 5.0f;
        cfg.sanitize();
        check("custom scale clamps at 3x", Math.abs(cfg.effectiveScale(3) - 3.0f) < 0.001f);
        cfg.size = "banana";
        cfg.sanitize();
        check("unknown size falls back to auto", "auto".equals(cfg.size));
    }

    private static void defaults() {
        HudConfig cfg = new HudConfig();
        check("default filter all", "all".equals(cfg.slotFilter));
        check("default sort", "default".equals(cfg.sortMode));
        check("armor off by default", !cfg.vanillaArmor);
        check("offhand off by default", !cfg.vanillaOffhand);
        check("hotbar off", !cfg.vanillaHotbar);
        check("inventory off", !cfg.vanillaInventory);
        check("empty off", !cfg.emptySlots);
        check("max slots", cfg.maxSlots >= 32);
        check("nothing hidden", cfg.slotVisible("accessories/ring/0"));
        cfg.sanitize();
        check("sanitize keeps all", "all".equals(cfg.slotFilter));
    }

    private static void filterAll() {
        HudConfig cfg = new HudConfig();
        cfg.setSlotVisible("accessories/ring/0", false);
        check("hidden in all mode", !cfg.slotVisible("accessories/ring/0"));
        check("other still visible", cfg.slotVisible("accessories/ring/1"));
        cfg.setSlotVisible("accessories/ring/0", true);
        check("toggled back on", cfg.slotVisible("accessories/ring/0"));
        check("no duplicate", cfg.hiddenSlots.isEmpty());
    }

    private static void filterSelected() {
        HudConfig cfg = new HudConfig();
        cfg.showOnly(List.of("accessories/ring/0"));
        check("selected mode", "selected".equals(cfg.slotFilter));
        check("only one visible", cfg.slotVisible("accessories/ring/0"));
        check("other hidden", !cfg.slotVisible("trinkets/chest/necklace/0"));
        cfg.setSlotVisible("trinkets/chest/necklace/0", true);
        check("added to selection", cfg.slotVisible("trinkets/chest/necklace/0"));
        cfg.setSlotVisible("accessories/ring/0", false);
        check("removed from selection", !cfg.slotVisible("accessories/ring/0"));
    }

    private static void switchModes() {
        HudConfig cfg = new HudConfig();
        cfg.setSlotVisible("vanilla/boots/0", false);
        List<String> shown = List.of("vanilla/helmet/0", "accessories/ring/0");
        cfg.switchFilter(shown);
        check("switch to selected", "selected".equals(cfg.slotFilter));
        check("kept shown slot", cfg.slotVisible("accessories/ring/0"));
        check("dropped other slot", !cfg.slotVisible("trinkets/chest/necklace/0"));
        cfg.switchFilter(shown);
        check("switch back to all", "all".equals(cfg.slotFilter));
        check("all shows everything", cfg.slotVisible("trinkets/chest/necklace/0"));
        check("all cleared hidden", cfg.hiddenSlots.isEmpty());
    }

    /**
     * A config saved with two panels can come back with one panel still carrying its own scope.
     * The slot menu edits the shared settings, so that scope has to fold into them.
     */
    private static void lonePanel() {
        HudConfig cfg = new HudConfig();
        // Scope tests use a vanilla slot, so the group has to be on for it to reach the renderer.
        cfg.vanillaArmor = true;
        cfg.sanitize();
        HudCluster panel = cfg.clusters.get(0);
        panel.scope = com.toancao.loadouttc.config.SlotScope.from(cfg);
        panel.scope.setSlotVisible("vanilla/armor/helmet/0", false);

        cfg.sanitize();
        check("lone panel drops its stale scope", panel.scope == null);
        check("lone panel keeps what it drew", !cfg.slotVisible("vanilla/armor/helmet/0"));

        cfg.setSlotVisible("vanilla/armor/helmet/0", true);
        HudConfig view = panel.resolve(cfg);
        check("slot menu tick reaches the HUD", view.slotVisible("vanilla/armor/helmet/0"));
        check("slot menu tick is drawn",
                HudRenderer.prepare(sample(), view).stream()
                        .anyMatch(slot -> slot.key().equals("vanilla/armor/helmet/0")));

        // Before a save folds it away, the lone panel must still follow the shared selection.
        cfg.setSlotVisible("vanilla/armor/helmet/0", false);
        panel.scope = com.toancao.loadouttc.config.SlotScope.from(cfg);
        cfg.setSlotVisible("vanilla/armor/helmet/0", true);
        check("unsaved lone scope never wins",
                panel.resolve(cfg).slotVisible("vanilla/armor/helmet/0"));
    }

    private static void ringLabels() {
        // A ring names the two ends of the circle above/below and everything else on the outside,
        // so two names can never be drawn under the same icon or over a neighbour's plate.
        check("ring top label goes above",
                HudRenderer.ringLabelSide(0, -48, 8, 28) == HudRenderer.LABEL_ABOVE);
        check("ring bottom label goes below",
                HudRenderer.ringLabelSide(0, 48, 8, 28) == HudRenderer.LABEL_BELOW);
        check("ring right label goes right",
                HudRenderer.ringLabelSide(48, 0, 8, 28) == HudRenderer.LABEL_RIGHT);
        check("ring left label goes left",
                HudRenderer.ringLabelSide(-48, 0, 8, 28) == HudRenderer.LABEL_LEFT);
        check("right corner names itself outside",
                HudRenderer.ringLabelSide(34, -34, 8, 28) == HudRenderer.LABEL_RIGHT);
        check("left corner names itself outside",
                HudRenderer.ringLabelSide(-34, 34, 8, 28) == HudRenderer.LABEL_LEFT);
        check("lone slot label goes below",
                HudRenderer.ringLabelSide(0, 0, 1, 28) == HudRenderer.LABEL_BELOW);
    }

    private static void sorting() {
        List<HudSlot> slots = sample();

        HudConfig name = new HudConfig();
        name.sortMode = "name";
        List<HudSlot> byName = HudRenderer.sortSlots(slots, name);
        check("name sort keeps size", byName.size() == slots.size());

        HudConfig type = new HudConfig();
        type.sortMode = "type";
        List<HudSlot> byType = HudRenderer.sortSlots(slots, type);
        check("type groups armor first", byType.get(0).group().equals("armor"));
        check("armor helmet before boots",
                byType.get(0).slot().equals("helmet") && byType.get(1).slot().equals("boots"));
        check("type is stable", byType.equals(HudRenderer.sortSlots(slots, type)));

        HudConfig source = new HudConfig();
        source.sortMode = "source";
        List<HudSlot> bySource = HudRenderer.sortSlots(slots, source);
        check("vanilla first", bySource.get(0).source().equals("vanilla"));
        check("accessories second", bySource.get(2).source().equals("accessories"));
        check("trinkets last", bySource.get(bySource.size() - 1).source().equals("trinkets"));

        HudConfig custom = new HudConfig();
        custom.sortMode = "custom";
        custom.slotOrder = new ArrayList<>(List.of("trinkets/chest/necklace/0"));
        List<HudSlot> byCustom = HudRenderer.sortSlots(slots, custom);
        check("custom puts listed first", byCustom.get(0).source().equals("trinkets"));
        check("custom keeps the rest", byCustom.size() == slots.size());

        HudConfig def = new HudConfig();
        check("default keeps provider order",
                HudRenderer.sortSlots(slots, def).get(0).slot().equals("helmet"));
    }

    private static void prepare() {
        HudConfig cfg = new HudConfig();
        // This test is about filtering, so it opts the vanilla groups back in.
        cfg.vanillaArmor = true;
        cfg.vanillaOffhand = true;
        cfg.emptySlots = false;
        List<HudSlot> drawable = HudRenderer.prepare(sample(), cfg);
        check("empty dropped", drawable.size() == 4);
        cfg.emptySlots = true;
        check("empty kept", HudRenderer.prepare(sample(), cfg).size() == 5);
        cfg.maxSlots = 2;
        check("cap applied after filter", HudRenderer.prepare(sample(), cfg).size() == 2);
    }

    private static void order() {
        HudConfig cfg = new HudConfig();
        cfg.ensureOrder(List.of("a", "b", "c"));
        check("order seeded", cfg.orderIndex("b") == 1);
        check("move down", cfg.moveSlot("a", 1) && cfg.orderIndex("a") == 1);
        check("move down keeps size", cfg.slotOrder.size() == 3);
        check("move past start is a no-op", !cfg.moveSlot("b", -5));
        check("move past end is a no-op", !cfg.moveSlot("c", 5));
        check("unknown key", !cfg.moveSlot("zz", 1));
        cfg.ensureOrder(List.of("a", "d"));
        check("new key appended", cfg.orderIndex("d") == 3);

        cfg.sanitize();
        check("sanitize trims duplicates", cfg.slotOrder.size() == 4);

        HudConfig dupes = new HudConfig();
        dupes.slotOrder = new ArrayList<>(List.of("a", "a", " ", "b", "", "b"));
        dupes.hiddenSlots = new ArrayList<>(List.of("x", "x"));
        dupes.sanitize();
        check("duplicates removed", dupes.slotOrder.equals(List.of("a", "b")));
        check("hidden duplicates removed", dupes.hiddenSlots.equals(List.of("x")));
    }

    private static void panelPlacement() {
        HudConfig cfg = new HudConfig();
        cfg.sanitize();
        HudCluster main = cfg.clusters.get(0);
        String sourceAnchor = main.anchor;

        // A panel split off a right/top anchored panel gets its own anchor at offset 0: shared
        // offsets are measured inward, so reusing them would stack the new panel on the old one.
        HudCluster split = cfg.addClusterNear("Panel 2", main.id);
        check("split panel does not share the source anchor",
                !sourceAnchor.equals(split.anchor));
        check("split panel sits at its own anchor", split.x == 0 && split.y == 0);
        check("split panel owns its position", split.ownPosition);
        check("split panel is not the catch-all", !split.rest);

        HudCluster third = cfg.addClusterNear("Panel 3", split.id);
        check("third panel finds a free anchor",
                !third.anchor.equals(main.anchor) && !third.anchor.equals(split.anchor));

        // Every visible slot belongs to exactly one panel: the rest panel keeps what nobody claims.
        String mine = "accessories/ring/0";
        cfg.assign(mine, split.id);
        check("claimed slot leaves the rest panel", split.id.equals(cfg.clusterIdFor(mine)));
        check("unclaimed slot stays in the rest panel",
                "main".equals(cfg.clusterIdFor("vanilla/armor/helmet/0")));

        // What the renderer hands each panel: the split panel must get its slot, the rest panel
        // only the others. This is the check that fails when both panels show the same thing.
        HudConfig render = new HudConfig();
        render.sanitize();
        HudCluster target = render.addClusterNear("Panel 2", render.clusters.get(0).id);
        render.assign(mine, target.id);
        check("split seeds per-panel slot scopes",
                render.clusters.stream().allMatch(cluster -> cluster.scope != null));
        try {
            var method = HudRenderer.class.getDeclaredMethod("slotsFor", List.class,
                    HudConfig.class, HudCluster.class, HudConfig.class);
            method.setAccessible(true);
            for (HudCluster cluster : render.clusters) {
                @SuppressWarnings("unchecked")
                List<HudSlot> drawn = (List<HudSlot>) method.invoke(null, sample(), render, cluster,
                        cluster.resolve(render));
                if (cluster == target) {
                    check("panel 2 draws only its own slot", drawn.size() == 1
                            && mine.equals(drawn.get(0).key()));
                } else {
                    check("rest panel drops the claimed slot",
                            drawn.stream().noneMatch(slot -> mine.equals(slot.key())));
                }
            }
            // Hiding a slot in one panel must not leak it into the other one either.
            target.scope.setSlotVisible(mine, false);
            @SuppressWarnings("unchecked")
            List<HudSlot> hidden = (List<HudSlot>) method.invoke(null, sample(), render, target,
                    target.resolve(render));
            check("slot hidden in one panel stays out of it", hidden.isEmpty());
        } catch (ReflectiveOperationException exception) {
            check("slotsFor readable", false);
        }
    }

    private static void check(String name, boolean ok) {
        if (!ok) {
            failures++;
            System.out.println("FAIL: " + name);
        } else {
            System.out.println("ok: " + name);
        }
    }
}
