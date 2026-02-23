# HyperFactions Developer Documentation

> **Version**: 0.9.0 | **377 classes** | **69 packages** | **20 managers** | **42 commands** | **60 permissions**

Developer documentation for HyperFactions - a comprehensive faction management plugin for Hytale servers.

## Documentation Index

### Core Architecture

| Document | Description |
|----------|-------------|
| [architecture.md](architecture.md) | High-level architecture overview, 9-layer design, package structure |
| [managers.md](managers.md) | Manager layer - 15 core managers with responsibilities and dependency graph |

### Systems

| Document | Description |
|----------|-------------|
| [commands.md](commands.md) | Command system - 42 subcommands across 10 categories |
| [permissions.md](permissions.md) | Permission framework - 60 nodes, chain-based resolution |
| [config.md](config.md) | Config system - ConfigManager, 8 modules, config v5 migration |
| [storage.md](storage.md) | Storage layer - interfaces, JSON adapters, backup system |
| [gui.md](gui.md) | GUI system - 59 pages, 3 registries, navigation flows |
| [protection.md](protection.md) | Protection system - ECS handlers, protection mixin hooks (HyperProtect-Mixin / OrbisGuard-Mixins) |

### API & Integrations

| Document | Description |
|----------|-------------|
| [api.md](api.md) | Developer API reference - HyperFactionsAPI, EconomyAPI, EventBus |
| [integrations.md](integrations.md) | Integration breakdown - permissions, PAPI, WiFlow, HyperProtect-Mixin, OrbisGuard, Gravestones, world map |
| [placeholders.md](placeholders.md) | Placeholder reference - all 35 PAPI & WiFlow placeholders with examples |

### Feature Documentation

| Document | Description |
|----------|-------------|
| [announcements.md](announcements.md) | Announcement system - 7 event types, config, admin exclusions |
| [data-import.md](data-import.md) | Data import & migration - ElbaphFactions/HyFactions importers, config v1→v5 |

## Quick Start

### Entry Points

| File | Purpose |
|------|---------|
| [`platform/HyperFactionsPlugin.java`](../src/main/java/com/hyperfactions/platform/HyperFactionsPlugin.java) | Hytale plugin lifecycle (`setup()` → `start()` → `shutdown()`) |
| [`HyperFactions.java`](../src/main/java/com/hyperfactions/HyperFactions.java) | Core singleton, manager initialization, platform callbacks |
| [`Permissions.java`](../src/main/java/com/hyperfactions/Permissions.java) | All 60 permission node constants |
| [`api/HyperFactionsAPI.java`](../src/main/java/com/hyperfactions/api/HyperFactionsAPI.java) | Public API for third-party mods |

### Key Patterns

**Manager Access**:
```java
HyperFactions core = HyperFactionsPlugin.getInstance().getHyperFactions();
FactionManager factions = core.getFactionManager();
ClaimManager claims = core.getClaimManager();
```

**Public API** (for third-party mods):
```java
if (HyperFactionsAPI.isAvailable()) {
    Faction faction = HyperFactionsAPI.getPlayerFaction(playerUuid);
    EventBus.register(FactionCreateEvent.class, event -> { ... });
}
```

**Config Access**:
```java
ConfigManager config = ConfigManager.get();
int maxMembers = config.getMaxMembers();
boolean pvpEnabled = config.isFactionDamage();
```

**Permission Check**:
```java
PermissionManager.get().hasPermission(playerUuid, Permissions.CLAIM);
```

## Package Overview

```
src/main/java/com/hyperfactions/         (377 classes, 69 packages)
├── HyperFactions.java          # Core singleton
├── Permissions.java            # 60 permission node constants
├── BuildInfo.java              # Auto-generated version info
├── platform/                   # Hytale plugin entry point + extracted handlers
├── lifecycle/                  # Plugin lifecycle helpers (callbacks, tasks, history)
├── manager/                    # Business logic (15 core managers)
├── command/                    # Command system (42 subcommands)
│   └── admin/handler/          # Admin command handlers (8 handler classes)
├── gui/                        # CustomUI pages (59 pages)
│   ├── faction/                # Faction member pages + registry
│   ├── admin/                  # Admin pages, registry, data
│   └── newplayer/              # New player pages, registry, data
├── protection/                 # Territory/zone protection + ECS handlers
├── config/                     # Configuration (8 module configs)
├── storage/                    # Data persistence layer
├── data/                       # Data models (records)
├── api/                        # Public API, EventBus, EconomyAPI
├── integration/                # External integrations
│   ├── permissions/            # Permission providers (HyperPerms, LuckPerms, etc.)
│   ├── protection/             # Protection integrations (HyperProtect-Mixin, OrbisGuard, Gravestones)
│   └── placeholder/            # Placeholder integrations (PAPI, WiFlow)
├── backup/                     # GFS backup management
├── migration/                  # Config migration (v1→v2→v3→v4→v5)
├── importer/                   # ElbaphFactions + HyFactions importers
├── worldmap/                   # World map integration (5 refresh modes)
├── territory/                  # Territory notifications
├── update/                     # Update checking
├── chat/                       # Chat formatting
├── listener/                   # Event listeners
├── debug/                      # Debug utilities
└── util/                       # Utilities (Logger, MessageUtil, UuidUtil, etc.)
```

## Tech Stack

- **Language**: Java 25 (records, pattern matching)
- **Build**: Gradle 9.3.0 with Shadow 9.3.1
- **Platform**: Hytale Server API
- **Storage**: JSON files with async CompletableFuture
- **GUI**: Hytale CustomUI (InteractiveCustomUIPage)
- **Dependencies**: Gson 2.11.0, JetBrains Annotations

## Related Documentation

- [CHANGELOG.md](../CHANGELOG.md) - Version history
- [README.md](../README.md) - User-facing plugin documentation
