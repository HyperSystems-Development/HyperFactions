package com.hyperfactions.territory;

import com.hyperfactions.data.RelationType;
import com.hyperfactions.data.Zone;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents information about a territory at a specific location.
 * Used for tracking player position and territory change notifications.
 *
 * @param type             the type of territory
 * @param factionId        the faction ID if this is a faction claim, null otherwise
 * @param factionName      the faction name if this is a faction claim, null otherwise
 * @param factionTag       the faction tag if this is a faction claim, null otherwise
 * @param relation         the player's relation to this territory's owner, null for non-faction territories
 * @param notifyOnEntry    whether to show entry notification (null/true = show, false = suppress)
 * @param notifyTitleUpper custom upper title text override (null = default)
 * @param notifyTitleLower custom lower title text override (null = default)
 */
public record TerritoryInfo(
  @NotNull TerritoryType type,
  @Nullable UUID factionId,
  @Nullable String factionName,
  @Nullable String factionTag,
  @Nullable RelationType relation,
  @Nullable Boolean notifyOnEntry,
  @Nullable String notifyTitleUpper,
  @Nullable String notifyTitleLower
) {
  /**
   * Types of territories.
   */
  public enum TerritoryType {
    /** Unclaimed wilderness. */
    WILDERNESS("Wilderness", "#AAAAAA"),
    /** Admin-protected SafeZone. */
    SAFEZONE("SafeZone", "#55FF55"),
    /** Admin PvP zone. */
    WARZONE("WarZone", "#FF5555"),
    /** Faction-claimed territory. */
    FACTION_CLAIM("Faction Claim", "#55FFFF");

    private final String displayName;

    private final String defaultColor;

    TerritoryType(String displayName, String defaultColor) {
      this.displayName = displayName;
      this.defaultColor = defaultColor;
    }

    /**
     * Gets the display name of this territory type.
     *
     * @return the display name
     */
    @NotNull
    public String getDisplayName() {
      return displayName;
    }

    /**
     * Gets the default color for this territory type.
     *
     * @return the hex color code
     */
    @NotNull
    public String getDefaultColor() {
      return defaultColor;
    }
  }

  /**
   * Creates TerritoryInfo for wilderness.
   *
   * @return wilderness territory info
   */
  public static TerritoryInfo wilderness() {
    return new TerritoryInfo(TerritoryType.WILDERNESS, null, null, null, null, null, null, null);
  }

  /**
   * Creates TerritoryInfo for wilderness with custom notification settings.
   * Used when the wilderness message depends on what the player left (zone vs claim).
   *
   * @param enabled whether to show the wilderness notification
   * @param upper   custom upper/secondary text (empty string = no secondary)
   * @param lower   custom lower/primary text (the main wilderness label)
   * @return customized wilderness territory info
   */
  public static TerritoryInfo wilderness(boolean enabled, @NotNull String upper, @NotNull String lower) {
    return new TerritoryInfo(TerritoryType.WILDERNESS, null, null, null, null,
        enabled, upper.isEmpty() ? null : upper, lower);
  }

  /**
   * Creates TerritoryInfo for a SafeZone with default notification settings.
   *
   * @param zoneName the zone name
   * @return safezone territory info
   */
  public static TerritoryInfo safeZone(@NotNull String zoneName) {
    return new TerritoryInfo(TerritoryType.SAFEZONE, null, zoneName, null, null, null, null, null);
  }

  /**
   * Creates TerritoryInfo for a SafeZone with zone notification settings.
   *
   * @param zone the zone
   * @return safezone territory info with notification settings
   */
  public static TerritoryInfo safeZone(@NotNull Zone zone) {
    return new TerritoryInfo(TerritoryType.SAFEZONE, null, zone.name(), null, null,
        zone.notifyOnEntry(), zone.notifyTitleUpper(), zone.notifyTitleLower());
  }

  /**
   * Creates TerritoryInfo for a WarZone with default notification settings.
   *
   * @param zoneName the zone name
   * @return warzone territory info
   */
  public static TerritoryInfo warZone(@NotNull String zoneName) {
    return new TerritoryInfo(TerritoryType.WARZONE, null, zoneName, null, null, null, null, null);
  }

  /**
   * Creates TerritoryInfo for a WarZone with zone notification settings.
   *
   * @param zone the zone
   * @return warzone territory info with notification settings
   */
  public static TerritoryInfo warZone(@NotNull Zone zone) {
    return new TerritoryInfo(TerritoryType.WARZONE, null, zone.name(), null, null,
        zone.notifyOnEntry(), zone.notifyTitleUpper(), zone.notifyTitleLower());
  }

  /**
   * Creates TerritoryInfo for a faction claim.
   *
   * @param factionId   the faction ID
   * @param factionName the faction name
   * @param factionTag  the faction tag (may be null)
   * @param relation    the player's relation to this faction
   * @return faction claim territory info
   */
  public static TerritoryInfo factionClaim(
      @NotNull UUID factionId,
      @NotNull String factionName,
      @Nullable String factionTag,
      @NotNull RelationType relation) {
    return new TerritoryInfo(TerritoryType.FACTION_CLAIM, factionId, factionName, factionTag, relation, null, null, null);
  }

  /**
   * Checks if entry notification is enabled for this territory.
   *
   * @return true if notification should be shown (default), false if suppressed
   */
  public boolean isNotificationEnabled() {
    return notifyOnEntry == null || notifyOnEntry;
  }

  /**
   * Checks if this territory is different from another territory.
   * Used to determine if a notification should be sent.
   *
   * @param other the other territory info (can be null for initial entry)
   * @return true if territories are different and notification should be sent
   */
  public boolean isDifferentFrom(@Nullable TerritoryInfo other) {
    if (other == null) {
      return true; // First territory always triggers notification
    }

    // Different territory types always count as different
    if (this.type != other.type) {
      return true;
    }

    // For faction claims, check if faction changed
    if (this.type == TerritoryType.FACTION_CLAIM) {
      return !Objects.equals(this.factionId, other.factionId);
    }

    // For zones (SafeZone/WarZone), check if zone name changed
    if (this.type == TerritoryType.SAFEZONE || this.type == TerritoryType.WARZONE) {
      return !Objects.equals(this.factionName, other.factionName);
    }

    // Same wilderness
    return false;
  }

  /**
   * Gets the display color for this territory based on type and relation.
   *
   * @return the hex color code
   */
  @NotNull
  public String getDisplayColor() {
    if (type == TerritoryType.FACTION_CLAIM && relation != null) {
      return switch (relation) {
        case OWN -> "#55FF55";     // Green for own faction
        case ALLY -> "#55FF55";    // Green for ally
        case NEUTRAL -> "#FFFF55"; // Yellow for neutral
        case ENEMY -> "#FF5555";   // Red for enemy
      };
    }
    return type.getDefaultColor();
  }

  /**
   * Gets the primary display text for the notification.
   * For faction claims, includes the tag if available (e.g., "FactionName [TAG]").
   *
   * @return the primary display text
   */
  @NotNull
  public String getPrimaryText() {
    if (notifyTitleLower != null) {
      return notifyTitleLower;
    }
    return switch (type) {
      case WILDERNESS -> "Wilderness";
      case SAFEZONE -> factionName != null ? factionName : "SafeZone";
      case WARZONE -> factionName != null ? factionName : "WarZone";
      case FACTION_CLAIM -> {
        if (factionName == null) {
          yield "Unknown Faction";
        }
        if (factionTag != null && !factionTag.isEmpty()) {
          yield factionName + " [" + factionTag + "]";
        }
        yield factionName;
      }
    };
  }

  /**
   * Gets the secondary display text for the notification.
   * Includes territory type and special status.
   *
   * @return the secondary display text, or null if none
   */
  @Nullable
  public String getSecondaryText() {
    if (notifyTitleUpper != null) {
      return notifyTitleUpper.isEmpty() ? null : notifyTitleUpper;
    }
    return switch (type) {
      case WILDERNESS -> null;
      case SAFEZONE -> "PvP Disabled";
      case WARZONE -> "PvP Enabled - No Protection";
      case FACTION_CLAIM -> {
        if (relation == RelationType.OWN) {
          yield "Your Territory";
        }
        if (relation != null) {
          yield relation.getDisplayName() + " Territory";
        }
        yield "Faction Territory";
      }
    };
  }
}
