# HyperFactions Config System

> **Version**: 0.13.0 | **Config version**: 8 | **11 config files**

Architecture documentation for the HyperFactions configuration system.

## Overview

HyperFactions uses a modular JSON-based configuration system with:

- **ConfigManager** - Central coordinator for all config files
- **FactionsConfig** - Faction gameplay settings in `config/factions.json`
- **ServerConfig** - Server behavior settings in `config/server.json` (includes `configVersion`)
- **Module Configs** - 8 feature-specific configs in `config/` subdirectory
- **Validation** - Automatic validation with warnings and auto-correction
- **Migration** - Automatic config migration (v1→v2→v3→v4→v5→v6→v7→v8) with backup/rollback

> **Note:** `CoreConfig` and `config.json` are deprecated. The V5→V6 migration splits `config.json` into `config/factions.json` and `config/server.json`, then deletes `config.json`. New installs create only the split files. If migration fails, the plugin falls back to loading from the legacy `config.json`.

## Architecture

```
ConfigManager (singleton)
     │
     ├─► FactionsConfig (config/factions.json)
     │        │
     │        └─► Roles, Faction, Power, Claims, Combat, Relations, Invites, Stuck
     │
     ├─► ServerConfig (config/server.json, configVersion: 8)
     │        │
     │        └─► Teleport, AutoSave, Messages, GUI, Permissions, Language, MobClearing, Updates
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
│   ├── server.json                # Server behavior (teleport, autoSave, messages, gui, permissions, language, mobClearing, updates, configVersion)
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

Configuration is automatically migrated on startup. See [Data Import & Migration](data-import.md#config-migration-system) for the full migration chain (v1→v2→v3→v4→v5→v6→v7→v8).

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

### V7→V8: Config Editor & Localization

The V7→V8 migration adds fields required by the runtime config editor and localization system:

1. **Adds** localization configuration fields to `config/server.json` (default locale, player language detection settings)
2. **Adds** any missing config keys required by the admin GUI config editor
3. **Sets** `configVersion` to 8 in `config/server.json`

## Key Classes

| Class | Path | Purpose |
|-------|------|---------|
| ConfigManager | [`config/ConfigManager.java`](../src/main/java/com/hyperfactions/config/ConfigManager.java) | Singleton coordinator |
| ConfigFile | [`config/ConfigFile.java`](../src/main/java/com/hyperfactions/config/ConfigFile.java) | Base class for config files |
| FactionsConfig | [`config/modules/FactionsConfig.java`](../src/main/java/com/hyperfactions/config/modules/FactionsConfig.java) | Faction gameplay (`config/factions.json`) |
| ServerConfig | [`config/modules/ServerConfig.java`](../src/main/java/com/hyperfactions/config/modules/ServerConfig.java) | Server behavior (`config/server.json`) |
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

[`config/modules/FactionsConfig.java`](../src/main/java/com/hyperfactions/config/modules/FactionsConfig.java) — `config/factions.json`

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
| `faction.maxMembers` | int | 50 | Maximum members per faction |
| `faction.maxMembershipHistory` | int | 10 | Maximum membership history entries tracked per faction |
| `faction.maxNameLength` | int | 24 | Maximum faction name length (validated: 1-64) |
| `faction.minNameLength` | int | 3 | Minimum faction name length (validated: 1-maxNameLength) |
| `faction.allowColors` | bool | true | Allow color codes in faction names |

### power

Power mechanics:

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `power.maxPlayerPower` | double | 20.0 | Maximum power per player |
| `power.startingPower` | double | 10.0 | Initial power for new players (validated: 0-maxPlayerPower) |
| `power.powerPerClaim` | double | 2.0 | Power cost per claim |
| `power.deathPenalty` | double | 1.0 | Power lost on death |
| `power.killReward` | double | 0.0 | Power gained on killing another player |
| `power.killRewardRequiresFaction` | bool | true | Only gain power from killing factioned players |
| `power.powerLossOnMobDeath` | bool | true | Apply death penalty for mob kills |
| `power.powerLossOnEnvironmentalDeath` | bool | true | Apply death penalty for fall/drowning/suffocation |
| `power.regenPerMinute` | double | 0.1 | Power regeneration rate |
| `power.regenWhenOffline` | bool | false | Regen while offline |
| `power.hardcoreMode` | bool | false | Shared faction power pool — deaths/kills affect the faction total directly, no per-death cap or floor |

### claims

Territory settings:

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `claims.maxClaims` | int | 100 | Global hard limit per faction (can be overridden per-world via `worlds.json`) |
| `claims.onlyAdjacent` | bool | false | Require adjacent claims |
| `claims.preventDisconnect` | bool | false | Prevent unclaiming if it would disconnect remaining claims |
| `claims.decayEnabled` | bool | true | Enable claim decay |
| `claims.decayDaysInactive` | int | 30 | Days before decay starts |
| `claims.decayClaimsPerCycle` | int | 5 | Number of claims removed per decay cycle |
| `claims.worldWhitelist` | array | [] | Only these worlds allow claiming |
| `claims.worldBlacklist` | array | [] | These worlds block claiming |
| `claims.outsiderPickupAllowed` | bool | true | Allow outsiders to pick up items in claimed territory |
| `claims.outsiderDropAllowed` | bool | true | Allow outsiders to drop items in claimed territory |
| `claims.factionlessExplosionsAllowed` | bool | false | Allow explosions in claims when the source has no faction |
| `claims.enemyExplosionsAllowed` | bool | false | Allow explosions in claims caused by enemy faction members |
| `claims.neutralExplosionsAllowed` | bool | false | Allow explosions in claims caused by neutral faction members |
| `claims.fireSpreadAllowed` | bool | true | Allow fire to spread within claimed territory |
| `claims.factionlessDamageAllowed` | bool | true | Allow factionless players to deal damage to entities in claims |
| `claims.enemyDamageAllowed` | bool | true | Allow enemy faction members to deal damage to entities in claims |
| `claims.neutralDamageAllowed` | bool | true | Allow neutral faction members to deal damage to entities in claims |

> **Migration note:** The old `allowExplosionsInClaims` boolean has been replaced by three granular explosion flags: `factionlessExplosionsAllowed`, `enemyExplosionsAllowed`, and `neutralExplosionsAllowed`. The old `allowCreatureExplosions`, `allowBlockExplosions`, and `allowOtherExplosions` flags are no longer present — explosions are now controlled by faction relationship.

### combat

Combat settings:

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `combat.tagDurationSeconds` | int | 15 | Combat tag duration |
| `combat.allyDamage` | bool | false | Allow ally damage |
| `combat.factionDamage` | bool | false | Allow faction damage |
| `combat.taggedLogoutPenalty` | bool | true | Punish combat logout |
| `combat.logoutPowerLoss` | double | 1.0 | Power lost when logging out while combat tagged |
| `combat.neutralAttackPenalty` | double | 0.0 | Power penalty for attacking a neutral player |
| `combat.spawnProtection.enabled` | bool | true | Enable spawn protection |
| `combat.spawnProtection.durationSeconds` | int | 5 | Protection duration |
| `combat.spawnProtection.breakOnAttack` | bool | true | Cancel spawn protection when the player attacks |
| `combat.spawnProtection.breakOnMove` | bool | true | Cancel spawn protection when the player moves |

### relations

Relation settings:

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `relations.maxAllies` | int | 10 | Maximum allied factions (-1 = unlimited) |
| `relations.maxEnemies` | int | -1 | Maximum enemy factions (-1 = unlimited) |

### invites

Invite and join request settings:

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `invites.inviteExpirationMinutes` | int | 5 | Minutes before a faction invite expires |
| `invites.joinRequestExpirationHours` | int | 24 | Hours before a join request expires |

### stuck

The `/f stuck` command teleports players out of enemy territory when trapped. The command searches for a safe location in expanding rings.

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `stuck.minRadius` | int | 3 | Minimum search radius (chunks) |
| `stuck.radiusIncrease` | int | 3 | Radius increase per failed attempt (chunks) |
| `stuck.maxAttempts` | int | 10 | Maximum search attempts before giving up |
| `stuck.warmupSeconds` | int | 30 | Warmup delay before teleport (cancelled by movement/damage) |
| `stuck.cooldownSeconds` | int | 300 | Cooldown between `/f stuck` uses |

**Default factions.json:**
```json
{
  "roles": {
    "leader": { "displayName": "Leader", "shortName": "LD" },
    "officer": { "displayName": "Officer", "shortName": "OF" },
    "member": { "displayName": "Member", "shortName": "MB" }
  },
  "faction": {
    "maxMembers": 50,
    "maxMembershipHistory": 10,
    "maxNameLength": 24,
    "minNameLength": 3,
    "allowColors": true
  },
  "power": {
    "maxPlayerPower": 20.0,
    "startingPower": 10.0,
    "powerPerClaim": 2.0,
    "deathPenalty": 1.0,
    "killReward": 0.0,
    "killRewardRequiresFaction": true,
    "powerLossOnMobDeath": true,
    "powerLossOnEnvironmentalDeath": true,
    "regenPerMinute": 0.1,
    "regenWhenOffline": false,
    "hardcoreMode": false
  },
  "claims": {
    "maxClaims": 100,
    "onlyAdjacent": false,
    "preventDisconnect": false,
    "decayEnabled": true,
    "decayDaysInactive": 30,
    "decayClaimsPerCycle": 5,
    "worldWhitelist": [],
    "worldBlacklist": [],
    "outsiderPickupAllowed": true,
    "outsiderDropAllowed": true,
    "factionlessExplosionsAllowed": false,
    "enemyExplosionsAllowed": false,
    "neutralExplosionsAllowed": false,
    "fireSpreadAllowed": true,
    "factionlessDamageAllowed": true,
    "enemyDamageAllowed": true,
    "neutralDamageAllowed": true
  },
  "combat": {
    "tagDurationSeconds": 15,
    "allyDamage": false,
    "factionDamage": false,
    "taggedLogoutPenalty": true,
    "logoutPowerLoss": 1.0,
    "neutralAttackPenalty": 0.0,
    "spawnProtection": {
      "enabled": true,
      "durationSeconds": 5,
      "breakOnAttack": true,
      "breakOnMove": true
    }
  },
  "relations": {
    "maxAllies": 10,
    "maxEnemies": -1
  },
  "invites": {
    "inviteExpirationMinutes": 5,
    "joinRequestExpirationHours": 24
  },
  "stuck": {
    "minRadius": 3,
    "radiusIncrease": 3,
    "maxAttempts": 10,
    "warmupSeconds": 30,
    "cooldownSeconds": 300
  }
}
```

## ServerConfig Sections

[`config/modules/ServerConfig.java`](../src/main/java/com/hyperfactions/config/modules/ServerConfig.java) — `config/server.json`

### teleport

Teleportation settings:

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `teleport.warmupSeconds` | int | 5 | Warmup before teleport |
| `teleport.cooldownSeconds` | int | 300 | Cooldown between teleports |
| `teleport.cancelOnMove` | bool | true | Cancel on movement |
| `teleport.cancelOnDamage` | bool | true | Cancel on damage |

### autoSave

Auto-save settings:

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `autoSave.enabled` | bool | true | Enable periodic auto-save |
| `autoSave.intervalMinutes` | int | 5 | Auto-save interval in minutes |

### messages

Message formatting settings:

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `messages.prefix.text` | string | `"HyperFactions"` | The text displayed inside the chat prefix brackets |
| `messages.prefix.color` | string | `"#55FFFF"` | Color of the prefix text |
| `messages.prefix.bracketColor` | string | `"#AAAAAA"` | Color of the bracket characters `[` and `]` |
| `messages.primaryColor` | string | `"#00FFFF"` | Primary accent color used throughout messages |

### gui

GUI behavior settings:

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `gui.title` | string | `"HyperFactions"` | Title displayed in the main GUI window |
| `gui.terrainMapEnabled` | bool | true | Enable terrain map rendering in the territory GUI |
| `gui.leaderboardKdRefreshSeconds` | int | 300 | Background cache refresh interval for aggregated faction K/D ratios on the leaderboard page |

### permissions

Permission behavior:

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `permissions.adminRequiresOp` | bool | true | Admin commands require OP |
| `permissions.allowWithoutPermissionMod` | bool | false | Allow all commands when no permission plugin is installed (if false, non-admin commands are denied without a permission plugin) |

### language

Localization / i18n settings:

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `language.default` | string | `"en-US"` | Default server language code |
| `language.usePlayerLanguage` | bool | true | Respect each player's client language for translations |

### mobClearing

Mob clearing settings (removes hostile mobs near claims periodically):

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `mobClearing.enabled` | bool | true | Enable periodic mob clearing |
| `mobClearing.intervalSeconds` | int | 10 | Sweep interval in seconds |

### updates

Update checking and HyperProtect-Mixin management:

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `updates.enabled` | bool | true | Enable update checking on startup |
| `updates.url` | string | GitHub Releases API | API endpoint for HyperFactions version checking |
| `updates.releaseChannel` | string | `"stable"` | Release channel: `"stable"` or `"prerelease"` |
| `updates.hyperProtect.autoDownload` | bool | false | Auto-download HP-Mixin to `earlyplugins/` if not installed |
| `updates.hyperProtect.autoUpdate` | bool | true | Check for HP-Mixin updates on startup, notify admins |
| `updates.hyperProtect.url` | string | GitHub Releases API | API endpoint for HP-Mixin version checking |

When `autoDownload` is disabled and HP-Mixin is not installed, the server logs install instructions. Use `/f admin update mixin` for manual install/update regardless of config. Use `/f admin update toggle-mixin-download` to toggle auto-download at runtime (persisted).

**Default server.json:**
```json
{
  "configVersion": 8,
  "teleport": {
    "warmupSeconds": 5,
    "cooldownSeconds": 300,
    "cancelOnMove": true,
    "cancelOnDamage": true
  },
  "autoSave": {
    "enabled": true,
    "intervalMinutes": 5
  },
  "messages": {
    "prefix": {
      "text": "HyperFactions",
      "color": "#55FFFF",
      "bracketColor": "#AAAAAA"
    },
    "primaryColor": "#00FFFF"
  },
  "gui": {
    "title": "HyperFactions",
    "terrainMapEnabled": true,
    "leaderboardKdRefreshSeconds": 300
  },
  "permissions": {
    "adminRequiresOp": true,
    "allowWithoutPermissionMod": false
  },
  "language": {
    "default": "en-US",
    "usePlayerLanguage": true
  },
  "mobClearing": {
    "enabled": true,
    "intervalSeconds": 10
  },
  "updates": {
    "enabled": true,
    "url": "https://api.github.com/repos/HyperSystems-Development/HyperFactions/releases/latest",
    "releaseChannel": "stable",
    "hyperProtect": {
      "autoDownload": false,
      "autoUpdate": true,
      "url": "https://api.github.com/repos/HyperSystems-Development/HyperProtect-Mixin/releases/latest"
    }
  }
}
```

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
| `manualRetention` | int | 10 | Manual backups to keep (0 = keep all) |
| `onShutdown` | bool | true | Backup on server shutdown |
| `shutdownRetention` | int | 5 | Shutdown backups to keep (0 = keep all) |

### ChatConfig

[`config/modules/ChatConfig.java`](../src/main/java/com/hyperfactions/config/modules/ChatConfig.java)

Chat formatting:

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `enabled` | bool | true | Enable chat formatting |
| `format` | string | `"{faction_tag}{prefix}{player}{suffix}: {message}"` | Chat format template |
| `tagDisplay` | string | `"tag"` | Tag display mode: `"tag"`, `"name"`, or `"none"` |
| `tagFormat` | string | `"[{tag}] "` | Tag format template |
| `noFactionTag` | string | `""` | Tag shown for players without a faction (empty = no tag) |
| `noFactionTagColor` | string | `"#555555"` | Color for the no-faction tag (dark gray) |
| `playerNameColor` | string | `"#FFFFFF"` | Color for {player} in public chat |
| `priority` | string | `"LATE"` | Event priority (`EARLIEST`, `EARLY`, `NORMAL`, `LATE`, `LATEST`) |
| `relationColors.own` | string | `"#00FF00"` | Own faction color (green) |
| `relationColors.ally` | string | `"#FF69B4"` | Ally color (pink) |
| `relationColors.neutral` | string | `"#AAAAAA"` | Neutral color (gray) |
| `relationColors.enemy` | string | `"#FF0000"` | Enemy color (red) |
| `factionChat.factionChatColor` | string | `"#00FFFF"` | Faction channel message color (cyan) |
| `factionChat.factionChatPrefix` | string | `"[Faction]"` | Prefix shown on faction chat messages |
| `factionChat.allyChatColor` | string | `"#AA00AA"` | Ally channel message color (purple) |
| `factionChat.allyChatPrefix` | string | `"[Ally]"` | Prefix shown on ally chat messages |
| `factionChat.senderNameColor` | string | `"#FFFF55"` | Sender name color in faction/ally chat (yellow) |
| `factionChat.messageColor` | string | `"#FFFFFF"` | Message text color in faction/ally chat (white) |
| `factionChat.historyEnabled` | bool | true | Enable faction chat history persistence |
| `factionChat.historyMaxMessages` | int | 200 | Maximum stored messages per faction (validated: 10-1000) |
| `factionChat.historyRetentionDays` | int | 7 | Days to retain chat history |
| `factionChat.historyCleanupIntervalMinutes` | int | 60 | Interval for automatic history cleanup (validated: min 5) |

### EconomyConfig

[`config/modules/EconomyConfig.java`](../src/main/java/com/hyperfactions/config/modules/EconomyConfig.java)

Faction treasury, currency display, fees, and upkeep:

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `currency.name` | string | `"dollar"` | Singular currency name |
| `currency.namePlural` | string | `"dollars"` | Plural currency name |
| `currency.symbol` | string | `"$"` | Currency symbol |
| `currency.symbolPosition` | string | `"left"` | Symbol placement: `"left"` (`$100.00`) or `"right"` (`100.00$`) |
| `treasury.startingBalance` | decimal | 0 | Starting balance for new factions |
| `treasury.disbandRefundToLeader` | bool | true | Refund balance to leader on disband |
| `treasury.limits.maxWithdrawAmount` | decimal | 0 | Per-transaction withdraw limit (0 = unlimited) |
| `treasury.limits.maxWithdrawPerPeriod` | decimal | 0 | Cumulative withdraw limit per period (0 = unlimited) |
| `treasury.limits.maxTransferAmount` | decimal | 0 | Per-transaction transfer limit (0 = unlimited) |
| `treasury.limits.maxTransferPerPeriod` | decimal | 0 | Cumulative transfer limit per period (0 = unlimited) |
| `treasury.limits.periodHours` | int | 24 | Rolling window for cumulative limits |
| `fees.depositPercent` | decimal | 0 | Deposit fee percentage (0-100) |
| `fees.withdrawPercent` | decimal | 0 | Withdrawal fee percentage (0-100) |
| `fees.transferPercent` | decimal | 0 | Transfer fee percentage (0-100) |
| `upkeep.enabled` | bool | true | Enable territory upkeep costs |
| `upkeep.costPerChunk` | decimal | 2.0 | Cost per chunk per cycle (flat mode) |
| `upkeep.intervalHours` | int | 24 | Collection interval |
| `upkeep.gracePeriodHours` | int | 48 | Grace period before claim forfeiture |
| `upkeep.autoPayDefault` | bool | true | Default auto-pay for new factions |
| `upkeep.freeChunks` | int | 3 | Chunks exempt from upkeep |
| `upkeep.claimLossPerCycle` | int | 1 | Claims lost per failed cycle after grace |
| `upkeep.warningHours` | int | 6 | Hours before collection to warn members |
| `upkeep.maxCostCap` | decimal | 0 | Max cost per cycle (0 = unlimited) |
| `upkeep.scalingMode` | string | `"flat"` | `"flat"` or `"progressive"` tiered pricing |
| `upkeep.scalingTiers` | array | see below | Progressive tier definitions |

**Scaling tiers** (when `scalingMode` is `"progressive"`):
```json
[
  { "chunkCount": 10, "costPerChunk": "2.00" },
  { "chunkCount": 15, "costPerChunk": "3.00" },
  { "chunkCount": 0, "costPerChunk": "5.00" }
]
```
A `chunkCount` of `0` means "all remaining chunks".

### DebugConfig

[`config/modules/DebugConfig.java`](../src/main/java/com/hyperfactions/config/modules/DebugConfig.java)

Debug logging:

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `enabled` | bool | false | Enable the debug module |
| `enabledByDefault` | bool | false | Enable all categories by default |
| `logToConsole` | bool | true | Output debug messages to console |
| `categories.power` | bool | false | Power system debug |
| `categories.claim` | bool | false | Claim system debug |
| `categories.combat` | bool | false | Combat system debug |
| `categories.protection` | bool | false | Protection debug |
| `categories.relation` | bool | false | Relation debug |
| `categories.territory` | bool | false | Territory debug |
| `categories.worldmap` | bool | false | World map debug |
| `categories.interaction` | bool | false | Block/entity interaction debug |
| `categories.mixin` | bool | false | HyperProtect-Mixin debug |
| `categories.spawning` | bool | false | Mob spawning debug |
| `categories.integration` | bool | false | Third-party integration debug |
| `categories.economy` | bool | false | Economy/treasury debug |
| `sentry.enabled` | bool | true | Enable Sentry error tracking |
| `sentry.dsn` | string | *(built-in)* | Sentry DSN (Data Source Name) URL |
| `sentry.environment` | string | `"production"` | Environment name sent to Sentry (e.g. "production", "development") |
| `sentry.debug` | bool | false | Enable Sentry debug logging |
| `sentry.tracesSampleRate` | double | 0.0 | Performance trace sample rate (0.0 = none, 1.0 = all) |

**Default debug.json:**
```json
{
  "enabled": false,
  "enabledByDefault": false,
  "logToConsole": true,
  "categories": {
    "power": false,
    "claim": false,
    "combat": false,
    "protection": false,
    "relation": false,
    "territory": false,
    "worldmap": false,
    "interaction": false,
    "mixin": false,
    "spawning": false,
    "integration": false,
    "economy": false
  },
  "sentry": {
    "enabled": true,
    "dsn": "https://...",
    "environment": "production",
    "debug": false,
    "tracesSampleRate": 0.0
  }
}
```

> **Note:** If a legacy `config/sentry.json` file exists, its settings are automatically migrated into the `sentry` section of `debug.json` and the old file is deleted.

### AnnouncementConfig

[`config/modules/AnnouncementConfig.java`](../src/main/java/com/hyperfactions/config/modules/AnnouncementConfig.java)

Server-wide faction event broadcast toggles, per-event colors, and territory notification customization:

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `enabled` | bool | true | Enable the announcement module |
| `events.factionCreated` | bool | true | Announce when a faction is created |
| `events.factionDisbanded` | bool | true | Announce when a faction is disbanded |
| `events.leadershipTransfer` | bool | true | Announce leadership transfers |
| `events.overclaim` | bool | true | Announce territory overclaims |
| `events.warDeclared` | bool | true | Announce war declarations |
| `events.allianceFormed` | bool | true | Announce new alliances |
| `events.allianceBroken` | bool | true | Announce broken alliances |
| `colors.factionCreated` | string | `"#55FF55"` | Color for faction creation announcements |
| `colors.factionDisbanded` | string | `"#FF5555"` | Color for faction disband announcements |
| `colors.leadershipTransfer` | string | `"#FFAA00"` | Color for leadership transfer announcements |
| `colors.overclaim` | string | `"#FF5555"` | Color for overclaim announcements |
| `colors.warDeclared` | string | `"#FF5555"` | Color for war declaration announcements |
| `colors.allianceFormed` | string | `"#55FF55"` | Color for alliance formed announcements |
| `colors.allianceBroken` | string | `"#FFAA00"` | Color for alliance broken announcements |
| `territoryNotifications.enabled` | bool | true | Enable territory enter/leave title notifications |
| `territoryNotifications.wilderness.onLeaveZone.enabled` | bool | true | Show wilderness notification when leaving a zone (safe/war) |
| `territoryNotifications.wilderness.onLeaveZone.upper` | string | `""` | Upper title text when leaving a zone to wilderness |
| `territoryNotifications.wilderness.onLeaveZone.lower` | string | `"Wilderness"` | Lower subtitle text when leaving a zone to wilderness |
| `territoryNotifications.wilderness.onLeaveClaim.enabled` | bool | true | Show wilderness notification when leaving a faction claim |
| `territoryNotifications.wilderness.onLeaveClaim.upper` | string | `""` | Upper title text when leaving a claim to wilderness |
| `territoryNotifications.wilderness.onLeaveClaim.lower` | string | `"Wilderness"` | Lower subtitle text when leaving a claim to wilderness |

**Default announcements.json:**
```json
{
  "enabled": true,
  "events": {
    "factionCreated": true,
    "factionDisbanded": true,
    "leadershipTransfer": true,
    "overclaim": true,
    "warDeclared": true,
    "allianceFormed": true,
    "allianceBroken": true
  },
  "colors": {
    "factionCreated": "#55FF55",
    "factionDisbanded": "#FF5555",
    "leadershipTransfer": "#FFAA00",
    "overclaim": "#FF5555",
    "warDeclared": "#FF5555",
    "allianceFormed": "#55FF55",
    "allianceBroken": "#FFAA00"
  },
  "territoryNotifications": {
    "enabled": true,
    "wilderness": {
      "onLeaveZone": {
        "enabled": true,
        "upper": "",
        "lower": "Wilderness"
      },
      "onLeaveClaim": {
        "enabled": true,
        "upper": "",
        "lower": "Wilderness"
      }
    }
  }
}
```

### GravestoneConfig

[`config/modules/GravestoneConfig.java`](../src/main/java/com/hyperfactions/config/modules/GravestoneConfig.java)

Faction-aware gravestone access rules per zone type. Controls how the GravestonePlugin integration interacts with faction territory protection.

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `enabled` | bool | true | Enable gravestone integration |
| `protectInOwnTerritory` | bool | true | Protect gravestones in own faction territory |
| `factionMembersCanAccess` | bool | true | Allow faction members to access each other's gravestones |
| `alliesCanAccess` | bool | false | Allow allied faction members to access gravestones |
| `protectInSafeZone` | bool | true | Protect gravestones in safe zones |
| `protectInWarZone` | bool | false | Protect gravestones in war zones |
| `protectInWilderness` | bool | false | Protect gravestones in wilderness |
| `announceDeathLocation` | bool | true | Announce death location to faction members |
| `protectInEnemyTerritory` | bool | false | Protect gravestones in enemy territory |
| `protectInNeutralTerritory` | bool | true | Protect gravestones in neutral territory |
| `enemiesCanLootInOwnTerritory` | bool | false | Allow enemies to loot gravestones in your territory |
| `allowLootDuringRaid` | bool | true | Allow gravestone looting during raids (placeholder — not enforced until raid system is implemented) |
| `allowLootDuringWar` | bool | true | Allow gravestone looting during wars (placeholder — not enforced until war system is implemented) |

**Default gravestones.json:**
```json
{
  "enabled": true,
  "protectInOwnTerritory": true,
  "factionMembersCanAccess": true,
  "alliesCanAccess": false,
  "protectInSafeZone": true,
  "protectInWarZone": false,
  "protectInWilderness": false,
  "announceDeathLocation": true,
  "protectInEnemyTerritory": false,
  "protectInNeutralTerritory": true,
  "enemiesCanLootInOwnTerritory": false,
  "allowLootDuringRaid": true,
  "allowLootDuringWar": true
}
```

### WorldMapConfig

[`config/modules/WorldMapConfig.java`](../src/main/java/com/hyperfactions/config/modules/WorldMapConfig.java)

World map integration controls how claim overlays are rendered on the in-game world map, with multiple refresh modes to balance performance vs. responsiveness.

**Refresh modes:**

| Mode | Description |
|------|-------------|
| `proximity` | Only refresh for players within range of claim changes. Most performant. |
| `incremental` | Refresh specific chunks for all players. Good balance of performance and consistency. **(default)** |
| `debounced` | Full map refresh after a quiet period with no changes. Use if incremental causes issues. |
| `immediate` | Full map refresh on every claim change. Original behavior, not recommended for busy servers. |
| `manual` | No automatic refresh. Use `/f admin map refresh` to update manually. |

**Top-level settings:**

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `enabled` | bool | true | Enable world map integration |
| `refreshMode` | string | `"incremental"` | Refresh mode (see table above) |
| `autoFallbackOnError` | bool | true | Auto-fall back to debounced mode if reflection errors occur |
| `showFactionTags` | bool | true | Show faction tag text on claimed chunks |
| `factionWideRefreshThreshold` | int | 200 | If a faction has more claims than this, use full refresh instead of queuing each chunk |
| `respectWorldConfig` | bool | true | Inherit map settings from world config; disabled worlds are skipped |
| `betterMapCompat` | string | `"auto"` | BetterMap compatibility mode: `"auto"` (detect), `"always"` (force on), `"never"` (force off) |

**Proximity mode settings (`proximity.*`):**

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `proximity.chunkRadius` | int | 32 | Chunk radius for proximity refresh (validated: 1-128) |
| `proximity.batchIntervalTicks` | int | 30 | Ticks between batch processing (30 ticks = 1s at 30 TPS) |
| `proximity.maxChunksPerBatch` | int | 50 | Maximum chunks per batch (validated: 1-500) |

**Incremental mode settings (`incremental.*`):**

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `incremental.batchIntervalTicks` | int | 30 | Ticks between batch processing (30 ticks = 1s at 30 TPS) |
| `incremental.maxChunksPerBatch` | int | 50 | Maximum chunks per batch (validated: 1-500) |

**Debounced mode settings (`debounced.*`):**

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `debounced.delaySeconds` | int | 5 | Quiet period before triggering refresh (validated: 1-60) |

**Player visibility filtering (`playerVisibility.*`):**

Controls which players are visible on the world map and compass based on faction relations.

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `playerVisibility.enabled` | bool | true | Enable player visibility filtering (disabled = all players visible, vanilla behavior) |
| `playerVisibility.showOwnFaction` | bool | true | Show own faction members on the map |
| `playerVisibility.showAllies` | bool | true | Show allied faction members on the map |
| `playerVisibility.showNeutrals` | bool | false | Show neutral faction members on the map |
| `playerVisibility.showEnemies` | bool | false | Show enemy faction members on the map |
| `playerVisibility.showFactionlessPlayers` | bool | false | Show factionless players to faction members |
| `playerVisibility.showFactionlessToFactionless` | bool | true | Show factionless players to other factionless players |

**Settings overrides (`settingsOverrides.*`):**

Override map settings inherited from the world config. Remove a key or set to `null` to inherit from the world.

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `settingsOverrides.defaultScale` | float | null | Override default map zoom scale |
| `settingsOverrides.minScale` | float | null | Override minimum map zoom |
| `settingsOverrides.maxScale` | float | null | Override maximum map zoom |
| `settingsOverrides.imageScale` | float | null | Override map image scale |
| `settingsOverrides.allowTeleportToCoordinates` | bool | null | Override teleport-to-coordinates permission |
| `settingsOverrides.allowTeleportToMarkers` | bool | null | Override teleport-to-markers permission |
| `settingsOverrides.allowCreatingMapMarkers` | bool | null | Override map marker creation permission |

### FactionPermissionsConfig

[`config/modules/FactionPermissionsConfig.java`](../src/main/java/com/hyperfactions/config/modules/FactionPermissionsConfig.java)

Territory permission defaults and locks. Uses a nested JSON format grouped by role level for readability. Also supports the legacy flat format (e.g. `outsiderBreak`) for backward compatibility.

**Two-section design:**
- **defaults** - Default values for new factions AND the forced value when locked
- **locks** - Whether each flag is locked (factions cannot change it)

When a flag is locked, its effective value is always the defaults value.

**Per-level flags** (applied for each level: `outsider`, `ally`, `member`, `officer`):

| Suffix | Description | Outsider Default | Ally Default | Member Default | Officer Default |
|--------|-------------|:----------------:|:------------:|:--------------:|:---------------:|
| `break` | Block breaking | false | false | true | true |
| `place` | Block placement | false | false | true | true |
| `interact` | General interaction (parent of door/container/bench/processing/seat/transport) | false | true | true | true |
| `doorUse` | Door and gate use | false | true | true | true |
| `containerUse` | Container (chest, barrel) access | false | false | true | true |
| `benchUse` | Crafting bench use | false | false | true | true |
| `processingUse` | Processing station use (furnace, etc.) | false | false | true | true |
| `seatUse` | Seat use | false | true | true | true |
| `transportUse` | Transport use (vehicles, teleporters) | false | true | true | true |
| `crateUse` | Crate access | false | false | true | true |
| `npcTame` | NPC taming | false | false | true | true |
| `pveDamage` | PvE damage (attacking mobs) | false | true | true | true |

**Mob spawning flags:**

| Key | Default | Description |
|-----|:-------:|-------------|
| `mobSpawning.enabled` | true | Master toggle for mob spawning in territory |
| `mobSpawning.hostile` | true | Allow hostile mob spawning |
| `mobSpawning.passive` | true | Allow passive mob spawning |
| `mobSpawning.neutral` | true | Allow neutral mob spawning |

**Treasury flags:**

| Key | Default | Description |
|-----|:-------:|-------------|
| `treasury.deposit` | true | Whether members can deposit into the faction treasury |
| `treasury.withdraw` | false | Whether officers can withdraw from the faction treasury |
| `treasury.transfer` | false | Whether officers can transfer money to other factions |

**Global flags:**

| Key | Default | Description |
|-----|:-------:|-------------|
| `pvpEnabled` | true | Whether PvP is enabled in faction territory |
| `officersCanEdit` | false | Whether officers can edit faction permissions |

**Parent-child relationships:**
- `{level}Interact` is the parent of `{level}DoorUse`, `{level}ContainerUse`, `{level}BenchUse`, `{level}ProcessingUse`, `{level}SeatUse`, `{level}TransportUse`
- `mobSpawning.enabled` is the parent of `hostile`, `passive`, `neutral`

When a parent flag is false, all child flags are effectively false regardless of their stored value.

**Default faction-permissions.json (nested format):**
```json
{
  "defaults": {
    "outsider": {
      "break": false, "place": false, "interact": false,
      "doorUse": false, "containerUse": false, "benchUse": false,
      "processingUse": false, "seatUse": false, "transportUse": false,
      "crateUse": false, "npcTame": false, "pveDamage": false
    },
    "ally": {
      "break": false, "place": false, "interact": true,
      "doorUse": true, "containerUse": false, "benchUse": false,
      "processingUse": false, "seatUse": true, "transportUse": true,
      "crateUse": false, "npcTame": false, "pveDamage": true
    },
    "member": {
      "break": true, "place": true, "interact": true,
      "doorUse": true, "containerUse": true, "benchUse": true,
      "processingUse": true, "seatUse": true, "transportUse": true,
      "crateUse": true, "npcTame": true, "pveDamage": true
    },
    "officer": {
      "break": true, "place": true, "interact": true,
      "doorUse": true, "containerUse": true, "benchUse": true,
      "processingUse": true, "seatUse": true, "transportUse": true,
      "crateUse": true, "npcTame": true, "pveDamage": true
    },
    "mobSpawning": {
      "enabled": true, "hostile": true, "passive": true, "neutral": true
    },
    "treasury": {
      "deposit": true, "withdraw": false, "transfer": false
    },
    "pvpEnabled": true,
    "officersCanEdit": false
  },
  "locks": {
    "outsider": {
      "break": false, "place": false, "interact": false,
      "doorUse": false, "containerUse": false, "benchUse": false,
      "processingUse": false, "seatUse": false, "transportUse": false,
      "crateUse": false, "npcTame": false, "pveDamage": false
    },
    "ally": { "...": "same structure, all false" },
    "member": { "...": "same structure, all false" },
    "officer": { "...": "same structure, all false" },
    "mobSpawning": {
      "enabled": false, "hostile": false, "passive": false, "neutral": false
    },
    "treasury": {
      "deposit": false, "withdraw": false, "transfer": false
    },
    "pvpEnabled": false,
    "officersCanEdit": false
  }
}
```

### WorldsConfig

[`config/modules/WorldsConfig.java`](../src/main/java/com/hyperfactions/config/modules/WorldsConfig.java)

Per-world behavior overrides in `config/worlds.json`:

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `enabled` | bool | true | Enable per-world settings module |
| `defaultPolicy` | string | `"allow"` | Default policy for unconfigured worlds: `"allow"` or `"deny"` |
| `worlds` | object | `{}` | Per-world setting overrides (keyed by world name or wildcard pattern) |

Per-world settings (5 per entry, all nullable — `null` means defer to global config):

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `claiming` | bool | null | Whether claiming is allowed in this world |
| `powerLoss` | bool | null | Whether power loss applies in this world |
| `friendlyFireFaction` | bool | null | Same-faction PvP override |
| `friendlyFireAlly` | bool | null | Ally PvP override |
| `maxClaims` | int | null | Maximum claims a faction can hold in this world. `null` or `0` = use global limit, `>0` = per-faction per-world hard cap |

**Wildcard support**: Use `%` as a wildcard in world names (e.g., `arena_%` matches `arena_1`, `arena_pvp`). Priority resolution: exact name match > wildcard patterns (fewer wildcards = higher priority) > default policy.

**Default rules**: A default `instance-%` wildcard rule blocks claiming in temporary instance worlds.

**Default worlds.json:**
```json
{
  "enabled": true,
  "defaultPolicy": "allow",
  "worlds": {
    "instance-%": { "claiming": false },
    "example-world-abc": { "claiming": true, "powerLoss": true, "friendlyFireFaction": false, "friendlyFireAlly": false }
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
3. `config/server.json` created with server behavior defaults (including `configVersion: 8`)
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
| FactionsConfig | [`config/modules/FactionsConfig.java`](../src/main/java/com/hyperfactions/config/modules/FactionsConfig.java) |
| ServerConfig | [`config/modules/ServerConfig.java`](../src/main/java/com/hyperfactions/config/modules/ServerConfig.java) |
| CoreConfig | [`config/CoreConfig.java`](../src/main/java/com/hyperfactions/config/CoreConfig.java) *(deprecated)* |
| ModuleConfig | [`config/ModuleConfig.java`](../src/main/java/com/hyperfactions/config/ModuleConfig.java) |
| ValidationResult | [`config/ValidationResult.java`](../src/main/java/com/hyperfactions/config/ValidationResult.java) |
| BackupConfig | [`config/modules/BackupConfig.java`](../src/main/java/com/hyperfactions/config/modules/BackupConfig.java) |
| ChatConfig | [`config/modules/ChatConfig.java`](../src/main/java/com/hyperfactions/config/modules/ChatConfig.java) |
| DebugConfig | [`config/modules/DebugConfig.java`](../src/main/java/com/hyperfactions/config/modules/DebugConfig.java) |
| EconomyConfig | [`config/modules/EconomyConfig.java`](../src/main/java/com/hyperfactions/config/modules/EconomyConfig.java) |
| FactionPermissionsConfig | [`config/modules/FactionPermissionsConfig.java`](../src/main/java/com/hyperfactions/config/modules/FactionPermissionsConfig.java) |
| AnnouncementConfig | [`config/modules/AnnouncementConfig.java`](../src/main/java/com/hyperfactions/config/modules/AnnouncementConfig.java) |
| GravestoneConfig | [`config/modules/GravestoneConfig.java`](../src/main/java/com/hyperfactions/config/modules/GravestoneConfig.java) |
| WorldMapConfig | [`config/modules/WorldMapConfig.java`](../src/main/java/com/hyperfactions/config/modules/WorldMapConfig.java) |
| WorldsConfig | [`config/modules/WorldsConfig.java`](../src/main/java/com/hyperfactions/config/modules/WorldsConfig.java) |
