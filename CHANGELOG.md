# Torch It Up — Changelog

## 2026-04-18

### Commands
- Added `/torchesplaced` command — shows how many torches you have placed in this world (both manually and by the auto-placer). The count persists across death, logout, and server restarts
- `/torchesplaced` message updated to read "You have placed X torches in this world." to reflect both placement types
- Added `/torchlight` command — shows the block light level at your position and whether the auto-placer would place a torch there

### Config Screen
- Added a **HUD Counter** toggle to the settings screen — turn the torch counter on or off without disabling the auto-placer

### Underwater Torch
- The underwater torch can now be crafted using any wood-variant torch in addition to the vanilla torch

---

## 2026-04-17

### Config Screen
- Renamed the config screen title from "Torch Placer Settings" to "Torch It Up Settings"

### HUD Torch Counter
- Added a torch counter to the bottom-right of the screen, visible when the auto-placer is enabled
- Counter colour shifts white → yellow → red as torches run low (below 16 and below 6)
- Respects the Torch Source setting — counts only from wherever the auto-placer draws torches
- Fixed the counter clipping off the right edge of the screen

### Bug Fixes
- Fixed the auto-placer not placing underwater torches in water blocks (water positions were incorrectly excluded as candidates)

---

## 2026-04-16

### Underwater Torch
- Added the Underwater Torch — a new torch type that survives and can be placed in water
- Shows bubble particles when submerged
- Crafted from a vanilla torch + dried kelp (shapeless)
- Fully integrated with the auto-placer and Torch Bag

---

## 2026-04-14

### General
- Renamed the mod display name to **Torch It Up**
- Removed the Oak Torch variant (it was never implemented)
- Fixed wood torch item names not displaying correctly

### Torch Bag
- Crafting recipe now requires a vanilla torch in the centre slot

---

## 2026-04-13

### Dynamic Lighting
- Holding any torch now illuminates the surrounding area in real time
- Soul torch dynamic lighting uses the correct light level (10)
- Redstone torch dynamic lighting uses the correct light level (7)
- Fixed multiple flickering issues when moving or sprinting while holding a torch

### Torch Bag
- Expanded the Torch Bag to 27 slots (3×9 grid)

### Auto-Placer
- Added **Torch Source** setting: choose whether the auto-placer draws from your inventory, your Torch Bag, or both
- Fixed torch placement to use a close-range line-of-sight check, removing the separate radius setting
- Fixed sky light being ignored in bright outdoor areas

---

## 2026-04-12 – 2026-04-11

### Wood Torch Variants
- Added textures for all remaining wood torch variants
- Removed Oak Torch variant (unimplemented)
- Registered cutout render layer for all custom torch blocks so transparent pixels render correctly
- Fixed transparency issues on Acacia, Birch, and Jungle torch textures

### Torch Bag
- Switched Torch Bag GUI to use the shulker box texture
- Realigned inventory slots

---

## 2026-04-10

### Wood Torch Variants
- Added 11 wood-variant torches (Spruce, Birch, Jungle, Acacia, Dark Oak, Mangrove, Cherry, Bamboo, Crimson, Warped, and Underwater)
- Each variant is crafted from the matching wood plank + a vanilla torch
- All variants work with the auto-placer and Torch Bag

### Torch Bag
- Added the Torch Bag item — a dedicated container for torches with a 16-slot GUI
- The auto-placer checks the Torch Bag for torches before the player inventory

---

## 2026-04-09

### Initial Release
- Auto-places torches in dark areas around the player every ~2 seconds
- Toggle the auto-placer on/off with **G**
- Open the settings screen with a configurable keybinding (unbound by default)
- Settings: light threshold (0–14), placement mode (floor, walls, or both)
- Supports vanilla torches
- Free to use in modpacks and personal games
