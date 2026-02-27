package com.hyperfactions.integration;

import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface for permission providers.
 * Allows HyperFactions to integrate with multiple permission systems
 * (VaultUnlocked, HyperPerms, LuckPerms) with a unified API.
 */
public interface PermissionProvider {

  /**
   * Gets the name of this permission provider.
   *
   * @return the provider name (e.g., "VaultUnlocked", "HyperPerms", "LuckPerms")
   */
  @NotNull
  String getName();

  /**
   * Checks if this provider is available and initialized.
   *
   * @return true if the provider is available and can handle permission checks
   */
  boolean isAvailable();

  /**
   * Checks if a player has a specific permission.
   *
   * @param playerUuid the player's UUID
   * @param permission the permission node to check
   * @return Optional containing true/false if the provider can answer,
   *         or empty if the provider cannot determine (e.g., player not found)
   */
  @NotNull
  Optional<Boolean> hasPermission(@NotNull UUID playerUuid, @NotNull String permission);

  /**
   * Gets the player's chat prefix.
   *
   * @param playerUuid the player's UUID
   * @param worldName  the world name for context (may be null for global)
   * @return the prefix string, or null if not set or unavailable
   */
  @Nullable
  String getPrefix(@NotNull UUID playerUuid, @Nullable String worldName);

  /**
   * Gets the player's chat suffix.
   *
   * @param playerUuid the player's UUID
   * @param worldName  the world name for context (may be null for global)
   * @return the suffix string, or null if not set or unavailable
   */
  @Nullable
  String getSuffix(@NotNull UUID playerUuid, @Nullable String worldName);

  /**
   * Gets the player's primary group name.
   *
   * @param playerUuid the player's UUID
   * @return the primary group name, or "default" if not found
   */
  @NotNull
  String getPrimaryGroup(@NotNull UUID playerUuid);
}
