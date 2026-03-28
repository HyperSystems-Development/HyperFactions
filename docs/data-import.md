# HyperFactions Data Import & Migration

> **Version**: 0.13.0 | **Packages**: `com.hyperfactions.importer`, `com.hyperfactions.migration`

HyperFactions supports importing data from other faction plugins and automatically migrating its own configuration between versions.

---

## Table of Contents

- [ElbaphFactions Importer](#elbaphfactions-importer)
- [HyFactions V1 Importer](#hyfactions-v1-importer)
- [SimpleClaims Importer](#simpleclaims-importer)
- [FactionsX Importer](#factionsx-importer)
- [Config Migration System](#config-migration-system)
- [Pre-Import Backup](#pre-import-backup)

---

## ElbaphFactions Importer

**Command**: `/f admin import elbaphfactions [path] [flags]`
**Permission**: `hyperfactions.admin.use`

Imports faction data from ElbaphFactions, converting its data format to HyperFactions' format.

### Data Directory

Default: `mods/ElbaphFactions` (or specify a custom path)

Expected files:

| File | Contents |
|------|----------|
| `factions.json` | Array of all factions |
| `claims.json` | Flat key format: `dimension:chunkX:chunkZ` |
| `zones.json` | SafeZones and WarZones in a single file |
| `playernames.json` | Map of UUID to player name |

### Command Options

| Flag | Description |
|------|-------------|
| `--dry-run` | Validate data without importing |
| `--overwrite` | Overwrite existing factions with matching names |
| `--no-zones` | Skip zone import |
| `--no-power` | Skip power data import |

### Data Mapping

| ElbaphFactions | HyperFactions |
|---------------|---------------|
| Faction name, description, color | Direct mapping (color converted to `#RRGGBB`) |
| Members with roles | FactionMember records with role hierarchy |
| Claims (flat key format) | ChunkKey records with world/chunkX/chunkZ |
| SafeZones / WarZones | Zone records with type and claim set |
| FactionPermissions | Converted per-role permission flags |

> **Note**: Worker, farm plot, and faction point data are logged as warnings since HyperFactions does not have equivalents.

```mermaid
flowchart TD
    A[/f admin import elbaphfactions] --> B{Validate Path}
    B -->|Invalid| C[Error: Directory not found]
    B -->|Valid| D[Create Pre-Import Backup]
    D --> E[Load playernames.json]
    E --> F[Load factions.json]
    F --> G[Load claims.json]
    G --> H[Load zones.json]
    H --> I{--dry-run?}
    I -->|Yes| J[Print Validation Report]
    I -->|No| K[Convert & Save Factions]
    K --> L[Convert & Save Claims]
    L --> M[Convert & Save Zones]
    M --> N[Import Complete]

    style C fill:#ef4444,color:#fff
    style J fill:#f59e0b,color:#fff
    style N fill:#22c55e,color:#fff
```

---

## HyFactions V1 Importer

**Command**: `/f admin import hyfactions [path] [flags]`
**Permission**: `hyperfactions.admin.use`

Imports faction data from HyFactions V1, the predecessor format with individual faction files.

### Data Directory

Default: `mods/Kaws_Hyfaction` (or specify a custom path)

Expected structure (files are inside a `config/` subdirectory):

| Path | Contents |
|------|----------|
| `config/faction/` | Individual JSON files per faction |
| `config/Claims.json` | Claims with dimension support |
| `config/SafeZones.json` | SafeZone definitions |
| `config/WarZones.json` | WarZone definitions |
| `config/NameCache.json` | UUID to player name mapping |

### Command Options

Same flags as ElbaphFactions: `--dry-run`, `--overwrite`, `--no-zones`, `--no-power`

### Format Differences from ElbaphFactions

| Aspect | HyFactions | ElbaphFactions |
|--------|-----------|----------------|
| Faction files | Individual per-faction | Single array |
| Claims format | Dimension support | Flat key |
| Zone files | Separate per type | Single combined file |
| Color format | ARGB integer | Hex string |
| Name cache | `NameCache.json` | `playernames.json` |

### Import Features

- Color conversion: ARGB integers to `#RRGGBB` hex
- Power distribution: Faction power split across members
- Chunk clustering: Adjacent zone chunks grouped automatically
- Empty faction handling: Factions with no members are skipped with a warning
- Thread-safe: `ReentrantLock` + `AtomicBoolean` prevents concurrent imports

---

## SimpleClaims Importer

**Command**: `/f admin import simpleclaims [path] [flags]`
**Permission**: `hyperfactions.admin.use`

Imports faction data from the SimpleClaims mod, converting parties and claims to HyperFactions format.

### Data Directory

Default: `Server/universe/SimpleClaims` (or specify a custom path)

Supports two storage formats (auto-detected):

| Format | File | Contents |
|--------|------|----------|
| SQLite | `SimpleClaims.db` | Modern format — parties, claims, name cache in one database |
| JSON | `Parties.json` | Legacy format — party definitions |
| JSON | `Claims.json` | Legacy format — territory claims (ChunkY=Z quirk) |
| JSON | `NameCache.json` | Legacy format — UUID to player name mapping |

### Command Options

| Flag | Description |
|------|-------------|
| `--dry-run` | Validate data without importing |
| `--overwrite` | Overwrite existing factions with matching names |
| `--no-power` | Skip power assignment |

### Data Mapping

| SimpleClaims | HyperFactions |
|-------------|---------------|
| Party name, description, color | Direct mapping (signed RGB integer converted to `#RRGGBB`) |
| Owner → LEADER, Members → MEMBER | 2 roles only (no officer equivalent) |
| Claims per dimension | FactionClaim records with world/chunkX/chunkZ |
| Protection overrides (place, break, interact, pvp) | FactionPermissions outsider flags |
| Mutual party alliances | ALLY relations (one-way alliances skipped) |
| Player allies | No equivalent — logged as warnings |

> **Notes:**
> - SimpleClaims has no power system — all imported players receive the configured max power
> - No faction home support
> - No zone (safezone/warzone) support
> - SQLite format requires the SimpleClaims JAR in the mods folder (for the JDBC driver)
> - Black or missing colors are replaced with a random color

---

## FactionsX Importer

**Command**: `/f admin import factionsx [path] [flags]`
**Permission**: `hyperfactions.admin.use`

Imports faction data from the FactionsX mod (by Humblegod666), converting factions, claims, zones, and player data to HyperFactions format.

### Data Directory

Default: `mods/FactionsX` (or specify a custom path)

Expected structure (files are inside a `config/` subdirectory):

| Path | Contents |
|------|----------|
| `config/factions/{UUID}.json` | Individual JSON files per faction |
| `config/players/{UUID}.json` | Per-player files (name + power) |
| `config/Claims.json` | Territory claims by dimension (ChunkY=Z quirk) |
| `config/Zones.json` | SafeZone and WarZone chunks per dimension |

### Command Options

| Flag | Description |
|------|-------------|
| `--dry-run` | Validate data without importing |
| `--overwrite` | Overwrite existing factions with matching names |
| `--no-zones` | Skip zone import |
| `--no-power` | Skip power data import |

### Data Mapping

| FactionsX | HyperFactions |
|-----------|---------------|
| Faction name, description, color | Direct mapping (color converted to `#RRGGBB`) |
| Owner (implicit LEADER) + Members | FactionMember records; RECRUIT mapped to MEMBER |
| Claims per dimension | FactionClaim records with world/chunkX/chunkZ |
| SafeZones / WarZones | Zone records with type and claim set |
| Per-player power/maxPower | PlayerPower records (power + max power preserved) |
| Per-role permissions (Build, Claim, Interact, Invite, Kick) | FactionPermissions flags per role |
| Home (x/y/z/dimension) | FactionHome with world mapping |
| Relations (ally/enemy/neutral) | FactionRelation records |

> **Notes:**
> - Owner is NOT in the Members map — always treated as LEADER implicitly
> - RECRUIT role is mapped to MEMBER (HyperFactions has 3 roles: LEADER, OFFICER, MEMBER)
> - Thread-safe: `ReentrantLock` + `AtomicBoolean` prevents concurrent imports
> - Empty factions (no members) are skipped with a warning

---

## Config Migration System

HyperFactions automatically migrates configuration files between versions on startup.

### Architecture

| Class | Role |
|-------|------|
| `Migration` | Interface defining a single migration step |
| `MigrationType` | Enum: `CONFIG`, `DATA`, `SCHEMA` |
| `MigrationRegistry` | Singleton registry of all migrations |
| `MigrationRunner` | Executes migrations with backup and rollback |
| `MigrationResult` | Result record with success/failure/warnings |
| `MigrationOptions` | Options record with progress callback |

### Migration Chain

Migrations are applied in sequence. The `MigrationRegistry` builds the chain automatically based on version numbers:

**Config Migrations** (run by `ConfigManager.loadAll()`):

| Migration | From | To | Description |
|-----------|------|----|-------------|
| `ConfigV1ToV2Migration` | v1 | v2 | Split monolithic config into modules |
| `ConfigV2ToV3Migration` | v2 | v3 | Move world map config, convert prefix colors |
| `ConfigV3ToV4Migration` | v3 | v4 | Restructure permissions, add interaction sub-types |
| `ConfigV4ToV5Migration` | v4 | v5 | Remove `warzonePowerLoss`, add per-zone `power_loss` flag |
| `ConfigV5ToV6Migration` | v5 | v6 | Split `config.json` into `config/factions.json` + `config/server.json` |
| `ConfigV6ToV7Migration` | v6 | v7 | Migrate updater URLs, remove worldMap section, restructure economy.json |
| `ConfigV7ToV8Migration` | v7 | v8 | Convert claimBlacklist entries to per-world settings with claiming disabled |

**Data Migrations** (run before storage init in `HyperFactions.enable()`):

| Migration | From | To | Description |
|-----------|------|----|-------------|
| `DataV0ToV1Migration` | v0 | v1 | Move data files into `data/` subdirectory |
| `DataV1ToV2Migration` | v1 | v2 | Move hardcore power data from standalone file into per-faction data files |

### DataV0ToV1Migration

Moves all data files from the plugin root into a `data/` subdirectory for cleaner filesystem layout. Files moved: `factions/`, `players/`, `chat/`, `economy/`, `zones.json`, `invites.json`, `join_requests.json`. Uses `data/.version` marker (absent = needs migration, `1` = already migrated). See [storage.md](storage.md#data-directory-migration-v0v1) for details.

### v1 to v2: Monolithic to Modular

Splits `config.json` into core + module configs:

- Creates `config/` directory
- Extracts: `backup.json`, `chat.json`, `debug.json`, `economy.json`, `faction-permissions.json`
- Adds `configVersion: 2`

### v2 to v3: World Map + Prefix Colors

- Moves `worldMap.enabled` from `config.json` to `config/worldmap.json`
- Converts message prefix from Minecraft color codes to hex colors (`#RRGGBB`)
- Adds `configVersion: 3`

### v3 to v4: Permission Restructure

- Removes `forced` section from `faction-permissions.json`
- Merges forced values into defaults for locked flags
- Adds officer-level permission grants
- Adds interaction sub-types: `doorUse`, `containerUse`, `benchUse`, `processingUse`, `seatUse`
- Adds mob spawning flags: `mobSpawning`, `hostileMobSpawning`, `passiveMobSpawning`, `neutralMobSpawning`
- Converts flat flag map to nested role-level JSON structure
- Adds `configVersion: 4`

### v4 to v5: Zone Power Loss Flag

- Removes deprecated `warzonePowerLoss` config option from `power` section
- Power loss in zones is now controlled per-zone via the `power_loss` zone flag
- Adds `configVersion: 5`

### Backup & Rollback

Before each migration:
1. A ZIP backup is created: `backup_migration_v{from}-to-v{to}_YYYY-MM-DD_HH-mm-ss.zip`
2. Backup includes all affected config files
3. On migration failure, the ZIP is extracted to restore the previous state
4. Separate backup handling for CONFIG vs DATA vs SCHEMA migrations

### Auto-Migration on Load

When `ConfigManager` loads configuration:
1. Reads `configVersion` from `config.json`
2. Checks `MigrationRegistry.hasPendingMigrations(type, dataDir)`
3. If migrations are needed, runs `MigrationRunner.runAll()` automatically
4. Logs all migration results with success/failure/warnings

---

## Pre-Import Backup

Before any import operation, a backup is automatically created:

- Type: `MANUAL` (exempt from auto-rotation)
- Contents: All faction data, player data, zones, and configuration
- Format: ZIP archive with full directory structure
- Location: `backups/` directory under the plugin data folder

This ensures you can always roll back to the pre-import state if the import produces unexpected results.
