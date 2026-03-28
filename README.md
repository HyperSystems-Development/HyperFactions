# HyperFactions

[![Discord](https://img.shields.io/badge/Discord-Join%20Us-7289DA?logo=discord&logoColor=white)](https://discord.com/invite/aZaa5vcFYh)
[![GitHub](https://img.shields.io/github/stars/HyperSystems-Development/HyperFactions?style=social)](https://github.com/HyperSystems-Development/HyperFactions)
[![License: GPLv3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

A comprehensive faction management mod for Hytale servers featuring territory claims, alliances, strategic PvP, economy, and extensive customization. Part of the **[HyperSystems](#hypersystems-suite)** plugin suite.

## Overview

HyperFactions transforms your Hytale server into a dynamic faction-based environment where players create factions, claim territories, forge alliances, manage treasuries, and engage in strategic PvP combat. With 70+ interactive GUI pages, 46 commands, and deep integration with the HyperSystems ecosystem, it provides a complete faction experience out of the box.

**Main Commands:** `/faction` | `/f` | `/hf`

---

## Features

### Faction Management

| Feature | Status |
|---------|--------|
| Create, disband, rename factions | Implemented |
| Customizable colors and descriptions | Implemented |
| Three-tier roles with customizable display names | Implemented |
| Smart invitation system with expiration | Implemented |
| Join requests for closed factions | Implemented |
| Configurable member limits | Implemented |

### Territory

| Feature | Status |
|---------|--------|
| Chunk-based claiming with visual feedback | Implemented |
| World map overlays with faction colors | Implemented |
| Power-based claim limits | Implemented |
| Adjacent claims mode | Implemented |
| Overclaiming for strategic warfare | Implemented |
| Inactive faction claim decay | Implemented |
| Multi-world support | Implemented |
| Per-world settings (wildcard patterns) | Implemented |
| Terrain-based map mode | Implemented |

### Power System

| Feature | Status |
|---------|--------|
| Per-player power with configurable cap | Implemented |
| Death penalty (configurable) | Implemented |
| Auto-regeneration at intervals | Implemented |
| Per-zone power loss flags | Implemented |
| Hardcore mode (shared faction pool) | Implemented |
| Per-world power loss control | Implemented |
| Admin power management | Implemented |

### Diplomacy

| Feature | Status |
|---------|--------|
| Ally, enemy, neutral relations | Implemented |
| Alliance request/accept/reject workflow | Implemented |
| Color-coded displays in GUI and chat | Implemented |

### Combat

| Feature | Status |
|---------|--------|
| Combat tagging with logout prevention | Implemented |
| Relationship-based PvP rules | Implemented |
| Spawn protection | Implemented |

### Communication

| Feature | Status |
|---------|--------|
| Faction chat with history | Implemented |
| Alliance chat | Implemented |
| Public chat faction tags | Implemented |

### Economy

| Feature | Status |
|---------|--------|
| Faction treasury with balance tracking | Implemented |
| Deposits, withdrawals, inter-faction transfers | Implemented |
| Transaction log | Implemented |
| Faction upkeep system (flat/progressive tiered pricing) | Implemented |
| VaultUnlocked integration | Implemented |

### Protection

| Feature | Status |
|---------|--------|
| Block, item, PvP protection | Implemented |
| [HyperProtect-Mixin](https://www.curseforge.com/hytale/bootstrap/hyperprotect-mixin) (28 hooks, recommended) | Implemented |
| OrbisGuard-Mixins (11 hooks, alternative) | Implemented |
| Dual-provider auto-detection | Implemented |
| Mob spawn suppression | Implemented |
| Mob clearing zone flags | Implemented |
| Gravestones integration | Implemented |
| Zone flags (51) | Implemented |

### GUI

| Feature | Status |
|---------|--------|
| 70+ interactive pages across 3 registries | Implemented |
| Faction leaderboard | Implemented |
| Admin dashboard | Implemented |
| Faction browser with search | Implemented |
| Territory map with terrain imagery | Implemented |
| Real-time updates | Implemented |

### Admin

| Feature | Status |
|---------|--------|
| Zone management (SafeZone, WarZone) | Implemented |
| Backup system (GFS rotation) | Implemented |
| Data import (ElbaphFactions, HyFactions, SimpleClaims, FactionsX) | Implemented |
| Config migration (v1-v8) | Implemented |
| Update checker | Implemented |
| Admin GUI: Config editor | Implemented |
| Admin GUI: Backup manager | Implemented |
| Admin GUI: Updates page | Implemented |

### Integrations

| Feature | Status |
|---------|--------|
| HyperPerms | Implemented |
| LuckPerms | Implemented |
| VaultUnlocked | Implemented |
| PlaceholderAPI (49 placeholders, incl. relational) | Implemented |
| WiFlow | Implemented |
| [HyperProtect-Mixin](https://www.curseforge.com/hytale/bootstrap/hyperprotect-mixin) (recommended) | Implemented |
| OrbisGuard / OrbisGuard-Mixins | Implemented |
| Gravestones | Implemented |
| KyuubiSoft Core (citizen NPC protection) | Implemented |
| BetterMap | Implemented |
| HyperEssentials | Implemented |

### Planned Features

| Feature | Status |
|---------|--------|
| Database storage backend | [Planned #45](https://github.com/HyperSystems-Development/HyperFactions/issues/45) |
| Performance caching | [Planned #44](https://github.com/HyperSystems-Development/HyperFactions/issues/44) |
| Command aliases | [Planned #43](https://github.com/HyperSystems-Development/HyperFactions/issues/43) |
| Faction levels & progression | [Planned #39](https://github.com/HyperSystems-Development/HyperFactions/issues/39) |
| Raid system | [Planned #38](https://github.com/HyperSystems-Development/HyperFactions/issues/38) |
| War declarations | [Planned #37](https://github.com/HyperSystems-Development/HyperFactions/issues/37) |
| Faction vaults | [Planned #35](https://github.com/HyperSystems-Development/HyperFactions/issues/35) |
| Server-managed factions | [Planned #33](https://github.com/HyperSystems-Development/HyperFactions/issues/33) |
| NPC integrations | [Considering #21](https://github.com/HyperSystems-Development/HyperFactions/issues/21) |

---

## Quick Start

1. **Download** the latest release from [GitHub Releases](https://github.com/HyperSystems-Development/HyperFactions/releases) or [CurseForge](https://www.curseforge.com/hytale/mods/hyperfactions)
2. **Install** by placing `HyperFactions-<version>.jar` in your server's `mods/` directory
3. **Configure** by editing files in `mods/com.hyperfactions_HyperFactions/config/` after first startup — `factions.json` for faction gameplay settings, `server.json` for server behavior settings
4. **Create a faction** with `/f create MyFaction` and claim territory with `/f claim`

**Recommended:** Install [HyperProtect-Mixin](https://www.curseforge.com/hytale/bootstrap/hyperprotect-mixin) in `earlyplugins/` for full protection coverage (28 hook types including teleporter/portal blocking, entity damage, capture crate/NPC protection, mount/barter/fluid/projectile control, and respawn override).

**Optional:** Install [HyperPerms](https://github.com/HyperSystems-Development/HyperPerms) for enhanced permission control with groups, tracks, and contextual permissions.

---

## Documentation

Comprehensive developer and admin documentation is available in the [`docs/`](docs/) directory:

### Core Architecture

| Document | Description |
|----------|-------------|
| [architecture.md](docs/architecture.md) | 9-layer design, package structure, dependency graph |
| [managers.md](docs/managers.md) | 16 core managers with responsibilities and lifecycles |

### Systems

| Document | Description |
|----------|-------------|
| [commands.md](docs/commands.md) | 52 subcommands across 10 categories with full syntax |
| [permissions.md](docs/permissions.md) | 76 permission nodes, chain-based resolution |
| [config.md](docs/config.md) | ConfigManager, 11 config files, migration (v1-v8) |
| [storage.md](docs/storage.md) | Interface-based storage, JSON adapters, backup system |
| [gui.md](docs/gui.md) | 76 pages, 3 registries, navigation flows |
| [protection.md](docs/protection.md) | ECS handlers, HyperProtect-Mixin / OrbisGuard-Mixins, zone flags |

### API & Integrations

| Document | Description |
|----------|-------------|
| [api.md](docs/api.md) | HyperFactionsAPI, EconomyAPI, EventBus for third-party mods |
| [integrations.md](docs/integrations.md) | HyperPerms, LuckPerms, PAPI, WiFlow, HyperProtect-Mixin, OrbisGuard, Gravestones, KyuubiSoft, BetterMap, HyperEssentials |
| [placeholders.md](docs/placeholders.md) | All 49 PAPI & WiFlow placeholders with examples |

### Feature Documentation

| Document | Description |
|----------|-------------|
| [announcements.md](docs/announcements.md) | Server-wide broadcasts, 7 event types |
| [data-import.md](docs/data-import.md) | ElbaphFactions/HyFactions/SimpleClaims/FactionsX importers, config migration |
| [translation-guide.md](docs/translation-guide.md) | Translation guide for adding new locales |
| [help-markdown.md](docs/help-markdown.md) | Help content markdown format |

---

## For Developers

### Maven Dependency (JitPack)

Add HyperFactions as a dependency to build integrations:

```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    compileOnly 'com.github.HyperSystems-Development:HyperFactions:v0.13.0'
}
```

See the [Developer API Reference](docs/api.md) for full API documentation.

## Building from Source

### Requirements

- Java 25
- Gradle 9.3.0+ (included via wrapper)

The Hytale Server API and all required dependencies are resolved automatically from Maven. Optional integrations (WiFlowPlaceholderAPI, GravestonePlugin) use reflection and are **not required** to compile.

For full functionality during local development, optionally download these JARs into `libs/`:

| JAR | Download | Required? |
|-----|----------|-----------|
| WiFlowPlaceholderAPI | [CurseForge](https://www.curseforge.com/hytale/mods/wiflows-placeholderapi) | Optional |
| GravestonePlugin | [CurseForge](https://www.curseforge.com/hytale/mods/gravestones) | Optional |

### Build

```bash
# From HyperSystems root (multi-project build):
./gradlew :HyperFactions:shadowJar

# Output: HyperFactions/build/libs/HyperFactions-<version>.jar
```

See [CONTRIBUTING.md](CONTRIBUTING.md) for full development setup and contribution guidelines.

---

## Support

- **Discord:** [Join our server](https://discord.com/invite/aZaa5vcFYh)
- **GitHub Issues:** [Report bugs or request features](https://github.com/HyperSystems-Development/HyperFactions/issues)

---

## HyperSystems Suite

HyperFactions is part of the HyperSystems plugin ecosystem. All plugins integrate seamlessly when installed together.

| Mod | Description | Status | Link |
|-----|-------------|--------|------|
| **HyperPerms** | Permissions management with groups, tracks, and web editor | Production | [GitHub](https://github.com/HyperSystems-Development/HyperPerms) |
| **HyperFactions** | Faction management with territory, diplomacy, and economy | Production | [GitHub](https://github.com/HyperSystems-Development/HyperFactions) |
| **HyperEssentials** | Modular server essentials (homes, warps, spawns, TPA, kits, moderation) | Production | [GitHub](https://github.com/HyperSystems-Development/HyperEssentials) |
| **HyperProtect-Mixin** | Server-level event interception for protection hooks | Production | [CurseForge](https://www.curseforge.com/hytale/bootstrap/hyperprotect-mixin) |
| **Ecotale** | Economy system with balance HUD, admin GUI, multi-storage | Production | [CurseForge](https://www.curseforge.com/hytale/mods/ecotale) |
| **Werchat** | Channel-based chat system with moderation tools | Production | [CurseForge](https://www.curseforge.com/hytale/mods/werchat) |
| **HyperGuard** | Server-side anti-cheat | In Development | [GitHub](https://github.com/HyperSystems-Development/HyperGuard) |
| **HyperVerse** | World management (Multiverse-like) | In Development | [GitHub](https://github.com/HyperSystems-Development/HyperVerse) |
| **HyperLogger** | Activity logging and rollback | In Development | [GitHub](https://github.com/HyperSystems-Development/HyperLogger) |

---

## License

HyperFactions is licensed under the [GNU General Public License v3.0](LICENSE).

---

*Developed by [HyperSystems-Development](https://github.com/HyperSystems-Development) — Forge Your Empire*
