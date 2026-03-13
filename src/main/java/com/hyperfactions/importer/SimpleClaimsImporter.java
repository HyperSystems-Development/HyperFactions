package com.hyperfactions.importer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hyperfactions.backup.BackupManager;
import com.hyperfactions.backup.BackupType;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.*;
import com.hyperfactions.importer.simpleclaims.*;
import com.hyperfactions.manager.ClaimManager;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.manager.PowerManager;
import com.hyperfactions.manager.ZoneManager;
import com.hyperfactions.util.Logger;
import com.hyperfactions.util.GuiKeys;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Imports faction data from SimpleClaims mod into HyperFactions.
 * Thread-safe: only one import can run at a time.
 *
 * <p>SimpleClaims data can be in two formats:
 * <ul>
 *   <li>SQLite ({@code SimpleClaims.db}) — modern format with correct column names</li>
 *   <li>JSON ({@code Parties.json}, {@code Claims.json}, etc.) — legacy format with ChunkY=Z quirk</li>
 * </ul>
 *
 * <p>Key differences from FactionsX/HyFactions importers:
 * <ul>
 *   <li>Only 2 roles: Owner (→ LEADER) and Member (→ MEMBER)</li>
 *   <li>No power system — assigns config defaults to all imported players</li>
 *   <li>No zones (safezone/warzone)</li>
 *   <li>No faction home</li>
 *   <li>One-way alliances — only mutual alliances are imported as ALLY</li>
 *   <li>Player allies have no HF equivalent — logged as warnings</li>
 * </ul>
 */
public class SimpleClaimsImporter {

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

  private boolean skipPower = false;

  private boolean createBackup = true;

  @Nullable
  private Consumer<String> progressCallback;

  // Name cache for UUID -> username lookups
  private final Map<UUID, String> nameCache = new HashMap<>();

  // Storage format detected
  private enum StorageFormat { SQLITE, JSON }

  /** Creates a new SimpleClaimsImporter. */
  public SimpleClaimsImporter(
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
  public SimpleClaimsImporter setDryRun(boolean dryRun) {
    this.dryRun = dryRun;
    return this;
  }

  /** Sets the overwrite. */
  public SimpleClaimsImporter setOverwrite(boolean overwrite) {
    this.overwrite = overwrite;
    return this;
  }

  /** Sets the skip power. */
  public SimpleClaimsImporter setSkipPower(boolean skipPower) {
    this.skipPower = skipPower;
    return this;
  }

  /** Sets the create backup. */
  public SimpleClaimsImporter setCreateBackup(boolean createBackup) {
    this.createBackup = createBackup;
    return this;
  }

  /** Sets the progress callback. */
  public SimpleClaimsImporter setProgressCallback(@Nullable Consumer<String> callback) {
    this.progressCallback = callback;
    return this;
  }

  /** Sets the on import complete. */
  public SimpleClaimsImporter setOnImportComplete(@Nullable Runnable callback) {
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

  // === Import Entry Point ===

  /**
   * Imports SimpleClaims data from the given source path.
   *
   * @param sourcePath path to the SimpleClaims data directory (typically {@code mods/SimpleClaims})
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

  // === Core Import Logic ===

  private ImportResult doImport(@NotNull Path sourcePath, ImportResult.Builder result) {
    progress("Starting SimpleClaims import from: " + sourcePath);

    File sourceDir = sourcePath.toFile();
    if (!sourceDir.exists() || !sourceDir.isDirectory()) {
      result.error("Source directory not found: " + sourcePath);
      return result.build();
    }

    // Detect storage format
    // SimpleClaims stores data under Server/universe/SimpleClaims/ but also reads
    // from its own mod dir. The admin provides the directory containing the data files.
    File dbFile = new File(sourceDir, "SimpleClaims.db");
    File partiesFile = new File(sourceDir, "Parties.json");

    StorageFormat format;
    if (dbFile.exists()) {
      if (!ScSqliteReader.isDriverAvailable()) {
        result.error("SimpleClaims data is in SQLite format but the SQLite JDBC driver "
          + "is not available. Please add the SimpleClaims JAR to your mods folder "
          + "and restart the server, then retry the import.");
        return result.build();
      }
      format = StorageFormat.SQLITE;
      progress("Detected SQLite storage format");
    } else if (partiesFile.exists()) {
      format = StorageFormat.JSON;
      progress("Detected legacy JSON storage format");
    } else {
      result.error("No SimpleClaims data found in " + sourcePath
        + " (expected SimpleClaims.db or Parties.json)");
      return result.build();
    }

    // Create pre-import backup if not dry run
    if (!dryRun && createBackup && backupManager != null) {
      progress("Creating pre-import backup...");
      try {
        var backupResult = backupManager.createBackup(
          BackupType.MANUAL, "pre-import-simpleclaims", null
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

    // Load data
    List<ScParty> parties;
    ScClaims claims;

    if (format == StorageFormat.SQLITE) {
      try {
        ScSqliteReader reader = new ScSqliteReader(dbFile.toPath());

        // Load name cache first
        Map<String, String> sqlNameCache = reader.readNameCache();
        for (Map.Entry<String, String> entry : sqlNameCache.entrySet()) {
          UUID uuid = parseUUID(entry.getKey());
          if (uuid != null) {
            nameCache.put(uuid, entry.getValue());
          }
        }
        progress("Loaded %d name cache entries from SQLite", sqlNameCache.size());

        parties = reader.readParties();
        claims = reader.readClaims();
      } catch (SQLException e) {
        result.error("Failed to read SQLite database: " + e.getMessage());
        return result.build();
      }
    } else {
      // Load from JSON
      loadNameCacheFromJson(sourceDir, result);
      parties = loadPartiesFromJson(sourceDir, result);
      claims = loadClaimsFromJson(sourceDir, result);
    }

    if (parties == null || parties.isEmpty()) {
      result.error("No parties found to import");
      return result.build();
    }

    // Build claims-by-party index
    Map<UUID, List<ClaimData>> claimsByParty = indexClaims(claims, format);

    int totalClaims = claimsByParty.values().stream().mapToInt(List::size).sum();
    progress("Found %d parties, %d claims", parties.size(), totalClaims);

    // Build alliance graph for mutual detection
    Map<UUID, Set<UUID>> allianceGraph = buildAllianceGraph(parties);

    // Process parties
    for (ScParty party : parties) {
      processParty(party, claimsByParty, allianceGraph, result);
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

  private void loadNameCacheFromJson(File sourceDir, ImportResult.Builder result) {
    File file = new File(sourceDir, "NameCache.json");
    if (!file.exists()) {
      result.warning("NameCache.json not found - usernames may show as 'Unknown'");
      return;
    }

    try (FileReader reader = new FileReader(file)) {
      ScNameCache cache = gson.fromJson(reader, ScNameCache.class);
      if (cache != null && cache.Values() != null) {
        for (ScNameEntry entry : cache.Values()) {
          if (entry.UUID() != null && entry.Name() != null) {
            UUID uuid = parseUUID(entry.UUID());
            if (uuid != null) {
              nameCache.put(uuid, entry.Name());
            }
          }
        }
      }
      progress("Loaded %d name cache entries from JSON", nameCache.size());
    } catch (Exception e) {
      result.warning("Failed to load NameCache.json: " + e.getMessage());
    }
  }

  @Nullable
  private List<ScParty> loadPartiesFromJson(File sourceDir, ImportResult.Builder result) {
    File file = new File(sourceDir, "Parties.json");
    if (!file.exists()) {
      result.error("Parties.json not found");
      return null;
    }

    try (FileReader reader = new FileReader(file)) {
      ScParties data = gson.fromJson(reader, ScParties.class);
      if (data != null && data.Parties() != null) {
        return data.Parties();
      }
      result.error("Parties.json is empty or malformed");
      return null;
    } catch (Exception e) {
      result.error("Failed to load Parties.json: " + e.getMessage());
      return null;
    }
  }

  @Nullable
  private ScClaims loadClaimsFromJson(File sourceDir, ImportResult.Builder result) {
    File file = new File(sourceDir, "Claims.json");
    if (!file.exists()) {
      result.warning("Claims.json not found - no claims will be imported");
      return null;
    }

    try (FileReader reader = new FileReader(file)) {
      return gson.fromJson(reader, ScClaims.class);
    } catch (Exception e) {
      result.warning("Failed to load Claims.json: " + e.getMessage());
      return null;
    }
  }

  // === Claim Indexing ===

  /** Wrapper for claim data from either format. */
  private record ClaimData(String dimension, int chunkX, int chunkZ, long claimedAt, @Nullable UUID claimedBy) {}

  /**
   * Indexes claims by party UUID.
   */
  private Map<UUID, List<ClaimData>> indexClaims(@Nullable ScClaims claims, StorageFormat format) {
    Map<UUID, List<ClaimData>> byParty = new HashMap<>();

    if (claims == null || claims.Dimensions() == null) {
      return byParty;
    }

    for (ScDimension dim : claims.Dimensions()) {
      if (dim.ChunkInfo() == null) continue;
      String dimension = dim.Dimension() != null ? dim.Dimension() : "default";

      for (ScChunkInfo chunk : dim.ChunkInfo()) {
        if (chunk.UUID() == null) continue;

        UUID partyId = parseUUID(chunk.UUID());
        if (partyId == null) continue;

        long claimedAt = chunk.CreatedTracker() != null
          ? chunk.CreatedTracker().toEpochMillis()
          : System.currentTimeMillis();

        UUID claimedBy = chunk.CreatedTracker() != null && chunk.CreatedTracker().UserUUID() != null
          ? parseUUID(chunk.CreatedTracker().UserUUID())
          : null;

        // JSON uses ChunkY for Z; SQLite stores chunkZ directly in the ChunkY field
        // via ScSqliteReader which already maps chunkZ → ScChunkInfo.ChunkY
        int chunkZ = chunk.getChunkZ();

        byParty.computeIfAbsent(partyId, k -> new ArrayList<>())
          .add(new ClaimData(dimension, chunk.ChunkX(), chunkZ, claimedAt, claimedBy));
      }
    }

    return byParty;
  }

  // === Alliance Graph ===

  /**
   * Builds a graph of party-to-party alliances for mutual detection.
   */
  private Map<UUID, Set<UUID>> buildAllianceGraph(List<ScParty> parties) {
    Map<UUID, Set<UUID>> graph = new HashMap<>();

    for (ScParty party : parties) {
      if (party.Id() == null || party.PartyAllies() == null) continue;

      UUID partyId = parseUUID(party.Id());
      if (partyId == null) continue;

      Set<UUID> allies = new HashSet<>();
      for (String allyIdStr : party.PartyAllies()) {
        UUID allyId = parseUUID(allyIdStr);
        if (allyId != null) {
          allies.add(allyId);
        }
      }

      if (!allies.isEmpty()) {
        graph.put(partyId, allies);
      }
    }

    return graph;
  }

  /**
   * Checks if two parties are mutually allied.
   */
  private boolean isMutualAlliance(UUID partyA, UUID partyB, Map<UUID, Set<UUID>> graph) {
    Set<UUID> aAllies = graph.get(partyA);
    Set<UUID> bAllies = graph.get(partyB);
    return aAllies != null && aAllies.contains(partyB)
        && bAllies != null && bAllies.contains(partyA);
  }

  // === Processing ===

  private void processParty(ScParty party, Map<UUID, List<ClaimData>> claimsByParty,
                Map<UUID, Set<UUID>> allianceGraph, ImportResult.Builder result) {
    if (party.Id() == null || party.Name() == null) {
      result.warning("Skipping party with missing ID or name");
      result.incrementFactionsSkipped();
      return;
    }

    UUID partyId;
    try {
      partyId = UUID.fromString(party.Id());
    } catch (IllegalArgumentException e) {
      result.warning("Skipping party with invalid ID: " + party.Id());
      result.incrementFactionsSkipped();
      return;
    }

    progress("Processing party: %s (%s)", party.Name(), party.Id().substring(0, 8));

    // Check for existing faction
    Faction existing = factionManager.getFaction(partyId);
    if (existing != null && !overwrite) {
      progress("  - Skipping (already exists, use --overwrite to replace)");
      result.incrementFactionsSkipped();
      return;
    }

    // Convert the party to a faction
    Faction converted = convertParty(party, claimsByParty, allianceGraph, result);
    if (converted == null) {
      result.incrementFactionsSkipped();
      return;
    }

    // Log summary
    progress("  - %d members", converted.getMemberCount());
    progress("  - %d claims", converted.getClaimCount());

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

    // Assign default power (SimpleClaims has no power system)
    if (!skipPower) {
      assignDefaultPower(converted, result);
    }
  }

  @Nullable
  private Faction convertParty(ScParty party, Map<UUID, List<ClaimData>> claimsByParty,
                 Map<UUID, Set<UUID>> allianceGraph, ImportResult.Builder result) {
    UUID partyId = UUID.fromString(party.Id());

    // Convert color: SimpleClaims uses signed 32-bit RGB (includes alpha), extract lower 24 bits
    String color = convertColor(party.Color());
    if (color.equals("#000000") || party.Color() == 0) {
      color = getRandomColor();
      progress("  - Generated random color (original was black/missing)");
    }

    // Get creation timestamp
    long createdAt = party.CreatedTracker() != null
      ? party.CreatedTracker().toEpochMillis()
      : System.currentTimeMillis();

    // Build members map (owner + members)
    Map<UUID, FactionMember> members = buildMembers(party, createdAt, result);
    if (members.isEmpty()) {
      result.warning(String.format("Party '%s' has no valid members", party.Name()));
      return null;
    }

    // No home in SimpleClaims

    // Convert claims
    Set<FactionClaim> claims = convertClaims(partyId, claimsByParty);

    // Convert relations (mutual alliances only)
    Map<UUID, FactionRelation> relations = convertRelations(party, allianceGraph, result);

    // Convert protection overrides to FactionPermissions
    FactionPermissions permissions = convertPermissions(party.Overrides());

    // Generate unique tag from party name
    String tag = factionManager.generateUniqueTag(party.Name());
    progress("  - Generated tag: %s", tag);

    // Description
    String description = party.Description() != null && !party.Description().isEmpty()
      ? party.Description()
      : "Imported from SimpleClaims";

    // Create import log entry
    List<FactionLog> logs = new ArrayList<>();
    logs.add(FactionLog.system(FactionLog.LogType.MEMBER_JOIN,
      "Faction imported from SimpleClaims",
      GuiKeys.LogsGui.MSG_IMPORTED_FROM, "SimpleClaims"));

    return new Faction(
      partyId,
      party.Name(),
      description,
      tag,
      color,
      createdAt,
      null, // no home
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
   * Builds the members map. Owner → LEADER, all Members → MEMBER.
   * SimpleClaims has only 2 roles.
   */
  private Map<UUID, FactionMember> buildMembers(ScParty party, long createdAt,
                          ImportResult.Builder result) {
    Map<UUID, FactionMember> members = new HashMap<>();
    long now = System.currentTimeMillis();

    // Add owner as LEADER
    UUID ownerUuid = party.Owner() != null ? parseUUID(party.Owner()) : null;
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

    // Add remaining members
    if (party.Members() != null) {
      for (String memberUuidStr : party.Members()) {
        UUID memberUuid = parseUUID(memberUuidStr);
        if (memberUuid == null) continue;

        // Skip if already added as owner
        if (memberUuid.equals(ownerUuid)) continue;

        String username = nameCache.getOrDefault(memberUuid, "Unknown");
        members.put(memberUuid, new FactionMember(
          memberUuid,
          username,
          FactionRole.MEMBER,
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
      result.warning(String.format("Party '%s' has no owner, promoted %s to leader",
        party.Name(), promoted.username()));
    }

    return members;
  }

  private Set<FactionClaim> convertClaims(UUID partyId, Map<UUID, List<ClaimData>> claimsByParty) {
    Set<FactionClaim> claims = new HashSet<>();
    List<ClaimData> partyClaims = claimsByParty.get(partyId);

    if (partyClaims == null) {
      return claims;
    }

    for (ClaimData cd : partyClaims) {
      UUID claimedBy = cd.claimedBy() != null ? cd.claimedBy() : UUID.randomUUID();
      claims.add(new FactionClaim(cd.dimension(), cd.chunkX(), cd.chunkZ(), cd.claimedAt(), claimedBy));
    }

    return claims;
  }

  /**
   * Converts SimpleClaims alliances. Only mutual alliances are imported as ALLY.
   * One-way alliances are logged as warnings. Player allies are also logged as warnings.
   */
  private Map<UUID, FactionRelation> convertRelations(ScParty party,
                              Map<UUID, Set<UUID>> allianceGraph,
                              ImportResult.Builder result) {
    Map<UUID, FactionRelation> relations = new HashMap<>();
    UUID partyId = parseUUID(party.Id());
    if (partyId == null) return relations;

    // Process party alliances
    if (party.PartyAllies() != null) {
      for (String allyIdStr : party.PartyAllies()) {
        UUID allyId = parseUUID(allyIdStr);
        if (allyId == null) continue;

        if (isMutualAlliance(partyId, allyId, allianceGraph)) {
          relations.put(allyId, FactionRelation.create(allyId, RelationType.ALLY));
        } else {
          result.warning(String.format(
            "One-way alliance from '%s' to party %s skipped (not mutual)",
            party.Name(), allyIdStr.substring(0, 8)));
        }
      }
    }

    // Log player allies as data loss
    if (party.PlayerAllies() != null && !party.PlayerAllies().isEmpty()) {
      result.warning(String.format(
        "Party '%s' has %d player allies (no HyperFactions equivalent, skipped)",
        party.Name(), party.PlayerAllies().size()));
    }

    return relations;
  }

  /**
   * Converts SimpleClaims protection overrides to HyperFactions FactionPermissions.
   *
   * <p>SimpleClaims uses inverted booleans: {@code false} = protected (default),
   * {@code true} = open to outsiders. HyperFactions flags: {@code true} = allowed.
   * So SimpleClaims protection values map directly to outsider flags.
   */
  @Nullable
  private FactionPermissions convertPermissions(@Nullable List<ScOverride> overrides) {
    if (overrides == null || overrides.isEmpty()) {
      return null; // Use default permissions
    }

    Map<String, Boolean> flags = new HashMap<>();

    for (ScOverride override : overrides) {
      if (override.Type() == null || override.Value() == null) continue;
      if (!"bool".equals(override.Value().Type())) continue;

      boolean value = override.Value().asBoolean();

      // Map SimpleClaims protection flags to HyperFactions outsider flags
      // SC false = protected = HF outsider false (cannot do action)
      // SC true = open = HF outsider true (can do action)
      switch (override.Type()) {
        case "simpleclaims.party.protection.place_blocks" -> {
          flags.put(FactionPermissions.OUTSIDER_PLACE, value);
        }
        case "simpleclaims.party.protection.break_blocks" -> {
          flags.put(FactionPermissions.OUTSIDER_BREAK, value);
        }
        case "simpleclaims.party.protection.interact" -> {
          flags.put(FactionPermissions.OUTSIDER_INTERACT, value);
          // Also set granular interact flags
          flags.put(FactionPermissions.OUTSIDER_DOOR_USE, value);
          flags.put(FactionPermissions.OUTSIDER_CONTAINER_USE, value);
          flags.put(FactionPermissions.OUTSIDER_BENCH_USE, value);
        }
        case "simpleclaims.party.protection.pvp" -> {
          flags.put(FactionPermissions.PVP_ENABLED, value);
        }
        case "simpleclaims.party.protection.friendly_fire" -> {
          // No direct HF equivalent for friendly fire toggle — skip with implicit default
        }
        case "simpleclaims.party.protection.interact.chest" -> {
          flags.put(FactionPermissions.OUTSIDER_CONTAINER_USE, value);
        }
        case "simpleclaims.party.protection.interact.door" -> {
          flags.put(FactionPermissions.OUTSIDER_DOOR_USE, value);
        }
        case "simpleclaims.party.protection.interact.bench" -> {
          flags.put(FactionPermissions.OUTSIDER_BENCH_USE, value);
        }
        // interact.chair → OUTSIDER_SEAT_USE, interact.portal → no direct equivalent
        case "simpleclaims.party.protection.interact.chair" -> {
          flags.put(FactionPermissions.OUTSIDER_SEAT_USE, value);
        }
        case "simpleclaims.party.protection.interact.portal" -> {
          // No direct equivalent in HF, log as part of general interact
          flags.put(FactionPermissions.OUTSIDER_TRANSPORT_USE, value);
        }
        default -> {
          // ignore unknown overrides (claim amounts, etc.)
        }
      }
    }

    if (flags.isEmpty()) {
      return null;
    }

    return new FactionPermissions(flags);
  }

  // === Existing Membership Handling ===

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
            GuiKeys.LogsGui.MSG_LEFT_IMPORT, playerName
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
                  GuiKeys.LogsGui.MSG_LEADER_IMPORT_TRANSFER, promoted.username()
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

  // === Power Assignment ===

  /**
   * Assigns default max power to all members. SimpleClaims has no power concept,
   * so we give every imported player the configured max power to prevent claim loss.
   */
  private void assignDefaultPower(Faction faction, ImportResult.Builder result) {
    ConfigManager config = ConfigManager.get();
    double maxPower = config.getMaxPlayerPower();
    int membersWithPower = 0;

    for (UUID memberUuid : faction.members().keySet()) {
      if (!dryRun) {
        powerManager.setPlayerPower(memberUuid, maxPower);
      }
      membersWithPower++;
    }

    if (membersWithPower > 0) {
      progress("  - Assigned default power (%.0f) to %d members", maxPower, membersWithPower);
      result.addPlayersWithPower(membersWithPower);
    }
  }

  // === Utility Methods ===

  /** Converts a signed 32-bit RGB integer to a hex color string. */
  private String convertColor(int rgb) {
    return String.format("#%02X%02X%02X", (rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
  }

  @NotNull
  private String getRandomColor() {
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
    Logger.info("[SimpleClaimsImport] " + message);
    if (progressCallback != null) {
      progressCallback.accept(message);
    }
  }
}
