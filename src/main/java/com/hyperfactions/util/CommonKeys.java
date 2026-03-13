package com.hyperfactions.util;

/**
 * Common and shared message keys split from the original MessageKeys.
 *
 * <p>
 * Contains cross-cutting message key constants used across multiple features:
 * common UI labels, protection denial messages, territory notifications,
 * announcements, teleportation, and chat display names.
 */
public final class CommonKeys {

  private CommonKeys() {}

  // =====================================================================
  // Common — shared messages used across multiple features
  // =====================================================================

  /** Shared messages used across multiple features (commands, GUI, protection). */
  public static final class Common {
    public static final String NO_PERMISSION = "hyperfactions.common.no_permission";
    public static final String NOT_IN_FACTION = "hyperfactions.common.not_in_faction";
    public static final String ALREADY_IN_FACTION = "hyperfactions.common.already_in_faction";
    public static final String PLAYER_NOT_FOUND = "hyperfactions.common.player_not_found";
    public static final String FACTION_NOT_FOUND = "hyperfactions.common.faction_not_found";
    public static final String PLAYER_NOT_ONLINE = "hyperfactions.common.player_not_online";
    public static final String MUST_BE_LEADER = "hyperfactions.common.must_be_leader";
    public static final String MUST_BE_OFFICER = "hyperfactions.common.must_be_officer";
    public static final String COMBAT_TAGGED = "hyperfactions.common.combat_tagged";
    public static final String CANCEL = "hyperfactions.common.cancel";
    public static final String CONFIRM = "hyperfactions.common.confirm";
    public static final String SAVE = "hyperfactions.common.save";
    public static final String CLOSE = "hyperfactions.common.close";
    public static final String YES = "hyperfactions.common.yes";
    public static final String NO = "hyperfactions.common.no";
    public static final String LOADING = "hyperfactions.common.loading";
    public static final String ONLINE = "hyperfactions.common.online";
    public static final String OFFLINE = "hyperfactions.common.offline";
    public static final String ENABLED = "hyperfactions.common.enabled";
    public static final String DISABLED = "hyperfactions.common.disabled";
    public static final String NONE = "hyperfactions.common.none";
    public static final String PAGE = "hyperfactions.common.page";
    public static final String UNKNOWN = "hyperfactions.common.unknown";
    public static final String ERROR_GENERIC = "hyperfactions.common.error_generic";
    public static final String GUI_FALLBACK = "hyperfactions.common.gui_fallback";
    public static final String ADMIN_PREFIX = "hyperfactions.common.admin_prefix";
    public static final String LOCATION_ERROR = "hyperfactions.common.location_error";
    public static final String WORLD_ERROR = "hyperfactions.common.world_error";
    public static final String INVALID_ID = "hyperfactions.common.invalid_id";
    public static final String NA = "hyperfactions.common.na";
    public static final String CLEAR = "hyperfactions.common.clear";
    public static final String BACK = "hyperfactions.common.back";
    public static final String LEAVE = "hyperfactions.common.leave";
    public static final String TRANSFER = "hyperfactions.common.transfer";
    public static final String DISBAND = "hyperfactions.common.disband";
    public static final String WORLD_FALLBACK = "hyperfactions.common.world_fallback";
    public static final String NO_DESCRIPTION = "hyperfactions.common.no_description";
    public static final String MEMBER_COUNT = "hyperfactions.common.member_count";
    public static final String ECONOMY_DISABLED = "hyperfactions.common.economy_disabled";

    private Common() {}
  }

  // =====================================================================
  // Protection — denial messages
  // =====================================================================

  /** Protection denial messages shown when actions are blocked. */
  public static final class Protection {
    // Action phrases (what the player tried to do)
    public static final String ACTION_GENERIC = "hyperfactions.protection.action.generic";
    public static final String ACTION_BUILD = "hyperfactions.protection.action.build";
    public static final String ACTION_INTERACT = "hyperfactions.protection.action.interact";
    public static final String ACTION_DOOR = "hyperfactions.protection.action.door";
    public static final String ACTION_CONTAINER = "hyperfactions.protection.action.container";
    public static final String ACTION_BENCH = "hyperfactions.protection.action.bench";
    public static final String ACTION_PROCESSING = "hyperfactions.protection.action.processing";
    public static final String ACTION_SEAT = "hyperfactions.protection.action.seat";
    public static final String ACTION_LIGHT = "hyperfactions.protection.action.light";
    public static final String ACTION_TELEPORTER = "hyperfactions.protection.action.teleporter";
    public static final String ACTION_CRATE = "hyperfactions.protection.action.crate";
    public static final String ACTION_TAME = "hyperfactions.protection.action.tame";
    public static final String ACTION_NPC = "hyperfactions.protection.action.npc";
    public static final String ACTION_MOUNT = "hyperfactions.protection.action.mount";
    public static final String ACTION_PVE = "hyperfactions.protection.action.pve";
    public static final String ACTION_ITEM_DROP = "hyperfactions.protection.action.item_drop";
    public static final String ACTION_ITEM_PICKUP = "hyperfactions.protection.action.item_pickup";

    // Denial reasons (with {0} placeholder for action phrase)
    public static final String DENIED_SAFEZONE = "hyperfactions.protection.denied.safezone";
    public static final String DENIED_WARZONE = "hyperfactions.protection.denied.warzone";
    public static final String DENIED_ENEMY_CLAIM = "hyperfactions.protection.denied.enemy_claim";
    public static final String DENIED_CLAIMED = "hyperfactions.protection.denied.claimed";
    public static final String DENIED_HERE = "hyperfactions.protection.denied.here";
    public static final String DENIED_ZONE = "hyperfactions.protection.denied.zone";
    public static final String DENIED_FACTION_PERM = "hyperfactions.protection.denied.faction_perm";
    public static final String DENIED_ALLY_TERRITORY = "hyperfactions.protection.denied.ally_territory";
    public static final String DENIED_ERROR = "hyperfactions.protection.denied.error";

    // PvP denial messages
    public static final String PVP_SAFEZONE = "hyperfactions.protection.pvp.safezone";
    public static final String PVP_SAME_FACTION = "hyperfactions.protection.pvp.same_faction";
    public static final String PVP_ALLY = "hyperfactions.protection.pvp.ally";
    public static final String PVP_SPAWN_PROTECTED = "hyperfactions.protection.pvp.spawn_protected";
    public static final String PVP_TERRITORY_DISABLED = "hyperfactions.protection.pvp.territory_disabled";
    public static final String PVP_GENERIC = "hyperfactions.protection.pvp.generic";

    // Entity damage (zone-level)
    public static final String MOB_DAMAGE_DISABLED = "hyperfactions.protection.mob_damage_disabled";
    public static final String PVE_DAMAGE_DISABLED = "hyperfactions.protection.pve_damage_disabled";
    public static final String PVE_TERRITORY_DENIED = "hyperfactions.protection.pve_territory_denied";

    // Combat tag
    public static final String COMBAT_TAG_COMMAND = "hyperfactions.protection.combat_tag_command";

    private Protection() {}
  }

  // =====================================================================
  // Territory — entry/exit notifications, announcements
  // =====================================================================

  /** Territory entry/exit and announcement messages. */
  public static final class Territory {
    public static final String ENTER_OWN = "hyperfactions.territory.enter_own";
    public static final String ENTER_ALLY = "hyperfactions.territory.enter_ally";
    public static final String ENTER_ENEMY = "hyperfactions.territory.enter_enemy";
    public static final String ENTER_NEUTRAL = "hyperfactions.territory.enter_neutral";
    public static final String ENTER_WILDERNESS = "hyperfactions.territory.enter_wilderness";
    public static final String ENTER_SAFEZONE = "hyperfactions.territory.enter_safezone";
    public static final String ENTER_WARZONE = "hyperfactions.territory.enter_warzone";
    public static final String INTRUDER_ALERT = "hyperfactions.territory.intruder_alert";

    // Display text for territory notification banners
    public static final String DISPLAY_WILDERNESS = "hyperfactions.territory.display.wilderness";
    public static final String DISPLAY_SAFEZONE = "hyperfactions.territory.display.safezone";
    public static final String DISPLAY_WARZONE = "hyperfactions.territory.display.warzone";
    public static final String DISPLAY_UNKNOWN_FACTION = "hyperfactions.territory.display.unknown_faction";
    public static final String SECONDARY_PVP_DISABLED = "hyperfactions.territory.secondary.pvp_disabled";
    public static final String SECONDARY_PVP_NO_PROTECTION = "hyperfactions.territory.secondary.pvp_no_protection";
    public static final String SECONDARY_YOUR_TERRITORY = "hyperfactions.territory.secondary.your_territory";
    public static final String SECONDARY_FACTION_TERRITORY = "hyperfactions.territory.secondary.faction_territory";
    public static final String SECONDARY_RELATION_TERRITORY = "hyperfactions.territory.secondary.relation_territory";

    private Territory() {}
  }

  // =====================================================================
  // Announcements — faction-wide broadcasts
  // =====================================================================

  /** Server-wide broadcast messages (AnnouncementManager). */
  public static final class ServerAnnounce {
    public static final String FACTION_CREATED = "hyperfactions.server_announce.faction_created";
    public static final String FACTION_DISBANDED = "hyperfactions.server_announce.faction_disbanded";
    public static final String LEADERSHIP_TRANSFER = "hyperfactions.server_announce.leadership_transfer";
    public static final String OVERCLAIM = "hyperfactions.server_announce.overclaim";
    public static final String WAR_DECLARED = "hyperfactions.server_announce.war_declared";
    public static final String ALLIANCE_FORMED = "hyperfactions.server_announce.alliance_formed";
    public static final String ALLIANCE_BROKEN = "hyperfactions.server_announce.alliance_broken";

    private ServerAnnounce() {}
  }

  /** Faction-wide broadcast messages. */
  public static final class Announce {
    public static final String MEMBER_JOIN = "hyperfactions.announce.member_join";
    public static final String MEMBER_LEAVE = "hyperfactions.announce.member_leave";
    public static final String MEMBER_KICK = "hyperfactions.announce.member_kick";
    public static final String MEMBER_PROMOTED = "hyperfactions.announce.member_promoted";
    public static final String MEMBER_DEMOTED = "hyperfactions.announce.member_demoted";
    public static final String MEMBER_DEATH = "hyperfactions.announce.member_death";
    public static final String TERRITORY_CLAIMED = "hyperfactions.announce.territory_claimed";
    public static final String TERRITORY_LOST = "hyperfactions.announce.territory_lost";
    public static final String POWER_LOW = "hyperfactions.announce.power_low";
    public static final String RAIDABLE = "hyperfactions.announce.raidable";
    public static final String DEATH_LOCATION = "hyperfactions.announce.death_location";

    private Announce() {}
  }

  // =====================================================================
  // Teleport — teleportation messages
  // =====================================================================

  /** Teleport system messages (TeleportManager). */
  public static final class Teleport {
    public static final String COOLDOWN_WAIT = "hyperfactions.teleport.cooldown_wait";
    public static final String WARMUP_START = "hyperfactions.teleport.warmup_start";
    public static final String COMBAT_CANCELLED = "hyperfactions.teleport.combat_cancelled";
    public static final String SUCCESS_DEFAULT = "hyperfactions.teleport.success_default";
    public static final String NO_HOME = "hyperfactions.teleport.no_home";
    public static final String WORLD_NOT_FOUND = "hyperfactions.teleport.world_not_found";
    public static final String FAILED = "hyperfactions.teleport.failed";
    public static final String COUNTDOWN = "hyperfactions.teleport.countdown";
    public static final String COUNTDOWN_ONE = "hyperfactions.teleport.countdown_one";
    public static final String MOVED_CANCELLED = "hyperfactions.teleport.moved_cancelled";
    public static final String DAMAGE_CANCELLED = "hyperfactions.teleport.damage_cancelled";
    public static final String MOUNT_TELEPORT_BLOCKED = "hyperfactions.teleport.mount_teleport_blocked";
    public static final String MOUNT_ENTRY_BLOCKED = "hyperfactions.teleport.mount_entry_blocked";

    private Teleport() {}
  }

  // =====================================================================
  // Chat — channel display names
  // =====================================================================

  /** Chat channel display names (ChatManager). */
  public static final class ChatDisplay {
    public static final String PUBLIC = "hyperfactions.chat.display.public";
    public static final String FACTION = "hyperfactions.chat.display.faction";
    public static final String ALLY = "hyperfactions.chat.display.ally";

    private ChatDisplay() {}
  }
}
