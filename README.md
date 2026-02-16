# HyperFactions

[![Discord](https://img.shields.io/badge/Discord-Join%20Us-7289DA?logo=discord&logoColor=white)](https://discord.gg/SNPjyfkYPc)
[![GitHub](https://img.shields.io/github/stars/HyperSystemsDev/HyperFactions?style=social)](https://github.com/HyperSystemsDev/HyperFactions)
[![License: GPLv3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

A comprehensive faction management mod for Hytale servers featuring territory claims, alliances, strategic PvP, economy, and extensive customization. Part of the **[HyperSystems](#hypersystems-suite)** plugin suite.

## Overview

HyperFactions transforms your Hytale server into a dynamic faction-based environment where players create factions, claim territories, forge alliances, manage treasuries, and engage in strategic PvP combat. With 59 interactive GUI pages, 42 commands, and deep integration with the HyperSystems ecosystem, it provides a complete faction experience out of the box.

**Main Commands:** `/faction` | `/f` | `/hf`

---

## Features

### Faction Management

| Feature | Status |
|---------|--------|
| Create, disband, rename factions | Implemented |
| Customizable colors and descriptions | Implemented |
| Three-tier roles (Leader, Officer, Member) | Implemented |
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
| Terrain-based map mode | Implemented |

### Power System

| Feature | Status |
|---------|--------|
| Per-player power with configurable cap | Implemented |
| Death penalty (configurable) | Implemented |
| Auto-regeneration at intervals | Implemented |
| Per-zone power loss flags | Implemented |
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
| VaultUnlocked integration | Implemented |
| Ecotale integration | [Planned #20](https://github.com/HyperSystemsDev/HyperFactions/issues/20) |

### Protection

| Feature | Status |
|---------|--------|
| Block, item, PvP protection | Implemented |
| OrbisGuard-Mixins (11 hooks) | Implemented |
| Mob spawn suppression | Implemented |
| Gravestones integration | Implemented |
| Zone flags (25+) | Implemented |
| Command blocking in zones | [Planned #29](https://github.com/HyperSystemsDev/HyperFactions/issues/29) |

### GUI

| Feature | Status |
|---------|--------|
| 59 interactive pages across 3 registries | Implemented |
| Admin dashboard | Implemented |
| Faction browser with search | Implemented |
| Territory map with terrain imagery | Implemented |
| Real-time updates | Implemented |

### Admin

| Feature | Status |
|---------|--------|
| Zone management (SafeZone, WarZone) | Implemented |
| Backup system (GFS rotation) | Implemented |
| Data import (ElbaphFactions, HyFactions) | Implemented |
| Config migration (v1-v5) | Implemented |
| Update checker | Implemented |
| Admin GUI: Config editor | [Planned #40](https://github.com/HyperSystemsDev/HyperFactions/issues/40) |
| Admin GUI: Backup manager | [Planned #41](https://github.com/HyperSystemsDev/HyperFactions/issues/41) |
| Admin GUI: Updates page | [Planned #42](https://github.com/HyperSystemsDev/HyperFactions/issues/42) |

### Integrations

| Feature | Status |
|---------|--------|
| HyperPerms | Implemented |
| LuckPerms | Implemented |
| VaultUnlocked | Implemented |
| PlaceholderAPI (35 placeholders) | Implemented |
| WiFlow | Implemented |
| OrbisGuard / OrbisGuard-Mixins | Implemented |
| Gravestones | Implemented |

### Planned Features

| Feature | Status |
|---------|--------|
| Database storage backend | [Planned #45](https://github.com/HyperSystemsDev/HyperFactions/issues/45) |
| Performance caching | [Planned #44](https://github.com/HyperSystemsDev/HyperFactions/issues/44) |
| Command aliases | [Planned #43](https://github.com/HyperSystemsDev/HyperFactions/issues/43) |
| Faction levels & progression | [Planned #39](https://github.com/HyperSystemsDev/HyperFactions/issues/39) |
| Raid system | [Planned #38](https://github.com/HyperSystemsDev/HyperFactions/issues/38) |
| War declarations | [Planned #37](https://github.com/HyperSystemsDev/HyperFactions/issues/37) |
| Faction vaults | [Planned #35](https://github.com/HyperSystemsDev/HyperFactions/issues/35) |
| Server-managed factions | [Planned #33](https://github.com/HyperSystemsDev/HyperFactions/issues/33) |
| Relational placeholders | [Planned #26](https://github.com/HyperSystemsDev/HyperFactions/issues/26) |
| NPC integrations | [Considering #21](https://github.com/HyperSystemsDev/HyperFactions/issues/21) |
| Localization | [Planned #19](https://github.com/HyperSystemsDev/HyperFactions/issues/19) |
| CurseForge updates | [Planned #17](https://github.com/HyperSystemsDev/HyperFactions/issues/17) |

---

## Quick Start

1. **Download** the latest release from [GitHub Releases](https://github.com/HyperSystemsDev/HyperFactions/releases) or [CurseForge](https://www.curseforge.com/hytale/mods/hyperfactions)
2. **Install** by placing `HyperFactions-<version>.jar` in your server's `mods/` directory
3. **Configure** by editing `mods/com.hyperfactions_HyperFactions/config.json` after first startup
4. **Create a faction** with `/f create MyFaction` and claim territory with `/f claim`

**Optional:** Install [HyperPerms](https://github.com/HyperSystemsDev/HyperPerms) for enhanced permission control with groups, tracks, and contextual permissions.

---

## Documentation

Comprehensive developer and admin documentation is available in the [`docs/`](docs/) directory:

### Core Architecture

| Document | Description |
|----------|-------------|
| [architecture.md](docs/architecture.md) | 9-layer design, package structure, dependency graph |
| [managers.md](docs/managers.md) | 15 core managers with responsibilities and lifecycles |

### Systems

| Document | Description |
|----------|-------------|
| [commands.md](docs/commands.md) | 42 subcommands across 10 categories with full syntax |
| [permissions.md](docs/permissions.md) | 60 permission nodes, chain-based resolution |
| [config.md](docs/config.md) | ConfigManager, 8 module configs, migration (v1-v5) |
| [storage.md](docs/storage.md) | Interface-based storage, JSON adapters, backup system |
| [gui.md](docs/gui.md) | 59 pages, 3 registries, navigation flows |
| [protection.md](docs/protection.md) | ECS handlers, OrbisGuard-Mixins, zone flags |

### API & Integrations

| Document | Description |
|----------|-------------|
| [api.md](docs/api.md) | HyperFactionsAPI, EconomyAPI, EventBus for third-party mods |
| [integrations.md](docs/integrations.md) | HyperPerms, LuckPerms, PAPI, WiFlow, OrbisGuard, Gravestones |
| [placeholders.md](docs/placeholders.md) | All 35 PAPI & WiFlow placeholders with examples |

### Feature Documentation

| Document | Description |
|----------|-------------|
| [announcements.md](docs/announcements.md) | Server-wide broadcasts, 7 event types |
| [data-import.md](docs/data-import.md) | ElbaphFactions/HyFactions importers, config migration |

---

## Building from Source

### Requirements

- Java 25
- Gradle 9.3.0+ (included via wrapper)
- Hytale Server JAR (Early Access)

### Build

```bash
# From HyperSystems root (multi-project build):
./gradlew :HyperFactions:shadowJar

# Output: HyperFactions/build/libs/HyperFactions-<version>.jar
```

See [CONTRIBUTING.md](CONTRIBUTING.md) for full development setup and contribution guidelines.

---

## Support

- **Discord:** [Join our server](https://discord.gg/SNPjyfkYPc)
- **GitHub Issues:** [Report bugs or request features](https://github.com/HyperSystemsDev/HyperFactions/issues)

---

## HyperSystems Suite

HyperFactions is part of the HyperSystems plugin ecosystem. All plugins integrate seamlessly when installed together.

| Mod | Description | Status | Link |
|-----|-------------|--------|------|
| **HyperPerms** | Permissions management with groups, tracks, and web editor | Production | [GitHub](https://github.com/HyperSystemsDev/HyperPerms) |
| **HyperHomes** | Personal home teleportation with sharing | Production | [GitHub](https://github.com/HyperSystemsDev/HyperHomes) |
| **HyperFactions** | Faction management with territory, diplomacy, and economy | Production | [GitHub](https://github.com/HyperSystemsDev/HyperFactions) |
| **HyperWarp** | Warps, spawns, TPA requests, and /back | Production | [GitHub](https://github.com/HyperSystemsDev/HyperWarp) |
| **HyperSpawns** | Mob spawn zone control with advanced filtering | Production | [GitHub](https://github.com/HyperSystemsDev/HyperSpawns) |
| **HyperGuard** | Server-side anti-cheat | In Development | [GitHub](https://github.com/HyperSystemsDev/HyperGuard) |
| **HyperVerse** | World management (Multiverse-like) | In Development | [GitHub](https://github.com/HyperSystemsDev/HyperVerse) |
| **HyperLogger** | Activity logging and rollback | In Development | [GitHub](https://github.com/HyperSystemsDev/HyperLogger) |

---

## License

HyperFactions is licensed under the [GNU General Public License v3.0](LICENSE).

---

*Developed by [HyperSystemsDev](https://github.com/HyperSystemsDev) — Forge Your Empire*
