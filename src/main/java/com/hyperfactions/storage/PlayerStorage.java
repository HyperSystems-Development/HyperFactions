package com.hyperfactions.storage;

import com.hyperfactions.data.PlayerData;
import com.hyperfactions.data.PlayerPower;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for player power data persistence.
 */
public interface PlayerStorage {

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
   * Loads player power data.
   *
   * @param uuid the player's UUID
   * @return a future containing the player power if found
   */
  CompletableFuture<Optional<PlayerPower>> loadPlayerPower(@NotNull UUID uuid);

  /**
   * Saves player power data.
   *
   * @param power the player power to save
   * @return a future that completes when saving is done
   */
  CompletableFuture<Void> savePlayerPower(@NotNull PlayerPower power);

  /**
   * Deletes player power data.
   *
   * @param uuid the player's UUID
   * @return a future that completes when deletion is done
   */
  CompletableFuture<Void> deletePlayerPower(@NotNull UUID uuid);

  /**
   * Loads all player power data.
   *
   * @return a future containing all player power data
   */
  CompletableFuture<Collection<PlayerPower>> loadAllPlayerPower();

  /**
   * Gets all known player UUIDs from storage.
   *
   * @return a future containing the set of all stored player UUIDs
   */
  CompletableFuture<Set<UUID>> getAllPlayerUuids();

  /**
   * Loads full player data (power + history + stats).
   *
   * @param uuid the player's UUID
   * @return a future containing the player data if found
   */
  CompletableFuture<Optional<PlayerData>> loadPlayerData(@NotNull UUID uuid);

  /**
   * Saves full player data (power + history + stats).
   *
   * @param data the player data to save
   * @return a future that completes when saving is done
   */
  CompletableFuture<Void> savePlayerData(@NotNull PlayerData data);

  /**
   * Atomically loads player data, applies the updater, and saves.
   * Thread-safe: concurrent updates to the same player are serialized.
   *
   * @param uuid    the player's UUID
   * @param updater a consumer that modifies the player data in place
   * @return a future that completes when the update is saved
   */
  CompletableFuture<Void> updatePlayerData(@NotNull UUID uuid, @NotNull Consumer<PlayerData> updater);
}
