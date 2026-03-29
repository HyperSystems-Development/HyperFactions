package com.hyperfactions.storage.sql;

import com.hyperfactions.data.PendingInvite;
import com.hyperfactions.storage.InviteStorage;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.jdbi.v3.core.Jdbi;
import org.jetbrains.annotations.NotNull;

/**
 * SQL implementation of {@link InviteStorage}.
 */
public class SqlInviteStorage implements InviteStorage {

  private final Jdbi jdbi;
  private final String table;

  public SqlInviteStorage(@NotNull Jdbi jdbi, @NotNull String prefix) {
    this.jdbi = jdbi;
    this.table = SqlHelper.table(prefix, "invites");
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
  public CompletableFuture<List<PendingInvite>> loadAll() {
    return CompletableFuture.supplyAsync(() -> {
      try {
        long now = System.currentTimeMillis();
        List<PendingInvite> invites = jdbi.withHandle(handle ->
            handle.createQuery("SELECT * FROM " + table + " WHERE expires_at > :now")
                .bind("now", now)
                .map((rs, ctx) -> new PendingInvite(
                    UUID.fromString(rs.getString("faction_id")),
                    UUID.fromString(rs.getString("player_uuid")),
                    UUID.fromString(rs.getString("invited_by")),
                    rs.getLong("created_at"),
                    rs.getLong("expires_at")
                ))
                .list()
        );
        Logger.info("[Storage] Loaded %d invites from SQL", invites.size());
        return invites;
      } catch (Exception e) {
        ErrorHandler.report("Failed to load invites from SQL", e);
        return List.of();
      }
    });
  }

  @Override
  public CompletableFuture<Void> saveAll(Collection<PendingInvite> invites) {
    return CompletableFuture.runAsync(() -> {
      try {
        jdbi.useTransaction(handle -> {
          handle.execute("DELETE FROM " + table);
          if (!invites.isEmpty()) {
            var batch = handle.prepareBatch(
                "INSERT INTO " + table
                    + " (faction_id, player_uuid, invited_by, created_at, expires_at)"
                    + " VALUES (:factionId, :playerUuid, :invitedBy, :createdAt, :expiresAt)");
            for (PendingInvite invite : invites) {
              batch
                  .bind("factionId", invite.factionId())
                  .bind("playerUuid", invite.playerUuid())
                  .bind("invitedBy", invite.invitedBy())
                  .bind("createdAt", invite.createdAt())
                  .bind("expiresAt", invite.expiresAt())
                  .add();
            }
            batch.execute();
          }
        });
      } catch (Exception e) {
        ErrorHandler.report("Failed to save invites to SQL", e);
      }
    });
  }
}
