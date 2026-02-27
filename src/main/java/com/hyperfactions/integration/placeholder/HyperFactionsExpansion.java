package com.hyperfactions.integration.placeholder;

import at.helpch.placeholderapi.expansion.PlaceholderExpansion;
import at.helpch.placeholderapi.expansion.Relational;
import com.hyperfactions.HyperFactions;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.*;
import com.hyperfactions.manager.*;
import com.hyperfactions.util.ChunkUtil;
import com.hyperfactions.util.LegacyColorParser;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PlaceholderAPI expansion for HyperFactions.
 *
 * <p>Exposes faction data as PAPI placeholders for use by scoreboards,
 * holograms, chat formatting, and other PAPI consumers.
 *
 * <p>Player faction:
 *   %factions_has_faction% - Whether player has a faction (yes/no)
 *   %factions_name% - Faction name
 *   %factions_faction_id% - Faction UUID
 *   %factions_tag% - Faction tag (short identifier)
 *   %factions_display% - Tag or name based on tagDisplay config
 *   %factions_color% - Faction color code
 *   %factions_role% - Player's role (Leader/Officer/Member)
 *   %factions_description% - Faction description
 *   %factions_leader% - Faction leader's name
 *   %factions_leader_id% - Faction leader's UUID
 *   %factions_open% - Whether faction is open (true/false)
 *   %factions_created% - Faction creation date (yyyy-MM-dd)
 *
 * <p>Power:
 *   %factions_power% - Player's current power
 *   %factions_maxpower% - Player's max power
 *   %factions_power_percent% - Player's power percentage
 *   %factions_faction_power% - Faction's total power
 *   %factions_faction_maxpower% - Faction's max power
 *   %factions_faction_power_percent% - Faction's power percentage
 *   %factions_raidable% - Whether faction is raidable (true/false)
 *
 * <p>Territory:
 *   %factions_land% - Number of claimed chunks
 *   %factions_land_max% - Max claimable chunks
 *   %factions_territory% - Faction owning current chunk
 *   %factions_territory_type% - Territory type at current location
 *
 * <p>Faction home:
 *   %factions_home_world% - World name of faction home
 *   %factions_home_x% - X coordinate of faction home (2 d.p.)
 *   %factions_home_y% - Y coordinate of faction home (2 d.p.)
 *   %factions_home_z% - Z coordinate of faction home (2 d.p.)
 *   %factions_home_coords% - X, Y, Z coordinates of faction home (2 d.p.)
 *   %factions_home_yaw% - Yaw of faction home (2 d.p.)
 *   %factions_home_pitch% - Pitch of faction home (2 d.p.)
 *
 * <p>Colored variants:
 *   %factions_color_legacy% - Nearest legacy color code (&a, &c, etc.)
 *   %factions_name_colored% - Faction name with hex color prefix (&#RRGGBB + name)
 *   %factions_tag_colored% - Faction tag with hex color prefix
 *   %factions_name_colored_legacy% - Faction name with legacy color prefix (&X + name)
 *   %factions_tag_colored_legacy% - Faction tag with legacy color prefix
 *
 * <p>Treasury (requires economy enabled):
 *   %factions_treasury_balance% - Formatted balance (e.g., $1,500.00)
 *   %factions_treasury_balance_raw% - Raw balance with 2 d.p. (e.g., 1500.00)
 *   %factions_treasury_autopay% - Whether autopay is enabled (true/false)
 *   %factions_treasury_limit% - Max treasury limit ("Unlimited" if none)
 *
 * <p>Members and relations:
 *   %factions_members% - Total member count
 *   %factions_members_online% - Online member count
 *   %factions_allies% - Number of allied factions
 *   %factions_enemies% - Number of enemy factions
 *   %factions_neutrals% - Number of neutral relations
 *   %factions_relations% - Total number of relations
 *
 * <p>Relational (two-player):
 *   %rel_factions_relation% - Relation between two players (Ally, Enemy, Neutral, Your)
 *   %rel_factions_relation_color% - Relation color code in ampersand format (&a, &c, &7)
 */
public class HyperFactionsExpansion extends PlaceholderExpansion implements Relational {

  private static final DateTimeFormatter DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());

  private final HyperFactions plugin;

  /** Creates a new HyperFactionsExpansion. */
  public HyperFactionsExpansion(@NotNull HyperFactions plugin) {
    this.plugin = plugin;
  }

  @Override
  public @NotNull String getIdentifier() {
    return "factions";
  }

  @Override
  public @NotNull String getAuthor() {
    return "HyperSystemsDev";
  }

  @Override
  public @NotNull String getVersion() {
    return HyperFactions.VERSION;
  }

  /** Persist. */
  @Override
  public boolean persist() {
    return true;
  }

  /** Called when placeholder request. */
  @Override
  @Nullable
  public String onPlaceholderRequest(@Nullable PlayerRef player, @NotNull String params) {
    if (player == null) {
      return null;
    }

    UUID uuid = player.getUuid();
    if (uuid == null) {
      return null;
    }

    return switch (params.toLowerCase()) {
      // Player faction info
      case "has_faction"  -> hasFaction(uuid);
      case "name"         -> getFactionName(uuid);
      case "faction_id"   -> getFactionId(uuid);
      case "tag"          -> getFactionTag(uuid);
      case "display"      -> getFactionDisplay(uuid);
      case "color"        -> getFactionColor(uuid);
      case "role"         -> getPlayerRole(uuid);
      case "role_display" -> getPlayerRoleDisplay(uuid);
      case "role_short"   -> getPlayerRoleShort(uuid);
      case "description"  -> getFactionDescription(uuid);
      case "leader"       -> getFactionLeader(uuid);
      case "leader_id"    -> getFactionLeaderId(uuid);
      case "open"         -> getFactionOpen(uuid);
      case "created"      -> getFactionCreated(uuid);

      // Power
      case "power"                 -> getPlayerPower(uuid);
      case "maxpower"              -> getPlayerMaxPower(uuid);
      case "power_percent"         -> getPlayerPowerPercent(uuid);
      case "faction_power"         -> getFactionPower(uuid);
      case "faction_maxpower"      -> getFactionMaxPower(uuid);
      case "faction_power_percent" -> getFactionPowerPercent(uuid);
      case "raidable"              -> getFactionRaidable(uuid);

      // Territory
      case "land"           -> getFactionLand(uuid);
      case "land_max"       -> getFactionLandMax(uuid);
      case "territory"      -> getTerritoryOwner(player);
      case "territory_type" -> getTerritoryType(player);

      // Faction home
      case "home_world" -> getFactionHomeWorld(uuid);
      case "home_x"     -> getFactionHomeX(uuid);
      case "home_y"     -> getFactionHomeY(uuid);
      case "home_z"     -> getFactionHomeZ(uuid);
      case "home_coords" -> getFactionHomeCoords(uuid);
      case "home_yaw"   -> getFactionHomeYaw(uuid);
      case "home_pitch" -> getFactionHomePitch(uuid);

      // Colored variants
      case "color_legacy"         -> getFactionColorLegacy(uuid);
      case "name_colored"         -> getFactionNameColored(uuid);
      case "tag_colored"          -> getFactionTagColored(uuid);
      case "name_colored_legacy"  -> getFactionNameColoredLegacy(uuid);
      case "tag_colored_legacy"   -> getFactionTagColoredLegacy(uuid);

      // Treasury
      case "treasury_balance"     -> getTreasuryBalance(uuid);
      case "treasury_balance_raw" -> getTreasuryBalanceRaw(uuid);
      case "treasury_autopay"     -> getTreasuryAutopay(uuid);
      case "treasury_limit"       -> "Unlimited";

      // Members & relations
      case "members"        -> getFactionMembers(uuid);
      case "members_online" -> getFactionMembersOnline(uuid);
      case "allies"         -> getFactionAllyCount(uuid);
      case "enemies"        -> getFactionEnemyCount(uuid);
      case "neutrals"       -> getFactionNeutralCount(uuid);
      case "relations"      -> getFactionRelationCount(uuid);

      default -> null; // Unknown placeholder - preserve original text
    };
  }

  // ==================== Player Faction Info ====================

  @NotNull
  private String hasFaction(@NotNull UUID uuid) {
    return plugin.getFactionManager().getPlayerFaction(uuid) != null ? "yes" : "no";
  }

  @NotNull
  private String getFactionName(@NotNull UUID uuid) {
    Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
    return faction != null ? faction.name() : "";
  }

  @NotNull
  private String getFactionId(@NotNull UUID uuid) {
    Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
    return faction != null ? faction.id().toString() : "";
  }

  @NotNull
  private String getFactionTag(@NotNull UUID uuid) {
    Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
    if (faction == null) {
      return "";
    }
    return faction.tag() != null ? faction.tag() : "";
  }

  // Returns the faction display text based on the tagDisplay config setting:
  //   "tag"  - faction tag (falls back to first 3 chars of name)
  //   "name" - full faction name
  //   "none" - empty string
  @NotNull
  private String getFactionDisplay(@NotNull UUID uuid) {
    Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
    if (faction == null) {
      return "";
    }

    String tagDisplay = ConfigManager.get().getChatTagDisplay();
    return switch (tagDisplay) {
      case "tag" -> {
        String tag = faction.tag();
        if (tag != null && !tag.isEmpty()) {
          yield tag;
        }

        // Fall back to first 3 chars of name
        String name = faction.name();
        yield name.substring(0, Math.min(3, name.length())).toUpperCase();
      }
      case "name" -> faction.name();
      case "none" -> "";
      default -> faction.name();
    };
  }

  @NotNull
  private String getFactionColor(@NotNull UUID uuid) {
    Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
    if (faction == null) {
      return "";
    }
    return faction.color() != null ? faction.color() : "";
  }

  @NotNull
  private String getPlayerRole(@NotNull UUID uuid) {
    Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
    if (faction == null) {
      return "";
    }
    FactionMember member = faction.getMember(uuid);
    if (member == null) {
      return "";
    }
    return member.role().getDisplayName();
  }

  @NotNull
  private String getPlayerRoleDisplay(@NotNull UUID uuid) {
    Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
    if (faction == null) {
      return "";
    }
    FactionMember member = faction.getMember(uuid);
    if (member == null) {
      return "";
    }
    return ConfigManager.get().getRoleDisplayName(member.role());
  }

  @NotNull
  private String getPlayerRoleShort(@NotNull UUID uuid) {
    Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
    if (faction == null) {
      return "";
    }
    FactionMember member = faction.getMember(uuid);
    if (member == null) {
      return "";
    }
    return ConfigManager.get().getRoleShortName(member.role());
  }

  @NotNull
  private String getFactionDescription(@NotNull UUID uuid) {
    Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
    if (faction == null) {
      return "";
    }
    return faction.description() != null ? faction.description() : "";
  }

  @NotNull
  private String getFactionLeader(@NotNull UUID uuid) {
    Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
    if (faction == null) {
      return "";
    }
    FactionMember leader = faction.getLeader();
    return leader != null ? leader.username() : "";
  }

  @NotNull
  private String getFactionOpen(@NotNull UUID uuid) {
    Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
    if (faction == null) {
      return "";
    }
    return String.valueOf(faction.open());
  }

  @NotNull
  private String getFactionLeaderId(@NotNull UUID uuid) {
    Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
    if (faction == null) {
      return "";
    }
    FactionMember leader = faction.getLeader();
    return leader != null ? leader.uuid().toString() : "";
  }

  @NotNull
  private String getFactionCreated(@NotNull UUID uuid) {
    Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
    if (faction == null) {
      return "";
    }
    return DATE_FORMAT.format(Instant.ofEpochMilli(faction.createdAt()));
  }

  // ==================== Power ====================

  @NotNull
  private String getPlayerPower(@NotNull UUID uuid) {
    PlayerPower power = plugin.getPowerManager().getPlayerPower(uuid);
    return String.format("%.1f", power.power());
  }

  @NotNull
  private String getPlayerMaxPower(@NotNull UUID uuid) {
    PlayerPower power = plugin.getPowerManager().getPlayerPower(uuid);
    return String.format("%.1f", power.getEffectiveMaxPower());
  }

  @NotNull
  private String getPlayerPowerPercent(@NotNull UUID uuid) {
    PlayerPower power = plugin.getPowerManager().getPlayerPower(uuid);
    return String.valueOf(power.getPowerPercent());
  }

  @NotNull
  private String getFactionPower(@NotNull UUID uuid) {
    Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
    if (faction == null) {
      return "0.0";
    }
    return String.format("%.1f", plugin.getPowerManager().getFactionPower(faction.id()));
  }

  @NotNull
  private String getFactionMaxPower(@NotNull UUID uuid) {
    Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
    if (faction == null) {
      return "0.0";
    }
    return String.format("%.1f", plugin.getPowerManager().getFactionMaxPower(faction.id()));
  }

  @NotNull
  private String getFactionPowerPercent(@NotNull UUID uuid) {
    Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
    if (faction == null) {
      return "0";
    }
    PowerManager.FactionPowerStats stats = plugin.getPowerManager().getFactionPowerStats(faction.id());
    return String.valueOf(stats.getPowerPercent());
  }

  @NotNull
  private String getFactionRaidable(@NotNull UUID uuid) {
    Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
    if (faction == null) {
      return "false";
    }
    return String.valueOf(plugin.getPowerManager().isFactionRaidable(faction.id()));
  }

  // ==================== Territory ====================

  @NotNull
  private String getFactionLand(@NotNull UUID uuid) {
    Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
    if (faction == null) {
      return "0";
    }
    return String.valueOf(faction.getClaimCount());
  }

  @NotNull
  private String getFactionLandMax(@NotNull UUID uuid) {
    Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
    if (faction == null) {
      return "0";
    }
    return String.valueOf(plugin.getPowerManager().getFactionClaimCapacity(faction.id()));
  }

  /**
   * Gets the name of the faction that owns the chunk the player is standing in.
   * Returns "Wilderness", "SafeZone", "WarZone", or the faction name.
   */
  @Nullable
  private String getTerritoryOwner(@NotNull PlayerRef playerRef) {
    PositionData pos = getPlayerPosition(playerRef);
    if (pos == null) {
      return null;
    }

    int chunkX = ChunkUtil.toChunkCoord(pos.x);
    int chunkZ = ChunkUtil.toChunkCoord(pos.z);

    // Check zones first
    if (plugin.getZoneManager().isInSafeZone(pos.world, chunkX, chunkZ)) {
      return "SafeZone";
    }
    if (plugin.getZoneManager().isInWarZone(pos.world, chunkX, chunkZ)) {
      return "WarZone";
    }

    // Check claims
    UUID claimOwner = plugin.getClaimManager().getClaimOwner(pos.world, chunkX, chunkZ);
    if (claimOwner != null) {
      Faction faction = plugin.getFactionManager().getFaction(claimOwner);
      return faction != null ? faction.name() : null;
    }

    return "Wilderness";
  }

  /**
   * Gets the territory type at the player's current location.
   * Returns "SafeZone", "WarZone", "Claimed", or "Wilderness".
   */
  @Nullable
  private String getTerritoryType(@NotNull PlayerRef playerRef) {
    PositionData pos = getPlayerPosition(playerRef);
    if (pos == null) {
      return null;
    }

    int chunkX = ChunkUtil.toChunkCoord(pos.x);
    int chunkZ = ChunkUtil.toChunkCoord(pos.z);

    if (plugin.getZoneManager().isInSafeZone(pos.world, chunkX, chunkZ)) {
      return "SafeZone";
    }
    if (plugin.getZoneManager().isInWarZone(pos.world, chunkX, chunkZ)) {
      return "WarZone";
    }
    if (plugin.getClaimManager().isClaimed(pos.world, chunkX, chunkZ)) {
      return "Claimed";
    }
    return "Wilderness";
  }

  // ==================== Faction Home ====================

  @NotNull
  private String getFactionHomeWorld(@NotNull UUID uuid) {
    Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
    if (faction == null || faction.home() == null) {
      return "";
    }
    return faction.home().world();
  }

  @NotNull
  private String getFactionHomeX(@NotNull UUID uuid) {
    Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
    if (faction == null || faction.home() == null) {
      return "";
    }
    return String.format("%.2f", faction.home().x());
  }

  @NotNull
  private String getFactionHomeY(@NotNull UUID uuid) {
    Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
    if (faction == null || faction.home() == null) {
      return "";
    }
    return String.format("%.2f", faction.home().y());
  }

  @NotNull
  private String getFactionHomeZ(@NotNull UUID uuid) {
    Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
    if (faction == null || faction.home() == null) {
      return "";
    }
    return String.format("%.2f", faction.home().z());
  }

  @NotNull
  private String getFactionHomeCoords(@NotNull UUID uuid) {
    Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
    if (faction == null || faction.home() == null) {
      return "";
    }
    Faction.FactionHome home = faction.home();
    return String.format("%.2f, %.2f, %.2f", home.x(), home.y(), home.z());
  }

  @NotNull
  private String getFactionHomeYaw(@NotNull UUID uuid) {
    Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
    if (faction == null || faction.home() == null) {
      return "";
    }
    return String.format("%.2f", faction.home().yaw());
  }

  @NotNull
  private String getFactionHomePitch(@NotNull UUID uuid) {
    Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
    if (faction == null || faction.home() == null) {
      return "";
    }
    return String.format("%.2f", faction.home().pitch());
  }

  // ==================== Members & Relations ====================

  @NotNull
  private String getFactionMembers(@NotNull UUID uuid) {
    Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
    if (faction == null) {
      return "0";
    }
    return String.valueOf(faction.getMemberCount());
  }

  @NotNull
  private String getFactionMembersOnline(@NotNull UUID uuid) {
    Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
    if (faction == null) {
      return "0";
    }

    int online = 0;
    for (UUID memberUuid : faction.members().keySet()) {
      PlayerRef member = plugin.lookupPlayer(memberUuid);
      if (member != null) {
        online++;
      }
    }
    return String.valueOf(online);
  }

  @NotNull
  private String getFactionAllyCount(@NotNull UUID uuid) {
    Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
    if (faction == null) {
      return "0";
    }
    long count = faction.relations().values().stream()
        .filter(r -> r.type() == RelationType.ALLY)
        .count();
    return String.valueOf(count);
  }

  @NotNull
  private String getFactionEnemyCount(@NotNull UUID uuid) {
    Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
    if (faction == null) {
      return "0";
    }
    long count = faction.relations().values().stream()
        .filter(r -> r.type() == RelationType.ENEMY)
        .count();
    return String.valueOf(count);
  }

  @NotNull
  private String getFactionNeutralCount(@NotNull UUID uuid) {
    Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
    if (faction == null) {
      return "0";
    }
    long count = faction.relations().values().stream()
        .filter(r -> r.type() == RelationType.NEUTRAL)
        .count();
    return String.valueOf(count);
  }

  @NotNull
  private String getFactionRelationCount(@NotNull UUID uuid) {
    Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
    if (faction == null) {
      return "0";
    }
    return String.valueOf(faction.relations().size());
  }

  // ==================== Treasury ====================

  @NotNull
  private String getTreasuryBalance(@NotNull UUID uuid) {
    Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
    if (faction == null) {
      return "";
    }
    EconomyManager econ = plugin.getEconomyManager();
    if (econ == null) {
      return "";
    }
    return econ.formatCurrency(econ.getFactionBalance(faction.id()));
  }

  @NotNull
  private String getTreasuryBalanceRaw(@NotNull UUID uuid) {
    Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
    if (faction == null) {
      return "0.00";
    }
    EconomyManager econ = plugin.getEconomyManager();
    if (econ == null) {
      return "0.00";
    }
    return econ.getFactionBalance(faction.id()).setScale(2, RoundingMode.HALF_UP).toPlainString();
  }

  @NotNull
  private String getTreasuryAutopay(@NotNull UUID uuid) {
    Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
    if (faction == null) {
      return "false";
    }
    EconomyManager econ = plugin.getEconomyManager();
    if (econ == null) {
      return "false";
    }
    var economy = econ.getEconomy(faction.id());
    return economy != null ? String.valueOf(economy.upkeepAutoPay()) : "false";
  }

  // ==================== Colored Variants ====================

  @NotNull
  private String getFactionColorLegacy(@NotNull UUID uuid) {
    Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
    if (faction == null || faction.color() == null || faction.color().isEmpty()) {
      return "";
    }
    char code = LegacyColorParser.hexToNearestLegacyCode(faction.color());
    return "&" + code;
  }

  @NotNull
  private String getFactionNameColored(@NotNull UUID uuid) {
    Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
    if (faction == null) {
      return "";
    }
    String color = faction.color();
    if (color == null || color.isEmpty()) {
      return faction.name();
    }

    // Ensure hex format is &#RRGGBB (strip leading # if present, prepend &#)
    String hex = color.startsWith("#") ? color.substring(1) : color;
    return "&#" + hex + faction.name();
  }

  @NotNull
  private String getFactionTagColored(@NotNull UUID uuid) {
    Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
    if (faction == null) {
      return "";
    }
    String tag = faction.tag();
    if (tag == null || tag.isEmpty()) {
      return "";
    }
    String color = faction.color();
    if (color == null || color.isEmpty()) {
      return tag;
    }
    String hex = color.startsWith("#") ? color.substring(1) : color;
    return "&#" + hex + tag;
  }

  @NotNull
  private String getFactionNameColoredLegacy(@NotNull UUID uuid) {
    Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
    if (faction == null) {
      return "";
    }
    String color = faction.color();
    if (color == null || color.isEmpty()) {
      return faction.name();
    }
    char code = LegacyColorParser.hexToNearestLegacyCode(color);
    return "&" + code + faction.name();
  }

  @NotNull
  private String getFactionTagColoredLegacy(@NotNull UUID uuid) {
    Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
    if (faction == null) {
      return "";
    }
    String tag = faction.tag();
    if (tag == null || tag.isEmpty()) {
      return "";
    }
    String color = faction.color();
    if (color == null || color.isEmpty()) {
      return tag;
    }
    char code = LegacyColorParser.hexToNearestLegacyCode(color);
    return "&" + code + tag;
  }

  // ==================== Relational Placeholders ====================

  /** Called when placeholder request. */
  @Override
  @Nullable
  public String onPlaceholderRequest(@NotNull PlayerRef one, @NotNull PlayerRef two, @NotNull String identifier) {
    UUID uuid1 = one.getUuid();
    UUID uuid2 = two.getUuid();
    if (uuid1 == null || uuid2 == null) {
      return null;
    }

    Faction faction1 = plugin.getFactionManager().getPlayerFaction(uuid1);
    Faction faction2 = plugin.getFactionManager().getPlayerFaction(uuid2);

    RelationType relation = resolveRelation(faction1, faction2);

    return switch (identifier.toLowerCase()) {
      case "relation" -> relation.getDisplayName();
      case "relation_color" -> {
        // Convert §X to &X for consumer compatibility
        String code = relation.getColorCode();
        yield code.replace('\u00A7', '&');
      }
      default -> null;
    };
  }

  /**
   * Resolves the relation type between two players' factions.
   */
  @NotNull
  private RelationType resolveRelation(@Nullable Faction faction1, @Nullable Faction faction2) {
    if (faction1 == null || faction2 == null) {
      return RelationType.NEUTRAL;
    }
    if (faction1.id().equals(faction2.id())) {
      return RelationType.OWN;
    }
    return plugin.getRelationManager().getRelation(faction1.id(), faction2.id());
  }

  // ==================== Position Utilities ====================

  /**
   * Extracts the player's current position from the ECS.
   * Returns null if the player is offline or position cannot be determined.
   */
  @Nullable
  private PositionData getPlayerPosition(@NotNull PlayerRef playerRef) {
    try {
      Ref<EntityStore> ref = playerRef.getReference();
      if (ref == null) {
        return null;
      }

      Store<EntityStore> store = ref.getStore();
      if (store == null) {
        return null;
      }

      TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
      if (transform == null) {
        return null;
      }

      var pos = transform.getPosition();
      String world = store.getExternalData().getWorld().getName();

      return new PositionData(world, pos.getX(), pos.getZ());
    } catch (Exception e) {
      return null;
    }
  }

  private record PositionData(String world, double x, double z) {}
}
