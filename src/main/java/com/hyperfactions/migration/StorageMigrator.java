package com.hyperfactions.migration;

import com.hyperfactions.data.*;
import com.hyperfactions.storage.*;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Migrates data between storage backends (JSON ↔ SQL, SQL ↔ SQL).
 *
 * <p>Loads all data from the source storage, then writes it to the target storage.
 * Each data type is migrated independently. Progress is reported via callback.</p>
 */
public class StorageMigrator {

  /**
   * Progress update during migration.
   */
  public record Progress(@NotNull String dataType, int current, int total, boolean done) {}

  /**
   * Result of a completed migration.
   */
  public record MigrationResult(
      int factions,
      int players,
      int zones,
      int economies,
      int chatHistories,
      int invites,
      int joinRequests,
      @Nullable String error
  ) {
    public int total() {
      return factions + players + zones + economies + chatHistories + invites + joinRequests;
    }

    public boolean isSuccess() {
      return error == null;
    }

    public static MigrationResult failure(String error) {
      return new MigrationResult(0, 0, 0, 0, 0, 0, 0, error);
    }
  }

  private final StorageFactory.StorageBundle source;
  private final StorageFactory.StorageBundle target;
  private final Consumer<Progress> progressCallback;

  /**
   * Creates a new migrator.
   *
   * @param source           the source storage bundle to read from
   * @param target           the target storage bundle to write to
   * @param progressCallback callback for progress updates
   */
  public StorageMigrator(@NotNull StorageFactory.StorageBundle source,
              @NotNull StorageFactory.StorageBundle target,
              @NotNull Consumer<Progress> progressCallback) {
    this.source = source;
    this.target = target;
    this.progressCallback = progressCallback;
  }

  /**
   * Runs the full migration synchronously.
   *
   * @return the migration result
   */
  @NotNull
  public MigrationResult migrate() {
    try {
      // Initialize both storage backends
      source.factionStorage().init().join();
      source.playerStorage().init().join();
      source.zoneStorage().init().join();
      source.chatHistoryStorage().init().join();
      source.economyStorage().init().join();
      source.inviteStorage().init().join();
      source.joinRequestStorage().init().join();

      target.factionStorage().init().join();
      target.playerStorage().init().join();
      target.zoneStorage().init().join();
      target.chatHistoryStorage().init().join();
      target.economyStorage().init().join();
      target.inviteStorage().init().join();
      target.joinRequestStorage().init().join();

      // Migrate each data type in dependency order
      int factions = migrateFactions();
      int players = migratePlayers();
      int zones = migrateZones();
      int economies = migrateEconomies();
      int chatHistories = migrateChatHistories();
      int invites = migrateInvites();
      int joinRequests = migrateJoinRequests();

      return new MigrationResult(factions, players, zones, economies,
          chatHistories, invites, joinRequests, null);
    } catch (Exception e) {
      ErrorHandler.report("Storage migration failed", e);
      return MigrationResult.failure(e.getMessage());
    }
  }

  private int migrateFactions() {
    progressCallback.accept(new Progress("factions", 0, 0, false));
    Collection<Faction> factions = source.factionStorage().loadAllFactions().join();
    int total = factions.size();
    int i = 0;
    for (Faction faction : factions) {
      target.factionStorage().saveFaction(faction).join();
      i++;
      progressCallback.accept(new Progress("factions", i, total, false));
    }
    progressCallback.accept(new Progress("factions", total, total, true));
    Logger.info("[Migration] Migrated %d factions", total);
    return total;
  }

  private int migratePlayers() {
    progressCallback.accept(new Progress("players", 0, 0, false));
    // Load full player data (power + stats + history)
    Set<UUID> uuids = source.playerStorage().getAllPlayerUuids().join();
    int total = uuids.size();
    int i = 0;
    for (UUID uuid : uuids) {
      Optional<PlayerData> data = source.playerStorage().loadPlayerData(uuid).join();
      if (data.isPresent()) {
        target.playerStorage().savePlayerData(data.get()).join();
      }
      i++;
      if (i % 100 == 0 || i == total) {
        progressCallback.accept(new Progress("players", i, total, false));
      }
    }
    progressCallback.accept(new Progress("players", total, total, true));
    Logger.info("[Migration] Migrated %d players", total);
    return total;
  }

  private int migrateZones() {
    progressCallback.accept(new Progress("zones", 0, 0, false));
    Collection<Zone> zones = source.zoneStorage().loadAllZones().join();
    target.zoneStorage().saveAllZones(zones).join();
    int total = zones.size();
    progressCallback.accept(new Progress("zones", total, total, true));
    Logger.info("[Migration] Migrated %d zones", total);
    return total;
  }

  private int migrateEconomies() {
    progressCallback.accept(new Progress("economy", 0, 0, false));
    Map<UUID, FactionEconomy> economies = source.economyStorage().loadAll().join();
    target.economyStorage().saveAll(economies).join();
    int total = economies.size();
    progressCallback.accept(new Progress("economy", total, total, true));
    Logger.info("[Migration] Migrated %d faction economies", total);
    return total;
  }

  private int migrateChatHistories() {
    progressCallback.accept(new Progress("chat history", 0, 0, false));
    List<UUID> factionIds = source.chatHistoryStorage().listAllFactionIds().join();
    int total = factionIds.size();
    int i = 0;
    for (UUID factionId : factionIds) {
      FactionChatHistory history = source.chatHistoryStorage().loadHistory(factionId).join();
      if (!history.messages().isEmpty()) {
        target.chatHistoryStorage().saveHistory(history).join();
      }
      i++;
      if (i % 50 == 0 || i == total) {
        progressCallback.accept(new Progress("chat history", i, total, false));
      }
    }
    progressCallback.accept(new Progress("chat history", total, total, true));
    Logger.info("[Migration] Migrated %d chat histories", total);
    return total;
  }

  private int migrateInvites() {
    progressCallback.accept(new Progress("invites", 0, 0, false));
    List<PendingInvite> invites = source.inviteStorage().loadAll().join();
    target.inviteStorage().saveAll(invites).join();
    int total = invites.size();
    progressCallback.accept(new Progress("invites", total, total, true));
    Logger.info("[Migration] Migrated %d invites", total);
    return total;
  }

  private int migrateJoinRequests() {
    progressCallback.accept(new Progress("join requests", 0, 0, false));
    List<JoinRequest> requests = source.joinRequestStorage().loadAll().join();
    target.joinRequestStorage().saveAll(requests).join();
    int total = requests.size();
    progressCallback.accept(new Progress("join requests", total, total, true));
    Logger.info("[Migration] Migrated %d join requests", total);
    return total;
  }
}
