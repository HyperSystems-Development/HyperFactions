package com.hyperfactions.storage;

import com.google.gson.*;
import com.hyperfactions.api.EconomyAPI;
import com.hyperfactions.data.FactionEconomy.TreasuryLimits;
import com.hyperfactions.data.FactionEconomy;
import com.hyperfactions.util.Logger;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Stores each faction's economy data in {@code data/economy/{factionId}.json}.
 * Follows the same patterns as {@link com.hyperfactions.storage.json.JsonFactionStorage}.
 */
public class JsonEconomyStorage {

  private final Path economyDir;

  private final Gson gson;

  /** Creates a new JsonEconomyStorage. */
  public JsonEconomyStorage(@NotNull Path dataDir) {
    this.economyDir = dataDir.resolve("economy");
    this.gson = new GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create();
  }

  /**
   * Initializes the storage directory.
   */
  public CompletableFuture<Void> init() {
    return CompletableFuture.runAsync(() -> {
      try {
        Files.createDirectories(economyDir);
        StorageUtils.cleanupOrphanedFiles(economyDir);
        Logger.info("[Storage] Economy storage initialized at %s", economyDir);
      } catch (IOException e) {
        Logger.severe("Failed to create economy directory", e);
      }
    });
  }

  /**
   * Loads all economy data from disk.
   *
   * @return map of factionId to FactionEconomy
   */
  public CompletableFuture<Map<UUID, FactionEconomy>> loadAll() {
    return CompletableFuture.supplyAsync(() -> {
      Map<UUID, FactionEconomy> economies = new HashMap<>();

      if (!Files.exists(economyDir)) {
        return economies;
      }

      try (DirectoryStream<Path> stream = Files.newDirectoryStream(economyDir, "*.json")) {
        for (Path file : stream) {
          try {
            String fileName = file.getFileName().toString();
            UUID factionId = UUID.fromString(fileName.replace(".json", ""));
            String json = Files.readString(file);
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            economies.put(factionId, deserialize(obj));
          } catch (Exception e) {
            Logger.severe("Failed to load economy file %s: %s",
                file.getFileName(), e.getMessage());
          }
        }
      } catch (IOException e) {
        Logger.severe("Failed to read economy directory", e);
      }

      Logger.info("[Storage] Loaded economy data for %d factions", economies.size());
      return economies;
    });
  }

  /**
   * Saves a single faction's economy data.
   *
   * @param factionId the faction ID
   * @param economy   the economy data
   */
  public CompletableFuture<Void> save(@NotNull UUID factionId, @NotNull FactionEconomy economy) {
    return CompletableFuture.runAsync(() -> {
      Path file = economyDir.resolve(factionId + ".json");
      JsonObject obj = serialize(economy);
      String content = gson.toJson(obj);
      StorageUtils.WriteResult result = StorageUtils.writeAtomic(file, content);
      if (result instanceof StorageUtils.WriteResult.Failure failure) {
        Logger.severe("Failed to save economy for faction %s: %s", factionId, failure.error());
      }
    });
  }

  /**
   * Saves all economy data.
   *
   * @param economies map of factionId to FactionEconomy
   */
  public CompletableFuture<Void> saveAll(@NotNull Map<UUID, FactionEconomy> economies) {
    return CompletableFuture.runAsync(() -> {
      for (var entry : economies.entrySet()) {
        Path file = economyDir.resolve(entry.getKey() + ".json");
        JsonObject obj = serialize(entry.getValue());
        String content = gson.toJson(obj);
        StorageUtils.WriteResult result = StorageUtils.writeAtomic(file, content);
        if (result instanceof StorageUtils.WriteResult.Failure failure) {
          Logger.severe("Failed to save economy for faction %s: %s",
              entry.getKey(), failure.error());
        }
      }
      Logger.debug("Saved economy data for %d factions", economies.size());
    });
  }

  /**
   * Deletes economy data for a faction.
   *
   * @param factionId the faction ID
   */
  public CompletableFuture<Void> delete(@NotNull UUID factionId) {
    return CompletableFuture.runAsync(() -> {
      Path file = economyDir.resolve(factionId + ".json");
      StorageUtils.deleteWithBackup(file);
      Logger.debug("Deleted economy data for faction %s", factionId);
    });
  }

  // === Serialization ===

  private JsonObject serialize(@NotNull FactionEconomy economy) {
    JsonObject obj = new JsonObject();
    obj.addProperty("balance", economy.balance().toPlainString());

    // Limits
    TreasuryLimits limits = economy.limits();
    JsonObject limitsObj = new JsonObject();
    limitsObj.addProperty("maxWithdrawAmount", limits.maxWithdrawAmount().toPlainString());
    limitsObj.addProperty("maxWithdrawPerPeriod", limits.maxWithdrawPerPeriod().toPlainString());
    limitsObj.addProperty("maxTransferAmount", limits.maxTransferAmount().toPlainString());
    limitsObj.addProperty("maxTransferPerPeriod", limits.maxTransferPerPeriod().toPlainString());
    limitsObj.addProperty("periodHours", limits.periodHours());
    obj.add("limits", limitsObj);

    // Transactions
    JsonArray transactions = new JsonArray();
    for (EconomyAPI.Transaction tx : economy.transactionHistory()) {
      JsonObject txObj = new JsonObject();
      if (tx.actorId() != null) {
        txObj.addProperty("actorId", tx.actorId().toString());
      }
      txObj.addProperty("type", tx.type().name());
      txObj.addProperty("amount", tx.amount().toPlainString());
      txObj.addProperty("balanceAfter", tx.balanceAfter().toPlainString());
      txObj.addProperty("timestamp", tx.timestamp());
      txObj.addProperty("description", tx.description());
      transactions.add(txObj);
    }
    obj.add("transactions", transactions);

    // Upkeep state
    obj.addProperty("lastUpkeepTimestamp", economy.lastUpkeepTimestamp());
    obj.addProperty("upkeepAutoPay", economy.upkeepAutoPay());

    return obj;
  }

  private FactionEconomy deserialize(@NotNull JsonObject obj) {
    BigDecimal balance = getBigDecimal(obj, "balance", BigDecimal.ZERO);

    // Limits
    TreasuryLimits limits = TreasuryLimits.defaults();
    if (obj.has("limits")) {
      JsonObject limitsObj = obj.getAsJsonObject("limits");
      limits = new TreasuryLimits(
          getBigDecimal(limitsObj, "maxWithdrawAmount", BigDecimal.ZERO),
          getBigDecimal(limitsObj, "maxWithdrawPerPeriod", BigDecimal.ZERO),
          getBigDecimal(limitsObj, "maxTransferAmount", BigDecimal.ZERO),
          getBigDecimal(limitsObj, "maxTransferPerPeriod", BigDecimal.ZERO),
          getInt(limitsObj, "periodHours", 24)
      );
    }

    // Transactions
    List<EconomyAPI.Transaction> transactions = new ArrayList<>();
    if (obj.has("transactions")) {
      JsonArray txArray = obj.getAsJsonArray("transactions");
      // We need factionId for the Transaction record but it's not stored in the file
      // (the factionId IS the filename). We pass a placeholder — callers set it from the key.
      for (JsonElement elem : txArray) {
        JsonObject txObj = elem.getAsJsonObject();
        UUID actorId = txObj.has("actorId") && !txObj.get("actorId").isJsonNull()
            ? UUID.fromString(txObj.get("actorId").getAsString()) : null;
        EconomyAPI.TransactionType type = EconomyAPI.TransactionType.valueOf(
            txObj.get("type").getAsString());
        BigDecimal amount = getBigDecimal(txObj, "amount", BigDecimal.ZERO);
        BigDecimal balanceAfter = getBigDecimal(txObj, "balanceAfter", BigDecimal.ZERO);
        long timestamp = txObj.get("timestamp").getAsLong();
        String description = txObj.has("description") ? txObj.get("description").getAsString() : "";

        // factionId is set to a dummy UUID — the caller (EconomyManager.loadAll) has the real one
        transactions.add(new EconomyAPI.Transaction(
            UUID.fromString("00000000-0000-0000-0000-000000000000"),
            actorId, type, amount, balanceAfter, timestamp, description
        ));
      }
    }

    // Upkeep state
    long lastUpkeepTimestamp = obj.has("lastUpkeepTimestamp")
        ? obj.get("lastUpkeepTimestamp").getAsLong() : 0L;
    boolean upkeepAutoPay = obj.has("upkeepAutoPay")
        ? obj.get("upkeepAutoPay").getAsBoolean() : true;

    return new FactionEconomy(balance, transactions, limits, lastUpkeepTimestamp, upkeepAutoPay);
  }

  /**
   * Reads a BigDecimal from a JsonObject, handling both old double format and new string format.
   * Uses getAsBigDecimal() which handles both number and string JSON values.
   */
  @NotNull
  private BigDecimal getBigDecimal(@NotNull JsonObject obj, @NotNull String key, @NotNull BigDecimal defaultValue) {
    if (!obj.has(key) || obj.get(key).isJsonNull()) {
      return defaultValue;
    }
    try {
      return obj.get(key).getAsBigDecimal();
    } catch (Exception e) {
      return defaultValue;
    }
  }

  private int getInt(JsonObject obj, String key, int defaultValue) {
    return obj.has(key) ? obj.get(key).getAsInt() : defaultValue;
  }
}
