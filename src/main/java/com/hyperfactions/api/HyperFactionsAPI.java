package com.hyperfactions.api;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.api.events.EventBus;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.PlayerPower;
import com.hyperfactions.data.RelationType;
import com.hyperfactions.manager.*;
import com.hyperfactions.protection.ProtectionChecker;
import com.hyperfactions.util.HFMessages;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Public API for HyperFactions.
 * Provides safe access to faction data and operations.
 */
public final class HyperFactionsAPI {

  private static HyperFactions instance;

  private HyperFactionsAPI() {}

  /**
   * Sets the HyperFactions instance.
   * Called by the plugin on enable/disable.
   *
   * @param hyperFactions the instance, or null to clear
   */
  public static void setInstance(@Nullable HyperFactions hyperFactions) {
    instance = hyperFactions;
  }

  /**
   * Gets the HyperFactions instance.
   *
   * @return the instance
   * @throws IllegalStateException if not initialized
   */
  @NotNull
  public static HyperFactions getInstance() {
    if (instance == null) {
      throw new IllegalStateException("HyperFactions is not initialized");
    }
    return instance;
  }

  /**
   * Checks if HyperFactions is available.
   *
   * @return true if available
   */
  public static boolean isAvailable() {
    return instance != null;
  }

  // === Faction Queries ===

  /**
   * Gets a faction by ID.
   *
   * @param factionId the faction ID
   * @return the faction, or null if not found
   */
  @Nullable
  public static Faction getFaction(@NotNull UUID factionId) {
    return getInstance().getFactionManager().getFaction(factionId);
  }

  /**
   * Gets a faction by name.
   *
   * @param name the faction name
   * @return the faction, or null if not found
   */
  @Nullable
  public static Faction getFactionByName(@NotNull String name) {
    return getInstance().getFactionManager().getFactionByName(name);
  }

  /**
   * Gets a player's faction.
   *
   * @param playerUuid the player's UUID
   * @return the faction, or null if not in one
   */
  @Nullable
  public static Faction getPlayerFaction(@NotNull UUID playerUuid) {
    return getInstance().getFactionManager().getPlayerFaction(playerUuid);
  }

  /**
   * Checks if a player is in any faction.
   *
   * @param playerUuid the player's UUID
   * @return true if in a faction
   */
  public static boolean isInFaction(@NotNull UUID playerUuid) {
    return getInstance().getFactionManager().isInFaction(playerUuid);
  }

  /**
   * Gets all factions.
   *
   * @return collection of all factions
   */
  @NotNull
  public static Collection<Faction> getAllFactions() {
    return getInstance().getFactionManager().getAllFactions();
  }

  // === Power ===

  /**
   * Gets a player's power data.
   *
   * @param playerUuid the player's UUID
   * @return the player power
   */
  @NotNull
  public static PlayerPower getPlayerPower(@NotNull UUID playerUuid) {
    return getInstance().getPowerManager().getPlayerPower(playerUuid);
  }

  /**
   * Gets a faction's total power.
   *
   * @param factionId the faction ID
   * @return the total power
   */
  public static double getFactionPower(@NotNull UUID factionId) {
    return getInstance().getPowerManager().getFactionPower(factionId);
  }

  // === Claims ===

  /**
   * Gets the faction that owns a chunk.
   *
   * @param world  the world name
   * @param chunkX the chunk X
   * @param chunkZ the chunk Z
   * @return the faction ID, or null if unclaimed
   */
  @Nullable
  public static UUID getClaimOwner(@NotNull String world, int chunkX, int chunkZ) {
    return getInstance().getClaimManager().getClaimOwner(world, chunkX, chunkZ);
  }

  /**
   * Checks if a chunk is claimed.
   *
   * @param world  the world name
   * @param chunkX the chunk X
   * @param chunkZ the chunk Z
   * @return true if claimed
   */
  public static boolean isClaimed(@NotNull String world, int chunkX, int chunkZ) {
    return getInstance().getClaimManager().isClaimed(world, chunkX, chunkZ);
  }

  // === Relations ===

  /**
   * Gets the relation between two factions.
   *
   * @param factionId1 first faction ID
   * @param factionId2 second faction ID
   * @return the relation type
   */
  @NotNull
  public static RelationType getRelation(@NotNull UUID factionId1, @NotNull UUID factionId2) {
    return getInstance().getRelationManager().getRelation(factionId1, factionId2);
  }

  /**
   * Checks if two factions are allies.
   *
   * @param factionId1 first faction ID
   * @param factionId2 second faction ID
   * @return true if allies
   */
  public static boolean areAllies(@NotNull UUID factionId1, @NotNull UUID factionId2) {
    return getInstance().getRelationManager().areAllies(factionId1, factionId2);
  }

  /**
   * Checks if two factions are enemies.
   *
   * @param factionId1 first faction ID
   * @param factionId2 second faction ID
   * @return true if enemies
   */
  public static boolean areEnemies(@NotNull UUID factionId1, @NotNull UUID factionId2) {
    return getInstance().getRelationManager().areEnemies(factionId1, factionId2);
  }

  // === Zones ===

  /**
   * Checks if a location is in a SafeZone.
   *
   * @param world  the world name
   * @param chunkX the chunk X
   * @param chunkZ the chunk Z
   * @return true if in SafeZone
   */
  public static boolean isInSafeZone(@NotNull String world, int chunkX, int chunkZ) {
    return getInstance().getZoneManager().isInSafeZone(world, chunkX, chunkZ);
  }

  /**
   * Checks if a location is in a WarZone.
   *
   * @param world  the world name
   * @param chunkX the chunk X
   * @param chunkZ the chunk Z
   * @return true if in WarZone
   */
  public static boolean isInWarZone(@NotNull String world, int chunkX, int chunkZ) {
    return getInstance().getZoneManager().isInWarZone(world, chunkX, chunkZ);
  }

  // === Combat ===

  /**
   * Checks if a player is combat tagged.
   *
   * @param playerUuid the player's UUID
   * @return true if tagged
   */
  public static boolean isCombatTagged(@NotNull UUID playerUuid) {
    return getInstance().getCombatTagManager().isTagged(playerUuid);
  }

  // === Protection ===

  /**
   * Gets the protection checker.
   *
   * @return the protection checker
   */
  @NotNull
  public static ProtectionChecker getProtectionChecker() {
    return getInstance().getProtectionChecker();
  }

  /**
   * Checks if a player can build at a location.
   *
   * @param playerUuid the player's UUID
   * @param world      the world name
   * @param x          the X coordinate
   * @param z          the Z coordinate
   * @return true if allowed
   */
  public static boolean canBuild(@NotNull UUID playerUuid, @NotNull String world, double x, double z) {
    return getInstance().getProtectionChecker().canBuild(playerUuid, world, x, z);
  }

  // === Manager Access ===

  /**
   * Gets the FactionManager for advanced operations.
   *
   * @return the faction manager
   */
  @NotNull
  public static FactionManager getFactionManager() {
    return getInstance().getFactionManager();
  }

  /**
   * Gets the ClaimManager for territory operations.
   *
   * @return the claim manager
   */
  @NotNull
  public static ClaimManager getClaimManager() {
    return getInstance().getClaimManager();
  }

  /**
   * Gets the PowerManager for power operations.
   *
   * @return the power manager
   */
  @NotNull
  public static PowerManager getPowerManager() {
    return getInstance().getPowerManager();
  }

  /**
   * Gets the RelationManager for alliance/enemy operations.
   *
   * @return the relation manager
   */
  @NotNull
  public static RelationManager getRelationManager() {
    return getInstance().getRelationManager();
  }

  /**
   * Gets the ZoneManager for SafeZone/WarZone operations.
   *
   * @return the zone manager
   */
  @NotNull
  public static ZoneManager getZoneManager() {
    return getInstance().getZoneManager();
  }

  /**
   * Gets the CombatTagManager for combat tag operations.
   *
   * @return the combat tag manager
   */
  @NotNull
  public static CombatTagManager getCombatTagManager() {
    return getInstance().getCombatTagManager();
  }

  /**
   * Gets the TeleportManager for teleport operations.
   *
   * @return the teleport manager
   */
  @NotNull
  public static TeleportManager getTeleportManager() {
    return getInstance().getTeleportManager();
  }

  /**
   * Gets the InviteManager for invite operations.
   *
   * @return the invite manager
   */
  @NotNull
  public static InviteManager getInviteManager() {
    return getInstance().getInviteManager();
  }

  // === Event System ===

  /**
   * Gets the EventBus for subscribing to faction events.
   *
   * @return the EventBus class for static access
   */
  @NotNull
  public static Class<EventBus> getEventBus() {
    return EventBus.class;
  }

  /**
   * Registers an event listener.
   * Convenience method that delegates to EventBus.
   *
   * @param eventClass the event class
   * @param listener   the listener
   * @param {@code <T>}        the event type
   */
  public static <T> void registerEventListener(@NotNull Class<T> eventClass, 
                         @NotNull java.util.function.Consumer<T> listener) {
    EventBus.register(eventClass, listener);
  }

  /**
   * Unregisters an event listener.
   * Convenience method that delegates to EventBus.
   *
   * @param eventClass the event class
   * @param listener   the listener
   * @param {@code <T>}        the event type
   */
  public static <T> void unregisterEventListener(@NotNull Class<T> eventClass,
                          @NotNull java.util.function.Consumer<T> listener) {
    EventBus.unregister(eventClass, listener);
  }

  // === Language / i18n ===

  /**
   * Sets the language for a player with immediate effect and persistence.
   * The change takes effect immediately for all subsequent messages and is
   * saved to PlayerData for persistence across sessions.
   *
   * @param playerUuid the player's UUID
   * @param locale     the locale code (e.g. "pl-PL", "en-US")
   * @throws IllegalArgumentException if locale is null, empty, or not in {@link #getSupportedLocales()}
   * @see #getSupportedLocales()
   */
  public static void setPlayerLanguage(@NotNull UUID playerUuid, @NotNull String locale) {
    if (locale.isEmpty()) {
      throw new IllegalArgumentException("Locale cannot be empty");
    }
    if (!HFMessages.isLocaleSupported(locale)) {
      throw new IllegalArgumentException("Unsupported locale: " + locale
          + ". Supported: " + HFMessages.getSupportedLocales());
    }
    // Immediate in-memory effect
    HFMessages.setLanguageOverride(playerUuid, locale);
    // Persist to PlayerData asynchronously
    getInstance().getPlayerStorage().updatePlayerData(playerUuid, data ->
        data.setLanguagePreference(locale));
  }

  /**
   * Gets the current language preference for a player.
   * Returns the explicitly-set preference (via API or player settings), or the
   * server default language if no preference is set.
   *
   * <p>Note: For online players who haven't set a preference but have
   * {@code usePlayerLanguage=true} in config, the actual message language may
   * differ (resolved from client language). This method returns the stored
   * preference only, not the fully-resolved effective language.
   *
   * @param playerUuid the player's UUID
   * @return the language preference or server default (e.g. "en-US")
   */
  @NotNull
  public static String getPlayerLanguage(@NotNull UUID playerUuid) {
    return HFMessages.getLanguageForUuid(playerUuid);
  }

  /**
   * Returns the set of locale codes supported by HyperFactions.
   * External plugins can use this to validate locale codes before calling
   * {@link #setPlayerLanguage(UUID, String)}.
   *
   * @return unmodifiable set of locale codes (e.g. {"en-US", "pl-PL", "de-DE", ...})
   */
  @NotNull
  public static Set<String> getSupportedLocales() {
    return HFMessages.getSupportedLocales();
  }
}
