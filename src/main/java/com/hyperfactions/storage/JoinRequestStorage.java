package com.hyperfactions.storage;

import com.hyperfactions.data.JoinRequest;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for join request data persistence.
 */
public interface JoinRequestStorage {

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
   * Loads all non-expired join requests from storage.
   *
   * @return a future containing all join requests
   */
  CompletableFuture<List<JoinRequest>> loadAll();

  /**
   * Saves all join requests to storage, replacing any existing data.
   *
   * @param requests the join requests to save
   * @return a future that completes when saving is done
   */
  CompletableFuture<Void> saveAll(Collection<JoinRequest> requests);
}
