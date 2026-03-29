package com.hyperfactions.storage;

import com.hyperfactions.data.FactionEconomy;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for faction economy data persistence.
 */
public interface EconomyStorage {

  /**
   * Initializes the storage provider.
   *
   * @return a future that completes when initialization is done
   */
  CompletableFuture<Void> init();

  /**
   * Loads all economy data from storage.
   *
   * @return a future containing a map of faction ID to economy data
   */
  CompletableFuture<Map<UUID, FactionEconomy>> loadAll();

  /**
   * Saves a single faction's economy data.
   *
   * @param factionId the faction ID
   * @param economy   the economy data to save
   * @return a future that completes when saving is done
   */
  CompletableFuture<Void> save(@NotNull UUID factionId, @NotNull FactionEconomy economy);

  /**
   * Saves all economy data.
   *
   * @param economies map of faction ID to economy data
   * @return a future that completes when saving is done
   */
  CompletableFuture<Void> saveAll(@NotNull Map<UUID, FactionEconomy> economies);

  /**
   * Deletes economy data for a faction.
   *
   * @param factionId the faction ID
   * @return a future that completes when deletion is done
   */
  CompletableFuture<Void> delete(@NotNull UUID factionId);
}
