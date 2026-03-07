package com.hyperfactions.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.PlayerPower;
import com.hyperfactions.storage.PlayerStorage;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/**
 * Manages player power for faction mechanics.
 */
public class PowerManager {

  private final PlayerStorage storage;

  private final FactionManager factionManager;

  private final Path dataDir;

  // Cache: player UUID -> PlayerPower
  private final Map<UUID, PlayerPower> powerCache = new ConcurrentHashMap<>();

  // Track online players for regen
  private final Set<UUID> onlinePlayers = ConcurrentHashMap.newKeySet();

  // Hardcore mode: faction-level power pool (factionId -> power)
  private final Map<UUID, Double> hardcoreFactionPower = new ConcurrentHashMap<>();

  /** Creates a new PowerManager. */
  public PowerManager(@NotNull PlayerStorage storage, @NotNull FactionManager factionManager, @NotNull Path dataDir) {
    this.storage = storage;
    this.factionManager = factionManager;
    this.dataDir = dataDir;
  }

  /**
   * Loads all player power data from storage.
   *
   * <p>SAFETY: This method will NOT clear existing data if loading fails or returns
   * suspiciously empty results when data was expected.
   *
   * @return a future that completes when loading is done
   */
  public CompletableFuture<Void> loadAll() {
    final int previousCount = powerCache.size();

    return storage.loadAllPlayerPower().thenAccept(loaded -> {
      // SAFETY CHECK: If we had data before but loading returned nothing,
      // this is likely a load failure - DO NOT clear existing data
      if (previousCount > 0 && loaded.isEmpty()) {
        String msg = String.format("CRITICAL: Load returned 0 player power records but %d were previously loaded! Keeping existing in-memory data.", previousCount);
        ErrorHandler.report(msg, (Exception) null);
        return;
      }

      // Merge loaded data into cache instead of clear+putAll to avoid
      // wiping in-flight defaults created by playerOnline() during loading.
      // Players who joined before loadAll() completed already have valid
      // defaults in the cache — only overwrite with loaded data that has
      // meaningful values (power > 0 or has death history).
      int merged = 0;
      for (PlayerPower loaded1 : loaded) {
        PlayerPower existing = powerCache.get(loaded1.uuid());
        if (existing != null && loaded1.power() == 0 && loaded1.lastDeath() == 0) {
          // Loaded record looks like uninitialized data — keep the in-memory default
          Logger.debugPower("Keeping in-memory default for %s (loaded power=0, no death history)", loaded1.uuid());
          continue;
        }
        powerCache.put(loaded1.uuid(), loaded1);
        merged++;
      }

      Logger.info("[Startup] Loaded %d player power records (%d merged, %d kept in-memory defaults)",
        loaded.size(), merged, loaded.size() - merged);
    }).exceptionally(ex -> {
      ErrorHandler.report("CRITICAL: Exception during player power loading - keeping existing data", ex);
      return null;
    });
  }

  /**
   * Saves all player power data to storage.
   *
   * @return a future that completes when saving is done
   */
  public CompletableFuture<Void> saveAll() {
    List<CompletableFuture<Void>> futures = powerCache.values().stream()
      .map(storage::savePlayerPower)
      .collect(Collectors.toList());

    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
  }

  // === Player Operations ===

  /**
   * Gets or creates power data for a player.
   *
   * @param playerUuid the player's UUID
   * @return the player power data
   */
  @NotNull
  public PlayerPower getPlayerPower(@NotNull UUID playerUuid) {
    return powerCache.computeIfAbsent(playerUuid, uuid -> {
      ConfigManager config = ConfigManager.get();
      return PlayerPower.create(uuid, config.getStartingPower(), config.getMaxPlayerPower());
    });
  }

  /**
   * Loads player power, creating default if not exists.
   *
   * @param playerUuid the player's UUID
   * @return a future containing the player power
   */
  public CompletableFuture<PlayerPower> loadPlayer(@NotNull UUID playerUuid) {
    // Check cache first
    PlayerPower cached = powerCache.get(playerUuid);
    if (cached != null) {
      return CompletableFuture.completedFuture(cached);
    }

    return storage.loadPlayerPower(playerUuid).thenApply(opt -> {
      PlayerPower power = opt.orElseGet(() -> {
        ConfigManager config = ConfigManager.get();
        return PlayerPower.create(playerUuid, config.getStartingPower(), config.getMaxPlayerPower());
      });
      powerCache.put(playerUuid, power);
      return power;
    });
  }

  /**
   * Marks a player as online for power regen.
   *
   * @param playerUuid the player's UUID
   */
  public void playerOnline(@NotNull UUID playerUuid) {
    onlinePlayers.add(playerUuid);
    loadPlayer(playerUuid).thenAccept(power -> {
      // Persist immediately so the default survives any future cache operations
      storage.savePlayerPower(power);
      Logger.debugPower("Player online: uuid=%s, power=%.2f, max=%.2f",
        playerUuid, power.power(), power.maxPower());
    });
  }

  /**
   * Marks a player as offline.
   *
   * @param playerUuid the player's UUID
   */
  public void playerOffline(@NotNull UUID playerUuid) {
    onlinePlayers.remove(playerUuid);

    // Save their power data
    PlayerPower power = powerCache.get(playerUuid);
    if (power != null) {
      storage.savePlayerPower(power);
    }
  }

  // === Admin Power Operations ===

  /**
   * Sets a player's power to an exact value (clamped 0..effectiveMax).
   *
   * @param playerUuid the player's UUID
   * @param newPower   the desired power level
   * @return the actual new power level (after clamping)
   */
  public double setPlayerPower(@NotNull UUID playerUuid, double newPower) {
    PlayerPower power = getPlayerPower(playerUuid);
    PlayerPower updated = power.withPower(newPower);
    powerCache.put(playerUuid, updated);
    storage.savePlayerPower(updated);
    Logger.debugPower("Admin set power: player=%s, before=%.2f, after=%.2f", playerUuid, power.power(), updated.power());
    return updated.power();
  }

  /**
   * Adjusts a player's power by a delta (clamped 0..effectiveMax).
   *
   * @param playerUuid the player's UUID
   * @param delta      the amount to add (negative to subtract)
   * @return the actual new power level (after clamping)
   */
  public double adjustPlayerPower(@NotNull UUID playerUuid, double delta) {
    PlayerPower power = getPlayerPower(playerUuid);
    PlayerPower updated = power.withPower(power.power() + delta);
    powerCache.put(playerUuid, updated);
    storage.savePlayerPower(updated);
    Logger.debugPower("Admin adjust power: player=%s, before=%.2f, delta=%.2f, after=%.2f", playerUuid, power.power(), delta, updated.power());
    return updated.power();
  }

  /**
   * Resets a player's power to the configured starting power.
   *
   * @param playerUuid the player's UUID
   * @return the new power level
   */
  public double resetPlayerPower(@NotNull UUID playerUuid) {
    double startingPower = ConfigManager.get().getStartingPower();
    return setPlayerPower(playerUuid, startingPower);
  }

  /**
   * Sets a per-player max power override.
   *
   * @param playerUuid  the player's UUID
   * @param maxOverride the max power override, or null to clear
   * @return the actual new power level (may be clamped if max was lowered)
   */
  public double setPlayerMaxPower(@NotNull UUID playerUuid, Double maxOverride) {
    PlayerPower power = getPlayerPower(playerUuid);
    PlayerPower updated = power.withMaxPowerOverride(maxOverride);
    powerCache.put(playerUuid, updated);
    storage.savePlayerPower(updated);
    Logger.debugPower("Admin set max power: player=%s, override=%s, power=%.2f",
      playerUuid, maxOverride != null ? String.format("%.2f", maxOverride) : "cleared", updated.power());
    return updated.power();
  }

  /**
   * Clears a player's max power override (reverts to global config).
   *
   * @param playerUuid the player's UUID
   * @return the actual new power level
   */
  public double resetPlayerMaxPower(@NotNull UUID playerUuid) {
    return setPlayerMaxPower(playerUuid, null);
  }

  /**
   * Sets the power loss disabled flag for a player.
   *
   * @param playerUuid the player's UUID
   * @param disabled   true to disable power loss
   */
  public void setPlayerPowerLossDisabled(@NotNull UUID playerUuid, boolean disabled) {
    PlayerPower power = getPlayerPower(playerUuid);
    PlayerPower updated = power.withPowerLossDisabled(disabled);
    powerCache.put(playerUuid, updated);
    storage.savePlayerPower(updated);
    Logger.debugPower("Admin set power loss disabled: player=%s, disabled=%s", playerUuid, disabled);
  }

  /**
   * Sets the claim decay exempt flag for a player.
   *
   * @param playerUuid the player's UUID
   * @param exempt     true to exempt from claim decay
   */
  public void setPlayerClaimDecayExempt(@NotNull UUID playerUuid, boolean exempt) {
    PlayerPower power = getPlayerPower(playerUuid);
    PlayerPower updated = power.withClaimDecayExempt(exempt);
    powerCache.put(playerUuid, updated);
    storage.savePlayerPower(updated);
    Logger.debugPower("Admin set claim decay exempt: player=%s, exempt=%s", playerUuid, exempt);
  }

  // === Gameplay Power Operations ===

  /**
   * Applies death penalty to a player.
   *
   * @param playerUuid the player's UUID
   * @return the new power level
   */
  public double applyDeathPenalty(@NotNull UUID playerUuid) {
    PlayerPower power = getPlayerPower(playerUuid);

    // Absolute bypass: power loss disabled
    if (power.powerLossDisabled()) {
      Logger.debugPower("Death penalty bypassed (powerLossDisabled): player=%s", playerUuid);
      return power.power();
    }

    ConfigManager config = ConfigManager.get();
    double penalty = config.getDeathPenalty();

    if (config.isHardcoreMode()) {
      // Hardcore: subtract from faction pool (no floor)
      Faction faction = factionManager.getPlayerFaction(playerUuid);
      if (faction != null) {
        double before = getHardcorePower(faction.id());
        double after = before - penalty;
        hardcoreFactionPower.put(faction.id(), after);
        Logger.debugPower("Hardcore death penalty: faction=%s, before=%.2f, after=%.2f, penalty=%.2f",
          faction.name(), before, after, penalty);
        saveHardcorePowerAsync();
      }
      return power.power(); // Player power unchanged in hardcore
    }

    PlayerPower updated = power.withDeathPenalty(penalty);
    powerCache.put(playerUuid, updated);
    storage.savePlayerPower(updated);

    Logger.debugPower("Death penalty: player=%s, before=%.2f, after=%.2f, penalty=%.2f, max=%.2f",
      playerUuid, power.power(), updated.power(), penalty, power.maxPower());
    return updated.power();
  }

  /**
   * Applies combat logout penalty to a player.
   * Uses the same logic as death penalty (updates lastDeath, clamps at 0).
   *
   * @param playerUuid the player's UUID
   * @param penalty the amount of power to remove
   * @return the new power level
   */
  public double applyCombatLogoutPenalty(@NotNull UUID playerUuid, double penalty) {
    PlayerPower power = getPlayerPower(playerUuid);

    // Absolute bypass: power loss disabled
    if (power.powerLossDisabled()) {
      Logger.debugPower("Combat logout penalty bypassed (powerLossDisabled): player=%s", playerUuid);
      return power.power();
    }

    // Reuse withDeathPenalty - combat logout is treated as a "virtual death"
    PlayerPower updated = power.withDeathPenalty(penalty);
    powerCache.put(playerUuid, updated);
    storage.savePlayerPower(updated);

    Logger.debugPower("Combat logout penalty: player=%s, before=%.2f, after=%.2f, penalty=%.2f",
      playerUuid, power.power(), updated.power(), penalty);
    return updated.power();
  }

  /**
   * Rewards power for a PvP kill (clamped at max).
   *
   * @param playerUuid the killer's UUID
   * @param reward     the amount of power to add
   * @return the new power level
   */
  public double applyKillReward(@NotNull UUID playerUuid, double reward) {
    if (ConfigManager.get().isHardcoreMode()) {
      // Hardcore: add to faction pool
      Faction faction = factionManager.getPlayerFaction(playerUuid);
      if (faction != null) {
        double before = getHardcorePower(faction.id());
        double maxPower = getFactionMaxPower(faction.id());
        double after = Math.min(before + reward, maxPower);
        hardcoreFactionPower.put(faction.id(), after);
        Logger.debugPower("Hardcore kill reward: faction=%s, before=%.2f, after=%.2f, reward=%.2f",
          faction.name(), before, after, reward);
        saveHardcorePowerAsync();
      }
      return getPlayerPower(playerUuid).power();
    }

    PlayerPower power = getPlayerPower(playerUuid);
    PlayerPower updated = power.withRegen(reward);
    powerCache.put(playerUuid, updated);
    storage.savePlayerPower(updated);

    Logger.debugPower("Kill reward: player=%s, before=%.2f, after=%.2f, reward=%.2f",
      playerUuid, power.power(), updated.power(), reward);
    return updated.power();
  }

  /**
   * Penalizes power for killing a neutral player (clamped at 0).
   *
   * @param playerUuid the killer's UUID
   * @param penalty    the amount of power to remove
   * @return the new power level
   */
  public double applyNeutralKillPenalty(@NotNull UUID playerUuid, double penalty) {
    PlayerPower power = getPlayerPower(playerUuid);

    // Absolute bypass: power loss disabled
    if (power.powerLossDisabled()) {
      Logger.debugPower("Neutral kill penalty bypassed (powerLossDisabled): player=%s", playerUuid);
      return power.power();
    }

    PlayerPower updated = power.withDeathPenalty(penalty);
    powerCache.put(playerUuid, updated);
    storage.savePlayerPower(updated);

    Logger.debugPower("Neutral kill penalty: player=%s, before=%.2f, after=%.2f, penalty=%.2f",
      playerUuid, power.power(), updated.power(), penalty);
    return updated.power();
  }

  /**
   * Regenerates power for a player.
   *
   * @param playerUuid the player's UUID
   * @param amount     the amount to regenerate
   */
  public void regeneratePower(@NotNull UUID playerUuid, double amount) {
    PlayerPower power = powerCache.get(playerUuid);
    if (power == null || power.isAtMax()) {
      return;
    }

    PlayerPower updated = power.withRegen(amount);
    powerCache.put(playerUuid, updated);

    Logger.debugPower("Regen: player=%s, before=%.2f, after=%.2f, amount=%.2f, max=%.2f",
      playerUuid, power.power(), updated.power(), amount, power.maxPower());
    // Don't save immediately - batch save periodically
  }

  /**
   * Called periodically to regenerate power for online players.
   */
  public void tickPowerRegen() {
    ConfigManager config = ConfigManager.get();
    double regenAmount = config.getRegenPerMinute();

    if (regenAmount <= 0) {
      return;
    }

    if (config.isHardcoreMode()) {
      // Hardcore: regen the faction pool directly
      tickHardcoreRegen(regenAmount);
      return;
    }

    Set<UUID> playersToRegen = config.isRegenWhenOffline()
      ? new HashSet<>(powerCache.keySet())
      : new HashSet<>(onlinePlayers);

    for (UUID playerUuid : playersToRegen) {
      regeneratePower(playerUuid, regenAmount);
    }
  }

  // === Faction Power ===

  /**
   * Gets the total power of a faction.
   *
   * @param factionId the faction ID
   * @return the total power
   */
  public double getFactionPower(@NotNull UUID factionId) {
    if (ConfigManager.get().isHardcoreMode()) {
      return getHardcorePower(factionId);
    }

    Faction faction = factionManager.getFaction(factionId);
    if (faction == null) {
      return 0;
    }

    double total = 0;
    for (UUID memberUuid : faction.members().keySet()) {
      total += getPlayerPower(memberUuid).power();
    }
    return total;
  }

  /**
   * Gets the maximum power of a faction.
   *
   * @param factionId the faction ID
   * @return the maximum power
   */
  public double getFactionMaxPower(@NotNull UUID factionId) {
    Faction faction = factionManager.getFaction(factionId);
    if (faction == null) {
      return 0;
    }

    double total = 0;
    for (UUID memberUuid : faction.members().keySet()) {
      total += getPlayerPower(memberUuid).getEffectiveMaxPower();
    }
    return total;
  }

  /**
   * Gets the claim capacity for a faction based on power.
   *
   * @param factionId the faction ID
   * @return the max claims allowed
   */
  public int getFactionClaimCapacity(@NotNull UUID factionId) {
    double power = getFactionPower(factionId);
    return ConfigManager.get().calculateMaxClaims(power);
  }

  /**
   * Checks if a faction is raidable (claims > power-based limit).
   *
   * @param factionId the faction ID
   * @return true if raidable
   */
  public boolean isFactionRaidable(@NotNull UUID factionId) {
    Faction faction = factionManager.getFaction(factionId);
    if (faction == null) {
      return false;
    }

    int claimCapacity = getFactionClaimCapacity(factionId);
    return faction.getClaimCount() > claimCapacity;
  }

  /**
   * Gets power statistics for display.
   *
   * @param factionId the faction ID
   * @return power stats
   */
  @NotNull
  public FactionPowerStats getFactionPowerStats(@NotNull UUID factionId) {
    double current = getFactionPower(factionId);
    double max = getFactionMaxPower(factionId);
    int claims = 0;
    int claimCapacity = getFactionClaimCapacity(factionId);

    Faction faction = factionManager.getFaction(factionId);
    if (faction != null) {
      claims = faction.getClaimCount();
    }

    return new FactionPowerStats(current, max, claims, claimCapacity);
  }

  // === Hardcore Power Mode ===

  /**
   * Gets the hardcore faction power, initializing from member sum if needed.
   */
  private double getHardcorePower(@NotNull UUID factionId) {
    return hardcoreFactionPower.computeIfAbsent(factionId, id -> {
      // Initialize: sum of all member power
      Faction faction = factionManager.getFaction(id);
      if (faction == null) {
        return 0.0;
      }
      double sum = 0;
      for (UUID memberUuid : faction.members().keySet()) {
        sum += getPlayerPower(memberUuid).power();
      }
      Logger.debugPower("Initialized hardcore power for faction %s: %.2f", faction.name(), sum);
      return sum;
    });
  }

  /**
   * Regenerates hardcore faction power pools.
   * Only regens factions with at least one online member (unless regenWhenOffline).
   */
  private void tickHardcoreRegen(double regenAmount) {
    boolean regenOffline = ConfigManager.get().isRegenWhenOffline();

    for (Faction faction : factionManager.getAllFactions()) {
      UUID fid = faction.id();
      double maxPower = getFactionMaxPower(fid);
      double current = getHardcorePower(fid);
      if (current >= maxPower) {
        continue;
      }

      if (!regenOffline) {
        // Only regen if at least one member is online
        boolean anyOnline = faction.members().keySet().stream().anyMatch(onlinePlayers::contains);
        if (!anyOnline) {
          continue;
        }
      }

      double after = Math.min(current + regenAmount, maxPower);
      hardcoreFactionPower.put(fid, after);
    }
  }

  /**
   * Loads hardcore faction power data from disk.
   */
  public CompletableFuture<Void> loadHardcorePowerData() {
    return CompletableFuture.runAsync(() -> {
      Path file = dataDir.resolve("hardcore_power.json");
      if (!Files.exists(file)) {
        return;
      }

      try {
        String json = Files.readString(file);
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        for (var entry : obj.entrySet()) {
          try {
            UUID factionId = UUID.fromString(entry.getKey());
            double power = entry.getValue().getAsDouble();
            hardcoreFactionPower.put(factionId, power);
          } catch (Exception e) {
            ErrorHandler.report(String.format("Invalid hardcore power entry: %s", entry.getKey()), e);
          }
        }
        Logger.info("[Startup] Loaded hardcore power for %d factions", hardcoreFactionPower.size());
      } catch (Exception e) {
        ErrorHandler.report("Failed to load hardcore power data", e);
      }
    });
  }

  /**
   * Saves hardcore faction power data to disk.
   */
  public CompletableFuture<Void> saveHardcorePowerData() {
    return CompletableFuture.runAsync(() -> {
      Path file = dataDir.resolve("hardcore_power.json");
      try {
        JsonObject obj = new JsonObject();
        for (var entry : hardcoreFactionPower.entrySet()) {
          obj.addProperty(entry.getKey().toString(), entry.getValue());
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Files.writeString(file, gson.toJson(obj));
      } catch (IOException e) {
        ErrorHandler.report("Failed to save hardcore power data", e);
      }
    });
  }

  /**
   * Async save without blocking the caller.
   */
  private void saveHardcorePowerAsync() {
    saveHardcorePowerData();
  }

  /**
   * Removes hardcore power data for a disbanded faction.
   */
  public void removeHardcorePower(@NotNull UUID factionId) {
    hardcoreFactionPower.remove(factionId);
  }

  /**
   * Power statistics for a faction.
   */
  public record FactionPowerStats(
    double currentPower,
    double maxPower,
    int currentClaims,
    int maxClaims
  ) {
    /** Returns the power percent. */
    public int getPowerPercent() {
      if (maxPower <= 0) {
        return 0;
      }
      return (int) Math.round((currentPower / maxPower) * 100);
    }

    /** Checks if raidable. */
    public boolean isRaidable() {
      return currentClaims > maxClaims;
    }

    /** Returns the claim deficit. */
    public int getClaimDeficit() {
      return Math.max(0, currentClaims - maxClaims);
    }
  }
}
