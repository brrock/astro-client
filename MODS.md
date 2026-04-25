# AstroClient Module Reference

> **Version:** 1.0.0 · **Minecraft:** 1.8.9 Forge · **Modules:** 39 · **Typed Settings:** 72

AstroClient is a Forge 1.8.9 client focused on Bedwars QoL, HUD overlays, and client-side rendering tweaks. Modules are managed centrally by `ModuleManager`, enabled through the ClickGUI, and persisted to `.minecraft/astroclient/config.json`.

## Controls & UI

| Action | Key |
|---|---|
| Open ClickGUI | `Right Shift` |
| Open HUD Editor | `H` |
| Zoom | `Q` held |
| Freelook | `Left Alt` held |

HUD modules are draggable and use the shared `Element Scale` setting from `HUDModule`. Hold modules are `Zoom` and `Freelook`; everything else is toggle-based.

## Module Catalogue

### Movement

| Module | Mode | Key | Notes |
|---|---|---|---|
| `ToggleSprint` | Toggle | None | `Show HUD` |
| `ToggleSneak` | Toggle | None | `Fly Boost`, `Fly Boost Amount` |
| `Freelook` | Hold | `Left Alt` | No typed settings |

### Render

| Module | Mode | Key | Notes |
|---|---|---|---|
| `Keystrokes` | Toggle | None | `Show CPS`, `Scale`, `Background Opacity` |
| `CPS Counter` | Toggle | None | `Right Click`, `Text Color` |
| `TNT Timer` | Toggle | None | `Max Distance`, `Line Width`, `Background` |
| `Cooldowns` | Toggle | None | `Ender Pearl`, `Sword` |
| `FPS Display` | Toggle | None | `Show Label`, `Text Color` |
| `Coordinates` | Toggle | None | `Decimals`, `Show Biome`, `Show Direction`, `Text Color` |
| `Clock` | Toggle | None | No typed settings |
| `Ping Display` | Toggle | None | `Show Label`, `Color Coded` |
| `Memory Display` | Toggle | None | No typed settings |
| `Server Address` | Toggle | None | No typed settings |
| `Day Counter` | Toggle | None | No typed settings |
| `Direction` | Toggle | None | No typed settings |
| `Combo Counter` | Toggle | None | `Reset Timeout`, `Hide At Zero` |
| `Reach Display` | Toggle | None | `Display Time`, `Decimals` |
| `Item Counter` | Toggle | None | No typed settings |
| `Pack Display` | Toggle | None | No typed settings |
| `Armor Status` | Toggle | None | `Show Durability`, `Durability Mode` |
| `Potion Effects` | Toggle | None | No typed settings |
| `Bedwars Stats` | Toggle | None | `Display`, `Show When Empty` |
| `Bedwars Timers` | Toggle | None | `Show Diamonds`, `Show Emeralds`, `Show Bed Gone`, `Show Game End`, `Compact` |
| `Zoom` | Hold | `Q` | `Default FOV`, `Smooth Camera`, `Scroll Step` |
| `Fullbright` | Toggle | None | No typed settings |
| `Level Head` | Toggle | None | `Background`, `Show Own Level`, `Display`, `Level Colors`, `Text Shadow`, `Render Distance`, `BG Opacity` |

### Player

| Module | Mode | Key | Notes |
|---|---|---|---|
| `Hitboxes` | Toggle | None | `Color`, `Line Width`, `Players`, `Mobs`, `Items`, `Show Self` |
| `FOVChanger` | Toggle | None | Persisted config field `fov` |
| `NoHurtCam` | Toggle | None | No typed settings |
| `Nametags` | Toggle | None | `Text Shadow`, `Background Opacity`, `Background Color`, `Scale`, `Show Health` |
| `Scoreboard` | Toggle | None | `Scale`, `Hide Numbers`, `Hide Scoreboard` |
| `CustomCrosshair` | Toggle | None | `Type`, `Color`, `Gap`, `Length`, `Thickness`, `Center Dot`, `Outline`, `Outline Width`, `Opacity` |

### World

| Module | Mode | Key | Notes |
|---|---|---|---|
| `MotionBlur` | Toggle | None | `Amount` |
| `MenuBlur` | Toggle | None | No typed settings |
| `TimeChanger` | Toggle | None | Persisted config field `targetTime` |
| `WeatherChanger` | Toggle | None | No typed settings |
| `BlockOutline` | Toggle | None | `Outline Color`, `Line Width` |

### Misc

| Module | Mode | Key | Notes |
|---|---|---|---|
| `AutoGG` | Toggle | None | Persisted config field `ggMessage` |
| `ChatMod` | Toggle | None | Persisted config fields `timestamps`, `stacking` |

## Commands

| Command | Alias | Purpose |
|---|---|---|
| `/astrokey <key>` | `/setapikey` | Stores the Hypixel API key used by `Level Head` |

## Settings & Persistence

- Module enable state, keybinds, visibility, HUD positions, and typed settings are saved in `.minecraft/astroclient/config.json`.
- Typed settings use the shared `Setting` hierarchy: `BooleanSetting`, `NumberSetting`, `ModeSetting`, and `ColorSetting`.
- Modules that implement `ConfigManager.Configurable` also persist custom fields in the same JSON entry.
- `Zoom` and `Freelook` are hold modules, so their enabled state is not persisted the same way as toggle modules.
- `HUDModule` provides the draggable/scalable HUD wrapper used by the on-screen overlays.

## Notes

- `Level Head` uses the Hypixel API key stored by `/astrokey`.
- `Nametags` adds the AstroClient user marker for players detected through the Astro client channel.
- Detected Astro users are also marked in the tab list beside their name.
- `Scoreboard` temporarily suppresses the vanilla sidebar and re-renders it with its own scale and filters.
