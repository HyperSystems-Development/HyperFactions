package com.hyperfactions.importer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hyperfactions.backup.BackupManager;
import com.hyperfactions.backup.BackupType;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.*;
import com.hyperfactions.importer.hyfactions.*;
import com.hyperfactions.manager.ClaimManager;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.manager.PowerManager;
import com.hyperfactions.manager.ZoneManager;
import com.hyperfactions.util.Logger;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Imports faction data from HyFactions mod into HyperFactions.
 * Thread-safe: only one import can run at a time.
 */
public class HyFactionsImporter {

  private final Gson gson;

  private final FactionManager factionManager;

  private final ClaimManager claimManager;

  private final ZoneManager zoneManager;

  private final PowerManager powerManager;

  @Nullable
  private final BackupManager backupManager;

  // Callback to refresh world maps after import
  @Nullable
  private Runnable onImportComplete;

  // Thread safety: only one import at a time
  private static final ReentrantLock importLock = new ReentrantLock();

  private static final AtomicBoolean importInProgress = new AtomicBoolean(false);

  // Import options
  private boolean dryRun = true;

  private boolean overwrite = false;

  private boolean skipZones = false;

  private boolean skipPower = false;

  private boolean createBackup = true;

  // Progress callback
  @Nullable
  private Consumer<String> progressCallback;

  // Name cache for UUID -> username lookups
  private final Map<UUID, String> nameCache = new HashMap<>();

  /**
   * Wrapper to hold chunk info with its dimension name.
   * HyFactions stores claims per dimension, but HyFactionChunkInfo doesn't carry the dimension.
   */
  private record ChunkWithDimension(String dimension, HyFactionChunkInfo chunk) {}

  // Minecraft color codes mapped from common colors
  private static final Map<String, String> COLOR_NAMES = Map.ofEntries(
    Map.entry("0", "black"),
    Map.entry("1", "dark_blue"),
    Map.entry("2", "dark_green"),
    Map.entry("3", "dark_aqua"),
    Map.entry("4", "dark_red"),
    Map.entry("5", "dark_purple"),
    Map.entry("6", "gold"),
    Map.entry("7", "gray"),
    Map.entry("8", "dark_gray"),
    Map.entry("9", "blue"),
    Map.entry("a", "green"),
    Map.entry("b", "aqua"),
    Map.entry("c", "red"),
    Map.entry("d", "light_purple"),
    Map.entry("e", "yellow"),
    Map.entry("f", "white")
  );

  /** Creates a new HyFactionsImporter. */
  public HyFactionsImporter(
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

  /**
   * Legacy constructor without backup manager support.
   */
  public HyFactionsImporter(
      @NotNull FactionManager factionManager,
      @NotNull ClaimManager claimManager,
      @NotNull ZoneManager zoneManager,
      @NotNull PowerManager powerManager
  ) {
    this(factionManager, claimManager, zoneManager, powerManager, null);
  }

  // === Configuration Methods ===

  /** Sets the dry run. */
  public HyFactionsImporter setDryRun(boolean dryRun) {
    this.dryRun = dryRun;
    return this;
  }

  /** Sets the overwrite. */
  public HyFactionsImporter setOverwrite(boolean overwrite) {
    this.overwrite = overwrite;
    return this;
  }

  /** Sets the skip zones. */
  public HyFactionsImporter setSkipZones(boolean skipZones) {
    this.skipZones = skipZones;
    return this;
  }

  /** Sets the skip power. */
  public HyFactionsImporter setSkipPower(boolean skipPower) {
    this.skipPower = skipPower;
    return this;
  }

  public HyFactionsImporter setCreateBackup(boolean createBackup) {
    this.createBackup = createBackup;
    return this;
  }

  /** Sets the progress callback. */
  public HyFactionsImporter setProgressCallback(@Nullable Consumer<String> callback) {
    this.progressCallback = callback;
    return this;
  }

  /** Sets the on import complete. */
  public HyFactionsImporter setOnImportComplete(@Nullable Runnable callback) {
    this.onImportComplete = callback;
    return this;
  }

  /**
   * Checks if an import is currently in progress.
   *
   * @return true if an import is running
   */
  public static boolean isImportInProgress() {
    return importInProgress.get();
  }

  // === Validation Method ===

  /**
   * Validates HyFactions data before import without making any changes.
   * Use this to preview what will be imported and identify potential issues.
   *
   * @param sourcePath the path to the Kaws_Hyfaction directory
   * @return validation report with conflicts and warnings
   */
  public ImportValidationReport validate(@NotNull Path sourcePath) {
    ImportValidationReport.Builder report = ImportValidationReport.builder();

    // Validate directory structure
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

    // Load name cache
    loadNameCacheForValidation(configDir, report);

    // Load and validate factions
    List<HyFaction> hyFactions = loadFactionsForValidation(configDir, report);
    report.totalFactions(hyFactions.size());

    // Track seen names and IDs for duplicate detection within import
    Set<String> seenNames = new HashSet<>();
    Set<String> seenIds = new HashSet<>();
    Set<String> seenMembers = new HashSet<>();

    for (HyFaction hyFaction : hyFactions) {
      validateFaction(hyFaction, seenNames, seenIds, seenMembers, report);
    }

    // Load and validate claims
    Map<UUID, List<HyFactionChunkInfo>> claimsByFaction = loadClaimsForValidation(configDir, report);
    int totalClaims = claimsByFaction.values().stream().mapToInt(List::size).sum();
    report.totalClaims(totalClaims);

    // Validate claims reference existing factions
    Set<UUID> factionIds = new HashSet<>();
    for (HyFaction f : hyFactions) {
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
      List<HyFactionZoneChunk> safeZones = loadSafeZonesForValidation(configDir, report);
      List<HyFactionZoneChunk> warZones = loadWarZonesForValidation(configDir, report);
      report.totalSafeZoneChunks(safeZones.size());
      report.totalWarZoneChunks(warZones.size());
    }

    return report.build();
  }

  /**
   * Validates a single faction and adds issues to the report.
   */
  private void validateFaction(HyFaction hyFaction, Set<String> seenNames, Set<String> seenIds,
                 Set<String> seenMembers, ImportValidationReport.Builder report) {
    // Validate ID
    if (hyFaction.Id() == null || hyFaction.Id().isEmpty()) {
      report.error("Faction missing ID: " + hyFaction.Name());
      return;
    }

    // Validate UUID format
    UUID factionId;
    try {
      factionId = UUID.fromString(hyFaction.Id());
    } catch (IllegalArgumentException e) {
      report.invalidUuid("Invalid faction ID format: " + hyFaction.Id());
      return;
    }

    // Check for duplicate IDs within import data
    if (seenIds.contains(hyFaction.Id())) {
      report.idConflict("Duplicate faction ID in import data: " + hyFaction.Id());
    }
    seenIds.add(hyFaction.Id());

    // Validate name
    if (hyFaction.Name() == null || hyFaction.Name().isEmpty()) {
      report.error("Faction missing name: " + hyFaction.Id());
      return;
    }

    // Check for duplicate names within import data
    String lowerName = hyFaction.Name().toLowerCase();
    if (seenNames.contains(lowerName)) {
      report.nameConflict("Duplicate faction name in import data: " + hyFaction.Name());
    }
    seenNames.add(lowerName);

    // Check for name conflict with existing HyperFactions factions
    Faction existingByName = factionManager.getFactionByName(hyFaction.Name());
    if (existingByName != null && !existingByName.id().equals(factionId)) {
      if (overwrite) {
        report.nameConflict("Faction '" + hyFaction.Name() + "' exists with different ID - will use imported ID");
      } else {
        report.nameConflict("Faction '" + hyFaction.Name() + "' already exists (different ID) - use --overwrite to replace");
      }
    }

    // Check for ID conflict with existing HyperFactions factions
    Faction existingById = factionManager.getFaction(factionId);
    if (existingById != null) {
      if (overwrite) {
        report.idConflict("Faction ID " + factionId + " exists - will be overwritten");
      } else {
        report.idConflict("Faction ID " + factionId + " already exists - use --overwrite to replace");
      }
    }

    // Validate members
    if (hyFaction.Members() == null || hyFaction.Members().isEmpty()) {
      report.warning("Faction '" + hyFaction.Name() + "' has no members");
    } else {
      report.addMembers(hyFaction.Members().size());

      for (String memberUuidStr : hyFaction.Members()) {
        // Validate member UUID format
        try {
          UUID.fromString(memberUuidStr);
        } catch (IllegalArgumentException e) {
          report.invalidUuid("Invalid member UUID in " + hyFaction.Name() + ": " + memberUuidStr);
          continue;
        }

        // Check for members in multiple imported factions
        if (seenMembers.contains(memberUuidStr)) {
          String memberName = nameCache.getOrDefault(parseUUID(memberUuidStr), memberUuidStr);
          report.memberConflict("Player '" + memberName + "' appears in multiple imported factions");
        }
        seenMembers.add(memberUuidStr);

        // Check if member is already in an existing HyperFactions faction
        UUID memberUuid = parseUUID(memberUuidStr);
        if (memberUuid != null) {
          Faction existingFaction = factionManager.getPlayerFaction(memberUuid);
          if (existingFaction != null && !existingFaction.id().equals(factionId)) {
            String memberName = nameCache.getOrDefault(memberUuid, memberUuidStr);
            report.memberConflict("Player '" + memberName + "' already in faction '"
              + existingFaction.name() + "' - will be moved to '" + hyFaction.Name() + "'");
          }
        }
      }
    }

    // Validate owner
    if (hyFaction.Owner() == null || hyFaction.Owner().isEmpty()) {
      report.warning("Faction '" + hyFaction.Name() + "' has no owner - first member will become leader");
    } else {
      try {
        UUID.fromString(hyFaction.Owner());
      } catch (IllegalArgumentException e) {
        report.invalidUuid("Invalid owner UUID in " + hyFaction.Name() + ": " + hyFaction.Owner());
      }
    }

    // Validate home dimension (world)
    if (hyFaction.hasHome() && hyFaction.HomeDimension() != null) {
      String worldName = hyFaction.HomeDimension();
      // Common dimension names that might need mapping
      if (!worldName.equals("default") && !worldName.equals("overworld")
        && !worldName.equals("nether") && !worldName.equals("end")) {
        report.worldWarning("Faction '" + hyFaction.Name() + "' home in unknown world: " + worldName);
      }
    }
  }

  /**
   * Loads name cache for validation (doesn't modify state).
   */
  private void loadNameCacheForValidation(File configDir, ImportValidationReport.Builder report) {
    File nameCacheFile = new File(configDir, "NameCache.json");
    if (!nameCacheFile.exists()) {
      report.warning("NameCache.json not found - usernames may be unavailable");
      return;
    }

    try (FileReader reader = new FileReader(nameCacheFile)) {
      HyFactionNameCache cache = gson.fromJson(reader, HyFactionNameCache.class);
      if (cache != null && cache.Values() != null) {
        for (HyFactionNameEntry entry : cache.Values()) {
          if (entry.UUID() != null && entry.Name() != null) {
            try {
              UUID uuid = UUID.fromString(entry.UUID());
              nameCache.put(uuid, entry.Name());
            } catch (IllegalArgumentException ignored) {}
          }
        }
      }
    } catch (Exception e) {
      report.warning("Failed to load NameCache.json: " + e.getMessage());
    }
  }

  /**
   * Loads factions for validation (doesn't modify state).
   */
  private List<HyFaction> loadFactionsForValidation(File configDir, ImportValidationReport.Builder report) {
    List<HyFaction> factions = new ArrayList<>();
    File factionDir = new File(configDir, "faction");

    if (!factionDir.exists() || !factionDir.isDirectory()) {
      report.warning("No faction directory found");
      return factions;
    }

    File[] files = factionDir.listFiles((dir, name) -> name.endsWith(".json"));
    if (files == null || files.length == 0) {
      report.warning("No faction files found");
      return factions;
    }

    for (File file : files) {
      try (FileReader reader = new FileReader(file)) {
        HyFaction faction = gson.fromJson(reader, HyFaction.class);
        if (faction != null && faction.Id() != null) {
          factions.add(faction);
        }
      } catch (Exception e) {
        report.warning("Failed to load faction file " + file.getName() + ": " + e.getMessage());
      }
    }

    return factions;
  }

  /**
   * Loads claims for validation (doesn't modify state).
   */
  private Map<UUID, List<HyFactionChunkInfo>> loadClaimsForValidation(File configDir, ImportValidationReport.Builder report) {
    Map<UUID, List<HyFactionChunkInfo>> claimsByFaction = new HashMap<>();
    File claimsFile = new File(configDir, "Claims.json");

    if (!claimsFile.exists()) {
      report.warning("Claims.json not found");
      return claimsByFaction;
    }

    try (FileReader reader = new FileReader(claimsFile)) {
      HyFactionClaims claims = gson.fromJson(reader, HyFactionClaims.class);
      if (claims != null && claims.Dimensions() != null) {
        for (HyFactionDimension dim : claims.Dimensions()) {
          if (dim.ChunkInfo() == null) {
            continue;
          }

          for (HyFactionChunkInfo chunk : dim.ChunkInfo()) {
            if (chunk.UUID() == null) {
              continue;
            }

            try {
              UUID factionId = UUID.fromString(chunk.UUID());
              claimsByFaction.computeIfAbsent(factionId, k -> new ArrayList<>()).add(chunk);
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

  /**
   * Loads safe zones for validation (doesn't modify state).
   */
  private List<HyFactionZoneChunk> loadSafeZonesForValidation(File configDir, ImportValidationReport.Builder report) {
    File file = new File(configDir, "SafeZones.json");
    if (!file.exists()) {
      return Collections.emptyList();
    }

    try (FileReader reader = new FileReader(file)) {
      HyFactionSafeZones zones = gson.fromJson(reader, HyFactionSafeZones.class);
      if (zones != null && zones.SafeZones() != null) {
        return zones.SafeZones();
      }
    } catch (Exception e) {
      report.warning("Failed to load SafeZones.json: " + e.getMessage());
    }

    return Collections.emptyList();
  }

  /**
   * Loads war zones for validation (doesn't modify state).
   */
  private List<HyFactionZoneChunk> loadWarZonesForValidation(File configDir, ImportValidationReport.Builder report) {
    File file = new File(configDir, "WarZones.json");
    if (!file.exists()) {
      return Collections.emptyList();
    }

    try (FileReader reader = new FileReader(file)) {
      HyFactionWarZones zones = gson.fromJson(reader, HyFactionWarZones.class);
      if (zones != null && zones.WarZones() != null) {
        return zones.WarZones();
      }
    } catch (Exception e) {
      report.warning("Failed to load WarZones.json: " + e.getMessage());
    }

    return Collections.emptyList();
  }

  // === Main Import Method ===

  /**
   * Imports HyFactions data from the specified directory.
   * Thread-safe: only one import can run at a time.
   *
   * @param sourcePath the path to the Kaws_Hyfaction directory
   * @return the import result
   */
  public ImportResult importFrom(@NotNull Path sourcePath) {
    ImportResult.Builder result = ImportResult.builder().dryRun(dryRun);

    // Check other importers aren't running
    if (ElbaphFactionsImporter.isImportInProgress()) {
      result.error("An ElbaphFactions import is already in progress. Please wait for it to complete.");
      return result.build();
    }
    if (FactionsXImporter.isImportInProgress()) {
      result.error("A FactionsX import is already in progress. Please wait for it to complete.");
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

  /**
   * Internal import implementation.
   */
  private ImportResult doImport(@NotNull Path sourcePath, ImportResult.Builder result) {
    progress("Starting HyFactions import from: " + sourcePath);

    // Validate directory structure
    File sourceDir = sourcePath.toFile();
    if (!sourceDir.exists() || !sourceDir.isDirectory()) {
      result.error("Source directory not found: " + sourcePath);
      return result.build();
    }

    File configDir = new File(sourceDir, "config");
    if (!configDir.exists()) {
      result.error("Config directory not found: " + configDir.getPath());
      return result.build();
    }

    // Create pre-import backup if not dry run
    if (!dryRun && createBackup && backupManager != null) {
      progress("Creating pre-import backup...");
      try {
        var backupResult = backupManager.createBackup(
          BackupType.MANUAL, "pre-import", null
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

    // Load name cache first
    loadNameCache(configDir, result);

    // Load faction files
    List<HyFaction> hyFactions = loadFactions(configDir, result);
    if (result.build().hasErrors()) {
      return result.build();
    }

    // Load claims
    Map<UUID, List<ChunkWithDimension>> claimsByFaction = loadClaims(configDir, result);

    // Load zones
    List<HyFactionZoneChunk> safeZones = Collections.emptyList();
    List<HyFactionZoneChunk> warZones = Collections.emptyList();
    if (!skipZones) {
      safeZones = loadSafeZones(configDir, result);
      warZones = loadWarZones(configDir, result);
    }

    // Calculate claim stats and dimensions
    int totalClaims = claimsByFaction.values().stream().mapToInt(List::size).sum();
    Set<String> dimensions = claimsByFaction.values().stream()
      .flatMap(List::stream)
      .map(ChunkWithDimension::dimension)
      .collect(Collectors.toSet());

    progress("Found %d factions, %d claims in dimensions %s, %d safe zone chunks, %d war zone chunks",
      hyFactions.size(),
      totalClaims,
      dimensions.isEmpty() ? "[none]" : dimensions.toString(),
      safeZones.size(),
      warZones.size()
    );

    // Process factions
    for (HyFaction hyFaction : hyFactions) {
      processFaction(hyFaction, claimsByFaction, result);
    }

    // Process zones with batch mode to avoid race conditions and excessive map refreshes
    if (!skipZones) {
      if (!dryRun) {
        zoneManager.startBatch();
      }
      try {
        processZones(safeZones, ZoneType.SAFE, "SafeZone", result);
        processZones(warZones, ZoneType.WAR, "WarZone", result);
      } finally {
        if (!dryRun) {
          zoneManager.endBatch();
        }
      }
    }

    if (dryRun) {
      progress("Dry run complete - no changes made");
    } else {
      // Rebuild claim index so ClaimManager knows about imported claims
      progress("Rebuilding claim index...");
      claimManager.buildIndex();

      // Trigger world map refresh callback
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

  private void loadNameCache(File configDir, ImportResult.Builder result) {
    File nameCacheFile = new File(configDir, "NameCache.json");
    if (!nameCacheFile.exists()) {
      result.warning("NameCache.json not found - usernames may be unavailable");
      return;
    }

    try (FileReader reader = new FileReader(nameCacheFile)) {
      HyFactionNameCache cache = gson.fromJson(reader, HyFactionNameCache.class);
      if (cache != null && cache.Values() != null) {
        for (HyFactionNameEntry entry : cache.Values()) {
          if (entry.UUID() != null && entry.Name() != null) {
            try {
              UUID uuid = UUID.fromString(entry.UUID());
              nameCache.put(uuid, entry.Name());
            } catch (IllegalArgumentException e) {
              // Skip invalid UUIDs
            }
          }
        }
        progress("Loaded %d name cache entries", nameCache.size());
      }
    } catch (Exception e) {
      result.warning("Failed to load NameCache.json: " + e.getMessage());
    }
  }

  private List<HyFaction> loadFactions(File configDir, ImportResult.Builder result) {
    List<HyFaction> factions = new ArrayList<>();
    File factionDir = new File(configDir, "faction");

    if (!factionDir.exists() || !factionDir.isDirectory()) {
      result.warning("No faction directory found");
      return factions;
    }

    File[] files = factionDir.listFiles((dir, name) -> name.endsWith(".json"));
    if (files == null || files.length == 0) {
      result.warning("No faction files found");
      return factions;
    }

    for (File file : files) {
      try (FileReader reader = new FileReader(file)) {
        HyFaction faction = gson.fromJson(reader, HyFaction.class);
        if (faction != null && faction.Id() != null) {
          factions.add(faction);
        }
      } catch (Exception e) {
        result.warning("Failed to load faction file " + file.getName() + ": " + e.getMessage());
      }
    }

    return factions;
  }

  private Map<UUID, List<ChunkWithDimension>> loadClaims(File configDir, ImportResult.Builder result) {
    Map<UUID, List<ChunkWithDimension>> claimsByFaction = new HashMap<>();
    File claimsFile = new File(configDir, "Claims.json");

    if (!claimsFile.exists()) {
      result.warning("Claims.json not found");
      return claimsByFaction;
    }

    try (FileReader reader = new FileReader(claimsFile)) {
      HyFactionClaims claims = gson.fromJson(reader, HyFactionClaims.class);
      if (claims != null && claims.Dimensions() != null) {
        for (HyFactionDimension dim : claims.Dimensions()) {
          if (dim.ChunkInfo() == null) {
            continue;
          }

          // Get dimension name, default to "default" if not specified
          String dimension = dim.Dimension() != null ? dim.Dimension() : "default";

          for (HyFactionChunkInfo chunk : dim.ChunkInfo()) {
            if (chunk.UUID() == null) {
              continue;
            }

            try {
              UUID factionId = UUID.fromString(chunk.UUID());
              HyFactionChunkInfo chunkInfo = new HyFactionChunkInfo(
                chunk.UUID(),
                chunk.ChunkX(),
                chunk.ChunkY(),
                chunk.CreatedTracker() != null ? chunk.CreatedTracker() :
                  new HyFactionTracker(null, null, null)
              );
              claimsByFaction
                .computeIfAbsent(factionId, k -> new ArrayList<>())
                .add(new ChunkWithDimension(dimension, chunkInfo));
            } catch (IllegalArgumentException e) {
              // Skip invalid UUIDs
            }
          }
        }
      }
    } catch (Exception e) {
      result.warning("Failed to load Claims.json: " + e.getMessage());
    }

    return claimsByFaction;
  }

  private List<HyFactionZoneChunk> loadSafeZones(File configDir, ImportResult.Builder result) {
    File file = new File(configDir, "SafeZones.json");
    if (!file.exists()) {
      return Collections.emptyList();
    }

    try (FileReader reader = new FileReader(file)) {
      HyFactionSafeZones zones = gson.fromJson(reader, HyFactionSafeZones.class);
      if (zones != null && zones.SafeZones() != null) {
        return zones.SafeZones();
      }
    } catch (Exception e) {
      result.warning("Failed to load SafeZones.json: " + e.getMessage());
    }

    return Collections.emptyList();
  }

  private List<HyFactionZoneChunk> loadWarZones(File configDir, ImportResult.Builder result) {
    File file = new File(configDir, "WarZones.json");
    if (!file.exists()) {
      return Collections.emptyList();
    }

    try (FileReader reader = new FileReader(file)) {
      HyFactionWarZones zones = gson.fromJson(reader, HyFactionWarZones.class);
      if (zones != null && zones.WarZones() != null) {
        return zones.WarZones();
      }
    } catch (Exception e) {
      result.warning("Failed to load WarZones.json: " + e.getMessage());
    }

    return Collections.emptyList();
  }

  // === Processing Methods ===

  private void processFaction(HyFaction hyFaction, Map<UUID, List<ChunkWithDimension>> claimsByFaction,
                ImportResult.Builder result) {
    if (hyFaction.Id() == null || hyFaction.Name() == null) {
      result.warning("Skipping faction with missing ID or name");
      result.incrementFactionsSkipped();
      return;
    }

    UUID factionId;
    try {
      factionId = UUID.fromString(hyFaction.Id());
    } catch (IllegalArgumentException e) {
      result.warning("Skipping faction with invalid ID: " + hyFaction.Id());
      result.incrementFactionsSkipped();
      return;
    }

    progress("Processing faction: %s (%s)", hyFaction.Name(), hyFaction.Id().substring(0, 8));

    // Check for existing faction
    Faction existing = factionManager.getFaction(factionId);
    if (existing != null && !overwrite) {
      progress("  - Skipping (already exists, use --overwrite to replace)");
      result.incrementFactionsSkipped();
      return;
    }

    // Convert the faction
    Faction converted = convertFaction(hyFaction, claimsByFaction, result);
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
      // Import the faction with proper index updates
      factionManager.importFaction(converted, overwrite);
    }

    result.incrementFactionsImported();
    result.addClaimsImported(converted.getClaimCount());

    // Handle power distribution
    if (!skipPower && hyFaction.TotalPower() > 0 && hyFaction.getMemberCount() > 0) {
      distributePower(converted, hyFaction.TotalPower(), result);
    }
  }

  /**
   * Handles players who are already in existing HyperFactions factions.
   * Removes them from their current faction, and disbands the faction if it becomes empty.
   *
   * @param importedFaction the faction being imported
   * @param result          the result builder for logging
   * @return the number of players removed from existing factions
   */
  private int handleExistingMemberships(Faction importedFaction, ImportResult.Builder result) {
    int playersRemoved = 0;
    Set<UUID> factionsToCheck = new HashSet<>();

    for (UUID memberUuid : importedFaction.members().keySet()) {
      // Check if player is already in an existing HyperFactions faction
      Faction existingFaction = factionManager.getPlayerFaction(memberUuid);

      // Skip if not in a faction, or if they're in the same faction we're importing (overwrite case)
      if (existingFaction == null || existingFaction.id().equals(importedFaction.id())) {
        continue;
      }

      FactionMember existingMember = existingFaction.getMember(memberUuid);
      String playerName = existingMember != null ? existingMember.username() : "Unknown";

      progress("  - Player %s is already in faction '%s', removing...",
        playerName, existingFaction.name());

      if (!dryRun) {
        // Remove player from existing faction
        Faction updatedExisting = existingFaction.withoutMember(memberUuid)
          .withLog(FactionLog.create(
            FactionLog.LogType.MEMBER_LEAVE,
            playerName + " left (imported to another faction)",
            null // System action
          ));

        // CRITICAL: Remove player from the player-to-faction index
        // This must be done for the import to properly add them to the new faction
        factionManager.removePlayerFromIndex(memberUuid);

        // Check if faction is now empty
        if (updatedExisting.getMemberCount() == 0) {
          progress("    - Faction '%s' is now empty, will be disbanded...", existingFaction.name());
          factionsToCheck.add(existingFaction.id());
          // Update the faction in cache so disband check works correctly
          factionManager.updateFaction(updatedExisting);
        } else {
          // Check if leader was removed - need to promote a successor
          if (existingMember != null && existingMember.isLeader()) {
            FactionMember successor = updatedExisting.findSuccessor();
            if (successor != null) {
              FactionMember promoted = successor.withRole(FactionRole.LEADER);
              updatedExisting = updatedExisting.withMember(promoted)
                .withLog(FactionLog.create(
                  FactionLog.LogType.LEADER_TRANSFER,
                  promoted.username() + " became leader (previous leader imported to another faction)",
                  null
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

    // Disband empty factions (must be done after all removals to handle edge cases)
    if (!dryRun) {
      for (UUID factionId : factionsToCheck) {
        Faction faction = factionManager.getFaction(factionId);
        if (faction != null && faction.getMemberCount() == 0) {
          // Disband the faction by removing it completely
          disbandEmptyFaction(faction, result);
        }
      }
    }

    return playersRemoved;
  }

  /**
   * Disbands an empty faction during import.
   */
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

  private Faction convertFaction(HyFaction hyFaction, Map<UUID, List<ChunkWithDimension>> claimsByFaction,
                 ImportResult.Builder result) {
    UUID factionId = UUID.fromString(hyFaction.Id());

    // Convert color - use default if missing or black (0)
    String color = convertColor(hyFaction.Color());
    if (color.equals("0") || hyFaction.Color() == 0) {
      // Black is often a missing value, assign a random color
      color = getRandomColor();
      progress("  - Generated random color for faction (original was black/missing)");
    }

    // Get creation timestamp
    long createdAt = hyFaction.CreatedTracker() != null
      ? hyFaction.CreatedTracker().toEpochMillis()
      : System.currentTimeMillis();

    // Build members map
    Map<UUID, FactionMember> members = buildMembers(hyFaction, createdAt, result);
    if (members.isEmpty()) {
      result.warning(String.format("Faction '%s' has no valid members", hyFaction.Name()));
      return null;
    }

    // Convert home
    Faction.FactionHome home = null;
    if (hyFaction.hasHome()) {
      UUID setBy = hyFaction.Owner() != null ? parseUUID(hyFaction.Owner()) : members.keySet().iterator().next();
      home = new Faction.FactionHome(
        hyFaction.HomeDimension(),
        hyFaction.HomeX(),
        hyFaction.HomeY(),
        hyFaction.HomeZ(),
        hyFaction.HomeYaw(),
        hyFaction.HomePitch(),
        createdAt,
        setBy != null ? setBy : members.keySet().iterator().next()
      );
    }

    // Convert claims
    Set<FactionClaim> claims = convertClaims(factionId, claimsByFaction);

    // Convert relations
    Map<UUID, FactionRelation> relations = convertRelations(hyFaction.Relations());

    // Convert logs
    List<FactionLog> logs = convertLogs(hyFaction.Logs());

    // Generate unique tag from faction name
    String tag = factionManager.generateUniqueTag(hyFaction.Name());
    progress("  - Generated tag: %s", tag);

    // Generate description if missing
    String description = "Imported from HyFactions";

    return new Faction(
      factionId,
      hyFaction.Name(),
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
      null   // use default permissions
    );
  }

  /**
   * Gets a random color code for factions missing color data.
   */
  @NotNull
  private String getRandomColor() {
    // Exclude black (0) and white (f) as they're hard to see
    String[] colors = {"1", "2", "3", "4", "5", "6", "9", "a", "b", "c", "d", "e"};
    return colors[new Random().nextInt(colors.length)];
  }

  private Map<UUID, FactionMember> buildMembers(HyFaction hyFaction, long createdAt, ImportResult.Builder result) {
    Map<UUID, FactionMember> members = new HashMap<>();
    long now = System.currentTimeMillis();

    if (hyFaction.Members() == null || hyFaction.Members().isEmpty()) {
      return members;
    }

    UUID ownerUuid = hyFaction.Owner() != null ? parseUUID(hyFaction.Owner()) : null;

    for (String memberUuidStr : hyFaction.Members()) {
      UUID memberUuid = parseUUID(memberUuidStr);
      if (memberUuid == null) {
        continue;
      }

      // Determine role
      FactionRole role = FactionRole.MEMBER;
      if (memberUuid.equals(ownerUuid)) {
        role = FactionRole.LEADER;
      } else if (hyFaction.MemberGrades() != null) {
        String grade = hyFaction.MemberGrades().get(memberUuidStr);
        if ("OFFICER".equalsIgnoreCase(grade)) {
          role = FactionRole.OFFICER;
        }
      }

      // Get username from cache
      String username = nameCache.getOrDefault(memberUuid, "Unknown");

      FactionMember member = new FactionMember(
        memberUuid,
        username,
        role,
        createdAt, // joinedAt = faction creation time
        now        // lastOnline = import time
      );

      members.put(memberUuid, member);
    }

    return members;
  }

  private Set<FactionClaim> convertClaims(UUID factionId, Map<UUID, List<ChunkWithDimension>> claimsByFaction) {
    Set<FactionClaim> claims = new HashSet<>();
    List<ChunkWithDimension> factionClaims = claimsByFaction.get(factionId);

    if (factionClaims == null) {
      return claims;
    }

    for (ChunkWithDimension chunkWithDim : factionClaims) {
      HyFactionChunkInfo chunk = chunkWithDim.chunk();
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

      // Note: HyFactions uses ChunkY for chunkZ
      // Use the dimension name from HyFactions data instead of hardcoding "default"
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

  private Map<UUID, FactionRelation> convertRelations(@Nullable Map<String, String> hyRelations) {
    Map<UUID, FactionRelation> relations = new HashMap<>();

    if (hyRelations == null) {
      return relations;
    }

    for (Map.Entry<String, String> entry : hyRelations.entrySet()) {
      UUID targetId = parseUUID(entry.getKey());
      if (targetId == null) {
        continue;
      }

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

  private List<FactionLog> convertLogs(@Nullable List<HyFactionLog> hyLogs) {
    List<FactionLog> logs = new ArrayList<>();

    if (hyLogs == null) {
      return logs;
    }

    // Take most recent logs up to the max
    List<HyFactionLog> recentLogs = hyLogs.size() > Faction.MAX_LOGS
      ? hyLogs.subList(hyLogs.size() - Faction.MAX_LOGS, hyLogs.size())
      : hyLogs;

    for (HyFactionLog hyLog : recentLogs) {
      FactionLog.LogType type = mapLogType(hyLog.Action());
      if (type == null) {
        continue;
      }

      String message = buildLogMessage(hyLog);
      UUID actorUuid = hyLog.UserUUID() != null ? parseUUID(hyLog.UserUUID()) : null;

      logs.add(new FactionLog(
        type,
        message,
        hyLog.toEpochMillis(),
        actorUuid
      ));
    }

    // Reverse so newest is first
    Collections.reverse(logs);
    return logs;
  }

  private FactionLog.LogType mapLogType(@Nullable String action) {
    if (action == null) {
      return null;
    }

    return switch (action.toUpperCase()) {
      case "CREATE" -> FactionLog.LogType.MEMBER_JOIN;
      case "CLAIM" -> FactionLog.LogType.CLAIM;
      case "UNCLAIM" -> FactionLog.LogType.UNCLAIM;
      case "JOIN" -> FactionLog.LogType.MEMBER_JOIN;
      case "LEAVE" -> FactionLog.LogType.MEMBER_LEAVE;
      case "KICK" -> FactionLog.LogType.MEMBER_KICK;
      case "INVITE" -> FactionLog.LogType.MEMBER_JOIN; // No direct equivalent
      case "SETHOME" -> FactionLog.LogType.HOME_SET;
      case "ALLY" -> FactionLog.LogType.RELATION_ALLY;
      case "ENEMY" -> FactionLog.LogType.RELATION_ENEMY;
      case "NEUTRAL" -> FactionLog.LogType.RELATION_NEUTRAL;
      case "PROMOTE" -> FactionLog.LogType.MEMBER_PROMOTE;
      case "DEMOTE" -> FactionLog.LogType.MEMBER_DEMOTE;
      case "TRANSFER" -> FactionLog.LogType.LEADER_TRANSFER;
      default -> null;
    };
  }

  private String buildLogMessage(HyFactionLog hyLog) {
    String username = hyLog.UserName() != null ? hyLog.UserName() : "Unknown";
    String action = hyLog.Action() != null ? hyLog.Action() : "unknown";

    return switch (action.toUpperCase()) {
      case "CREATE" -> username + " created the faction";
      case "CLAIM" -> username + " claimed territory";
      case "UNCLAIM" -> username + " unclaimed territory";
      case "JOIN" -> username + " joined the faction";
      case "LEAVE" -> username + " left the faction";
      case "KICK" -> username + " was kicked";
      case "INVITE" -> username + " invited " + (hyLog.TargetPlayer() != null ? hyLog.TargetPlayer() : "a player");
      case "SETHOME" -> username + " set faction home";
      default -> username + " performed " + action.toLowerCase();
    };
  }

  private void distributePower(Faction faction, int totalPower, ImportResult.Builder result) {
    int memberCount = faction.getMemberCount();
    if (memberCount == 0) {
      return;
    }

    double maxPower = ConfigManager.get().getMaxPlayerPower();
    double powerPerMember = Math.min((double) totalPower / memberCount, maxPower);

    progress("  - Distributing %d power among %d members (%.1f each, capped at %.1f)",
      totalPower, memberCount, powerPerMember, maxPower);

    if (!dryRun) {
      for (UUID memberUuid : faction.members().keySet()) {
        PlayerPower power = PlayerPower.create(memberUuid, powerPerMember, maxPower);
        // PowerManager will handle this via its API
      }
    }

    result.addPlayersWithPower(memberCount);
  }

  private void processZones(List<HyFactionZoneChunk> chunks, ZoneType type, String namePrefix,
               ImportResult.Builder result) {
    if (chunks.isEmpty()) {
      return;
    }

    // Group chunks by dimension and cluster adjacent chunks
    Map<String, List<HyFactionZoneChunk>> byDimension = chunks.stream()
      .filter(c -> c.dimension() != null)
      .collect(Collectors.groupingBy(HyFactionZoneChunk::dimension));

    int zoneCount = 0;
    List<CompletableFuture<ZoneManager.ZoneResult>> futures = new ArrayList<>();

    for (Map.Entry<String, List<HyFactionZoneChunk>> entry : byDimension.entrySet()) {
      String dimension = entry.getKey();
      List<HyFactionZoneChunk> dimChunks = entry.getValue();

      // Cluster adjacent chunks into zones
      List<Set<ChunkKey>> clusters = clusterChunks(dimension, dimChunks);

      for (Set<ChunkKey> cluster : clusters) {
        zoneCount++;
        String zoneName = namePrefix + "-" + zoneCount;

        progress("  Creating %s with %d chunks in %s", zoneName, cluster.size(), dimension);

        if (!dryRun) {
          // Create the zone atomically with all its chunks
          // Apply default flags since HyFactions doesn't have a flag system
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

    // Wait for all zone creations to complete
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
   * Clusters adjacent chunks into connected groups.
   */
  private List<Set<ChunkKey>> clusterChunks(String dimension, List<HyFactionZoneChunk> chunks) {
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

        // Check all 4 adjacent chunks
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
   * Converts an ARGB integer color to the nearest Minecraft color code.
   */
  private String convertColor(int argb) {
    int r = (argb >> 16) & 0xFF;
    int g = (argb >> 8) & 0xFF;
    int b = argb & 0xFF;

    // Minecraft color RGB values
    int[][] mcColors = {
      {0, 0, 0},       // 0 - black
      {0, 0, 170},     // 1 - dark_blue
      {0, 170, 0},     // 2 - dark_green
      {0, 170, 170},   // 3 - dark_aqua
      {170, 0, 0},     // 4 - dark_red
      {170, 0, 170},   // 5 - dark_purple
      {255, 170, 0},   // 6 - gold
      {170, 170, 170}, // 7 - gray
      {85, 85, 85},    // 8 - dark_gray
      {85, 85, 255},   // 9 - blue
      {85, 255, 85},   // a - green
      {85, 255, 255},  // b - aqua
      {255, 85, 85},   // c - red
      {255, 85, 255},  // d - light_purple
      {255, 255, 85},  // e - yellow
      {255, 255, 255}  // f - white
    };

    String[] codes = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"};

    int bestIndex = 15; // Default to white
    double bestDistance = Double.MAX_VALUE;

    for (int i = 0; i < mcColors.length; i++) {
      double distance = Math.sqrt(
        Math.pow(r - mcColors[i][0], 2)
        + Math.pow(g - mcColors[i][1], 2) +
        Math.pow(b - mcColors[i][2], 2)
      );
      if (distance < bestDistance) {
        bestDistance = distance;
        bestIndex = i;
      }
    }

    return codes[bestIndex];
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
    Logger.info("[HyFactionsImport] " + message);
    if (progressCallback != null) {
      progressCallback.accept(message);
    }
  }
}
