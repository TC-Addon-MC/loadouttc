# Loadout TC — API

> Rendered reference with every signature: [api.html](api.html).

`LoadoutTcApi` is a small, optional client-side API. Mods, resource packs or script mods can use
it to change the HUD look or animate a specific slot. No call is required for the HUD to work.

```java
import com.toancao.loadouttc.api.LoadoutTcApi;
import com.toancao.loadouttc.api.EffectContext;
import com.toancao.loadouttc.api.EffectFrame;
import com.toancao.loadouttc.api.ApiSlot;
import com.toancao.loadouttc.api.SlotSource;
```

Add the Loadout TC jar as a `compileOnly` dependency. Everything runs on the client; do not call
these methods from a dedicated server.

## Version check

```java
if (LoadoutTcApi.apiVersion() >= 3) {
    // method introduced in API 3
}
```

`apiVersion()` only goes up when a method changes meaning or disappears. `version()` returns the
Fabric version string (for example `1.0.0+1.21.1`). Both are cheap and safe to call from an
entrypoint.

API 4 added `SlotSource` / `ApiSlot`, the extra `SlotView` state fields and `sourceViews()`.

## Panels (clusters)

A panel is one independent HUD: its own slots, its own position and optionally its own look. A
config always has at least one, and the default panel catches every slot no other panel claims, so
a single-panel setup is the same as before this feature existed.

```java
String panel = LoadoutTcApi.splitToCluster("Rings", List.of("accessories/ring/0"));
LoadoutTcApi.assignSlotToCluster("accessories/ring/1", panel);
int[] rect = LoadoutTcApi.clusterBounds(panel);   // x, y, w, h on screen
int[] all  = LoadoutTcApi.hudBounds();            // union of every panel
```

## Slot keys

Slots are addressed by a stable key:

```
source/slot/index            when the group and the slot share a name
source/group/slot/index      otherwise
```

```
vanilla/armor/helmet/0
vanilla/offhand/0
vanilla/hotbar/3
vanilla/inventory/14
accessories/ring/0
trinkets/chest/necklace/0
curios/ring/1
```

Get the current list at runtime with:

```java
List<String> keys = LoadoutTcApi.slotKeys();
```

The list is in display order and only contains slots the HUD is actually drawing (filtered and
sorted). Use `slots()` for every known slot, including hidden and empty ones.

## Slot menu API

```java
List<LoadoutTcApi.SlotView> all = LoadoutTcApi.slots();       // every known slot
List<LoadoutTcApi.SlotView> shown = LoadoutTcApi.visibleSlots();
List<String> sources = LoadoutTcApi.sources();                // vanilla, accessories, ...
List<LoadoutTcApi.SourceView> tabs = LoadoutTcApi.sourceViews(); // id + display label
```

`SourceView` is a record of `id` and `label`, in the order the slot menu shows its tabs.

`SlotView` carries `key`, `source`, `group`, `label`, `index`, `itemId`, `empty` and `visible`,
plus the live state: `stack`, `count`, `damage`, `maxDamage`, `cooldownRemaining` (0 = ready,
1 = just started), `abilityRemaining` (0..1) and `abilitySeconds` (the timer started with
`showAbility`, 0 when idle). Both `damageable()` and `onCooldown()` are convenience helpers.
`visible` and `visibleSlots()` follow the panel that owns each slot, including that panel's own
sort order and slot cap, so they match what is drawn.

Once a config has more than one panel, each panel keeps its own slot selection. The setters below
apply to every panel; `isSlotVisible`, `slotFilter` and `sortMode` return the shared defaults that
new panels start from. Ticking a slot in the menu also claims it for the panel being edited.

| Call | Effect |
|------|--------|
| `setEmptySlots(boolean)` | Draw empty slots as ghost frames |
| `setSlotVisible(key, boolean)` | Tick / untick one slot (uses the active filter mode) |
| `showOnlySlots(Collection<String>)` | Show only these keys (`selected` mode) |
| `showAllSlots()` | Reset to `all` mode |
| `setSlotFilter("all" \| "selected")` | Switch visibility model |
| `setSortMode("default" \| "name" \| "type" \| "source" \| "custom")` | Change ordering |
| `setSlotOrder(List<String>)` | Replace the manual order and switch to `custom` |
| `moveSlot(key, -1 \| 1)` | Move one slot in the manual order |
| `setSourceEnabled("trinkets", false)` | Hide a whole source without uninstalling it |
| `isSourceEnabled("trinkets")` / `hasSource("curios")` | Whether a source is switched on / installed |
| `setVanillaGroup("armor" \| "offhand" \| "hotbar" \| "inventory", boolean)` | Toggle vanilla groups |
| `slotFilter()` / `sortMode()` / `layoutMode()` / `featuredSlot()` | Read the current value back |
| `openSlots()` | Open the slot menu from another mod |
| `clusters()` / `clusterOf(key)` | Panel ids, and the panel that draws a slot |
| `createCluster(name)` / `splitToCluster(name, keys)` | Add an empty panel, or move slots into a new one |
| `assignSlotToCluster(key, id)` | Move one slot; a blank id leaves it to the catch-all panel |
| `removeCluster(id)` | Delete a panel and hide the slots it was drawing; the last panel cannot be removed |
| `mergeClusters(source, dest, takeSourceLook)` | Fold one panel into another; optionally adopt the source look |
| `renameCluster(id, name)` / `setClusterEnabled(id, on)` | Label or hide a panel |
| `clusterSlots(id)` | Slots explicitly owned by a panel |
| `clusterBounds(id)` / `hudBounds()` / `slotBounds(key)` | Screen rectangles, so another overlay can avoid the HUD |
| `setCooldownThickness(1-4)` / `setAbilityThickness(1-4)` / `setDurabilityThickness(1-4)` | Bar / ring stroke width |
| `setCooldownOpacity(30-100)` / `setAbilityOpacity(...)` / `setDurabilityOpacity(...)` | Fade that layer |
| `setCooldownText("seconds" \| "int" \| "percent")` / `setAbilityText(...)` | Countdown format |
| `setCooldownPulse(boolean)` / `setAbilityPulse(...)` | Blink when nearly done |
| `setCooldownReverse(boolean)` / `setAbilityReverse(...)` | Draw from the opposite end |
| `setCooldownTrack(boolean)` / `setAbilityTrack(...)` / `setDurabilityTrack(...)` | Background track |
| `triggerFeaturedEffect()` / `triggerFeaturedEffect(ms)` | Play the pinned effect once, default length or given |
| `cooldownStyles()` | Every id `setCooldownStyle` accepts |
| `clearAbilities()` | Stop every ability timer at once |
| `setLayoutMode("vertical" \| "horizontal" \| "grid" \| "circle")` | Slot arrangement |
| `setColumns(1-8)` | Grid columns |
| `setCircleRadius(16-200)` | Ring radius in GUI pixels |

## Register a slot source

Another mod can contribute slots; its tab appears in the slot menu automatically and its keys use
`<your-id>/<slot>/<index>`. Use `SlotSource` + `ApiSlot` from `com.toancao.loadouttc.api` so the
integration never imports internal classes:

```java
import com.toancao.loadouttc.api.ApiSlot;
import com.toancao.loadouttc.api.SlotSource;

public final class MySource implements SlotSource {
    public String id() { return "mymod"; }
    public String label() { return "My Mod"; }
    public boolean available() { return true; }

    public void collect(Minecraft minecraft, Player player, boolean includeEmpty,
                        List<ApiSlot> out) {
        ItemStack stack = myEquippedStack(player);
        if (!stack.isEmpty() || includeEmpty) {
            out.add(new ApiSlot(id(), "charm", "Charm", "charm", 0, stack));
        }
    }
}

LoadoutTcApi.registerSource(new MySource());
```

`unregisterSource("mymod")` removes it again. Both calls are client-side and safe at any time.

`SlotSource` contract:

| Member | Contract |
|--------|----------|
| `id()` | Unique, lowercase; becomes the key prefix (`mymod/...`) and the tab id |
| `label()` | Tab caption; defaults to `id()` |
| `available()` | Return `false` while the other mod is absent; the tab is then hidden and `collect` is never called |
| `collect(...)` | Fill `out` in display order; emit empty stacks only when `includeEmpty` is true |

`ApiSlot` fields:

| Field | Meaning |
|-------|---------|
| `source` | Must equal `id()` |
| `group` / `groupLabel` | Sub-group inside the tab (for example `ring`), and the heading printed for it |
| `slot` | Slot type; repeat the group name when they are the same |
| `index` | Position inside that slot type, starting at 0 |
| `stack` | `ItemStack.EMPTY` for an empty slot |

`key()`, `empty()` and `itemId()` are derived for you; `key()` matches the format described in
[Slot keys](#slot-keys).

### Self-registering without any Loadout TC change

A slot mod does not need Loadout TC to know about it. Declare the `loadouttc:slot_source`
entrypoint in its own `fabric.mod.json` and the HUD picks it up at launch:

```json
{
  "entrypoints": {
    "loadouttc:slot_source": ["com.example.mymod.MySlots"]
  }
}
```

`MySlots` is the same `SlotSource` implementation as above (no-arg constructor). One entrypoint
class can be listed per source; a mod with several inventories lists several classes. Registering
the same id at runtime replaces the entrypoint version, so a mod can also call
`LoadoutTcApi.registerSource(...)` later (for example after its own config loads).

Failures are isolated: a throwing `available()` or `collect()` disables only that source and is
logged, the rest of the HUD keeps working.

> The older `AccessorySource`/`HudSlot` overload still works, but those types live outside the API
> package and may change; new integrations should use `SlotSource`/`ApiSlot`.

## Effects

### Trigger a built-in effect

```java
LoadoutTcApi.playEffect("accessories/ring/0", "spin", 3000);          // 3 seconds
LoadoutTcApi.playEffect("accessories/ring/0", "pulse", 0, 0xFFFF6B6B); // permanent, red glow
LoadoutTcApi.clearEffect("accessories/ring/0");
LoadoutTcApi.clearAllEffects();
```

`durationMillis <= 0` means "until cleared". `color` is ARGB; pass `0` to use the HUD accent.
Returns `false` when the slot key is empty or the effect id is unknown.

Built-ins:

| id | Effect |
|----|--------|
| `spin` | 2D wheel rotation, 360°/s (best for flat icons) |
| `spin_3d` | 3D coin rotation, 300°/s |
| `spin_fast` | 2D rotation, 720°/s |
| `spin_3d_fast` | 3D rotation, 720°/s |
| `bob` | Gentle vertical float |
| `pulse` | Scale + accent glow pulse |
| `shake` | Small jitter |
| `flash` | Glow blink |
| `rainbow` | Hue-cycling glow |
| `hover` | Small lift + glow (used automatically on hover) |
| `none` | No-op |

### Pin a slot

```java
LoadoutTcApi.setFeaturedSlot("accessories/ring/0"); // legacy single featured slot
LoadoutTcApi.setFeaturedEffect("pulse");
LoadoutTcApi.setFeaturedSlot(""); // clear
```

### Per-slot overrides

Each slot can keep its own effect and bar styles independently:

```java
LoadoutTcApi.setSlotEffect("accessories/ring/0", "spin_3d");
LoadoutTcApi.setSlotCooldownStyle("accessories/ring/0", "arc");
LoadoutTcApi.setSlotDurabilityStyle("accessories/ring/0", "ring");

LoadoutTcApi.setSlotEffect("accessories/necklace/0", "pulse");
LoadoutTcApi.clearSlotOverrides("accessories/ring/0");
```

`setSlotEffect(..., "none")` disables the effect for that slot. Empty fields fall back to the
global defaults; `off` / `none` explicitly disable. The editor's right-click selection edits the
same map.

The featured slot is the one the player right-clicks in the editor. It persists in
`config/loadouttc.json` as `featuredSlot` / `featuredEffect`.

### Register a custom effect

```java
LoadoutTcApi.registerEffect("sparkle", context -> {
    float wave = 0.5f + 0.5f * (float) Math.sin(context.seconds() * 9.0f);
    return new EffectFrame(
            0.0f,                       // offsetX
            0.0f,                       // offsetY
            1.0f + wave * 0.06f,        // scale
            0.0f,                       // spinDegrees
            false,                      // threeD
            0xFFFFD166,                 // glowColor (ARGB)
            wave * 0.6f                 // glowStrength 0..1
    );
});
```

`context` contains:

| field | Meaning |
|-------|---------|
| `slotKey()` | Key of the slot being drawn |
| `stack()` | Its `ItemStack` |
| `seconds()` | Seconds since the effect started (or since launch for featured/hover) |
| `progress()` | 0..1 for timed effects, 1 for permanent effects |
| `featured()` | Whether this slot is the pinned/featured slot |
| `hovered()` | Whether the mouse is over it in the editor |
| `accentColor()` | Active accent (or the color passed to `playEffect`) |

Unregister with `LoadoutTcApi.unregisterEffect("sparkle")`; list everything with
`LoadoutTcApi.effects()`.

### EffectFrame

`EffectFrame` is the transform the renderer applies to the icon for one frame:

| Field | Range | Meaning |
|-------|-------|---------|
| `offsetX` / `offsetY` | GUI pixels | Translate the icon |
| `scale` | > 0 | Uniform scale; values `<= 0` become 1 |
| `spinDegrees` | degrees | Rotation, 3D around the vertical axis when `threeD` is true |
| `threeD` | boolean | Picks 3D coin spin over flat 2D spin |
| `glowColor` | ARGB | `0` = no glow |
| `glowStrength` | 0..1 | Clamped |

Shortcuts: `EffectFrame.NONE`, `EffectFrame.spin(degrees)`, `EffectFrame.offset(x, y)`,
`EffectFrame.scale(s)` and `EffectFrame.glow(color, strength)`.

Effects are rendered on the client render thread. Keep them cheap: they run every frame.

## Look & feel

```java
LoadoutTcApi.setStyle("frost");                 // any of LoadoutTcApi.styles()
LoadoutTcApi.setSize("auto");                  // auto | micro | tiny | small | medium | large | xlarge | huge | giant | custom
LoadoutTcApi.setCustomScale(0.9f);
LoadoutTcApi.setDurabilityStyle("ring");       // bar | segments | pips | text | ring | battery | off
LoadoutTcApi.setCooldownStyle("arc");          // arc | text | bar | frame | off
LoadoutTcApi.triggerFeaturedEffect(2000);      // play the pinned effect once
LoadoutTcApi.setAccent(0xFF7C9EFF);
LoadoutTcApi.setEnabled(true);
LoadoutTcApi.openEditor();
LoadoutTcApi.reload();
```

`setSize("auto")` follows Minecraft's GUI scale; presets go up to `giant` (3.0x) and
`setCustomScale` accepts `0.5 - 3.0`. Every setter saves `config/loadouttc.json` immediately.

| Call | Note |
|------|------|
| `setEnabled(false)` | Hides the HUD but keeps all settings |
| `openEditor()` | Only opens when no other screen is showing |
| `reload()` | Re-reads `loadouttc.json`, for example after another mod edited it |

## Style ids

`LoadoutTcApi.styles()` returns every id (31). Ten of them are shown in the editor gallery; the
rest still load from an existing config:

```
vanilla, steel, hex, liquid_glass, bare, fantasy_rpg, carbon, parchment, arcane, outline,
notion, glassmorphism, neumorphism, bento, dynamic_island, dock, material_expressive, terminal,
pixel_vanilla, minimal, chips, sticker, clay, neon, brutalist, editorial, frost, arcade,
grouped, compact, scifi_tactical
```

The first line is the gallery set, in menu order. `bare`, `minimal`, `chips`, `sticker` and
`outline` draw no outer panel.

## Threading / safety

- Call from the Minecraft client thread (tick, screen, or render callbacks).
- The internal effect maps are concurrent, but `ConfigStore` writes are intended for the client.
- If Loadout TC is not installed, other mods must guard calls themselves; the API is not a
  separate library jar.

## Ability timers (mod-only)

Vanilla only exposes item cooldowns. For skills, channels or buffs, push a timer yourself:

```java
// 4.5 s cast on the ring slot; colour 0 uses the configured accent
LoadoutTcApi.showAbility("accessories/ring/0", 4500L);
LoadoutTcApi.showAbility("accessories/necklace/0", 4500L, 0xFF7C9EFF);
LoadoutTcApi.hideAbility("accessories/ring/0");
LoadoutTcApi.clearAbilities();              // stop every timer
LoadoutTcApi.setAbilityStyle("frame"); // off | ring | bar | text | frame
```

Display styles:

| Style | Look |
|-------|------|
| `ring` | arc just outside the icon |
| `bar` | 2px strip on the slot's top edge |
| `text` | remaining seconds over the icon |
| `frame` | progress traced along the slot frame (follows hex/rounded/square) |
| `off` | hidden |

`frame` is the "combined" style: it reuses the slot's own shape, so it matches whatever slot
style is active. The same option exists for item cooldowns (`Cooldown: Frame`).