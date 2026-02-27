package com.hyperfactions.manager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Manages command confirmation prompts for destructive actions in text mode.
 * Players must confirm certain actions (disband, leave, transfer) within a timeout period.
 */
public class ConfirmationManager {

  /** Default confirmation timeout in milliseconds (30 seconds). */
  public static final long DEFAULT_TIMEOUT_MS = 30_000;

  /** Map of player UUID to their pending confirmation. */
  private final Map<UUID, PendingConfirmation> pendingConfirmations = new ConcurrentHashMap<>();

  /**
   * Types of confirmable actions.
   */
  public enum ConfirmationType {
    DISBAND("disband your faction"),
    LEAVE("leave your faction"),
    TRANSFER("transfer faction leadership"),
    RESTORE_BACKUP("restore from backup (this will overwrite current data)");

    private final String description;

    ConfirmationType(String description) {
      this.description = description;
    }

    /** Returns the description. */
    public String getDescription() {
      return description;
    }
  }

  /**
   * Represents a pending confirmation from a player.
   */
  public record PendingConfirmation(
      @NotNull ConfirmationType type,
      @NotNull UUID playerUuid,
      @Nullable UUID targetUuid,  // For transfer: target player UUID
      long createdAt,
      long expiresAt
  ) {
    /**
     * Checks if this confirmation has expired.
     */
    public boolean isExpired() {
      return System.currentTimeMillis() > expiresAt;
    }
  }

  /**
   * Result of a confirmation check.
   */
  public enum ConfirmationResult {
    /** No pending confirmation exists - created a new one. */
    NEEDS_CONFIRMATION,
    /** Pending confirmation exists and matches - action confirmed. */
    CONFIRMED,
    /** Pending confirmation exists but doesn't match (different action/target). */
    DIFFERENT_ACTION,
    /** Previous confirmation expired - created a new one. */
    EXPIRED_RECREATED
  }

  /**
   * Checks if a player has a pending confirmation and either confirms it or creates a new one.
   *
   * @param playerUuid The player's UUID
   * @param type       The type of action being confirmed
   * @param targetUuid Optional target UUID (for transfer actions)
   * @return The confirmation result
   */
  @NotNull
  public ConfirmationResult checkOrCreate(
      @NotNull UUID playerUuid,
      @NotNull ConfirmationType type,
      @Nullable UUID targetUuid) {

    PendingConfirmation existing = pendingConfirmations.get(playerUuid);

    if (existing != null) {
      // Check if expired
      if (existing.isExpired()) {
        // Create new confirmation
        createConfirmation(playerUuid, type, targetUuid);
        return ConfirmationResult.EXPIRED_RECREATED;
      }

      // Check if same action/target
      boolean sameType = existing.type() == type;
      boolean sameTarget = (existing.targetUuid() == null && targetUuid == null)
          || (existing.targetUuid() != null && existing.targetUuid().equals(targetUuid));

      if (sameType && sameTarget) {
        // Confirmed! Remove the pending confirmation
        pendingConfirmations.remove(playerUuid);
        return ConfirmationResult.CONFIRMED;
      } else {
        // Different action - replace with new confirmation
        createConfirmation(playerUuid, type, targetUuid);
        return ConfirmationResult.DIFFERENT_ACTION;
      }
    }

    // No existing confirmation - create one
    createConfirmation(playerUuid, type, targetUuid);
    return ConfirmationResult.NEEDS_CONFIRMATION;
  }

  /**
   * Creates a new pending confirmation for a player.
   *
   * @param playerUuid The player's UUID
   * @param type       The type of action
   * @param targetUuid Optional target UUID (for transfer)
   */
  private void createConfirmation(
      @NotNull UUID playerUuid,
      @NotNull ConfirmationType type,
      @Nullable UUID targetUuid) {

    long now = System.currentTimeMillis();
    PendingConfirmation confirmation = new PendingConfirmation(
        type,
        playerUuid,
        targetUuid,
        now,
        now + DEFAULT_TIMEOUT_MS
    );
    pendingConfirmations.put(playerUuid, confirmation);
  }

  /**
   * Gets the pending confirmation for a player, if any.
   *
   * @param playerUuid The player's UUID
   * @return The pending confirmation, or null if none exists or it expired
   */
  @Nullable
  public PendingConfirmation getPending(@NotNull UUID playerUuid) {
    PendingConfirmation confirmation = pendingConfirmations.get(playerUuid);
    if (confirmation != null && confirmation.isExpired()) {
      pendingConfirmations.remove(playerUuid);
      return null;
    }
    return confirmation;
  }

  /**
   * Cancels any pending confirmation for a player.
   *
   * @param playerUuid The player's UUID
   */
  public void cancel(@NotNull UUID playerUuid) {
    pendingConfirmations.remove(playerUuid);
  }

  /**
   * Cleans up expired confirmations.
   * Should be called periodically (e.g., every minute) to prevent memory leaks.
   */
  public void cleanupExpired() {
    long now = System.currentTimeMillis();
    pendingConfirmations.entrySet().removeIf(entry -> entry.getValue().expiresAt < now);
  }

  /**
   * Gets the timeout duration in seconds.
   *
   * @return The timeout in seconds
   */
  public int getTimeoutSeconds() {
    return (int) (DEFAULT_TIMEOUT_MS / 1000);
  }
}
