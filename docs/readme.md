# HyperFactions Developer Documentation

> **Version**: 0.13.0 | **~480 classes** | **72 packages** | **17 core managers** | **~46 commands** | **76 permissions**

Developer documentation for HyperFactions - a comprehensive faction management plugin for Hytale servers.

## Documentation Index

### Core Architecture

| Document | Description |
|----------|-------------|
| [architecture.md](architecture.md) | High-level architecture overview, 9-layer design, package structure |
| [managers.md](managers.md) | Manager layer - 17 core managers with responsibilities and dependency graph |

### Systems

| Document | Description |
|----------|-------------|
| [commands.md](commands.md) | Command system - ~46 subcommands across 10 categories |
| [permissions.md](permissions.md) | Permission framework - 76 nodes, chain-based resolution |
| [config.md](config.md) | Config system - ConfigManager, 11 config files, config v8 migration |
| [storage.md](storage.md) | Storage layer - interfaces, JSON adapters, safe-save, data directory, backup system |
| [gui.md](gui.md) | GUI system - ~76 pages, 3 registries, navigation flows |
| [protection.md](protection.md) | Protection system - ECS handlers, protection mixin hooks (HyperProtect-Mixin / OrbisGuard-Mixins) |

### API & Integrations

| Document | Description |
|----------|-------------|
| [api.md](api.md) | Developer API reference - HyperFactionsAPI, EconomyAPI, EventBus (in `api.events` package) |
| [integrations.md](integrations.md) | Integration breakdown - permissions, PAPI, WiFlow, HyperProtect-Mixin, OrbisGuard, Gravestones, world map |
| [placeholders.md](placeholders.md) | Placeholder reference - all 51 PAPI & 47 WiFlow placeholders with examples |

### Feature Documentation

| Document | Description |
|----------|-------------|
| [announcements.md](announcements.md) | Announcement system - 7 event types, config, admin exclusions |
| [data-import.md](data-import.md) | Data import & migration - ElbaphFactions/HyFactions/SimpleClaims/FactionsX importers, config v1→v8, data v0→v2 |
| [translation-guide.md](translation-guide.md) | Translation guide for adding new locales |
| [help-markdown.md](help-markdown.md) | Help content markdown format |

## Quick Start

### Entry Points

| File | Purpose |
|------|---------|
| [`platform/HyperFactionsPlugin.java`](../src/main/java/com/hyperfactions/platform/HyperFactionsPlugin.java) | Hytale plugin lifecycle (`setup()` → `start()` → `shutdown()`) |
| [`HyperFactions.java`](../src/main/java/com/hyperfactions/HyperFactions.java) | Core singleton, manager initialization, platform callbacks |
| [`Permissions.java`](../src/main/java/com/hyperfactions/Permissions.java) | All 76 permission node constants |
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
    // EventBus is in api.events package
    EventBus.register(FactionCreateEvent.class, event -> { /* handle */ });
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
src/main/java/com/hyperfactions/         (~480 classes, 72 packages)
├── HyperFactions.java          # Core singleton
├── Permissions.java            # 76 permission node constants
├── BuildInfo.java              # Auto-generated version info
├── platform/                   # Hytale plugin entry point + extracted handlers
├── lifecycle/                  # Plugin lifecycle helpers (callbacks, tasks, history)
├── manager/                    # Business logic (17 core managers)
├── command/                    # Command system (~46 subcommands)
│   ├── admin/handler/          # Admin command handlers (11 handler classes)
│   ├── economy/                # Economy subcommands
│   ├── faction/                # Faction management subcommands
│   ├── info/                   # Info subcommands
│   ├── member/                 # Member management subcommands
│   ├── relation/               # Relation subcommands
│   ├── social/                 # Social subcommands
│   ├── teleport/               # Teleport subcommands
│   ├── territory/              # Territory subcommands
│   ├── ui/                     # UI subcommands
│   └── util/                   # Command utilities
├── gui/                        # CustomUI pages (~70 pages + modals)
│   ├── faction/                # Faction member pages + registry + data
│   ├── admin/                  # Admin pages, registry, data
│   ├── help/                   # Help system pages + data
│   ├── newplayer/              # New player pages, registry, data
│   ├── shared/                 # Shared pages, components, data
│   └── test/                   # Test/debug pages
├── protection/                 # Territory/zone protection
│   ├── damage/                 # Damage protection handlers
│   ├── debug/                  # Protection debug utilities
│   ├── ecs/                    # ECS-based protection handlers
│   ├── interactions/           # Interaction protection handlers
│   └── zone/                   # Zone protection handlers
├── config/                     # Configuration (11 module configs)
│   └── modules/                # Individual config modules
├── storage/                    # Data persistence layer
│   └── json/                   # JSON storage adapters
├── data/                       # Data models (records)
├── economy/                    # Economy system
├── api/                        # Public API, EconomyAPI
│   └── events/                 # EventBus and event types
├── integration/                # External integrations
│   ├── permissions/            # Permission providers (HyperPerms, LuckPerms, etc.)
│   ├── economy/                # Economy integrations
│   ├── protection/             # Protection integrations (HyperProtect-Mixin, OrbisGuard, Gravestones)
│   └── placeholder/            # Placeholder integrations (PAPI, WiFlow)
├── backup/                     # GFS backup management
├── migration/                  # Config migration (v1→v8) and data migration (v0→v2)
│   └── migrations/             # config/ and data/ migration implementations
├── importer/                   # ElbaphFactions, HyFactions, SimpleClaims, FactionsX importers
│   ├── elbaphfactions/         # ElbaphFactions data models
│   ├── factionsx/              # FactionsX data models
│   ├── hyfactions/             # HyFactions data models
│   └── simpleclaims/           # SimpleClaims data models
├── worldmap/                   # World map integration (5 refresh modes)
├── territory/                  # Territory notifications
├── update/                     # Update checking
├── chat/                       # Chat formatting
├── listener/                   # Event listeners
├── debug/                      # Debug utilities
├── build/                      # Build-time tools (HelpLangGenerator)
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
