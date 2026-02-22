# HyperFactions Config System

> **Version**: 0.8.2 | **Config version**: 5 | **8 module configs**

Architecture documentation for the HyperFactions configuration system.

## Overview

HyperFactions uses a modular JSON-based configuration system with:

- **ConfigManager** - Central coordinator for all config files
- **CoreConfig** - Main `config.json` with core settings
- **Module Configs** - 8 feature-specific configs in `config/` subdirectory
- **Validation** - Automatic validation with warnings and auto-correction
- **Migration** - Automatic config migration (v1→v2→v3→v4→v5) with backup/rollback

## Architecture

```
ConfigManager (singleton)
     │
     ├─► CoreConfig (config.json, configVersion: 5)
     │        │
     │        └─► Faction, Power, Claims, Combat, Relations,
     │            Invites, Teleport, Updates, AutoSave, Messages, GUI
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
              └─► WorldMapConfig (worldmap.json)
```

## File Structure

```
<server>/mods/com.hyperfactions_HyperFactions/
├── config.json                    # Core configuration (v5)
├── config/                        # Module configs
│   ├── backup.json
│   ├── chat.json
│   ├── debug.json
│   ├── economy.json
│   ├── faction-permissions.json
│   ├── announcements.json         # Event broadcast toggles
│   ├── gravestones.json           # Gravestone integration settings
│   └── worldmap.json              # World map refresh modes
├── factions/                      # Faction data (see storage.md)
├── players/                       # Player data (see storage.md)
├── backups/                       # Backup archives (see storage.md)
└── zones.json                     # Zone data (see storage.md)
```

## Config Migration

Configuration is automatically migrated on startup. See [Data Import & Migration](data-import.md#config-migration-system) for the full migration chain (v1→v2→v3→v4→v5).

## Key Classes

| Class | Path | Purpose |
|-------|------|---------|
| ConfigManager | [`config/ConfigManager.java`](../src/main/java/com/hyperfactions/config/ConfigManager.java) | Singleton coordinator |
| ConfigFile | [`config/ConfigFile.java`](../src/main/java/com/hyperfactions/config/ConfigFile.java) | Base class for config files |
| CoreConfig | [`config/CoreConfig.java`](../src/main/java/com/hyperfactions/config/CoreConfig.java) | Main config.json |
| ModuleConfig | [`config/ModuleConfig.java`](../src/main/java/com/hyperfactions/config/ModuleConfig.java) | Base for module configs |
| ValidationResult | [`config/ValidationResult.java`](../src/main/java/com/hyperfactions/config/ValidationResult.java) | Validation tracking |

## ConfigManager

[`config/ConfigManager.java`](../src/main/java/com/hyperfactions/config/ConfigManager.java)

Singleton that orchestrates all configuration:

```java
public class ConfigManager {

    private static ConfigManager instance;

    private Path dataDir;
    private CoreConfig coreConfig;
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

        // 1. Run pending migrations
        runMigrations();

        // 2. Load core config
        coreConfig = new CoreConfig(dataDir.resolve("config.json"));
        coreConfig.load();

        // 3. Load module configs
        Path configDir = dataDir.resolve("config");
        backupConfig = new BackupConfig(configDir.resolve("backup.json"));
        chatConfig = new ChatConfig(configDir.resolve("chat.json"));
        // ... etc

        // 4. Validate all configs
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
CoreConfig core = config.core();
BackupConfig backup = config.backup();
```

## Config Loading Process

```
1. ConfigManager.loadAll(dataDir)
        │
        ▼
2. Run pending migrations (MigrationRunner)
        │
        ▼
3. Load CoreConfig
   - Read config.json
   - Apply defaults for missing keys
   - Save with new keys added
        │
        ▼
4. Load Module Configs (same process)
        │
        ▼
5. Validate all configs
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

## Core Config Sections

[`config/CoreConfig.java`](../src/main/java/com/hyperfactions/config/CoreConfig.java)

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
    "allyBreak": false,
    "allyPlace": false,
    "allyInteract": true,
    "memberBreak": true,
    "memberPlace": true,
    "memberInteract": true,
    "pvpEnabled": true,
    "officersCanEdit": false
  },
  "locks": {
    "pvpEnabled": false
  },
  "forced": {
    "pvpEnabled": true
  }
}
```

- **defaults** - Applied to new factions
- **locks** - When true, factions cannot change this setting
- **forced** - Value used when a setting is locked

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

Example validation in CoreConfig:

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
    coreConfig.reload();
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

1. `config.json` created with all core settings
2. `config/` directory created
3. Module configs created with their defaults
4. All files are pretty-printed JSON

## Accessing Config Values

### Direct Access

```java
ConfigManager config = ConfigManager.get();

// Via convenience methods (most common)
int maxMembers = config.getMaxMembers();
double powerPerClaim = config.getPowerPerClaim();
boolean pvpEnabled = config.isFactionDamage();

// Via config object (for grouped access)
CoreConfig core = config.core();
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

1. **Add to appropriate config class** (CoreConfig or a module):
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
       return coreConfig.getNewSetting();
   }
   ```

## Code Links

| Class | Path |
|-------|------|
| ConfigManager | [`config/ConfigManager.java`](../src/main/java/com/hyperfactions/config/ConfigManager.java) |
| ConfigFile | [`config/ConfigFile.java`](../src/main/java/com/hyperfactions/config/ConfigFile.java) |
| CoreConfig | [`config/CoreConfig.java`](../src/main/java/com/hyperfactions/config/CoreConfig.java) |
| ModuleConfig | [`config/ModuleConfig.java`](../src/main/java/com/hyperfactions/config/ModuleConfig.java) |
| ValidationResult | [`config/ValidationResult.java`](../src/main/java/com/hyperfactions/config/ValidationResult.java) |
| BackupConfig | [`config/modules/BackupConfig.java`](../src/main/java/com/hyperfactions/config/modules/BackupConfig.java) |
| ChatConfig | [`config/modules/ChatConfig.java`](../src/main/java/com/hyperfactions/config/modules/ChatConfig.java) |
| DebugConfig | [`config/modules/DebugConfig.java`](../src/main/java/com/hyperfactions/config/modules/DebugConfig.java) |
| EconomyConfig | [`config/modules/EconomyConfig.java`](../src/main/java/com/hyperfactions/config/modules/EconomyConfig.java) |
| FactionPermissionsConfig | [`config/modules/FactionPermissionsConfig.java`](../src/main/java/com/hyperfactions/config/modules/FactionPermissionsConfig.java) |
