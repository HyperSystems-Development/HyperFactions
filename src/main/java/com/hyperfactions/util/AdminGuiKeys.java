package com.hyperfactions.util;

/**
 * Static constants for admin GUI page message keys.
 *
 * <p>
 * Split from the original MessageKeys to reduce file size. Contains the {@link AdminGui}
 * inner class with all {@code hyperfactions_admin.*} keys used by admin GUI pages.
 *
 * <p>
 * Key format: {@code hyperfactions_admin.{domain}.{action}}
 * Maps to {@code hyperfactions_admin.lang} file.
 */
public final class AdminGuiKeys {

  private AdminGuiKeys() {}

  /** Admin GUI page labels and messages. */
  public static final class AdminGui {
    // Common admin labels
    public static final String FACTION_NOT_FOUND_LABEL = "hyperfactions_admin.common.faction_not_found";
    public static final String NO_FACTION = "hyperfactions_admin.common.no_faction";
    public static final String NOT_SET = "hyperfactions_admin.common.not_set";
    public static final String ON = "hyperfactions_admin.common.on";
    public static final String OFF = "hyperfactions_admin.common.off";
    public static final String ENABLE_BTN = "hyperfactions_admin.common.enable";
    public static final String DISABLE_BTN = "hyperfactions_admin.common.disable";
    public static final String NONE_PAREN = "hyperfactions_admin.common.none_paren";
    public static final String INVALID_FACTION = "hyperfactions_admin.common.invalid_faction";
    public static final String LEADER_PREFIX = "hyperfactions_admin.common.leader_prefix";
    public static final String CLAIMS_SUFFIX = "hyperfactions_admin.common.claims_suffix";
    public static final String FACTIONS_SUFFIX = "hyperfactions_admin.common.factions_suffix";
    public static final String NAV_TITLE = "hyperfactions_admin.gui.nav_title";
    public static final String GUI_ECON_BTN_ADJUST = "hyperfactions_admin.gui.econ_btn_adjust";
    public static final String GUI_ECON_BTN_INFO = "hyperfactions_admin.gui.econ_btn_info";
    public static final String PLAYERS_SUFFIX = "hyperfactions_admin.common.players_suffix";
    public static final String CHUNKS_SUFFIX = "hyperfactions_admin.common.chunks_suffix";
    public static final String ENTRIES_SUFFIX = "hyperfactions_admin.common.entries_suffix";
    public static final String FOUND_SUFFIX = "hyperfactions_admin.common.found_suffix";
    public static final String POWER_FORMAT = "hyperfactions_admin.common.power_format";
    public static final String RAIDABLE = "hyperfactions_admin.common.raidable";
    public static final String PROTECTED = "hyperfactions_admin.common.protected";
    public static final String OFFICERS_MORE = "hyperfactions_admin.common.officers_more";
    public static final String CUSTOM_MAX = "hyperfactions_admin.common.custom_max";
    public static final String DEFAULT_MAX = "hyperfactions_admin.common.default_max";
    public static final String NOW = "hyperfactions_admin.common.now";
    public static final String AGO_SUFFIX = "hyperfactions_admin.common.ago_suffix";
    public static final String JUST_NOW = "hyperfactions_admin.common.just_now";
    public static final String NO_MEMBERSHIP_HISTORY = "hyperfactions_admin.common.no_membership_history";
    // Dashboard
    public static final String DASH_FACTIONS_PREFIX = "hyperfactions_admin.dashboard.factions_prefix";
    public static final String DASH_MEMBERS_PREFIX = "hyperfactions_admin.dashboard.members_prefix";
    public static final String DASH_CLAIMS_PREFIX = "hyperfactions_admin.dashboard.claims_prefix";
    // Actions
    public static final String ACT_CONFIRM_RESET = "hyperfactions_admin.actions.confirm_reset";
    public static final String ACT_CONFIRM_TRIGGER = "hyperfactions_admin.actions.confirm_trigger";
    public static final String ACT_KD_RESET = "hyperfactions_admin.actions.kd_reset";
    public static final String ACT_KD_RESET_FAILED = "hyperfactions_admin.actions.kd_reset_failed";
    public static final String ACT_UPKEEP_UNAVAILABLE = "hyperfactions_admin.actions.upkeep_unavailable";
    public static final String ACT_UPKEEP_TRIGGERED = "hyperfactions_admin.actions.upkeep_triggered";
    public static final String ACT_UPKEEP_FAILED = "hyperfactions_admin.actions.upkeep_failed";
    // Disband confirm
    public static final String DISBAND_FACTION_GONE = "hyperfactions_admin.disband.faction_gone";
    public static final String DISBAND_SUCCESS = "hyperfactions_admin.disband.success";
    public static final String DISBAND_FAILED = "hyperfactions_admin.disband.failed";
    public static final String DISBAND_NO_LEADER = "hyperfactions_admin.disband.no_leader";
    // Unclaim all confirm
    public static final String UNCLAIM_REMOVED = "hyperfactions_admin.unclaim.removed";
    public static final String UNCLAIM_NO_CLAIMS = "hyperfactions_admin.unclaim.no_claims";
    // Factions list
    public static final String FAC_HOME_NOT_SET = "hyperfactions_admin.factions.home_not_set";
    public static final String FAC_TELEPORTED = "hyperfactions_admin.factions.teleported";
    public static final String FAC_NO_HOME = "hyperfactions_admin.factions.no_home";
    public static final String FAC_WORLD_NOT_FOUND = "hyperfactions_admin.factions.world_not_found";
    // Faction info
    public static final String INFO_FACTION_GONE = "hyperfactions_admin.info.faction_gone";
    // Faction members
    public static final String MEM_SORT_ROLE = "hyperfactions_admin.members.sort_role";
    public static final String MEM_SORT_ONLINE = "hyperfactions_admin.members.sort_online";
    public static final String MEM_SORT_NAME = "hyperfactions_admin.members.sort_name";
    public static final String MEM_SORT_POWER = "hyperfactions_admin.members.sort_power";
    public static final String MEM_PROMOTED = "hyperfactions_admin.members.promoted";
    public static final String MEM_DEMOTED = "hyperfactions_admin.members.demoted";
    public static final String MEM_KICKED = "hyperfactions_admin.members.kicked";
    // Faction relations
    public static final String REL_ALLIES_HEADER = "hyperfactions_admin.relations.allies_header";
    public static final String REL_ENEMIES_HEADER = "hyperfactions_admin.relations.enemies_header";
    public static final String REL_NO_ALLIES = "hyperfactions_admin.relations.no_allies";
    public static final String REL_NO_ENEMIES = "hyperfactions_admin.relations.no_enemies";
    public static final String REL_NEUTRAL_COUNT = "hyperfactions_admin.relations.neutral_count";
    public static final String REL_SINCE_TODAY = "hyperfactions_admin.relations.since_today";
    public static final String REL_SINCE_ONE_DAY = "hyperfactions_admin.relations.since_one_day";
    public static final String REL_SINCE_DAYS = "hyperfactions_admin.relations.since_days";
    public static final String REL_SET_ALLY = "hyperfactions_admin.relations.set_ally";
    public static final String REL_SET_ENEMY = "hyperfactions_admin.relations.set_enemy";
    public static final String REL_SET_NEUTRAL = "hyperfactions_admin.relations.set_neutral";
    // Faction settings
    public static final String SET_LOCKED = "hyperfactions_admin.settings.locked";
    public static final String SET_PERM_TOGGLED = "hyperfactions_admin.settings.perm_toggled";
    public static final String SET_COLOR_CHANGED = "hyperfactions_admin.settings.color_changed";
    public static final String SET_RECRUITMENT_SET = "hyperfactions_admin.settings.recruitment_set";
    public static final String SET_NO_HOME = "hyperfactions_admin.settings.no_home";
    public static final String SET_HOME_CLEARED = "hyperfactions_admin.settings.home_cleared";
    // Sort dropdown labels (shared)
    public static final String SORT_POWER = "hyperfactions_admin.sort.power";
    public static final String SORT_NAME = "hyperfactions_admin.sort.name";
    public static final String SORT_MEMBERS = "hyperfactions_admin.sort.members";
    public static final String SORT_BALANCE = "hyperfactions_admin.sort.balance";
    // Players
    public static final String PLR_SORT_LAST_ONLINE = "hyperfactions_admin.players.sort_last_online";
    public static final String PLR_SORT_FACTION = "hyperfactions_admin.players.sort_faction";
    public static final String PLR_SORT_ONLINE = "hyperfactions_admin.players.sort_online";
    public static final String PLR_NOT_ONLINE = "hyperfactions_admin.players.not_online";
    public static final String PLR_WORLD_NOT_FOUND = "hyperfactions_admin.players.world_not_found";
    public static final String PLR_TELEPORTED = "hyperfactions_admin.players.teleported";
    // Player info
    public static final String PLR_DISBAND_FACTION = "hyperfactions_admin.playerinfo.disband_faction";
    public static final String PLR_KICK_LEADER = "hyperfactions_admin.playerinfo.kick_leader";
    public static final String PLR_ENTER_VALID_NUMBER = "hyperfactions_admin.playerinfo.enter_valid_number";
    public static final String PLR_ENTER_VALID_POSITIVE = "hyperfactions_admin.playerinfo.enter_valid_positive";
    public static final String PLR_FACTION_GONE = "hyperfactions_admin.playerinfo.faction_gone";
    public static final String PLR_KD_RESET = "hyperfactions_admin.playerinfo.kd_reset";
    public static final String PLR_KICKED_SUCCESS = "hyperfactions_admin.playerinfo.kicked_success";
    public static final String PLR_KICKED_LEADER = "hyperfactions_admin.playerinfo.kicked_leader";
    public static final String PLR_DISBANDED_KICK = "hyperfactions_admin.playerinfo.disbanded_kick";
    public static final String ECON_NOT_ENABLED = "hyperfactions_admin.gui.econ_not_enabled";
    public static final String GUI_INFO_MORE = "hyperfactions_admin.gui.info_more";
    public static final String LOG_TIME_1H = "hyperfactions_admin.gui.log_time_1h";
    public static final String LOG_TIME_24H = "hyperfactions_admin.gui.log_time_24h";
    public static final String LOG_TIME_7D = "hyperfactions_admin.gui.log_time_7d";
    public static final String LOG_TIME_ALL = "hyperfactions_admin.gui.log_time_all";
    public static final String SHAPE_CIRCULAR = "hyperfactions_admin.gui.shape_circular";
    public static final String SHAPE_SQUARE = "hyperfactions_admin.gui.shape_square";
    // Economy
    public static final String ECON_NO_DATA = "hyperfactions_admin.economy.no_data";
    public static final String ECON_AMOUNT_ZERO = "hyperfactions_admin.economy.amount_zero";
    public static final String ECON_ENTER_AMOUNT = "hyperfactions_admin.economy.enter_amount";
    public static final String ECON_INVALID_NUMBER = "hyperfactions_admin.economy.invalid_number";
    public static final String ECON_ERROR = "hyperfactions_admin.economy.error";
    public static final String ECON_BALANCE_NEGATIVE = "hyperfactions_admin.economy.balance_negative";
    public static final String ECON_FAILED = "hyperfactions_admin.economy.failed";
    public static final String ECON_BULK_COMPLETE = "hyperfactions_admin.economy.bulk_complete";
    public static final String ECON_BULK_FAILURES = "hyperfactions_admin.economy.bulk_failures";
    // Zones
    public static final String ZONE_NOT_FOUND = "hyperfactions_admin.zones.not_found";
    public static final String ZONE_INVALID_ID = "hyperfactions_admin.zones.invalid_id";
    public static final String ZONE_DELETED = "hyperfactions_admin.zones.deleted";
    public static final String ZONE_DELETE_FAILED = "hyperfactions_admin.zones.delete_failed";
    public static final String ZONE_NO_CHUNKS = "hyperfactions_admin.zones.no_chunks";
    public static final String ZONE_CHUNKS_SUFFIX = "hyperfactions_admin.zones.chunks_suffix";
    // Zone create wizard
    public static final String WIZ_ENTER_NAME = "hyperfactions_admin.wizard.enter_name";
    public static final String WIZ_NAME_TOO_SHORT = "hyperfactions_admin.wizard.name_too_short";
    public static final String WIZ_NAME_TOO_LONG = "hyperfactions_admin.wizard.name_too_long";
    public static final String WIZ_NAME_TAKEN = "hyperfactions_admin.wizard.name_taken";
    public static final String WIZ_RADIUS_RANGE = "hyperfactions_admin.wizard.radius_range";
    public static final String WIZ_CREATE_FAILED = "hyperfactions_admin.wizard.create_failed";
    public static final String WIZ_CREATED_NOT_FOUND = "hyperfactions_admin.wizard.created_not_found";
    public static final String WIZ_CREATED = "hyperfactions_admin.wizard.created";
    public static final String WIZ_CHUNK_CLAIMED = "hyperfactions_admin.wizard.chunk_claimed";
    public static final String WIZ_CHUNK_FAILED = "hyperfactions_admin.wizard.chunk_failed";
    public static final String WIZ_RADIUS_CLAIMED = "hyperfactions_admin.wizard.radius_claimed";
    public static final String WIZ_RADIUS_NO_CLAIMS = "hyperfactions_admin.wizard.radius_no_claims";
    public static final String WIZ_NO_CLAIMS = "hyperfactions_admin.wizard.no_claims";
    public static final String WIZ_CHUNKS_PREVIEW = "hyperfactions_admin.wizard.chunks_preview";
    // Zone rename
    public static final String ZREN_ZONE_GONE = "hyperfactions_admin.zone_rename.zone_gone";
    public static final String ZREN_ENTER_NAME = "hyperfactions_admin.zone_rename.enter_name";
    public static final String ZREN_TOO_SHORT = "hyperfactions_admin.zone_rename.too_short";
    public static final String ZREN_TOO_LONG = "hyperfactions_admin.zone_rename.too_long";
    public static final String ZREN_SAME_NAME = "hyperfactions_admin.zone_rename.same_name";
    public static final String ZREN_RENAMED = "hyperfactions_admin.zone_rename.renamed";
    public static final String ZREN_NAME_TAKEN = "hyperfactions_admin.zone_rename.name_taken";
    public static final String ZREN_INVALID_NAME = "hyperfactions_admin.zone_rename.invalid_name";
    public static final String ZREN_RENAME_FAILED = "hyperfactions_admin.zone_rename.rename_failed";
    // Zone change type
    public static final String ZTYPE_ZONE_GONE = "hyperfactions_admin.zone_type.zone_gone";
    public static final String ZTYPE_CHANGED = "hyperfactions_admin.zone_type.changed";
    public static final String ZTYPE_FAILED = "hyperfactions_admin.zone_type.failed";
    public static final String ZTYPE_FLAGS_RESET = "hyperfactions_admin.zone_type.flags_reset";
    public static final String ZTYPE_FLAGS_KEPT = "hyperfactions_admin.zone_type.flags_kept";
    // Zone integration flags
    public static final String ZINT_ZONE_NOT_FOUND = "hyperfactions_admin.zone_int.zone_not_found";
    public static final String ZINT_NO_PLUGIN = "hyperfactions_admin.zone_int.no_plugin";
    public static final String ZINT_DEFAULT = "hyperfactions_admin.zone_int.default";
    public static final String ZINT_CUSTOM = "hyperfactions_admin.zone_int.custom";

    // Integration flags UI labels
    public static final String GUI_ZINT_CAT_GRAVESTONES = "hyperfactions_admin.gui.zint_cat_gravestones";
    public static final String GUI_ZINT_GRAVESTONES_DESC = "hyperfactions_admin.gui.zint_gravestones_desc";
    public static final String GUI_ZINT_CAT_WORLD_MAP = "hyperfactions_admin.gui.zint_cat_world_map";
    public static final String GUI_ZINT_WORLD_MAP_DESC = "hyperfactions_admin.gui.zint_world_map_desc";
    public static final String GUI_ZINT_VISIBILITY_LABEL = "hyperfactions_admin.gui.zint_visibility_label";
    public static final String GUI_ZINT_CAT_ESSENTIALS = "hyperfactions_admin.gui.zint_cat_essentials";
    public static final String GUI_ZINT_RESET_DEFAULTS = "hyperfactions_admin.gui.zint_reset_defaults";
    public static final String GUI_ZINT_BACK_TO_FLAGS = "hyperfactions_admin.gui.zint_back_to_flags";
    public static final String GUI_ZINT_MAP_VIS_FACTION = "hyperfactions_admin.gui.zint_map_vis_faction";
    public static final String GUI_ZINT_MAP_VIS_ALLY = "hyperfactions_admin.gui.zint_map_vis_ally";
    public static final String GUI_ZINT_MAP_VIS_ALL = "hyperfactions_admin.gui.zint_map_vis_all";

    // Activity log
    public static final String LOG_ALL_TYPES = "hyperfactions_admin.log.all_types";
    public static final String LOG_NO_LOGS = "hyperfactions_admin.log.no_logs";
    // Version page
    public static final String VER_ACTIVE = "hyperfactions_admin.version.active";
    public static final String VER_NOT_FOUND = "hyperfactions_admin.version.not_found";
    public static final String VER_NOT_DETECTED = "hyperfactions_admin.version.not_detected";
    public static final String VER_NOT_INSTALLED = "hyperfactions_admin.version.not_installed";
    public static final String VER_ACTIVE_VERSION = "hyperfactions_admin.version.active_version";
    public static final String VER_ACTIVE_COMPATIBLE = "hyperfactions_admin.version.active_compatible";
    public static final String VER_ACTIVE_CLAIMS_ONLY = "hyperfactions_admin.version.active_claims_only";
    public static final String VER_INSTALLED_NO_PERM = "hyperfactions_admin.version.installed_no_perm";
    public static final String VER_ACTIVE_PROVIDER = "hyperfactions_admin.version.active_provider";
    // Admin main page
    public static final String MAIN_RELOAD_HINT = "hyperfactions_admin.main.reload_hint";
    public static final String MAIN_UNCLAIM_HINT = "hyperfactions_admin.main.unclaim_hint";

    // Zone flags/settings (shared)
    public static final String ZFLAGS_INVALID_FLAG = "hyperfactions_admin.zflags.invalid_flag";
    public static final String ZFLAGS_ZONE_NOT_FOUND = "hyperfactions_admin.zflags.zone_not_found";
    public static final String ZFLAGS_CONFLICT = "hyperfactions_admin.zflags.conflict";
    public static final String ZFLAGS_MIXIN = "hyperfactions_admin.zflags.mixin";
    public static final String ZFLAGS_RESET_INT = "hyperfactions_admin.zflags.reset_int";
    public static final String ZFLAGS_RESET_ALL = "hyperfactions_admin.zflags.reset_all";
    public static final String ZFLAGS_RESET_FAILED = "hyperfactions_admin.zflags.reset_failed";
    public static final String ZFLAGS_BACK_TO_SETTINGS = "hyperfactions_admin.zflags.back_to_settings";

    // Zone settings UI labels
    public static final String GUI_ZSET_CAT_COMBAT = "hyperfactions_admin.gui.zset_cat_combat";
    public static final String GUI_ZSET_CAT_DAMAGE = "hyperfactions_admin.gui.zset_cat_damage";
    public static final String GUI_ZSET_CAT_DEATH = "hyperfactions_admin.gui.zset_cat_death";
    public static final String GUI_ZSET_CAT_BUILDING = "hyperfactions_admin.gui.zset_cat_building";
    public static final String GUI_ZSET_CAT_INTERACTION = "hyperfactions_admin.gui.zset_cat_interaction";
    public static final String GUI_ZSET_CAT_TRANSPORT = "hyperfactions_admin.gui.zset_cat_transport";
    public static final String GUI_ZSET_CAT_ITEMS = "hyperfactions_admin.gui.zset_cat_items";
    public static final String GUI_ZSET_CAT_SPAWNING = "hyperfactions_admin.gui.zset_cat_spawning";
    public static final String GUI_ZSET_CAT_MOB_CLEAR = "hyperfactions_admin.gui.zset_cat_mob_clear";
    public static final String GUI_ZSET_CHILDREN_HINT = "hyperfactions_admin.gui.zset_children_hint";
    public static final String GUI_ZSET_RESET_DEFAULTS = "hyperfactions_admin.gui.zset_reset_defaults";
    public static final String GUI_ZSET_INTEGRATION_FLAGS = "hyperfactions_admin.gui.zset_integration_flags";
    public static final String GUI_ZSET_BACK_TO_ZONES = "hyperfactions_admin.gui.zset_back_to_zones";
    public static final String GUI_ZSET_CHUNKS = "hyperfactions_admin.gui.zset_chunks";

    // Zone properties
    public static final String ZPROP_CURRENT_CUSTOM = "hyperfactions_admin.zprop.current_custom";
    public static final String ZPROP_CURRENT_DEFAULT = "hyperfactions_admin.zprop.current_default";
    public static final String ZPROP_PVP_DISABLED = "hyperfactions_admin.zprop.pvp_disabled";
    public static final String ZPROP_PVP_ENABLED = "hyperfactions_admin.zprop.pvp_enabled";
    public static final String ZPROP_NAME_EMPTY = "hyperfactions_admin.zprop.name_empty";
    public static final String ZPROP_RENAMED = "hyperfactions_admin.zprop.renamed";
    public static final String ZPROP_NAME_TAKEN = "hyperfactions_admin.zprop.name_taken";
    public static final String ZPROP_NAME_INVALID = "hyperfactions_admin.zprop.name_invalid";
    public static final String ZPROP_RENAME_FAILED = "hyperfactions_admin.zprop.rename_failed";
    public static final String ZPROP_UPPER_EMPTY = "hyperfactions_admin.zprop.upper_empty";
    public static final String ZPROP_UPPER_SET = "hyperfactions_admin.zprop.upper_set";
    public static final String ZPROP_UPPER_RESET = "hyperfactions_admin.zprop.upper_reset";
    public static final String ZPROP_LOWER_EMPTY = "hyperfactions_admin.zprop.lower_empty";
    public static final String ZPROP_LOWER_SET = "hyperfactions_admin.zprop.lower_set";
    public static final String ZPROP_LOWER_RESET = "hyperfactions_admin.zprop.lower_reset";
    // Relations additional
    public static final String REL_FAILED = "hyperfactions_admin.relations.failed";
    // Members additional
    public static final String MEM_NEVER = "hyperfactions_admin.members.never";
    public static final String MEM_TELEPORTED = "hyperfactions_admin.members.teleported";
    // Member entry labels
    public static final String GUI_MEM_LABEL_POWER = "hyperfactions_admin.gui.mem_label_power";
    public static final String GUI_MEM_LABEL_JOINED = "hyperfactions_admin.gui.mem_label_joined";
    public static final String GUI_MEM_LABEL_LAST_DEATH = "hyperfactions_admin.gui.mem_label_last_death";
    public static final String GUI_MEM_LABEL_UUID = "hyperfactions_admin.gui.mem_label_uuid";
    public static final String GUI_MEM_BTN_INFO = "hyperfactions_admin.gui.mem_btn_info";
    public static final String GUI_MEM_BTN_TELEPORT = "hyperfactions_admin.gui.mem_btn_teleport";
    public static final String GUI_MEM_BTN_PROMOTE = "hyperfactions_admin.gui.mem_btn_promote";
    public static final String GUI_MEM_BTN_DEMOTE = "hyperfactions_admin.gui.mem_btn_demote";
    public static final String GUI_MEM_BTN_KICK = "hyperfactions_admin.gui.mem_btn_kick";
    // Player info additional
    public static final String PLR_RECORDS = "hyperfactions_admin.playerinfo.records";
    public static final String PLR_JOINED_DATE = "hyperfactions_admin.playerinfo.joined_date";
    public static final String PLR_CURRENT = "hyperfactions_admin.playerinfo.current";
    public static final String PLR_LEFT_DATE = "hyperfactions_admin.playerinfo.left_date";
    // Zone map
    public static final String MAP_WORLD_WARNING = "hyperfactions_admin.map.world_warning";
    public static final String MAP_POSITION = "hyperfactions_admin.map.position";
    public static final String MAP_ZONE_GONE = "hyperfactions_admin.map.zone_gone";
    public static final String MAP_CLAIMED = "hyperfactions_admin.map.claimed";
    public static final String MAP_CLAIM_FAILED = "hyperfactions_admin.map.claim_failed";
    public static final String MAP_UNCLAIMED = "hyperfactions_admin.map.unclaimed";
    public static final String MAP_UNCLAIM_FAILED = "hyperfactions_admin.map.unclaim_failed";
    public static final String MAP_CHUNK_BELONGS = "hyperfactions_admin.map.chunk_belongs";
    public static final String MAP_CHUNK_FACTION = "hyperfactions_admin.map.chunk_faction";
    public static final String MAP_CHUNK_PROTECTED = "hyperfactions_admin.map.chunk_protected";
    public static final String MAP_ANOTHER_ZONE = "hyperfactions_admin.map.another_zone";

    // ========== GUI Label Keys (for .ui hardcoded text localization) ==========

    // Page Titles
    public static final String GUI_TITLE_DASHBOARD = "hyperfactions_admin.gui.title_dashboard";
    public static final String GUI_TITLE_MAIN = "hyperfactions_admin.gui.title_main";
    public static final String GUI_TITLE_ACTIONS = "hyperfactions_admin.gui.title_actions";
    public static final String GUI_TITLE_FACTIONS = "hyperfactions_admin.gui.title_factions";
    public static final String GUI_TITLE_PLAYERS = "hyperfactions_admin.gui.title_players";
    public static final String GUI_TITLE_ECONOMY = "hyperfactions_admin.gui.title_economy";
    public static final String GUI_TITLE_ZONES = "hyperfactions_admin.gui.title_zones";
    public static final String GUI_TITLE_BACKUPS = "hyperfactions_admin.gui.title_backups";
    public static final String GUI_TITLE_CONFIG = "hyperfactions_admin.gui.title_config";
    public static final String GUI_TITLE_HELP = "hyperfactions_admin.gui.title_help";
    public static final String GUI_TITLE_UPDATES = "hyperfactions_admin.gui.title_updates";
    public static final String GUI_TITLE_VERSION = "hyperfactions_admin.gui.title_version";
    public static final String GUI_TITLE_ACTIVITY_LOG = "hyperfactions_admin.gui.title_activity_log";
    public static final String GUI_TITLE_PLAYER_INFO = "hyperfactions_admin.gui.title_player_info";
    public static final String GUI_TITLE_FACTION_INFO = "hyperfactions_admin.gui.title_faction_info";
    public static final String GUI_TITLE_FACTION_SETTINGS = "hyperfactions_admin.gui.title_faction_settings";
    public static final String GUI_TITLE_FACTION_MEMBERS = "hyperfactions_admin.gui.title_faction_members";
    public static final String GUI_TITLE_FACTION_RELATIONS = "hyperfactions_admin.gui.title_faction_relations";
    public static final String GUI_TITLE_ZONE_MAP = "hyperfactions_admin.gui.title_zone_map";
    public static final String GUI_TITLE_ZONE_SETTINGS = "hyperfactions_admin.gui.title_zone_settings";
    public static final String GUI_TITLE_ZONE_PROPERTIES = "hyperfactions_admin.gui.title_zone_properties";
    public static final String GUI_TITLE_BULK_ECONOMY = "hyperfactions_admin.gui.title_bulk_economy";
    public static final String GUI_TITLE_ECONOMY_ADJUST = "hyperfactions_admin.gui.title_economy_adjust";

    // Dashboard labels
    public static final String GUI_DASH_SERVER_STATS = "hyperfactions_admin.gui.dash_server_stats";
    public static final String GUI_DASH_FACTIONS = "hyperfactions_admin.gui.dash_factions";
    public static final String GUI_DASH_TOTAL_MEMBERS = "hyperfactions_admin.gui.dash_total_members";
    public static final String GUI_DASH_TOTAL_CLAIMS = "hyperfactions_admin.gui.dash_total_claims";
    public static final String GUI_DASH_ZONES = "hyperfactions_admin.gui.dash_zones";
    public static final String GUI_DASH_SAFE_WAR = "hyperfactions_admin.gui.dash_safe_war";
    public static final String GUI_DASH_TOTAL_POWER = "hyperfactions_admin.gui.dash_total_power";
    public static final String GUI_DASH_AVG_POWER = "hyperfactions_admin.gui.dash_avg_power";
    public static final String GUI_DASH_TOTAL_ECONOMY = "hyperfactions_admin.gui.dash_total_economy";
    public static final String GUI_DASH_WEALTHIEST = "hyperfactions_admin.gui.dash_wealthiest";
    public static final String GUI_DASH_AVG_BALANCE = "hyperfactions_admin.gui.dash_avg_balance";
    public static final String GUI_DASH_PROTECTION_BYPASS = "hyperfactions_admin.gui.dash_protection_bypass";

    // Common buttons and labels
    public static final String GUI_SEARCH = "hyperfactions_admin.gui.search";
    public static final String GUI_SORT = "hyperfactions_admin.gui.sort";
    public static final String GUI_PREV = "hyperfactions_admin.gui.prev";
    public static final String GUI_NEXT = "hyperfactions_admin.gui.next";
    public static final String GUI_DONE = "hyperfactions_admin.gui.done";
    public static final String GUI_APPLY = "hyperfactions_admin.gui.apply";
    public static final String GUI_SET = "hyperfactions_admin.gui.set";
    public static final String GUI_RESET = "hyperfactions_admin.gui.reset";
    public static final String GUI_COMING_SOON = "hyperfactions_admin.gui.coming_soon";
    public static final String GUI_ZONES_BTN = "hyperfactions_admin.gui.zones_btn";
    public static final String GUI_RELOAD_BTN = "hyperfactions_admin.gui.reload_btn";
    public static final String GUI_ALL = "hyperfactions_admin.gui.all";
    public static final String GUI_SAFE = "hyperfactions_admin.gui.safe";
    public static final String GUI_WAR = "hyperfactions_admin.gui.war";
    public static final String GUI_CREATE_ZONE = "hyperfactions_admin.gui.create_zone";

    // Actions page labels
    public static final String GUI_ACT_COMBAT_STATS = "hyperfactions_admin.gui.act_combat_stats";
    public static final String GUI_ACT_COMBAT_DESC = "hyperfactions_admin.gui.act_combat_desc";
    public static final String GUI_ACT_RESET_KD = "hyperfactions_admin.gui.act_reset_kd";
    public static final String GUI_ACT_ECONOMY = "hyperfactions_admin.gui.act_economy";
    public static final String GUI_ACT_ECONOMY_DESC = "hyperfactions_admin.gui.act_economy_desc";
    public static final String GUI_ACT_BULK_ADJUST = "hyperfactions_admin.gui.act_bulk_adjust";
    public static final String GUI_ACT_UPKEEP_COLLECTION = "hyperfactions_admin.gui.act_upkeep_collection";
    public static final String GUI_ACT_UPKEEP_DESC = "hyperfactions_admin.gui.act_upkeep_desc";
    public static final String GUI_ACT_TRIGGER_UPKEEP = "hyperfactions_admin.gui.act_trigger_upkeep";

    // Placeholder page labels
    public static final String GUI_BACKUP_HEADING = "hyperfactions_admin.gui.backup_heading";
    public static final String GUI_BACKUP_DESC1 = "hyperfactions_admin.gui.backup_desc1";
    public static final String GUI_BACKUP_DESC2 = "hyperfactions_admin.gui.backup_desc2";
    // Backups page labels
    public static final String BKP_TITLE = "hyperfactions_admin.backups.title";
    public static final String BKP_TOTAL_COUNT = "hyperfactions_admin.backups.total_count";
    public static final String BKP_EMPTY = "hyperfactions_admin.backups.empty";
    public static final String BKP_BTN_CREATE = "hyperfactions_admin.backups.btn_create";
    public static final String BKP_BTN_RESTORE = "hyperfactions_admin.backups.btn_restore";
    public static final String BKP_BTN_DELETE = "hyperfactions_admin.backups.btn_delete";
    public static final String BKP_NAME_PLACEHOLDER = "hyperfactions_admin.backups.name_placeholder";
    public static final String BKP_CREATING = "hyperfactions_admin.backups.creating";
    public static final String BKP_CREATED = "hyperfactions_admin.backups.created";
    public static final String BKP_CREATE_FAILED = "hyperfactions_admin.backups.create_failed";
    public static final String BKP_TYPE_HOURLY = "hyperfactions_admin.backups.type_hourly";
    public static final String BKP_TYPE_DAILY = "hyperfactions_admin.backups.type_daily";
    public static final String BKP_TYPE_WEEKLY = "hyperfactions_admin.backups.type_weekly";
    public static final String BKP_TYPE_MANUAL = "hyperfactions_admin.backups.type_manual";
    public static final String BKP_TYPE_MIGRATION = "hyperfactions_admin.backups.type_migration";
    public static final String BKP_DETAIL_TYPE = "hyperfactions_admin.backups.detail_type";
    public static final String BKP_DETAIL_CREATED = "hyperfactions_admin.backups.detail_created";
    public static final String BKP_DETAIL_SIZE = "hyperfactions_admin.backups.detail_size";
    public static final String BKP_RESTORE_WARNING = "hyperfactions_admin.backups.restore_warning";
    public static final String BKP_RESTORE_CONFIRM = "hyperfactions_admin.backups.restore_confirm";
    public static final String BKP_RESTORING = "hyperfactions_admin.backups.restoring";
    public static final String BKP_RESTORED = "hyperfactions_admin.backups.restored";
    public static final String BKP_RESTORE_FAILED = "hyperfactions_admin.backups.restore_failed";
    public static final String BKP_DELETE_CONFIRM = "hyperfactions_admin.backups.delete_confirm";
    public static final String BKP_DELETING = "hyperfactions_admin.backups.deleting";
    public static final String BKP_DELETED = "hyperfactions_admin.backups.deleted";
    public static final String BKP_DELETE_FAILED = "hyperfactions_admin.backups.delete_failed";
    public static final String BKP_RELOAD_REQUIRED = "hyperfactions_admin.backups.reload_required";
    public static final String GUI_CONFIG_HEADING = "hyperfactions_admin.gui.config_heading";
    public static final String GUI_CONFIG_DESC1 = "hyperfactions_admin.gui.config_desc1";
    public static final String GUI_CONFIG_DESC2 = "hyperfactions_admin.gui.config_desc2";
    public static final String GUI_HELP_HEADING = "hyperfactions_admin.gui.help_heading";
    public static final String GUI_HELP_DESC1 = "hyperfactions_admin.gui.help_desc1";
    public static final String GUI_HELP_DESC2 = "hyperfactions_admin.gui.help_desc2";
    public static final String GUI_UPDATES_HEADING = "hyperfactions_admin.gui.updates_heading";
    public static final String GUI_UPDATES_DESC1 = "hyperfactions_admin.gui.updates_desc1";
    public static final String GUI_UPDATES_DESC2 = "hyperfactions_admin.gui.updates_desc2";
    // Updates page labels
    public static final String UPD_CURRENT_VERSION = "hyperfactions_admin.updates.current_version";
    public static final String UPD_BUILD_DATE = "hyperfactions_admin.updates.build_date";
    public static final String UPD_LATEST_VERSION = "hyperfactions_admin.updates.latest_version";
    public static final String UPD_STATUS_CHECKING = "hyperfactions_admin.updates.status_checking";
    public static final String UPD_STATUS_UP_TO_DATE = "hyperfactions_admin.updates.status_up_to_date";
    public static final String UPD_STATUS_AVAILABLE = "hyperfactions_admin.updates.status_available";
    public static final String UPD_STATUS_DOWNLOADING = "hyperfactions_admin.updates.status_downloading";
    public static final String UPD_STATUS_DOWNLOADED = "hyperfactions_admin.updates.status_downloaded";
    public static final String UPD_STATUS_FAILED = "hyperfactions_admin.updates.status_failed";
    public static final String UPD_BTN_CHECK = "hyperfactions_admin.updates.btn_check";
    public static final String UPD_BTN_DOWNLOAD = "hyperfactions_admin.updates.btn_download";
    public static final String UPD_BTN_ROLLBACK = "hyperfactions_admin.updates.btn_rollback";
    public static final String UPD_CHANGELOG_TITLE = "hyperfactions_admin.updates.changelog_title";
    public static final String UPD_NO_CHANGELOG = "hyperfactions_admin.updates.no_changelog";
    public static final String UPD_ROLLBACK_CONFIRM = "hyperfactions_admin.updates.rollback_confirm";
    public static final String UPD_ROLLBACK_UNSAFE = "hyperfactions_admin.updates.rollback_unsafe";
    public static final String UPD_ROLLBACK_SUCCESS = "hyperfactions_admin.updates.rollback_success";
    public static final String UPD_ROLLBACK_FAILED = "hyperfactions_admin.updates.rollback_failed";
    public static final String UPD_MIXIN_TITLE = "hyperfactions_admin.updates.mixin_title";
    public static final String UPD_MIXIN_VERSION = "hyperfactions_admin.updates.mixin_version";
    public static final String UPD_MIXIN_CHECK = "hyperfactions_admin.updates.mixin_check";
    public static final String UPD_MIXIN_UP_TO_DATE = "hyperfactions_admin.updates.mixin_up_to_date";
    public static final String UPD_MIXIN_AVAILABLE = "hyperfactions_admin.updates.mixin_available";
    public static final String UPD_MIXIN_DOWNLOADING = "hyperfactions_admin.updates.mixin_downloading";
    public static final String UPD_MIXIN_DOWNLOADED = "hyperfactions_admin.updates.mixin_downloaded";
    public static final String UPD_MIXIN_FAILED = "hyperfactions_admin.updates.mixin_failed";
    public static final String UPD_RESTART_REQUIRED = "hyperfactions_admin.updates.restart_required";
    public static final String UPD_VERSION_INFO = "hyperfactions_admin.updates.version_info";
    public static final String UPD_UPDATE_STATUS = "hyperfactions_admin.updates.update_status";
    public static final String UPD_ROLLBACK_SECTION = "hyperfactions_admin.updates.rollback_section";
    public static final String UPD_PRE_RELEASE = "hyperfactions_admin.updates.pre_release";
    public static final String UPD_CHANNEL = "hyperfactions_admin.updates.channel";
    public static final String UPD_MIXIN_NOT_INSTALLED = "hyperfactions_admin.updates.mixin_not_installed";

    // Version page labels
    public static final String GUI_VER_HYPERFACTIONS = "hyperfactions_admin.gui.ver_hyperfactions";
    public static final String GUI_VER_HYTALE_SERVER = "hyperfactions_admin.gui.ver_hytale_server";
    public static final String GUI_VER_JAVA = "hyperfactions_admin.gui.ver_java";
    public static final String GUI_VER_PERMISSIONS = "hyperfactions_admin.gui.ver_permissions";
    public static final String GUI_VER_PLACEHOLDERS = "hyperfactions_admin.gui.ver_placeholders";
    public static final String GUI_VER_ECONOMY_SECTION = "hyperfactions_admin.gui.ver_economy_section";
    public static final String GUI_VER_PROTECTION = "hyperfactions_admin.gui.ver_protection";
    public static final String GUI_VER_DISABLED = "hyperfactions_admin.gui.ver_disabled";

    // Column headers (shared across pages)
    public static final String GUI_COL_FACTION = "hyperfactions_admin.gui.col_faction";
    public static final String GUI_COL_BALANCE = "hyperfactions_admin.gui.col_balance";
    public static final String GUI_COL_MEMBERS = "hyperfactions_admin.gui.col_members";
    public static final String GUI_COL_ACTIONS = "hyperfactions_admin.gui.col_actions";
    public static final String GUI_COL_TIME = "hyperfactions_admin.gui.col_time";
    public static final String GUI_COL_TYPE = "hyperfactions_admin.gui.col_type";
    public static final String GUI_COL_MESSAGE = "hyperfactions_admin.gui.col_message";

    // Economy page labels
    public static final String GUI_ECON_TOTAL_BALANCE = "hyperfactions_admin.gui.econ_total_balance";
    public static final String GUI_ECON_FACTIONS = "hyperfactions_admin.gui.econ_factions";
    public static final String GUI_ECON_AVG_BALANCE = "hyperfactions_admin.gui.econ_avg_balance";
    public static final String GUI_ECON_IN_GRACE = "hyperfactions_admin.gui.econ_in_grace";
    public static final String GUI_ECON_COLLECTED = "hyperfactions_admin.gui.econ_collected";
    public static final String GUI_ECON_NEXT_COLLECTION = "hyperfactions_admin.gui.econ_next_collection";
    public static final String GUI_ECON_NO_DATA = "hyperfactions_admin.gui.econ_no_data";

    // Activity log labels
    public static final String GUI_LOG_TYPE = "hyperfactions_admin.gui.log_type";
    public static final String GUI_LOG_TIME = "hyperfactions_admin.gui.log_time";
    public static final String GUI_LOG_PLAYER = "hyperfactions_admin.gui.log_player";
    public static final String GUI_LOG_NO_LOGS = "hyperfactions_admin.gui.log_no_logs";

    // Player info labels
    public static final String GUI_PLR_FIRST_JOINED = "hyperfactions_admin.gui.plr_first_joined";
    public static final String GUI_PLR_LAST_ONLINE = "hyperfactions_admin.gui.plr_last_online";
    public static final String GUI_PLR_UUID = "hyperfactions_admin.gui.plr_uuid";
    public static final String GUI_PLR_FACTION = "hyperfactions_admin.gui.plr_faction";
    public static final String GUI_PLR_ROLE = "hyperfactions_admin.gui.plr_role";
    public static final String GUI_PLR_VIEW_FACTION = "hyperfactions_admin.gui.plr_view_faction";
    public static final String GUI_PLR_POWER = "hyperfactions_admin.gui.plr_power";
    public static final String GUI_PLR_MAX_POWER = "hyperfactions_admin.gui.plr_max_power";
    public static final String GUI_PLR_SET_POWER = "hyperfactions_admin.gui.plr_set_power";
    public static final String GUI_PLR_RESET_POWER = "hyperfactions_admin.gui.plr_reset_power";
    public static final String GUI_PLR_SET_MAX = "hyperfactions_admin.gui.plr_set_max";
    public static final String GUI_PLR_RESET_MAX = "hyperfactions_admin.gui.plr_reset_max";
    public static final String GUI_PLR_NO_POWER_LOSS = "hyperfactions_admin.gui.plr_no_power_loss";
    public static final String GUI_PLR_NO_CLAIM_DECAY = "hyperfactions_admin.gui.plr_no_claim_decay";
    public static final String GUI_PLR_KILLS = "hyperfactions_admin.gui.plr_kills";
    public static final String GUI_PLR_DEATHS = "hyperfactions_admin.gui.plr_deaths";
    public static final String GUI_PLR_KDR = "hyperfactions_admin.gui.plr_kdr";
    public static final String GUI_PLR_RESET_KD = "hyperfactions_admin.gui.plr_reset_kd";
    public static final String GUI_PLR_KICK = "hyperfactions_admin.gui.plr_kick";
    public static final String GUI_PLR_MEMBERSHIP_HISTORY = "hyperfactions_admin.gui.plr_membership_history";
    public static final String GUI_PLR_NO_FACTION = "hyperfactions_admin.gui.plr_no_faction_label";
    public static final String GUI_PLR_POWER_MANAGEMENT = "hyperfactions_admin.gui.plr_power_management";
    public static final String GUI_PLR_COMBAT_STATS = "hyperfactions_admin.gui.plr_combat_stats";
    public static final String GUI_PLR_BYPASS_FLAGS = "hyperfactions_admin.gui.plr_bypass_flags";
    public static final String GUI_PLR_ADMIN_CONTROLS = "hyperfactions_admin.gui.plr_admin_controls";
    public static final String GUI_PLR_KD_SUBTITLE = "hyperfactions_admin.gui.plr_kd_subtitle";
    public static final String GUI_PLR_MAX_PREFIX = "hyperfactions_admin.gui.plr_max_prefix";
    public static final String GUI_PLR_VIEW = "hyperfactions_admin.gui.plr_view";
    public static final String GUI_PLR_KICK_FROM_FACTION = "hyperfactions_admin.gui.plr_kick_from_faction";
    public static final String GUI_PLR_SET_MAX_BTN = "hyperfactions_admin.gui.plr_set_max_btn";
    public static final String GUI_PLR_COMBAT = "hyperfactions_admin.gui.plr_combat";
    // Player info history reason labels
    public static final String GUI_PLR_REASON_ACTIVE = "hyperfactions_admin.gui.plr_reason_active";
    public static final String GUI_PLR_REASON_LEFT = "hyperfactions_admin.gui.plr_reason_left";
    public static final String GUI_PLR_REASON_KICKED = "hyperfactions_admin.gui.plr_reason_kicked";
    public static final String GUI_PLR_REASON_DISBANDED = "hyperfactions_admin.gui.plr_reason_disbanded";

    // Faction info labels
    public static final String GUI_FAC_DESCRIPTION = "hyperfactions_admin.gui.fac_description";
    public static final String GUI_FAC_POWER = "hyperfactions_admin.gui.fac_power";
    public static final String GUI_FAC_CLAIMS = "hyperfactions_admin.gui.fac_claims";
    public static final String GUI_FAC_MEMBERS = "hyperfactions_admin.gui.fac_members";
    public static final String GUI_FAC_RECRUITMENT = "hyperfactions_admin.gui.fac_recruitment";
    public static final String GUI_FAC_FOUNDED = "hyperfactions_admin.gui.fac_founded";
    public static final String GUI_FAC_ALLIES = "hyperfactions_admin.gui.fac_allies";
    public static final String GUI_FAC_ENEMIES = "hyperfactions_admin.gui.fac_enemies";
    public static final String GUI_FAC_RAIDABLE = "hyperfactions_admin.gui.fac_raidable";
    public static final String GUI_FAC_TREASURY = "hyperfactions_admin.gui.fac_treasury";
    public static final String GUI_FAC_LEADER = "hyperfactions_admin.gui.fac_leader";
    public static final String GUI_FAC_OFFICERS = "hyperfactions_admin.gui.fac_officers";
    public static final String GUI_FAC_VIEW_MEMBERS = "hyperfactions_admin.gui.fac_view_members";
    public static final String GUI_FAC_VIEW_RELATIONS = "hyperfactions_admin.gui.fac_view_relations";
    public static final String GUI_FAC_VIEW_SETTINGS = "hyperfactions_admin.gui.fac_view_settings";
    public static final String GUI_FAC_DISBAND = "hyperfactions_admin.gui.fac_disband";
    public static final String GUI_FAC_POWER_MANAGEMENT = "hyperfactions_admin.gui.fac_power_management";
    public static final String GUI_FAC_RESET_ALL_POWER = "hyperfactions_admin.gui.fac_reset_all_power";
    public static final String GUI_FAC_ECON_ADJUST = "hyperfactions_admin.gui.fac_econ_adjust";
    public static final String GUI_FAC_ECON_VIEW_LOG = "hyperfactions_admin.gui.fac_econ_view_log";
    public static final String GUI_FAC_CURRENT_MAX = "hyperfactions_admin.gui.fac_current_max";
    public static final String GUI_FAC_CLAIMED_MAX = "hyperfactions_admin.gui.fac_claimed_max";
    public static final String GUI_FAC_RELATIONS = "hyperfactions_admin.gui.fac_relations";
    public static final String GUI_FAC_ALLY_ENEMY = "hyperfactions_admin.gui.fac_ally_enemy";
    public static final String GUI_FAC_STATUS = "hyperfactions_admin.gui.fac_status";
    public static final String GUI_FAC_INFO = "hyperfactions_admin.gui.fac_info";
    public static final String GUI_FAC_TREASURY_BALANCE = "hyperfactions_admin.gui.fac_treasury_balance";
    public static final String GUI_FAC_LEADERSHIP = "hyperfactions_admin.gui.fac_leadership";
    public static final String GUI_FAC_LEADER_LABEL = "hyperfactions_admin.gui.fac_leader_label";
    public static final String GUI_FAC_OFFICERS_LABEL = "hyperfactions_admin.gui.fac_officers_label";
    public static final String GUI_FAC_ECON_MGMT = "hyperfactions_admin.gui.fac_econ_mgmt";
    public static final String GUI_FAC_DANGER_ZONE = "hyperfactions_admin.gui.fac_danger_zone";
    public static final String GUI_FAC_VIEW_TREASURY = "hyperfactions_admin.gui.fac_view_treasury";

    // Faction settings labels
    public static final String GUI_SET_EDITING = "hyperfactions_admin.gui.set_editing";
    public static final String GUI_SET_GENERAL = "hyperfactions_admin.gui.set_general";
    public static final String GUI_SET_NAME = "hyperfactions_admin.gui.set_name";
    public static final String GUI_SET_TAG = "hyperfactions_admin.gui.set_tag";
    public static final String GUI_SET_DESCRIPTION = "hyperfactions_admin.gui.set_description";
    public static final String GUI_SET_RECRUITMENT = "hyperfactions_admin.gui.set_recruitment";
    public static final String GUI_SET_HOME = "hyperfactions_admin.gui.set_home";
    public static final String GUI_SET_CLEAR_HOME = "hyperfactions_admin.gui.set_clear_home";
    public static final String GUI_SET_DISBAND_FACTION = "hyperfactions_admin.gui.set_disband_faction";
    public static final String GUI_SET_FACTION_COLOR = "hyperfactions_admin.gui.set_faction_color";
    public static final String GUI_SET_ADMIN_OVERRIDE = "hyperfactions_admin.gui.set_admin_override";
    public static final String GUI_SET_TERRITORY_PERMS = "hyperfactions_admin.gui.set_territory_perms";
    public static final String GUI_SET_MOB_SPAWNING = "hyperfactions_admin.gui.set_mob_spawning";
    public static final String GUI_SET_FACTION_SETTINGS = "hyperfactions_admin.gui.set_faction_settings";
    public static final String GUI_SET_NAME_LABEL = "hyperfactions_admin.gui.set_name_label";
    public static final String GUI_SET_TAG_LABEL = "hyperfactions_admin.gui.set_tag_label";
    public static final String GUI_SET_DESC_LABEL = "hyperfactions_admin.gui.set_desc_label";
    public static final String GUI_SET_EDIT = "hyperfactions_admin.gui.set_edit";
    public static final String GUI_SET_STATUS_LABEL = "hyperfactions_admin.gui.set_status_label";
    public static final String GUI_SET_LOCATION_LABEL = "hyperfactions_admin.gui.set_location_label";
    public static final String GUI_SET_DANGER_ZONE = "hyperfactions_admin.gui.set_danger_zone";
    public static final String GUI_SET_IRREVERSIBLE = "hyperfactions_admin.gui.set_irreversible";
    public static final String GUI_SET_LOCK_HINT = "hyperfactions_admin.gui.set_lock_hint";
    public static final String GUI_SET_APPEARANCE = "hyperfactions_admin.gui.set_appearance";
    public static final String GUI_SET_COLOR_LABEL = "hyperfactions_admin.gui.set_color_label";
    public static final String GUI_SET_MOB_SUB = "hyperfactions_admin.gui.set_mob_sub";
    public static final String GUI_SET_BACK_TO_INFO = "hyperfactions_admin.gui.set_back_to_info";
    public static final String GUI_SET_COL_OUT = "hyperfactions_admin.gui.set_col_out";
    public static final String GUI_SET_COL_ALLY = "hyperfactions_admin.gui.set_col_ally";
    public static final String GUI_SET_COL_MEM = "hyperfactions_admin.gui.set_col_mem";
    public static final String GUI_SET_COL_OFF = "hyperfactions_admin.gui.set_col_off";
    public static final String GUI_SET_CAT_BUILDING = "hyperfactions_admin.gui.set_cat_building";
    public static final String GUI_SET_CAT_INTERACTION = "hyperfactions_admin.gui.set_cat_interaction";
    public static final String GUI_SET_CAT_INTERACT_SUB = "hyperfactions_admin.gui.set_cat_interact_sub";
    public static final String GUI_SET_CAT_OTHER = "hyperfactions_admin.gui.set_cat_other";
    public static final String GUI_SET_PERM_BREAK = "hyperfactions_admin.gui.set_perm_break";
    public static final String GUI_SET_PERM_PLACE = "hyperfactions_admin.gui.set_perm_place";
    public static final String GUI_SET_PERM_ALL = "hyperfactions_admin.gui.set_perm_all";
    public static final String GUI_SET_PERM_DOOR = "hyperfactions_admin.gui.set_perm_door";
    public static final String GUI_SET_PERM_CHEST = "hyperfactions_admin.gui.set_perm_chest";
    public static final String GUI_SET_PERM_BENCH = "hyperfactions_admin.gui.set_perm_bench";
    public static final String GUI_SET_PERM_PROCESSING = "hyperfactions_admin.gui.set_perm_processing";
    public static final String GUI_SET_PERM_SEAT = "hyperfactions_admin.gui.set_perm_seat";
    public static final String GUI_SET_PERM_TRANSPORT = "hyperfactions_admin.gui.set_perm_transport";
    public static final String GUI_SET_PERM_CRATE_USE = "hyperfactions_admin.gui.set_perm_crate_use";
    public static final String GUI_SET_PERM_NPC_TAME = "hyperfactions_admin.gui.set_perm_npc_tame";
    public static final String GUI_SET_PERM_PVE_DAMAGE = "hyperfactions_admin.gui.set_perm_pve_damage";
    public static final String GUI_SET_PERM_MOB_SPAWNING = "hyperfactions_admin.gui.set_perm_mob_spawning";
    public static final String GUI_SET_PERM_HOSTILE = "hyperfactions_admin.gui.set_perm_hostile";
    public static final String GUI_SET_PERM_PASSIVE = "hyperfactions_admin.gui.set_perm_passive";
    public static final String GUI_SET_PERM_NEUTRAL = "hyperfactions_admin.gui.set_perm_neutral";
    public static final String GUI_SET_PERM_PVP = "hyperfactions_admin.gui.set_perm_pvp";
    public static final String GUI_SET_PERM_OFFICERS_EDIT = "hyperfactions_admin.gui.set_perm_officers_edit";

    // Faction relations labels
    public static final String GUI_REL_SUBTITLE = "hyperfactions_admin.gui.rel_subtitle";
    public static final String GUI_REL_SET_NEW = "hyperfactions_admin.gui.rel_set_new";
    public static final String GUI_REL_BTN_ALLY = "hyperfactions_admin.gui.rel_btn_ally";
    public static final String GUI_REL_BTN_NEUTRAL = "hyperfactions_admin.gui.rel_btn_neutral";
    public static final String GUI_REL_BTN_ENEMY = "hyperfactions_admin.gui.rel_btn_enemy";

    // Zone page labels
    public static final String GUI_ZONE_SORT_NAME = "hyperfactions_admin.gui.zone_sort_name";
    public static final String GUI_ZONE_SORT_TYPE = "hyperfactions_admin.gui.zone_sort_type";
    public static final String GUI_ZONE_SORT_CHUNKS = "hyperfactions_admin.gui.zone_sort_chunks";
    public static final String GUI_ZONE_SORT_WORLD = "hyperfactions_admin.gui.zone_sort_world";
    public static final String GUI_ZONE_COUNT_FORMAT = "hyperfactions_admin.gui.zone_count_format";

    // Zone map labels
    public static final String GUI_MAP_ZONE_CHUNK = "hyperfactions_admin.gui.map_zone_chunk";
    public static final String GUI_MAP_EMPTY = "hyperfactions_admin.gui.map_empty";
    public static final String GUI_MAP_OTHER_ZONE = "hyperfactions_admin.gui.map_other_zone";
    public static final String GUI_MAP_FACTION_CLAIM = "hyperfactions_admin.gui.map_faction_claim";
    public static final String GUI_MAP_PROTECTED = "hyperfactions_admin.gui.map_protected";
    public static final String GUI_MAP_YOUR_POS = "hyperfactions_admin.gui.map_your_pos";
    public static final String GUI_MAP_CLICK_HINT = "hyperfactions_admin.gui.map_click_hint";
    public static final String GUI_MAP_LEGEND_ZONE_SAFE = "hyperfactions_admin.gui.map_legend_zone_safe";
    public static final String GUI_MAP_LEGEND_ZONE_WAR = "hyperfactions_admin.gui.map_legend_zone_war";
    public static final String GUI_MAP_LEGEND_OTHER_SAFE = "hyperfactions_admin.gui.map_legend_other_safe";
    public static final String GUI_MAP_LEGEND_OTHER_WAR = "hyperfactions_admin.gui.map_legend_other_war";
    public static final String GUI_MAP_LEGEND_FACTION = "hyperfactions_admin.gui.map_legend_faction";
    public static final String GUI_MAP_LEGEND_UNCLAIMED = "hyperfactions_admin.gui.map_legend_unclaimed";
    public static final String GUI_MAP_LEGEND_YOU_HERE = "hyperfactions_admin.gui.map_legend_you_here";
    public static final String GUI_MAP_ACTION_HINT = "hyperfactions_admin.gui.map_action_hint";
    public static final String GUI_MAP_DONE = "hyperfactions_admin.gui.map_done";

    // Zone properties labels
    public static final String GUI_ZPROP_GENERAL = "hyperfactions_admin.gui.zprop_general";
    public static final String GUI_ZPROP_ZONE_NAME = "hyperfactions_admin.gui.zprop_zone_name";
    public static final String GUI_ZPROP_ZONE_TYPE = "hyperfactions_admin.gui.zprop_zone_type";
    public static final String GUI_ZPROP_CHANGE_TYPE = "hyperfactions_admin.gui.zprop_change_type";
    public static final String GUI_ZPROP_NOTIFICATIONS = "hyperfactions_admin.gui.zprop_notifications";
    public static final String GUI_ZPROP_SHOW_ENTRY = "hyperfactions_admin.gui.zprop_show_entry";
    public static final String GUI_ZPROP_UPPER_TITLE = "hyperfactions_admin.gui.zprop_upper_title";
    public static final String GUI_ZPROP_UPPER_DESC = "hyperfactions_admin.gui.zprop_upper_desc";
    public static final String GUI_ZPROP_LOWER_TITLE = "hyperfactions_admin.gui.zprop_lower_title";
    public static final String GUI_ZPROP_LOWER_DESC = "hyperfactions_admin.gui.zprop_lower_desc";
    public static final String GUI_ZPROP_EDIT_FLAGS = "hyperfactions_admin.gui.zprop_edit_flags";
    public static final String GUI_ZPROP_BACK_TO_ZONES = "hyperfactions_admin.gui.zprop_back_to_zones";

    // Bulk economy labels
    public static final String GUI_BULK_HEADER = "hyperfactions_admin.gui.bulk_header";
    public static final String GUI_BULK_FACTIONS_LABEL = "hyperfactions_admin.gui.bulk_factions_label";
    public static final String GUI_BULK_TOTAL_LABEL = "hyperfactions_admin.gui.bulk_total_label";
    public static final String GUI_BULK_AMOUNT_HINT = "hyperfactions_admin.gui.bulk_amount_hint";
    public static final String GUI_BULK_HINT = "hyperfactions_admin.gui.bulk_hint";
    public static final String GUI_BULK_WARNING_MSG = "hyperfactions_admin.gui.bulk_warning_msg";
    public static final String GUI_BULK_APPLY_ALL = "hyperfactions_admin.gui.bulk_apply_all";
    public static final String GUI_BULK_OPERATION = "hyperfactions_admin.gui.bulk_operation";
    public static final String GUI_BULK_ADD = "hyperfactions_admin.gui.bulk_add";
    public static final String GUI_BULK_REMOVE = "hyperfactions_admin.gui.bulk_remove";
    public static final String GUI_BULK_AMOUNT = "hyperfactions_admin.gui.bulk_amount";
    public static final String GUI_BULK_WARNING = "hyperfactions_admin.gui.bulk_warning";
    public static final String GUI_BULK_PREVIEW = "hyperfactions_admin.gui.bulk_preview";

    // Economy adjust labels
    public static final String GUI_ECADJ_HEADER = "hyperfactions_admin.gui.ecadj_header";
    public static final String GUI_ECADJ_FACTION_LABEL = "hyperfactions_admin.gui.ecadj_faction_label";
    public static final String GUI_ECADJ_CURRENT_BALANCE = "hyperfactions_admin.gui.ecadj_current_balance";
    public static final String GUI_ECADJ_AMOUNT_HINT = "hyperfactions_admin.gui.ecadj_amount_hint";
    public static final String GUI_ECADJ_PREVIEW_HINT = "hyperfactions_admin.gui.ecadj_preview_hint";
    public static final String GUI_ECADJ_ADJUSTMENT = "hyperfactions_admin.gui.ecadj_adjustment";
    public static final String GUI_ECADJ_SET_BALANCE = "hyperfactions_admin.gui.ecadj_set_balance";
    public static final String GUI_ECADJ_CONFIRM = "hyperfactions_admin.gui.ecadj_confirm";
    public static final String GUI_ECADJ_OPERATION = "hyperfactions_admin.gui.ecadj_operation";
    public static final String GUI_ECADJ_ADD = "hyperfactions_admin.gui.ecadj_add";
    public static final String GUI_ECADJ_REMOVE = "hyperfactions_admin.gui.ecadj_remove";
    public static final String GUI_ECADJ_SET_TO = "hyperfactions_admin.gui.ecadj_set_to";
    public static final String GUI_ECADJ_AMOUNT = "hyperfactions_admin.gui.ecadj_amount";
    public static final String GUI_ECADJ_NEW_BALANCE = "hyperfactions_admin.gui.ecadj_new_balance";

    // Version page integration labels
    public static final String GUI_VER_HYPERPERMS = "hyperfactions_admin.gui.ver_hyperperms";
    public static final String GUI_VER_LUCKPERMS = "hyperfactions_admin.gui.ver_luckperms";
    public static final String GUI_VER_VAULT = "hyperfactions_admin.gui.ver_vault";
    public static final String GUI_VER_NATIVE = "hyperfactions_admin.gui.ver_native";
    public static final String GUI_VER_HYPERPROTECT = "hyperfactions_admin.gui.ver_hyperprotect";
    public static final String GUI_VER_ORBISGUARD_MIXINS = "hyperfactions_admin.gui.ver_orbisguard_mixins";
    public static final String GUI_VER_ORBISGUARD_API = "hyperfactions_admin.gui.ver_orbisguard_api";
    public static final String GUI_VER_MIXIN_HOOKS = "hyperfactions_admin.gui.ver_mixin_hooks";
    public static final String GUI_VER_GRAVESTONES = "hyperfactions_admin.gui.ver_gravestones";
    public static final String GUI_VER_KYUUBISOFT = "hyperfactions_admin.gui.ver_kyuubisoft";
    public static final String GUI_VER_PLACEHOLDER_API = "hyperfactions_admin.gui.ver_placeholder_api";
    public static final String GUI_VER_WIFLOW_PAPI = "hyperfactions_admin.gui.ver_wiflow_papi";
    public static final String GUI_VER_TREASURY = "hyperfactions_admin.gui.ver_treasury";

    // Unclaim all confirm modal labels
    public static final String GUI_UNCLAIM_TITLE = "hyperfactions_admin.gui.unclaim_title";
    public static final String GUI_UNCLAIM_CONFIRM_MSG1 = "hyperfactions_admin.gui.unclaim_confirm_msg1";
    public static final String GUI_UNCLAIM_CONFIRM_MSG2 = "hyperfactions_admin.gui.unclaim_confirm_msg2";
    public static final String GUI_UNCLAIM_WARNING = "hyperfactions_admin.gui.unclaim_warning";
    public static final String GUI_UNCLAIM_ALL = "hyperfactions_admin.gui.unclaim_all";

    // Zone rename modal labels
    public static final String GUI_ZREN_TITLE = "hyperfactions_admin.gui.zren_title";
    public static final String GUI_ZREN_CURRENT = "hyperfactions_admin.gui.zren_current";
    public static final String GUI_ZREN_NEW_NAME = "hyperfactions_admin.gui.zren_new_name";

    // Zone change type modal labels
    public static final String GUI_ZTYPE_TITLE = "hyperfactions_admin.gui.ztype_title";
    public static final String GUI_ZTYPE_ZONE_LABEL = "hyperfactions_admin.gui.ztype_zone_label";
    public static final String GUI_ZTYPE_CURRENT = "hyperfactions_admin.gui.ztype_current";
    public static final String GUI_ZTYPE_WILL_BECOME = "hyperfactions_admin.gui.ztype_will_become";
    public static final String GUI_ZTYPE_NEW = "hyperfactions_admin.gui.ztype_new";
    public static final String GUI_ZTYPE_WARNING1 = "hyperfactions_admin.gui.ztype_warning1";
    public static final String GUI_ZTYPE_WARNING2 = "hyperfactions_admin.gui.ztype_warning2";
    public static final String GUI_ZTYPE_KEEP_DESC = "hyperfactions_admin.gui.ztype_keep_desc";
    public static final String GUI_ZTYPE_KEEP_FLAGS = "hyperfactions_admin.gui.ztype_keep_flags";
    public static final String GUI_ZTYPE_RESET_DESC = "hyperfactions_admin.gui.ztype_reset_desc";
    public static final String GUI_ZTYPE_RESET_FLAGS = "hyperfactions_admin.gui.ztype_reset_flags";

    // Create zone wizard labels
    public static final String GUI_CZW_TITLE = "hyperfactions_admin.gui.czw_title";
    public static final String GUI_CZW_BACK = "hyperfactions_admin.gui.czw_back";
    public static final String GUI_CZW_CREATE = "hyperfactions_admin.gui.czw_create";
    public static final String GUI_CZW_ZONE_TYPE = "hyperfactions_admin.gui.czw_zone_type";
    public static final String GUI_CZW_SAFE_DESC = "hyperfactions_admin.gui.czw_safe_desc";
    public static final String GUI_CZW_WAR_DESC = "hyperfactions_admin.gui.czw_war_desc";
    public static final String GUI_CZW_ZONE_NAME = "hyperfactions_admin.gui.czw_zone_name";
    public static final String GUI_CZW_NAME_DESC = "hyperfactions_admin.gui.czw_name_desc";
    public static final String GUI_CZW_CLAIM_METHOD = "hyperfactions_admin.gui.czw_claim_method";
    public static final String GUI_CZW_METHOD_NONE_DESC = "hyperfactions_admin.gui.czw_method_none_desc";
    public static final String GUI_CZW_METHOD_NONE = "hyperfactions_admin.gui.czw_method_none";
    public static final String GUI_CZW_METHOD_SINGLE_DESC = "hyperfactions_admin.gui.czw_method_single_desc";
    public static final String GUI_CZW_METHOD_SINGLE = "hyperfactions_admin.gui.czw_method_single";
    public static final String GUI_CZW_METHOD_CIRCLE_DESC = "hyperfactions_admin.gui.czw_method_circle_desc";
    public static final String GUI_CZW_METHOD_CIRCLE = "hyperfactions_admin.gui.czw_method_circle";
    public static final String GUI_CZW_METHOD_SQUARE_DESC = "hyperfactions_admin.gui.czw_method_square_desc";
    public static final String GUI_CZW_METHOD_SQUARE = "hyperfactions_admin.gui.czw_method_square";
    public static final String GUI_CZW_METHOD_MAP_DESC = "hyperfactions_admin.gui.czw_method_map_desc";
    public static final String GUI_CZW_METHOD_MAP = "hyperfactions_admin.gui.czw_method_map";
    public static final String GUI_CZW_RADIUS = "hyperfactions_admin.gui.czw_radius";
    public static final String GUI_CZW_CUSTOM_RADIUS = "hyperfactions_admin.gui.czw_custom_radius";
    public static final String GUI_CZW_FLAGS = "hyperfactions_admin.gui.czw_flags";
    public static final String GUI_CZW_FLAGS_DEFAULTS_DESC = "hyperfactions_admin.gui.czw_flags_defaults_desc";
    public static final String GUI_CZW_FLAGS_DEFAULTS = "hyperfactions_admin.gui.czw_flags_defaults";
    public static final String GUI_CZW_FLAGS_CUSTOMIZE_DESC = "hyperfactions_admin.gui.czw_flags_customize_desc";
    public static final String GUI_CZW_FLAGS_CUSTOMIZE = "hyperfactions_admin.gui.czw_flags_customize";

    // Config editor page labels
    public static final String CFG_TAB_SERVER = "hyperfactions_admin.config.tab_server";
    public static final String CFG_TAB_CHAT = "hyperfactions_admin.config.tab_chat";
    public static final String CFG_TAB_ANNOUNCEMENTS = "hyperfactions_admin.config.tab_announcements";
    public static final String CFG_TAB_ECONOMY = "hyperfactions_admin.config.tab_economy";
    public static final String CFG_TAB_FACTIONS = "hyperfactions_admin.config.tab_factions";
    public static final String CFG_TAB_FACTION_PERMS = "hyperfactions_admin.config.tab_faction_perms";
    public static final String CFG_TAB_WORLDMAP = "hyperfactions_admin.config.tab_worldmap";
    public static final String CFG_TAB_WORLDS = "hyperfactions_admin.config.tab_worlds";
    public static final String CFG_TAB_BACKUP = "hyperfactions_admin.config.tab_backup";
    public static final String CFG_TAB_DEBUG = "hyperfactions_admin.config.tab_debug";
    public static final String CFG_TAB_GRAVESTONES = "hyperfactions_admin.config.tab_gravestones";
    public static final String CFG_CHANGES_PENDING = "hyperfactions_admin.config.changes_pending";
    public static final String CFG_NO_CHANGES = "hyperfactions_admin.config.no_changes";
    public static final String CFG_BTN_SAVE = "hyperfactions_admin.config.btn_save";
    public static final String CFG_BTN_REVERT = "hyperfactions_admin.config.btn_revert";
    public static final String CFG_BTN_RESET = "hyperfactions_admin.config.btn_reset";
    public static final String CFG_SAVED = "hyperfactions_admin.config.saved";
    public static final String CFG_REVERTED = "hyperfactions_admin.config.reverted";
    public static final String CFG_RESET_CONFIRM = "hyperfactions_admin.config.reset_confirm";
    public static final String CFG_RESET_DONE = "hyperfactions_admin.config.reset_done";
    // Config section headers
    public static final String CFG_SEC_TELEPORT = "hyperfactions_admin.config.sec_teleport";
    public static final String CFG_SEC_AUTOSAVE = "hyperfactions_admin.config.sec_autosave";
    public static final String CFG_SEC_MESSAGES = "hyperfactions_admin.config.sec_messages";
    public static final String CFG_SEC_GUI = "hyperfactions_admin.config.sec_gui";
    public static final String CFG_SEC_PERMISSIONS = "hyperfactions_admin.config.sec_permissions";
    public static final String CFG_SEC_LANGUAGE = "hyperfactions_admin.config.sec_language";
    public static final String CFG_SEC_MOB_CLEAR = "hyperfactions_admin.config.sec_mob_clear";
    public static final String CFG_SEC_UPDATES = "hyperfactions_admin.config.sec_updates";
    public static final String CFG_SEC_FACTION_LIMITS = "hyperfactions_admin.config.sec_faction_limits";
    public static final String CFG_SEC_POWER = "hyperfactions_admin.config.sec_power";
    public static final String CFG_SEC_POWER_LOSS = "hyperfactions_admin.config.sec_power_loss";
    public static final String CFG_SEC_REGEN = "hyperfactions_admin.config.sec_regen";
    public static final String CFG_SEC_CLAIMS = "hyperfactions_admin.config.sec_claims";
    public static final String CFG_SEC_DECAY = "hyperfactions_admin.config.sec_decay";
    public static final String CFG_SEC_PROTECTION = "hyperfactions_admin.config.sec_protection";
    public static final String CFG_SEC_COMBAT_TAG = "hyperfactions_admin.config.sec_combat_tag";
    public static final String CFG_SEC_FRIENDLY_FIRE = "hyperfactions_admin.config.sec_friendly_fire";
    public static final String CFG_SEC_SPAWN_PROT = "hyperfactions_admin.config.sec_spawn_prot";
    public static final String CFG_SEC_RELATIONS = "hyperfactions_admin.config.sec_relations";
    public static final String CFG_SEC_INVITES = "hyperfactions_admin.config.sec_invites";
    public static final String CFG_SEC_STUCK = "hyperfactions_admin.config.sec_stuck";
    public static final String CFG_SEC_FORMAT = "hyperfactions_admin.config.sec_format";
    public static final String CFG_SEC_COLORS = "hyperfactions_admin.config.sec_colors";
    public static final String CFG_SEC_REL_COLORS = "hyperfactions_admin.config.sec_rel_colors";
    public static final String CFG_SEC_FACTION_CHAT = "hyperfactions_admin.config.sec_faction_chat";
    public static final String CFG_SEC_HISTORY = "hyperfactions_admin.config.sec_history";
    public static final String CFG_SEC_BACKUP = "hyperfactions_admin.config.sec_backup";
    public static final String CFG_SEC_ECONOMY = "hyperfactions_admin.config.sec_economy";
    public static final String CFG_SEC_ANNOUNCE = "hyperfactions_admin.config.sec_announcements";
    public static final String CFG_SEC_MAP_DISPLAY = "hyperfactions_admin.config.sec_map_display";
    public static final String CFG_SEC_VISIBILITY = "hyperfactions_admin.config.sec_visibility";
    public static final String CFG_SEC_MIXIN = "hyperfactions_admin.config.sec_mixin";
    public static final String CFG_SEC_TERRITORY_NOTIFY = "hyperfactions_admin.config.sec_territory_notify";
    public static final String CFG_SEC_WILDERNESS = "hyperfactions_admin.config.sec_wilderness";
    public static final String CFG_SEC_CURRENCY = "hyperfactions_admin.config.sec_currency";
    public static final String CFG_SEC_TREASURY_LIMITS = "hyperfactions_admin.config.sec_treasury_limits";
    public static final String CFG_SEC_FEES = "hyperfactions_admin.config.sec_fees";
    public static final String CFG_SEC_UPKEEP = "hyperfactions_admin.config.sec_upkeep";
    public static final String CFG_SEC_PROXIMITY = "hyperfactions_admin.config.sec_proximity";
    public static final String CFG_SEC_INCREMENTAL = "hyperfactions_admin.config.sec_incremental";
    public static final String CFG_SEC_DEBOUNCED = "hyperfactions_admin.config.sec_debounced";
    public static final String CFG_SEC_DEBUG_GLOBAL = "hyperfactions_admin.config.sec_debug_global";
    public static final String CFG_SEC_DEBUG_CATEGORIES = "hyperfactions_admin.config.sec_debug_categories";
    public static final String CFG_SEC_SENTRY = "hyperfactions_admin.config.sec_sentry";
    public static final String CFG_SEC_GRAVESTONE_PROTECTION = "hyperfactions_admin.config.sec_gravestone_protection";
    public static final String CFG_SEC_GRAVESTONE_ACCESS = "hyperfactions_admin.config.sec_gravestone_access";
    public static final String CFG_SEC_GRAVESTONE_LOOT = "hyperfactions_admin.config.sec_gravestone_loot";

    // Faction entry labels
    public static final String GUI_FAC_ENTRY_POWER = "hyperfactions_admin.gui.fac_entry_power";
    public static final String GUI_FAC_ENTRY_CLAIMS = "hyperfactions_admin.gui.fac_entry_claims";
    public static final String GUI_FAC_ENTRY_MEMBERS = "hyperfactions_admin.gui.fac_entry_members";
    public static final String GUI_FAC_ENTRY_CREATED = "hyperfactions_admin.gui.fac_entry_created";
    public static final String GUI_FAC_ENTRY_HOME = "hyperfactions_admin.gui.fac_entry_home";
    public static final String GUI_FAC_ENTRY_TP_HOME = "hyperfactions_admin.gui.fac_entry_tp_home";
    public static final String GUI_FAC_ENTRY_VIEW_INFO = "hyperfactions_admin.gui.fac_entry_view_info";
    public static final String GUI_FAC_ENTRY_MEMBERS_BTN = "hyperfactions_admin.gui.fac_entry_members_btn";
    public static final String GUI_FAC_ENTRY_SETTINGS = "hyperfactions_admin.gui.fac_entry_settings";
    public static final String GUI_FAC_ENTRY_UNCLAIM_ALL = "hyperfactions_admin.gui.fac_entry_unclaim_all";
    public static final String GUI_FAC_ENTRY_DISBAND = "hyperfactions_admin.gui.fac_entry_disband";
    // Player entry labels
    public static final String GUI_PLR_ENTRY_ROLE = "hyperfactions_admin.gui.plr_entry_role";
    public static final String GUI_PLR_ENTRY_JOINED = "hyperfactions_admin.gui.plr_entry_joined";
    public static final String GUI_PLR_ENTRY_LAST_ONLINE = "hyperfactions_admin.gui.plr_entry_last_online";
    public static final String GUI_PLR_ENTRY_KDR = "hyperfactions_admin.gui.plr_entry_kdr";
    public static final String GUI_PLR_ENTRY_POWER = "hyperfactions_admin.gui.plr_entry_power";
    public static final String GUI_PLR_ENTRY_UUID = "hyperfactions_admin.gui.plr_entry_uuid";
    public static final String GUI_PLR_ENTRY_INFO = "hyperfactions_admin.gui.plr_entry_info";
    public static final String GUI_PLR_ENTRY_TELEPORT = "hyperfactions_admin.gui.plr_entry_teleport";
    public static final String GUI_PLR_ENTRY_NA = "hyperfactions_admin.gui.plr_entry_na";
    public static final String GUI_PLR_ENTRY_UNKNOWN = "hyperfactions_admin.gui.plr_entry_unknown";
    public static final String GUI_PLR_ENTRY_AGO = "hyperfactions_admin.gui.plr_entry_ago";
    // Zone entry labels
    public static final String GUI_ZONE_ENTRY_WORLD = "hyperfactions_admin.gui.zone_entry_world";
    public static final String GUI_ZONE_ENTRY_CHUNKS = "hyperfactions_admin.gui.zone_entry_chunks";
    public static final String GUI_ZONE_ENTRY_BOUNDS = "hyperfactions_admin.gui.zone_entry_bounds";
    public static final String GUI_ZONE_ENTRY_CREATED = "hyperfactions_admin.gui.zone_entry_created";
    public static final String GUI_ZONE_ENTRY_EDIT_MAP = "hyperfactions_admin.gui.zone_entry_edit_map";
    public static final String GUI_ZONE_ENTRY_FLAGS = "hyperfactions_admin.gui.zone_entry_flags";
    public static final String GUI_ZONE_ENTRY_SETTINGS = "hyperfactions_admin.gui.zone_entry_settings";
    public static final String GUI_ZONE_ENTRY_DELETE = "hyperfactions_admin.gui.zone_entry_delete";

    private AdminGui() {}
  }
}
