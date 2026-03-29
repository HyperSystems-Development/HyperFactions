package com.hyperfactions.storage;

import com.hyperfactions.data.PendingInvite;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for pending invite data persistence.
 */
public interface InviteStorage {

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
   * Loads all non-expired invites from storage.
   *
   * @return a future containing all invites
   */
  CompletableFuture<List<PendingInvite>> loadAll();

  /**
   * Saves all invites to storage, replacing any existing data.
   *
   * @param invites the invites to save
   * @return a future that completes when saving is done
   */
  CompletableFuture<Void> saveAll(Collection<PendingInvite> invites);
}
