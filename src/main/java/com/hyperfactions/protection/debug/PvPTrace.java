package com.hyperfactions.protection.debug;

import com.hyperfactions.data.ChunkKey;
import com.hyperfactions.data.RelationType;
import com.hyperfactions.protection.ProtectionChecker.PvPResult;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Debug trace record for PvP check operations.
 *
 * @param attacker  the attacker UUID
 * @param defender  the defender UUID
 * @param chunk     the chunk location
 * @param result    the PvP result
 * @param relation  the relation between players
 * @param timestamp when this occurred (epoch millis)
 */
public record PvPTrace(
  @NotNull UUID attacker,
  @NotNull UUID defender,
  @NotNull ChunkKey chunk,
  @NotNull PvPResult result,
  @Nullable RelationType relation,
  long timestamp
) {
  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
      .withZone(ZoneId.systemDefault());

  /**
   * Creates a PvP trace.
   *
   * @param attacker the attacker UUID
   * @param defender the defender UUID
   * @param chunk    the chunk location
   * @param result   the PvP result
   * @param relation the relation between players
   * @return a new PvPTrace
   */
  public static PvPTrace create(@NotNull UUID attacker, @NotNull UUID defender,
                 @NotNull ChunkKey chunk, @NotNull PvPResult result,
                 @Nullable RelationType relation) {
    return new PvPTrace(attacker, defender, chunk, result, relation, System.currentTimeMillis());
  }

  /**
   * Checks if PvP is allowed.
   *
   * @return true if allowed
   */
  public boolean isAllowed() {
    return result == PvPResult.ALLOWED || result == PvPResult.ALLOWED_WARZONE;
  }

  /**
   * Returns a verbose string with all details.
   *
   * @return verbose string
   */
  public String toVerboseString() {
    String time = TIME_FORMAT.format(Instant.ofEpochMilli(timestamp));
    String relationStr = relation != null ? relation.name() : "none";
    return String.format("[%s] PvPTrace { attacker=%s, defender=%s, chunk=%s,%s (%s), result=%s, relation=%s }",
        time, attacker.toString().substring(0, 8), defender.toString().substring(0, 8),
        chunk.chunkX(), chunk.chunkZ(), chunk.world(), result, relationStr);
  }
}
