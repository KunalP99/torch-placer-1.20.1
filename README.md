# Torch It Up

A Fabric mod for Minecraft 1.20.1 that automatically places torches in dark areas around you as you explore. Supports vanilla torches, soul torches, redstone torches, 11 wood-variant torches, and an underwater torch. Also lights up the area around you while you hold a torch in your hand.

## Requirements

- Minecraft 1.20.1
- [Fabric Loader](https://fabricmc.net/use/installer/) 0.18.6+
- [Fabric API](https://modrinth.com/mod/fabric-api) 0.92.7+1.20.1

---

## Features at a Glance

| Feature | Summary |
|---------|---------|
| Auto-placement | Places torches in spots below your light threshold every 2 seconds |
| Dynamic lighting | Holding any torch illuminates the area around you |
| Torch Bag | Dedicated storage bag that holds up to 27 stacks of torches |
| Wood-variant torches | 11 custom torch types, one per wood type |
| Underwater torch | A torch that survives and can be placed in water |
| HUD counter | Shows your remaining torch count when the auto-placer is active |
| Configurable | Light threshold, placement surface, and torch source |

---

## Auto-Placement

When enabled, the mod scans blocks within **4 blocks** of you every 2 seconds. If it finds a spot darker than your configured light threshold it takes a torch from your inventory or torch bag and places it automatically.

### What gets placed where

Candidates are filtered by two rules before anything is placed:

- **Line of sight** — the candidate block must be directly visible from your eye position. Torches are never placed behind walls or around corners you cannot see.
- **Sturdy surface** — wall torches require a solid face to attach to; floor torches require a solid block directly below.

### Placement priority

When multiple valid spots exist, the mod picks the best one in this order:

1. **Side walls** (left/right of where you're facing) — preferred first
2. **Front/back walls**
3. **Floor**

Within the same surface type, the **nearest** valid spot is always chosen first. Light level is only used as a tiebreaker between spots that are the same distance away.

---

## Dynamic Lighting

While you hold a torch in your main hand or offhand, the mod places an invisible light source at your feet. The brightness matches the vanilla light level of the torch you're holding:

| Torch | Light level |
|-------|-------------|
| Regular torch / wood-variant torches | 14 |
| Soul torch | 10 |
| Redstone torch | 7 |
| Underwater torch (submerged only) | 14 |

The light updates as you move and disappears the moment you put the torch away. Switching between torch types (e.g. regular → soul torch) instantly adjusts the brightness.

Regular torches do not provide dynamic lighting when you are submerged. The underwater torch only provides dynamic lighting when you are submerged.

---

## Torch Bag

The **Torch Bag** is a dedicated storage item that holds up to 27 stacks of torches of any type. Right-click the bag to open its inventory.

- The auto-placer checks the bag **before** your regular inventory (when torch source is set to *Bag, then Inventory* or *Bag Only*).
- Torches inside the bag are consumed during auto-placement without needing to move them to your hotbar.
- The bag stores its contents in NBT — torches are safe even if you close the game mid-session.

---

## Wood-Variant Torches

The mod adds 11 wood-typed torches, one for each wood type in the game. They behave identically to a vanilla torch for placement purposes.

| Torch | Crafting ingredients |
|-------|---------------------|
| Spruce Torch | Spruce Planks + Torch |
| Birch Torch | Birch Planks + Torch |
| Jungle Torch | Jungle Planks + Torch |
| Acacia Torch | Acacia Planks + Torch |
| Dark Oak Torch | Dark Oak Planks + Torch |
| Mangrove Torch | Mangrove Planks + Torch |
| Cherry Torch | Cherry Planks + Torch |
| Bamboo Torch | Bamboo Planks + Torch |
| Crimson Torch | Crimson Planks + Torch |
| Warped Torch | Warped Planks + Torch |

### Crafting

Craft in any crafting grid (including the 2×2 inventory grid):

```
[ Plank ]
[ Torch ]
```

Place the matching wood plank directly above a vanilla torch. Yields 1 wood torch.

---

## Underwater Torch

The **Underwater Torch** is a special torch that can be placed in and survives contact with water. Normal torches pop off when water flows over them — the underwater torch stays put.

- Shows bubble particles when submerged instead of the usual flame
- **Can only be placed in water** — attempting to place it in air or on dry land does nothing
- Fully supported by the auto-placer (placed in water blocks only)
- Works inside the Torch Bag
- Provides dynamic hand-held lighting **only when the player is submerged**

### Crafting

```
[ Any Torch ]
[ Dried Kelp ]
```

Shapeless — place any torch (vanilla or any wood-variant) and a dried kelp anywhere in the crafting grid. Yields 1 underwater torch.

---

## HUD Torch Counter

When the auto-placer is enabled, a small torch icon and count appear in the **bottom-right corner** of the screen showing how many torches you have left. The colour shifts as your supply runs low:

| Count | Colour |
|-------|--------|
| 16 or more | White |
| 6 – 15 | Yellow |
| 0 – 5 | Red |

The counter respects your **Torch Source** setting — it only counts torches from wherever the auto-placer actually draws them. It can be toggled on or off in the **Torch It Up Settings** screen.

---

## Keybindings

| Key | Action | Default |
|-----|--------|---------|
| `G` | Toggle auto-torch on/off | G |
| *(unbound)* | Open settings screen | — |

Rebind either key in **Options → Controls → Torch It Up**.

When you toggle the mod, a message appears above your hotbar confirming the new state (**Auto Torch: ON** / **Auto Torch: OFF**).

---

## Settings Screen

Open the **Torch It Up Settings** screen with your bound key. Changes take effect after clicking **Save**.

### Light Threshold

Controls how dark a spot must be before a torch is placed there. Block light level must be **at or below** this value.

| Threshold | Effect |
|-----------|--------|
| 0 | Only pitch-black spots — very few placements |
| 7 (default) | Comfortable safety buffer — well-lit caves |
| 14 | Almost everywhere — very aggressive placement |

> In Minecraft 1.20.1 hostile mobs spawn at block light level **0**. A threshold of 7 keeps your surroundings lit with a generous safety margin.

### Placement Mode

Controls which surfaces torches can be placed on.

| Mode | Behaviour |
|------|-----------|
| **Walls & Floor** (default) | Prefers side walls, then front/back walls, then the floor |
| **Walls Only** | Wall torches only — useful in structured builds |
| **Floor Only** | Ground torches only — good for wide open flat areas |

### Torch Source

Controls where the auto-placer takes torches from.

| Source | Behaviour |
|--------|-----------|
| **Bag, then Inventory** (default) | Checks the torch bag first; falls back to your inventory if the bag is empty |
| **Bag Only** | Only uses torches stored in the torch bag |
| **Inventory Only** | Only uses torches in your regular inventory; ignores the bag |

---

## Commands

| Command | Description |
|---------|-------------|
| `/torchesplaced` | Shows how many torches you have placed in this world (manually and by the auto-placer) |
| `/torchlight` | Shows the block light level at your position and whether the auto-placer would place a torch there |

The count persists across death, logout, and server restarts. It is saved per-world and per-player.

---

## Tips

- Keep torches stocked — the mod consumes them just like placing manually.
- The **Torch Bag** frees up inventory space and is checked before your inventory by default — load it up before a long caving session.
- A **light threshold of 0** combined with **Floor Only** is useful for fully automated mine lighting with minimal torch use.
- The **Underwater Torch** is the only torch the auto-placer will place in water blocks — bring some when exploring ocean monuments or underwater caves.
- Soul torches and redstone torches work for dynamic hand-held lighting but are not used for auto-placement — only vanilla, wood-variant, and underwater torches are placed automatically.
- The config is saved to `config/torch-placer.json` and persists between sessions.

---

## License

All Rights Reserved. You are free to include this mod in your modpack. You may not redistribute, reupload, or modify the mod without permission.
