package com.hyperfactions.storage.sql;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.hyperfactions.data.*;
import com.hyperfactions.storage.FactionStorage;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.jdbi.v3.core.Jdbi;
import org.jetbrains.annotations.NotNull;

/**
 * SQL implementation of {@link FactionStorage}.
 *
 * <p>Uses multi-query assembly pattern: loads faction rows from the main table,
 * then batch-loads members, claims, relations, logs, and permissions in
 * separate queries, assembling them in Java.</p>
 */
public class SqlFactionStorage implements FactionStorage {

  private static final int MAX_LOGS = 100;

  private final Jdbi jdbi;
  private final SqlDialect dialect;
  private final String factionsTable;
  private final String membersTable;
  private final String claimsTable;
  private final String relationsTable;
  private final String logsTable;
  private final Gson gson;

  public SqlFactionStorage(@NotNull Jdbi jdbi, @NotNull SqlDialect dialect, @NotNull String prefix) {
    this.jdbi = jdbi;
    this.dialect = dialect;
    this.factionsTable = SqlHelper.table(prefix, "factions");
    this.membersTable = SqlHelper.table(prefix, "faction_members");
    this.claimsTable = SqlHelper.table(prefix, "faction_claims");
    this.relationsTable = SqlHelper.table(prefix, "faction_relations");
    this.logsTable = SqlHelper.table(prefix, "faction_logs");
    this.gson = new GsonBuilder().disableHtmlEscaping().create();
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
  public CompletableFuture<Optional<Faction>> loadFaction(@NotNull UUID factionId) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        return jdbi.withHandle(handle -> {
          // Load faction row
          Optional<FactionRow> rowOpt = handle.createQuery(
                  "SELECT * FROM " + factionsTable + " WHERE id = :id")
              .bind("id", factionId)
              .map((rs, ctx) -> mapFactionRow(rs))
              .findOne();

          if (rowOpt.isEmpty()) {
            return Optional.<Faction>empty();
          }

          FactionRow row = rowOpt.get();

          // Load related data
          Map<UUID, FactionMember> members = loadMembers(handle, factionId);
          Set<FactionClaim> claims = loadClaims(handle, factionId);
          Map<UUID, FactionRelation> relations = loadRelations(handle, factionId);
          List<FactionLog> logs = loadLogs(handle, factionId);

          return Optional.of(assembleFaction(row, members, claims, relations, logs));
        });
      } catch (Exception e) {
        ErrorHandler.report("Failed to load faction from SQL: " + factionId, e);
        return Optional.empty();
      }
    });
  }

  @Override
  @NotNull
  public CompletableFuture<Void> saveFaction(@NotNull Faction faction) {
    return CompletableFuture.runAsync(() -> {
      try {
        jdbi.useTransaction(handle -> {
          // Upsert faction row
          String permissionsJson = faction.permissions() != null
              ? gson.toJson(faction.permissions().toMap()) : null;

          String upsert = SqlHelper.upsert(dialect, factionsTable,
              new String[]{"id", "name", "description", "tag", "color", "created_at", "open",
                  "home_world", "home_x", "home_y", "home_z", "home_yaw", "home_pitch",
                  "home_set_at", "home_set_by", "hardcore_power", "permissions_json"},
              new String[]{":id", ":name", ":description", ":tag", ":color", ":createdAt", ":open",
                  ":homeWorld", ":homeX", ":homeY", ":homeZ", ":homeYaw", ":homePitch",
                  ":homeSetAt", ":homeSetBy", ":hardcorePower", ":permissionsJson"},
              new String[]{"id"},
              new String[]{"name", "description", "tag", "color", "open",
                  "home_world", "home_x", "home_y", "home_z", "home_yaw", "home_pitch",
                  "home_set_at", "home_set_by", "hardcore_power", "permissions_json"});

          var update = handle.createUpdate(upsert)
              .bind("id", faction.id())
              .bind("name", faction.name())
              .bind("description", faction.description())
              .bind("tag", faction.tag())
              .bind("color", faction.color())
              .bind("createdAt", faction.createdAt())
              .bind("open", faction.open())
              .bind("hardcorePower", faction.hardcorePower())
              .bind("permissionsJson", permissionsJson);

          if (faction.home() != null) {
            update.bind("homeWorld", faction.home().world())
                .bind("homeX", faction.home().x())
                .bind("homeY", faction.home().y())
                .bind("homeZ", faction.home().z())
                .bind("homeYaw", faction.home().yaw())
                .bind("homePitch", faction.home().pitch())
                .bind("homeSetAt", faction.home().setAt())
                .bind("homeSetBy", faction.home().setBy());
          } else {
            update.bind("homeWorld", (String) null)
                .bind("homeX", (Double) null)
                .bind("homeY", (Double) null)
                .bind("homeZ", (Double) null)
                .bind("homeYaw", (Float) null)
                .bind("homePitch", (Float) null)
                .bind("homeSetAt", (Long) null)
                .bind("homeSetBy", (UUID) null);
          }
          update.execute();

          // Replace members
          handle.execute("DELETE FROM " + membersTable + " WHERE faction_id = ?",
              faction.id().toString());
          if (!faction.members().isEmpty()) {
            var batch = handle.prepareBatch(
                "INSERT INTO " + membersTable
                    + " (faction_id, player_uuid, username, role, joined_at, last_online)"
                    + " VALUES (:factionId, :playerUuid, :username, :role, :joinedAt, :lastOnline)");
            for (FactionMember member : faction.members().values()) {
              batch.bind("factionId", faction.id())
                  .bind("playerUuid", member.uuid())
                  .bind("username", member.username())
                  .bind("role", member.role().name())
                  .bind("joinedAt", member.joinedAt())
                  .bind("lastOnline", member.lastOnline())
                  .add();
            }
            batch.execute();
          }

          // Replace claims
          handle.execute("DELETE FROM " + claimsTable + " WHERE faction_id = ?",
              faction.id().toString());
          if (!faction.claims().isEmpty()) {
            var batch = handle.prepareBatch(
                "INSERT INTO " + claimsTable
                    + " (world, chunk_x, chunk_z, faction_id, claimed_at, claimed_by)"
                    + " VALUES (:world, :chunkX, :chunkZ, :factionId, :claimedAt, :claimedBy)");
            for (FactionClaim claim : faction.claims()) {
              batch.bind("world", claim.world())
                  .bind("chunkX", claim.chunkX())
                  .bind("chunkZ", claim.chunkZ())
                  .bind("factionId", faction.id())
                  .bind("claimedAt", claim.claimedAt())
                  .bind("claimedBy", claim.claimedBy())
                  .add();
            }
            batch.execute();
          }

          // Replace relations
          handle.execute("DELETE FROM " + relationsTable + " WHERE faction_id = ?",
              faction.id().toString());
          if (!faction.relations().isEmpty()) {
            var batch = handle.prepareBatch(
                "INSERT INTO " + relationsTable
                    + " (faction_id, target_faction_id, type, since)"
                    + " VALUES (:factionId, :targetFactionId, :type, :since)");
            for (FactionRelation rel : faction.relations().values()) {
              batch.bind("factionId", faction.id())
                  .bind("targetFactionId", rel.targetFactionId())
                  .bind("type", rel.type().name())
                  .bind("since", rel.since())
                  .add();
            }
            batch.execute();
          }

          // Replace logs (capped at MAX_LOGS)
          handle.execute("DELETE FROM " + logsTable + " WHERE faction_id = ?",
              faction.id().toString());
          List<FactionLog> logs = faction.logs();
          int logLimit = Math.min(logs.size(), MAX_LOGS);
          if (logLimit > 0) {
            var batch = handle.prepareBatch(
                "INSERT INTO " + logsTable
                    + " (faction_id, type, message, timestamp, actor_uuid, message_key, message_args)"
                    + " VALUES (:factionId, :type, :message, :timestamp, :actorUuid, :messageKey, :messageArgs)");
            for (int i = 0; i < logLimit; i++) {
              FactionLog log = logs.get(i);
              batch.bind("factionId", faction.id())
                  .bind("type", log.type().name())
                  .bind("message", log.message())
                  .bind("timestamp", log.timestamp())
                  .bind("actorUuid", log.actorUuid())
                  .bind("messageKey", log.messageKey())
                  .bind("messageArgs", log.messageArgs() != null ? gson.toJson(log.messageArgs()) : null)
                  .add();
            }
            batch.execute();
          }
        });
      } catch (Exception e) {
        ErrorHandler.report("Failed to save faction to SQL: " + faction.id(), e);
      }
    });
  }

  @Override
  @NotNull
  public CompletableFuture<Void> deleteFaction(@NotNull UUID factionId) {
    return CompletableFuture.runAsync(() -> {
      try {
        String id = factionId.toString();
        jdbi.useTransaction(handle -> {
          handle.execute("DELETE FROM " + logsTable + " WHERE faction_id = ?", id);
          handle.execute("DELETE FROM " + relationsTable + " WHERE faction_id = ?", id);
          handle.execute("DELETE FROM " + claimsTable + " WHERE faction_id = ?", id);
          handle.execute("DELETE FROM " + membersTable + " WHERE faction_id = ?", id);
          handle.execute("DELETE FROM " + factionsTable + " WHERE id = ?", id);
        });
        Logger.debug("Deleted faction %s from SQL", factionId);
      } catch (Exception e) {
        ErrorHandler.report("Failed to delete faction from SQL: " + factionId, e);
      }
    });
  }

  @Override
  @NotNull
  public CompletableFuture<Collection<Faction>> loadAllFactions() {
    return CompletableFuture.supplyAsync(() -> {
      try {
        return jdbi.withHandle(handle -> {
          // Load all faction rows
          Map<UUID, FactionRow> rows = new LinkedHashMap<>();
          handle.createQuery("SELECT * FROM " + factionsTable)
              .map((rs, ctx) -> {
                FactionRow row = mapFactionRow(rs);
                rows.put(row.id, row);
                return row;
              })
              .list();

          if (rows.isEmpty()) {
            Logger.info("[Storage] Loaded 0 factions from SQL");
            return List.<Faction>of();
          }

          // Batch load all related data
          Map<UUID, Map<UUID, FactionMember>> allMembers = new HashMap<>();
          handle.createQuery("SELECT * FROM " + membersTable)
              .map((rs, ctx) -> {
                UUID factionId = UUID.fromString(rs.getString("faction_id"));
                FactionMember member = new FactionMember(
                    UUID.fromString(rs.getString("player_uuid")),
                    rs.getString("username"),
                    FactionRole.valueOf(rs.getString("role")),
                    rs.getLong("joined_at"),
                    rs.getLong("last_online")
                );
                allMembers.computeIfAbsent(factionId, k -> new HashMap<>())
                    .put(member.uuid(), member);
                return null;
              }).list();

          Map<UUID, Set<FactionClaim>> allClaims = new HashMap<>();
          handle.createQuery("SELECT * FROM " + claimsTable)
              .map((rs, ctx) -> {
                UUID factionId = UUID.fromString(rs.getString("faction_id"));
                FactionClaim claim = new FactionClaim(
                    rs.getString("world"),
                    rs.getInt("chunk_x"),
                    rs.getInt("chunk_z"),
                    rs.getLong("claimed_at"),
                    UUID.fromString(rs.getString("claimed_by"))
                );
                allClaims.computeIfAbsent(factionId, k -> new HashSet<>()).add(claim);
                return null;
              }).list();

          Map<UUID, Map<UUID, FactionRelation>> allRelations = new HashMap<>();
          handle.createQuery("SELECT * FROM " + relationsTable)
              .map((rs, ctx) -> {
                UUID factionId = UUID.fromString(rs.getString("faction_id"));
                FactionRelation rel = new FactionRelation(
                    UUID.fromString(rs.getString("target_faction_id")),
                    RelationType.valueOf(rs.getString("type")),
                    rs.getLong("since")
                );
                allRelations.computeIfAbsent(factionId, k -> new HashMap<>())
                    .put(rel.targetFactionId(), rel);
                return null;
              }).list();

          Map<UUID, List<FactionLog>> allLogs = new HashMap<>();
          handle.createQuery("SELECT * FROM " + logsTable + " ORDER BY timestamp DESC")
              .map((rs, ctx) -> {
                UUID factionId = UUID.fromString(rs.getString("faction_id"));
                String argsJson = rs.getString("message_args");
                List<String> args = argsJson != null
                    ? gson.fromJson(argsJson, new TypeToken<List<String>>(){}.getType())
                    : null;
                FactionLog log = new FactionLog(
                    FactionLog.LogType.valueOf(rs.getString("type")),
                    rs.getString("message"),
                    rs.getLong("timestamp"),
                    rs.getString("actor_uuid") != null
                        ? UUID.fromString(rs.getString("actor_uuid")) : null,
                    rs.getString("message_key"),
                    args
                );
                allLogs.computeIfAbsent(factionId, k -> new ArrayList<>()).add(log);
                return null;
              }).list();

          // Assemble factions
          List<Faction> factions = new ArrayList<>(rows.size());
          for (FactionRow row : rows.values()) {
            factions.add(assembleFaction(
                row,
                allMembers.getOrDefault(row.id, Map.of()),
                allClaims.getOrDefault(row.id, Set.of()),
                allRelations.getOrDefault(row.id, Map.of()),
                allLogs.getOrDefault(row.id, List.of())
            ));
          }

          Logger.info("[Storage] Loaded %d factions from SQL", factions.size());
          return factions;
        });
      } catch (Exception e) {
        ErrorHandler.report("Failed to load all factions from SQL", e);
        return List.of();
      }
    });
  }

  // === Internal helpers ===

  /** Intermediate row holder for faction core data. */
  private record FactionRow(
      UUID id, String name, String description, String tag, String color,
      long createdAt, boolean open, Faction.FactionHome home,
      Double hardcorePower, FactionPermissions permissions
  ) {}

  private FactionRow mapFactionRow(java.sql.ResultSet rs) throws java.sql.SQLException {
    UUID id = UUID.fromString(rs.getString("id"));

    // Parse home
    Faction.FactionHome home = null;
    String homeWorld = rs.getString("home_world");
    if (homeWorld != null) {
      home = new Faction.FactionHome(
          homeWorld,
          rs.getDouble("home_x"),
          rs.getDouble("home_y"),
          rs.getDouble("home_z"),
          rs.getFloat("home_yaw"),
          rs.getFloat("home_pitch"),
          rs.getLong("home_set_at"),
          UUID.fromString(rs.getString("home_set_by"))
      );
    }

    // Parse permissions
    FactionPermissions permissions = null;
    String permissionsJson = rs.getString("permissions_json");
    if (permissionsJson != null) {
      Map<String, Boolean> flags = gson.fromJson(permissionsJson,
          new TypeToken<Map<String, Boolean>>(){}.getType());
      permissions = new FactionPermissions(flags);
    }

    Double hardcorePower = rs.getObject("hardcore_power") != null
        ? rs.getDouble("hardcore_power") : null;

    return new FactionRow(id, rs.getString("name"), rs.getString("description"),
        rs.getString("tag"), rs.getString("color"), rs.getLong("created_at"),
        rs.getBoolean("open"), home, hardcorePower, permissions);
  }

  private Faction assembleFaction(FactionRow row, Map<UUID, FactionMember> members,
                  Set<FactionClaim> claims, Map<UUID, FactionRelation> relations,
                  List<FactionLog> logs) {
    return new Faction(
        row.id, row.name, row.description, row.tag, row.color,
        row.createdAt, row.home, members, claims, relations, logs,
        row.open, row.permissions, row.hardcorePower
    );
  }

  private Map<UUID, FactionMember> loadMembers(org.jdbi.v3.core.Handle handle, UUID factionId) {
    Map<UUID, FactionMember> members = new HashMap<>();
    handle.createQuery("SELECT * FROM " + membersTable + " WHERE faction_id = :factionId")
        .bind("factionId", factionId)
        .map((rs, ctx) -> {
          FactionMember m = new FactionMember(
              UUID.fromString(rs.getString("player_uuid")),
              rs.getString("username"),
              FactionRole.valueOf(rs.getString("role")),
              rs.getLong("joined_at"),
              rs.getLong("last_online")
          );
          members.put(m.uuid(), m);
          return null;
        }).list();
    return members;
  }

  private Set<FactionClaim> loadClaims(org.jdbi.v3.core.Handle handle, UUID factionId) {
    return new HashSet<>(handle.createQuery(
            "SELECT * FROM " + claimsTable + " WHERE faction_id = :factionId")
        .bind("factionId", factionId)
        .map((rs, ctx) -> new FactionClaim(
            rs.getString("world"),
            rs.getInt("chunk_x"),
            rs.getInt("chunk_z"),
            rs.getLong("claimed_at"),
            UUID.fromString(rs.getString("claimed_by"))
        ))
        .list());
  }

  private Map<UUID, FactionRelation> loadRelations(org.jdbi.v3.core.Handle handle, UUID factionId) {
    Map<UUID, FactionRelation> relations = new HashMap<>();
    handle.createQuery("SELECT * FROM " + relationsTable + " WHERE faction_id = :factionId")
        .bind("factionId", factionId)
        .map((rs, ctx) -> {
          FactionRelation r = new FactionRelation(
              UUID.fromString(rs.getString("target_faction_id")),
              RelationType.valueOf(rs.getString("type")),
              rs.getLong("since")
          );
          relations.put(r.targetFactionId(), r);
          return null;
        }).list();
    return relations;
  }

  private List<FactionLog> loadLogs(org.jdbi.v3.core.Handle handle, UUID factionId) {
    return handle.createQuery(
            "SELECT * FROM " + logsTable + " WHERE faction_id = :factionId ORDER BY timestamp DESC")
        .bind("factionId", factionId)
        .map((rs, ctx) -> {
          String argsJson = rs.getString("message_args");
          List<String> args = argsJson != null
              ? gson.fromJson(argsJson, new TypeToken<List<String>>(){}.getType())
              : null;
          return new FactionLog(
              FactionLog.LogType.valueOf(rs.getString("type")),
              rs.getString("message"),
              rs.getLong("timestamp"),
              rs.getString("actor_uuid") != null
                  ? UUID.fromString(rs.getString("actor_uuid")) : null,
              rs.getString("message_key"),
              args
          );
        })
        .list();
  }
}
