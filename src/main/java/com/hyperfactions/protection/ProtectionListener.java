package com.hyperfactions.protection;

import com.hyperfactions.HyperFactions;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Handles protection-related events for HyperFactions.
 */
public class ProtectionListener {

  private final HyperFactions hyperFactions;

  /** Creates a new ProtectionListener. */
  public ProtectionListener(@NotNull HyperFactions hyperFactions) {
    this.hyperFactions = hyperFactions;
  }

  /**
   * Called when a player attempts to place a block.
   *
   * @param playerUuid the player's UUID
   * @param world      the world name
   * @param x          the block X
   * @param y          the block Y
   * @param z          the block Z
   * @return true if block placement should be cancelled
   */
  public boolean onBlockPlace(@NotNull UUID playerUuid, @NotNull String world,
                int x, int y, int z) {
    ProtectionChecker checker = hyperFactions.getProtectionChecker();
    ProtectionChecker.ProtectionResult result = checker.canInteract(
      playerUuid, world, x, z, ProtectionChecker.InteractionType.BUILD
    );

    return !checker.isAllowed(result);
  }

  /**
   * Called when a player attempts to break a block.
   *
   * @param playerUuid the player's UUID
   * @param world      the world name
   * @param x          the block X
   * @param y          the block Y
   * @param z          the block Z
   * @return true if block break should be cancelled
   */
  public boolean onBlockBreak(@NotNull UUID playerUuid, @NotNull String world,
                int x, int y, int z) {
    ProtectionChecker checker = hyperFactions.getProtectionChecker();
    ProtectionChecker.ProtectionResult result = checker.canInteract(
      playerUuid, world, x, z, ProtectionChecker.InteractionType.BUILD
    );

    return !checker.isAllowed(result);
  }

  /**
   * Called when a player attempts to interact with a block (doors, buttons, etc).
   *
   * @param playerUuid the player's UUID
   * @param world      the world name
   * @param x          the block X
   * @param y          the block Y
   * @param z          the block Z
   * @return true if interaction should be cancelled
   */
  public boolean onBlockInteract(@NotNull UUID playerUuid, @NotNull String world,
                 int x, int y, int z) {
    ProtectionChecker checker = hyperFactions.getProtectionChecker();
    ProtectionChecker.ProtectionResult result = checker.canInteract(
      playerUuid, world, x, z, ProtectionChecker.InteractionType.INTERACT
    );

    return !checker.isAllowed(result);
  }

  /**
   * Called when a player attempts to open a container.
   *
   * @param playerUuid the player's UUID
   * @param world      the world name
   * @param x          the container X
   * @param y          the container Y
   * @param z          the container Z
   * @return true if container access should be cancelled
   */
  public boolean onContainerAccess(@NotNull UUID playerUuid, @NotNull String world,
                  int x, int y, int z) {
    ProtectionChecker checker = hyperFactions.getProtectionChecker();
    ProtectionChecker.ProtectionResult result = checker.canInteract(
      playerUuid, world, x, z, ProtectionChecker.InteractionType.CONTAINER
    );

    return !checker.isAllowed(result);
  }

  /**
   * Called when a player attempts to pick up an item.
   *
   * @param playerUuid the player's UUID
   * @param world      the world name
   * @param x          the player's X position
   * @param y          the player's Y position
   * @param z          the player's Z position
   * @return true if item pickup should be cancelled
   */
  public boolean onItemPickup(@NotNull UUID playerUuid, @NotNull String world,
                int x, int y, int z) {
    ProtectionChecker checker = hyperFactions.getProtectionChecker();
    ProtectionChecker.ProtectionResult result = checker.canInteract(
      playerUuid, world, x, z, ProtectionChecker.InteractionType.INTERACT
    );

    return !checker.isAllowed(result);
  }

  /**
   * Gets the denial message for the last failed protection check.
   *
   * @param result the protection result
   * @return the denial message
   */
  @NotNull
  public String getDenialMessage(@NotNull ProtectionChecker.ProtectionResult result) {
    return hyperFactions.getProtectionChecker().getDenialMessage(result);
  }

  /**
   * Gets a denial message with specific action context.
   *
   * @param result the protection result
   * @param type   the interaction type for specific messaging
   * @return the denial message
   */
  @NotNull
  public String getDenialMessage(@NotNull ProtectionChecker.ProtectionResult result,
                  @NotNull ProtectionChecker.InteractionType type) {
    return hyperFactions.getProtectionChecker().getDenialMessage(result, type);
  }

  /**
   * Gets the denial message for a PvP result.
   *
   * @param result the PvP result
   * @return the denial message
   */
  @NotNull
  public String getDenialMessage(@NotNull ProtectionChecker.PvPResult result) {
    return hyperFactions.getProtectionChecker().getDenialMessage(result);
  }
}
