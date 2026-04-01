package com.hyperfactions.storage.sql;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hyperfactions.api.EconomyAPI;
import com.hyperfactions.data.FactionEconomy;
import com.hyperfactions.data.FactionEconomy.TreasuryLimits;
import com.hyperfactions.storage.EconomyStorage;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.jdbi.v3.core.Jdbi;
import org.jetbrains.annotations.NotNull;

/**
 * SQL implementation of {@link EconomyStorage}.
 */
public class SqlEconomyStorage implements EconomyStorage {

  private final Jdbi jdbi;
  private final SqlDialect dialect;
  private final String economyTable;
  private final String txTable;
  private final Gson gson;

  public SqlEconomyStorage(@NotNull Jdbi jdbi, @NotNull SqlDialect dialect, @NotNull String prefix) {
    this.jdbi = jdbi;
    this.dialect = dialect;
    this.economyTable = SqlHelper.table(prefix, "faction_economy");
    this.txTable = SqlHelper.table(prefix, "faction_transactions");
    this.gson = new GsonBuilder().disableHtmlEscaping().create();
  }

  @Override
  public CompletableFuture<Void> init() {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  @NotNull
  public CompletableFuture<Map<UUID, FactionEconomy>> loadAll() {
    return CompletableFuture.supplyAsync(() -> {
      try {
        // Load economy rows
        Map<UUID, FactionEconomy> economies = new HashMap<>();
        jdbi.withHandle(handle -> {
          handle.createQuery("SELECT * FROM " + economyTable)
              .map((rs, ctx) -> {
                UUID factionId = UUID.fromString(rs.getString("faction_id"));
                BigDecimal balance = rs.getBigDecimal("balance");
                String limitsJson = rs.getString("limits_json");
                TreasuryLimits limits = limitsJson != null
                    ? gson.fromJson(limitsJson, TreasuryLimits.class)
                    : TreasuryLimits.defaults();

                economies.put(factionId, new FactionEconomy(
                    balance,
                    new ArrayList<>(), // transactions loaded below
                    limits,
                    rs.getLong("last_upkeep_timestamp"),
                    rs.getBoolean("upkeep_auto_pay"),
                    rs.getLong("upkeep_grace_start_timestamp"),
                    rs.getInt("consecutive_missed_payments")
                ));
                return null;
              })
              .list();
          return null;
        });

        // Load transactions per faction into temp map, then rebuild economies
        if (!economies.isEmpty()) {
          Map<UUID, List<EconomyAPI.Transaction>> txMap = new HashMap<>();
          jdbi.withHandle(handle -> {
            handle.createQuery("SELECT * FROM " + txTable + " ORDER BY timestamp DESC")
                .map((rs, ctx) -> {
                  UUID factionId = UUID.fromString(rs.getString("faction_id"));
                  if (economies.containsKey(factionId)) {
                    String actorIdStr = rs.getString("actor_id");
                    UUID actorId = actorIdStr != null ? UUID.fromString(actorIdStr) : null;
                    txMap.computeIfAbsent(factionId, k -> new ArrayList<>())
                        .add(new EconomyAPI.Transaction(
                            factionId,
                            actorId,
                            EconomyAPI.TransactionType.valueOf(rs.getString("type")),
                            rs.getBigDecimal("amount"),
                            rs.getBigDecimal("balance_after"),
                            rs.getLong("timestamp"),
                            rs.getString("description")
                        ));
                  }
                  return null;
                })
                .list();
            return null;
          });

          // Rebuild economy objects with their transactions
          for (var entry : txMap.entrySet()) {
            FactionEconomy econ = economies.get(entry.getKey());
            if (econ != null) {
              economies.put(entry.getKey(), new FactionEconomy(
                  econ.balance(), entry.getValue(), econ.limits(),
                  econ.lastUpkeepTimestamp(), econ.upkeepAutoPay(),
                  econ.upkeepGraceStartTimestamp(), econ.consecutiveMissedPayments()
              ));
            }
          }
        }

        Logger.info("[Storage] Loaded economy data for %d factions from SQL", economies.size());
        return economies;
      } catch (Exception e) {
        ErrorHandler.report("Failed to load economy data from SQL", e);
        return Map.of();
      }
    });
  }

  @Override
  @NotNull
  public CompletableFuture<Void> save(@NotNull UUID factionId, @NotNull FactionEconomy economy) {
    return CompletableFuture.runAsync(() -> {
      try {
        jdbi.useTransaction(handle -> {
          // Upsert economy row
          String upsert = SqlHelper.upsert(dialect, economyTable,
              new String[]{"faction_id", "balance", "limits_json", "last_upkeep_timestamp",
                  "upkeep_auto_pay", "upkeep_grace_start_timestamp", "consecutive_missed_payments"},
              new String[]{":factionId", ":balance", ":limitsJson", ":lastUpkeep",
                  ":autoPay", ":graceStart", ":missedPayments"},
              new String[]{"faction_id"},
              new String[]{"balance", "limits_json", "last_upkeep_timestamp",
                  "upkeep_auto_pay", "upkeep_grace_start_timestamp", "consecutive_missed_payments"});

          handle.createUpdate(upsert)
              .bind("factionId", factionId)
              .bind("balance", economy.balance())
              .bind("limitsJson", gson.toJson(economy.limits()))
              .bind("lastUpkeep", economy.lastUpkeepTimestamp())
              .bind("autoPay", economy.upkeepAutoPay())
              .bind("graceStart", economy.upkeepGraceStartTimestamp())
              .bind("missedPayments", economy.consecutiveMissedPayments())
              .execute();

          // Replace transactions (delete + reinsert, capped at MAX_HISTORY)
          handle.execute("DELETE FROM " + txTable + " WHERE faction_id = ?", factionId.toString());
          List<EconomyAPI.Transaction> txs = economy.transactionHistory();
          int limit = Math.min(txs.size(), FactionEconomy.MAX_HISTORY);
          if (limit > 0) {
            var batch = handle.prepareBatch(
                "INSERT INTO " + txTable
                    + " (faction_id, actor_id, type, amount, balance_after, timestamp, description)"
                    + " VALUES (:factionId, :actorId, :type, :amount, :balanceAfter, :timestamp, :description)");
            for (int i = 0; i < limit; i++) {
              EconomyAPI.Transaction tx = txs.get(i);
              batch
                  .bind("factionId", factionId)
                  .bind("actorId", tx.actorId())
                  .bind("type", tx.type().name())
                  .bind("amount", tx.amount())
                  .bind("balanceAfter", tx.balanceAfter())
                  .bind("timestamp", tx.timestamp())
                  .bind("description", tx.description())
                  .add();
            }
            batch.execute();
          }
        });
      } catch (Exception e) {
        ErrorHandler.report("Failed to save economy for faction " + factionId + " to SQL", e);
      }
    });
  }

  @Override
  @NotNull
  public CompletableFuture<Void> saveAll(@NotNull Map<UUID, FactionEconomy> economies) {
    return CompletableFuture.runAsync(() -> {
      for (var entry : economies.entrySet()) {
        save(entry.getKey(), entry.getValue()).join();
      }
      Logger.debug("Saved economy data for %d factions to SQL", economies.size());
    });
  }

  @Override
  @NotNull
  public CompletableFuture<Void> delete(@NotNull UUID factionId) {
    return CompletableFuture.runAsync(() -> {
      try {
        jdbi.useTransaction(handle -> {
          handle.execute("DELETE FROM " + txTable + " WHERE faction_id = ?", factionId.toString());
          handle.execute("DELETE FROM " + economyTable + " WHERE faction_id = ?", factionId.toString());
        });
        Logger.debug("Deleted economy data for faction %s from SQL", factionId);
      } catch (Exception e) {
        ErrorHandler.report("Failed to delete economy for faction " + factionId + " from SQL", e);
      }
    });
  }
}
