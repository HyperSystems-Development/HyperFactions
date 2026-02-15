package com.hyperfactions.integration.wiflow;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.*;
import com.hyperfactions.manager.*;
import com.wiflow.placeholderapi.context.PlaceholderContext;
import com.wiflow.placeholderapi.expansion.PlaceholderExpansion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * WiFlow PlaceholderAPI expansion for HyperFactions.
 *
 * Exposes faction data as WiFlow placeholders ({@code {factions_xxx}}) for use
 * by scoreboards, holograms, chat formatting, and other WiFlow consumers.
 *
 * Player faction:
 *   {factions_has_faction} - Whether player has a faction (yes/no)
 *   {factions_name} - Faction name
 *   {factions_faction_id} - Faction UUID
 *   {factions_tag} - Faction tag (short identifier)
 *   {factions_display} - Tag or name based on tagDisplay config
 *   {factions_color} - Faction color code
 *   {factions_role} - Player's role (Leader/Officer/Member)
 *   {factions_description} - Faction description
 *   {factions_leader} - Faction leader's name
 *   {factions_leader_id} - Faction leader's UUID
 *   {factions_open} - Whether faction is open (true/false)
 *   {factions_created} - Faction creation date (yyyy-MM-dd)
 *
 * Power:
 *   {factions_power} - Player's current power
 *   {factions_maxpower} - Player's max power
 *   {factions_power_percent} - Player's power percentage
 *   {factions_faction_power} - Faction's total power
 *   {factions_faction_maxpower} - Faction's max power
 *   {factions_faction_power_percent} - Faction's power percentage
 *   {factions_raidable} - Whether faction is raidable (true/false)
 *
 * Territory:
 *   {factions_land} - Number of claimed chunks
 *   {factions_land_max} - Max claimable chunks
 *   {factions_territory} - Faction owning current chunk
 *   {factions_territory_type} - Territory type at current location
 *
 * Faction home:
 *   {factions_home_world} - World name of faction home
 *   {factions_home_x} - X coordinate of faction home (2 d.p.)
 *   {factions_home_y} - Y coordinate of faction home (2 d.p.)
 *   {factions_home_z} - Z coordinate of faction home (2 d.p.)
 *   {factions_home_coords} - X, Y, Z coordinates of faction home (2 d.p.)
 *   {factions_home_yaw} - Yaw of faction home (2 d.p.)
 *   {factions_home_pitch} - Pitch of faction home (2 d.p.)
 *
 * Members and relations:
 *   {factions_members} - Total member count
 *   {factions_members_online} - Online member count
 *   {factions_allies} - Number of allied factions
 *   {factions_enemies} - Number of enemy factions
 *   {factions_neutrals} - Number of neutral relations
 *   {factions_relations} - Total number of relations
 */
public class WiFlowExpansion extends PlaceholderExpansion {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());

    private final HyperFactions plugin;

    public WiFlowExpansion(@NotNull HyperFactions plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "factions";
    }

    @Override
    public String getAuthor() {
        return "HyperSystemsDev";
    }

    @Override
    public String getVersion() {
        return HyperFactions.VERSION;
    }

    @Override
    public String getName() {
        return "HyperFactions";
    }

    @Override
    public String getDescription() {
        return "Faction data placeholders: name, tag, role, power, territory, members, relations";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public List<String> getPlaceholders() {
        return List.of(
                "has_faction", "name", "faction_id", "tag", "display", "color",
                "role", "description", "leader", "leader_id", "open", "created",
                "power", "maxpower", "power_percent",
                "faction_power", "faction_maxpower", "faction_power_percent", "raidable",
                "land", "land_max", "territory", "territory_type",
                "home_world", "home_x", "home_y", "home_z", "home_coords", "home_yaw", "home_pitch",
                "members", "members_online",
                "allies", "enemies", "neutrals", "relations"
        );
    }

    @Override
    @Nullable
    public String onPlaceholderRequest(@NotNull PlaceholderContext context, @NotNull String params) {
        UUID uuid = context.getPlayerUuid();
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
            case "territory"      -> getTerritoryOwner(context);
            case "territory_type" -> getTerritoryType(context);

            // Faction home
            case "home_world" -> getFactionHomeWorld(uuid);
            case "home_x"     -> getFactionHomeX(uuid);
            case "home_y"     -> getFactionHomeY(uuid);
            case "home_z"     -> getFactionHomeZ(uuid);
            case "home_coords" -> getFactionHomeCoords(uuid);
            case "home_yaw"   -> getFactionHomeYaw(uuid);
            case "home_pitch" -> getFactionHomePitch(uuid);

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
        if (faction == null) return "";
        return faction.tag() != null ? faction.tag() : "";
    }

    @NotNull
    private String getFactionDisplay(@NotNull UUID uuid) {
        Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
        if (faction == null) return "";

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
        if (faction == null) return "";
        return faction.color() != null ? faction.color() : "";
    }

    @NotNull
    private String getPlayerRole(@NotNull UUID uuid) {
        Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
        if (faction == null) return "";
        FactionMember member = faction.getMember(uuid);
        if (member == null) return "";
        return member.role().getDisplayName();
    }

    @NotNull
    private String getFactionDescription(@NotNull UUID uuid) {
        Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
        if (faction == null) return "";
        return faction.description() != null ? faction.description() : "";
    }

    @NotNull
    private String getFactionLeader(@NotNull UUID uuid) {
        Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
        if (faction == null) return "";
        FactionMember leader = faction.getLeader();
        return leader != null ? leader.username() : "";
    }

    @NotNull
    private String getFactionOpen(@NotNull UUID uuid) {
        Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
        if (faction == null) return "";
        return String.valueOf(faction.open());
    }

    @NotNull
    private String getFactionLeaderId(@NotNull UUID uuid) {
        Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
        if (faction == null) return "";
        FactionMember leader = faction.getLeader();
        return leader != null ? leader.uuid().toString() : "";
    }

    @NotNull
    private String getFactionCreated(@NotNull UUID uuid) {
        Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
        if (faction == null) return "";
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
        if (faction == null) return "0.0";
        return String.format("%.1f", plugin.getPowerManager().getFactionPower(faction.id()));
    }

    @NotNull
    private String getFactionMaxPower(@NotNull UUID uuid) {
        Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
        if (faction == null) return "0.0";
        return String.format("%.1f", plugin.getPowerManager().getFactionMaxPower(faction.id()));
    }

    @NotNull
    private String getFactionPowerPercent(@NotNull UUID uuid) {
        Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
        if (faction == null) return "0";
        PowerManager.FactionPowerStats stats = plugin.getPowerManager().getFactionPowerStats(faction.id());
        return String.valueOf(stats.getPowerPercent());
    }

    @NotNull
    private String getFactionRaidable(@NotNull UUID uuid) {
        Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
        if (faction == null) return "false";
        return String.valueOf(plugin.getPowerManager().isFactionRaidable(faction.id()));
    }

    // ==================== Territory ====================

    @NotNull
    private String getFactionLand(@NotNull UUID uuid) {
        Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
        if (faction == null) return "0";
        return String.valueOf(faction.getClaimCount());
    }

    @NotNull
    private String getFactionLandMax(@NotNull UUID uuid) {
        Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
        if (faction == null) return "0";
        return String.valueOf(plugin.getPowerManager().getFactionClaimCapacity(faction.id()));
    }

    /**
     * Gets the name of the faction that owns the chunk at the player's position.
     * Uses chunk coordinates directly from the WiFlow PlaceholderContext.
     * Returns "Wilderness", "SafeZone", "WarZone", or the faction name.
     */
    @Nullable
    private String getTerritoryOwner(@NotNull PlaceholderContext context) {
        String world = context.getWorldName();
        if (world == null || world.isEmpty()) return null;

        // WiFlow context provides block coordinates; convert to chunk coords
        int chunkX = context.getPosX() >> 4;
        int chunkZ = context.getPosZ() >> 4;

        // Check zones first
        if (plugin.getZoneManager().isInSafeZone(world, chunkX, chunkZ)) {
            return "SafeZone";
        }
        if (plugin.getZoneManager().isInWarZone(world, chunkX, chunkZ)) {
            return "WarZone";
        }

        // Check claims
        UUID claimOwner = plugin.getClaimManager().getClaimOwner(world, chunkX, chunkZ);
        if (claimOwner != null) {
            Faction faction = plugin.getFactionManager().getFaction(claimOwner);
            return faction != null ? faction.name() : null;
        }

        return "Wilderness";
    }

    /**
     * Gets the territory type at the player's current location.
     * Uses chunk coordinates directly from the WiFlow PlaceholderContext.
     * Returns "SafeZone", "WarZone", "Claimed", or "Wilderness".
     */
    @Nullable
    private String getTerritoryType(@NotNull PlaceholderContext context) {
        String world = context.getWorldName();
        if (world == null || world.isEmpty()) return null;

        // WiFlow context provides block coordinates; convert to chunk coords
        int chunkX = context.getPosX() >> 4;
        int chunkZ = context.getPosZ() >> 4;

        if (plugin.getZoneManager().isInSafeZone(world, chunkX, chunkZ)) {
            return "SafeZone";
        }
        if (plugin.getZoneManager().isInWarZone(world, chunkX, chunkZ)) {
            return "WarZone";
        }
        if (plugin.getClaimManager().isClaimed(world, chunkX, chunkZ)) {
            return "Claimed";
        }
        return "Wilderness";
    }

    // ==================== Faction Home ====================

    @NotNull
    private String getFactionHomeWorld(@NotNull UUID uuid) {
        Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
        if (faction == null || faction.home() == null) return "";
        return faction.home().world();
    }

    @NotNull
    private String getFactionHomeX(@NotNull UUID uuid) {
        Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
        if (faction == null || faction.home() == null) return "";
        return String.format("%.2f", faction.home().x());
    }

    @NotNull
    private String getFactionHomeY(@NotNull UUID uuid) {
        Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
        if (faction == null || faction.home() == null) return "";
        return String.format("%.2f", faction.home().y());
    }

    @NotNull
    private String getFactionHomeZ(@NotNull UUID uuid) {
        Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
        if (faction == null || faction.home() == null) return "";
        return String.format("%.2f", faction.home().z());
    }

    @NotNull
    private String getFactionHomeCoords(@NotNull UUID uuid) {
        Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
        if (faction == null || faction.home() == null) return "";
        Faction.FactionHome home = faction.home();
        return String.format("%.2f, %.2f, %.2f", home.x(), home.y(), home.z());
    }

    @NotNull
    private String getFactionHomeYaw(@NotNull UUID uuid) {
        Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
        if (faction == null || faction.home() == null) return "";
        return String.format("%.2f", faction.home().yaw());
    }

    @NotNull
    private String getFactionHomePitch(@NotNull UUID uuid) {
        Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
        if (faction == null || faction.home() == null) return "";
        return String.format("%.2f", faction.home().pitch());
    }

    // ==================== Members & Relations ====================

    @NotNull
    private String getFactionMembers(@NotNull UUID uuid) {
        Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
        if (faction == null) return "0";
        return String.valueOf(faction.getMemberCount());
    }

    @NotNull
    private String getFactionMembersOnline(@NotNull UUID uuid) {
        Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
        if (faction == null) return "0";

        int online = 0;
        for (UUID memberUuid : faction.members().keySet()) {
            if (plugin.lookupPlayer(memberUuid) != null) {
                online++;
            }
        }
        return String.valueOf(online);
    }

    @NotNull
    private String getFactionAllyCount(@NotNull UUID uuid) {
        Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
        if (faction == null) return "0";
        long count = faction.relations().values().stream()
                .filter(r -> r.type() == RelationType.ALLY)
                .count();
        return String.valueOf(count);
    }

    @NotNull
    private String getFactionEnemyCount(@NotNull UUID uuid) {
        Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
        if (faction == null) return "0";
        long count = faction.relations().values().stream()
                .filter(r -> r.type() == RelationType.ENEMY)
                .count();
        return String.valueOf(count);
    }

    @NotNull
    private String getFactionNeutralCount(@NotNull UUID uuid) {
        Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
        if (faction == null) return "0";
        long count = faction.relations().values().stream()
                .filter(r -> r.type() == RelationType.NEUTRAL)
                .count();
        return String.valueOf(count);
    }

    @NotNull
    private String getFactionRelationCount(@NotNull UUID uuid) {
        Faction faction = plugin.getFactionManager().getPlayerFaction(uuid);
        if (faction == null) return "0";
        return String.valueOf(faction.relations().size());
    }
}
