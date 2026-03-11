package com.hyperfactions.manager;

import com.hyperfactions.Permissions;
import com.hyperfactions.api.events.EventBus;
import com.hyperfactions.api.events.FactionDisbandEvent;
import com.hyperfactions.api.events.FactionMemberEvent;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.*;
import com.hyperfactions.integration.PermissionManager;
import com.hyperfactions.storage.FactionStorage;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;
import com.hyperfactions.util.MessageKeys;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Manages faction data, membership, and operations.
 */
public class FactionManager {

  private final FactionStorage storage;

  // Primary cache: faction ID -> Faction
  private final Map<UUID, Faction> factions = new ConcurrentHashMap<>();

  // Index: player UUID -> faction ID
  private final Map<UUID, UUID> playerToFaction = new ConcurrentHashMap<>();

  // Index: name (lowercase) -> faction ID
  private final Map<String, UUID> nameToFaction = new ConcurrentHashMap<>();

  // Announcement callbacks
  @Nullable
  private BiConsumer<String, String> onFactionCreated;

  @Nullable
  private TriConsumer<String, String, String> onLeadershipTransferred;

  @Nullable
  private java.util.function.Consumer<String> onFactionDisbanded;

  // Economy callback (fires after faction creation with full Faction object)
  @Nullable
  private java.util.function.Consumer<Faction> onFactionCreatedForEconomy;

  /**
   * Functional interface for callbacks with three parameters.
   */
  @FunctionalInterface
  public interface TriConsumer<A, B, C> {
    void accept(A a, B b, C c);
  }

  /** Creates a new FactionManager. */
  public FactionManager(@NotNull FactionStorage storage) {
    this.storage = storage;
  }

  /**
   * Sets a callback for when a new faction is created.
   * Params: factionName, leaderName
   */
  public void setOnFactionCreated(@Nullable BiConsumer<String, String> callback) {
    this.onFactionCreated = callback;
  }

  /**
   * Sets a callback for when a faction is created (full Faction object).
   * Used by economy module for initialization.
   */
  public void setOnFactionCreatedForEconomy(@Nullable java.util.function.Consumer<Faction> callback) {
    this.onFactionCreatedForEconomy = callback;
  }

  /**
   * Sets a callback for when leadership is transferred.
   * Params: factionName, oldLeaderName, newLeaderName
   */
  public void setOnLeadershipTransferred(@Nullable TriConsumer<String, String, String> callback) {
    this.onLeadershipTransferred = callback;
  }

  /**
   * Sets a callback for when a faction is disbanded by its leader.
   * Not called for admin force-disbands or auto-disbands.
   * Param: factionName
   */
  public void setOnFactionDisbanded(@Nullable java.util.function.Consumer<String> callback) {
    this.onFactionDisbanded = callback;
  }

  /**
   * Loads all factions from storage.
   *
   * <p>SAFETY: This method will NOT clear existing data if loading fails or returns
   * suspiciously empty results when data was expected. This prevents data loss
   * from deserialization failures or I/O errors.
   *
   * @return a future that completes when loading is done
   */
  public CompletableFuture<Void> loadAll() {
    // Capture current state for safety validation
    final int previousFactionCount = factions.size();

    final int previousMemberCount = playerToFaction.size();

    return storage.loadAllFactions().thenAccept(loaded -> {
      // SAFETY CHECK: If we had data before but loading returned nothing,
      // this is likely a load failure - DO NOT clear existing data
      if (previousFactionCount > 0 && loaded.isEmpty()) {
        String msg = String.format("CRITICAL: Load returned 0 factions but %d were previously loaded! Keeping existing in-memory data.", previousFactionCount);
        ErrorHandler.report(msg, (Exception) null);
        Logger.severe("Check logs above for deserialization errors. Use '/f admin reload' to retry.");
        return;
      }

      // Build new indices before clearing old ones (atomic swap pattern)
      Map<UUID, Faction> newFactions = new HashMap<>();
      Map<UUID, UUID> newPlayerToFaction = new HashMap<>();
      Map<String, UUID> newNameToFaction = new HashMap<>();

      for (Faction faction : loaded) {
        newFactions.put(faction.id(), faction);
        newNameToFaction.put(faction.name().toLowerCase(), faction.id());

        for (UUID memberUuid : faction.members().keySet()) {
          newPlayerToFaction.put(memberUuid, faction.id());
        }
      }

      // Now atomically swap the data
      factions.clear();
      factions.putAll(newFactions);

      playerToFaction.clear();
      playerToFaction.putAll(newPlayerToFaction);

      nameToFaction.clear();
      nameToFaction.putAll(newNameToFaction);

      Logger.info("[Startup] Loaded %d factions with %d members indexed",
        factions.size(), playerToFaction.size());
    }).exceptionally(ex -> {
      ErrorHandler.report("CRITICAL: Exception during faction loading - keeping existing data", ex);
      return null;
    });
  }

  /**
   * Saves all factions to storage.
   *
   * @return a future that completes when saving is done
   */
  public CompletableFuture<Void> saveAll() {
    List<CompletableFuture<Void>> futures = factions.values().stream()
      .map(storage::saveFaction)
      .collect(Collectors.toList());

    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
  }

  /**
   * Syncs faction data from disk, merging with in-memory data based on timestamps.
   * Members with newer lastOnline in memory are kept; members only on disk are added.
   * Members removed in-game (not on disk but in memory with newer timestamp) stay removed.
   *
   * @return a future containing the sync result summary
   */
  public CompletableFuture<SyncResult> syncFromDisk() {
    return storage.loadAllFactions().thenApply(diskFactions -> {
      int factionsUpdated = 0;
      int membersAdded = 0;
      int membersUpdated = 0;

      Map<UUID, Faction> diskFactionMap = new HashMap<>();
      for (Faction f : diskFactions) {
        diskFactionMap.put(f.id(), f);
      }

      for (Faction diskFaction : diskFactions) {
        Faction memoryFaction = factions.get(diskFaction.id());

        if (memoryFaction == null) {
          // New faction from disk - add it
          factions.put(diskFaction.id(), diskFaction);
          nameToFaction.put(diskFaction.name().toLowerCase(), diskFaction.id());
          for (UUID memberUuid : diskFaction.members().keySet()) {
            playerToFaction.put(memberUuid, diskFaction.id());
          }
          factionsUpdated++;
          membersAdded += diskFaction.members().size();
        } else {
          // Existing faction - merge members
          Map<UUID, FactionMember> mergedMembers = new HashMap<>(memoryFaction.members());
          boolean changed = false;

          for (FactionMember diskMember : diskFaction.members().values()) {
            FactionMember memoryMember = memoryFaction.members().get(diskMember.uuid());

            if (memoryMember == null) {
              // Member exists on disk but not in memory - add them
              mergedMembers.put(diskMember.uuid(), diskMember);
              playerToFaction.put(diskMember.uuid(), diskFaction.id());
              membersAdded++;
              changed = true;
            } else if (diskMember.lastOnline() > memoryMember.lastOnline()) {
              // Disk has newer data - use disk version
              mergedMembers.put(diskMember.uuid(), diskMember);
              membersUpdated++;
              changed = true;
            }

            // If memory has newer or equal lastOnline, keep memory version
          }

          if (changed) {
            Faction updated = new Faction(
              memoryFaction.id(),
              memoryFaction.name(),
              memoryFaction.description(),
              memoryFaction.tag(),
              memoryFaction.color(),
              memoryFaction.createdAt(),
              memoryFaction.home(),
              mergedMembers,
              memoryFaction.claims(),
              memoryFaction.relations(),
              memoryFaction.logs(),
              memoryFaction.open(),
              memoryFaction.permissions(),
              memoryFaction.hardcorePower()
            );
            factions.put(updated.id(), updated);
            factionsUpdated++;
          }
        }
      }

      Logger.info("[Sync] Sync complete: %d factions updated, %d members added, %d members updated",
        factionsUpdated, membersAdded, membersUpdated);

      return new SyncResult(factionsUpdated, membersAdded, membersUpdated);
    });
  }

  /**
   * Result of a sync operation.
   */
  public record SyncResult(int factionsUpdated, int membersAdded, int membersUpdated) {}

  // === Faction Queries ===

  /**
   * Gets a faction by its ID.
   *
   * @param factionId the faction ID
   * @return the faction, or null if not found
   */
  @Nullable
  public Faction getFaction(@NotNull UUID factionId) {
    return factions.get(factionId);
  }

  /**
   * Gets a faction by name (case-insensitive).
   *
   * @param name the faction name
   * @return the faction, or null if not found
   */
  @Nullable
  public Faction getFactionByName(@NotNull String name) {
    UUID id = nameToFaction.get(name.toLowerCase());
    return id != null ? factions.get(id) : null;
  }

  /**
   * Gets a faction by tag (case-insensitive).
   *
   * @param tag the faction tag
   * @return the faction, or null if not found
   */
  @Nullable
  public Faction getFactionByTag(@NotNull String tag) {
    String lowerTag = tag.toLowerCase();
    return factions.values().stream()
        .filter(f -> f.tag() != null && f.tag().equalsIgnoreCase(lowerTag))
        .findFirst()
        .orElse(null);
  }

  /**
   * Gets a player's faction.
   *
   * @param playerUuid the player's UUID
   * @return the faction, or null if not in one
   */
  @Nullable
  public Faction getPlayerFaction(@NotNull UUID playerUuid) {
    UUID factionId = playerToFaction.get(playerUuid);
    return factionId != null ? factions.get(factionId) : null;
  }

  /**
   * Gets a player's faction ID.
   *
   * @param playerUuid the player's UUID
   * @return the faction ID, or null if not in one
   */
  @Nullable
  public UUID getPlayerFactionId(@NotNull UUID playerUuid) {
    return playerToFaction.get(playerUuid);
  }

  /**
   * Checks if a player is in any faction.
   *
   * @param playerUuid the player's UUID
   * @return true if in a faction
   */
  public boolean isInFaction(@NotNull UUID playerUuid) {
    return playerToFaction.containsKey(playerUuid);
  }

  /**
   * Checks if two players are in the same faction.
   *
   * @param player1 first player's UUID
   * @param player2 second player's UUID
   * @return true if same faction
   */
  public boolean areInSameFaction(@NotNull UUID player1, @NotNull UUID player2) {
    UUID f1 = playerToFaction.get(player1);
    UUID f2 = playerToFaction.get(player2);
    return f1 != null && f1.equals(f2);
  }

  /**
   * Checks if a faction name is taken.
   *
   * @param name the name to check
   * @return true if taken
   */
  public boolean isNameTaken(@NotNull String name) {
    return nameToFaction.containsKey(name.toLowerCase());
  }

  /**
   * Gets all factions.
   *
   * @return collection of all factions
   */
  @NotNull
  public Collection<Faction> getAllFactions() {
    return Collections.unmodifiableCollection(factions.values());
  }

  /**
   * Gets faction count.
   *
   * @return number of factions
   */
  public int getFactionCount() {
    return factions.size();
  }

  // === Faction Operations ===

  /**
   * Result of a faction operation.
   */
  public enum FactionResult {
    SUCCESS,
    ALREADY_IN_FACTION,
    NOT_IN_FACTION,
    FACTION_NOT_FOUND,
    NAME_TAKEN,
    NAME_TOO_SHORT,
    NAME_TOO_LONG,
    FACTION_FULL,
    NOT_LEADER,
    NOT_OFFICER,
    CANNOT_KICK_LEADER,
    CANNOT_DEMOTE_MEMBER,
    CANNOT_PROMOTE_LEADER,
    TARGET_NOT_IN_FACTION,
    NO_PERMISSION
  }

  /**
   * Creates a new faction.
   *
   * @param name       the faction name
   * @param leaderUuid the leader's UUID
   * @param leaderName the leader's username
   * @return the result
   */
  public FactionResult createFaction(@NotNull String name, @NotNull UUID leaderUuid, @NotNull String leaderName) {
    // Check permission first
    if (!PermissionManager.get().hasPermission(leaderUuid, Permissions.CREATE)) {
      return FactionResult.NO_PERMISSION;
    }

    // Validation
    if (isInFaction(leaderUuid)) {
      return FactionResult.ALREADY_IN_FACTION;
    }

    ConfigManager config = ConfigManager.get();
    if (name.length() < config.getMinNameLength()) {
      return FactionResult.NAME_TOO_SHORT;
    }
    if (name.length() > config.getMaxNameLength()) {
      return FactionResult.NAME_TOO_LONG;
    }
    if (isNameTaken(name)) {
      return FactionResult.NAME_TAKEN;
    }

    // Create faction with auto-generated tag
    Faction faction = Faction.create(name, leaderUuid, leaderName);
    String generatedTag = generateUniqueTag(name);
    faction = faction.withTag(generatedTag);

    // Update caches
    factions.put(faction.id(), faction);
    nameToFaction.put(name.toLowerCase(), faction.id());
    playerToFaction.put(leaderUuid, faction.id());

    // Save async
    storage.saveFaction(faction);

    // Publish member join event for the creator (so membership history is recorded)
    EventBus.publish(new FactionMemberEvent(faction, leaderUuid, FactionMemberEvent.Type.JOIN));

    Logger.info("[Faction] Faction '%s' [%s] created by %s", name, generatedTag, leaderName);

    if (onFactionCreated != null) {
      try {
        onFactionCreated.accept(name, leaderName);
      } catch (Exception e) {
        ErrorHandler.report("Error in faction created callback", e);
      }
    }
    if (onFactionCreatedForEconomy != null) {
      try {
        onFactionCreatedForEconomy.accept(faction);
      } catch (Exception e) {
        ErrorHandler.report("Error in economy creation callback", e);
      }
    }

    return FactionResult.SUCCESS;
  }

  /**
   * Generates a unique faction tag from the faction name.
   * Takes first 3 alphanumeric characters (uppercase), appends numbers if collision exists.
   *
   * @param factionName the faction name to generate a tag from
   * @return a unique tag (1-5 chars)
   */
  @NotNull
  public String generateUniqueTag(@NotNull String factionName) {
    // Extract alphanumeric characters only, uppercase
    StringBuilder baseBuilder = new StringBuilder();
    for (char c : factionName.toCharArray()) {
      if (Character.isLetterOrDigit(c)) {
        baseBuilder.append(Character.toUpperCase(c));
        if (baseBuilder.length() >= 3) {
          break;
        }
      }
    }

    // Ensure at least 1 character
    if (baseBuilder.isEmpty()) {
      baseBuilder.append("F"); // Fallback to 'F' for Faction
    }

    String baseTag = baseBuilder.toString();

    // Check if base tag is unique
    if (getFactionByTag(baseTag) == null) {
      return baseTag;
    }

    // Append numbers until unique (max 5 chars total)
    for (int suffix = 2; suffix <= 99; suffix++) {
      String candidate = baseTag + suffix;
      if (candidate.length() > 5) {
        // Shorten base to make room for suffix
        int maxBaseLen = 5 - String.valueOf(suffix).length();
        candidate = baseTag.substring(0, Math.min(baseTag.length(), maxBaseLen)) + suffix;
      }
      if (getFactionByTag(candidate) == null) {
        return candidate;
      }
    }

    // Fallback: random suffix (very unlikely to reach here)
    return baseTag.substring(0, Math.min(baseTag.length(), 2)) + UUID.randomUUID().toString().substring(0, 3).toUpperCase();
  }

  /**
   * Disbands a faction.
   *
   * @param factionId  the faction ID
   * @param actorUuid  the actor's UUID (must be leader)
   * @return the result
   */
  public FactionResult disbandFaction(@NotNull UUID factionId, @NotNull UUID actorUuid) {
    // Check permission first
    if (!PermissionManager.get().hasPermission(actorUuid, Permissions.DISBAND)) {
      return FactionResult.NO_PERMISSION;
    }

    Faction faction = factions.get(factionId);
    if (faction == null) {
      return FactionResult.FACTION_NOT_FOUND;
    }

    // Check if actor is leader
    FactionMember actor = faction.getMember(actorUuid);
    if (actor == null || !actor.isLeader()) {
      return FactionResult.NOT_LEADER;
    }

    // Remove from caches
    factions.remove(factionId);
    nameToFaction.remove(faction.name().toLowerCase());
    for (UUID memberUuid : faction.members().keySet()) {
      playerToFaction.remove(memberUuid);
    }

    // Delete from storage
    storage.deleteFaction(factionId);

    // Fire event so listeners can clean up claims, relations, etc.
    EventBus.publish(new FactionDisbandEvent(faction, actorUuid));

    Logger.info("[Faction] Faction '%s' disbanded", faction.name());

    if (onFactionDisbanded != null) {
      try {
        onFactionDisbanded.accept(faction.name());
      } catch (Exception e) {
        ErrorHandler.report("Error in faction disbanded callback", e);
      }
    }

    return FactionResult.SUCCESS;
  }

  /**
   * Adds a member to a faction.
   *
   * @param factionId   the faction ID
   * @param playerUuid  the player's UUID
   * @param playerName  the player's username
   * @return the result
   */
  public FactionResult addMember(@NotNull UUID factionId, @NotNull UUID playerUuid, @NotNull String playerName) {
    Faction faction = factions.get(factionId);
    if (faction == null) {
      return FactionResult.FACTION_NOT_FOUND;
    }

    if (isInFaction(playerUuid)) {
      return FactionResult.ALREADY_IN_FACTION;
    }

    if (faction.getMemberCount() >= ConfigManager.get().getMaxMembers()) {
      return FactionResult.FACTION_FULL;
    }

    // Add member
    FactionMember member = FactionMember.create(playerUuid, playerName);
    Faction updated = faction.withMember(member)
      .withLog(FactionLog.create(FactionLog.LogType.MEMBER_JOIN, playerName + " joined the faction", playerUuid,
        MessageKeys.LogsGui.MSG_MEMBER_JOINED, playerName));

    // Update caches
    factions.put(factionId, updated);
    playerToFaction.put(playerUuid, factionId);

    // Save async
    storage.saveFaction(updated);

    // Publish member join event
    EventBus.publish(new FactionMemberEvent(updated, playerUuid, FactionMemberEvent.Type.JOIN));

    return FactionResult.SUCCESS;
  }

  /**
   * Removes a member from a faction.
   * If the leader leaves and other members exist, promotes the highest-ranked member with most tenure.
   * If the leader leaves and no other members exist, disbands the faction.
   *
   * @param factionId  the faction ID
   * @param playerUuid the player's UUID
   * @param actorUuid  the actor's UUID (self for leaving, officer+ for kicking)
   * @param isKick     true if this is a kick, false if leaving
   * @return the result
   */
  public FactionResult removeMember(@NotNull UUID factionId, @NotNull UUID playerUuid,
                   @NotNull UUID actorUuid, boolean isKick) {
    Faction faction = factions.get(factionId);
    if (faction == null) {
      return FactionResult.FACTION_NOT_FOUND;
    }

    FactionMember target = faction.getMember(playerUuid);
    if (target == null) {
      return FactionResult.TARGET_NOT_IN_FACTION;
    }

    // Handle leader removal with succession
    if (target.isLeader()) {
      // Find successor
      FactionMember successor = faction.findSuccessor();

      if (successor == null) {
        // No other members - disband the faction
        return disbandFactionInternal(factionId, "Leader left with no remaining members");
      }

      // Promote successor to leader
      FactionMember promoted = successor.withRole(FactionRole.LEADER);
      Faction updated = faction
          .withoutMember(playerUuid)
          .withMember(promoted)
          .withLog(FactionLog.create(FactionLog.LogType.LEADER_TRANSFER,
              target.username() + " left, " + promoted.username() + " is now leader", playerUuid,
              MessageKeys.LogsGui.MSG_LEADER_LEFT_TRANSFER, target.username(), promoted.username()));

      factions.put(factionId, updated);
      playerToFaction.remove(playerUuid);
      storage.saveFaction(updated);

      // Publish member leave event
      EventBus.publish(new FactionMemberEvent(updated, playerUuid, FactionMemberEvent.Type.LEAVE));

      Logger.info("[Faction] Leader %s left faction '%s', %s promoted to leader",
          target.username(), faction.name(), promoted.username());

      if (onLeadershipTransferred != null) {
        try {
          onLeadershipTransferred.accept(faction.name(), target.username(), promoted.username());
        } catch (Exception e) {
          ErrorHandler.report("Error in leadership transferred callback", e);
        }
      }

      return FactionResult.SUCCESS;
    }

    // If kicking (non-leader), check permissions
    if (isKick && !actorUuid.equals(playerUuid)) {
      FactionMember actor = faction.getMember(actorUuid);
      if (actor == null || !actor.role().canManage(target.role())) {
        return FactionResult.NOT_OFFICER;
      }
    }

    // Remove member
    FactionLog.LogType logType = isKick ? FactionLog.LogType.MEMBER_KICK : FactionLog.LogType.MEMBER_LEAVE;
    String message = isKick ? target.username() + " was kicked" : target.username() + " left the faction";
    String msgKey = isKick ? MessageKeys.LogsGui.MSG_MEMBER_KICKED : MessageKeys.LogsGui.MSG_MEMBER_LEFT;

    Faction updated = faction.withoutMember(playerUuid)
      .withLog(FactionLog.create(logType, message, actorUuid, msgKey, target.username()));

    // Update caches
    factions.put(factionId, updated);
    playerToFaction.remove(playerUuid);

    // Save async
    storage.saveFaction(updated);

    // Publish member leave/kick event
    FactionMemberEvent.Type eventType = isKick ? FactionMemberEvent.Type.KICK : FactionMemberEvent.Type.LEAVE;
    EventBus.publish(new FactionMemberEvent(updated, playerUuid, eventType));

    return FactionResult.SUCCESS;
  }

  /**
   * Disbands a faction internally (system-initiated).
   * Used when leader leaves with no remaining members, or for ban-triggered dissolution.
   *
   * @param factionId the faction ID
   * @param reason    the reason for disbanding (logged)
   * @return the result
   */
  private FactionResult disbandFactionInternal(@NotNull UUID factionId, @NotNull String reason) {
    Faction faction = factions.get(factionId);
    if (faction == null) {
      return FactionResult.FACTION_NOT_FOUND;
    }

    // Get leader UUID for event (use first found, or null for system-initiated)
    UUID disbandedBy = faction.getLeader() != null ? faction.getLeader().uuid() : null;

    // Remove from caches
    factions.remove(factionId);
    nameToFaction.remove(faction.name().toLowerCase());
    for (UUID memberUuid : faction.members().keySet()) {
      playerToFaction.remove(memberUuid);
    }

    // Delete from storage
    storage.deleteFaction(factionId);

    // Fire event so listeners can clean up claims, relations, etc.
    EventBus.publish(new FactionDisbandEvent(faction, disbandedBy));

    Logger.info("[Faction] Faction '%s' disbanded: %s", faction.name(), reason);
    return FactionResult.SUCCESS;
  }

  /**
   * Force disbands a faction without permission checks.
   * Used for admin operations and data imports.
   *
   * @param factionId the faction ID
   * @param reason    the reason for disbanding (logged)
   * @return the result
   */
  public FactionResult forceDisband(@NotNull UUID factionId, @NotNull String reason) {
    return disbandFactionInternal(factionId, reason);
  }

  /**
   * Promotes a member to the next rank.
   *
   * @param factionId  the faction ID
   * @param playerUuid the player's UUID to promote
   * @param actorUuid  the actor's UUID
   * @return the result
   */
  public FactionResult promoteMember(@NotNull UUID factionId, @NotNull UUID playerUuid, @NotNull UUID actorUuid) {
    // Check permission first
    if (!PermissionManager.get().hasPermission(actorUuid, Permissions.PROMOTE)) {
      return FactionResult.NO_PERMISSION;
    }

    Faction faction = factions.get(factionId);
    if (faction == null) {
      return FactionResult.FACTION_NOT_FOUND;
    }

    FactionMember actor = faction.getMember(actorUuid);
    if (actor == null || !actor.isLeader()) {
      return FactionResult.NOT_LEADER;
    }

    FactionMember target = faction.getMember(playerUuid);
    if (target == null) {
      return FactionResult.TARGET_NOT_IN_FACTION;
    }

    // Can't promote a leader
    if (target.role() == FactionRole.LEADER) {
      return FactionResult.CANNOT_PROMOTE_LEADER;
    }

    // Promote to next rank
    FactionRole newRole = target.role() == FactionRole.MEMBER ? FactionRole.OFFICER : FactionRole.LEADER;
    FactionMember promoted = target.withRole(newRole);

    Faction updated = faction.withMember(promoted)
      .withLog(FactionLog.create(FactionLog.LogType.MEMBER_PROMOTE,
        target.username() + " promoted to " + ConfigManager.get().getRoleDisplayName(newRole), actorUuid,
        MessageKeys.LogsGui.MSG_MEMBER_PROMOTED, target.username(), ConfigManager.get().getRoleDisplayName(newRole)));

    factions.put(factionId, updated);
    storage.saveFaction(updated);

    // Publish promote event
    EventBus.publish(new FactionMemberEvent(updated, playerUuid, FactionMemberEvent.Type.PROMOTE));

    return FactionResult.SUCCESS;
  }

  /**
   * Demotes a member to the previous rank.
   *
   * @param factionId  the faction ID
   * @param playerUuid the player's UUID to demote
   * @param actorUuid  the actor's UUID
   * @return the result
   */
  public FactionResult demoteMember(@NotNull UUID factionId, @NotNull UUID playerUuid, @NotNull UUID actorUuid) {
    // Check permission first
    if (!PermissionManager.get().hasPermission(actorUuid, Permissions.DEMOTE)) {
      return FactionResult.NO_PERMISSION;
    }

    Faction faction = factions.get(factionId);
    if (faction == null) {
      return FactionResult.FACTION_NOT_FOUND;
    }

    FactionMember actor = faction.getMember(actorUuid);
    if (actor == null || !actor.isLeader()) {
      return FactionResult.NOT_LEADER;
    }

    FactionMember target = faction.getMember(playerUuid);
    if (target == null) {
      return FactionResult.TARGET_NOT_IN_FACTION;
    }

    // Can't demote a member (already lowest)
    if (target.role() == FactionRole.MEMBER) {
      return FactionResult.CANNOT_DEMOTE_MEMBER;
    }

    // Demote
    FactionMember demoted = target.withRole(FactionRole.MEMBER);

    Faction updated = faction.withMember(demoted)
      .withLog(FactionLog.create(FactionLog.LogType.MEMBER_DEMOTE,
        target.username() + " demoted to " + ConfigManager.get().getRoleDisplayName(FactionRole.MEMBER), actorUuid,
        MessageKeys.LogsGui.MSG_MEMBER_DEMOTED, target.username(), ConfigManager.get().getRoleDisplayName(FactionRole.MEMBER)));

    factions.put(factionId, updated);
    storage.saveFaction(updated);

    // Publish demote event
    EventBus.publish(new FactionMemberEvent(updated, playerUuid, FactionMemberEvent.Type.DEMOTE));

    return FactionResult.SUCCESS;
  }

  /**
   * Transfers leadership to another member.
   *
   * @param factionId  the faction ID
   * @param newLeader  the new leader's UUID
   * @param actorUuid  the current leader's UUID
   * @return the result
   */
  public FactionResult transferLeadership(@NotNull UUID factionId, @NotNull UUID newLeader, @NotNull UUID actorUuid) {
    // Check permission first
    if (!PermissionManager.get().hasPermission(actorUuid, Permissions.TRANSFER)) {
      return FactionResult.NO_PERMISSION;
    }

    Faction faction = factions.get(factionId);
    if (faction == null) {
      return FactionResult.FACTION_NOT_FOUND;
    }

    FactionMember actor = faction.getMember(actorUuid);
    if (actor == null || !actor.isLeader()) {
      return FactionResult.NOT_LEADER;
    }

    FactionMember target = faction.getMember(newLeader);
    if (target == null) {
      return FactionResult.TARGET_NOT_IN_FACTION;
    }

    // Demote old leader, promote new leader
    FactionMember oldLeader = actor.withRole(FactionRole.OFFICER);
    FactionMember promoted = target.withRole(FactionRole.LEADER);

    Faction updated = faction
      .withMember(oldLeader)
      .withMember(promoted)
      .withLog(FactionLog.create(FactionLog.LogType.LEADER_TRANSFER,
        "Leadership transferred to " + target.username(), actorUuid,
        MessageKeys.LogsGui.MSG_LEADER_TRANSFERRED, target.username()));

    factions.put(factionId, updated);
    storage.saveFaction(updated);

    Logger.info("[Faction] Faction '%s' leadership transferred to %s", faction.name(), target.username());

    if (onLeadershipTransferred != null) {
      try {
        onLeadershipTransferred.accept(faction.name(), actor.username(), target.username());
      } catch (Exception e) {
        ErrorHandler.report("Error in leadership transferred callback", e);
      }
    }

    return FactionResult.SUCCESS;
  }

  /**
   * Admin: Sets a member's role without permission checks.
   * Handles leader transfers automatically.
   *
   * @param factionId  the faction ID
   * @param playerUuid the player's UUID
   * @param newRole    the new role
   * @return the result
   */
  public FactionResult adminSetMemberRole(@NotNull UUID factionId, @NotNull UUID playerUuid, @NotNull FactionRole newRole) {
    Faction faction = factions.get(factionId);
    if (faction == null) {
      return FactionResult.FACTION_NOT_FOUND;
    }

    FactionMember target = faction.getMember(playerUuid);
    if (target == null) {
      return FactionResult.TARGET_NOT_IN_FACTION;
    }

    // If promoting to leader, demote current leader
    Faction updated = faction;
    if (newRole == FactionRole.LEADER && target.role() != FactionRole.LEADER) {
      FactionMember currentLeader = faction.getLeader();
      if (currentLeader != null && !currentLeader.uuid().equals(playerUuid)) {
        FactionMember demotedLeader = currentLeader.withRole(FactionRole.OFFICER);
        updated = updated.withMember(demotedLeader);
      }
    }

    // Update target's role
    FactionMember updatedMember = target.withRole(newRole);
    updated = updated.withMember(updatedMember)
      .withLog(FactionLog.create(FactionLog.LogType.MEMBER_PROMOTE,
        "[Admin] " + target.username() + " role set to " + ConfigManager.get().getRoleDisplayName(newRole), null,
        MessageKeys.LogsGui.MSG_ADMIN_ROLE_SET, target.username(), ConfigManager.get().getRoleDisplayName(newRole)));

    factions.put(factionId, updated);
    storage.saveFaction(updated);

    Logger.info("[Admin] Set %s role to %s in faction '%s'",
        target.username(), ConfigManager.get().getRoleDisplayName(newRole), faction.name());
    return FactionResult.SUCCESS;
  }

  /**
   * Admin: Removes a member without permission checks.
   * Cannot remove leaders (must demote first or disband).
   *
   * @param factionId  the faction ID
   * @param playerUuid the player's UUID
   * @return the result
   */
  public FactionResult adminRemoveMember(@NotNull UUID factionId, @NotNull UUID playerUuid) {
    Faction faction = factions.get(factionId);
    if (faction == null) {
      return FactionResult.FACTION_NOT_FOUND;
    }

    FactionMember target = faction.getMember(playerUuid);
    if (target == null) {
      return FactionResult.TARGET_NOT_IN_FACTION;
    }

    // Can't kick the leader - must demote or disband
    if (target.role() == FactionRole.LEADER) {
      return FactionResult.CANNOT_KICK_LEADER;
    }

    // Remove member
    Faction updated = faction.withoutMember(playerUuid)
      .withLog(FactionLog.create(FactionLog.LogType.MEMBER_KICK,
        "[Admin] " + target.username() + " was kicked", null,
        MessageKeys.LogsGui.MSG_ADMIN_KICKED, target.username()));

    factions.put(factionId, updated);
    playerToFaction.remove(playerUuid);
    storage.saveFaction(updated);

    // Publish kick event (so membership history is recorded)
    EventBus.publish(new FactionMemberEvent(updated, playerUuid, FactionMemberEvent.Type.KICK));

    Logger.info("[Admin] Kicked %s from faction '%s'", target.username(), faction.name());
    return FactionResult.SUCCESS;
  }

  /**
   * Updates a faction's home.
   *
   * @param factionId the faction ID
   * @param home      the new home, or null to clear
   * @param actorUuid the actor's UUID
   * @return the result
   */
  public FactionResult setHome(@NotNull UUID factionId, @Nullable Faction.FactionHome home, @NotNull UUID actorUuid) {
    // Check permission first
    if (!PermissionManager.get().hasPermission(actorUuid, Permissions.SETHOME)) {
      return FactionResult.NO_PERMISSION;
    }

    Faction faction = factions.get(factionId);
    if (faction == null) {
      return FactionResult.FACTION_NOT_FOUND;
    }

    FactionMember actor = faction.getMember(actorUuid);
    if (actor == null || !actor.isOfficerOrHigher()) {
      return FactionResult.NOT_OFFICER;
    }

    Faction updated = faction.withHome(home)
      .withLog(FactionLog.create(FactionLog.LogType.HOME_SET,
        home != null ? "Home set" : "Home cleared", actorUuid,
        home != null ? MessageKeys.LogsGui.MSG_HOME_SET : MessageKeys.LogsGui.MSG_HOME_CLEARED));

    factions.put(factionId, updated);
    storage.saveFaction(updated);

    return FactionResult.SUCCESS;
  }

  /**
   * Clears faction homes that are set in worlds where claiming is disallowed.
   * Called on startup and config reload to clean up stale data
   * (e.g., homes set in instance worlds that no longer exist).
   *
   * @return the number of homes cleared
   */
  public int cleanupDisallowedWorldHomes() {
    int cleared = 0;
    for (Faction faction : factions.values()) {
      Faction.FactionHome home = faction.home();
      if (home != null && !ConfigManager.get().isWorldAllowed(home.world())) {
        Faction updated = faction.withHome(null)
          .withLog(FactionLog.create(FactionLog.LogType.HOME_SET,
            "Home in '" + home.world() + "' cleared (world disallows claiming)", null,
            MessageKeys.LogsGui.MSG_HOME_CLEARED_WORLD, home.world()));
        factions.put(faction.id(), updated);
        storage.saveFaction(updated);
        cleared++;
        Logger.info("[Cleanup] Cleared home for faction '%s' in disallowed world '%s'",
          faction.name(), home.world());
      }
    }
    return cleared;
  }

  /**
   * Updates a faction in the in-memory cache without triggering a save.
   * Used by PowerManager to sync hardcore power before bulk saves.
   *
   * @param faction the updated faction
   */
  void putFaction(@NotNull Faction faction) {
    factions.put(faction.id(), faction);
  }

  /**
   * Updates a faction in the cache and saves it.
   * Used by other managers (e.g., ClaimManager) to persist changes.
   *
   * <p>NOTE: This does NOT update player indices. If members were added or removed,
   * use updateFactionWithMemberChanges() instead.
   *
   * @param faction the updated faction
   */
  public void updateFaction(@NotNull Faction faction) {
    factions.put(faction.id(), faction);
    storage.saveFaction(faction);
  }

  /**
   * Updates a faction and synchronizes player indices for member changes.
   * This method compares old and new member lists and updates playerToFaction accordingly.
   *
   * @param faction the updated faction
   */
  public void updateFactionWithMemberChanges(@NotNull Faction faction) {
    Faction oldFaction = factions.get(faction.id());

    if (oldFaction != null) {
      // Find removed members and remove from index
      for (UUID memberUuid : oldFaction.members().keySet()) {
        if (!faction.members().containsKey(memberUuid)) {
          playerToFaction.remove(memberUuid);
        }
      }

      // Find added members and add to index
      for (UUID memberUuid : faction.members().keySet()) {
        if (!oldFaction.members().containsKey(memberUuid)) {
          playerToFaction.put(memberUuid, faction.id());
        }
      }
    } else {
      // New faction - add all members to index
      for (UUID memberUuid : faction.members().keySet()) {
        playerToFaction.put(memberUuid, faction.id());
      }
    }

    factions.put(faction.id(), faction);
    storage.saveFaction(faction);
  }

  /**
   * Removes a player from the player-to-faction index.
   * Used during import when manually handling member removals.
   *
   * @param playerUuid the player UUID to remove from the index
   */
  public void removePlayerFromIndex(@NotNull UUID playerUuid) {
    playerToFaction.remove(playerUuid);
  }

  /**
   * Imports or replaces a faction, properly updating all indices.
   * Used for data import operations where faction data comes from external sources.
   * This method handles:
   * - Removing old members from playerToFaction index if overwriting
   * - Adding new members to playerToFaction index
   * - Updating nameToFaction index
   *
   * @param faction the faction to import
   * @param overwrite if true, replaces existing faction with same ID
   * @return true if imported successfully, false if faction exists and overwrite is false
   */
  public boolean importFaction(@NotNull Faction faction, boolean overwrite) {
    Faction existing = factions.get(faction.id());

    if (existing != null && !overwrite) {
      return false;
    }

    // If overwriting, clean up old member indices
    if (existing != null) {
      // Remove old name mapping if name changed
      if (!existing.name().equalsIgnoreCase(faction.name())) {
        nameToFaction.remove(existing.name().toLowerCase());
      }

      // Remove old members from index
      for (UUID memberUuid : existing.members().keySet()) {
        playerToFaction.remove(memberUuid);
      }
    }

    // Add new faction to caches
    factions.put(faction.id(), faction);
    nameToFaction.put(faction.name().toLowerCase(), faction.id());

    // Add new members to index
    for (UUID memberUuid : faction.members().keySet()) {
      playerToFaction.put(memberUuid, faction.id());
    }

    // Save to storage
    storage.saveFaction(faction);

    Logger.info("[Import] Imported faction '%s' with %d members", faction.name(), faction.getMemberCount());
    return true;
  }

  /**
   * Updates a member's last online time.
   *
   * @param playerUuid the player's UUID
   */
  public void updateLastOnline(@NotNull UUID playerUuid) {
    UUID factionId = playerToFaction.get(playerUuid);
    if (factionId == null) {
      return;
    }

    Faction faction = factions.get(factionId);
    if (faction == null) {
      return;
    }

    FactionMember member = faction.getMember(playerUuid);
    if (member == null) {
      return;
    }

    FactionMember updated = member.withLastOnline(System.currentTimeMillis());
    Faction updatedFaction = faction.withMember(updated);

    factions.put(factionId, updatedFaction);
    // Don't save immediately for last online updates - batch save later
  }
}
