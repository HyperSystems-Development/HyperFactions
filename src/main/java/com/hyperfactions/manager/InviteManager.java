package com.hyperfactions.manager;

import com.hyperfactions.Permissions;
import com.hyperfactions.api.events.*;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.PendingInvite;
import com.hyperfactions.integration.PermissionManager;
import com.hyperfactions.storage.InviteStorage;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Manages pending faction invitations.
 * Persists invites to JSON file.
 */
public class InviteManager {

  // Invites by player: player UUID -> set of invites
  private final Map<UUID, Set<PendingInvite>> invitesByPlayer = new ConcurrentHashMap<>();

  // Invites by faction: faction ID -> set of invited player UUIDs
  private final Map<UUID, Set<UUID>> invitesByFaction = new ConcurrentHashMap<>();

  // Persistence
  private final InviteStorage storage;

  // GUI update callbacks
  @Nullable
  private Consumer<PendingInvite> onInviteCreated;

  @Nullable
  private BiConsumer<UUID, UUID> onInviteRemoved;

  /**
   * Creates a new InviteManager with persistence.
   *
   * @param storage the storage provider for invites
   */
  public InviteManager(@NotNull InviteStorage storage) {
    this.storage = storage;
  }

  /**
   * Sets a callback for when an invite is created.
   */
  public void setOnInviteCreated(@Nullable Consumer<PendingInvite> callback) {
    this.onInviteCreated = callback;
  }

  /**
   * Sets a callback for when an invite is removed.
   * Params: factionId, playerUuid
   */
  public void setOnInviteRemoved(@Nullable BiConsumer<UUID, UUID> callback) {
    this.onInviteRemoved = callback;
  }

  /**
   * Initializes the manager by loading persisted invites.
   */
  public void init() {
    List<PendingInvite> loaded = storage.loadAll().join();
    for (PendingInvite invite : loaded) {
      invitesByPlayer.computeIfAbsent(invite.playerUuid(), k -> ConcurrentHashMap.newKeySet())
        .add(invite);
      invitesByFaction.computeIfAbsent(invite.factionId(), k -> ConcurrentHashMap.newKeySet())
        .add(invite.playerUuid());
    }
  }

  /**
   * Shuts down the manager, saving any pending data.
   */
  public void shutdown() {
    save();
  }

  /**
   * Result of an invite operation.
   */
  public enum InviteResult {
    SUCCESS,
    NO_PERMISSION
  }

  /**
   * Result of a permission-checked invite creation.
   *
   * @param result the result of the operation
   * @param invite the created invite if successful, null otherwise
   */
  public record CreateInviteResult(
    @NotNull InviteResult result,
    @Nullable PendingInvite invite
  ) {
    /** Checks if success. */
    public boolean isSuccess() {
      return result == InviteResult.SUCCESS;
    }
  }

  /**
   * Creates a new invite with permission check.
   *
   * @param factionId  the faction ID
   * @param playerUuid the invited player's UUID
   * @param invitedBy  UUID of the inviter
   * @return the result with the created invite if successful
   */
  @NotNull
  public CreateInviteResult createInviteChecked(@NotNull UUID factionId, @NotNull UUID playerUuid, @NotNull UUID invitedBy) {
    // Check permission first
    if (!PermissionManager.get().hasPermission(invitedBy, Permissions.INVITE)) {
      return new CreateInviteResult(InviteResult.NO_PERMISSION, null);
    }

    PendingInvite invite = createInvite(factionId, playerUuid, invitedBy);
    return new CreateInviteResult(InviteResult.SUCCESS, invite);
  }

  /**
   * Creates a new invite.
   * Note: For permission-checked creation, use {@link #createInviteChecked}.
   *
   * @param factionId  the faction ID
   * @param playerUuid the invited player's UUID
   * @param invitedBy  UUID of the inviter
   * @return the created invite
   */
  @NotNull
  public PendingInvite createInvite(@NotNull UUID factionId, @NotNull UUID playerUuid, @NotNull UUID invitedBy) {
    // Remove any existing invite from this faction
    removeInviteInternal(factionId, playerUuid);

    long durationMs = ConfigManager.get().getInviteExpirationMs();
    PendingInvite invite = PendingInvite.create(factionId, playerUuid, invitedBy, durationMs);

    invitesByPlayer.computeIfAbsent(playerUuid, k -> ConcurrentHashMap.newKeySet())
      .add(invite);

    invitesByFaction.computeIfAbsent(factionId, k -> ConcurrentHashMap.newKeySet())
      .add(playerUuid);

    save();

    if (onInviteCreated != null) {
      try {
        onInviteCreated.accept(invite);
      } catch (Exception e) {
        ErrorHandler.report("Error in invite created callback", e);
      }
    }

    EventBus.publish(new FactionInviteEvent(factionId, playerUuid, invitedBy, FactionInviteEvent.Type.CREATED));
    return invite;
  }

  /**
   * Gets an invite from a specific faction for a player.
   *
   * @param factionId  the faction ID
   * @param playerUuid the player's UUID
   * @return the invite, or null if not found or expired
   */
  @Nullable
  public PendingInvite getInvite(@NotNull UUID factionId, @NotNull UUID playerUuid) {
    Set<PendingInvite> invites = invitesByPlayer.get(playerUuid);
    if (invites == null) {
      return null;
    }

    for (PendingInvite invite : invites) {
      if (invite.factionId().equals(factionId)) {
        if (invite.isExpired()) {
          removeInvite(factionId, playerUuid);
          return null;
        }
        return invite;
      }
    }
    return null;
  }

  /**
   * Gets all pending invites for a player.
   *
   * @param playerUuid the player's UUID
   * @return list of non-expired invites
   */
  @NotNull
  public List<PendingInvite> getPlayerInvites(@NotNull UUID playerUuid) {
    Set<PendingInvite> invites = invitesByPlayer.get(playerUuid);
    if (invites == null) {
      return Collections.emptyList();
    }

    // Filter out expired and return
    List<PendingInvite> valid = invites.stream()
      .filter(i -> !i.isExpired())
      .collect(Collectors.toList());

    // Clean up expired
    if (valid.size() != invites.size()) {
      invites.removeIf(PendingInvite::isExpired);
      save();
    }

    return valid;
  }

  /**
   * Checks if a player has any pending invites.
   *
   * @param playerUuid the player's UUID
   * @return true if has non-expired invites
   */
  public boolean hasInvites(@NotNull UUID playerUuid) {
    return !getPlayerInvites(playerUuid).isEmpty();
  }

  /**
   * Checks if a player has an invite from a specific faction.
   *
   * @param factionId  the faction ID
   * @param playerUuid the player's UUID
   * @return true if has valid invite
   */
  public boolean hasInvite(@NotNull UUID factionId, @NotNull UUID playerUuid) {
    return getInvite(factionId, playerUuid) != null;
  }

  /**
   * Gets all players invited by a faction.
   *
   * @param factionId the faction ID
   * @return set of invited player UUIDs
   */
  @NotNull
  public Set<UUID> getFactionInvites(@NotNull UUID factionId) {
    Set<UUID> invited = invitesByFaction.get(factionId);
    if (invited == null) {
      return Collections.emptySet();
    }

    // Filter expired invites
    Set<UUID> valid = new HashSet<>();
    for (UUID playerUuid : invited) {
      if (hasInvite(factionId, playerUuid)) {
        valid.add(playerUuid);
      }
    }

    return valid;
  }

  /**
   * Gets all pending invites for a faction with full invite details.
   *
   * @param factionId the faction ID
   * @return list of non-expired invites
   */
  @NotNull
  public List<PendingInvite> getFactionInvitesList(@NotNull UUID factionId) {
    Set<UUID> playerUuids = invitesByFaction.get(factionId);
    if (playerUuids == null || playerUuids.isEmpty()) {
      return Collections.emptyList();
    }

    List<PendingInvite> valid = new ArrayList<>();
    Set<UUID> expired = new HashSet<>();

    for (UUID playerUuid : playerUuids) {
      PendingInvite invite = getInvite(factionId, playerUuid);
      if (invite != null && !invite.isExpired()) {
        valid.add(invite);
      } else {
        expired.add(playerUuid);
      }
    }

    // Clean up expired
    for (UUID playerUuid : expired) {
      removeInviteInternal(factionId, playerUuid);
    }
    if (!expired.isEmpty()) {
      save();
    }

    // Sort by creation time (oldest first)
    valid.sort(Comparator.comparingLong(PendingInvite::createdAt));
    return valid;
  }

  /**
   * Gets the count of pending invites for a faction.
   *
   * @param factionId the faction ID
   * @return count of non-expired invites
   */
  public int getFactionInviteCount(@NotNull UUID factionId) {
    return getFactionInvitesList(factionId).size();
  }

  /**
   * Removes an invite.
   *
   * @param factionId  the faction ID
   * @param playerUuid the player's UUID
   */
  public void removeInvite(@NotNull UUID factionId, @NotNull UUID playerUuid) {
    removeInviteInternal(factionId, playerUuid);
    save();

    if (onInviteRemoved != null) {
      try {
        onInviteRemoved.accept(factionId, playerUuid);
      } catch (Exception e) {
        ErrorHandler.report("Error in invite removed callback", e);
      }
    }
  }

  /**
   * Internal remove without save (for batch operations).
   */
  private void removeInviteInternal(@NotNull UUID factionId, @NotNull UUID playerUuid) {
    Set<PendingInvite> invites = invitesByPlayer.get(playerUuid);
    if (invites != null) {
      invites.removeIf(i -> i.factionId().equals(factionId));
      if (invites.isEmpty()) {
        invitesByPlayer.remove(playerUuid);
      }
    }

    Set<UUID> factionInvites = invitesByFaction.get(factionId);
    if (factionInvites != null) {
      factionInvites.remove(playerUuid);
      if (factionInvites.isEmpty()) {
        invitesByFaction.remove(factionId);
      }
    }
  }

  /**
   * Removes all invites for a player.
   *
   * @param playerUuid the player's UUID
   */
  public void clearPlayerInvites(@NotNull UUID playerUuid) {
    Set<PendingInvite> invites = invitesByPlayer.remove(playerUuid);
    if (invites != null) {
      for (PendingInvite invite : invites) {
        Set<UUID> factionInvites = invitesByFaction.get(invite.factionId());
        if (factionInvites != null) {
          factionInvites.remove(playerUuid);
        }
      }
      save();
    }
  }

  /**
   * Removes all invites from a faction.
   *
   * @param factionId the faction ID
   */
  public void clearFactionInvites(@NotNull UUID factionId) {
    Set<UUID> invited = invitesByFaction.remove(factionId);
    if (invited != null) {
      for (UUID playerUuid : invited) {
        Set<PendingInvite> invites = invitesByPlayer.get(playerUuid);
        if (invites != null) {
          invites.removeIf(i -> i.factionId().equals(factionId));
          if (invites.isEmpty()) {
            invitesByPlayer.remove(playerUuid);
          }
        }
      }
      save();
    }
  }

  /**
   * Cleans up all expired invites.
   * Call periodically.
   */
  public void cleanupExpired() {
    boolean changed = false;

    for (Map.Entry<UUID, Set<PendingInvite>> entry : invitesByPlayer.entrySet()) {
      int before = entry.getValue().size();
      // Publish EXPIRED events before removing
      for (PendingInvite invite : entry.getValue()) {
        if (invite.isExpired()) {
          EventBus.publish(new FactionInviteEvent(invite.factionId(), invite.playerUuid(), invite.invitedBy(), FactionInviteEvent.Type.EXPIRED));
        }
      }
      entry.getValue().removeIf(PendingInvite::isExpired);
      if (entry.getValue().size() != before) {
        changed = true;
      }
      if (entry.getValue().isEmpty()) {
        invitesByPlayer.remove(entry.getKey());
      }
    }

    // Rebuild faction index
    invitesByFaction.clear();
    for (Map.Entry<UUID, Set<PendingInvite>> entry : invitesByPlayer.entrySet()) {
      for (PendingInvite invite : entry.getValue()) {
        invitesByFaction.computeIfAbsent(invite.factionId(), k -> ConcurrentHashMap.newKeySet())
          .add(entry.getKey());
      }
    }

    if (changed) {
      save();
    }
  }

  // === Persistence ===

  /**
   * Collects all non-expired invites from the in-memory maps and saves via storage.
   */
  private void save() {
    List<PendingInvite> all = new ArrayList<>();
    for (Set<PendingInvite> invites : invitesByPlayer.values()) {
      for (PendingInvite invite : invites) {
        if (!invite.isExpired()) {
          all.add(invite);
        }
      }
    }
    storage.saveAll(all).join();
  }
}
