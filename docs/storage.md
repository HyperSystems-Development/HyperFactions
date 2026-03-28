# HyperFactions Storage Layer

> **Version**: 0.13.0

Architecture documentation for the HyperFactions data persistence system.

## Overview

HyperFactions uses an interface-based storage layer with:

- **Storage Interfaces** - Abstract contracts for data operations
- **JSON Implementations** - File-based storage with pretty-printed JSON
- **Async Operations** - All I/O returns `CompletableFuture` for non-blocking
- **Data Models** - Java records for immutable data structures (Faction, Zone, PlayerPower, ChunkKey) and mutable PlayerData class
- **Auto-Save** - Periodic saves with configurable interval
- **Safe-Save** - Atomic writes with SHA-256 checksums, backup recovery, `.bak` auto-cleanup
- **Per-UUID Locking** - `JsonPlayerStorage` uses per-UUID locks to prevent concurrent load-modify-save race conditions (e.g., simultaneous deaths losing kill/death increments)
- **Migration Support** - Automatic config (v1→v8) and data (v0→v1, v1→v2) format upgrades
- **Backup System** - GFS rotation with hourly/daily/weekly/manual/migration types
- **Import Directories** - Data import from ElbaphFactions, HyFactions, SimpleClaims, and FactionsX

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
           Faction, PlayerPower, PlayerData,
           Zone, FactionClaim, ChunkKey, etc.

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
├── config/                        # Configuration files
│   ├── factions.json              # Faction gameplay settings
│   ├── server.json                # Server behavior settings
│   └── ...                        # Other module configs
├── data/                          # All data files (migrated from root in v0→v1)
│   ├── factions/                  # Per-faction JSON files
│   │   └── {uuid}.json
│   ├── players/                   # Per-player power data
│   │   └── {uuid}.json
│   ├── chat/                      # Per-faction chat history
│   │   └── {factionId}.json
│   ├── economy/                   # Per-faction treasury data
│   │   └── {factionId}.json
│   ├── zones.json                 # All zones in one file
│   ├── invites.json               # Pending faction invites
│   ├── join_requests.json         # Pending join requests
│   └── .version                   # Data layout version marker (currently: 1)
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
- `data/chat/` — Per-faction chat history files
- `data/economy/` — Per-faction treasury data files
- `data/zones.json` — Zone definitions
- `data/invites.json` — Pending faction invites
- `data/join_requests.json` — Pending join requests
- `config/` — Configuration files (factions.json, server.json, etc.)

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
| ChatHistoryStorage | [`storage/ChatHistoryStorage.java`](../src/main/java/com/hyperfactions/storage/ChatHistoryStorage.java) | Chat history storage interface |
| JsonChatHistoryStorage | [`storage/json/JsonChatHistoryStorage.java`](../src/main/java/com/hyperfactions/storage/json/JsonChatHistoryStorage.java) | JSON chat history storage |
| JsonEconomyStorage | [`storage/JsonEconomyStorage.java`](../src/main/java/com/hyperfactions/storage/JsonEconomyStorage.java) | JSON economy/treasury storage |
| StorageUtils | [`storage/StorageUtils.java`](../src/main/java/com/hyperfactions/storage/StorageUtils.java) | Atomic write, checksum, backup recovery |
| StorageHealth | [`storage/StorageHealth.java`](../src/main/java/com/hyperfactions/storage/StorageHealth.java) | Storage health monitoring |

## Data Directory Structure

```
<server>/mods/com.hyperfactions_HyperFactions/
├── config/                        # Configuration files (see config.md)
│   ├── factions.json              # Faction gameplay settings
│   ├── server.json                # Server behavior settings
│   └── ...                        # Other module configs
├── data/                          # All data files
│   ├── factions/                  # One file per faction
│   │   └── {uuid}.json
│   ├── players/                   # One file per player
│   │   └── {uuid}.json
│   ├── chat/                      # Per-faction chat history
│   │   └── {factionId}.json
│   ├── economy/                   # Per-faction treasury data
│   │   └── {factionId}.json
│   ├── zones.json                 # All zones in single file
│   ├── invites.json               # Pending faction invites
│   ├── join_requests.json         # Pending join requests
│   └── .version                   # Data layout version (1)
├── update_preferences.json        # Update notification preferences
└── backups/                       # Backup storage (ZIP archives)
    └── backup_*.zip
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

    // Power-only operations (delegates to PlayerData internally)
    CompletableFuture<Optional<PlayerPower>> loadPlayerPower(UUID uuid);
    CompletableFuture<Void> savePlayerPower(PlayerPower power);
    CompletableFuture<Void> deletePlayerPower(UUID uuid);
    CompletableFuture<Collection<PlayerPower>> loadAllPlayerPower();

    // UUID discovery
    CompletableFuture<Set<UUID>> getAllPlayerUuids();

    // Full player data operations (power + history + stats + preferences)
    CompletableFuture<Optional<PlayerData>> loadPlayerData(UUID uuid);
    CompletableFuture<Void> savePlayerData(PlayerData data);

    /**
     * Atomically loads player data, applies the updater, and saves.
     * Thread-safe: concurrent updates to the same player are serialized.
     *
     * @param updater a Consumer that modifies the player data in place
     */
    CompletableFuture<Void> updatePlayerData(UUID uuid, Consumer<PlayerData> updater);
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

Stores one JSON file per player in `players/` directory. Uses per-UUID locking to prevent race conditions from concurrent kill/death tracking:

```java
public class JsonPlayerStorage implements PlayerStorage {

    private final Path playersDir;
    private final ConcurrentHashMap<UUID, ReentrantLock> playerLocks = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<Void> savePlayerPower(PlayerPower power) {
        return CompletableFuture.runAsync(() -> {
            Path file = playersDir.resolve(power.uuid() + ".json");
            // Write JSON...
        });
    }

    /**
     * Atomically update player data under a per-UUID lock.
     * Prevents lost updates from concurrent deaths/kills.
     * Note: updater is a Consumer that modifies PlayerData in place (mutable class).
     */
    public CompletableFuture<Void> updatePlayerData(UUID uuid, Consumer<PlayerData> updater) {
        return CompletableFuture.runAsync(() -> {
            ReentrantLock lock = playerLocks.computeIfAbsent(uuid, k -> new ReentrantLock());
            lock.lock();
            try {
                PlayerData data = loadPlayerDataSync(uuid);
                if (data == null) data = new PlayerData(uuid);
                updater.accept(data);   // Mutates in place
                savePlayerDataSync(data);
            } finally {
                lock.unlock();
            }
        });
    }
}
```

The `updatePlayerData` method ensures that concurrent operations (e.g., two simultaneous deaths) do not lose increments through unsynchronized load-modify-save cycles.

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

Immutable record with `with*()` copy methods for updates:

```java
public record Faction(
    UUID id,
    String name,
    @Nullable String description,
    @Nullable String tag,
    String color,                           // Hex string, e.g., "#55FFFF"
    long createdAt,
    @Nullable FactionHome home,
    Map<UUID, FactionMember> members,       // Map, not List
    Set<FactionClaim> claims,               // Set, not List
    Map<UUID, FactionRelation> relations,   // Map keyed by target faction UUID
    List<FactionLog> logs,
    boolean open,
    @Nullable FactionPermissions permissions,
    @Nullable Double hardcorePower          // Hardcore mode faction power pool
) {
    // Compact constructor: copies collections to immutable, auto-migrates legacy color codes
    // Update methods return new Faction instances:
    //   withName(), withDescription(), withTag(), withColor(), withOpen()
    //   withMember(), withoutMember(), withClaim(), withoutClaimAt()
    //   withRelation(), withHome(), withLog(), withPermissions(), withHardcorePower()
}
```

**JSON Structure** (`factions/{uuid}.json`):

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Warriors",
  "description": "A mighty faction",
  "tag": "WAR",
  "color": "#55FFFF",
  "createdAt": 1706745600000,
  "open": false,
  "hardcorePower": null,
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
  "members": {
    "player-uuid": {
      "uuid": "player-uuid",
      "username": "PlayerName",
      "role": "LEADER",
      "joinedAt": 1706745600000,
      "lastOnline": 1706832000000
    }
  },
  "claims": [
    {
      "world": "world",
      "chunkX": 10,
      "chunkZ": 20,
      "claimedAt": 1706745600000,
      "claimedBy": "player-uuid"
    }
  ],
  "relations": {
    "other-faction-uuid": {
      "targetFactionId": "other-faction-uuid",
      "type": "ALLY",
      "since": 1706745600000
    }
  },
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

> **Note**: `color` is stored as a hex string (e.g., `"#55FFFF"`). Legacy single-char codes (e.g., `"c"`) are auto-migrated to hex on load. `members` and `relations` are serialized as maps keyed by UUID.

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
    long lastRegen,
    @Nullable Double maxPowerOverride,   // Per-player max power override (null = use global config)
    boolean powerLossDisabled,            // Absolute bypass: never loses power from any source
    boolean claimDecayExempt             // Treated as always online for claim decay
) {
    // getEffectiveMaxPower() returns override if set, else maxPower
    // withPower(), withDeathPenalty(), withRegen(), withMaxPower()
    // withMaxPowerOverride(), withPowerLossDisabled(), withClaimDecayExempt()
}
```

> **Note**: `PlayerPower` is the immutable power-only record. Actual on-disk storage uses the mutable `PlayerData` class which wraps power fields plus kill/death stats, membership history, preferences, and admin bypass state. `PlayerPower` is extracted from `PlayerData` via `toPower()`.

**JSON Structure** (`players/{uuid}.json`) — stored as `PlayerData`:

```json
{
  "uuid": "550e8400-e29b-41d4-a716-446655440000",
  "username": "PlayerName",
  "power": 15.5,
  "maxPower": 20.0,
  "lastDeath": 1706745600000,
  "lastRegen": 1706832000000,
  "kills": 5,
  "deaths": 2,
  "firstJoined": 1706745600000,
  "lastOnline": 1706832000000,
  "maxPowerOverride": null,
  "powerLossDisabled": false,
  "claimDecayExempt": false,
  "adminBypassEnabled": false,
  "languagePreference": null,
  "territoryAlertsEnabled": true,
  "deathAnnouncementsEnabled": true,
  "powerNotificationsEnabled": true,
  "membershipHistory": []
}
```

### PlayerData

[`data/PlayerData.java`](../src/main/java/com/hyperfactions/data/PlayerData.java)

Mutable class combining power fields with extended player data. Stored on disk in `data/players/{uuid}.json`:

```java
public class PlayerData {
    private UUID uuid;
    private String username;

    // Power fields (same as PlayerPower record)
    private double power;
    private double maxPower;
    private long lastDeath;
    private long lastRegen;
    private Double maxPowerOverride;
    private boolean powerLossDisabled;
    private boolean claimDecayExempt;

    // Stats
    private int kills;
    private int deaths;
    private long firstJoined;
    private long lastOnline;

    // History
    private List<MembershipRecord> membershipHistory;

    // Admin state
    private boolean adminBypassEnabled;

    // Player preferences (i18n + notifications)
    private String languagePreference;
    private boolean territoryAlertsEnabled = true;
    private boolean deathAnnouncementsEnabled = true;
    private boolean powerNotificationsEnabled = true;

    // Conversion: toPower() -> PlayerPower, updatePower(PlayerPower) <- PlayerPower
    // Membership: addRecord(), closeActiveRecord(), getActiveRecord(), updateHighestRole()
    // Stats: incrementKills(), incrementDeaths()
}
```

### Zone

[`data/Zone.java`](../src/main/java/com/hyperfactions/data/Zone.java)

```java
public record Zone(
    UUID id,
    String name,
    ZoneType type,
    String world,
    Set<ChunkKey> chunks,
    long createdAt,
    UUID createdBy,
    @Nullable Map<String, Boolean> flags,           // Boolean flags (null = use zone type defaults)
    @Nullable Map<String, String> settings,         // String-valued settings for enum/selection options
    @Nullable Boolean notifyOnEntry,                // Show entry notification (null/true = show)
    @Nullable String notifyTitleUpper,              // Custom upper title text (null = default)
    @Nullable String notifyTitleLower               // Custom lower title text (null = default)
) {
    // Compact constructor ensures chunks is immutable
    // withChunk(), withoutChunk(), withName(), withFlag(), withoutFlag()
    // withSetting(), withoutSetting(), withNotifyOnEntry(), withNotifyTitleUpper/Lower()
    // getEffectiveFlag() considers parent-child flag enforcement and zone type defaults
    // getEffectiveSetting() considers zone type defaults
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
    },
    "settings": null,
    "notifyOnEntry": true,
    "notifyTitleUpper": null,
    "notifyTitleLower": null
  }
]
```

### ChunkKey

[`data/ChunkKey.java`](../src/main/java/com/hyperfactions/data/ChunkKey.java)

Immutable identifier for a chunk:

```java
/**
 * Note: Hytale uses 32-block chunks (shift by 5), not 16-block chunks.
 */
public record ChunkKey(
    String world,
    int chunkX,    // NOT "x" — field name is "chunkX"
    int chunkZ     // NOT "z" — field name is "chunkZ"
) {
    private static final int CHUNK_SIZE = 32;
    private static final int CHUNK_SHIFT = 5;

    public static ChunkKey fromWorldCoords(String world, double x, double z) {
        return new ChunkKey(world, (int) Math.floor(x) >> CHUNK_SHIFT, (int) Math.floor(z) >> CHUNK_SHIFT);
    }

    public static ChunkKey fromBlockCoords(String world, int blockX, int blockZ) {
        return new ChunkKey(world, blockX >> CHUNK_SHIFT, blockZ >> CHUNK_SHIFT);
    }

    // Navigation: north(), south(), east(), west()
    // Utility: isAdjacentTo(), getCenterX(), getCenterZ(), getMinBlockX(), etc.
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

Configured in `config/server.json`:

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

## Safe-Save Mechanism

[`storage/StorageUtils.java`](../src/main/java/com/hyperfactions/storage/StorageUtils.java)

All 7 storage types use `StorageUtils.writeAtomic()` for crash-safe writes:

1. Write content to a temp file (`file.{counter}.tmp`)
2. Compute SHA-256 checksum of content
3. Read back temp file and verify checksum matches
4. Copy existing file to `.bak` backup
5. Atomic rename: temp → target
6. Delete `.bak` file (cleanup after successful write)

If the process crashes during steps 1-4, the original file is untouched. If it crashes during step 5, the `.bak` file provides recovery. On startup, `cleanupOrphanedFiles()` removes any stray `.tmp` or orphaned `.bak` files.

### Storage Types Using writeAtomic()

| Storage | File Pattern | Notes |
|---------|-------------|-------|
| JsonFactionStorage | `data/factions/{uuid}.json` | One file per faction |
| JsonPlayerStorage | `data/players/{uuid}.json` | One file per player |
| JsonZoneStorage | `data/zones.json` | Single file for all zones |
| JsonChatHistoryStorage | `data/chat/{factionId}.json` | One file per faction |
| JsonEconomyStorage | `data/economy/{factionId}.json` | One file per faction |
| InviteManager | `data/invites.json` | Single file |
| JoinRequestManager | `data/join_requests.json` | Single file |

## Data Migration

### Data Directory Migration (v0→v1)

[`migration/migrations/data/DataV0ToV1Migration.java`](../src/main/java/com/hyperfactions/migration/migrations/data/DataV0ToV1Migration.java)

Moves data files from the plugin root into a `data/` subdirectory. The migration:
- Creates `data/` directory
- Moves: `factions/`, `players/`, `chat/`, `economy/`, `zones.json`, `invites.json`, `join_requests.json`
- Also moves any `.bak` files alongside their data files
- Writes `data/.version` with `1` (last step — if crash before this, migration re-runs)
- MigrationRunner creates ZIP backup before execution for rollback support

**Detection:** Runs when `data/.version` doesn't exist AND at least one old-path item exists.

### Data Format Migration (v1→v2)

[`migration/migrations/data/DataV1ToV2Migration.java`](../src/main/java/com/hyperfactions/migration/migrations/data/DataV1ToV2Migration.java)

Second data migration step. Also handled by MigrationRunner.

### Zone Format Migration

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
public final class StorageHealth {

    private static final StorageHealth INSTANCE = new StorageHealth();

    /** Time window for rate calculation — 5 minutes. */
    private static final long RATE_WINDOW_MS = 5 * 60 * 1000;

    private final AtomicLong totalSuccesses = new AtomicLong(0);
    private final AtomicLong totalFailures = new AtomicLong(0);

    /** Timestamped writes for rate calculation. */
    private final LinkedList<TimestampedWrite> recentWrites = new LinkedList<>();

    /** Per-file success/failure counts. */
    private final Map<String, AtomicLong> successCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> failureCounts = new ConcurrentHashMap<>();

    public void recordSuccess(String filePath) { ... }
    public void recordFailure(String filePath, String error) { ... }

    /**
     * Returns false if the recent failure rate exceeds 10%
     * (rate-based, not consecutive-failure-based).
     */
    public boolean isHealthy() {
        return getRecentFailureRate() < 0.10;
    }

    public double getRecentFailureRate() {
        // Calculates failures / total writes in the 5-minute window
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

    // Copy data directories to backup (from data/ subdirectory)
    Path dataPath = dataDir.resolve("data");
    copyDirectory(dataPath.resolve("factions"), backupDir);
    copyDirectory(dataPath.resolve("players"), backupDir);
    copyDirectory(dataPath.resolve("chat"), backupDir);
    copyDirectory(dataPath.resolve("economy"), backupDir);
    copyFile(dataPath.resolve("zones.json"), backupDir);
    copyFile(dataPath.resolve("invites.json"), backupDir);
    copyFile(dataPath.resolve("join_requests.json"), backupDir);
    copyDirectory(dataDir.resolve("config"), backupDir);
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
| PlayerData | [`data/PlayerData.java`](../src/main/java/com/hyperfactions/data/PlayerData.java) |
| ChatHistoryStorage | [`storage/ChatHistoryStorage.java`](../src/main/java/com/hyperfactions/storage/ChatHistoryStorage.java) |
| JsonChatHistoryStorage | [`storage/json/JsonChatHistoryStorage.java`](../src/main/java/com/hyperfactions/storage/json/JsonChatHistoryStorage.java) |
| JsonEconomyStorage | [`storage/JsonEconomyStorage.java`](../src/main/java/com/hyperfactions/storage/JsonEconomyStorage.java) |
| StorageUtils | [`storage/StorageUtils.java`](../src/main/java/com/hyperfactions/storage/StorageUtils.java) |
| DataV0ToV1Migration | [`migration/migrations/data/DataV0ToV1Migration.java`](../src/main/java/com/hyperfactions/migration/migrations/data/DataV0ToV1Migration.java) |
| DataV1ToV2Migration | [`migration/migrations/data/DataV1ToV2Migration.java`](../src/main/java/com/hyperfactions/migration/migrations/data/DataV1ToV2Migration.java) |
| MigrationRunner | [`migration/MigrationRunner.java`](../src/main/java/com/hyperfactions/migration/MigrationRunner.java) |
| BackupManager | [`backup/BackupManager.java`](../src/main/java/com/hyperfactions/backup/BackupManager.java) |
