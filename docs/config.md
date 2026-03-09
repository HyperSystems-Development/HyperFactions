# HyperFactions Config System

> **Version**: 0.11.0 | **Config version**: 7 | **11 config files**

Architecture documentation for the HyperFactions configuration system.

## Overview

HyperFactions uses a modular JSON-based configuration system with:

- **ConfigManager** - Central coordinator for all config files
- **FactionsConfig** - Faction gameplay settings in `config/factions.json`
- **ServerConfig** - Server behavior settings in `config/server.json` (includes `configVersion`)
- **Module Configs** - 8 feature-specific configs in `config/` subdirectory
- **Validation** - Automatic validation with warnings and auto-correction
- **Migration** - Automatic config migration (v1→v2→v3→v4→v5→v6→v7) with backup/rollback

> **Note:** `CoreConfig` and `config.json` are deprecated. The V5→V6 migration splits `config.json` into `config/factions.json` and `config/server.json`, then deletes `config.json`. New installs create only the split files. If migration fails, the plugin falls back to loading from the legacy `config.json`.

## Architecture

```
ConfigManager (singleton)
     │
     ├─► FactionsConfig (config/factions.json)
     │        │
     │        └─► Roles, Faction, Power, Claims, Combat, Relations, Invites, Stuck
     │
     ├─► ServerConfig (config/server.json, configVersion: 7)
     │        │
     │        └─► Teleport, AutoSave, Messages, GUI, Permissions, Updates
     │
     └─► Module Configs (config/*.json)
              │
              ├─► BackupConfig (backup.json)
              ├─► ChatConfig (chat.json)
              ├─► DebugConfig (debug.json)
              ├─► EconomyConfig (economy.json)
              ├─► FactionPermissionsConfig (faction-permissions.json)
              ├─► AnnouncementConfig (announcements.json)
              ├─► GravestoneConfig (gravestones.json)
              ├─► WorldMapConfig (worldmap.json)
              └─► WorldsConfig (worlds.json) — per-world behavior overrides
```

> **Deprecated:** `CoreConfig` (`config.json`) is no longer used. It has been replaced by `FactionsConfig` and `ServerConfig`. Existing installs are automatically migrated (V5→V6).

## File Structure

```
<server>/mods/com.hyperfactions_HyperFactions/
├── config/                        # All configuration files
│   ├── factions.json              # Faction gameplay (roles, faction, power, claims, combat, relations, invites, stuck)
│   ├── server.json                # Server behavior (teleport, autoSave, messages, gui, permissions, updates, configVersion)
│   ├── backup.json
│   ├── chat.json
│   ├── debug.json
│   ├── economy.json
│   ├── faction-permissions.json
│   ├── announcements.json         # Event broadcast toggles + territory notifications
│   ├── gravestones.json           # Gravestone integration settings
│   ├── worldmap.json              # World map refresh modes
│   └── worlds.json                # Per-world settings
├── data/                          # All data files (see storage.md)
│   ├── factions/                  # Per-faction data
│   ├── players/                   # Per-player power data
│   ├── chat/                      # Per-faction chat history
│   ├── economy/                   # Per-faction treasury data
│   ├── zones.json                 # Zone definitions
│   ├── invites.json               # Pending invites
│   ├── join_requests.json         # Pending join requests
│   └── .version                   # Data layout version marker
└── backups/                       # Backup archives (see storage.md)
```

> **Note:** `config.json` no longer exists. It is deleted after the V5→V6 migration. New installs never create it.

## Config Migration

Configuration is automatically migrated on startup. See [Data Import & Migration](data-import.md#config-migration-system) for the full migration chain (v1→v2→v3→v4→v5→v6→v7).

### V5→V6: Config Split

The V5→V6 migration restructures the configuration layout:

1. **Backs up** `config.json` before making changes
2. **Splits** `config.json` into two new files:
   - `config/factions.json` — faction gameplay settings (roles, faction, power, claims, combat, relations, invites, stuck)
   - `config/server.json` — server behavior settings (teleport, autoSave, messages, gui, permissions, updates, configVersion)
3. **Moves** `territoryNotificationsEnabled` to `config/announcements.json`
4. **Replaces** `allowExplosionsInClaims` with 3 granular explosion flags in `config/factions.json` under `claims`
5. **Adds** 9 new claim protection settings in `config/factions.json` under `claims`
6. **Sets** `configVersion` to 6 in `config/server.json`
7. **Deletes** `config.json` after successful migration

**Legacy fallback:** If migration fails, the plugin can still load from the old `config.json`.

### V6→V7: Upkeep & World Map Cleanup

The V6→V7 migration adds economy upkeep fields and removes deprecated world map settings:

1. **Adds** upkeep configuration fields to `config/economy.json` (upkeep interval, rates, and thresholds)
2. **Removes** deprecated `worldMap` section from `config/server.json` (world map settings moved to `config/worldmap.json` in earlier versions)
3. **Sets** `configVersion` to 7 in `config/server.json`

## Key Classes

| Class | Path | Purpose |
|-------|------|---------|
| ConfigManager | [`config/ConfigManager.java`](../src/main/java/com/hyperfactions/config/ConfigManager.java) | Singleton coordinator |
| ConfigFile | [`config/ConfigFile.java`](../src/main/java/com/hyperfactions/config/ConfigFile.java) | Base class for config files |
| FactionsConfig | [`config/FactionsConfig.java`](../src/main/java/com/hyperfactions/config/FactionsConfig.java) | Faction gameplay (`config/factions.json`) |
| ServerConfig | [`config/ServerConfig.java`](../src/main/java/com/hyperfactions/config/ServerConfig.java) | Server behavior (`config/server.json`) |
| CoreConfig | [`config/CoreConfig.java`](../src/main/java/com/hyperfactions/config/CoreConfig.java) | **Deprecated** — legacy `config.json` fallback |
| ModuleConfig | [`config/ModuleConfig.java`](../src/main/java/com/hyperfactions/config/ModuleConfig.java) | Base for module configs |
| ValidationResult | [`config/ValidationResult.java`](../src/main/java/com/hyperfactions/config/ValidationResult.java) | Validation tracking |

## ConfigManager

[`config/ConfigManager.java`](../src/main/java/com/hyperfactions/config/ConfigManager.java)

Singleton that orchestrates all configuration:

```java
public class ConfigManager {

    private static ConfigManager instance;

    private Path dataDir;
    private FactionsConfig factionsConfig;
    private ServerConfig serverConfig;
    private BackupConfig backupConfig;
    private ChatConfig chatConfig;
    private DebugConfig debugConfig;
    private EconomyConfig economyConfig;
    private FactionPermissionsConfig factionPermissionsConfig;

    public static ConfigManager get() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    public void loadAll(Path dataDir) {
        this.dataDir = dataDir;

        // 1. Run pending migrations (includes V5→V6 config split)
        runMigrations();

        // 2. Load faction gameplay config
        Path configDir = dataDir.resolve("config");
        factionsConfig = new FactionsConfig(configDir.resolve("factions.json"));
        factionsConfig.load();

        // 3. Load server behavior config
        serverConfig = new ServerConfig(configDir.resolve("server.json"));
        serverConfig.load();

        // 4. Load module configs
        backupConfig = new BackupConfig(configDir.resolve("backup.json"));
        chatConfig = new ChatConfig(configDir.resolve("chat.json"));
        // ... etc

        // 5. Validate all configs
        validateAll();
    }

    public void reloadAll() { ... }
    public void saveAll() { ... }
}
```

### Usage Pattern

```java
// Access config values
ConfigManager config = ConfigManager.get();
int maxMembers = config.getMaxMembers();
boolean pvpEnabled = config.isFactionDamage();

// Access specific config objects
FactionsConfig factions = config.factions();
ServerConfig server = config.server();
BackupConfig backup = config.backup();
```

## Config Loading Process

```
1. ConfigManager.loadAll(dataDir)
        │
        ▼
2. Run pending migrations (MigrationRunner)
   - Includes V5→V6: split config.json → config/factions.json + config/server.json
        │
        ▼
3. Load FactionsConfig
   - Read config/factions.json
   - Apply defaults for missing keys
   - Save with new keys added
        │
        ▼
4. Load ServerConfig
   - Read config/server.json
   - Apply defaults for missing keys
   - Save with new keys added
        │
        ▼
5. Load Module Configs (same process)
        │
        ▼
6. Validate all configs
   - Check value ranges
   - Log warnings for invalid values
   - Auto-correct where possible
```

## ConfigFile Base Class

[`config/ConfigFile.java`](../src/main/java/com/hyperfactions/config/ConfigFile.java)

Base class providing common functionality:

```java
public abstract class ConfigFile {

    protected final Path path;
    protected JsonObject data;
    protected ValidationResult lastValidationResult;

    public void load() {
        if (Files.exists(path)) {
            // Load existing config
            data = parseJson(path);
        } else {
            // Create with defaults
            data = new JsonObject();
        }

        // Apply defaults for any missing keys
        applyDefaults();

        // Save (adds missing keys to file)
        save();
    }

    protected abstract void applyDefaults();

    public void validateAndLog() {
        lastValidationResult = validate();
        for (String warning : lastValidationResult.getWarnings()) {
            Logger.warn("[Config] %s: %s", path.getFileName(), warning);
        }
    }

    protected abstract ValidationResult validate();
}
```

## FactionsConfig Sections

[`config/FactionsConfig.java`](../src/main/java/com/hyperfactions/config/FactionsConfig.java) — `config/factions.json`

### roles

Custom display names and abbreviations for faction roles. These are presentation-only — the internal role names (`Leader`, `Officer`, `Member`) stored in data files are unchanged.

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `roles.leader.displayName` | string | `"Leader"` | Display name for the Leader role |
| `roles.leader.shortName` | string | `"LD"` | Short abbreviation (max 4 chars) |
| `roles.officer.displayName` | string | `"Officer"` | Display name for the Officer role |
| `roles.officer.shortName` | string | `"OF"` | Short abbreviation (max 4 chars) |
| `roles.member.displayName` | string | `"Member"` | Display name for the Member role |
| `roles.member.shortName` | string | `"MB"` | Short abbreviation (max 4 chars) |

**Example — Mafia theme:**
```json
{
  "roles": {
    "leader": { "displayName": "Boss", "shortName": "BO" },
    "officer": { "displayName": "Underboss", "shortName": "UB" },
    "member": { "displayName": "Soldier", "shortName": "SO" }
  }
}
```

All GUIs, commands, log messages, and the `factions_role_display` / `factions_role_short` placeholders use these configured names. Changes take effect immediately after `/f admin reload`.

### faction

Basic faction settings:

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `maxMembers` | int | 50 | Maximum members per faction |
| `maxNameLength` | int | 24 | Maximum faction name length |
| `minNameLength` | int | 3 | Minimum faction name length |
| `allowColors` | bool | true | Allow color codes in names |

### power

Power mechanics:

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `maxPlayerPower` | double | 20.0 | Maximum power per player |
| `startingPower` | double | 10.0 | Initial power for new players |
| `powerPerClaim` | double | 2.0 | Power cost per claim |
| `deathPenalty` | double | 1.0 | Power lost on death |
| `powerLossOnMobDeath` | bool | true | Apply death penalty for mob kills |
| `powerLossOnEnvironmentalDeath` | bool | true | Apply death penalty for fall/drowning/suffocation |
| `regenPerMinute` | double | 0.1 | Power regeneration rate |
| `regenWhenOffline` | bool | false | Regen while offline |
| `killRewardRequiresFaction` | bool | true | Only gain power from killing factioned players |
| `hardcoreMode` | bool | false | Shared faction power pool — deaths/kills affect the faction total directly, no per-death cap or floor |

### claims

Territory settings:

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `maxClaims` | int | 100 | Hard limit per faction |
| `onlyAdjacent` | bool | false | Require adjacent claims |
| `decayEnabled` | bool | true | Enable claim decay |
| `decayDaysInactive` | int | 30 | Days before decay starts |
| `worldWhitelist` | array | [] | Only these worlds allow claiming |
| `worldBlacklist` | array | [] | These worlds block claiming |
| `allowCreatureExplosions` | bool | false | Allow creature explosions (e.g. creepers) in claims |
| `allowBlockExplosions` | bool | false | Allow block-based explosions (e.g. TNT) in claims |
| `allowOtherExplosions` | bool | false | Allow other explosion types in claims |
| `protectContainers` | bool | true | Protect containers (chests, barrels) in claims |
| `protectRedstone` | bool | true | Protect redstone components in claims |
| `protectDoors` | bool | true | Protect doors/gates in claims |
| `protectFire` | bool | true | Prevent fire spread in claims |
| `protectFrostWalker` | bool | true | Prevent frost walker in enemy claims |
| `protectPistons` | bool | true | Prevent pistons pushing into claims |
| `protectFluids` | bool | true | Prevent fluid flow into claims |
| `protectEntityInteract` | bool | true | Protect entity interactions (armor stands, item frames) |
| `protectVehicles` | bool | true | Protect vehicles (boats, minecarts) in claims |

> **Migration note:** The old `allowExplosionsInClaims` boolean has been replaced by three granular flags: `allowCreatureExplosions`, `allowBlockExplosions`, and `allowOtherExplosions`.

### combat

Combat settings:

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `tagDurationSeconds` | int | 15 | Combat tag duration |
| `allyDamage` | bool | false | Allow ally damage |
| `factionDamage` | bool | false | Allow faction damage |
| `taggedLogoutPenalty` | bool | true | Punish combat logout |
| `spawnProtection.enabled` | bool | true | Enable spawn protection |
| `spawnProtection.durationSeconds` | int | 5 | Protection duration |

## ServerConfig Sections

[`config/ServerConfig.java`](../src/main/java/com/hyperfactions/config/ServerConfig.java) — `config/server.json`

### teleport

Teleportation settings:

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `warmupSeconds` | int | 5 | Warmup before teleport |
| `cooldownSeconds` | int | 300 | Cooldown between teleports |
| `cancelOnMove` | bool | true | Cancel on movement |
| `cancelOnDamage` | bool | true | Cancel on damage |
| `stuckMinRadius` | int | 5 | Minimum search radius for `/f stuck` (chunks) |
| `stuckRadiusIncrease` | int | 5 | Radius increase per failed attempt |
| `stuckMaxAttempts` | int | 6 | Maximum search attempts before giving up |

### permissions

Permission behavior:

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `adminRequiresOp` | bool | true | Admin commands require OP |
| `fallbackBehavior` | string | "deny" | Default when no provider |

### updates.hyperProtect

[HyperProtect-Mixin](https://www.curseforge.com/hytale/bootstrap/hyperprotect-mixin) lifecycle management:

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `autoDownload` | bool | false | Auto-download HP-Mixin to `earlyplugins/` if not installed |
| `autoUpdate` | bool | true | Check for HP-Mixin updates on startup, notify admins |
| `url` | string | GitHub Releases API | API endpoint for version checking |

When `autoDownload` is disabled and HP-Mixin is not installed, the server logs install instructions. Use `/f admin update mixin` for manual install/update regardless of config. Use `/f admin update toggle-mixin-download` to toggle auto-download at runtime (persisted).

### gui

GUI behavior settings:

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `leaderboardKdRefreshSeconds` | int | 300 | Background cache refresh interval for aggregated faction K/D ratios on the leaderboard page |

## Module Configs

### BackupConfig

[`config/modules/BackupConfig.java`](../src/main/java/com/hyperfactions/config/modules/BackupConfig.java)

GFS (Grandfather-Father-Son) backup system:

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `enabled` | bool | true | Enable automatic backups |
| `hourlyRetention` | int | 24 | Hourly backups to keep |
| `dailyRetention` | int | 7 | Daily backups to keep |
| `weeklyRetention` | int | 4 | Weekly backups to keep |
| `manualRetention` | int | 10 | Manual backups to keep |
| `onShutdown` | bool | true | Backup on server shutdown |

### ChatConfig

[`config/modules/ChatConfig.java`](../src/main/java/com/hyperfactions/config/modules/ChatConfig.java)

Chat formatting:

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `enabled` | bool | true | Enable chat formatting |
| `format` | string | `"{faction_tag}..."` | Chat format template |
| `tagDisplay` | string | "tag" | Tag display mode |
| `tagFormat` | string | `"[{tag}] "` | Tag format |
| `priority` | string | "LATE" | Event priority |
| `relationColors.own` | string | "#00FF00" | Own faction color |
| `relationColors.ally` | string | "#FF69B4" | Ally color |
| `relationColors.neutral` | string | "#AAAAAA" | Neutral color |
| `relationColors.enemy` | string | "#FF0000" | Enemy color |

### DebugConfig

[`config/modules/DebugConfig.java`](../src/main/java/com/hyperfactions/config/modules/DebugConfig.java)

Debug logging:

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `enabledByDefault` | bool | false | Enable all categories |
| `logToConsole` | bool | true | Output to console |
| `categories.power` | bool | false | Power system debug |
| `categories.claim` | bool | false | Claim system debug |
| `categories.combat` | bool | false | Combat system debug |
| `categories.protection` | bool | false | Protection debug |
| `categories.relation` | bool | false | Relation debug |
| `categories.territory` | bool | false | Territory debug |

### FactionPermissionsConfig

[`config/modules/FactionPermissionsConfig.java`](../src/main/java/com/hyperfactions/config/modules/FactionPermissionsConfig.java)

Territory permission defaults and locks:

```json
{
  "defaults": {
    "outsiderBreak": false,
    "outsiderPlace": false,
    "outsiderInteract": false,
    "outsiderCrateUse": false,
    "outsiderNpcTame": false,
    "allyBreak": false,
    "allyPlace": false,
    "allyInteract": true,
    "allyCrateUse": false,
    "allyNpcTame": false,
    "memberBreak": true,
    "memberPlace": true,
    "memberInteract": true,
    "memberCrateUse": true,
    "memberNpcTame": true,
    "officerCrateUse": true,
    "officerNpcTame": true,
    "pvpEnabled": true,
    "officersCanEdit": false,
    "treasuryDeposit": true,
    "treasuryWithdraw": false,
    "treasuryTransfer": false
  },
  "locks": {
    "pvpEnabled": false
  },
  "forced": {
    "pvpEnabled": true
  }
}
```

- **defaults** - Applied to new factions (includes `CrateUse`, `NpcTame`, and `treasury*` flags)
- **locks** - When true, factions cannot change this setting
- **forced** - Value used when a setting is locked

### WorldsConfig

[`config/modules/WorldsConfig.java`](../src/main/java/com/hyperfactions/config/modules/WorldsConfig.java)

Per-world behavior overrides in `config/worlds.json`:

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `enabled` | bool | true | Enable per-world settings module |
| `claimBlacklist` | array | [] | Worlds where claiming is unconditionally blocked |
| `worlds` | object | `{}` | Per-world setting overrides (keyed by world name or wildcard pattern) |

Per-world settings (4 per entry):

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `claiming` | bool | true | Whether claiming is allowed in this world |
| `powerLoss` | bool | true | Whether power loss applies in this world |
| `friendlyFireFaction` | bool | *(from global config)* | Same-faction PvP override |
| `friendlyFireAlly` | bool | *(from global config)* | Ally PvP override |

**Wildcard support**: Use `%` as a wildcard in world names (e.g., `arena_%` matches `arena_1`, `arena_pvp`). Priority resolution: exact name match > wildcard patterns (fewer wildcards = higher priority) > default policy.

**Default rules**: A default `instance-%` wildcard rule blocks claiming in temporary instance worlds.

**Example:**
```json
{
  "enabled": true,
  "claimBlacklist": ["lobby"],
  "worlds": {
    "arena_%": { "claiming": false, "powerLoss": false },
    "instance-%": { "claiming": false }
  }
}
```

Admin commands: `/f admin world list|info|set|reset`

## Validation System

[`config/ValidationResult.java`](../src/main/java/com/hyperfactions/config/ValidationResult.java)

```java
public class ValidationResult {

    private final List<String> warnings = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    public void addWarning(String message) {
        warnings.add(message);
    }

    public void addError(String message) {
        errors.add(message);
    }

    public boolean hasIssues() {
        return !warnings.isEmpty() || !errors.isEmpty();
    }

    public void merge(ValidationResult other) {
        warnings.addAll(other.warnings);
        errors.addAll(other.errors);
    }
}
```

Example validation in FactionsConfig:

```java
@Override
protected ValidationResult validate() {
    ValidationResult result = new ValidationResult();

    if (getMaxMembers() < 1) {
        result.addWarning("maxMembers must be at least 1, using 1");
        data.addProperty("faction.maxMembers", 1);
    }

    if (getDeathPenalty() < 0) {
        result.addWarning("deathPenalty cannot be negative, using 0");
        data.addProperty("power.deathPenalty", 0.0);
    }

    return result;
}
```

## Migration System

[`migration/MigrationRunner.java`](../src/main/java/com/hyperfactions/migration/MigrationRunner.java)

Config migrations run automatically on load:

```java
public static List<MigrationResult> runPendingMigrations(Path dataDir, MigrationType type) {
    List<Migration> migrations = getMigrationsForType(type);
    List<MigrationResult> results = new ArrayList<>();

    for (Migration migration : migrations) {
        if (migration.isNeeded(dataDir)) {
            MigrationResult result = migration.run(dataDir);
            results.add(result);
        }
    }

    return results;
}
```

Migrations handle:
- Renaming config keys
- Moving settings between files
- Adding new required keys
- Converting data formats

## Reload Behavior

When `/f admin reload` is called:

1. All config files are re-read from disk
2. Validation runs again
3. Managers receive updated values
4. Debug logging levels are reapplied

```java
public void reloadAll() {
    factionsConfig.reload();
    serverConfig.reload();
    backupConfig.reload();
    chatConfig.reload();
    debugConfig.reload();
    economyConfig.reload();
    factionPermissionsConfig.reload();

    validateAll();
}
```

## Default Config Generation

On first run, all config files are created with defaults:

1. `config/` directory created
2. `config/factions.json` created with faction gameplay defaults
3. `config/server.json` created with server behavior defaults (including `configVersion: 7`)
4. Module configs created with their defaults
5. All files are pretty-printed JSON

## Accessing Config Values

### Direct Access

```java
ConfigManager config = ConfigManager.get();

// Via convenience methods (most common)
int maxMembers = config.getMaxMembers();
double powerPerClaim = config.getPowerPerClaim();
boolean pvpEnabled = config.isFactionDamage();

// Via config object (for grouped access)
FactionsConfig factions = config.factions();
ServerConfig server = config.server();
BackupConfig backup = config.backup();
```

### In Managers

```java
public class ClaimManager {

    public ClaimResult claim(UUID playerUuid, String world, int chunkX, int chunkZ) {
        ConfigManager config = ConfigManager.get();

        // Check world whitelist/blacklist
        if (!config.isWorldAllowed(world)) {
            return ClaimResult.WORLD_BLACKLISTED;
        }

        // Check max claims
        int maxClaims = config.calculateMaxClaims(factionPower);
        if (currentClaims >= maxClaims) {
            return ClaimResult.MAX_CLAIMS_REACHED;
        }

        // ...
    }
}
```

## Adding New Config Options

1. **Add to appropriate config class** (FactionsConfig, ServerConfig, or a module):
   ```java
   public int getNewSetting() {
       return data.get("newSection").getAsJsonObject()
           .get("newSetting").getAsInt();
   }
   ```

2. **Add default value**:
   ```java
   @Override
   protected void applyDefaults() {
       // ...
       setDefault("newSection.newSetting", 42);
   }
   ```

3. **Add validation** (if needed):
   ```java
   if (getNewSetting() < 0) {
       result.addWarning("newSetting cannot be negative");
   }
   ```

4. **Add convenience method to ConfigManager** (optional):
   ```java
   public int getNewSetting() {
       return factionsConfig.getNewSetting();  // or serverConfig for server settings
   }
   ```

## Code Links

| Class | Path |
|-------|------|
| ConfigManager | [`config/ConfigManager.java`](../src/main/java/com/hyperfactions/config/ConfigManager.java) |
| ConfigFile | [`config/ConfigFile.java`](../src/main/java/com/hyperfactions/config/ConfigFile.java) |
| FactionsConfig | [`config/FactionsConfig.java`](../src/main/java/com/hyperfactions/config/FactionsConfig.java) |
| ServerConfig | [`config/ServerConfig.java`](../src/main/java/com/hyperfactions/config/ServerConfig.java) |
| CoreConfig | [`config/CoreConfig.java`](../src/main/java/com/hyperfactions/config/CoreConfig.java) *(deprecated)* |
| ModuleConfig | [`config/ModuleConfig.java`](../src/main/java/com/hyperfactions/config/ModuleConfig.java) |
| ValidationResult | [`config/ValidationResult.java`](../src/main/java/com/hyperfactions/config/ValidationResult.java) |
| BackupConfig | [`config/modules/BackupConfig.java`](../src/main/java/com/hyperfactions/config/modules/BackupConfig.java) |
| ChatConfig | [`config/modules/ChatConfig.java`](../src/main/java/com/hyperfactions/config/modules/ChatConfig.java) |
| DebugConfig | [`config/modules/DebugConfig.java`](../src/main/java/com/hyperfactions/config/modules/DebugConfig.java) |
| EconomyConfig | [`config/modules/EconomyConfig.java`](../src/main/java/com/hyperfactions/config/modules/EconomyConfig.java) |
| FactionPermissionsConfig | [`config/modules/FactionPermissionsConfig.java`](../src/main/java/com/hyperfactions/config/modules/FactionPermissionsConfig.java) |
