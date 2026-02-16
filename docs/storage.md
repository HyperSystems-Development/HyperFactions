# HyperFactions Storage Layer

> **Version**: 0.8.0

Architecture documentation for the HyperFactions data persistence system.

## Overview

HyperFactions uses an interface-based storage layer with:

- **Storage Interfaces** - Abstract contracts for data operations
- **JSON Implementations** - File-based storage with pretty-printed JSON
- **Async Operations** - All I/O returns `CompletableFuture` for non-blocking
- **Data Models** - Java records for immutable data structures
- **Auto-Save** - Periodic saves with configurable interval
- **Migration Support** - Automatic data format upgrades (v1→v2→v3→v4)
- **Backup System** - GFS rotation with hourly/daily/weekly/manual/migration types
- **Import Directories** - Data import from ElbaphFactions and HyFactions

## Architecture

```
Storage Interface                  Implementation
      │                                  │
FactionStorage ────────────────► JsonFactionStorage
PlayerStorage  ────────────────► JsonPlayerStorage
ZoneStorage    ────────────────► JsonZoneStorage
      │                                  │
      └──────── Data Models ◄────────────┘
                    │
           Faction, PlayerPower,
           Zone, FactionClaim, etc.

Backup System
      │
BackupManager ─────────────────► ZIP archives in backups/
      │                          (GFS rotation: hourly, daily, weekly)
      │
      └── BackupMetadata ──────► Filename-encoded metadata
```

## Data Directory Structure

```
<server>/mods/com.hyperfactions_HyperFactions/
├── config.json                    # Core configuration (v4)
├── config/                        # Module configs (7 files)
├── factions/                      # Per-faction JSON files
│   ├── <uuid>.json
│   └── ...
├── players/                       # Per-player power data
│   ├── <uuid>.json
│   └── ...
├── zones.json                     # All zones in one file
└── backups/                       # Backup archives
    ├── hourly_2025-01-15_12-00-00.zip
    ├── daily_2025-01-15_00-00-00.zip
    ├── weekly_2025-01-13_00-00-00.zip
    ├── manual_my-backup.zip
    └── migration_v3-to-v4_2025-01-15_00-00-00.zip
```

## Backup System

The `BackupManager` implements GFS (Grandfather-Father-Son) rotation for automatic backup management.

### Backup Types

| Type | Auto-Rotated | Default Retention |
|------|-------------|-------------------|
| `HOURLY` | Yes | Last 24 |
| `DAILY` | Yes | Last 7 |
| `WEEKLY` | Yes | Last 4 |
| `MANUAL` | No | Keep all (configurable) |
| `MIGRATION` | No | Keep all |

### Backup Contents

Each ZIP archive contains:
- `data/factions/` — All faction JSON files
- `data/players/` — All player power JSON files
- `zones.json` — Zone definitions
- `config.json` — Core configuration
- `config/` — Module config directory

### Key Operations

| Method | Description |
|--------|-------------|
| `createBackup(type)` | Create async ZIP backup |
| `restoreBackup(name)` | Async ZIP extraction + reload |
| `listBackups()` | List sorted by timestamp (newest first) |
| `performRotation()` | GFS cleanup of old backups |
| `startScheduledBackups()` | Schedule hourly backups (72,000 ticks) |

See [Data Import & Migration](data-import.md) for import directory details and config migration.

## Key Classes

| Class | Path | Purpose |
|-------|------|---------|
| FactionStorage | [`storage/FactionStorage.java`](../src/main/java/com/hyperfactions/storage/FactionStorage.java) | Faction storage interface |
| PlayerStorage | [`storage/PlayerStorage.java`](../src/main/java/com/hyperfactions/storage/PlayerStorage.java) | Player power storage interface |
| ZoneStorage | [`storage/ZoneStorage.java`](../src/main/java/com/hyperfactions/storage/ZoneStorage.java) | Zone storage interface |
| JsonFactionStorage | [`storage/json/JsonFactionStorage.java`](../src/main/java/com/hyperfactions/storage/json/JsonFactionStorage.java) | JSON faction storage |
| JsonPlayerStorage | [`storage/json/JsonPlayerStorage.java`](../src/main/java/com/hyperfactions/storage/json/JsonPlayerStorage.java) | JSON player storage |
| JsonZoneStorage | [`storage/json/JsonZoneStorage.java`](../src/main/java/com/hyperfactions/storage/json/JsonZoneStorage.java) | JSON zone storage |
| StorageHealth | [`storage/StorageHealth.java`](../src/main/java/com/hyperfactions/storage/StorageHealth.java) | Storage health monitoring |

## Data Directory Structure

```
<server>/mods/com.hyperfactions_HyperFactions/
├── config.json                    # Configuration (see config.md)
├── config/                        # Module configs
├── zones.json                     # All zones in single file
├── update_preferences.json        # Update notification preferences
├── factions/                      # One file per faction
│   ├── {uuid}.json
│   ├── {uuid}.json
│   └── ...
├── players/                       # One file per player
│   ├── {uuid}.json
│   ├── {uuid}.json
│   └── ...
└── backups/                       # Backup storage
    ├── hourly/
    ├── daily/
    ├── weekly/
    └── manual/
```

## Storage Interfaces

### FactionStorage

[`storage/FactionStorage.java`](../src/main/java/com/hyperfactions/storage/FactionStorage.java)

```java
public interface FactionStorage {

    /**
     * Initialize storage (create directories, etc.).
     */
    CompletableFuture<Void> init();

    /**
     * Shutdown storage (flush pending writes).
     */
    CompletableFuture<Void> shutdown();

    /**
     * Load a single faction by ID.
     */
    CompletableFuture<Optional<Faction>> loadFaction(UUID factionId);

    /**
     * Save a faction (create or update).
     */
    CompletableFuture<Void> saveFaction(Faction faction);

    /**
     * Delete a faction.
     */
    CompletableFuture<Void> deleteFaction(UUID factionId);

    /**
     * Load all factions.
     */
    CompletableFuture<Collection<Faction>> loadAllFactions();
}
```

### PlayerStorage

[`storage/PlayerStorage.java`](../src/main/java/com/hyperfactions/storage/PlayerStorage.java)

```java
public interface PlayerStorage {

    CompletableFuture<Void> init();
    CompletableFuture<Void> shutdown();

    CompletableFuture<Optional<PlayerPower>> loadPlayerPower(UUID playerUuid);
    CompletableFuture<Void> savePlayerPower(PlayerPower power);
    CompletableFuture<Void> deletePlayerPower(UUID playerUuid);
    CompletableFuture<Collection<PlayerPower>> loadAllPlayerPower();
}
```

### ZoneStorage

[`storage/ZoneStorage.java`](../src/main/java/com/hyperfactions/storage/ZoneStorage.java)

```java
public interface ZoneStorage {

    CompletableFuture<Void> init();
    CompletableFuture<Void> shutdown();

    CompletableFuture<Collection<Zone>> loadAllZones();
    CompletableFuture<Void> saveAllZones(Collection<Zone> zones);
}
```

## JSON Implementations

### JsonFactionStorage

[`storage/json/JsonFactionStorage.java`](../src/main/java/com/hyperfactions/storage/json/JsonFactionStorage.java)

Stores one JSON file per faction in `factions/` directory:

```java
public class JsonFactionStorage implements FactionStorage {

    private final Path factionsDir;
    private final Gson gson;

    public JsonFactionStorage(Path dataDir) {
        this.factionsDir = dataDir.resolve("factions");
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();
    }

    @Override
    public CompletableFuture<Void> saveFaction(Faction faction) {
        return CompletableFuture.runAsync(() -> {
            Path file = factionsDir.resolve(faction.id() + ".json");
            try (Writer writer = Files.newBufferedWriter(file)) {
                gson.toJson(factionToJson(faction), writer);
            }
        });
    }

    private Path getFactionFile(UUID factionId) {
        return factionsDir.resolve(factionId.toString() + ".json");
    }
}
```

### JsonPlayerStorage

[`storage/json/JsonPlayerStorage.java`](../src/main/java/com/hyperfactions/storage/json/JsonPlayerStorage.java)

Stores one JSON file per player in `players/` directory:

```java
public class JsonPlayerStorage implements PlayerStorage {

    private final Path playersDir;

    @Override
    public CompletableFuture<Void> savePlayerPower(PlayerPower power) {
        return CompletableFuture.runAsync(() -> {
            Path file = playersDir.resolve(power.uuid() + ".json");
            // Write JSON...
        });
    }
}
```

### JsonZoneStorage

[`storage/json/JsonZoneStorage.java`](../src/main/java/com/hyperfactions/storage/json/JsonZoneStorage.java)

Stores all zones in a single `zones.json` file (zones are typically few in number):

```java
public class JsonZoneStorage implements ZoneStorage {

    private final Path zonesFile;

    @Override
    public CompletableFuture<Void> saveAllZones(Collection<Zone> zones) {
        return CompletableFuture.runAsync(() -> {
            // Write all zones as JSON array
        });
    }
}
```

## Data Models

### Faction

[`data/Faction.java`](../src/main/java/com/hyperfactions/data/Faction.java)

Mutable entity with builder-style setters:

```java
public class Faction {
    private final UUID id;
    private String name;
    private String description;
    private String tag;
    private String color;
    private long createdAt;
    private boolean open;
    private FactionHome home;
    private final List<FactionMember> members;
    private final List<FactionClaim> claims;
    private final List<FactionRelation> relations;
    private final List<FactionLog> logs;
    private FactionPermissions permissions;

    // Getters and builder-style setters
    public Faction setName(String name) {
        this.name = name;
        return this;
    }
}
```

**JSON Structure** (`factions/{uuid}.json`):

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Warriors",
  "description": "A mighty faction",
  "tag": "WAR",
  "color": "c",
  "createdAt": 1706745600000,
  "open": false,
  "home": {
    "world": "world",
    "x": 100.5,
    "y": 64.0,
    "z": 200.5,
    "yaw": 90.0,
    "pitch": 0.0,
    "setAt": 1706745600000,
    "setBy": "player-uuid"
  },
  "members": [
    {
      "uuid": "player-uuid",
      "username": "PlayerName",
      "role": "LEADER",
      "joinedAt": 1706745600000,
      "lastOnline": 1706832000000
    }
  ],
  "claims": [
    {
      "world": "world",
      "chunkX": 10,
      "chunkZ": 20,
      "claimedAt": 1706745600000,
      "claimedBy": "player-uuid"
    }
  ],
  "relations": [
    {
      "targetFactionId": "other-uuid",
      "type": "ALLY",
      "since": 1706745600000
    }
  ],
  "logs": [
    {
      "type": "MEMBER_JOIN",
      "message": "PlayerName joined",
      "timestamp": 1706745600000,
      "actorUuid": "player-uuid"
    }
  ],
  "permissions": {
    "outsiderBreak": false,
    "memberBreak": true,
    "pvpEnabled": true
  }
}
```

### FactionMember

[`data/FactionMember.java`](../src/main/java/com/hyperfactions/data/FactionMember.java)

```java
public record FactionMember(
    UUID uuid,
    String username,
    FactionRole role,
    long joinedAt,
    long lastOnline
) {}
```

### FactionRole

[`data/FactionRole.java`](../src/main/java/com/hyperfactions/data/FactionRole.java)

```java
public enum FactionRole {
    LEADER,   // Full control
    OFFICER,  // Can manage members, claims
    MEMBER    // Basic permissions
}
```

### PlayerPower

[`data/PlayerPower.java`](../src/main/java/com/hyperfactions/data/PlayerPower.java)

```java
public record PlayerPower(
    UUID uuid,
    double power,
    double maxPower,
    long lastDeath,
    long lastRegen
) {}
```

**JSON Structure** (`players/{uuid}.json`):

```json
{
  "uuid": "550e8400-e29b-41d4-a716-446655440000",
  "power": 15.5,
  "maxPower": 20.0,
  "lastDeath": 1706745600000,
  "lastRegen": 1706832000000
}
```

### Zone

[`data/Zone.java`](../src/main/java/com/hyperfactions/data/Zone.java)

```java
public class Zone {
    private final UUID id;
    private String name;
    private ZoneType type;
    private String world;
    private final Set<ChunkKey> chunks;
    private long createdAt;
    private UUID createdBy;
    private final Map<String, Boolean> flags;
}
```

**JSON Structure** (`zones.json`):

```json
[
  {
    "id": "zone-uuid",
    "name": "Spawn",
    "type": "SAFE",
    "world": "world",
    "chunks": [
      { "x": 0, "z": 0 },
      { "x": 0, "z": 1 }
    ],
    "createdAt": 1706745600000,
    "createdBy": "admin-uuid",
    "flags": {
      "pvp_enabled": false,
      "build_allowed": false
    }
  }
]
```

### ChunkKey

[`data/ChunkKey.java`](../src/main/java/com/hyperfactions/data/ChunkKey.java)

Immutable identifier for a chunk:

```java
public record ChunkKey(String world, int x, int z) {

    @Override
    public int hashCode() {
        return Objects.hash(world, x, z);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChunkKey other)) return false;
        return x == other.x && z == other.z && world.equals(other.world);
    }
}
```

## Async Pattern

All storage operations are async to prevent blocking the main thread:

```java
// In manager
public void loadAll() {
    factionStorage.loadAllFactions()
        .thenAccept(factions -> {
            for (Faction faction : factions) {
                cache.put(faction.id(), faction);
            }
        })
        .join(); // Block only during startup
}

public void saveFaction(Faction faction) {
    // Fire and forget during normal operation
    factionStorage.saveFaction(faction);
}
```

### Startup Loading

During startup, `.join()` is used to ensure data is loaded before the plugin is ready:

```java
// In HyperFactions.enable()
factionStorage.init().join();
playerStorage.init().join();
zoneStorage.init().join();

factionManager.loadAll().join();
powerManager.loadAll().join();
zoneManager.loadAll().join();
```

### Runtime Saves

During normal operation, saves are fire-and-forget:

```java
// In FactionManager
public void updateFaction(Faction faction) {
    cache.put(faction.id(), faction);
    factionStorage.saveFaction(faction); // Async, doesn't block
}
```

## Auto-Save System

Configured in `config.json`:

```json
{
  "autoSave": {
    "enabled": true,
    "intervalMinutes": 5
  }
}
```

Implementation in `HyperFactions.java`:

```java
private void startAutoSaveTask() {
    int intervalMinutes = ConfigManager.get().getAutoSaveIntervalMinutes();
    int periodTicks = intervalMinutes * 60 * 20;

    autoSaveTaskId = scheduleRepeatingTask(periodTicks, periodTicks, this::saveAllData);
}

public void saveAllData() {
    Logger.info("Auto-saving data...");
    factionManager.saveAll().join();
    powerManager.saveAll().join();
    zoneManager.saveAll().join();
    Logger.info("Auto-save complete");
}
```

## Data Migration

[`migration/MigrationRunner.java`](../src/main/java/com/hyperfactions/migration/MigrationRunner.java)

Handles automatic data format upgrades:

### Zone Format Migration

Old single-chunk format:
```json
{
  "id": "...",
  "world": "world",
  "chunkX": 10,
  "chunkZ": 20
}
```

Migrates to multi-chunk format:
```json
{
  "id": "...",
  "world": "world",
  "chunks": [{ "x": 10, "z": 20 }]
}
```

Migration is detected and run automatically on load.

## Storage Health

[`storage/StorageHealth.java`](../src/main/java/com/hyperfactions/storage/StorageHealth.java)

Monitors storage system health:

```java
public class StorageHealth {

    private final AtomicLong lastSaveTime = new AtomicLong();
    private final AtomicInteger failedSaves = new AtomicInteger();

    public void recordSave() {
        lastSaveTime.set(System.currentTimeMillis());
    }

    public void recordFailure() {
        failedSaves.incrementAndGet();
    }

    public boolean isHealthy() {
        // Check if saves are succeeding
        return failedSaves.get() < MAX_CONSECUTIVE_FAILURES;
    }
}
```

## Implementing Alternative Storage

To add database support, implement the storage interfaces:

```java
public class MySqlFactionStorage implements FactionStorage {

    private final DataSource dataSource;

    @Override
    public CompletableFuture<Void> saveFaction(Faction faction) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection()) {
                // SQL INSERT/UPDATE
            }
        });
    }

    @Override
    public CompletableFuture<Optional<Faction>> loadFaction(UUID factionId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dataSource.getConnection()) {
                // SQL SELECT
            }
        });
    }
}
```

Then configure in HyperFactions:

```java
// In HyperFactions.enable()
if (ConfigManager.get().isUsingDatabase()) {
    factionStorage = new MySqlFactionStorage(dataSource);
} else {
    factionStorage = new JsonFactionStorage(dataDir);
}
```

## Backup Integration

Storage integrates with the backup system:

```java
// In BackupManager
public void createBackup(BackupType type) {
    // Save all data first
    hyperFactions.saveAllData();

    // Copy data directories to backup
    copyDirectory(dataDir.resolve("factions"), backupDir);
    copyDirectory(dataDir.resolve("players"), backupDir);
    copyFile(dataDir.resolve("zones.json"), backupDir);
    copyFile(dataDir.resolve("config.json"), backupDir);
}
```

## Manual Data Editing

JSON files can be manually edited while the server is stopped:

1. Stop the server
2. Edit JSON files
3. Start the server (data loads fresh)

**Warning:** Editing while the server is running may cause data loss due to auto-save overwriting changes.

## Code Links

| Class | Path |
|-------|------|
| FactionStorage | [`storage/FactionStorage.java`](../src/main/java/com/hyperfactions/storage/FactionStorage.java) |
| PlayerStorage | [`storage/PlayerStorage.java`](../src/main/java/com/hyperfactions/storage/PlayerStorage.java) |
| ZoneStorage | [`storage/ZoneStorage.java`](../src/main/java/com/hyperfactions/storage/ZoneStorage.java) |
| JsonFactionStorage | [`storage/json/JsonFactionStorage.java`](../src/main/java/com/hyperfactions/storage/json/JsonFactionStorage.java) |
| JsonPlayerStorage | [`storage/json/JsonPlayerStorage.java`](../src/main/java/com/hyperfactions/storage/json/JsonPlayerStorage.java) |
| JsonZoneStorage | [`storage/json/JsonZoneStorage.java`](../src/main/java/com/hyperfactions/storage/json/JsonZoneStorage.java) |
| Faction | [`data/Faction.java`](../src/main/java/com/hyperfactions/data/Faction.java) |
| PlayerPower | [`data/PlayerPower.java`](../src/main/java/com/hyperfactions/data/PlayerPower.java) |
| Zone | [`data/Zone.java`](../src/main/java/com/hyperfactions/data/Zone.java) |
| ChunkKey | [`data/ChunkKey.java`](../src/main/java/com/hyperfactions/data/ChunkKey.java) |
| BackupManager | [`backup/BackupManager.java`](../src/main/java/com/hyperfactions/backup/BackupManager.java) |
