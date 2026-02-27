package com.hyperfactions.storage;

import com.hyperfactions.data.FactionChatHistory;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for faction chat history persistence.
 */
public interface ChatHistoryStorage {

  /**
   * Initializes the storage provider.
   *
   * @return a future that completes when initialization is done
   */
  CompletableFuture<Void> init();

  /**
   * Shuts down the storage provider.
   *
   * @return a future that completes when shutdown is done
   */
  CompletableFuture<Void> shutdown();

  /**
   * Loads chat history for a faction.
   *
   * @param factionId the faction's UUID
   * @return a future containing the chat history (empty history if none exists)
   */
  CompletableFuture<FactionChatHistory> loadHistory(@NotNull UUID factionId);

  /**
   * Saves chat history for a faction.
   *
   * @param history the chat history to save
   * @return a future that completes when saving is done
   */
  CompletableFuture<Void> saveHistory(@NotNull FactionChatHistory history);

  /**
   * Deletes chat history for a faction.
   *
   * @param factionId the faction's UUID
   * @return a future that completes when deletion is done
   */
  CompletableFuture<Void> deleteHistory(@NotNull UUID factionId);

  /**
   * Lists all faction IDs that have stored chat history.
   * Used for retention scanning.
   *
   * @return a future containing all faction IDs with history
   */
  CompletableFuture<List<UUID>> listAllFactionIds();
}
