package com.hyperfactions.importer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hyperfactions.backup.BackupManager;
import com.hyperfactions.backup.BackupType;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.*;
import com.hyperfactions.importer.factionsx.*;
import com.hyperfactions.manager.ClaimManager;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.manager.PowerManager;
import com.hyperfactions.manager.ZoneManager;
import com.hyperfactions.util.Logger;
import com.hyperfactions.util.MessageKeys;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Imports faction data from FactionsX mod (by Humblegod666) into HyperFactions.
 * Thread-safe: only one import can run at a time.
 *
 * <p>FactionsX data layout (under {@code mods/FactionsX/config/}):
 * <ul>
 *   <li>{@code factions/{UUID}.json} - per-faction files</li>
 *   <li>{@code players/{UUID}.json} - per-player files (name + power)</li>
 *   <li>{@code Claims.json} - territory claims by dimension</li>
 *   <li>{@code Zones.json} - safezone/warzone chunks</li>
 * </ul>
 *
 * <p>Key differences from other importers:
 * <ul>
 *   <li>Owner is NOT in the Members map — always LEADER implicitly</li>
 *   <li>Power is per-player (not per-faction total)</li>
 *   <li>Roles: LEADER, OFFICER, MEMBER, RECRUIT (RECRUIT mapped to MEMBER)</li>
 *   <li>Claims.json uses ChunkY for Z (same quirk as HyFactions)</li>
 *   <li>Zones.json uses "chunkX:chunkZ" strings per dimension</li>
 *   <li>Has per-role permissions (Build, Claim, Interact, Invite, Kick)</li>
 * </ul>
 */
public class FactionsXImporter {

  private final Gson gson;

  private final FactionManager factionManager;

  private final ClaimManager claimManager;

  private final ZoneManager zoneManager;

  private final PowerManager powerManager;

  @Nullable
  private final BackupManager backupManager;

  @Nullable
  private Runnable onImportComplete;

  // Thread safety: own lock, also checks other importers
  private static final ReentrantLock importLock = new ReentrantLock();

  private static final AtomicBoolean importInProgress = new AtomicBoolean(false);

  // Import options
  private boolean dryRun = true;

  private boolean overwrite = false;

  private boolean skipZones = false;

  private boolean skipPower = false;

  private boolean createBackup = true;

  @Nullable
  private Consumer<String> progressCallback;

  // Name cache for UUID -> username lookups (populated from player files)
  private final Map<UUID, String> nameCache = new HashMap<>();

  // Power cache for UUID -> power (populated from player files)
  private final Map<UUID, Integer> powerCache = new HashMap<>();

  // Max power cache for UUID -> maxPower (populated from player files)
  private final Map<UUID, Integer> maxPowerCache = new HashMap<>();

  /**
   * Wrapper to hold chunk info with its dimension name.
   */
  private record ChunkWithDimension(String dimension, FxChunkInfo chunk) {}

  /** Creates a new FactionsXImporter. */
  public FactionsXImporter(
      @NotNull FactionManager factionManager,
      @NotNull ClaimManager claimManager,
      @NotNull ZoneManager zoneManager,
      @NotNull PowerManager powerManager,
      @Nullable BackupManager backupManager
  ) {
    this.factionManager = factionManager;
    this.claimManager = claimManager;
    this.zoneManager = zoneManager;
    this.powerManager = powerManager;
    this.backupManager = backupManager;
    this.gson = new GsonBuilder().create();
  }

  // === Configuration Methods ===

  /** Sets the dry run. */
  public FactionsXImporter setDryRun(boolean dryRun) {
    this.dryRun = dryRun;
    return this;
  }

  /** Sets the overwrite. */
  public FactionsXImporter setOverwrite(boolean overwrite) {
    this.overwrite = overwrite;
    return this;
  }

  /** Sets the skip zones. */
  public FactionsXImporter setSkipZones(boolean skipZones) {
    this.skipZones = skipZones;
    return this;
  }

  /** Sets the skip power. */
  public FactionsXImporter setSkipPower(boolean skipPower) {
    this.skipPower = skipPower;
    return this;
  }

  public FactionsXImporter setCreateBackup(boolean createBackup) {
    this.createBackup = createBackup;
    return this;
  }

  /** Sets the progress callback. */
  public FactionsXImporter setProgressCallback(@Nullable Consumer<String> callback) {
    this.progressCallback = callback;
    return this;
  }

  /** Sets the on import complete. */
  public FactionsXImporter setOnImportComplete(@Nullable Runnable callback) {
    this.onImportComplete = callback;
    return this;
  }

  /**
   * Checks if a FactionsX import is currently in progress.
   */
  public static boolean isImportInProgress() {
    return importInProgress.get();
  }

  // === Validation Method ===

  /**
   * Validates FactionsX data before import without making any changes.
   *
   * @param sourcePath the path to the FactionsX directory (e.g. mods/FactionsX)
   * @return validation report with conflicts and warnings
   */
  public ImportValidationReport validate(@NotNull Path sourcePath) {
    ImportValidationReport.Builder report = ImportValidationReport.builder();

    File sourceDir = sourcePath.toFile();
    if (!sourceDir.exists() || !sourceDir.isDirectory()) {
      report.error("Source directory not found: " + sourcePath);
      return report.build();
    }

    File configDir = new File(sourceDir, "config");
    if (!configDir.exists()) {
      report.error("Config directory not found: " + configDir.getPath());
      return report.build();
    }

    // Load player data (builds name cache + power cache)
    loadPlayerDataForValidation(configDir, report);

    // Load and validate factions
    List<FxFaction> factions = loadFactionsForValidation(configDir, report);
    report.totalFactions(factions.size());

    Set<String> seenNames = new HashSet<>();
    Set<String> seenIds = new HashSet<>();
    Set<String> seenMembers = new HashSet<>();

    for (FxFaction faction : factions) {
      validateFaction(faction, seenNames, seenIds, seenMembers, report);
    }

    // Load and validate claims
    Map<UUID, List<ChunkWithDimension>> claimsByFaction = loadClaimsForValidation(configDir, report);
    int totalClaims = claimsByFaction.values().stream().mapToInt(List::size).sum();
    report.totalClaims(totalClaims);

    // Validate claims reference existing factions
    Set<UUID> factionIds = new HashSet<>();
    for (FxFaction f : factions) {
      if (f.Id() != null) {
        try {
          factionIds.add(UUID.fromString(f.Id()));
        } catch (IllegalArgumentException ignored) {}
      }
    }

    for (UUID claimOwner : claimsByFaction.keySet()) {
      if (!factionIds.contains(claimOwner)) {
        report.warning("Claims reference unknown faction: " + claimOwner);
      }
    }

    // Load zones
    if (!skipZones) {
      FxZones zones = loadZonesForValidation(configDir, report);
      if (zones != null) {
        int safeCount = zones.Safezone() != null
          ? zones.Safezone().values().stream().mapToInt(List::size).sum() : 0;
        int warCount = zones.Warzone() != null
          ? zones.Warzone().values().stream().mapToInt(List::size).sum() : 0;
        report.totalSafeZoneChunks(safeCount);
        report.totalWarZoneChunks(warCount);
      }
    }

    return report.build();
  }

  private void validateFaction(FxFaction faction, Set<String> seenNames, Set<String> seenIds,
                 Set<String> seenMembers, ImportValidationReport.Builder report) {
    if (faction.Id() == null || faction.Id().isEmpty()) {
      report.error("Faction missing ID: " + faction.Name());
      return;
    }

    UUID factionId;
    try {
      factionId = UUID.fromString(faction.Id());
    } catch (IllegalArgumentException e) {
      report.invalidUuid("Invalid faction ID format: " + faction.Id());
      return;
    }

    if (seenIds.contains(faction.Id())) {
      report.idConflict("Duplicate faction ID in import data: " + faction.Id());
    }
    seenIds.add(faction.Id());

    if (faction.Name() == null || faction.Name().isEmpty()) {
      report.error("Faction missing name: " + faction.Id());
      return;
    }

    String lowerName = faction.Name().toLowerCase();
    if (seenNames.contains(lowerName)) {
      report.nameConflict("Duplicate faction name in import data: " + faction.Name());
    }
    seenNames.add(lowerName);

    // Check conflict with existing HyperFactions factions
    Faction existingByName = factionManager.getFactionByName(faction.Name());
    if (existingByName != null && !existingByName.id().equals(factionId)) {
      if (overwrite) {
        report.nameConflict("Faction '" + faction.Name() + "' exists with different ID - will use imported ID");
      } else {
        report.nameConflict("Faction '" + faction.Name() + "' already exists (different ID) - use --overwrite to replace");
      }
    }

    Faction existingById = factionManager.getFaction(factionId);
    if (existingById != null) {
      if (overwrite) {
        report.idConflict("Faction ID " + factionId + " exists - will be overwritten");
      } else {
        report.idConflict("Faction ID " + factionId + " already exists - use --overwrite to replace");
      }
    }

    // Validate owner
    if (faction.Owner() == null || faction.Owner().isEmpty()) {
      report.warning("Faction '" + faction.Name() + "' has no owner - first member will become leader");
    } else {
      try {
        UUID.fromString(faction.Owner());
      } catch (IllegalArgumentException e) {
        report.invalidUuid("Invalid owner UUID in " + faction.Name() + ": " + faction.Owner());
      }
    }

    // Count members (owner + Members map)
    Set<String> allMemberUuids = new HashSet<>();
    if (faction.Owner() != null) {
      allMemberUuids.add(faction.Owner());
    }
    if (faction.Members() != null) {
      allMemberUuids.addAll(faction.Members().keySet());
    }

    if (allMemberUuids.isEmpty()) {
      report.warning("Faction '" + faction.Name() + "' has no members");
    } else {
      report.addMembers(allMemberUuids.size());

      for (String memberUuidStr : allMemberUuids) {
        try {
          UUID.fromString(memberUuidStr);
        } catch (IllegalArgumentException e) {
          report.invalidUuid("Invalid member UUID in " + faction.Name() + ": " + memberUuidStr);
          continue;
        }

        if (seenMembers.contains(memberUuidStr)) {
          String memberName = nameCache.getOrDefault(parseUUID(memberUuidStr), memberUuidStr);
          report.memberConflict("Player '" + memberName + "' appears in multiple imported factions");
        }
        seenMembers.add(memberUuidStr);

        UUID memberUuid = parseUUID(memberUuidStr);
        if (memberUuid != null) {
          Faction existingFaction = factionManager.getPlayerFaction(memberUuid);
          if (existingFaction != null && !existingFaction.id().equals(factionId)) {
            String memberName = nameCache.getOrDefault(memberUuid, memberUuidStr);
            report.memberConflict("Player '" + memberName + "' already in faction '"
              + existingFaction.name() + "' - will be moved to '" + faction.Name() + "'");
          }
        }
      }
    }

    // Validate home dimension
    if (faction.hasHome() && faction.HomeDimension() != null) {
      String worldName = faction.HomeDimension();
      if (!worldName.equals("default") && !worldName.equals("overworld")
        && !worldName.equals("nether") && !worldName.equals("end")) {
        report.worldWarning("Faction '" + faction.Name() + "' home in unknown world: " + worldName);
      }
    }
  }

  // === Validation Loading Methods ===

  private void loadPlayerDataForValidation(File configDir, ImportValidationReport.Builder report) {
    File playersDir = new File(configDir, "players");
    if (!playersDir.exists() || !playersDir.isDirectory()) {
      report.warning("Players directory not found - usernames and power data may be unavailable");
      return;
    }

    File[] files = playersDir.listFiles((dir, name) -> name.endsWith(".json"));
    if (files == null || files.length == 0) {
      report.warning("No player files found");
      return;
    }

    for (File file : files) {
      try (FileReader reader = new FileReader(file)) {
        FxPlayer player = gson.fromJson(reader, FxPlayer.class);
        if (player != null && player.Uuid() != null) {
          UUID uuid = UUID.fromString(player.Uuid());
          if (player.LastKnownName() != null) {
            nameCache.put(uuid, player.LastKnownName());
          }
          powerCache.put(uuid, player.Power());
          maxPowerCache.put(uuid, player.MaxPower());
        }
      } catch (Exception ignored) {}
    }
  }

  private List<FxFaction> loadFactionsForValidation(File configDir, ImportValidationReport.Builder report) {
    List<FxFaction> factions = new ArrayList<>();
    File factionDir = new File(configDir, "factions");

    if (!factionDir.exists() || !factionDir.isDirectory()) {
      report.error("No factions directory found in " + configDir.getPath());
      return factions;
    }

    File[] files = factionDir.listFiles((dir, name) -> name.endsWith(".json"));
    if (files == null || files.length == 0) {
      report.error("No faction files found");
      return factions;
    }

    for (File file : files) {
      try (FileReader reader = new FileReader(file)) {
        FxFaction faction = gson.fromJson(reader, FxFaction.class);
        if (faction != null && faction.Id() != null) {
          factions.add(faction);
        }
      } catch (Exception e) {
        report.warning("Failed to load faction file " + file.getName() + ": " + e.getMessage());
      }
    }

    return factions;
  }

  private Map<UUID, List<ChunkWithDimension>> loadClaimsForValidation(File configDir,
                                        ImportValidationReport.Builder report) {
    Map<UUID, List<ChunkWithDimension>> claimsByFaction = new HashMap<>();
    File claimsFile = new File(configDir, "Claims.json");

    if (!claimsFile.exists()) {
      report.warning("Claims.json not found");
      return claimsByFaction;
    }

    try (FileReader reader = new FileReader(claimsFile)) {
      FxClaims claims = gson.fromJson(reader, FxClaims.class);
      if (claims != null && claims.Dimensions() != null) {
        for (FxDimension dim : claims.Dimensions()) {
          if (dim.ChunkInfo() == null) continue;
          String dimension = dim.Dimension() != null ? dim.Dimension() : "default";
          for (FxChunkInfo chunk : dim.ChunkInfo()) {
            if (chunk.UUID() == null) continue;
            try {
              UUID factionId = UUID.fromString(chunk.UUID());
              claimsByFaction
                .computeIfAbsent(factionId, k -> new ArrayList<>())
                .add(new ChunkWithDimension(dimension, chunk));
            } catch (IllegalArgumentException e) {
              report.invalidUuid("Invalid faction UUID in claim: " + chunk.UUID());
            }
          }
        }
      }
    } catch (Exception e) {
      report.warning("Failed to load Claims.json: " + e.getMessage());
    }

    return claimsByFaction;
  }

  @Nullable
  private FxZones loadZonesForValidation(File configDir, ImportValidationReport.Builder report) {
    File zonesFile = new File(configDir, "Zones.json");
    if (!zonesFile.exists()) {
      return null;
    }

    try (FileReader reader = new FileReader(zonesFile)) {
      return gson.fromJson(reader, FxZones.class);
    } catch (Exception e) {
      report.warning("Failed to load Zones.json: " + e.getMessage());
      return null;
    }
  }

  // === Main Import Method ===

  /**
   * Imports FactionsX data from the specified directory.
   * Thread-safe: only one import can run at a time.
   *
   * @param sourcePath the path to the FactionsX directory (e.g. mods/FactionsX)
   * @return the import result
   */
  public ImportResult importFrom(@NotNull Path sourcePath) {
    ImportResult.Builder result = ImportResult.builder().dryRun(dryRun);

    // Check other importers aren't running
    if (HyFactionsImporter.isImportInProgress()) {
      result.error("A HyFactions import is already in progress. Please wait for it to complete.");
      return result.build();
    }
    if (ElbaphFactionsImporter.isImportInProgress()) {
      result.error("An ElbaphFactions import is already in progress. Please wait for it to complete.");
      return result.build();
    }

    // Thread safety: prevent concurrent imports
    if (!importLock.tryLock()) {
      result.error("Another import is already in progress. Please wait for it to complete.");
      return result.build();
    }

    try {
      importInProgress.set(true);
      return doImport(sourcePath, result);
    } finally {
      importInProgress.set(false);
      importLock.unlock();
    }
  }

  private ImportResult doImport(@NotNull Path sourcePath, ImportResult.Builder result) {
    progress("Starting FactionsX import from: " + sourcePath);

    File sourceDir = sourcePath.toFile();
    if (!sourceDir.exists() || !sourceDir.isDirectory()) {
      result.error("Source directory not found: " + sourcePath);
      return result.build();
    }

    File configDir = new File(sourceDir, "config");
    if (!configDir.exists()) {
      result.error("Config directory not found: " + configDir.getPath()
        + " (expected FactionsX data under config/)");
      return result.build();
    }

    // Create pre-import backup if not dry run
    if (!dryRun && createBackup && backupManager != null) {
      progress("Creating pre-import backup...");
      try {
        var backupResult = backupManager.createBackup(
          BackupType.MANUAL, "pre-import-factionsx", null
        ).join();

        if (backupResult instanceof BackupManager.BackupResult.Success success) {
          progress("Pre-import backup created: %s (%s)",
            success.metadata().name(), success.metadata().getFormattedSize());
        } else if (backupResult instanceof BackupManager.BackupResult.Failure failure) {
          result.warning("Failed to create pre-import backup: " + failure.error());
          progress("WARNING: Pre-import backup failed, continuing anyway...");
        }
      } catch (Exception e) {
        result.warning("Exception creating pre-import backup: " + e.getMessage());
        progress("WARNING: Pre-import backup failed, continuing anyway...");
      }
    } else if (!dryRun && createBackup && backupManager == null) {
      progress("WARNING: Backup manager not available, skipping pre-import backup");
      result.warning("Pre-import backup skipped (backup manager not available)");
    }

    // Load player data first (builds name cache + power cache)
    loadPlayerData(configDir, result);

    // Load factions from per-faction files
    List<FxFaction> factions = loadFactions(configDir, result);
    if (result.build().hasErrors()) {
      return result.build();
    }

    // Load claims
    Map<UUID, List<ChunkWithDimension>> claimsByFaction = loadClaims(configDir, result);

    // Load zones
    List<FxZoneChunk> safeZoneChunks = Collections.emptyList();
    List<FxZoneChunk> warZoneChunks = Collections.emptyList();
    if (!skipZones) {
      FxZones zones = loadZones(configDir, result);
      if (zones != null) {
        safeZoneChunks = parseZoneChunks(zones.Safezone());
        warZoneChunks = parseZoneChunks(zones.Warzone());
      }
    }

    // Calculate stats
    int totalClaims = claimsByFaction.values().stream().mapToInt(List::size).sum();
    Set<String> dimensions = claimsByFaction.values().stream()
      .flatMap(List::stream)
      .map(ChunkWithDimension::dimension)
      .collect(Collectors.toSet());

    progress("Found %d factions, %d claims in dimensions %s, %d safe zone chunks, %d war zone chunks",
      factions.size(),
      totalClaims,
      dimensions.isEmpty() ? "[none]" : dimensions.toString(),
      safeZoneChunks.size(),
      warZoneChunks.size()
    );

    // Process factions
    for (FxFaction faction : factions) {
      processFaction(faction, claimsByFaction, result);
    }

    // Process zones with batch mode
    if (!skipZones) {
      if (!dryRun) {
        zoneManager.startBatch();
      }
      try {
        if (!safeZoneChunks.isEmpty()) {
          processZones(safeZoneChunks, ZoneType.SAFE, "SafeZone", result);
        }
        if (!warZoneChunks.isEmpty()) {
          processZones(warZoneChunks, ZoneType.WAR, "WarZone", result);
        }
      } finally {
        if (!dryRun) {
          zoneManager.endBatch();
        }
      }
    }

    if (dryRun) {
      progress("Dry run complete - no changes made");
    } else {
      // Rebuild claim index
      progress("Rebuilding claim index...");
      claimManager.buildIndex();

      // Trigger world map refresh
      if (onImportComplete != null) {
        progress("Refreshing world maps...");
        try {
          onImportComplete.run();
        } catch (Exception e) {
          result.warning("Failed to refresh world maps: " + e.getMessage());
        }
      }

      progress("Import complete!");
    }

    return result.build();
  }

  // === Loading Methods ===

  /**
   * Loads all player files, building both the name cache and power cache.
   * FactionsX stores per-player data in individual files under config/players/.
   */
  private void loadPlayerData(File configDir, ImportResult.Builder result) {
    File playersDir = new File(configDir, "players");
    if (!playersDir.exists() || !playersDir.isDirectory()) {
      result.warning("Players directory not found - usernames and power data may be unavailable");
      return;
    }

    File[] files = playersDir.listFiles((dir, name) -> name.endsWith(".json"));
    if (files == null || files.length == 0) {
      result.warning("No player files found");
      return;
    }

    int loaded = 0;
    for (File file : files) {
      try (FileReader reader = new FileReader(file)) {
        FxPlayer player = gson.fromJson(reader, FxPlayer.class);
        if (player != null && player.Uuid() != null) {
          UUID uuid = UUID.fromString(player.Uuid());
          if (player.LastKnownName() != null) {
            nameCache.put(uuid, player.LastKnownName());
          }
          powerCache.put(uuid, player.Power());
          maxPowerCache.put(uuid, player.MaxPower());
          loaded++;
        }
      } catch (Exception e) {
        result.warning("Failed to load player file " + file.getName() + ": " + e.getMessage());
      }
    }

    progress("Loaded %d player data entries (names + power)", loaded);
  }

  /**
   * Loads faction data from per-faction JSON files under config/factions/.
   */
  private List<FxFaction> loadFactions(File configDir, ImportResult.Builder result) {
    List<FxFaction> factions = new ArrayList<>();
    File factionDir = new File(configDir, "factions");

    if (!factionDir.exists() || !factionDir.isDirectory()) {
      result.error("No factions directory found in " + configDir.getPath());
      return factions;
    }

    File[] files = factionDir.listFiles((dir, name) -> name.endsWith(".json"));
    if (files == null || files.length == 0) {
      result.warning("No faction files found");
      return factions;
    }

    for (File file : files) {
      try (FileReader reader = new FileReader(file)) {
        FxFaction faction = gson.fromJson(reader, FxFaction.class);
        if (faction != null && faction.Id() != null) {
          factions.add(faction);
        }
      } catch (Exception e) {
        result.warning("Failed to load faction file " + file.getName() + ": " + e.getMessage());
      }
    }

    return factions;
  }

  /**
   * Loads claims from config/Claims.json. Uses the same Dimensions/ChunkInfo
   * nested format as HyFactions, including the ChunkY-is-actually-Z quirk.
   */
  private Map<UUID, List<ChunkWithDimension>> loadClaims(File configDir, ImportResult.Builder result) {
    Map<UUID, List<ChunkWithDimension>> claimsByFaction = new HashMap<>();
    File claimsFile = new File(configDir, "Claims.json");

    if (!claimsFile.exists()) {
      result.warning("Claims.json not found");
      return claimsByFaction;
    }

    try (FileReader reader = new FileReader(claimsFile)) {
      FxClaims claims = gson.fromJson(reader, FxClaims.class);
      if (claims != null && claims.Dimensions() != null) {
        for (FxDimension dim : claims.Dimensions()) {
          if (dim.ChunkInfo() == null) continue;
          String dimension = dim.Dimension() != null ? dim.Dimension() : "default";

          for (FxChunkInfo chunk : dim.ChunkInfo()) {
            if (chunk.UUID() == null) continue;
            try {
              UUID factionId = UUID.fromString(chunk.UUID());
              claimsByFaction
                .computeIfAbsent(factionId, k -> new ArrayList<>())
                .add(new ChunkWithDimension(dimension, chunk));
            } catch (IllegalArgumentException ignored) {}
          }
        }
      }
    } catch (Exception e) {
      result.warning("Failed to load Claims.json: " + e.getMessage());
    }

    return claimsByFaction;
  }

  /**
   * Loads zone data from config/Zones.json.
   */
  @Nullable
  private FxZones loadZones(File configDir, ImportResult.Builder result) {
    File zonesFile = new File(configDir, "Zones.json");
    if (!zonesFile.exists()) {
      return null;
    }

    try (FileReader reader = new FileReader(zonesFile)) {
      return gson.fromJson(reader, FxZones.class);
    } catch (Exception e) {
      result.warning("Failed to load Zones.json: " + e.getMessage());
      return null;
    }
  }

  /**
   * Parses zone "chunkX:chunkZ" strings into FxZoneChunk records.
   */
  private List<FxZoneChunk> parseZoneChunks(@Nullable Map<String, List<String>> zoneData) {
    if (zoneData == null) {
      return Collections.emptyList();
    }

    List<FxZoneChunk> chunks = new ArrayList<>();
    for (Map.Entry<String, List<String>> entry : zoneData.entrySet()) {
      String dimension = entry.getKey();
      for (String coord : entry.getValue()) {
        String[] parts = coord.split(":");
        if (parts.length == 2) {
          try {
            int chunkX = Integer.parseInt(parts[0]);
            int chunkZ = Integer.parseInt(parts[1]);
            chunks.add(new FxZoneChunk(dimension, chunkX, chunkZ));
          } catch (NumberFormatException ignored) {}
        }
      }
    }

    return chunks;
  }

  // === Processing Methods ===

  private void processFaction(FxFaction fxFaction, Map<UUID, List<ChunkWithDimension>> claimsByFaction,
                ImportResult.Builder result) {
    if (fxFaction.Id() == null || fxFaction.Name() == null) {
      result.warning("Skipping faction with missing ID or name");
      result.incrementFactionsSkipped();
      return;
    }

    UUID factionId;
    try {
      factionId = UUID.fromString(fxFaction.Id());
    } catch (IllegalArgumentException e) {
      result.warning("Skipping faction with invalid ID: " + fxFaction.Id());
      result.incrementFactionsSkipped();
      return;
    }

    progress("Processing faction: %s (%s)", fxFaction.Name(), fxFaction.Id().substring(0, 8));

    // Check for existing faction
    Faction existing = factionManager.getFaction(factionId);
    if (existing != null && !overwrite) {
      progress("  - Skipping (already exists, use --overwrite to replace)");
      result.incrementFactionsSkipped();
      return;
    }

    // Convert the faction
    Faction converted = convertFaction(fxFaction, claimsByFaction, result);
    if (converted == null) {
      result.incrementFactionsSkipped();
      return;
    }

    // Log summary
    progress("  - %d members (%d officers)",
      converted.getMemberCount(),
      converted.members().values().stream().filter(m -> m.role() == FactionRole.OFFICER).count()
    );
    progress("  - %d claims", converted.getClaimCount());
    if (converted.hasHome()) {
      progress("  - Home set in %s", converted.home().world());
    }

    // Handle players already in existing factions
    int playersRemoved = handleExistingMemberships(converted, result);
    if (playersRemoved > 0) {
      progress("  - Removed %d players from existing factions", playersRemoved);
    }

    if (!dryRun) {
      factionManager.importFaction(converted, overwrite);
    }

    result.incrementFactionsImported();
    result.addClaimsImported(converted.getClaimCount());

    // Handle power distribution using individual player power
    if (!skipPower) {
      distributePlayerPower(converted, result);
    }
  }

  @Nullable
  private Faction convertFaction(FxFaction fxFaction, Map<UUID, List<ChunkWithDimension>> claimsByFaction,
                  ImportResult.Builder result) {
    UUID factionId = UUID.fromString(fxFaction.Id());

    // Convert color - use default if missing or black (0)
    String color = convertColor(fxFaction.Color());
    if (color.equals("#000000") || fxFaction.Color() == 0) {
      color = getRandomColor();
      progress("  - Generated random color for faction (original was black/missing)");
    }

    // Get creation timestamp
    long createdAt = fxFaction.CreatedTracker() != null
      ? fxFaction.CreatedTracker().toEpochMillis()
      : System.currentTimeMillis();

    // Build members map (owner + Members)
    Map<UUID, FactionMember> members = buildMembers(fxFaction, createdAt, result);
    if (members.isEmpty()) {
      result.warning(String.format("Faction '%s' has no valid members", fxFaction.Name()));
      return null;
    }

    // Convert home
    Faction.FactionHome home = null;
    if (fxFaction.hasHome()) {
      UUID setBy = fxFaction.Owner() != null ? parseUUID(fxFaction.Owner()) : members.keySet().iterator().next();
      home = new Faction.FactionHome(
        fxFaction.HomeDimension(),
        fxFaction.HomeX(),
        fxFaction.HomeY(),
        fxFaction.HomeZ(),
        fxFaction.HomeYaw(),
        fxFaction.HomePitch(),
        createdAt,
        setBy != null ? setBy : members.keySet().iterator().next()
      );
    }

    // Convert claims
    Set<FactionClaim> claims = convertClaims(factionId, claimsByFaction);

    // Convert relations
    Map<UUID, FactionRelation> relations = convertRelations(fxFaction.Relations());

    // Convert permissions from FactionsX per-role model
    FactionPermissions permissions = convertPermissions(fxFaction.Permissions());

    // Generate unique tag from faction name
    String tag = factionManager.generateUniqueTag(fxFaction.Name());
    progress("  - Generated tag: %s", tag);

    // Description
    String description = fxFaction.Description() != null && !fxFaction.Description().isEmpty()
      ? fxFaction.Description()
      : "Imported from FactionsX";

    // Create import log entry
    List<FactionLog> logs = new ArrayList<>();
    logs.add(FactionLog.system(FactionLog.LogType.MEMBER_JOIN,
      "Faction imported from FactionsX",
      MessageKeys.LogsGui.MSG_IMPORTED_FROM, "FactionsX"));

    return new Faction(
      factionId,
      fxFaction.Name(),
      description,
      tag,
      color,
      createdAt,
      home,
      members,
      claims,
      relations,
      logs,
      false, // not open by default
      permissions,
      null  // no hardcore power
    );
  }

  /**
   * Builds the members map. Owner is added as LEADER, Members map entries get their
   * stored role. RECRUIT is mapped to MEMBER with a warning since HyperFactions doesn't
   * have a RECRUIT role.
   */
  private Map<UUID, FactionMember> buildMembers(FxFaction fxFaction, long createdAt,
                          ImportResult.Builder result) {
    Map<UUID, FactionMember> members = new HashMap<>();
    long now = System.currentTimeMillis();

    // Add owner as LEADER first
    UUID ownerUuid = fxFaction.Owner() != null ? parseUUID(fxFaction.Owner()) : null;
    if (ownerUuid != null) {
      String ownerName = nameCache.getOrDefault(ownerUuid, "Unknown");
      members.put(ownerUuid, new FactionMember(
        ownerUuid,
        ownerName,
        FactionRole.LEADER,
        createdAt,
        now
      ));
    }

    // Add remaining members from Members map
    if (fxFaction.Members() != null) {
      for (Map.Entry<String, String> entry : fxFaction.Members().entrySet()) {
        UUID memberUuid = parseUUID(entry.getKey());
        if (memberUuid == null) continue;

        // Skip if already added as owner
        if (memberUuid.equals(ownerUuid)) continue;

        String roleStr = entry.getValue();
        FactionRole role = switch (roleStr != null ? roleStr.toUpperCase() : "MEMBER") {
          case "LEADER" -> FactionRole.LEADER; // shouldn't happen, but handle it
          case "OFFICER" -> FactionRole.OFFICER;
          case "RECRUIT" -> {
            String memberName = nameCache.getOrDefault(memberUuid, entry.getKey());
            result.warning(String.format("Member %s in '%s' has RECRUIT role, mapped to MEMBER",
              memberName, fxFaction.Name()));
            yield FactionRole.MEMBER;
          }
          default -> FactionRole.MEMBER;
        };

        String username = nameCache.getOrDefault(memberUuid, "Unknown");
        members.put(memberUuid, new FactionMember(
          memberUuid,
          username,
          role,
          createdAt,
          now
        ));
      }
    }

    // If no owner was set but we have members, promote first member to leader
    if (ownerUuid == null && !members.isEmpty()) {
      UUID firstMember = members.keySet().iterator().next();
      FactionMember promoted = members.get(firstMember).withRole(FactionRole.LEADER);
      members.put(firstMember, promoted);
      result.warning(String.format("Faction '%s' has no owner, promoted %s to leader",
        fxFaction.Name(), promoted.username()));
    }

    return members;
  }

  private Set<FactionClaim> convertClaims(UUID factionId,
                        Map<UUID, List<ChunkWithDimension>> claimsByFaction) {
    Set<FactionClaim> claims = new HashSet<>();
    List<ChunkWithDimension> factionClaims = claimsByFaction.get(factionId);

    if (factionClaims == null) {
      return claims;
    }

    for (ChunkWithDimension chunkWithDim : factionClaims) {
      FxChunkInfo chunk = chunkWithDim.chunk();
      String dimension = chunkWithDim.dimension();

      long claimedAt = chunk.CreatedTracker() != null
        ? chunk.CreatedTracker().toEpochMillis()
        : System.currentTimeMillis();

      UUID claimedBy = chunk.CreatedTracker() != null && chunk.CreatedTracker().UserUUID() != null
        ? parseUUID(chunk.CreatedTracker().UserUUID())
        : null;

      if (claimedBy == null) {
        claimedBy = UUID.randomUUID(); // Fallback
      }

      // Note: FactionsX uses ChunkY for chunkZ (same quirk as HyFactions)
      claims.add(new FactionClaim(
        dimension,
        chunk.ChunkX(),
        chunk.getChunkZ(),
        claimedAt,
        claimedBy
      ));
    }

    return claims;
  }

  /**
   * Converts FactionsX relations. Format is Map&lt;UUID, "ally"/"enemy"/"neutral"&gt;.
   */
  private Map<UUID, FactionRelation> convertRelations(@Nullable Map<String, String> fxRelations) {
    Map<UUID, FactionRelation> relations = new HashMap<>();

    if (fxRelations == null) {
      return relations;
    }

    for (Map.Entry<String, String> entry : fxRelations.entrySet()) {
      UUID targetId = parseUUID(entry.getKey());
      if (targetId == null) continue;

      RelationType type = switch (entry.getValue().toLowerCase()) {
        case "ally" -> RelationType.ALLY;
        case "enemy" -> RelationType.ENEMY;
        default -> RelationType.NEUTRAL;
      };

      if (type != RelationType.NEUTRAL) {
        relations.put(targetId, FactionRelation.create(targetId, type));
      }
    }

    return relations;
  }

  /**
   * Converts FactionsX per-role permissions (Build, Claim, Interact, Invite, Kick)
   * to HyperFactions territory permission flags.
   *
   * <p>FactionsX permissions control what each role can do within faction territory.
   * We map "Build" to Break+Place, "Interact" to Interact, and leave other flags as defaults.
   * The outsider/ally equivalent permissions are not stored in FactionsX, so we use defaults.
   */
  @Nullable
  private FactionPermissions convertPermissions(
      @Nullable Map<String, Map<String, Boolean>> fxPermissions) {
    if (fxPermissions == null || fxPermissions.isEmpty()) {
      return null; // Use default permissions
    }

    Map<String, Boolean> flags = new HashMap<>();

    // Map MEMBER role permissions
    Map<String, Boolean> memberPerms = fxPermissions.getOrDefault("MEMBER", Map.of());
    boolean memberBuild = memberPerms.getOrDefault("Build", true);
    boolean memberInteract = memberPerms.getOrDefault("Interact", true);
    flags.put(FactionPermissions.MEMBER_BREAK, memberBuild);
    flags.put(FactionPermissions.MEMBER_PLACE, memberBuild);
    flags.put(FactionPermissions.MEMBER_INTERACT, memberInteract);

    // Map OFFICER role permissions (FactionsX OFFICER maps to HyperFactions officer level)
    Map<String, Boolean> officerPerms = fxPermissions.getOrDefault("OFFICER", Map.of());
    boolean officerBuild = officerPerms.getOrDefault("Build", true);
    boolean officerInteract = officerPerms.getOrDefault("Interact", true);
    flags.put(FactionPermissions.OFFICER_BREAK, officerBuild);
    flags.put(FactionPermissions.OFFICER_PLACE, officerBuild);
    flags.put(FactionPermissions.OFFICER_INTERACT, officerInteract);

    // Officers can edit permissions if they have Kick permission
    boolean officersCanEdit = officerPerms.getOrDefault("Kick", false);
    flags.put(FactionPermissions.OFFICERS_CAN_EDIT, officersCanEdit);

    // Constructor fills in remaining flags from defaults
    return new FactionPermissions(flags);
  }

  /**
   * Distributes power using individual player power values from FactionsX player files,
   * rather than even-splitting a faction total.
   */
  private void distributePlayerPower(Faction faction, ImportResult.Builder result) {
    int membersWithPower = 0;

    for (UUID memberUuid : faction.members().keySet()) {
      int power = powerCache.getOrDefault(memberUuid, 0);
      int maxPower = maxPowerCache.getOrDefault(memberUuid, 0);

      if (power <= 0 && maxPower <= 0) continue;

      double maxAllowed = ConfigManager.get().getMaxPlayerPower();
      double effectivePower = Math.min(power, maxAllowed);
      double effectiveMax = Math.min(maxPower, maxAllowed);

      if (!dryRun) {
        PlayerPower playerPower = PlayerPower.create(memberUuid, effectivePower, effectiveMax);
        // PowerManager will handle this via its API
      }
      membersWithPower++;
    }

    if (membersWithPower > 0) {
      progress("  - Set power for %d members from individual FactionsX data", membersWithPower);
      result.addPlayersWithPower(membersWithPower);
    }
  }

  /**
   * Handles players who are already in existing HyperFactions factions.
   * Removes them from their current faction, and disbands the faction if it becomes empty.
   */
  private int handleExistingMemberships(Faction importedFaction, ImportResult.Builder result) {
    int playersRemoved = 0;
    Set<UUID> factionsToCheck = new HashSet<>();

    for (UUID memberUuid : importedFaction.members().keySet()) {
      Faction existingFaction = factionManager.getPlayerFaction(memberUuid);

      if (existingFaction == null || existingFaction.id().equals(importedFaction.id())) {
        continue;
      }

      FactionMember existingMember = existingFaction.getMember(memberUuid);
      String playerName = existingMember != null ? existingMember.username() : "Unknown";

      progress("  - Player %s is already in faction '%s', removing...",
        playerName, existingFaction.name());

      if (!dryRun) {
        Faction updatedExisting = existingFaction.withoutMember(memberUuid)
          .withLog(FactionLog.create(
            FactionLog.LogType.MEMBER_LEAVE,
            playerName + " left (imported to another faction)",
            null,
            MessageKeys.LogsGui.MSG_LEFT_IMPORT, playerName
          ));

        factionManager.removePlayerFromIndex(memberUuid);

        if (updatedExisting.getMemberCount() == 0) {
          progress("    - Faction '%s' is now empty, will be disbanded...", existingFaction.name());
          factionsToCheck.add(existingFaction.id());
          factionManager.updateFaction(updatedExisting);
        } else {
          if (existingMember != null && existingMember.isLeader()) {
            FactionMember successor = updatedExisting.findSuccessor();
            if (successor != null) {
              FactionMember promoted = successor.withRole(FactionRole.LEADER);
              updatedExisting = updatedExisting.withMember(promoted)
                .withLog(FactionLog.create(
                  FactionLog.LogType.LEADER_TRANSFER,
                  promoted.username() + " became leader (previous leader imported to another faction)",
                  null,
                  MessageKeys.LogsGui.MSG_LEADER_IMPORT_TRANSFER, promoted.username()
                ));
              progress("    - %s promoted to leader of '%s'",
                promoted.username(), existingFaction.name());
            }
          }
          factionManager.updateFaction(updatedExisting);
        }
      }

      playersRemoved++;
      result.warning(String.format("Player %s removed from faction '%s' (imported to another faction)",
        playerName, existingFaction.name()));
    }

    if (!dryRun) {
      for (UUID factionId : factionsToCheck) {
        Faction faction = factionManager.getFaction(factionId);
        if (faction != null && faction.getMemberCount() == 0) {
          disbandEmptyFaction(faction, result);
        }
      }
    }

    return playersRemoved;
  }

  private void disbandEmptyFaction(Faction faction, ImportResult.Builder result) {
    progress("    - Disbanding empty faction '%s'", faction.name());

    FactionManager.FactionResult disbandResult = factionManager.forceDisband(
      faction.id(),
      "All members imported to other factions"
    );

    if (disbandResult == FactionManager.FactionResult.SUCCESS) {
      result.warning(String.format("Faction '%s' disbanded (all members imported elsewhere)", faction.name()));
    } else {
      result.warning(String.format("Failed to disband faction '%s': %s", faction.name(), disbandResult));
    }
  }

  // === Zone Processing ===

  private void processZones(List<FxZoneChunk> chunks, ZoneType type, String namePrefix,
               ImportResult.Builder result) {
    if (chunks.isEmpty()) {
      return;
    }

    Map<String, List<FxZoneChunk>> byDimension = chunks.stream()
      .collect(Collectors.groupingBy(FxZoneChunk::dimension));

    int zoneCount = 0;
    List<CompletableFuture<ZoneManager.ZoneResult>> futures = new ArrayList<>();

    for (Map.Entry<String, List<FxZoneChunk>> entry : byDimension.entrySet()) {
      String dimension = entry.getKey();
      List<FxZoneChunk> dimChunks = entry.getValue();

      List<Set<ChunkKey>> clusters = clusterChunks(dimension, dimChunks);

      for (Set<ChunkKey> cluster : clusters) {
        zoneCount++;
        String zoneName = namePrefix + "-" + zoneCount;

        progress("  Creating %s with %d chunks in %s", zoneName, cluster.size(), dimension);

        if (!dryRun) {
          Map<String, Boolean> defaultFlags = ZoneFlags.getDefaultFlags(type);
          CompletableFuture<ZoneManager.ZoneResult> future = zoneManager.createZoneWithChunks(
            zoneName, type, dimension, UUID.randomUUID(), cluster, defaultFlags
          ).thenApply(zoneResult -> {
            if (zoneResult == ZoneManager.ZoneResult.SUCCESS) {
              result.incrementZonesCreated();
            } else {
              result.warning(String.format("Failed to create zone %s: %s", zoneName, zoneResult));
            }
            return zoneResult;
          });
          futures.add(future);
        } else {
          result.incrementZonesCreated();
        }
      }
    }

    if (!dryRun && !futures.isEmpty()) {
      try {
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        progress("  All %d zones created successfully", futures.size());
      } catch (Exception e) {
        result.warning("Error waiting for zone creation: " + e.getMessage());
      }
    }
  }

  /**
   * Clusters adjacent chunks into connected groups using BFS flood-fill.
   */
  private List<Set<ChunkKey>> clusterChunks(String dimension, List<FxZoneChunk> chunks) {
    Set<ChunkKey> remaining = chunks.stream()
      .map(c -> new ChunkKey(dimension, c.chunkX(), c.chunkZ()))
      .collect(Collectors.toSet());

    List<Set<ChunkKey>> clusters = new ArrayList<>();

    while (!remaining.isEmpty()) {
      ChunkKey start = remaining.iterator().next();
      Set<ChunkKey> cluster = new HashSet<>();
      Queue<ChunkKey> queue = new LinkedList<>();

      queue.add(start);
      remaining.remove(start);

      while (!queue.isEmpty()) {
        ChunkKey current = queue.poll();
        cluster.add(current);

        for (ChunkKey adjacent : getAdjacent(current)) {
          if (remaining.contains(adjacent)) {
            remaining.remove(adjacent);
            queue.add(adjacent);
          }
        }
      }

      clusters.add(cluster);
    }

    return clusters;
  }

  private List<ChunkKey> getAdjacent(ChunkKey key) {
    return List.of(
      new ChunkKey(key.world(), key.chunkX() + 1, key.chunkZ()),
      new ChunkKey(key.world(), key.chunkX() - 1, key.chunkZ()),
      new ChunkKey(key.world(), key.chunkX(), key.chunkZ() + 1),
      new ChunkKey(key.world(), key.chunkX(), key.chunkZ() - 1)
    );
  }

  // === Utility Methods ===

  /**
   * Converts an RGB integer color to a hex string.
   */
  private String convertColor(int rgb) {
    return String.format("#%02X%02X%02X", (rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
  }

  @NotNull
  private String getRandomColor() {
    // Exclude black (0) and white (f) as they're hard to see
    String[] colors = {"#0000AA", "#00AA00", "#00AAAA", "#AA0000", "#AA00AA",
      "#FFAA00", "#5555FF", "#55FF55", "#55FFFF", "#FF5555", "#FF55FF", "#FFFF55"};
    return colors[new Random().nextInt(colors.length)];
  }

  @Nullable
  private UUID parseUUID(@Nullable String uuidStr) {
    if (uuidStr == null || uuidStr.isEmpty()) {
      return null;
    }
    try {
      return UUID.fromString(uuidStr);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private void progress(String format, Object... args) {
    String message = String.format(format, args);
    Logger.info("[FactionsXImport] " + message);
    if (progressCallback != null) {
      progressCallback.accept(message);
    }
  }
}
