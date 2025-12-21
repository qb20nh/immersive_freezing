# Immersive Freezing 🥶

[![Modloader Fabric](https://qb20nh.github.io/githubstatic/dist/ModLoader-Fabric-DBD0B4.svg)](https://modrinth.com/mod/cbbg)
[![Available on modrinth](https://img.shields.io/badge/dynamic/json?color=158000&label=downloads&prefix=+%20&query=downloads&url=https://api.modrinth.com/v2/project/[TBD]&logo=modrinth)](https://modrinth.com/mod/cbbg)
[![Available on curseforge](https://cf.way2muchnoise.eu/full_[TBD]_downloads.svg)](https://www.curseforge.com/minecraft/mc-mods/cbbg)
[![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/qb20nh/immersive_freezing/gradle.yml?logo=github)](https://github.com/qb20nh/immersive_freezing/actions/workflows/gradle.yml)
[![Funding Github](https://qb20nh.github.io/githubstatic/dist/funding-github-hotpink.svg)](https://github.com/sponsors/qb20nh)

**Immersive Freezing** is a client-side Fabric mod that enhances the visual experience of freezing in Minecraft (e.g., in powder snow). It introduces a camera shake effect and a frosty vignette that intensifies as you freeze, making the danger feel more immediate and immersive.

## What it does

When you are freezing (e.g., inside powder snow):

- **Camera Shake:** The camera will gently shake and rotate, simulating the shivering of the player.
- **Frost Vignette:** A frost effect creeps in from the edges of your screen, growing stronger as your freeze percentage increases.

All effects are purely visual and do not affect gameplay mechanics.

## Installation

- Install **Fabric Loader** and **Fabric API** for your Minecraft version.
- (Optional) Install **Mod Menu** to access the in-game configuration screen.
- Put the `immersive_freezing-x.x.x.jar` into your `.minecraft/mods` folder.

This mod is **client-only**. It does not need to be installed on servers.

## Configuration

### In-game (recommended)

If you have **Mod Menu** installed, you can access the configuration screen directly from the Mods list.

Available options:

- **Rotation Intensity:** Adjusts how much the camera rotates during the shake effect.
- **Translation Intensity:** Adjusts how much the camera moves side-to-side during the shake effect.
- **Vignette Enabled:** Toggles the frost vignette overlay.
- **Vignette Range:** Controls the spread of the vignette.
- **Vignette Speed:** Controls how fast the vignette pulse/disturbance animates.
- **Vignette Disturbance Intensity:** Controls the intensity of the vignette's fluctuating effect.
- **Debug Overlay:** Shows debug information about the freezing effect.

### Config file

You can also edit the config file located at `.minecraft/config/immersive_freezing.json`.

## Compatibility notes

- Requires **Fabric API**.
- Should be compatible with most other client-side mods.

## Credits / Third-party assets

- Developed by **qb20nh**.
- Licensed under **MIT**.
- **Third-party notices**: see `THIRD_PARTY_NOTICES.md` (includes any ElevenLabs-generated SFX usage/attribution constraints, if present).
