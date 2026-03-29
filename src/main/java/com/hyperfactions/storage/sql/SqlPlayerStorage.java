package com.hyperfactions.storage.sql;

import com.hyperfactions.data.FactionRole;
import com.hyperfactions.data.MembershipRecord;
import com.hyperfactions.data.PlayerData;
import com.hyperfactions.data.PlayerPower;
import com.hyperfactions.storage.PlayerStorage;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.jdbi.v3.core.Jdbi;
import org.jetbrains.annotations.NotNull;

/**
 * SQL implementation of {@link PlayerStorage}.
 */
public class SqlPlayerStorage implements PlayerStorage {

  private final Jdbi jdbi;
  private final SqlDialect dialect;
  private final String playersTable;
  private final String historyTable;

  public SqlPlayerStorage(@NotNull Jdbi jdbi, @NotNull SqlDialect dialect, @NotNull String prefix) {
    this.jdbi = jdbi;
    this.dialect = dialect;
    this.playersTable = SqlHelper.table(prefix, "players");
    this.historyTable = SqlHelper.table(prefix, "player_membership_history");
  }

  @Override
  public CompletableFuture<Void> init() {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<Void> shutdown() {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  @NotNull
  public CompletableFuture<Optional<PlayerPower>> loadPlayerPower(@NotNull UUID uuid) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        return jdbi.withHandle(handle ->
            handle.createQuery("SELECT * FROM " + playersTable + " WHERE uuid = :uuid")
                .bind("uuid", uuid)
                .map((rs, ctx) -> mapPlayerPower(rs))
                .findOne()
        );
      } catch (Exception e) {
        ErrorHandler.report("Failed to load player power from SQL: " + uuid, e);
        return Optional.empty();
      }
    });
  }

  @Override
  @NotNull
  public CompletableFuture<Void> savePlayerPower(@NotNull PlayerPower power) {
    return CompletableFuture.runAsync(() -> {
      try {
        String upsert = SqlHelper.upsert(dialect, playersTable,
            new String[]{"uuid", "username", "power", "max_power", "last_death", "last_regen",
                "max_power_override", "power_loss_disabled", "claim_decay_exempt"},
            new String[]{":uuid", ":username", ":power", ":maxPower", ":lastDeath", ":lastRegen",
                ":maxPowerOverride", ":powerLossDisabled", ":claimDecayExempt"},
            new String[]{"uuid"},
            new String[]{"power", "max_power", "last_death", "last_regen",
                "max_power_override", "power_loss_disabled", "claim_decay_exempt"});

        jdbi.useHandle(handle ->
            handle.createUpdate(upsert)
                .bind("uuid", power.uuid())
                .bind("username", "") // Not available in PlayerPower
                .bind("power", power.power())
                .bind("maxPower", power.maxPower())
                .bind("lastDeath", power.lastDeath())
                .bind("lastRegen", power.lastRegen())
                .bind("maxPowerOverride", power.maxPowerOverride())
                .bind("powerLossDisabled", power.powerLossDisabled())
                .bind("claimDecayExempt", power.claimDecayExempt())
                .execute()
        );
      } catch (Exception e) {
        ErrorHandler.report("Failed to save player power to SQL: " + power.uuid(), e);
      }
    });
  }

  @Override
  @NotNull
  public CompletableFuture<Void> deletePlayerPower(@NotNull UUID uuid) {
    return CompletableFuture.runAsync(() -> {
      try {
        jdbi.useTransaction(handle -> {
          handle.execute("DELETE FROM " + historyTable + " WHERE player_uuid = ?", uuid.toString());
          handle.execute("DELETE FROM " + playersTable + " WHERE uuid = ?", uuid.toString());
        });
      } catch (Exception e) {
        ErrorHandler.report("Failed to delete player power from SQL: " + uuid, e);
      }
    });
  }

  @Override
  @NotNull
  public CompletableFuture<Collection<PlayerPower>> loadAllPlayerPower() {
    return CompletableFuture.supplyAsync(() -> {
      try {
        List<PlayerPower> powers = jdbi.withHandle(handle ->
            handle.createQuery("SELECT * FROM " + playersTable)
                .map((rs, ctx) -> mapPlayerPower(rs))
                .list()
        );
        Logger.info("[Storage] Loaded power data for %d players from SQL", powers.size());
        return powers;
      } catch (Exception e) {
        ErrorHandler.report("Failed to load all player power from SQL", e);
        return List.of();
      }
    });
  }

  @Override
  @NotNull
  public CompletableFuture<Set<UUID>> getAllPlayerUuids() {
    return CompletableFuture.supplyAsync(() -> {
      try {
        return new HashSet<>(jdbi.withHandle(handle ->
            handle.createQuery("SELECT uuid FROM " + playersTable)
                .map((rs, ctx) -> UUID.fromString(rs.getString("uuid")))
                .list()
        ));
      } catch (Exception e) {
        ErrorHandler.report("Failed to get all player UUIDs from SQL", e);
        return Set.of();
      }
    });
  }

  @Override
  @NotNull
  public CompletableFuture<Optional<PlayerData>> loadPlayerData(@NotNull UUID uuid) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        return jdbi.withHandle(handle -> {
          // Load player row
          Optional<PlayerData> playerOpt = handle.createQuery("SELECT * FROM " + playersTable + " WHERE uuid = :uuid")
              .bind("uuid", uuid)
              .map((rs, ctx) -> mapPlayerData(rs))
              .findOne();

          if (playerOpt.isEmpty()) {
            return Optional.<PlayerData>empty();
          }

          PlayerData data = playerOpt.get();

          // Load membership history
          List<MembershipRecord> history = handle.createQuery(
                  "SELECT * FROM " + historyTable + " WHERE player_uuid = :uuid ORDER BY joined_at DESC")
              .bind("uuid", uuid)
              .map((rs, ctx) -> new MembershipRecord(
                  UUID.fromString(rs.getString("faction_id")),
                  rs.getString("faction_name"),
                  rs.getString("faction_tag"),
                  FactionRole.valueOf(rs.getString("highest_role")),
                  rs.getLong("joined_at"),
                  rs.getLong("left_at"),
                  MembershipRecord.LeaveReason.valueOf(rs.getString("reason"))
              ))
              .list();
          data.getMembershipHistory().addAll(history);

          return Optional.of(data);
        });
      } catch (Exception e) {
        ErrorHandler.report("Failed to load player data from SQL: " + uuid, e);
        return Optional.empty();
      }
    });
  }

  @Override
  @NotNull
  public CompletableFuture<Void> savePlayerData(@NotNull PlayerData data) {
    return CompletableFuture.runAsync(() -> {
      try {
        jdbi.useTransaction(handle -> {
          // Upsert player row
          String upsert = SqlHelper.upsert(dialect, playersTable,
              new String[]{"uuid", "username", "power", "max_power", "last_death", "last_regen",
                  "kills", "deaths", "first_joined", "last_online",
                  "max_power_override", "power_loss_disabled", "claim_decay_exempt",
                  "admin_bypass_enabled", "language_preference",
                  "territory_alerts_enabled", "death_announcements_enabled", "power_notifications_enabled"},
              new String[]{":uuid", ":username", ":power", ":maxPower", ":lastDeath", ":lastRegen",
                  ":kills", ":deaths", ":firstJoined", ":lastOnline",
                  ":maxPowerOverride", ":powerLossDisabled", ":claimDecayExempt",
                  ":adminBypass", ":langPref",
                  ":territoryAlerts", ":deathAnnouncements", ":powerNotifications"},
              new String[]{"uuid"},
              new String[]{"username", "power", "max_power", "last_death", "last_regen",
                  "kills", "deaths", "first_joined", "last_online",
                  "max_power_override", "power_loss_disabled", "claim_decay_exempt",
                  "admin_bypass_enabled", "language_preference",
                  "territory_alerts_enabled", "death_announcements_enabled", "power_notifications_enabled"});

          handle.createUpdate(upsert)
              .bind("uuid", data.getUuid())
              .bind("username", data.getUsername())
              .bind("power", data.getPower())
              .bind("maxPower", data.getMaxPower())
              .bind("lastDeath", data.getLastDeath())
              .bind("lastRegen", data.getLastRegen())
              .bind("kills", data.getKills())
              .bind("deaths", data.getDeaths())
              .bind("firstJoined", data.getFirstJoined())
              .bind("lastOnline", data.getLastOnline())
              .bind("maxPowerOverride", data.getMaxPowerOverride())
              .bind("powerLossDisabled", data.isPowerLossDisabled())
              .bind("claimDecayExempt", data.isClaimDecayExempt())
              .bind("adminBypass", data.isAdminBypassEnabled())
              .bind("langPref", data.getLanguagePreference())
              .bind("territoryAlerts", data.isTerritoryAlertsEnabled())
              .bind("deathAnnouncements", data.isDeathAnnouncementsEnabled())
              .bind("powerNotifications", data.isPowerNotificationsEnabled())
              .execute();

          // Replace membership history
          handle.execute("DELETE FROM " + historyTable + " WHERE player_uuid = ?",
              data.getUuid().toString());
          if (!data.getMembershipHistory().isEmpty()) {
            var batch = handle.prepareBatch(
                "INSERT INTO " + historyTable
                    + " (player_uuid, faction_id, faction_name, faction_tag, highest_role,"
                    + " joined_at, left_at, reason)"
                    + " VALUES (:playerUuid, :factionId, :factionName, :factionTag, :highestRole,"
                    + " :joinedAt, :leftAt, :reason)");
            for (MembershipRecord record : data.getMembershipHistory()) {
              batch
                  .bind("playerUuid", data.getUuid())
                  .bind("factionId", record.factionId())
                  .bind("factionName", record.factionName())
                  .bind("factionTag", record.factionTag())
                  .bind("highestRole", record.highestRole().name())
                  .bind("joinedAt", record.joinedAt())
                  .bind("leftAt", record.leftAt())
                  .bind("reason", record.reason().name())
                  .add();
            }
            batch.execute();
          }
        });
      } catch (Exception e) {
        ErrorHandler.report("Failed to save player data to SQL: " + data.getUuid(), e);
      }
    });
  }

  @Override
  @NotNull
  public CompletableFuture<Void> updatePlayerData(@NotNull UUID uuid, @NotNull Consumer<PlayerData> updater) {
    return CompletableFuture.runAsync(() -> {
      try {
        jdbi.useTransaction(handle -> {
          // SELECT FOR UPDATE to acquire row-level lock
          Optional<PlayerData> dataOpt = handle.createQuery(
                  "SELECT * FROM " + playersTable + " WHERE uuid = :uuid FOR UPDATE")
              .bind("uuid", uuid)
              .map((rs, ctx) -> mapPlayerData(rs))
              .findOne();

          if (dataOpt.isEmpty()) {
            return;
          }

          PlayerData data = dataOpt.get();

          // Load membership history within the same transaction
          List<MembershipRecord> history = handle.createQuery(
                  "SELECT * FROM " + historyTable + " WHERE player_uuid = :uuid ORDER BY joined_at DESC")
              .bind("uuid", uuid)
              .map((rs, ctx) -> new MembershipRecord(
                  UUID.fromString(rs.getString("faction_id")),
                  rs.getString("faction_name"),
                  rs.getString("faction_tag"),
                  FactionRole.valueOf(rs.getString("highest_role")),
                  rs.getLong("joined_at"),
                  rs.getLong("left_at"),
                  MembershipRecord.LeaveReason.valueOf(rs.getString("reason"))
              ))
              .list();
          data.getMembershipHistory().addAll(history);

          // Apply the update
          updater.accept(data);

          // Save back (reuse the save logic inline to stay in the same transaction)
          handle.createUpdate("UPDATE " + playersTable + " SET "
                  + "username = :username, power = :power, max_power = :maxPower,"
                  + " last_death = :lastDeath, last_regen = :lastRegen,"
                  + " kills = :kills, deaths = :deaths,"
                  + " first_joined = :firstJoined, last_online = :lastOnline,"
                  + " max_power_override = :maxPowerOverride,"
                  + " power_loss_disabled = :powerLossDisabled,"
                  + " claim_decay_exempt = :claimDecayExempt,"
                  + " admin_bypass_enabled = :adminBypass,"
                  + " language_preference = :langPref,"
                  + " territory_alerts_enabled = :territoryAlerts,"
                  + " death_announcements_enabled = :deathAnnouncements,"
                  + " power_notifications_enabled = :powerNotifications"
                  + " WHERE uuid = :uuid")
              .bind("uuid", data.getUuid())
              .bind("username", data.getUsername())
              .bind("power", data.getPower())
              .bind("maxPower", data.getMaxPower())
              .bind("lastDeath", data.getLastDeath())
              .bind("lastRegen", data.getLastRegen())
              .bind("kills", data.getKills())
              .bind("deaths", data.getDeaths())
              .bind("firstJoined", data.getFirstJoined())
              .bind("lastOnline", data.getLastOnline())
              .bind("maxPowerOverride", data.getMaxPowerOverride())
              .bind("powerLossDisabled", data.isPowerLossDisabled())
              .bind("claimDecayExempt", data.isClaimDecayExempt())
              .bind("adminBypass", data.isAdminBypassEnabled())
              .bind("langPref", data.getLanguagePreference())
              .bind("territoryAlerts", data.isTerritoryAlertsEnabled())
              .bind("deathAnnouncements", data.isDeathAnnouncementsEnabled())
              .bind("powerNotifications", data.isPowerNotificationsEnabled())
              .execute();

          // Replace membership history
          handle.execute("DELETE FROM " + historyTable + " WHERE player_uuid = ?", uuid.toString());
          if (!data.getMembershipHistory().isEmpty()) {
            var batch = handle.prepareBatch(
                "INSERT INTO " + historyTable
                    + " (player_uuid, faction_id, faction_name, faction_tag, highest_role,"
                    + " joined_at, left_at, reason)"
                    + " VALUES (:playerUuid, :factionId, :factionName, :factionTag, :highestRole,"
                    + " :joinedAt, :leftAt, :reason)");
            for (MembershipRecord record : data.getMembershipHistory()) {
              batch
                  .bind("playerUuid", uuid)
                  .bind("factionId", record.factionId())
                  .bind("factionName", record.factionName())
                  .bind("factionTag", record.factionTag())
                  .bind("highestRole", record.highestRole().name())
                  .bind("joinedAt", record.joinedAt())
                  .bind("leftAt", record.leftAt())
                  .bind("reason", record.reason().name())
                  .add();
            }
            batch.execute();
          }
        });
      } catch (Exception e) {
        ErrorHandler.report("Failed to update player data in SQL: " + uuid, e);
      }
    });
  }

  // === Row Mappers ===

  private PlayerPower mapPlayerPower(java.sql.ResultSet rs) throws java.sql.SQLException {
    Double maxPowerOverride = rs.getObject("max_power_override") != null
        ? rs.getDouble("max_power_override") : null;
    return new PlayerPower(
        UUID.fromString(rs.getString("uuid")),
        rs.getDouble("power"),
        rs.getDouble("max_power"),
        rs.getLong("last_death"),
        rs.getLong("last_regen"),
        maxPowerOverride,
        rs.getBoolean("power_loss_disabled"),
        rs.getBoolean("claim_decay_exempt")
    );
  }

  private PlayerData mapPlayerData(java.sql.ResultSet rs) throws java.sql.SQLException {
    PlayerData data = new PlayerData(UUID.fromString(rs.getString("uuid")));
    data.setUsername(rs.getString("username"));
    data.setPower(rs.getDouble("power"));
    data.setMaxPower(rs.getDouble("max_power"));
    data.setLastDeath(rs.getLong("last_death"));
    data.setLastRegen(rs.getLong("last_regen"));
    data.setKills(rs.getInt("kills"));
    data.setDeaths(rs.getInt("deaths"));
    data.setFirstJoined(rs.getLong("first_joined"));
    data.setLastOnline(rs.getLong("last_online"));
    data.setMaxPowerOverride(rs.getObject("max_power_override") != null
        ? rs.getDouble("max_power_override") : null);
    data.setPowerLossDisabled(rs.getBoolean("power_loss_disabled"));
    data.setClaimDecayExempt(rs.getBoolean("claim_decay_exempt"));
    data.setAdminBypassEnabled(rs.getBoolean("admin_bypass_enabled"));
    data.setLanguagePreference(rs.getString("language_preference"));
    data.setTerritoryAlertsEnabled(rs.getBoolean("territory_alerts_enabled"));
    data.setDeathAnnouncementsEnabled(rs.getBoolean("death_announcements_enabled"));
    data.setPowerNotificationsEnabled(rs.getBoolean("power_notifications_enabled"));
    return data;
  }
}
