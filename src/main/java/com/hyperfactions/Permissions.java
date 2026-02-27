package com.hyperfactions;

/**
 * Centralized permission node definitions for HyperFactions.
 *
 * <p>Follows the Hytale permission best practices format:
 * {@code <namespace>.<category>.<subcategory>.<action>}
 *
 * <p>Permission hierarchy:
 * - hyperfactions.* - All permissions (wildcard)
 * - hyperfactions.faction.* - Faction management
 * - hyperfactions.member.* - Membership management
 * - hyperfactions.territory.* - Territory claims
 * - hyperfactions.teleport.* - Teleportation
 * - hyperfactions.relation.* - Diplomatic relations
 * - hyperfactions.chat.* - Communication
 * - hyperfactions.info.* - Information viewing
 * - hyperfactions.economy.* - Economy/treasury
 * - hyperfactions.bypass.* - Protection bypass
 * - hyperfactions.admin.* - Administration
 * - hyperfactions.limit.* - Numeric limits
 */
public final class Permissions {

  private Permissions() {}

  // === Root ===
  public static final String ROOT = "hyperfactions";

  public static final String WILDCARD = "hyperfactions.*";

  // === Basic Access ===
  /** Basic faction access - required for GUI. */
  public static final String USE = "hyperfactions.use";

  // === Faction Management (hyperfactions.faction.*) ===
  public static final String FACTION_WILDCARD = "hyperfactions.faction.*";

  /** Create a new faction. */
  public static final String CREATE = "hyperfactions.faction.create";

  /** Disband your faction (leader only). */
  public static final String DISBAND = "hyperfactions.faction.disband";

  /** Rename your faction. */
  public static final String RENAME = "hyperfactions.faction.rename";

  /** Set faction description. */
  public static final String DESC = "hyperfactions.faction.description";

  /** Set faction tag. */
  public static final String TAG = "hyperfactions.faction.tag";

  /** Set faction color. */
  public static final String COLOR = "hyperfactions.faction.color";

  /** Make faction open (anyone can join). */
  public static final String OPEN = "hyperfactions.faction.open";

  /** Make faction closed (invite only). */
  public static final String CLOSE = "hyperfactions.faction.close";

  /** Edit faction territory permissions (for officers when allowed). */
  public static final String FACTION_PERMISSIONS = "hyperfactions.faction.permissions";

  // === Membership (hyperfactions.member.*) ===
  public static final String MEMBER_WILDCARD = "hyperfactions.member.*";

  /** Invite players to your faction. */
  public static final String INVITE = "hyperfactions.member.invite";

  /** Accept faction invites / request to join. */
  public static final String JOIN = "hyperfactions.member.join";

  /** Leave your faction. */
  public static final String LEAVE = "hyperfactions.member.leave";

  /** Kick members from your faction. */
  public static final String KICK = "hyperfactions.member.kick";

  /** Promote faction members. */
  public static final String PROMOTE = "hyperfactions.member.promote";

  /** Demote faction members. */
  public static final String DEMOTE = "hyperfactions.member.demote";

  /** Transfer faction leadership. */
  public static final String TRANSFER = "hyperfactions.member.transfer";

  // === Territory (hyperfactions.territory.*) ===
  public static final String TERRITORY_WILDCARD = "hyperfactions.territory.*";

  /** Claim territory chunks. */
  public static final String CLAIM = "hyperfactions.territory.claim";

  /** Unclaim territory chunks. */
  public static final String UNCLAIM = "hyperfactions.territory.unclaim";

  /** Overclaim enemy territory. */
  public static final String OVERCLAIM = "hyperfactions.territory.overclaim";

  /** View faction territory map. */
  public static final String MAP = "hyperfactions.territory.map";

  // === Teleportation (hyperfactions.teleport.*) ===
  public static final String TELEPORT_WILDCARD = "hyperfactions.teleport.*";

  /** Teleport to faction home. */
  public static final String HOME = "hyperfactions.teleport.home";

  /** Set faction home location. */
  public static final String SETHOME = "hyperfactions.teleport.sethome";

  /** Delete faction home location. */
  public static final String DELHOME = "hyperfactions.teleport.delhome";

  /** Use the /f stuck command. */
  public static final String STUCK = "hyperfactions.teleport.stuck";

  // === Diplomacy (hyperfactions.relation.*) ===
  public static final String RELATION_WILDCARD = "hyperfactions.relation.*";

  /** Request/accept ally relations. */
  public static final String ALLY = "hyperfactions.relation.ally";

  /** Declare enemy relations. */
  public static final String ENEMY = "hyperfactions.relation.enemy";

  /** Set neutral relations. */
  public static final String NEUTRAL = "hyperfactions.relation.neutral";

  /** View faction relations. */
  public static final String RELATIONS = "hyperfactions.relation.view";

  // === Communication (hyperfactions.chat.*) ===
  public static final String CHAT_WILDCARD = "hyperfactions.chat.*";

  /** Send faction chat messages. */
  public static final String CHAT_FACTION = "hyperfactions.chat.faction";

  /** Send ally chat messages. */
  public static final String CHAT_ALLY = "hyperfactions.chat.ally";

  // === Information (hyperfactions.info.*) ===
  public static final String INFO_WILDCARD = "hyperfactions.info.*";

  /** View faction info. */
  public static final String INFO = "hyperfactions.info.faction";

  /** View faction list. */
  public static final String LIST = "hyperfactions.info.list";

  /** View player info. */
  public static final String WHO = "hyperfactions.info.player";

  /** View power info. */
  public static final String POWER = "hyperfactions.info.power";

  /** View faction members. */
  public static final String MEMBERS = "hyperfactions.info.members";

  /** View faction activity logs. */
  public static final String LOGS = "hyperfactions.info.logs";

  /** View help. */
  public static final String HELP = "hyperfactions.info.help";

  // === Economy (hyperfactions.economy.*) ===
  public static final String ECONOMY_WILDCARD = "hyperfactions.economy.*";

  /** View faction treasury balance. */
  public static final String ECONOMY_BALANCE = "hyperfactions.economy.balance";

  /** Deposit money into faction treasury. */
  public static final String ECONOMY_DEPOSIT = "hyperfactions.economy.deposit";

  /** Withdraw money from faction treasury. */
  public static final String ECONOMY_WITHDRAW = "hyperfactions.economy.withdraw";

  /** Transfer money between factions. */
  public static final String ECONOMY_TRANSFER = "hyperfactions.economy.transfer";

  /** View faction transaction log. */
  public static final String ECONOMY_LOG = "hyperfactions.economy.log";

  // === Bypass Permissions (hyperfactions.bypass.*) ===
  /** Bypass all protections. */
  public static final String BYPASS_WILDCARD = "hyperfactions.bypass.*";

  /** Bypass block placement/breaking protection. */
  public static final String BYPASS_BUILD = "hyperfactions.bypass.build";

  /** Bypass interaction protection (doors, buttons). */
  public static final String BYPASS_INTERACT = "hyperfactions.bypass.interact";

  /** Bypass container protection (chests). */
  public static final String BYPASS_CONTAINER = "hyperfactions.bypass.container";

  /** Bypass entity damage protection. */
  public static final String BYPASS_DAMAGE = "hyperfactions.bypass.damage";

  /** Bypass item use protection. */
  public static final String BYPASS_USE = "hyperfactions.bypass.use";

  /** Bypass home warmup delay. */
  public static final String BYPASS_WARMUP = "hyperfactions.bypass.warmup";

  /** Bypass home cooldown timer. */
  public static final String BYPASS_COOLDOWN = "hyperfactions.bypass.cooldown";

  /** Player is always visible to everyone on map (admin/staff). */
  public static final String BYPASS_MAP_VISIBILITY = "hyperfactions.bypass.mapvisibility";

  /** Player can see all other players regardless of faction filter. */
  public static final String BYPASS_MAP_FILTER = "hyperfactions.bypass.mapfilter";

  // === Admin Permissions (hyperfactions.admin.*) ===
  /** All admin permissions. */
  public static final String ADMIN_WILDCARD = "hyperfactions.admin.*";

  /** Base admin access (opens admin GUI). */
  public static final String ADMIN = "hyperfactions.admin.use";

  /** Reload configuration. */
  public static final String ADMIN_RELOAD = "hyperfactions.admin.reload";

  /** Debug commands. */
  public static final String ADMIN_DEBUG = "hyperfactions.admin.debug";

  /** Manage safezones and warzones. */
  public static final String ADMIN_ZONES = "hyperfactions.admin.zones";

  /** Force disband any faction. */
  public static final String ADMIN_DISBAND = "hyperfactions.admin.disband";

  /** Modify any faction. */
  public static final String ADMIN_MODIFY = "hyperfactions.admin.modify";

  /** Bypass claim limits. */
  public static final String ADMIN_BYPASS_LIMITS = "hyperfactions.admin.bypass.limits";

  /** Manage backups (create, restore, delete). */
  public static final String ADMIN_BACKUP = "hyperfactions.admin.backup";

  /** Admin power management (set, adjust, reset, max override, bypass toggles). */
  public static final String ADMIN_POWER = "hyperfactions.admin.power";

  /** Admin economy management (adjust balance, view all treasuries). */
  public static final String ADMIN_ECONOMY = "hyperfactions.admin.economy";

  // === Limit Permissions (hyperfactions.limit.*) ===
  /** Maximum claims permission prefix (e.g., hyperfactions.limit.claims.50). */
  public static final String LIMIT_CLAIMS_PREFIX = "hyperfactions.limit.claims.";

  /** Maximum power permission prefix (e.g., hyperfactions.limit.power.100). */
  public static final String LIMIT_POWER_PREFIX = "hyperfactions.limit.power.";

  /**
   * Gets all defined permissions for registration.
   *
   * @return array of all permission nodes
   */
  public static String[] getAllPermissions() {
    return new String[] {
      // Basic
      USE,
      // Faction management
      CREATE, DISBAND, RENAME, DESC, TAG, COLOR, OPEN, CLOSE, FACTION_PERMISSIONS,
      // Membership
      INVITE, JOIN, LEAVE, KICK, PROMOTE, DEMOTE, TRANSFER,
      // Territory
      CLAIM, UNCLAIM, OVERCLAIM, MAP,
      // Teleportation
      HOME, SETHOME, DELHOME, STUCK,
      // Diplomacy
      ALLY, ENEMY, NEUTRAL, RELATIONS,
      // Communication
      CHAT_FACTION, CHAT_ALLY,
      // Information
      INFO, LIST, WHO, POWER, MEMBERS, LOGS, HELP,
      // Economy
      ECONOMY_BALANCE, ECONOMY_DEPOSIT, ECONOMY_WITHDRAW, ECONOMY_TRANSFER, ECONOMY_LOG,
      // Bypass
      BYPASS_BUILD, BYPASS_INTERACT, BYPASS_CONTAINER,
      BYPASS_DAMAGE, BYPASS_USE, BYPASS_WARMUP, BYPASS_COOLDOWN,
      BYPASS_MAP_VISIBILITY, BYPASS_MAP_FILTER,
      // Admin
      ADMIN, ADMIN_RELOAD, ADMIN_DEBUG, ADMIN_ZONES,
      ADMIN_DISBAND, ADMIN_MODIFY, ADMIN_BYPASS_LIMITS, ADMIN_BACKUP,
      ADMIN_POWER, ADMIN_ECONOMY
    };
  }

  /**
   * Gets all category wildcards for registration.
   *
   * @return array of wildcard permission nodes
   */
  public static String[] getWildcards() {
    return new String[] {
      WILDCARD,
      FACTION_WILDCARD,
      MEMBER_WILDCARD,
      TERRITORY_WILDCARD,
      TELEPORT_WILDCARD,
      RELATION_WILDCARD,
      CHAT_WILDCARD,
      INFO_WILDCARD,
      ECONOMY_WILDCARD,
      BYPASS_WILDCARD,
      ADMIN_WILDCARD
    };
  }

  /**
   * Gets all user-level permissions (non-admin, non-bypass).
   *
   * @return array of user permission nodes
   */
  public static String[] getUserPermissions() {
    return new String[] {
      USE,
      CREATE, DISBAND, RENAME, DESC, TAG, COLOR, OPEN, CLOSE, FACTION_PERMISSIONS,
      INVITE, JOIN, LEAVE, KICK, PROMOTE, DEMOTE, TRANSFER,
      CLAIM, UNCLAIM, OVERCLAIM, MAP,
      HOME, SETHOME, DELHOME, STUCK,
      ALLY, ENEMY, NEUTRAL, RELATIONS,
      CHAT_FACTION, CHAT_ALLY,
      INFO, LIST, WHO, POWER, MEMBERS, LOGS, HELP,
      ECONOMY_BALANCE, ECONOMY_DEPOSIT, ECONOMY_WITHDRAW, ECONOMY_TRANSFER, ECONOMY_LOG
    };
  }

  /**
   * Gets all bypass permissions.
   *
   * @return array of bypass permission nodes
   */
  public static String[] getBypassPermissions() {
    return new String[] {
      BYPASS_BUILD, BYPASS_INTERACT, BYPASS_CONTAINER,
      BYPASS_DAMAGE, BYPASS_USE, BYPASS_WARMUP, BYPASS_COOLDOWN,
      BYPASS_MAP_VISIBILITY, BYPASS_MAP_FILTER
    };
  }

  /**
   * Gets all admin permissions.
   *
   * @return array of admin permission nodes
   */
  public static String[] getAdminPermissions() {
    return new String[] {
      ADMIN, ADMIN_RELOAD, ADMIN_DEBUG, ADMIN_ZONES,
      ADMIN_DISBAND, ADMIN_MODIFY, ADMIN_BYPASS_LIMITS, ADMIN_BACKUP,
      ADMIN_POWER, ADMIN_ECONOMY
    };
  }
}
