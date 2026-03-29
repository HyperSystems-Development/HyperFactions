package com.hyperfactions.manager;

import com.hyperfactions.Permissions;
import com.hyperfactions.api.events.*;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.JoinRequest;
import com.hyperfactions.integration.PermissionManager;
import com.hyperfactions.storage.JoinRequestStorage;
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
 * Manages join requests from players wanting to join closed factions.
 * Uses dual-indexed storage for efficient lookups by player and faction.
 * Persists requests to JSON file.
 */
public class JoinRequestManager {

  /**
   * Result of creating or processing a join request.
   */
  public enum RequestResult {
    SUCCESS,
    NO_PERMISSION,
    ALREADY_REQUESTED,
    PLAYER_IN_FACTION,
    FACTION_NOT_FOUND,
    FACTION_IS_OPEN,
    REQUEST_NOT_FOUND
  }

  // Requests by player: player UUID -> set of requests
  private final Map<UUID, Set<JoinRequest>> requestsByPlayer = new ConcurrentHashMap<>();

  // Requests by faction: faction ID -> set of player UUIDs who requested
  private final Map<UUID, Set<UUID>> requestsByFaction = new ConcurrentHashMap<>();

  // Persistence
  private final JoinRequestStorage storage;

  // GUI update callbacks
  @Nullable
  private Consumer<JoinRequest> onRequestCreated;

  @Nullable
  private BiConsumer<UUID, UUID> onRequestAccepted;

  @Nullable
  private BiConsumer<UUID, UUID> onRequestDeclined;

  /**
   * Creates a new JoinRequestManager with persistence.
   *
   * @param storage the storage provider for join requests
   */
  public JoinRequestManager(@NotNull JoinRequestStorage storage) {
    this.storage = storage;
  }

  /**
   * Sets a callback for when a request is created.
   */
  public void setOnRequestCreated(@Nullable Consumer<JoinRequest> callback) {
    this.onRequestCreated = callback;
  }

  /**
   * Sets a callback for when a request is accepted.
   * Params: factionId, playerUuid
   */
  public void setOnRequestAccepted(@Nullable BiConsumer<UUID, UUID> callback) {
    this.onRequestAccepted = callback;
  }

  /**
   * Sets a callback for when a request is declined.
   * Params: factionId, playerUuid
   */
  public void setOnRequestDeclined(@Nullable BiConsumer<UUID, UUID> callback) {
    this.onRequestDeclined = callback;
  }

  /**
   * Initializes the manager by loading persisted requests.
   */
  public void init() {
    List<JoinRequest> loaded = storage.loadAll().join();
    for (JoinRequest request : loaded) {
      requestsByPlayer.computeIfAbsent(request.playerUuid(), k -> ConcurrentHashMap.newKeySet())
        .add(request);
      requestsByFaction.computeIfAbsent(request.factionId(), k -> ConcurrentHashMap.newKeySet())
        .add(request.playerUuid());
    }
  }

  /**
   * Shuts down the manager, saving any pending data.
   */
  public void shutdown() {
    save();
  }

  /**
   * Result of a permission-checked request creation.
   *
   * @param result  the result of the operation
   * @param request the created request if successful, null otherwise
   */
  public record CreateRequestResult(
    @NotNull RequestResult result,
    @Nullable JoinRequest request
  ) {
    /** Checks if success. */
    public boolean isSuccess() {
      return result == RequestResult.SUCCESS;
    }
  }

  /**
   * Creates a new join request with permission check.
   *
   * @param factionId  the faction ID
   * @param playerUuid the player's UUID
   * @param playerName the player's name
   * @param message    optional intro message
   * @return the result with the created request if successful
   */
  @NotNull
  public CreateRequestResult createRequestChecked(@NotNull UUID factionId, @NotNull UUID playerUuid,
                          @NotNull String playerName, @Nullable String message) {
    // Check permission first
    if (!PermissionManager.get().hasPermission(playerUuid, Permissions.JOIN)) {
      return new CreateRequestResult(RequestResult.NO_PERMISSION, null);
    }

    JoinRequest request = createRequest(factionId, playerUuid, playerName, message);
    return new CreateRequestResult(RequestResult.SUCCESS, request);
  }

  /**
   * Creates a new join request.
   * Note: For permission-checked creation, use {@link #createRequestChecked}.
   *
   * @param factionId  the faction ID
   * @param playerUuid the player's UUID
   * @param playerName the player's name
   * @param message    optional intro message
   * @return the created request
   */
  @NotNull
  public JoinRequest createRequest(@NotNull UUID factionId, @NotNull UUID playerUuid,
                   @NotNull String playerName, @Nullable String message) {
    // Remove any existing request to this faction
    removeRequestInternal(factionId, playerUuid);

    long durationMs = ConfigManager.get().getJoinRequestExpirationMs();
    JoinRequest request = JoinRequest.create(factionId, playerUuid, playerName, message, durationMs);

    requestsByPlayer.computeIfAbsent(playerUuid, k -> ConcurrentHashMap.newKeySet())
      .add(request);

    requestsByFaction.computeIfAbsent(factionId, k -> ConcurrentHashMap.newKeySet())
      .add(playerUuid);

    save();

    if (onRequestCreated != null) {
      try {
        onRequestCreated.accept(request);
      } catch (Exception e) {
        ErrorHandler.report("Error in request created callback", e);
      }
    }

    EventBus.publish(new FactionJoinRequestEvent(factionId, playerUuid, FactionJoinRequestEvent.Type.CREATED, message));
    return request;
  }

  /**
   * Gets a request from a specific player for a faction.
   *
   * @param factionId  the faction ID
   * @param playerUuid the player's UUID
   * @return the request, or null if not found or expired
   */
  @Nullable
  public JoinRequest getRequest(@NotNull UUID factionId, @NotNull UUID playerUuid) {
    Set<JoinRequest> requests = requestsByPlayer.get(playerUuid);
    if (requests == null) {
      return null;
    }

    for (JoinRequest request : requests) {
      if (request.factionId().equals(factionId)) {
        if (request.isExpired()) {
          removeRequest(factionId, playerUuid);
          return null;
        }
        return request;
      }
    }
    return null;
  }

  /**
   * Gets all pending requests for a faction.
   *
   * @param factionId the faction ID
   * @return list of non-expired requests
   */
  @NotNull
  public List<JoinRequest> getFactionRequests(@NotNull UUID factionId) {
    Set<UUID> playerUuids = requestsByFaction.get(factionId);
    if (playerUuids == null || playerUuids.isEmpty()) {
      return Collections.emptyList();
    }

    List<JoinRequest> valid = new ArrayList<>();
    Set<UUID> expired = new HashSet<>();

    for (UUID playerUuid : playerUuids) {
      JoinRequest request = getRequest(factionId, playerUuid);
      if (request != null && !request.isExpired()) {
        valid.add(request);
      } else {
        expired.add(playerUuid);
      }
    }

    // Clean up expired
    for (UUID playerUuid : expired) {
      removeRequestInternal(factionId, playerUuid);
    }
    if (!expired.isEmpty()) {
      save();
    }

    // Sort by creation time (oldest first)
    valid.sort(Comparator.comparingLong(JoinRequest::createdAt));
    return valid;
  }

  /**
   * Gets the count of pending requests for a faction.
   *
   * @param factionId the faction ID
   * @return count of non-expired requests
   */
  public int getFactionRequestCount(@NotNull UUID factionId) {
    return getFactionRequests(factionId).size();
  }

  /**
   * Gets all pending requests from a player.
   *
   * @param playerUuid the player's UUID
   * @return list of non-expired requests
   */
  @NotNull
  public List<JoinRequest> getPlayerRequests(@NotNull UUID playerUuid) {
    Set<JoinRequest> requests = requestsByPlayer.get(playerUuid);
    if (requests == null) {
      return Collections.emptyList();
    }

    // Filter out expired and return
    List<JoinRequest> valid = requests.stream()
      .filter(r -> !r.isExpired())
      .collect(Collectors.toList());

    // Clean up expired
    if (valid.size() != requests.size()) {
      requests.removeIf(JoinRequest::isExpired);
      save();
    }

    return valid;
  }

  /**
   * Checks if a player has a pending request to a faction.
   *
   * @param factionId  the faction ID
   * @param playerUuid the player's UUID
   * @return true if has valid request
   */
  public boolean hasRequest(@NotNull UUID factionId, @NotNull UUID playerUuid) {
    return getRequest(factionId, playerUuid) != null;
  }

  /**
   * Accepts a join request and returns the request for processing.
   * The caller should handle actually adding the player to the faction.
   *
   * @param factionId  the faction ID
   * @param playerUuid the player's UUID
   * @return the accepted request, or null if not found
   */
  @Nullable
  public JoinRequest acceptRequest(@NotNull UUID factionId, @NotNull UUID playerUuid) {
    JoinRequest request = getRequest(factionId, playerUuid);
    if (request != null) {
      removeRequestInternal(factionId, playerUuid);
      save();

      if (onRequestAccepted != null) {
        try {
          onRequestAccepted.accept(factionId, playerUuid);
        } catch (Exception e) {
          ErrorHandler.report("Error in request accepted callback", e);
        }
      }

      EventBus.publish(new FactionJoinRequestEvent(factionId, playerUuid, FactionJoinRequestEvent.Type.ACCEPTED, null));
    }
    return request;
  }

  /**
   * Declines a join request.
   *
   * @param factionId  the faction ID
   * @param playerUuid the player's UUID
   */
  public void declineRequest(@NotNull UUID factionId, @NotNull UUID playerUuid) {
    removeRequestInternal(factionId, playerUuid);
    save();

    if (onRequestDeclined != null) {
      try {
        onRequestDeclined.accept(factionId, playerUuid);
      } catch (Exception e) {
        ErrorHandler.report("Error in request declined callback", e);
      }
    }

    EventBus.publish(new FactionJoinRequestEvent(factionId, playerUuid, FactionJoinRequestEvent.Type.DECLINED, null));
  }

  /**
   * Removes a request.
   *
   * @param factionId  the faction ID
   * @param playerUuid the player's UUID
   */
  public void removeRequest(@NotNull UUID factionId, @NotNull UUID playerUuid) {
    removeRequestInternal(factionId, playerUuid);
    save();
  }

  /**
   * Internal remove without save (for batch operations).
   */
  private void removeRequestInternal(@NotNull UUID factionId, @NotNull UUID playerUuid) {
    Set<JoinRequest> requests = requestsByPlayer.get(playerUuid);
    if (requests != null) {
      requests.removeIf(r -> r.factionId().equals(factionId));
      if (requests.isEmpty()) {
        requestsByPlayer.remove(playerUuid);
      }
    }

    Set<UUID> factionRequests = requestsByFaction.get(factionId);
    if (factionRequests != null) {
      factionRequests.remove(playerUuid);
      if (factionRequests.isEmpty()) {
        requestsByFaction.remove(factionId);
      }
    }
  }

  /**
   * Removes all requests from a player.
   * Called when a player joins any faction.
   *
   * @param playerUuid the player's UUID
   */
  public void clearPlayerRequests(@NotNull UUID playerUuid) {
    Set<JoinRequest> requests = requestsByPlayer.remove(playerUuid);
    if (requests != null) {
      for (JoinRequest request : requests) {
        Set<UUID> factionRequests = requestsByFaction.get(request.factionId());
        if (factionRequests != null) {
          factionRequests.remove(playerUuid);
          if (factionRequests.isEmpty()) {
            requestsByFaction.remove(request.factionId());
          }
        }
      }
      save();
    }
  }

  /**
   * Removes all requests to a faction.
   * Called when a faction disbands.
   *
   * @param factionId the faction ID
   */
  public void clearFactionRequests(@NotNull UUID factionId) {
    Set<UUID> requesters = requestsByFaction.remove(factionId);
    if (requesters != null) {
      for (UUID playerUuid : requesters) {
        Set<JoinRequest> requests = requestsByPlayer.get(playerUuid);
        if (requests != null) {
          requests.removeIf(r -> r.factionId().equals(factionId));
          if (requests.isEmpty()) {
            requestsByPlayer.remove(playerUuid);
          }
        }
      }
      save();
    }
  }

  /**
   * Cleans up all expired requests.
   * Call periodically.
   */
  public void cleanupExpired() {
    boolean changed = false;

    for (Map.Entry<UUID, Set<JoinRequest>> entry : requestsByPlayer.entrySet()) {
      int before = entry.getValue().size();
      // Publish EXPIRED events before removing
      for (JoinRequest request : entry.getValue()) {
        if (request.isExpired()) {
          EventBus.publish(new FactionJoinRequestEvent(request.factionId(), request.playerUuid(), FactionJoinRequestEvent.Type.EXPIRED, null));
        }
      }
      entry.getValue().removeIf(JoinRequest::isExpired);
      if (entry.getValue().size() != before) {
        changed = true;
      }
      if (entry.getValue().isEmpty()) {
        requestsByPlayer.remove(entry.getKey());
      }
    }

    // Rebuild faction index
    requestsByFaction.clear();
    for (Map.Entry<UUID, Set<JoinRequest>> entry : requestsByPlayer.entrySet()) {
      for (JoinRequest request : entry.getValue()) {
        requestsByFaction.computeIfAbsent(request.factionId(), k -> ConcurrentHashMap.newKeySet())
          .add(entry.getKey());
      }
    }

    if (changed) {
      save();
    }
  }

  // === Persistence ===

  /**
   * Collects all non-expired requests from the in-memory maps and saves via storage.
   */
  private void save() {
    List<JoinRequest> all = new ArrayList<>();
    for (Set<JoinRequest> requests : requestsByPlayer.values()) {
      for (JoinRequest request : requests) {
        if (!request.isExpired()) {
          all.add(request);
        }
      }
    }
    storage.saveAll(all).join();
  }
}
