# AstroClient

AstroClient is a Forge 1.8.9 client mod for Minecraft focused on Bedwars HUDs, quality-of-life toggles, and client-side visual customization. Fully vibe coded with Opus 4.6 and GPT5.4/5.

## Overview

- `39` modules
- `72` typed settings
- Minecraft `1.8.9`
- Java `8`
- Forge Gradle build

The mod stores its config in `.minecraft/astroclient/config.json` and supports draggable HUD overlays through the built-in HUD editor.

## Features

- Movement utilities: `ToggleSprint`, `ToggleSneak`, `Freelook`
- HUD modules: FPS, coordinates, CPS, keystrokes, ping, clock, memory, server address, day counter, direction, combo, reach, item count, resource pack, armor status, potion effects, Bedwars stats, Bedwars timers, cooldowns, and more
- Player rendering tools: `Hitboxes`, `FOVChanger`, `NoHurtCam`, `Nametags`, `Scoreboard`, `CustomCrosshair`
- World visual tweaks: `MotionBlur`, `MenuBlur`, `TimeChanger`, `WeatherChanger`, `BlockOutline`
- Misc tools: `AutoGG`, `ChatMod`

## Controls

| Action | Key |
|---|---|
| Open ClickGUI | `Right Shift` |
| Open HUD Editor | `H` |
| Zoom | `Q` held |
| Freelook | `Left Alt` held |

## Commands

- `/astrokey <key>` or `/setapikey` sets the Hypixel API key used by `Level Head`

## Build

```bash
./gradlew build
```

The compiled jar is written to `build/libs/`.

## Config

- Main config: `.minecraft/astroclient/config.json`
- API key file: `.minecraft/astroclient/apikey.txt`
- Forge splash config: `.minecraft/config/splash.properties`

## Notes

- `Zoom` and `Freelook` are hold-style modules.
- `HUDModule` provides draggable, scalable on-screen elements.
- `ConfigManager` persists enabled state, keybinds, visibility, HUD positions, typed settings, and a few legacy custom fields.
- Astro user markers are automatic only: detected Astro clients get a marker in nametags and the tab list.
