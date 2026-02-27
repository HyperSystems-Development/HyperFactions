package com.hyperfactions.storage;

import com.hyperfactions.data.Zone;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for zone data persistence.
 */
public interface ZoneStorage {

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
   * Loads all zones from storage.
   *
   * @return a future containing all zones
   */
  CompletableFuture<Collection<Zone>> loadAllZones();

  /**
   * Saves all zones to storage.
   *
   * @param zones the zones to save
   * @return a future that completes when saving is done
   */
  CompletableFuture<Void> saveAllZones(@NotNull Collection<Zone> zones);
}
