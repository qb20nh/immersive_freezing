# Immersive Freezing 🥶

[![Modloader Fabric](https://qb20nh.github.io/githubstatic/dist/ModLoader-Fabric-DBD0B4.svg)](https://modrinth.com/mod/cbbg)
[![Available on modrinth](https://img.shields.io/badge/dynamic/json?color=158000&label=downloads&prefix=+%20&query=downloads&url=https://api.modrinth.com/v2/project/Llm6s2xf&logo=modrinth)](https://modrinth.com/mod/cbbg)
[![Available on curseforge](https://cf.way2muchnoise.eu/full_1410474_downloads.svg)](https://www.curseforge.com/minecraft/mc-mods/cbbg)
[![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/qb20nh/immersive_freezing/gradle.yml?logo=github)](https://github.com/qb20nh/immersive_freezing/actions/workflows/gradle.yml)
[![Funding Github](https://qb20nh.github.io/githubstatic/dist/funding-github-hotpink.svg)](https://github.com/sponsors/qb20nh)

**Immersive Freezing** is a client-side Fabric mod that makes freezing feel more intense through configurable **visual and audio** feedback (e.g., while freezing in powder snow).

## Features

When you’re freezing:

- **First-person camera shake**: configurable **rotation** + **translation** shake that scales with how frozen you are.
- **First-person hand/item tremble**: makes the held item/arm subtly shiver (separate intensity slider).
- **Frost vignette overlay**: a configurable frost effect that **fades in/out smoothly** as you enter/leave freezing conditions.
  - **Resource-pack friendly**: uses the vanilla powder snow overlay texture.
  - **Frost Texture Height Fix**: optional compatibility toggle for resource packs/merged jars that ship the powder snow overlay at an unexpected height.
- **Whiteout post-effect**: optional screen whiteout that ramps up as you freeze (configurable intensity).
- **Ice crack SFX**: plays a short ice-crack sound as you start freezing; volume is configurable and the sound fades out smoothly while thawing.

All effects are **client-only** and **cosmetic** (no gameplay/mechanics changes).

## Video comparison

- [Vanilla Freeze](https://www.youtube.com/watch?v=pjrVJRZpDxI)
- [Immersive Freeze](https://www.youtube.com/watch?v=mBl7QYUDUHQ)

## Installation

- Install **Fabric Loader** and **Fabric API** for your Minecraft version.
- (Optional) Install **Mod Menu** to access the in-game configuration screen.
- Put the `immersive_freezing-x.x.x.jar` into your `.minecraft/mods` folder.

This mod is **client-only**. It does not need to be installed on servers.

## Configuration

### In-game (recommended)

If you have **Mod Menu** installed, you can access the configuration screen directly from the Mods list.

Options:

- **Vignette Enabled**
- **Rotation Intensity**
- **Translation Intensity**
- **Hand Tremble Intensity**
- **Vignette Range**
- **Vignette Speed**
- **Vignette Disturbance Intensity**
- **Frost Texture Height Fix**
- **Whiteout Enabled**
- **Whiteout Intensity**
- **Freeze Sound Volume**

### Config file

You can also edit the config file located at `.minecraft/config/immersive_freezing.json`.

## Compatibility notes

- Requires **Fabric API**.
- Should be compatible with most other client-side mods.

## Credits / Third-party assets

- Developed by **qb20nh**.
- Licensed under **MIT**.
- **Third-party notices**: see `THIRD_PARTY_NOTICES.md` (includes any ElevenLabs-generated SFX usage/attribution constraints, if present).
