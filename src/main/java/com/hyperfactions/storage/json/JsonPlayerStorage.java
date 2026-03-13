package com.hyperfactions.storage.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hyperfactions.data.FactionRole;
import com.hyperfactions.data.MembershipRecord;
import com.hyperfactions.data.PlayerData;
import com.hyperfactions.data.PlayerPower;
import com.hyperfactions.storage.PlayerStorage;
import com.hyperfactions.storage.StorageHealth;
import com.hyperfactions.storage.StorageUtils;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;
import com.hyperfactions.util.UuidUtil;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

/**
 * JSON file-based implementation of PlayerStorage.
 * Stores each player's data in: data/players/{uuid}.json
 *
 * <p>
 * Backwards-compatible: old files with only power fields load without errors.
 * New fields (username, kills, deaths, membershipHistory) default to null/0/empty.
 */
public class JsonPlayerStorage implements PlayerStorage {

  private final Path dataDir;

  private final Path playersDir;

  private final Gson gson;

  private final ConcurrentHashMap<UUID, ReentrantLock> playerLocks = new ConcurrentHashMap<>();

  /** Creates a new JsonPlayerStorage. */
  public JsonPlayerStorage(@NotNull Path dataDir) {
    this.dataDir = dataDir;
    this.playersDir = dataDir.resolve("players");
    this.gson = new GsonBuilder()
      .setPrettyPrinting()
      .disableHtmlEscaping()
      .create();
  }

  /** Initializes this component. */
  @Override
  public CompletableFuture<Void> init() {
    return CompletableFuture.runAsync(() -> {
      try {
        Files.createDirectories(playersDir);
        // Clean up orphaned temp and backup files from previous crashes
        StorageUtils.cleanupOrphanedFiles(playersDir);
        Logger.info("[Storage] Player storage initialized at %s", playersDir);
      } catch (IOException e) {
        ErrorHandler.report("Failed to create players directory", e);
      }
    });
  }

  /** Cleans up resources. */
  @Override
  public CompletableFuture<Void> shutdown() {
    return CompletableFuture.completedFuture(null);
  }

  /** Loads player power. */
  @Override
  public CompletableFuture<Optional<PlayerPower>> loadPlayerPower(@NotNull UUID uuid) {
    return loadPlayerData(uuid).thenApply(opt -> opt.map(PlayerData::toPower));
  }

  /** Saves player power. */
  @Override
  public CompletableFuture<Void> savePlayerPower(@NotNull PlayerPower power) {
    // Load-modify-save to preserve history and other fields
    return CompletableFuture.runAsync(() -> {
      ReentrantLock lock = getPlayerLock(power.uuid());
      lock.lock();
      try {
        // Load existing data or create new
        PlayerData data = loadPlayerDataSync(power.uuid());
        if (data == null) {
          data = new PlayerData(power.uuid());
        }
        data.updatePower(power);
        savePlayerDataSync(data);
      } catch (Exception e) {
        Path file = playersDir.resolve(power.uuid() + ".json");
        StorageHealth.get().recordFailure(file.toString(), e.getMessage());
        ErrorHandler.report(String.format("Failed to save player power %s", power.uuid()), e);
      } finally {
        lock.unlock();
      }
    });
  }

  /** Deletes player power. */
  @Override
  public CompletableFuture<Void> deletePlayerPower(@NotNull UUID uuid) {
    return CompletableFuture.runAsync(() -> {
      Path file = playersDir.resolve(uuid + ".json");
      // Delete both the main file and its backup
      StorageUtils.deleteWithBackup(file);
    });
  }

  /** Loads all player power. */
  @Override
  public CompletableFuture<Collection<PlayerPower>> loadAllPlayerPower() {
    return CompletableFuture.supplyAsync(() -> {
      List<PlayerPower> powers = new ArrayList<>();
      List<String> failedFiles = new ArrayList<>();
      int totalFiles = 0;

      if (!Files.exists(playersDir)) {
        Logger.info("[Storage] Players directory does not exist yet, no player power to load");
        return powers;
      }

      try (DirectoryStream<Path> stream = Files.newDirectoryStream(playersDir, "*.json")) {
        for (Path file : stream) {
          totalFiles++;
          try {
            String json = Files.readString(file);
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            powers.add(deserializePlayerData(obj).toPower());
          } catch (Exception e) {
            failedFiles.add(file.getFileName().toString());
            ErrorHandler.report(String.format("Failed to load player file %s", file.getFileName()), e);
          }
        }
      } catch (IOException e) {
        ErrorHandler.report("CRITICAL: Failed to read players directory - data may be lost!", e);
        throw new RuntimeException("Failed to read players directory", e);
      }

      // Report loading results
      if (!failedFiles.isEmpty()) {
        ErrorHandler.report(String.format("WARNING: %d of %d player files failed to load: %s",
          failedFiles.size(), totalFiles, String.join(", ", failedFiles)), (Exception) null);
      }

      Logger.info("[Storage] Loaded %d/%d player power records successfully", powers.size(), totalFiles);
      return powers;
    });
  }

  /** Returns the all player uuids. */
  @Override
  public CompletableFuture<Set<UUID>> getAllPlayerUuids() {
    return CompletableFuture.supplyAsync(() -> {
      Set<UUID> uuids = new HashSet<>();
      if (!Files.exists(playersDir)) {
        return uuids;
      }
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(playersDir, "*.json")) {
        for (Path file : stream) {
          String filename = file.getFileName().toString();
          String uuidStr = filename.substring(0, filename.length() - 5); // Remove .json
          UUID uuid = UuidUtil.parseOrNull(uuidStr);
          if (uuid != null) {
            uuids.add(uuid);
          }

          // Skip non-UUID filenames
        }
      } catch (IOException e) {
        ErrorHandler.report("Failed to list player files", e);
      }
      return uuids;
    });
  }

  /** Loads player data. */
  @Override
  public CompletableFuture<Optional<PlayerData>> loadPlayerData(@NotNull UUID uuid) {
    return CompletableFuture.supplyAsync(() -> Optional.ofNullable(loadPlayerDataSync(uuid)));
  }

  /** Saves player data. */
  @Override
  public CompletableFuture<Void> savePlayerData(@NotNull PlayerData data) {
    return CompletableFuture.runAsync(() -> {
      ReentrantLock lock = getPlayerLock(data.getUuid());
      lock.lock();
      try {
        savePlayerDataSync(data);
      } finally {
        lock.unlock();
      }
    });
  }

  /** Updates player data. */
  @Override
  public CompletableFuture<Void> updatePlayerData(@NotNull UUID uuid, @NotNull Consumer<PlayerData> updater) {
    return CompletableFuture.runAsync(() -> {
      ReentrantLock lock = getPlayerLock(uuid);
      lock.lock();
      try {
        PlayerData data = loadPlayerDataSync(uuid);
        if (data == null) {
          data = new PlayerData(uuid);
        }
        updater.accept(data);
        savePlayerDataSync(data);
      } finally {
        lock.unlock();
      }
    });
  }

  // === Internal sync helpers ===

  private ReentrantLock getPlayerLock(UUID uuid) {
    return playerLocks.computeIfAbsent(uuid, k -> new ReentrantLock());
  }

  /**
   * Synchronously saves player data to disk. Caller must hold the player lock.
   */
  private void savePlayerDataSync(@NotNull PlayerData data) {
    Path file = playersDir.resolve(data.getUuid() + ".json");
    String filePath = file.toString();

    try {
      String content = gson.toJson(serializePlayerData(data));
      StorageUtils.WriteResult result = StorageUtils.writeAtomic(file, content);

      if (result instanceof StorageUtils.WriteResult.Success) {
        StorageHealth.get().recordSuccess(filePath);
      } else if (result instanceof StorageUtils.WriteResult.Failure failure) {
        StorageHealth.get().recordFailure(filePath, failure.error());
        ErrorHandler.report(String.format("Failed to save player data %s: %s", data.getUuid(), failure.error()), failure.cause());
      }
    } catch (Exception e) {
      StorageHealth.get().recordFailure(filePath, e.getMessage());
      ErrorHandler.report(String.format("Failed to save player data %s", data.getUuid()), e);
    }
  }

  /**
   * Synchronously loads player data from disk. Returns null if not found.
   */
  private PlayerData loadPlayerDataSync(@NotNull UUID uuid) {
    Path file = playersDir.resolve(uuid + ".json");
    if (!Files.exists(file)) {
      if (StorageUtils.hasBackup(file)) {
        Logger.warn("Player data file %s missing but backup exists, attempting recovery", uuid);
        if (!StorageUtils.recoverFromBackup(file)) {
          return null;
        }
        Logger.info("[Storage] Successfully recovered player data %s from backup", uuid);
      } else {
        return null;
      }
    }

    try {
      String json = Files.readString(file);
      JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
      return deserializePlayerData(obj);
    } catch (Exception e) {
      ErrorHandler.report(String.format("Failed to load player data %s, attempting backup recovery", uuid), e);
      if (StorageUtils.recoverFromBackup(file)) {
        try {
          String json = Files.readString(file);
          JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
          Logger.info("[Storage] Successfully loaded player data %s from recovered backup", uuid);
          return deserializePlayerData(obj);
        } catch (Exception e2) {
          ErrorHandler.report(String.format("Backup recovery failed for player data %s", uuid), e2);
        }
      }
      return null;
    }
  }

  // === Serialization ===

  private JsonObject serializePlayerData(PlayerData data) {
    JsonObject obj = new JsonObject();
    obj.addProperty("uuid", data.getUuid().toString());
    if (data.getUsername() != null) {
      obj.addProperty("username", data.getUsername());
    }
    obj.addProperty("power", data.getPower());
    obj.addProperty("maxPower", data.getMaxPower());
    obj.addProperty("lastDeath", data.getLastDeath());
    obj.addProperty("lastRegen", data.getLastRegen());
    obj.addProperty("kills", data.getKills());
    obj.addProperty("deaths", data.getDeaths());
    if (data.getFirstJoined() > 0) {
      obj.addProperty("firstJoined", data.getFirstJoined());
    }
    if (data.getLastOnline() > 0) {
      obj.addProperty("lastOnline", data.getLastOnline());
    }

    // Admin power overrides
    if (data.getMaxPowerOverride() != null) {
      obj.addProperty("maxPowerOverride", data.getMaxPowerOverride());
    }
    if (data.isPowerLossDisabled()) {
      obj.addProperty("powerLossDisabled", true);
    }
    if (data.isClaimDecayExempt()) {
      obj.addProperty("claimDecayExempt", true);
    }
    if (data.isAdminBypassEnabled()) {
      obj.addProperty("adminBypassEnabled", true);
    }

    // Player preferences (i18n + notifications)
    if (data.getLanguagePreference() != null) {
      obj.addProperty("languagePreference", data.getLanguagePreference());
    }
    if (!data.isTerritoryAlertsEnabled()) {
      obj.addProperty("territoryAlertsEnabled", false);
    }
    if (!data.isDeathAnnouncementsEnabled()) {
      obj.addProperty("deathAnnouncementsEnabled", false);
    }
    if (!data.isPowerNotificationsEnabled()) {
      obj.addProperty("powerNotificationsEnabled", false);
    }

    // Membership history
    if (!data.getMembershipHistory().isEmpty()) {
      JsonArray historyArr = new JsonArray();
      for (MembershipRecord rec : data.getMembershipHistory()) {
        JsonObject recObj = new JsonObject();
        recObj.addProperty("factionId", rec.factionId().toString());
        recObj.addProperty("factionName", rec.factionName());
        if (rec.factionTag() != null) {
          recObj.addProperty("factionTag", rec.factionTag());
        }
        recObj.addProperty("highestRole", rec.highestRole().name());
        recObj.addProperty("joinedAt", rec.joinedAt());
        recObj.addProperty("leftAt", rec.leftAt());
        recObj.addProperty("reason", rec.reason().name());
        historyArr.add(recObj);
      }
      obj.add("membershipHistory", historyArr);
    }

    return obj;
  }

  private PlayerData deserializePlayerData(JsonObject obj) {
    PlayerData data = new PlayerData();
    data.setUuid(UUID.fromString(obj.get("uuid").getAsString()));

    if (obj.has("username") && !obj.get("username").isJsonNull()) {
      data.setUsername(obj.get("username").getAsString());
    }

    data.setPower(obj.get("power").getAsDouble());
    data.setMaxPower(obj.get("maxPower").getAsDouble());
    data.setLastDeath(obj.get("lastDeath").getAsLong());
    data.setLastRegen(obj.get("lastRegen").getAsLong());

    if (obj.has("kills")) {
      data.setKills(obj.get("kills").getAsInt());
    }
    if (obj.has("deaths")) {
      data.setDeaths(obj.get("deaths").getAsInt());
    }
    if (obj.has("firstJoined")) {
      data.setFirstJoined(obj.get("firstJoined").getAsLong());
    }
    if (obj.has("lastOnline")) {
      data.setLastOnline(obj.get("lastOnline").getAsLong());
    }

    // Admin power overrides
    if (obj.has("maxPowerOverride") && !obj.get("maxPowerOverride").isJsonNull()) {
      data.setMaxPowerOverride(obj.get("maxPowerOverride").getAsDouble());
    }
    if (obj.has("powerLossDisabled")) {
      data.setPowerLossDisabled(obj.get("powerLossDisabled").getAsBoolean());
    }
    if (obj.has("claimDecayExempt")) {
      data.setClaimDecayExempt(obj.get("claimDecayExempt").getAsBoolean());
    }
    if (obj.has("adminBypassEnabled")) {
      data.setAdminBypassEnabled(obj.get("adminBypassEnabled").getAsBoolean());
    }

    // Player preferences (i18n + notifications)
    if (obj.has("languagePreference") && !obj.get("languagePreference").isJsonNull()) {
      data.setLanguagePreference(obj.get("languagePreference").getAsString());
    }
    if (obj.has("territoryAlertsEnabled")) {
      data.setTerritoryAlertsEnabled(obj.get("territoryAlertsEnabled").getAsBoolean());
    }
    if (obj.has("deathAnnouncementsEnabled")) {
      data.setDeathAnnouncementsEnabled(obj.get("deathAnnouncementsEnabled").getAsBoolean());
    }
    if (obj.has("powerNotificationsEnabled")) {
      data.setPowerNotificationsEnabled(obj.get("powerNotificationsEnabled").getAsBoolean());
    }

    // Membership history
    if (obj.has("membershipHistory") && obj.get("membershipHistory").isJsonArray()) {
      JsonArray historyArr = obj.getAsJsonArray("membershipHistory");
      List<MembershipRecord> history = new ArrayList<>();
      for (var element : historyArr) {
        try {
          JsonObject recObj = element.getAsJsonObject();
          history.add(new MembershipRecord(
            UUID.fromString(recObj.get("factionId").getAsString()),
            recObj.get("factionName").getAsString(),
            recObj.has("factionTag") && !recObj.get("factionTag").isJsonNull()
              ? recObj.get("factionTag").getAsString() : null,
            FactionRole.valueOf(recObj.get("highestRole").getAsString()),
            recObj.get("joinedAt").getAsLong(),
            recObj.get("leftAt").getAsLong(),
            MembershipRecord.LeaveReason.valueOf(recObj.get("reason").getAsString())
          ));
        } catch (Exception e) {
          ErrorHandler.report("Failed to deserialize membership record", e);
        }
      }
      data.setMembershipHistory(history);
    }

    return data;
  }
}
