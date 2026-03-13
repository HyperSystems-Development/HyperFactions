package com.hyperfactions.importer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.hyperfactions.backup.BackupManager;
import com.hyperfactions.backup.BackupType;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.*;
import com.hyperfactions.importer.elbaphfactions.*;
import com.hyperfactions.manager.ClaimManager;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.manager.PowerManager;
import com.hyperfactions.manager.ZoneManager;
import com.hyperfactions.util.Logger;
import com.hyperfactions.util.MessageKeys;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Type;
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
 * Imports faction data from ElbaphFactions mod into HyperFactions.
 * Thread-safe: only one import can run at a time.
 *
 * <p>Data format differences from HyFactions:
 * - factions.json: Single JSON array of all factions (not individual files)
 * - claims.json: Object keyed by "dimension:chunkX:chunkZ" (not wrapped dimensions)
 * - zones.json: Single file with safeZones/warZones arrays (not separate files)
 * - playernames.json: Simple Map<UUID, name> (not structured NameCache)
 * - No config/ subdirectory - files directly in mod root
 */
public class ElbaphFactionsImporter {

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

  // Thread safety: own lock, also checks HyFactionsImporter
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

  /** Creates a new ElbaphFactionsImporter. */
  public ElbaphFactionsImporter(
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
  public ElbaphFactionsImporter setDryRun(boolean dryRun) {
    this.dryRun = dryRun;
    return this;
  }

  /** Sets the overwrite. */
  public ElbaphFactionsImporter setOverwrite(boolean overwrite) {
    this.overwrite = overwrite;
    return this;
  }

  /** Sets the skip zones. */
  public ElbaphFactionsImporter setSkipZones(boolean skipZones) {
    this.skipZones = skipZones;
    return this;
  }

  /** Sets the skip power. */
  public ElbaphFactionsImporter setSkipPower(boolean skipPower) {
    this.skipPower = skipPower;
    return this;
  }

  public ElbaphFactionsImporter setCreateBackup(boolean createBackup) {
    this.createBackup = createBackup;
    return this;
  }

  /** Sets the progress callback. */
  public ElbaphFactionsImporter setProgressCallback(@Nullable Consumer<String> callback) {
    this.progressCallback = callback;
    return this;
  }

  /** Sets the on import complete. */
  public ElbaphFactionsImporter setOnImportComplete(@Nullable Runnable callback) {
    this.onImportComplete = callback;
    return this;
  }

  /**
   * Checks if an ElbaphFactions import is currently in progress.
   */
  public static boolean isImportInProgress() {
    return importInProgress.get();
  }

  // === Validation Method ===

  /**
   * Validates ElbaphFactions data before import without making any changes.
   *
   * @param sourcePath the path to the ElbaphFactions directory
   * @return validation report with conflicts and warnings
   */
  public ImportValidationReport validate(@NotNull Path sourcePath) {
    ImportValidationReport.Builder report = ImportValidationReport.builder();

    File sourceDir = sourcePath.toFile();
    if (!sourceDir.exists() || !sourceDir.isDirectory()) {
      report.error("Source directory not found: " + sourcePath);
      return report.build();
    }

    // Load name cache
    loadNameCacheForValidation(sourceDir, report);

    // Load and validate factions
    List<ElbaphFaction> factions = loadFactionsForValidation(sourceDir, report);
    report.totalFactions(factions.size());

    Set<String> seenNames = new HashSet<>();
    Set<String> seenIds = new HashSet<>();
    Set<String> seenMembers = new HashSet<>();

    for (ElbaphFaction faction : factions) {
      validateFaction(faction, seenNames, seenIds, seenMembers, report);
    }

    // Load and validate claims
    Map<UUID, List<ElbaphClaim>> claimsByFaction = loadClaimsForValidation(sourceDir, report);
    int totalClaims = claimsByFaction.values().stream().mapToInt(List::size).sum();
    report.totalClaims(totalClaims);

    // Validate claims reference existing factions
    Set<UUID> factionIds = new HashSet<>();
    for (ElbaphFaction f : factions) {
      if (f.id() != null) {
        try {
          factionIds.add(UUID.fromString(f.id()));
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
      ElbaphZones zones = loadZonesForValidation(sourceDir, report);
      if (zones != null) {
        report.totalSafeZoneChunks(zones.safeZones() != null ? zones.safeZones().size() : 0);
        report.totalWarZoneChunks(zones.warZones() != null ? zones.warZones().size() : 0);
      }
    }

    return report.build();
  }

  private void validateFaction(ElbaphFaction faction, Set<String> seenNames, Set<String> seenIds,
                 Set<String> seenMembers, ImportValidationReport.Builder report) {
    if (faction.id() == null || faction.id().isEmpty()) {
      report.error("Faction missing ID: " + faction.name());
      return;
    }

    UUID factionId;
    try {
      factionId = UUID.fromString(faction.id());
    } catch (IllegalArgumentException e) {
      report.invalidUuid("Invalid faction ID format: " + faction.id());
      return;
    }

    if (seenIds.contains(faction.id())) {
      report.idConflict("Duplicate faction ID in import data: " + faction.id());
    }
    seenIds.add(faction.id());

    if (faction.name() == null || faction.name().isEmpty()) {
      report.error("Faction missing name: " + faction.id());
      return;
    }

    String lowerName = faction.name().toLowerCase();
    if (seenNames.contains(lowerName)) {
      report.nameConflict("Duplicate faction name in import data: " + faction.name());
    }
    seenNames.add(lowerName);

    // Check conflict with existing HyperFactions factions
    Faction existingByName = factionManager.getFactionByName(faction.name());
    if (existingByName != null && !existingByName.id().equals(factionId)) {
      if (overwrite) {
        report.nameConflict("Faction '" + faction.name() + "' exists with different ID - will use imported ID");
      } else {
        report.nameConflict("Faction '" + faction.name() + "' already exists (different ID) - use --overwrite to replace");
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

    // Validate members
    if (faction.memberUuids() == null || faction.memberUuids().isEmpty()) {
      report.warning("Faction '" + faction.name() + "' has no members");
    } else {
      report.addMembers(faction.memberUuids().size());

      for (String memberUuidStr : faction.memberUuids()) {
        try {
          UUID.fromString(memberUuidStr);
        } catch (IllegalArgumentException e) {
          report.invalidUuid("Invalid member UUID in " + faction.name() + ": " + memberUuidStr);
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
              + existingFaction.name() + "' - will be moved to '" + faction.name() + "'");
          }
        }
      }
    }

    // Validate owner
    if (faction.ownerUuid() == null || faction.ownerUuid().isEmpty()) {
      report.warning("Faction '" + faction.name() + "' has no owner - first member will become leader");
    } else {
      try {
        UUID.fromString(faction.ownerUuid());
      } catch (IllegalArgumentException e) {
        report.invalidUuid("Invalid owner UUID in " + faction.name() + ": " + faction.ownerUuid());
      }
    }

    // Warn about faction points (no equivalent)
    if (faction.factionPoints() > 0) {
      report.warning("Faction '" + faction.name() + "' has " + faction.factionPoints()
        + " faction points (no HyperFactions equivalent)");
    }

    // Warn about workers/farmPlots
    if (faction.workers() != null && !faction.workers().isEmpty()) {
      report.warning("Faction '" + faction.name() + "' has " + faction.workers().size()
        + " workers (no HyperFactions equivalent)");
    }
    if (faction.farmPlots() != null && !faction.farmPlots().isEmpty()) {
      report.warning("Faction '" + faction.name() + "' has " + faction.farmPlots().size()
        + " farm plots (no HyperFactions equivalent)");
    }
  }

  private void loadNameCacheForValidation(File sourceDir, ImportValidationReport.Builder report) {
    File nameCacheFile = new File(sourceDir, "playernames.json");
    if (!nameCacheFile.exists()) {
      report.warning("playernames.json not found - usernames may be unavailable");
      return;
    }

    try (FileReader reader = new FileReader(nameCacheFile)) {
      Type type = new TypeToken<Map<String, String>>(){}.getType();
      Map<String, String> names = gson.fromJson(reader, type);
      if (names != null) {
        for (Map.Entry<String, String> entry : names.entrySet()) {
          try {
            UUID uuid = UUID.fromString(entry.getKey());
            nameCache.put(uuid, entry.getValue());
          } catch (IllegalArgumentException ignored) {}
        }
      }
    } catch (Exception e) {
      report.warning("Failed to load playernames.json: " + e.getMessage());
    }
  }

  private List<ElbaphFaction> loadFactionsForValidation(File sourceDir, ImportValidationReport.Builder report) {
    File factionsFile = new File(sourceDir, "factions.json");
    if (!factionsFile.exists()) {
      report.error("factions.json not found");
      return Collections.emptyList();
    }

    try (FileReader reader = new FileReader(factionsFile)) {
      Type type = new TypeToken<List<ElbaphFaction>>(){}.getType();
      List<ElbaphFaction> factions = gson.fromJson(reader, type);
      return factions != null ? factions : Collections.emptyList();
    } catch (Exception e) {
      report.error("Failed to load factions.json: " + e.getMessage());
      return Collections.emptyList();
    }
  }

  private Map<UUID, List<ElbaphClaim>> loadClaimsForValidation(File sourceDir, ImportValidationReport.Builder report) {
    Map<UUID, List<ElbaphClaim>> claimsByFaction = new HashMap<>();
    File claimsFile = new File(sourceDir, "claims.json");

    if (!claimsFile.exists()) {
      report.warning("claims.json not found");
      return claimsByFaction;
    }

    try (FileReader reader = new FileReader(claimsFile)) {
      Type type = new TypeToken<Map<String, ElbaphClaim>>(){}.getType();
      Map<String, ElbaphClaim> claims = gson.fromJson(reader, type);
      if (claims != null) {
        for (ElbaphClaim claim : claims.values()) {
          if (claim.factionId() == null) {
            continue;
          }
          try {
            UUID factionId = UUID.fromString(claim.factionId());
            claimsByFaction.computeIfAbsent(factionId, k -> new ArrayList<>()).add(claim);
          } catch (IllegalArgumentException e) {
            report.invalidUuid("Invalid faction UUID in claim: " + claim.factionId());
          }
        }
      }
    } catch (Exception e) {
      report.warning("Failed to load claims.json: " + e.getMessage());
    }

    return claimsByFaction;
  }

  @Nullable
  private ElbaphZones loadZonesForValidation(File sourceDir, ImportValidationReport.Builder report) {
    File zonesFile = new File(sourceDir, "zones.json");
    if (!zonesFile.exists()) {
      return null;
    }

    try (FileReader reader = new FileReader(zonesFile)) {
      return gson.fromJson(reader, ElbaphZones.class);
    } catch (Exception e) {
      report.warning("Failed to load zones.json: " + e.getMessage());
      return null;
    }
  }

  // === Main Import Method ===

  /**
   * Imports ElbaphFactions data from the specified directory.
   * Thread-safe: only one import can run at a time.
   *
   * @param sourcePath the path to the ElbaphFactions directory
   * @return the import result
   */
  public ImportResult importFrom(@NotNull Path sourcePath) {
    ImportResult.Builder result = ImportResult.builder().dryRun(dryRun);

    // Check other importers aren't running
    if (HyFactionsImporter.isImportInProgress()) {
      result.error("A HyFactions import is already in progress. Please wait for it to complete.");
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

  private ImportResult doImport(@NotNull Path sourcePath, ImportResult.Builder result) {
    progress("Starting ElbaphFactions import from: " + sourcePath);

    File sourceDir = sourcePath.toFile();
    if (!sourceDir.exists() || !sourceDir.isDirectory()) {
      result.error("Source directory not found: " + sourcePath);
      return result.build();
    }

    // Create pre-import backup if not dry run
    if (!dryRun && createBackup && backupManager != null) {
      progress("Creating pre-import backup...");
      try {
        var backupResult = backupManager.createBackup(
          BackupType.MANUAL, "pre-import-elbaphfactions", null
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

    // Load name cache first (directly in mod root, not config/)
    loadNameCache(sourceDir, result);

    // Load factions from single factions.json array
    List<ElbaphFaction> factions = loadFactions(sourceDir, result);
    if (result.build().hasErrors()) {
      return result.build();
    }

    // Load claims
    Map<UUID, List<ElbaphClaim>> claimsByFaction = loadClaims(sourceDir, result);

    // Load zones
    ElbaphZones zones = null;
    if (!skipZones) {
      zones = loadZones(sourceDir, result);
    }

    // Calculate stats
    int totalClaims = claimsByFaction.values().stream().mapToInt(List::size).sum();
    Set<String> dimensions = claimsByFaction.values().stream()
      .flatMap(List::stream)
      .map(c -> c.dimension() != null ? c.dimension() : "default")
      .collect(Collectors.toSet());

    int safeZoneCount = zones != null && zones.safeZones() != null ? zones.safeZones().size() : 0;
    int warZoneCount = zones != null && zones.warZones() != null ? zones.warZones().size() : 0;

    progress("Found %d factions, %d claims in dimensions %s, %d safe zone chunks, %d war zone chunks",
      factions.size(),
      totalClaims,
      dimensions.isEmpty() ? "[none]" : dimensions.toString(),
      safeZoneCount,
      warZoneCount
    );

    // Process factions
    for (ElbaphFaction faction : factions) {
      processFaction(faction, claimsByFaction, result);
    }

    // Process zones with batch mode
    if (!skipZones && zones != null) {
      if (!dryRun) {
        zoneManager.startBatch();
      }
      try {
        if (zones.safeZones() != null && !zones.safeZones().isEmpty()) {
          processZones(zones.safeZones(), ZoneType.SAFE, "SafeZone", result);
        }
        if (zones.warZones() != null && !zones.warZones().isEmpty()) {
          processZones(zones.warZones(), ZoneType.WAR, "WarZone", result);
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

  private void loadNameCache(File sourceDir, ImportResult.Builder result) {
    File nameCacheFile = new File(sourceDir, "playernames.json");
    if (!nameCacheFile.exists()) {
      result.warning("playernames.json not found - usernames may be unavailable");
      return;
    }

    try (FileReader reader = new FileReader(nameCacheFile)) {
      Type type = new TypeToken<Map<String, String>>(){}.getType();
      Map<String, String> names = gson.fromJson(reader, type);
      if (names != null) {
        for (Map.Entry<String, String> entry : names.entrySet()) {
          try {
            UUID uuid = UUID.fromString(entry.getKey());
            nameCache.put(uuid, entry.getValue());
          } catch (IllegalArgumentException ignored) {}
        }
        progress("Loaded %d name cache entries", nameCache.size());
      }
    } catch (Exception e) {
      result.warning("Failed to load playernames.json: " + e.getMessage());
    }
  }

  private List<ElbaphFaction> loadFactions(File sourceDir, ImportResult.Builder result) {
    File factionsFile = new File(sourceDir, "factions.json");
    if (!factionsFile.exists()) {
      result.error("factions.json not found in " + sourceDir.getPath());
      return Collections.emptyList();
    }

    try (FileReader reader = new FileReader(factionsFile)) {
      Type type = new TypeToken<List<ElbaphFaction>>(){}.getType();
      List<ElbaphFaction> factions = gson.fromJson(reader, type);
      if (factions == null || factions.isEmpty()) {
        result.warning("No factions found in factions.json");
        return Collections.emptyList();
      }
      return factions;
    } catch (Exception e) {
      result.error("Failed to load factions.json: " + e.getMessage());
      return Collections.emptyList();
    }
  }

  private Map<UUID, List<ElbaphClaim>> loadClaims(File sourceDir, ImportResult.Builder result) {
    Map<UUID, List<ElbaphClaim>> claimsByFaction = new HashMap<>();
    File claimsFile = new File(sourceDir, "claims.json");

    if (!claimsFile.exists()) {
      result.warning("claims.json not found");
      return claimsByFaction;
    }

    try (FileReader reader = new FileReader(claimsFile)) {
      Type type = new TypeToken<Map<String, ElbaphClaim>>(){}.getType();
      Map<String, ElbaphClaim> claims = gson.fromJson(reader, type);
      if (claims != null) {
        for (ElbaphClaim claim : claims.values()) {
          if (claim.factionId() == null) {
            continue;
          }

          try {
            UUID factionId = UUID.fromString(claim.factionId());
            claimsByFaction.computeIfAbsent(factionId, k -> new ArrayList<>()).add(claim);
          } catch (IllegalArgumentException ignored) {}
        }
      }
    } catch (Exception e) {
      result.warning("Failed to load claims.json: " + e.getMessage());
    }

    return claimsByFaction;
  }

  @Nullable
  private ElbaphZones loadZones(File sourceDir, ImportResult.Builder result) {
    File zonesFile = new File(sourceDir, "zones.json");
    if (!zonesFile.exists()) {
      return null;
    }

    try (FileReader reader = new FileReader(zonesFile)) {
      return gson.fromJson(reader, ElbaphZones.class);
    } catch (Exception e) {
      result.warning("Failed to load zones.json: " + e.getMessage());
      return null;
    }
  }

  // === Processing Methods ===

  private void processFaction(ElbaphFaction elbaphFaction, Map<UUID, List<ElbaphClaim>> claimsByFaction,
                ImportResult.Builder result) {
    if (elbaphFaction.id() == null || elbaphFaction.name() == null) {
      result.warning("Skipping faction with missing ID or name");
      result.incrementFactionsSkipped();
      return;
    }

    UUID factionId;
    try {
      factionId = UUID.fromString(elbaphFaction.id());
    } catch (IllegalArgumentException e) {
      result.warning("Skipping faction with invalid ID: " + elbaphFaction.id());
      result.incrementFactionsSkipped();
      return;
    }

    progress("Processing faction: %s (%s)", elbaphFaction.name(), elbaphFaction.id().substring(0, 8));

    // Check for existing faction
    Faction existing = factionManager.getFaction(factionId);
    if (existing != null && !overwrite) {
      progress("  - Skipping (already exists, use --overwrite to replace)");
      result.incrementFactionsSkipped();
      return;
    }

    // Convert the faction
    Faction converted = convertFaction(elbaphFaction, claimsByFaction, result);
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

    // Handle power distribution
    if (!skipPower && elbaphFaction.currentPower() > 0 && elbaphFaction.getMemberCount() > 0) {
      distributePower(converted, elbaphFaction.currentPower(), result);
    }
  }

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

  @Nullable
  private Faction convertFaction(ElbaphFaction elbaphFaction, Map<UUID, List<ElbaphClaim>> claimsByFaction,
                  ImportResult.Builder result) {
    UUID factionId = UUID.fromString(elbaphFaction.id());

    // Convert color
    String color = convertColor(elbaphFaction.color());
    if (color.equals("0") || elbaphFaction.color() == 0) {
      color = getRandomColor();
      progress("  - Generated random color for faction (original was black/missing)");
    }

    long createdAt = elbaphFaction.createdAt() > 0
      ? elbaphFaction.createdAt()
      : System.currentTimeMillis();

    // Build members map
    Map<UUID, FactionMember> members = buildMembers(elbaphFaction, createdAt, result);
    if (members.isEmpty()) {
      result.warning(String.format("Faction '%s' has no valid members", elbaphFaction.name()));
      return null;
    }

    // Convert home (no dimension field - ElbaphFactions always uses "default", no yaw/pitch)
    Faction.FactionHome home = null;
    if (elbaphFaction.hasHome()) {
      UUID setBy = elbaphFaction.ownerUuid() != null
        ? parseUUID(elbaphFaction.ownerUuid())
        : members.keySet().iterator().next();
      home = new Faction.FactionHome(
        "default", // ElbaphFactions doesn't store home dimension
        elbaphFaction.homeX(),
        elbaphFaction.homeY(),
        elbaphFaction.homeZ(),
        0.0f, // No yaw in ElbaphFactions
        0.0f, // No pitch in ElbaphFactions
        createdAt,
        setBy != null ? setBy : members.keySet().iterator().next()
      );
    }

    // Convert claims
    Set<FactionClaim> claims = convertClaims(factionId, elbaphFaction, claimsByFaction);

    // Convert relations
    Map<UUID, FactionRelation> relations = convertRelations(elbaphFaction);

    // Convert territory permissions
    FactionPermissions permissions = convertPermissions(elbaphFaction);

    // Handle tag - import directly from ElbaphFactions, validate and fallback
    String tag = elbaphFaction.tag();
    if (tag != null && !tag.isEmpty()) {
      // Check if tag is already taken by another faction
      Faction existingWithTag = factionManager.getFactionByTag(tag);
      if (existingWithTag != null && !existingWithTag.id().equals(factionId)) {
        String oldTag = tag;
        tag = factionManager.generateUniqueTag(elbaphFaction.name());
        progress("  - Tag '%s' already taken, generated: %s", oldTag, tag);
      } else {
        progress("  - Using existing tag: %s", tag);
      }
    } else {
      tag = factionManager.generateUniqueTag(elbaphFaction.name());
      progress("  - Generated tag: %s", tag);
    }

    // Import description directly
    String description = elbaphFaction.description() != null && !elbaphFaction.description().isEmpty()
      ? elbaphFaction.description()
      : "Imported from ElbaphFactions";

    // Create import log entry
    List<FactionLog> logs = new ArrayList<>();
    logs.add(FactionLog.system(FactionLog.LogType.MEMBER_JOIN,
      "Faction imported from ElbaphFactions",
      MessageKeys.LogsGui.MSG_IMPORTED_FROM, "ElbaphFactions"));

    // Warn about faction points
    if (elbaphFaction.factionPoints() > 0) {
      result.warning(String.format("Faction '%s' has %.1f faction points (no HyperFactions equivalent)",
        elbaphFaction.name(), elbaphFaction.factionPoints()));
    }

    // Warn about workers/farmPlots
    if (elbaphFaction.workers() != null && !elbaphFaction.workers().isEmpty()) {
      result.warning(String.format("Faction '%s' has %d workers (no HyperFactions equivalent)",
        elbaphFaction.name(), elbaphFaction.workers().size()));
    }
    if (elbaphFaction.farmPlots() != null && !elbaphFaction.farmPlots().isEmpty()) {
      result.warning(String.format("Faction '%s' has %d farm plots (no HyperFactions equivalent)",
        elbaphFaction.name(), elbaphFaction.farmPlots().size()));
    }

    return new Faction(
      factionId,
      elbaphFaction.name(),
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
      null   // no hardcore power
    );
  }

  @NotNull
  private String getRandomColor() {
    String[] colors = {"1", "2", "3", "4", "5", "6", "9", "a", "b", "c", "d", "e"};
    return colors[new Random().nextInt(colors.length)];
  }

  private Map<UUID, FactionMember> buildMembers(ElbaphFaction elbaphFaction, long createdAt,
                          ImportResult.Builder result) {
    Map<UUID, FactionMember> members = new HashMap<>();

    if (elbaphFaction.memberUuids() == null || elbaphFaction.memberUuids().isEmpty()) {
      return members;
    }

    UUID ownerUuid = elbaphFaction.ownerUuid() != null ? parseUUID(elbaphFaction.ownerUuid()) : null;

    for (String memberUuidStr : elbaphFaction.memberUuids()) {
      UUID memberUuid = parseUUID(memberUuidStr);
      if (memberUuid == null) {
        continue;
      }

      // Determine role from memberRanks
      FactionRole role = FactionRole.MEMBER;
      if (memberUuid.equals(ownerUuid)) {
        role = FactionRole.LEADER;
      } else {
        String rankStr = elbaphFaction.getMemberRole(memberUuidStr);
        if (rankStr != null) {
          role = switch (rankStr.toUpperCase()) {
            case "LEADER" -> FactionRole.LEADER;
            case "OFFICER" -> FactionRole.OFFICER;
            case "WORKER_SPECIALIST" -> {
              result.warning(String.format("Member %s in '%s' has WORKER_SPECIALIST role, mapped to MEMBER",
                nameCache.getOrDefault(memberUuid, memberUuidStr), elbaphFaction.name()));
              yield FactionRole.MEMBER;
            }
            default -> FactionRole.MEMBER;
          };
        }
      }

      // Get username from cache
      String username = nameCache.getOrDefault(memberUuid, "Unknown");

      // Get lastLogin from memberRanks
      long lastOnline = elbaphFaction.getMemberLastLogin(memberUuidStr);
      if (lastOnline == 0) {
        lastOnline = System.currentTimeMillis(); // Fallback to import time
      }

      FactionMember member = new FactionMember(
        memberUuid,
        username,
        role,
        createdAt,
        lastOnline
      );

      members.put(memberUuid, member);
    }

    return members;
  }

  private Set<FactionClaim> convertClaims(UUID factionId, ElbaphFaction elbaphFaction,
                      Map<UUID, List<ElbaphClaim>> claimsByFaction) {
    Set<FactionClaim> claims = new HashSet<>();
    List<ElbaphClaim> factionClaims = claimsByFaction.get(factionId);

    if (factionClaims == null) {
      return claims;
    }

    // ElbaphFactions claimedBy is the faction UUID, not player UUID
    // Substitute ownerUuid as the claiming player
    UUID claimedByPlayer = elbaphFaction.ownerUuid() != null
      ? parseUUID(elbaphFaction.ownerUuid())
      : null;
    if (claimedByPlayer == null) {
      claimedByPlayer = UUID.randomUUID(); // Fallback
    }

    for (ElbaphClaim claim : factionClaims) {
      String dimension = claim.dimension() != null ? claim.dimension() : "default";
      long claimedAt = claim.claimedAt() > 0 ? claim.claimedAt() : System.currentTimeMillis();

      claims.add(new FactionClaim(
        dimension,
        claim.x(),
        claim.z(),
        claimedAt,
        claimedByPlayer
      ));
    }

    return claims;
  }

  private Map<UUID, FactionRelation> convertRelations(ElbaphFaction elbaphFaction) {
    Map<UUID, FactionRelation> relations = new HashMap<>();

    // Process allies
    if (elbaphFaction.allies() != null) {
      for (String allyUuidStr : elbaphFaction.allies()) {
        UUID targetId = parseUUID(allyUuidStr);
        if (targetId != null) {
          relations.put(targetId, FactionRelation.create(targetId, RelationType.ALLY));
        }
      }
    }

    // Process enemies
    if (elbaphFaction.enemies() != null) {
      for (String enemyUuidStr : elbaphFaction.enemies()) {
        UUID targetId = parseUUID(enemyUuidStr);
        if (targetId != null) {
          relations.put(targetId, FactionRelation.create(targetId, RelationType.ENEMY));
        }
      }
    }

    return relations;
  }

  private FactionPermissions convertPermissions(ElbaphFaction elbaphFaction) {
    Map<String, Boolean> flags = new java.util.HashMap<>();
    flags.put(FactionPermissions.OUTSIDER_BREAK, elbaphFaction.outsiderCanBreak());
    flags.put(FactionPermissions.OUTSIDER_PLACE, elbaphFaction.outsiderCanPlace());
    flags.put(FactionPermissions.OUTSIDER_INTERACT, elbaphFaction.outsiderCanInteract());
    flags.put(FactionPermissions.ALLY_BREAK, elbaphFaction.allyCanBreak());
    flags.put(FactionPermissions.ALLY_PLACE, elbaphFaction.allyCanPlace());
    flags.put(FactionPermissions.ALLY_INTERACT, elbaphFaction.allyCanInteract());
    flags.put(FactionPermissions.MEMBER_BREAK, elbaphFaction.memberCanBreak());
    flags.put(FactionPermissions.MEMBER_PLACE, elbaphFaction.memberCanPlace());
    flags.put(FactionPermissions.MEMBER_INTERACT, elbaphFaction.memberCanInteract());
    flags.put(FactionPermissions.PVP_ENABLED, elbaphFaction.pvpEnabled());
    flags.put(FactionPermissions.OFFICERS_CAN_EDIT, false);
    // Constructor fills in remaining flags from defaults
    return new FactionPermissions(flags);
  }

  private void distributePower(Faction faction, double totalPower, ImportResult.Builder result) {
    int memberCount = faction.getMemberCount();
    if (memberCount == 0) {
      return;
    }

    double maxPower = ConfigManager.get().getMaxPlayerPower();
    double powerPerMember = Math.min(totalPower / memberCount, maxPower);

    progress("  - Distributing %.1f power among %d members (%.1f each, capped at %.1f)",
      totalPower, memberCount, powerPerMember, maxPower);

    if (!dryRun) {
      for (UUID memberUuid : faction.members().keySet()) {
        PlayerPower power = PlayerPower.create(memberUuid, powerPerMember, maxPower);
        // PowerManager will handle this via its API
      }
    }

    result.addPlayersWithPower(memberCount);
  }

  private void processZones(List<ElbaphZoneChunk> chunks, ZoneType type, String namePrefix,
               ImportResult.Builder result) {
    if (chunks.isEmpty()) {
      return;
    }

    // Group chunks by dimension and cluster adjacent chunks
    Map<String, List<ElbaphZoneChunk>> byDimension = chunks.stream()
      .filter(c -> c.dimension() != null)
      .collect(Collectors.groupingBy(ElbaphZoneChunk::dimension));

    int zoneCount = 0;
    List<CompletableFuture<ZoneManager.ZoneResult>> futures = new ArrayList<>();

    for (Map.Entry<String, List<ElbaphZoneChunk>> entry : byDimension.entrySet()) {
      String dimension = entry.getKey();
      List<ElbaphZoneChunk> dimChunks = entry.getValue();

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

  private List<Set<ChunkKey>> clusterChunks(String dimension, List<ElbaphZoneChunk> chunks) {
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

  private String convertColor(int argb) {
    return String.format("#%02X%02X%02X", (argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF);
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
    Logger.info("[ElbaphFactionsImport] " + message);
    if (progressCallback != null) {
      progressCallback.accept(message);
    }
  }
}
