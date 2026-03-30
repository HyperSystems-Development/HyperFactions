package com.hyperfactions.storage.sql;

import com.hyperfactions.data.*;
import com.hyperfactions.storage.*;
import com.hyperfactions.storage.json.*;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;

/**
 * SQL storage exporter — exports all SQL data to JSON files for portable backups,
 * and imports JSON data back into SQL for restore operations.
 *
 * <p>Uses JSON storage implementations as intermediaries: reads from SQL storage,
 * writes to JSON files (export), or reads JSON files and writes to SQL (import).</p>
 */
public class SqlStorageExporter implements StorageExporter {

  private final FactionStorage sqlFactionStorage;
  private final PlayerStorage sqlPlayerStorage;
  private final ZoneStorage sqlZoneStorage;
  private final ChatHistoryStorage sqlChatStorage;
  private final EconomyStorage sqlEconomyStorage;
  private final InviteStorage sqlInviteStorage;
  private final JoinRequestStorage sqlJoinRequestStorage;

  public SqlStorageExporter(@NotNull FactionStorage factionStorage,
               @NotNull PlayerStorage playerStorage,
               @NotNull ZoneStorage zoneStorage,
               @NotNull ChatHistoryStorage chatStorage,
               @NotNull EconomyStorage economyStorage,
               @NotNull InviteStorage inviteStorage,
               @NotNull JoinRequestStorage joinRequestStorage) {
    this.sqlFactionStorage = factionStorage;
    this.sqlPlayerStorage = playerStorage;
    this.sqlZoneStorage = zoneStorage;
    this.sqlChatStorage = chatStorage;
    this.sqlEconomyStorage = economyStorage;
    this.sqlInviteStorage = inviteStorage;
    this.sqlJoinRequestStorage = joinRequestStorage;
  }

  @Override
  public boolean requiresExport() {
    return true;
  }

  @Override
  public CompletableFuture<ExportResult> exportToJson(@NotNull Path targetDir) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        // Create JSON storage instances pointing at the target directory
        JsonFactionStorage jsonFactions = new JsonFactionStorage(targetDir);
        JsonPlayerStorage jsonPlayers = new JsonPlayerStorage(targetDir);
        JsonZoneStorage jsonZones = new JsonZoneStorage(targetDir);
        JsonChatHistoryStorage jsonChat = new JsonChatHistoryStorage(targetDir);
        JsonEconomyStorage jsonEconomy = new JsonEconomyStorage(targetDir);
        JsonInviteStorage jsonInvites = new JsonInviteStorage(targetDir);
        JsonJoinRequestStorage jsonRequests = new JsonJoinRequestStorage(targetDir);

        // Initialize target directories
        jsonFactions.init().join();
        jsonPlayers.init().join();
        jsonZones.init().join();
        jsonChat.init().join();
        jsonEconomy.init().join();

        // Export factions
        Collection<Faction> factions = sqlFactionStorage.loadAllFactions().join();
        for (Faction faction : factions) {
          jsonFactions.saveFaction(faction).join();
        }

        // Export players
        Set<UUID> playerUuids = sqlPlayerStorage.getAllPlayerUuids().join();
        int playerCount = 0;
        for (UUID uuid : playerUuids) {
          Optional<PlayerData> data = sqlPlayerStorage.loadPlayerData(uuid).join();
          if (data.isPresent()) {
            jsonPlayers.savePlayerData(data.get()).join();
            playerCount++;
          }
        }

        // Export zones
        Collection<Zone> zones = sqlZoneStorage.loadAllZones().join();
        jsonZones.saveAllZones(zones).join();

        // Export economy
        Map<UUID, FactionEconomy> economies = sqlEconomyStorage.loadAll().join();
        jsonEconomy.saveAll(economies).join();

        // Export chat history
        List<UUID> chatFactionIds = sqlChatStorage.listAllFactionIds().join();
        int chatCount = 0;
        for (UUID factionId : chatFactionIds) {
          FactionChatHistory history = sqlChatStorage.loadHistory(factionId).join();
          if (!history.messages().isEmpty()) {
            jsonChat.saveHistory(history).join();
            chatCount++;
          }
        }

        // Export invites
        List<PendingInvite> invites = sqlInviteStorage.loadAll().join();
        jsonInvites.saveAll(invites).join();

        // Export join requests
        List<JoinRequest> requests = sqlJoinRequestStorage.loadAll().join();
        jsonRequests.saveAll(requests).join();

        Logger.info("[Backup] Exported SQL data to JSON: %d factions, %d players, %d zones",
            factions.size(), playerCount, zones.size());

        return new ExportResult(factions.size(), playerCount, zones.size(),
            economies.size(), chatCount, invites.size(), requests.size());
      } catch (Exception e) {
        ErrorHandler.report("[Backup] Failed to export SQL data to JSON", e);
        return new ExportResult(0, 0, 0, 0, 0, 0, 0);
      }
    });
  }

  @Override
  public CompletableFuture<ImportResult> importFromJson(@NotNull Path sourceDir) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        // Create JSON storage instances to read from the source directory
        JsonFactionStorage jsonFactions = new JsonFactionStorage(sourceDir);
        JsonPlayerStorage jsonPlayers = new JsonPlayerStorage(sourceDir);
        JsonZoneStorage jsonZones = new JsonZoneStorage(sourceDir);
        JsonChatHistoryStorage jsonChat = new JsonChatHistoryStorage(sourceDir);
        JsonEconomyStorage jsonEconomy = new JsonEconomyStorage(sourceDir);
        JsonInviteStorage jsonInvites = new JsonInviteStorage(sourceDir);
        JsonJoinRequestStorage jsonRequests = new JsonJoinRequestStorage(sourceDir);

        // Initialize source readers
        jsonFactions.init().join();
        jsonPlayers.init().join();
        jsonZones.init().join();
        jsonChat.init().join();
        jsonEconomy.init().join();

        // Import factions
        Collection<Faction> factions = jsonFactions.loadAllFactions().join();
        for (Faction faction : factions) {
          sqlFactionStorage.saveFaction(faction).join();
        }

        // Import players
        Set<UUID> playerUuids = jsonPlayers.getAllPlayerUuids().join();
        int playerCount = 0;
        for (UUID uuid : playerUuids) {
          Optional<PlayerData> data = jsonPlayers.loadPlayerData(uuid).join();
          if (data.isPresent()) {
            sqlPlayerStorage.savePlayerData(data.get()).join();
            playerCount++;
          }
        }

        // Import zones
        Collection<Zone> zones = jsonZones.loadAllZones().join();
        sqlZoneStorage.saveAllZones(zones).join();

        // Import economy
        Map<UUID, FactionEconomy> economies = jsonEconomy.loadAll().join();
        sqlEconomyStorage.saveAll(economies).join();

        // Import chat history
        List<UUID> chatFactionIds = jsonChat.listAllFactionIds().join();
        int chatCount = 0;
        for (UUID factionId : chatFactionIds) {
          FactionChatHistory history = jsonChat.loadHistory(factionId).join();
          if (!history.messages().isEmpty()) {
            sqlChatStorage.saveHistory(history).join();
            chatCount++;
          }
        }

        // Import invites
        List<PendingInvite> invites = jsonInvites.loadAll().join();
        sqlInviteStorage.saveAll(invites).join();

        // Import join requests
        List<JoinRequest> requests = jsonRequests.loadAll().join();
        sqlJoinRequestStorage.saveAll(requests).join();

        Logger.info("[Backup] Imported JSON data to SQL: %d factions, %d players, %d zones",
            factions.size(), playerCount, zones.size());

        return new ImportResult(factions.size(), playerCount, zones.size(),
            economies.size(), chatCount, invites.size(), requests.size());
      } catch (Exception e) {
        ErrorHandler.report("[Backup] Failed to import JSON data to SQL", e);
        return new ImportResult(0, 0, 0, 0, 0, 0, 0);
      }
    });
  }
}
