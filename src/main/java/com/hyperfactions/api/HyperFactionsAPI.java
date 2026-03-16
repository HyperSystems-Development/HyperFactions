package com.hyperfactions.api;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.api.events.EventBus;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.PlayerPower;
import com.hyperfactions.data.RelationType;
import com.hyperfactions.manager.*;
import com.hyperfactions.protection.ProtectionChecker;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.config.modules.ChatConfig;
import com.hyperfactions.util.HFMessages;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
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

  // === Chat Color Customization ===

  /** Hex color pattern: #RRGGBB (6 digits). */
  private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#[0-9A-Fa-f]{6}$");

  /** Valid keys for {@link #setChatColors(Map)} and {@link #getChatColors()}. */
  private static final Set<String> CHAT_COLOR_KEYS = Set.of(
      "relationOwn", "relationAlly", "relationNeutral", "relationEnemy",
      "prefixColor", "prefixBracketColor",
      "playerNameColor", "senderNameColor", "messageColor",
      "factionChatColor", "allyChatColor",
      "noFactionTagColor"
  );

  private static void validateHexColor(@NotNull String hexColor, @NotNull String paramName) {
    if (!HEX_COLOR_PATTERN.matcher(hexColor).matches()) {
      throw new IllegalArgumentException(paramName + " must be a valid hex color (#RRGGBB), got: " + hexColor);
    }
  }

  /**
   * Sets a chat relation color by relation name.
   * Takes effect immediately for all subsequent chat messages.
   *
   * @param relation "OWN", "ALLY", "NEUTRAL", or "ENEMY" (case-insensitive)
   * @param hexColor hex color string in #RRGGBB format (e.g. "#4ade80")
   * @throws IllegalArgumentException if relation is unknown or hexColor is invalid
   */
  public static void setChatRelationColor(@NotNull String relation, @NotNull String hexColor) {
    validateHexColor(hexColor, "hexColor");
    ChatConfig chat = ConfigManager.get().chat();
    switch (relation.toUpperCase()) {
      case "OWN" -> chat.setRelationColorOwn(hexColor);
      case "ALLY" -> chat.setRelationColorAlly(hexColor);
      case "NEUTRAL" -> chat.setRelationColorNeutral(hexColor);
      case "ENEMY" -> chat.setRelationColorEnemy(hexColor);
      default -> throw new IllegalArgumentException("Unknown relation: " + relation
          + ". Valid values: OWN, ALLY, NEUTRAL, ENEMY");
    }
  }

  /**
   * Sets the prefix text color (the text inside the brackets).
   *
   * @param hexColor hex color string in #RRGGBB format
   */
  public static void setPrefixColor(@NotNull String hexColor) {
    validateHexColor(hexColor, "hexColor");
    ConfigManager.get().server().setPrefixColor(hexColor);
  }

  /**
   * Sets the prefix bracket color (the [ ] around the prefix).
   *
   * @param hexColor hex color string in #RRGGBB format
   */
  public static void setPrefixBracketColor(@NotNull String hexColor) {
    validateHexColor(hexColor, "hexColor");
    ConfigManager.get().server().setPrefixBracketColor(hexColor);
  }

  /**
   * Sets the player name color in public chat.
   *
   * @param hexColor hex color string in #RRGGBB format
   */
  public static void setPlayerNameColor(@NotNull String hexColor) {
    validateHexColor(hexColor, "hexColor");
    ConfigManager.get().chat().setPlayerNameColor(hexColor);
  }

  /**
   * Sets the message text color in faction/ally chat.
   *
   * @param hexColor hex color string in #RRGGBB format
   */
  public static void setMessageColor(@NotNull String hexColor) {
    validateHexColor(hexColor, "hexColor");
    ConfigManager.get().chat().setMessageColor(hexColor);
  }

  /**
   * Sets the faction chat message color.
   *
   * @param hexColor hex color string in #RRGGBB format
   */
  public static void setFactionChatColor(@NotNull String hexColor) {
    validateHexColor(hexColor, "hexColor");
    ConfigManager.get().chat().setFactionChatColor(hexColor);
  }

  /**
   * Sets the ally chat message color.
   *
   * @param hexColor hex color string in #RRGGBB format
   */
  public static void setAllyChatColor(@NotNull String hexColor) {
    validateHexColor(hexColor, "hexColor");
    ConfigManager.get().chat().setAllyChatColor(hexColor);
  }

  /**
   * Sets the sender name color in faction/ally chat.
   *
   * @param hexColor hex color string in #RRGGBB format
   */
  public static void setSenderNameColor(@NotNull String hexColor) {
    validateHexColor(hexColor, "hexColor");
    ConfigManager.get().chat().setSenderNameColor(hexColor);
  }

  /**
   * Sets the no-faction tag color.
   *
   * @param hexColor hex color string in #RRGGBB format
   */
  public static void setNoFactionTagColor(@NotNull String hexColor) {
    validateHexColor(hexColor, "hexColor");
    ConfigManager.get().chat().setNoFactionTagColor(hexColor);
  }

  /**
   * Applies a color theme as a map of property names to hex colors.
   * Only provided keys are updated; others remain unchanged.
   * All entries are validated before any are applied (atomic semantics).
   *
   * <p>Supported keys:
   * <ul>
   *   <li>{@code relationOwn}, {@code relationAlly}, {@code relationNeutral}, {@code relationEnemy}</li>
   *   <li>{@code prefixColor}, {@code prefixBracketColor}</li>
   *   <li>{@code playerNameColor}, {@code senderNameColor}, {@code messageColor}</li>
   *   <li>{@code factionChatColor}, {@code allyChatColor}</li>
   *   <li>{@code noFactionTagColor}</li>
   * </ul>
   *
   * @param colors map of property name → hex color (#RRGGBB)
   * @throws IllegalArgumentException if any key is unrecognized or any value is not valid hex
   */
  public static void setChatColors(@NotNull Map<String, String> colors) {
    // Validate all entries before applying any (atomic semantics)
    for (Map.Entry<String, String> entry : colors.entrySet()) {
      validateHexColor(entry.getValue(), entry.getKey());
      if (!CHAT_COLOR_KEYS.contains(entry.getKey())) {
        throw new IllegalArgumentException("Unknown chat color key: " + entry.getKey()
            + ". Valid keys: " + CHAT_COLOR_KEYS);
      }
    }

    ChatConfig chat = ConfigManager.get().chat();
    for (Map.Entry<String, String> entry : colors.entrySet()) {
      switch (entry.getKey()) {
        case "relationOwn" -> chat.setRelationColorOwn(entry.getValue());
        case "relationAlly" -> chat.setRelationColorAlly(entry.getValue());
        case "relationNeutral" -> chat.setRelationColorNeutral(entry.getValue());
        case "relationEnemy" -> chat.setRelationColorEnemy(entry.getValue());
        case "prefixColor" -> ConfigManager.get().server().setPrefixColor(entry.getValue());
        case "prefixBracketColor" -> ConfigManager.get().server().setPrefixBracketColor(entry.getValue());
        case "playerNameColor" -> chat.setPlayerNameColor(entry.getValue());
        case "senderNameColor" -> chat.setSenderNameColor(entry.getValue());
        case "messageColor" -> chat.setMessageColor(entry.getValue());
        case "factionChatColor" -> chat.setFactionChatColor(entry.getValue());
        case "allyChatColor" -> chat.setAllyChatColor(entry.getValue());
        case "noFactionTagColor" -> chat.setNoFactionTagColor(entry.getValue());
        default -> {} // Already validated above
      }
    }
  }

  /**
   * Returns the current chat color values as a map.
   * Keys match those accepted by {@link #setChatColors(Map)}.
   * Useful for reading current values before applying a theme, enabling restore.
   *
   * @return map of property name → current hex color
   */
  @NotNull
  public static Map<String, String> getChatColors() {
    ChatConfig chat = ConfigManager.get().chat();
    return Map.ofEntries(
        Map.entry("relationOwn", chat.getRelationColorOwn()),
        Map.entry("relationAlly", chat.getRelationColorAlly()),
        Map.entry("relationNeutral", chat.getRelationColorNeutral()),
        Map.entry("relationEnemy", chat.getRelationColorEnemy()),
        Map.entry("prefixColor", ConfigManager.get().server().getPrefixColor()),
        Map.entry("prefixBracketColor", ConfigManager.get().server().getPrefixBracketColor()),
        Map.entry("playerNameColor", chat.getPlayerNameColor()),
        Map.entry("senderNameColor", chat.getSenderNameColor()),
        Map.entry("messageColor", chat.getMessageColor()),
        Map.entry("factionChatColor", chat.getFactionChatColor()),
        Map.entry("allyChatColor", chat.getAllyChatColor()),
        Map.entry("noFactionTagColor", chat.getNoFactionTagColor())
    );
  }
}
