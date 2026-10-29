# Loadout TC

A client-side equipment HUD for Fabric 1.21.1. It reads **Vanilla**, **Accessories**, **Trinkets**
and **Curios** through one model, draws them with a styleable renderer, and exposes a small API so
other mods can add slot sources, effects and ability timers.

> Status: 1.0.0 · Fabric 1.21.1 · client side, nothing to install on the server.
>
> [Overview page](https://tc-addon-mc.github.io/loadouttc/) · [API reference](https://tc-addon-mc.github.io/loadouttc/api.html)

---

## Features

- One HUD for every equipment mod; a source that is missing is simply skipped.
- Slot menu with one tab per source, per-slot ticks, five sort orders and manual `▲` `▼`.
- Draggable panels: several independent HUDs on screen, each with its own slots and look.
- Durability, cooldown and ability layers, each with its own style, colour, thickness and text.
- In-game editor with live preview; everything is saved to `config/loadouttc.json`.
- API for other mods, including self-registering slot sources via a Fabric entrypoint.

## Controls

| Input | Action |
|-------|--------|
| **H** | Open / close the editor |
| **Tab** / **Preview** | Hide the editor and watch the HUD over the live world |
| Drag the panel | Move it; the anchor becomes `CUSTOM` |
| Scroll | Size `0.5 - 3.0x` (switches to Custom) |
| Shift + scroll | Opacity `20 - 100%` |
| Ctrl + scroll | Icon size `12 - 40 px` |
| Right-click a slot | Select it for per-slot FX / cooldown / durability |
| **Design** / **Slots** / **Pos** / **Size** | Design screen, slot menu, anchor cycle, size cycle |
| **Delete** / **Reset** | Clear the selected slot's overrides / reset everything |
| **Done** / **Esc** | Save and close |

## Sizes

`Auto` keeps a slot at vanilla hotbar size for the current GUI scale. Presets go from `S` (0.8x) to
`3x`, and `Custom` covers `0.5 - 3.0x`, so a HUD can be much larger than the vanilla hotbar:

`Auto` · `S 0.8x` · `M 1.0x` · `L 1.25x` · `XL 1.5x` · `2x` · `3x` · `Custom`

If the HUD would run off screen, the renderer scales it down just enough to keep it visible.

## Look

The look is four independent axes, not a list of skins. Ten styles ship in the gallery; older style
ids still load for existing configs.

| Axis | Values |
|------|--------|
| **Panel** | Auto, None, Vanilla, Dark, Glass, Wood |
| **Slot** | None, Vanilla, Steel, Hex, Cut, Inset, Rounded, Circle, Triangle, Ellipse |
| **Text** | Normal, Upper, Bright, Gold, Outline |
| **Layout** | Vertical, Horizontal, Grid (`Columns` 1-8), Circle (`Ring radius` 16-200) |

Spacing, radius, borders and per-axis nudges live in `Design → Spacing`. Defaults are a bare,
background-free HUD with vanilla slots and normal labels.

## Slots and panels

Open the slot menu with **Slots** in the editor toolbar (or `Design → Slots → Open slot menu`).
Tabs appear only for installed sources and show `shown/total`; each vanilla group has its own
heading and on/off switch. `All / Checked`, `Empty`, `Source`, `Sort` and `Check all / Clear` sit in
one control row; `To / Move checked` sends the ticked slots of the tab to another panel.

A panel is an independent HUD. `Panel: <name>` picks one, **Split panel** creates one from the
current slots, **Merge panels** absorbs one into another without losing slots. A slot no panel
claims falls into the *rest* panel, so a fresh install behaves like a single HUD.

## Feedback layers

All three layers are drawn inside the slot, never over the item model:

| Layer | Styles |
|-------|--------|
| **Durability** | `Bar`, `Seg`, `Pip`, `Txt`, `Val`, `Ring`, `Bat`, `Off` |
| **Cooldown** (vanilla) | `Arc`, `Txt`, `Bar`, `Frame`, `Off` |
| **Ability** (pushed by mods) | `Ring`, `Bar`, `Text`, `Frame`, `Off` |

Each layer has its own colour row (`Auto` follows health, the accent, or a lightened accent),
thickness `1-4 px`, opacity, track, pulse, reverse and text mode (`seconds` / `int` / `percent`).
Full durability bars sit at 45% opacity; below 20% they turn red and pulse.

## API

See the [API reference](docs/api.html) for every method, or the [Markdown version](docs/API.md).
A minimal example:

```java
LoadoutTcApi.registerEffect("sparkle", ctx ->
        EffectFrame.glow(0xFFFFD166, 0.5f + 0.5f * (float) Math.sin(ctx.seconds() * 9)));

String ring = LoadoutTcApi.slotKeys().stream()
        .filter(key -> key.startsWith("accessories/ring/"))
        .findFirst().orElse("");

LoadoutTcApi.playEffect(ring, "sparkle", 4000);
LoadoutTcApi.setSize("2x");
```

Everything runs on the client; no call is required for the HUD to work.

## Install

1. Install **Fabric Loader** and **Fabric API** for Minecraft **1.21.1**.
2. Install at least one of [Accessories](https://modrinth.com/mod/accessories),
   [Trinkets](https://modrinth.com/mod/trinkets) or [Curios](https://modrinth.com/mod/curios).
3. Drop `loadouttc-1.0.0+1.21.1.jar` into `.minecraft/mods`.
4. Launch the game and press **H**.

Curios 9.5.1 for 1.21.1 is NeoForge-only, so the mod talks to it reflectively and only activates
when the API is visible (for example through Sinytra Connector). Without it, the other providers
work normally.

> Close Minecraft before replacing the jar. Fabric keeps it open and lazy-loads classes, so
> overwriting a running jar produces `ZipException: invalid LOC header`.

## Configuration

`config/loadouttc.json` is generated on first launch. A trimmed example:

```json
{
  "style": "vanilla",
  "anchor": "TOP_RIGHT",
  "size": "auto",
  "scale": 1.0,
  "opacity": 1.0,
  "iconSize": 18,
  "panelStyle": "none",
  "slotStyle": "vanilla",
  "textStyle": "normal",
  "labels": true,
  "emptySlots": false,
  "layoutMode": "vertical",
  "sortMode": "default",
  "durabilityStyle": "bar",
  "cooldownStyle": "frame",
  "abilityStyle": "ring",
  "maxSlots": 64,
  "accent": "7C9EFF",
  "vanillaArmor": false,
  "vanillaOffhand": false,
  "vanillaHotbar": false,
  "vanillaInventory": false
}
```

- `size`: `auto`, `small`, `medium`, `large`, `xlarge`, `huge`, `giant`, `custom`.
- `anchor`: `TOP_LEFT`, `TOP_RIGHT`, `BOTTOM_LEFT`, `BOTTOM_RIGHT`, `ABOVE_HOTBAR`, `CENTER`,
  `CUSTOM` (`x`/`y` are absolute only for `CUSTOM`).
- `layoutMode`: `vertical`, `horizontal`, `grid`, `circle`. An empty value follows `horizontal`.
- `slotFilter`: `all` (everything except `hiddenSlots`) or `selected` (only `visibleSlots`).
- `slotOverrides` is keyed by slot key; blank fields inherit, `off` disables that layer per slot.

Settings are written 400 ms after the last change, and immediately on close or shutdown.

## Build

Java 21, then `./gradlew build`. The jar lands in `build/libs/`; `./gradlew runClient` launches a
dev client with the supported mods on the classpath.

## Known limitations

- Curios needs a Connector-style runtime on 1.21.1; pure Fabric uses Accessories and Trinkets.
- Hover tooltips are editor-only: in normal gameplay the cursor is grabbed.
- Glass looks are layered translucency, not a framebuffer blur.

## Community

- [Ko-fi](https://ko-fi.com/toancao) — support development
- [YouTube](https://www.youtube.com/@toancaomc) — demos and updates
- [Discord](https://discord.gg/Dd349SKRvQ) — questions and bug reports
- [CurseForge](https://www.curseforge.com/members/toancao/projects) — all projects

## License

MIT. See `LICENSE`.
