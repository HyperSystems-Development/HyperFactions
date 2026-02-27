package com.hyperfactions.debug;

import com.hyperfactions.data.ChunkKey;
import com.hyperfactions.manager.ClaimManager.ClaimResult;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Debug trace record for claim-related operations.
 *
 * @param chunk         the chunk being claimed/unclaimed
 * @param result        the operation result
 * @param actor         the player performing the action
 * @param factionId     the faction ID involved
 * @param operation     the operation type (e.g., "claim", "unclaim", "overclaim")
 * @param failureReason additional context for failures (nullable)
 * @param timestamp     when this occurred (epoch millis)
 */
public record ClaimTrace(
  @NotNull ChunkKey chunk,
  @NotNull ClaimResult result,
  @NotNull UUID actor,
  @Nullable UUID factionId,
  @NotNull String operation,
  @Nullable String failureReason,
  long timestamp
) {
  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
      .withZone(ZoneId.systemDefault());

  /**
   * Creates a claim trace for a successful claim operation.
   *
   * @param chunk     the claimed chunk
   * @param actor     the player who claimed
   * @param factionId the faction ID
   * @return a new ClaimTrace
   */
  public static ClaimTrace claimSuccess(@NotNull ChunkKey chunk, @NotNull UUID actor, @NotNull UUID factionId) {
    return new ClaimTrace(chunk, ClaimResult.SUCCESS, actor, factionId, "claim", null, System.currentTimeMillis());
  }

  /**
   * Creates a claim trace for a failed claim operation.
   *
   * @param chunk     the target chunk
   * @param result    the failure result
   * @param actor     the player who attempted
   * @param factionId the faction ID (nullable)
   * @return a new ClaimTrace
   */
  public static ClaimTrace claimFailed(@NotNull ChunkKey chunk, @NotNull ClaimResult result,
                     @NotNull UUID actor, @Nullable UUID factionId) {
    return new ClaimTrace(chunk, result, actor, factionId, "claim", result.name(), System.currentTimeMillis());
  }

  /**
   * Creates a claim trace for an unclaim operation.
   *
   * @param chunk     the unclaimed chunk
   * @param result    the operation result
   * @param actor     the player who unclaimed
   * @param factionId the faction ID
   * @return a new ClaimTrace
   */
  public static ClaimTrace unclaim(@NotNull ChunkKey chunk, @NotNull ClaimResult result,
                   @NotNull UUID actor, @Nullable UUID factionId) {
    String reason = result == ClaimResult.SUCCESS ? null : result.name();
    return new ClaimTrace(chunk, result, actor, factionId, "unclaim", reason, System.currentTimeMillis());
  }

  /**
   * Creates a claim trace for an overclaim operation.
   *
   * @param chunk     the overclaimed chunk
   * @param result    the operation result
   * @param actor     the player who overclaimed
   * @param factionId the attacking faction ID
   * @return a new ClaimTrace
   */
  public static ClaimTrace overclaim(@NotNull ChunkKey chunk, @NotNull ClaimResult result,
                    @NotNull UUID actor, @Nullable UUID factionId) {
    String reason = result == ClaimResult.SUCCESS ? null : result.name();
    return new ClaimTrace(chunk, result, actor, factionId, "overclaim", reason, System.currentTimeMillis());
  }

  /**
   * Checks if the operation was successful.
   *
   * @return true if successful
   */
  public boolean isSuccess() {
    return result == ClaimResult.SUCCESS;
  }

  /**
   * Returns a compact string representation.
   *
   * @return compact string
   */
  @Override
  public String toString() {
    String resultStr = isSuccess() ? "SUCCESS" : result.name();
    return String.format("[%s] %s at %s,%s (%s): %s",
        operation, actor.toString().substring(0, 8),
        chunk.chunkX(), chunk.chunkZ(), chunk.world(), resultStr);
  }

  /**
   * Returns a verbose string with all details.
   *
   * @return verbose string
   */
  public String toVerboseString() {
    String time = TIME_FORMAT.format(Instant.ofEpochMilli(timestamp));
    String factionStr = factionId != null ? factionId.toString().substring(0, 8) : "none";
    String reasonStr = failureReason != null ? ", reason=" + failureReason : "";
    return String.format("[%s] ClaimTrace { operation=%s, chunk=%s,%s (%s), actor=%s, faction=%s, result=%s%s }",
        time, operation, chunk.chunkX(), chunk.chunkZ(), chunk.world(),
        actor.toString().substring(0, 8), factionStr, result, reasonStr);
  }
}
