package com.hyperfactions.manager;

import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.CombatTag;
import com.hyperfactions.protection.SpawnProtection;
import com.hyperfactions.util.Logger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Manages combat tagging for PvP logout prevention and spawn protection.
 */
public class CombatTagManager {

  /**
   * Categorizes the cause of a player's death for power loss configuration.
   * Recorded by {@link com.hyperfactions.protection.damage.DamageProtectionHandler}
   * when damage is allowed through, and read by
   * {@link com.hyperfactions.protection.ecs.PlayerDeathSystem} at death time.
   */
  public enum DeathCauseType {
    /** Player was killed by another player (direct or projectile). */
    PVP,
    /** Player was killed by a mob (direct or mob projectile). */
    MOB,
    /** Player died to fall damage, drowning, suffocation, or other non-entity damage. */
    ENVIRONMENTAL,
    /** Death cause could not be determined. */
    UNKNOWN
  }

  // Active combat tags: player UUID -> CombatTag
  private final Map<UUID, CombatTag> tags = new ConcurrentHashMap<>();

  // Last attacker tracking: defender UUID -> attacker UUID (for kill rewards)
  private final Map<UUID, UUID> lastAttacker = new ConcurrentHashMap<>();

  // Last damage type tracking: player UUID -> DeathCauseType (for configurable power loss)
  private final Map<UUID, DeathCauseType> lastDamageType = new ConcurrentHashMap<>();

  // Active spawn protections: player UUID -> SpawnProtection
  private final Map<UUID, SpawnProtection> spawnProtections = new ConcurrentHashMap<>();

  // Callbacks for tag events
  private Consumer<UUID> onTagExpired;

  private Consumer<UUID> onCombatLogout;

  /**
   * Sets the callback for when a tag expires naturally.
   *
   * @param callback the callback
   */
  public void setOnTagExpired(@Nullable Consumer<UUID> callback) {
    this.onTagExpired = callback;
  }

  /**
   * Sets the callback for when a player logs out while tagged.
   *
   * @param callback the callback
   */
  public void setOnCombatLogout(@Nullable Consumer<UUID> callback) {
    this.onCombatLogout = callback;
  }

  // === Queries ===

  /**
   * Checks if a player is combat tagged.
   *
   * @param playerUuid the player's UUID
   * @return true if tagged and not expired
   */
  public boolean isTagged(@NotNull UUID playerUuid) {
    CombatTag tag = tags.get(playerUuid);
    if (tag == null) {
      return false;
    }
    if (tag.isExpired()) {
      tags.remove(playerUuid);
      return false;
    }
    return true;
  }

  /**
   * Gets the combat tag for a player.
   *
   * @param playerUuid the player's UUID
   * @return the tag, or null if not tagged
   */
  @Nullable
  public CombatTag getTag(@NotNull UUID playerUuid) {
    CombatTag tag = tags.get(playerUuid);
    if (tag != null && tag.isExpired()) {
      tags.remove(playerUuid);
      return null;
    }
    return tag;
  }

  /**
   * Gets the remaining tag time in seconds.
   *
   * @param playerUuid the player's UUID
   * @return remaining seconds, 0 if not tagged
   */
  public int getRemainingSeconds(@NotNull UUID playerUuid) {
    CombatTag tag = getTag(playerUuid);
    return tag != null ? tag.getRemainingSeconds() : 0;
  }

  /**
   * Gets all currently tagged players.
   *
   * @return set of tagged player UUIDs
   */
  @NotNull
  public Set<UUID> getTaggedPlayers() {
    // Clean up expired tags and return active ones
    Set<UUID> active = new HashSet<>();
    Iterator<Map.Entry<UUID, CombatTag>> iter = tags.entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry<UUID, CombatTag> entry = iter.next();
      if (entry.getValue().isExpired()) {
        iter.remove();
      } else {
        active.add(entry.getKey());
      }
    }
    return active;
  }

  // === Operations ===

  /**
   * Tags a player with combat.
   *
   * @param playerUuid the player's UUID
   * @return the combat tag
   */
  @NotNull
  public CombatTag tagPlayer(@NotNull UUID playerUuid) {
    int duration = ConfigManager.get().getTagDurationSeconds();
    return tagPlayer(playerUuid, duration);
  }

  /**
   * Tags a player with combat for a specific duration.
   *
   * @param playerUuid the player's UUID
   * @param durationSeconds the tag duration
   * @return the combat tag
   */
  @NotNull
  public CombatTag tagPlayer(@NotNull UUID playerUuid, int durationSeconds) {
    CombatTag existing = tags.get(playerUuid);

    // Refresh if already tagged
    if (existing != null && !existing.isExpired()) {
      CombatTag refreshed = existing.refresh(durationSeconds);
      tags.put(playerUuid, refreshed);
      return refreshed;
    }

    // New tag
    CombatTag tag = CombatTag.create(playerUuid, durationSeconds);
    tags.put(playerUuid, tag);
    return tag;
  }

  /**
   * Tags both players in a combat interaction.
   *
   * @param attacker the attacker's UUID
   * @param defender the defender's UUID
   */
  public void tagCombat(@NotNull UUID attacker, @NotNull UUID defender) {
    int duration = ConfigManager.get().getTagDurationSeconds();
    tagPlayer(attacker);
    tagPlayer(defender);
    lastAttacker.put(defender, attacker);
    Logger.debugCombat("Combat tag: attacker=%s, defender=%s, duration=%ds",
      attacker, defender, duration);
  }

  /**
   * Gets and removes the last attacker for a player (consume-on-read).
   * Used to determine killer for power rewards on death.
   *
   * @param defenderUuid the defender's UUID
   * @return the attacker's UUID, or null if no attacker tracked
   */
  @Nullable
  public UUID getLastAttacker(@NotNull UUID defenderUuid) {
    return lastAttacker.remove(defenderUuid);
  }

  // === Damage Type Tracking ===

  /**
   * Records the type of damage a player received.
   * Called by {@link com.hyperfactions.protection.damage.DamageProtectionHandler}
   * when damage is allowed through protection checks.
   *
   * @param playerUuid the player's UUID
   * @param type       the damage cause type
   */
  public void recordDamageType(@NotNull UUID playerUuid, @NotNull DeathCauseType type) {
    lastDamageType.put(playerUuid, type);
  }

  /**
   * Gets the last recorded damage type for a player.
   *
   * @param playerUuid the player's UUID
   * @return the last damage type, or {@link DeathCauseType#UNKNOWN} if not recorded
   */
  @NotNull
  public DeathCauseType getLastDamageType(@NotNull UUID playerUuid) {
    DeathCauseType type = lastDamageType.get(playerUuid);
    return type != null ? type : DeathCauseType.UNKNOWN;
  }

  /**
   * Clears the recorded damage type for a player.
   *
   * @param playerUuid the player's UUID
   */
  public void clearDamageType(@NotNull UUID playerUuid) {
    lastDamageType.remove(playerUuid);
  }

  /**
   * Clears a player's combat tag.
   *
   * @param playerUuid the player's UUID
   */
  public void clearTag(@NotNull UUID playerUuid) {
    tags.remove(playerUuid);
    lastAttacker.remove(playerUuid);
    lastDamageType.remove(playerUuid);
  }

  /**
   * Called when a player disconnects.
   * Handles combat logout penalty if tagged.
   *
   * @param playerUuid the player's UUID
   * @return true if the player was combat tagged
   */
  public boolean handleDisconnect(@NotNull UUID playerUuid) {
    CombatTag tag = tags.remove(playerUuid);
    lastAttacker.remove(playerUuid);
    lastDamageType.remove(playerUuid);
    if (tag != null && !tag.isExpired()) {
      Logger.debugCombat("Combat logout: player=%s, remainingSeconds=%d, penaltyEnabled=%b",
        playerUuid, tag.getRemainingSeconds(), ConfigManager.get().isTaggedLogoutPenalty());
      if (ConfigManager.get().isTaggedLogoutPenalty() && onCombatLogout != null) {
        onCombatLogout.accept(playerUuid);
      }
      return true;
    }
    return false;
  }

  /**
   * Called periodically to decay expired tags.
   * Also triggers callbacks for naturally expired tags.
   */
  public void tickDecay() {
    Iterator<Map.Entry<UUID, CombatTag>> iter = tags.entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry<UUID, CombatTag> entry = iter.next();
      if (entry.getValue().isExpired()) {
        UUID playerUuid = entry.getKey();
        iter.remove();
        if (onTagExpired != null) {
          onTagExpired.accept(playerUuid);
        }
      }
    }
  }

  /**
   * Gets the count of active combat tags.
   *
   * @return number of tagged players
   */
  public int getTagCount() {
    return getTaggedPlayers().size();
  }

  // === Spawn Protection ===

  /**
   * Checks if a player has active spawn protection.
   *
   * @param playerUuid the player's UUID
   * @return true if protected and not expired
   */
  public boolean hasSpawnProtection(@NotNull UUID playerUuid) {
    SpawnProtection protection = spawnProtections.get(playerUuid);
    if (protection == null) {
      return false;
    }
    if (protection.isExpired()) {
      spawnProtections.remove(playerUuid);
      return false;
    }
    return true;
  }

  /**
   * Gets the spawn protection for a player.
   *
   * @param playerUuid the player's UUID
   * @return the protection, or null if not protected
   */
  @Nullable
  public SpawnProtection getSpawnProtection(@NotNull UUID playerUuid) {
    SpawnProtection protection = spawnProtections.get(playerUuid);
    if (protection != null && protection.isExpired()) {
      spawnProtections.remove(playerUuid);
      return null;
    }
    return protection;
  }

  /**
   * Applies spawn protection to a player.
   *
   * @param playerUuid      the player's UUID
   * @param durationSeconds the protection duration
   * @param world           the spawn world
   * @param chunkX          the spawn chunk X
   * @param chunkZ          the spawn chunk Z
   * @return the spawn protection
   */
  @NotNull
  public SpawnProtection applySpawnProtection(@NotNull UUID playerUuid, int durationSeconds,
                        @NotNull String world, int chunkX, int chunkZ) {
    SpawnProtection protection = SpawnProtection.create(playerUuid, durationSeconds, world, chunkX, chunkZ);
    spawnProtections.put(playerUuid, protection);
    return protection;
  }

  /**
   * Clears spawn protection for a player.
   *
   * @param playerUuid the player's UUID
   */
  public void clearSpawnProtection(@NotNull UUID playerUuid) {
    spawnProtections.remove(playerUuid);
  }

  /**
   * Checks if spawn protection should be broken due to chunk movement.
   * If the player has left their spawn chunk, protection is cleared.
   *
   * @param playerUuid    the player's UUID
   * @param currentWorld  the player's current world
   * @param currentChunkX the player's current chunk X
   * @param currentChunkZ the player's current chunk Z
   * @return true if protection was broken due to movement
   */
  public boolean checkSpawnProtectionMove(@NotNull UUID playerUuid, @NotNull String currentWorld,
                      int currentChunkX, int currentChunkZ) {
    SpawnProtection protection = getSpawnProtection(playerUuid);
    if (protection == null) {
      return false;
    }

    if (ConfigManager.get().isSpawnProtectionBreakOnMove()
      && protection.hasLeftSpawnChunk(currentWorld, currentChunkX, currentChunkZ)) {
      clearSpawnProtection(playerUuid);
      return true;
    }
    return false;
  }

  /**
   * Gets the remaining spawn protection time in seconds.
   *
   * @param playerUuid the player's UUID
   * @return remaining seconds, 0 if not protected
   */
  public int getSpawnProtectionRemainingSeconds(@NotNull UUID playerUuid) {
    SpawnProtection protection = getSpawnProtection(playerUuid);
    return protection != null ? protection.getRemainingSeconds() : 0;
  }
}
