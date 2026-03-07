package com.hyperfactions.protection.debug;

import com.hyperfactions.data.ChunkKey;
import com.hyperfactions.data.RelationType;
import com.hyperfactions.protection.ProtectionChecker.InteractionType;
import com.hyperfactions.protection.ProtectionChecker.ProtectionResult;
import com.hyperfactions.protection.ProtectionChecker.PvPResult;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Debug trace record for protection check operations.
 *
 * @param playerUuid   the player being checked
 * @param chunk        the chunk location
 * @param type         the interaction type
 * @param result       the protection result
 * @param claimOwner   the faction that owns the claim (nullable)
 * @param relation     the relation to claim owner (nullable)
 * @param timestamp    when this occurred (epoch millis)
 */
public record ProtectionTrace(
  @NotNull UUID playerUuid,
  @NotNull ChunkKey chunk,
  @NotNull InteractionType type,
  @NotNull ProtectionResult result,
  @Nullable UUID claimOwner,
  @Nullable RelationType relation,
  long timestamp
) {
  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
      .withZone(ZoneId.systemDefault());

  /**
   * Creates a protection trace for an allowed interaction.
   *
   * @param playerUuid the player UUID
   * @param chunk      the chunk
   * @param type       the interaction type
   * @param result     the result (should be an allowed result)
   * @return a new ProtectionTrace
   */
  public static ProtectionTrace allowed(@NotNull UUID playerUuid, @NotNull ChunkKey chunk,
                     @NotNull InteractionType type, @NotNull ProtectionResult result) {
    return new ProtectionTrace(playerUuid, chunk, type, result, null, null, System.currentTimeMillis());
  }

  /**
   * Creates a protection trace for a denied interaction.
   *
   * @param playerUuid the player UUID
   * @param chunk      the chunk
   * @param type       the interaction type
   * @param result     the result (should be a denied result)
   * @param claimOwner the faction that owns the claim
   * @param relation   the relation between player's faction and claim owner
   * @return a new ProtectionTrace
   */
  public static ProtectionTrace denied(@NotNull UUID playerUuid, @NotNull ChunkKey chunk,
                     @NotNull InteractionType type, @NotNull ProtectionResult result,
                     @Nullable UUID claimOwner, @Nullable RelationType relation) {
    return new ProtectionTrace(playerUuid, chunk, type, result, claimOwner, relation, System.currentTimeMillis());
  }

  /**
   * Creates a protection trace with full context.
   *
   * @param playerUuid the player UUID
   * @param chunk      the chunk
   * @param type       the interaction type
   * @param result     the result
   * @param claimOwner the faction that owns the claim
   * @param relation   the relation between player's faction and claim owner
   * @return a new ProtectionTrace
   */
  public static ProtectionTrace create(@NotNull UUID playerUuid, @NotNull ChunkKey chunk,
                     @NotNull InteractionType type, @NotNull ProtectionResult result,
                     @Nullable UUID claimOwner, @Nullable RelationType relation) {
    return new ProtectionTrace(playerUuid, chunk, type, result, claimOwner, relation, System.currentTimeMillis());
  }

  /**
   * Checks if the result is allowed.
   *
   * @return true if allowed
   */
  public boolean isAllowed() {
    return switch (result) {
      case ALLOWED, ALLOWED_BYPASS, ALLOWED_WILDERNESS, ALLOWED_SAFEZONE,
        ALLOWED_OWN_CLAIM, ALLOWED_ALLY_CLAIM, ALLOWED_WARZONE -> true;
      default -> false;
    };
  }

  /**
   * Returns a compact string representation.
   *
   * @return compact string
   */
  @Override
  public String toString() {
    String resultStr = isAllowed() ? "ALLOWED" : "DENIED";
    return String.format("[%s] %s at %s,%s (%s): %s (%s)",
        type, playerUuid.toString().substring(0, 8),
        chunk.chunkX(), chunk.chunkZ(), chunk.world(),
        resultStr, result);
  }

  /**
   * Returns a verbose string with all details.
   *
   * @return verbose string
   */
  public String toVerboseString() {
    String time = TIME_FORMAT.format(Instant.ofEpochMilli(timestamp));
    String ownerStr = claimOwner != null ? claimOwner.toString().substring(0, 8) : "none";
    String relationStr = relation != null ? relation.name() : "none";
    return String.format("[%s] ProtectionTrace { player=%s, type=%s, chunk=%s,%s (%s), result=%s, owner=%s, relation=%s }",
        time, playerUuid.toString().substring(0, 8), type,
        chunk.chunkX(), chunk.chunkZ(), chunk.world(),
        result, ownerStr, relationStr);
  }
}
