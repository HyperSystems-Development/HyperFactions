package com.hyperfactions.storage;

import com.hyperfactions.data.Faction;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for faction data persistence.
 */
public interface FactionStorage {

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
   * Loads a faction by its ID.
   *
   * @param factionId the faction's UUID
   * @return a future containing the faction if found
   */
  CompletableFuture<Optional<Faction>> loadFaction(@NotNull UUID factionId);

  /**
   * Saves a faction to storage.
   *
   * @param faction the faction to save
   * @return a future that completes when saving is done
   */
  CompletableFuture<Void> saveFaction(@NotNull Faction faction);

  /**
   * Deletes a faction from storage.
   *
   * @param factionId the faction's UUID
   * @return a future that completes when deletion is done
   */
  CompletableFuture<Void> deleteFaction(@NotNull UUID factionId);

  /**
   * Loads all factions from storage.
   *
   * @return a future containing all factions
   */
  CompletableFuture<Collection<Faction>> loadAllFactions();
}
