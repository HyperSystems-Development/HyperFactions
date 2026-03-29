package com.hyperfactions.storage.sql;

import com.hyperfactions.data.ChatMessage;
import com.hyperfactions.data.FactionChatHistory;
import com.hyperfactions.storage.ChatHistoryStorage;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.jdbi.v3.core.Jdbi;
import org.jetbrains.annotations.NotNull;

/**
 * SQL implementation of {@link ChatHistoryStorage}.
 */
public class SqlChatHistoryStorage implements ChatHistoryStorage {

  private final Jdbi jdbi;
  private final String table;

  public SqlChatHistoryStorage(@NotNull Jdbi jdbi, @NotNull String prefix) {
    this.jdbi = jdbi;
    this.table = SqlHelper.table(prefix, "chat_messages");
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
  public CompletableFuture<FactionChatHistory> loadHistory(@NotNull UUID factionId) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        List<ChatMessage> messages = jdbi.withHandle(handle ->
            handle.createQuery("SELECT * FROM " + table
                    + " WHERE faction_id = :factionId ORDER BY timestamp DESC")
                .bind("factionId", factionId)
                .map((rs, ctx) -> new ChatMessage(
                    UUID.fromString(rs.getString("player_uuid")),
                    rs.getString("player_name"),
                    rs.getString("player_faction_tag"),
                    ChatMessage.Channel.valueOf(rs.getString("channel")),
                    rs.getString("message"),
                    rs.getLong("timestamp")
                ))
                .list()
        );
        return new FactionChatHistory(factionId, messages);
      } catch (Exception e) {
        ErrorHandler.report("Failed to load chat history from SQL for faction " + factionId, e);
        return FactionChatHistory.empty(factionId);
      }
    });
  }

  @Override
  @NotNull
  public CompletableFuture<Void> saveHistory(@NotNull FactionChatHistory history) {
    return CompletableFuture.runAsync(() -> {
      try {
        jdbi.useTransaction(handle -> {
          // Delete existing and re-insert (simple approach for chat history)
          handle.execute("DELETE FROM " + table + " WHERE faction_id = ?",
              history.factionId().toString());

          if (!history.messages().isEmpty()) {
            var batch = handle.prepareBatch(
                "INSERT INTO " + table
                    + " (faction_id, player_uuid, player_name, player_faction_tag, channel, message, timestamp)"
                    + " VALUES (:factionId, :playerUuid, :playerName, :playerFactionTag, :channel, :message, :timestamp)");
            for (ChatMessage msg : history.messages()) {
              batch
                  .bind("factionId", history.factionId())
                  .bind("playerUuid", msg.senderId())
                  .bind("playerName", msg.senderName())
                  .bind("playerFactionTag", msg.senderFactionTag())
                  .bind("channel", msg.channel().name())
                  .bind("message", msg.message())
                  .bind("timestamp", msg.timestamp())
                  .add();
            }
            batch.execute();
          }
        });
      } catch (Exception e) {
        ErrorHandler.report("Failed to save chat history to SQL for faction " + history.factionId(), e);
      }
    });
  }

  @Override
  @NotNull
  public CompletableFuture<Void> deleteHistory(@NotNull UUID factionId) {
    return CompletableFuture.runAsync(() -> {
      try {
        jdbi.useHandle(handle ->
            handle.execute("DELETE FROM " + table + " WHERE faction_id = ?",
                factionId.toString()));
      } catch (Exception e) {
        ErrorHandler.report("Failed to delete chat history from SQL for faction " + factionId, e);
      }
    });
  }

  @Override
  @NotNull
  public CompletableFuture<List<UUID>> listAllFactionIds() {
    return CompletableFuture.supplyAsync(() -> {
      try {
        return jdbi.withHandle(handle ->
            handle.createQuery("SELECT DISTINCT faction_id FROM " + table)
                .map((rs, ctx) -> UUID.fromString(rs.getString("faction_id")))
                .list()
        );
      } catch (Exception e) {
        ErrorHandler.report("Failed to list faction IDs from SQL chat history", e);
        return List.of();
      }
    });
  }
}
