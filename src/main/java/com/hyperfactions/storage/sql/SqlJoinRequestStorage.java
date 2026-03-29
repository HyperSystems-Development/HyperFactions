package com.hyperfactions.storage.sql;

import com.hyperfactions.data.JoinRequest;
import com.hyperfactions.storage.JoinRequestStorage;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.jdbi.v3.core.Jdbi;
import org.jetbrains.annotations.NotNull;

/**
 * SQL implementation of {@link JoinRequestStorage}.
 */
public class SqlJoinRequestStorage implements JoinRequestStorage {

  private final Jdbi jdbi;
  private final String table;

  public SqlJoinRequestStorage(@NotNull Jdbi jdbi, @NotNull String prefix) {
    this.jdbi = jdbi;
    this.table = SqlHelper.table(prefix, "join_requests");
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
  public CompletableFuture<List<JoinRequest>> loadAll() {
    return CompletableFuture.supplyAsync(() -> {
      try {
        long now = System.currentTimeMillis();
        List<JoinRequest> requests = jdbi.withHandle(handle ->
            handle.createQuery("SELECT * FROM " + table + " WHERE expires_at > :now")
                .bind("now", now)
                .map((rs, ctx) -> new JoinRequest(
                    UUID.fromString(rs.getString("faction_id")),
                    UUID.fromString(rs.getString("player_uuid")),
                    rs.getString("player_name"),
                    rs.getString("message"),
                    rs.getLong("created_at"),
                    rs.getLong("expires_at")
                ))
                .list()
        );
        Logger.info("[Storage] Loaded %d join requests from SQL", requests.size());
        return requests;
      } catch (Exception e) {
        ErrorHandler.report("Failed to load join requests from SQL", e);
        return List.of();
      }
    });
  }

  @Override
  public CompletableFuture<Void> saveAll(Collection<JoinRequest> requests) {
    return CompletableFuture.runAsync(() -> {
      try {
        jdbi.useTransaction(handle -> {
          handle.execute("DELETE FROM " + table);
          if (!requests.isEmpty()) {
            var batch = handle.prepareBatch(
                "INSERT INTO " + table
                    + " (faction_id, player_uuid, player_name, message, created_at, expires_at)"
                    + " VALUES (:factionId, :playerUuid, :playerName, :message, :createdAt, :expiresAt)");
            for (JoinRequest request : requests) {
              batch
                  .bind("factionId", request.factionId())
                  .bind("playerUuid", request.playerUuid())
                  .bind("playerName", request.playerName())
                  .bind("message", request.message())
                  .bind("createdAt", request.createdAt())
                  .bind("expiresAt", request.expiresAt())
                  .add();
            }
            batch.execute();
          }
        });
      } catch (Exception e) {
        ErrorHandler.report("Failed to save join requests to SQL", e);
      }
    });
  }
}
