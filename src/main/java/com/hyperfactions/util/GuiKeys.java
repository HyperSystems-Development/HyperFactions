package com.hyperfactions.util;

/**
 * GUI page message keys split from the original MessageKeys.
 *
 * <p>
 * Contains all player-facing GUI inner classes — navigation, page labels,
 * modal dialogs, and interactive page messages. Key prefix is
 * {@code hyperfactions_gui.*} mapping to {@code hyperfactions_gui.lang}.
 */
public final class GuiKeys {

  private GuiKeys() {}

  // =====================================================================
  // GUI — Navigation and shared GUI elements
  // =====================================================================

  /** Navigation bar labels. */
  public static final class Nav {
    public static final String DASHBOARD = "hyperfactions_gui.nav.dashboard";
    public static final String CHAT = "hyperfactions_gui.nav.chat";
    public static final String MEMBERS = "hyperfactions_gui.nav.members";
    public static final String INVITES = "hyperfactions_gui.nav.invites";
    public static final String BROWSER = "hyperfactions_gui.nav.browser";
    public static final String MAP = "hyperfactions_gui.nav.map";
    public static final String LEADERBOARD = "hyperfactions_gui.nav.leaderboard";
    public static final String RELATIONS = "hyperfactions_gui.nav.relations";
    public static final String TREASURY = "hyperfactions_gui.nav.treasury";
    public static final String SETTINGS = "hyperfactions_gui.nav.settings";
    public static final String LOGS = "hyperfactions_gui.nav.logs";
    public static final String HELP = "hyperfactions_gui.nav.help";
    public static final String ADMIN = "hyperfactions_gui.nav.admin";
    public static final String CREATE = "hyperfactions_gui.nav.create";
    public static final String PLAYER_SETTINGS = "hyperfactions_gui.nav.player_settings";

    private Nav() {}
  }

  /** Main menu page labels. */
  public static final class MainMenu {
    public static final String TITLE = "hyperfactions_gui.main_menu.title";
    public static final String SECTION_MY_FACTION = "hyperfactions_gui.main_menu.section_my_faction";
    public static final String SECTION_GET_STARTED = "hyperfactions_gui.main_menu.section_get_started";
    public static final String SECTION_TERRITORY = "hyperfactions_gui.main_menu.section_territory";
    public static final String SECTION_BROWSE = "hyperfactions_gui.main_menu.section_browse";
    public static final String SECTION_ADMIN = "hyperfactions_gui.main_menu.section_admin";
    public static final String CLAIM_HINT = "hyperfactions_gui.main_menu.claim_hint";

    private MainMenu() {}
  }

  // =====================================================================
  // GUI — Shared labels
  // =====================================================================

  /** Shared GUI labels used across multiple pages. */
  public static final class GuiCommon {
    public static final String FACTION_COUNT = "hyperfactions_gui.common.faction_count";
    public static final String LEADER_LABEL = "hyperfactions_gui.common.leader_label";
    public static final String SORT_POWER = "hyperfactions_gui.common.sort_power";
    public static final String SORT_MEMBERS = "hyperfactions_gui.common.sort_members";
    public static final String PAGE_FORMAT = "hyperfactions_gui.common.page_format";
    public static final String OWN_FACTION = "hyperfactions_gui.common.own_faction";
    public static final String SEARCH = "hyperfactions_gui.common.search";
    public static final String SORT = "hyperfactions_gui.common.sort";
    public static final String PREV = "hyperfactions_gui.common.prev";
    public static final String NEXT = "hyperfactions_gui.common.next";

    public static final String TREASURY_NOT_AVAILABLE = "hyperfactions_gui.common.treasury_not_available";

    private GuiCommon() {}
  }

  // =====================================================================
  // GUI — Confirmation pages
  // =====================================================================

  /** Confirmation page messages (disband, leave, transfer). */
  public static final class ConfirmGui {
    // Static UI labels
    public static final String DISBAND_TITLE = "hyperfactions_gui.confirm.disband_title";
    public static final String DISBAND_PROMPT = "hyperfactions_gui.confirm.disband_prompt";
    public static final String DISBAND_WARNING = "hyperfactions_gui.confirm.disband_warning";
    public static final String LEAVE_TITLE = "hyperfactions_gui.confirm.leave_title";
    public static final String LEAVE_PROMPT = "hyperfactions_gui.confirm.leave_prompt";
    public static final String LEAVE_WARNING = "hyperfactions_gui.confirm.leave_warning";
    public static final String LEADER_LEAVE_TITLE = "hyperfactions_gui.confirm.leader_leave_title";
    public static final String LEADER_LEAVE_PROMPT = "hyperfactions_gui.confirm.leader_leave_prompt";
    public static final String TRANSFER_TITLE = "hyperfactions_gui.confirm.transfer_title";
    public static final String TRANSFER_PROMPT = "hyperfactions_gui.confirm.transfer_prompt";
    public static final String TRANSFER_WARNING = "hyperfactions_gui.confirm.transfer_warning";
    public static final String ERROR_TITLE = "hyperfactions_gui.confirm.error_title";
    public static final String ERROR_DEFAULT = "hyperfactions_gui.confirm.error_default";
    // DisbandConfirm
    public static final String DISBAND_NOT_LEADER = "hyperfactions_gui.confirm.disband_not_leader";
    public static final String DISBANDED = "hyperfactions_gui.confirm.disbanded";
    public static final String DISBAND_FAILED = "hyperfactions_gui.confirm.disband_failed";
    // LeaderLeaveConfirm
    public static final String SUCCESSION_TITLE = "hyperfactions_gui.confirm.succession_title";
    public static final String NO_MEMBERS_WARNING = "hyperfactions_gui.confirm.no_members_warning";
    public static final String WILL_DISBAND = "hyperfactions_gui.confirm.will_disband";
    public static final String NOT_IN_FACTION = "hyperfactions_gui.confirm.not_in_faction";
    public static final String NOT_LEADER_ANYMORE = "hyperfactions_gui.confirm.not_leader_anymore";
    public static final String NO_SUCCESSOR = "hyperfactions_gui.confirm.no_successor";
    public static final String TRANSFER_FAILED = "hyperfactions_gui.confirm.transfer_failed";
    public static final String LEADER_LEFT = "hyperfactions_gui.confirm.leader_left";
    public static final String LEAVE_FAILED = "hyperfactions_gui.confirm.leave_failed";
    // LeaveConfirm
    public static final String LEADER_CANNOT_LEAVE = "hyperfactions_gui.confirm.leader_cannot_leave";
    public static final String LEFT_FACTION = "hyperfactions_gui.confirm.left_faction";
    // TransferConfirm
    public static final String FACTION_GONE = "hyperfactions_gui.confirm.faction_gone";
    public static final String NOT_LEADER_TRANSFER = "hyperfactions_gui.confirm.not_leader_transfer";
    public static final String LEADERSHIP_TRANSFERRED = "hyperfactions_gui.confirm.leadership_transferred";

    private ConfirmGui() {}
  }

  // =====================================================================
  // GUI — Faction info and main pages
  // =====================================================================

  /** Faction info page labels. */
  public static final class FactionInfoGui {
    public static final String TITLE = "hyperfactions_gui.faction_info.title";
    public static final String STATUS_OPEN = "hyperfactions_gui.faction_info.status_open";
    public static final String STATUS_INVITE_ONLY = "hyperfactions_gui.faction_info.status_invite_only";
    public static final String STATUS_RAIDABLE = "hyperfactions_gui.faction_info.status_raidable";
    public static final String STATUS_PROTECTED = "hyperfactions_gui.faction_info.status_protected";
    public static final String OFFICERS_MORE = "hyperfactions_gui.faction_info.officers_more";
    // Stat card headers
    public static final String POWER_HEADER = "hyperfactions_gui.faction_info.power_header";
    public static final String CLAIMS_HEADER = "hyperfactions_gui.faction_info.claims_header";
    public static final String MEMBERS_HEADER = "hyperfactions_gui.faction_info.members_header";
    public static final String RELATIONS_HEADER = "hyperfactions_gui.faction_info.relations_header";
    public static final String STATUS_HEADER = "hyperfactions_gui.faction_info.status_header";
    public static final String TREASURY_HEADER = "hyperfactions_gui.faction_info.treasury_header";
    // Stat card subtitles
    public static final String CURRENT_MAX = "hyperfactions_gui.faction_info.current_max";
    public static final String CLAIMED_MAX = "hyperfactions_gui.faction_info.claimed_max";
    public static final String ALLY_ENEMY = "hyperfactions_gui.faction_info.ally_enemy";
    public static final String FACTION_BALANCE = "hyperfactions_gui.faction_info.faction_balance";
    // Leadership labels
    public static final String LEADER_LABEL = "hyperfactions_gui.faction_info.leader_label";
    public static final String OFFICERS_LABEL = "hyperfactions_gui.faction_info.officers_label";
    // Button text
    public static final String VIEW_MEMBERS_BTN = "hyperfactions_gui.faction_info.view_members_btn";
    public static final String RELATIONS_BTN = "hyperfactions_gui.faction_info.relations_btn";

    private FactionInfoGui() {}
  }

  /** Faction main page (no-faction view) labels and messages. */
  public static final class FactionMainGui {
    public static final String NO_FACTION = "hyperfactions_gui.main.no_faction";
    public static final String JOINED = "hyperfactions_gui.main.joined";
    public static final String JOIN_FAILED = "hyperfactions_gui.main.join_failed";
    public static final String INVITE_DECLINED = "hyperfactions_gui.main.invite_declined";
    public static final String COOLDOWN = "hyperfactions_gui.main.cooldown";
    public static final String WORLD_NOT_FOUND = "hyperfactions_gui.main.world_not_found";
    public static final String LEAVE_FAILED = "hyperfactions_gui.main.leave_failed";

    private FactionMainGui() {}
  }

  // =====================================================================
  // GUI — Modal dialogs (rename, description, tag)
  // =====================================================================

  /** Rename modal page messages. */
  public static final class RenameGui {
    public static final String TITLE = "hyperfactions_gui.rename.title";
    public static final String CURRENT_LABEL = "hyperfactions_gui.rename.current_label";
    public static final String NEW_NAME_LABEL = "hyperfactions_gui.rename.new_name_label";
    public static final String NO_PERMISSION = "hyperfactions_gui.rename.no_permission";
    public static final String ENTER_NAME = "hyperfactions_gui.rename.enter_name";
    public static final String TOO_SHORT = "hyperfactions_gui.rename.too_short";
    public static final String TOO_LONG = "hyperfactions_gui.rename.too_long";
    public static final String SAME_NAME = "hyperfactions_gui.rename.same_name";
    public static final String NAME_TAKEN = "hyperfactions_gui.rename.name_taken";
    public static final String SUCCESS = "hyperfactions_gui.rename.success";

    private RenameGui() {}
  }

  /** Description modal page messages. */
  public static final class DescGui {
    public static final String TITLE = "hyperfactions_gui.desc.title";
    public static final String CURRENT_LABEL = "hyperfactions_gui.desc.current_label";
    public static final String NEW_DESC_LABEL = "hyperfactions_gui.desc.new_desc_label";
    public static final String NO_PERMISSION = "hyperfactions_gui.desc.no_permission";
    public static final String DISPLAY_NONE = "hyperfactions_gui.desc.display_none";
    public static final String CLEARED = "hyperfactions_gui.desc.cleared";
    public static final String UPDATED = "hyperfactions_gui.desc.updated";

    private DescGui() {}
  }

  /** Tag modal page messages. */
  public static final class TagGui {
    public static final String TITLE = "hyperfactions_gui.tag.title";
    public static final String CURRENT_LABEL = "hyperfactions_gui.tag.current_label";
    public static final String INSTRUCTIONS = "hyperfactions_gui.tag.instructions";
    public static final String HELP_TEXT = "hyperfactions_gui.tag.help_text";
    public static final String NO_PERMISSION = "hyperfactions_gui.tag.no_permission";
    public static final String DISPLAY_NONE = "hyperfactions_gui.tag.display_none";
    public static final String CLEARED = "hyperfactions_gui.tag.cleared";
    public static final String TOO_SHORT = "hyperfactions_gui.tag.too_short";
    public static final String TOO_LONG = "hyperfactions_gui.tag.too_long";
    public static final String INVALID_FORMAT = "hyperfactions_gui.tag.invalid_format";
    public static final String SAME_TAG = "hyperfactions_gui.tag.same_tag";
    public static final String TAG_TAKEN = "hyperfactions_gui.tag.tag_taken";
    public static final String SUCCESS = "hyperfactions_gui.tag.success";

    private TagGui() {}
  }

  // =====================================================================
  // GUI — Dashboard
  // =====================================================================

  /** Dashboard page labels and messages. */
  public static final class DashboardGui {
    public static final String TITLE = "hyperfactions_gui.dashboard.title";
    public static final String POWER_LABEL = "hyperfactions_gui.dashboard.power_label";
    public static final String LAND_LABEL = "hyperfactions_gui.dashboard.land_label";
    public static final String MEMBERS_LABEL = "hyperfactions_gui.dashboard.members_label";
    public static final String ONLINE_LABEL = "hyperfactions_gui.dashboard.online_label";
    public static final String ALLIES_LABEL = "hyperfactions_gui.dashboard.allies_label";
    public static final String ENEMIES_LABEL = "hyperfactions_gui.dashboard.enemies_label";
    public static final String RELATIONS_LABEL = "hyperfactions_gui.dashboard.relations_label";
    public static final String ALLY_ENEMY_LABEL = "hyperfactions_gui.dashboard.ally_enemy_label";
    public static final String STATUS_LABEL = "hyperfactions_gui.dashboard.status_label";
    public static final String INVITES_LABEL = "hyperfactions_gui.dashboard.invites_label";
    public static final String SENT_REQUESTS_LABEL = "hyperfactions_gui.dashboard.sent_requests_label";
    public static final String TREASURY_LABEL = "hyperfactions_gui.dashboard.treasury_label";
    public static final String UPKEEP_LABEL = "hyperfactions_gui.dashboard.upkeep_label";
    public static final String PER_CYCLE = "hyperfactions_gui.dashboard.per_cycle";
    public static final String YOUR_WALLET = "hyperfactions_gui.dashboard.your_wallet";
    public static final String PERSONAL_BALANCE = "hyperfactions_gui.dashboard.personal_balance";
    public static final String QUICK_ACTIONS = "hyperfactions_gui.dashboard.quick_actions";
    public static final String TELEPORT_LABEL = "hyperfactions_gui.dashboard.teleport_label";
    public static final String TERRITORY_LABEL = "hyperfactions_gui.dashboard.territory_label";
    public static final String CHANNEL_LABEL = "hyperfactions_gui.dashboard.channel_label";
    public static final String MEMBERSHIP_LABEL = "hyperfactions_gui.dashboard.membership_label";
    public static final String RECENT_ACTIVITY = "hyperfactions_gui.dashboard.recent_activity";
    public static final String VIEW_ALL = "hyperfactions_gui.dashboard.view_all";
    public static final String INCOME_24H = "hyperfactions_gui.dashboard.income_24h";
    public static final String DEPOSITS_TRANSFERS_IN = "hyperfactions_gui.dashboard.deposits_transfers_in";
    public static final String EXPENSES_24H = "hyperfactions_gui.dashboard.expenses_24h";
    public static final String WITHDRAWALS_TRANSFERS_OUT = "hyperfactions_gui.dashboard.withdrawals_transfers_out";
    public static final String FACTION_GONE = "hyperfactions_gui.dashboard.faction_gone";
    public static final String AVAILABLE = "hyperfactions_gui.dashboard.available";
    public static final String AT_RISK = "hyperfactions_gui.dashboard.at_risk";
    public static final String ONLINE_COUNT = "hyperfactions_gui.dashboard.online_count";
    public static final String STATUS_INVITE = "hyperfactions_gui.dashboard.status_invite";
    public static final String IN_GRACE = "hyperfactions_gui.dashboard.in_grace";
    public static final String BILLABLE_CHUNKS = "hyperfactions_gui.dashboard.billable_chunks";
    public static final String BTN_HOME = "hyperfactions_gui.dashboard.btn_home";
    public static final String BTN_SET_HOME = "hyperfactions_gui.dashboard.btn_set_home";
    public static final String BTN_CLAIM = "hyperfactions_gui.dashboard.btn_claim";
    public static final String CHAT_PREFIX = "hyperfactions_gui.dashboard.chat_prefix";
    public static final String BTN_LEAVE = "hyperfactions_gui.dashboard.btn_leave";
    public static final String NO_ACTIVITY = "hyperfactions_gui.dashboard.no_activity";
    public static final String TIME_NOW = "hyperfactions_gui.dashboard.time_now";
    public static final String TIME_MINUTES = "hyperfactions_gui.dashboard.time_minutes";
    public static final String TIME_HOURS = "hyperfactions_gui.dashboard.time_hours";
    public static final String TIME_DAYS = "hyperfactions_gui.dashboard.time_days";
    public static final String NO_HOME_HINT = "hyperfactions_gui.dashboard.no_home_hint";
    public static final String CHAT_MODE_SET = "hyperfactions_gui.dashboard.chat_mode_set";
    public static final String CLAIM_SUCCESS = "hyperfactions_gui.dashboard.claim_success";
    public static final String UPKEEP_IN = "hyperfactions_gui.dashboard.upkeep_in";

    private DashboardGui() {}
  }

  // =====================================================================
  // GUI — Members page
  // =====================================================================

  /** Members page labels and messages. */
  public static final class MembersGui {
    public static final String TITLE = "hyperfactions_gui.members.title";
    public static final String SEARCH_LABEL = "hyperfactions_gui.members.search_label";
    public static final String SORT_LABEL = "hyperfactions_gui.members.sort_label";
    public static final String PREV_BTN = "hyperfactions_gui.members.prev_btn";
    public static final String NEXT_BTN = "hyperfactions_gui.members.next_btn";
    public static final String SORT_ROLE = "hyperfactions_gui.members.sort_role";
    public static final String SORT_LAST_ONLINE = "hyperfactions_gui.members.sort_last_online";
    public static final String JUST_NOW = "hyperfactions_gui.members.just_now";
    public static final String AGO = "hyperfactions_gui.members.ago";
    public static final String NEVER = "hyperfactions_gui.members.never";
    public static final String MEMBER_NOT_FOUND = "hyperfactions_gui.members.member_not_found";
    public static final String PROMOTED = "hyperfactions_gui.members.promoted";
    public static final String PROMOTE_FAILED = "hyperfactions_gui.members.promote_failed";
    public static final String DEMOTED = "hyperfactions_gui.members.demoted";
    public static final String DEMOTE_FAILED = "hyperfactions_gui.members.demote_failed";
    public static final String KICKED = "hyperfactions_gui.members.kicked";
    public static final String KICK_FAILED = "hyperfactions_gui.members.kick_failed";
    public static final String LABEL_POWER = "hyperfactions_gui.members.label_power";
    public static final String LABEL_JOINED = "hyperfactions_gui.members.label_joined";
    public static final String LABEL_LAST_DEATH = "hyperfactions_gui.members.label_last_death";
    public static final String BTN_PROMOTE = "hyperfactions_gui.members.btn_promote";
    public static final String BTN_DEMOTE = "hyperfactions_gui.members.btn_demote";
    public static final String BTN_KICK = "hyperfactions_gui.members.btn_kick";
    public static final String BTN_MAKE_LEADER = "hyperfactions_gui.members.btn_make_leader";
    public static final String BTN_PROFILE = "hyperfactions_gui.members.btn_profile";
    public static final String SELF_LABEL = "hyperfactions_gui.members.self_label";

    private MembersGui() {}
  }

  // =====================================================================
  // GUI — Browser page
  // =====================================================================

  /** Browser page labels. */
  public static final class BrowserGui {
    public static final String TITLE = "hyperfactions_gui.browser.title";
    public static final String SEARCH_LABEL = "hyperfactions_gui.browser.search_label";
    public static final String SORT_LABEL = "hyperfactions_gui.browser.sort_label";
    public static final String PREV_BTN = "hyperfactions_gui.browser.prev_btn";
    public static final String NEXT_BTN = "hyperfactions_gui.browser.next_btn";
    public static final String SORT_NAME = "hyperfactions_gui.browser.sort_name";
    public static final String INVALID_FACTION = "hyperfactions_gui.browser.invalid_faction";
    public static final String LABEL_POWER = "hyperfactions_gui.browser.label_power";
    public static final String LABEL_CLAIMS = "hyperfactions_gui.browser.label_claims";
    public static final String LABEL_MEMBERS = "hyperfactions_gui.browser.label_members";
    public static final String LABEL_RECRUITMENT = "hyperfactions_gui.browser.label_recruitment";
    public static final String LABEL_CREATED = "hyperfactions_gui.browser.label_created";
    public static final String LABEL_DESCRIPTION = "hyperfactions_gui.browser.label_description";
    public static final String VIEW_INFO_BTN = "hyperfactions_gui.browser.view_info_btn";
    public static final String LABEL_LEADER = "hyperfactions_gui.browser.label_leader";

    private BrowserGui() {}
  }

  // =====================================================================
  // GUI — Leaderboard page
  // =====================================================================

  /** Leaderboard page labels. */
  public static final class LeaderboardGui {
    public static final String TITLE = "hyperfactions_gui.leaderboard.title";
    public static final String RANK_BY = "hyperfactions_gui.leaderboard.rank_by";
    public static final String COL_RANK = "hyperfactions_gui.leaderboard.col_rank";
    public static final String COL_FACTION = "hyperfactions_gui.leaderboard.col_faction";
    public static final String COL_CLAIMS = "hyperfactions_gui.leaderboard.col_claims";
    public static final String COL_MEMBERS = "hyperfactions_gui.leaderboard.col_members";
    public static final String PREV_BTN = "hyperfactions_gui.leaderboard.prev_btn";
    public static final String NEXT_BTN = "hyperfactions_gui.leaderboard.next_btn";
    public static final String SORT_KD = "hyperfactions_gui.leaderboard.sort_kd";
    public static final String SORT_TERRITORY = "hyperfactions_gui.leaderboard.sort_territory";
    public static final String SORT_BALANCE = "hyperfactions_gui.leaderboard.sort_balance";

    private LeaderboardGui() {}
  }

  // =====================================================================
  // GUI — Player info page
  // =====================================================================

  /** Player info page labels and messages. */
  public static final class PlayerInfoGui {
    public static final String TITLE = "hyperfactions_gui.playerinfo.title";
    public static final String FIRST_JOINED_LABEL = "hyperfactions_gui.playerinfo.first_joined_label";
    public static final String LAST_ONLINE_LABEL = "hyperfactions_gui.playerinfo.last_online_label";
    public static final String FACTION_LABEL = "hyperfactions_gui.playerinfo.faction_label";
    public static final String ROLE_LABEL = "hyperfactions_gui.playerinfo.role_label";
    public static final String JOINED_LABEL_STATIC = "hyperfactions_gui.playerinfo.joined_label_static";
    public static final String NOT_IN_FACTION = "hyperfactions_gui.playerinfo.not_in_faction";
    public static final String POWER_HEADER = "hyperfactions_gui.playerinfo.power_header";
    public static final String CURRENT_MAX = "hyperfactions_gui.playerinfo.current_max";
    public static final String COMBAT_HEADER = "hyperfactions_gui.playerinfo.combat_header";
    public static final String KILLS_DEATHS = "hyperfactions_gui.playerinfo.kills_deaths";
    public static final String KDR_HEADER = "hyperfactions_gui.playerinfo.kdr_header";
    public static final String MEMBERSHIP_HISTORY = "hyperfactions_gui.playerinfo.membership_history";
    public static final String VIEW_FACTION_BTN = "hyperfactions_gui.playerinfo.view_faction_btn";
    public static final String NOW = "hyperfactions_gui.playerinfo.now";
    public static final String HISTORY_COUNT = "hyperfactions_gui.playerinfo.history_count";
    public static final String JOINED_LABEL = "hyperfactions_gui.playerinfo.joined_label";
    public static final String CURRENT = "hyperfactions_gui.playerinfo.current";
    public static final String LEFT_LABEL = "hyperfactions_gui.playerinfo.left_label";
    public static final String NO_HISTORY = "hyperfactions_gui.playerinfo.no_history";
    public static final String FACTION_GONE = "hyperfactions_gui.playerinfo.faction_gone";
    public static final String REASON_ACTIVE = "hyperfactions_gui.playerinfo.reason_active";
    public static final String REASON_LEFT = "hyperfactions_gui.playerinfo.reason_left";
    public static final String REASON_KICKED = "hyperfactions_gui.playerinfo.reason_kicked";
    public static final String REASON_DISBANDED = "hyperfactions_gui.playerinfo.reason_disbanded";

    private PlayerInfoGui() {}
  }

  // =====================================================================
  // GUI — Help page
  // =====================================================================

  /** Help GUI category display names and new player help page content. */
  public static final class HelpGui {
    public static final String WELCOME = "hyperfactions_gui.help.category.welcome";
    public static final String YOUR_FACTION = "hyperfactions_gui.help.category.your_faction";
    public static final String POWER_LAND = "hyperfactions_gui.help.category.power_land";
    public static final String DIPLOMACY = "hyperfactions_gui.help.category.diplomacy";
    public static final String COMBAT = "hyperfactions_gui.help.category.combat";
    public static final String ECONOMY = "hyperfactions_gui.help.category.economy";
    public static final String QUICK_REF = "hyperfactions_gui.help.category.quick_ref";
    // Admin help categories
    public static final String ADMIN_OVERVIEW = "hyperfactions_gui.help.category.admin_overview";
    public static final String ADMIN_FACTIONS = "hyperfactions_gui.help.category.admin_factions";
    public static final String ADMIN_ZONES = "hyperfactions_gui.help.category.admin_zones";
    public static final String ADMIN_POWER = "hyperfactions_gui.help.category.admin_power";
    public static final String ADMIN_ECONOMY = "hyperfactions_gui.help.category.admin_economy";
    public static final String ADMIN_CONFIG = "hyperfactions_gui.help.category.admin_config";
    public static final String ADMIN_MAINTENANCE = "hyperfactions_gui.help.category.admin_maintenance";
    public static final String ADMIN_REFERENCE = "hyperfactions_gui.help.category.admin_reference";
    // Help Center page title
    public static final String HELP_CENTER_TITLE = "hyperfactions_gui.help.center_title";
    // New player help page
    public static final String GETTING_STARTED_TITLE = "hyperfactions_gui.help.getting_started_title";
    public static final String WHAT_ARE_FACTIONS_TITLE = "hyperfactions_gui.help.what_are_factions_title";
    public static final String WHAT_ARE_FACTIONS_1 = "hyperfactions_gui.help.what_are_factions_1";
    public static final String WHAT_ARE_FACTIONS_2 = "hyperfactions_gui.help.what_are_factions_2";
    public static final String WHAT_ARE_FACTIONS_BULLET_1 = "hyperfactions_gui.help.what_are_factions_bullet_1";
    public static final String WHAT_ARE_FACTIONS_BULLET_2 = "hyperfactions_gui.help.what_are_factions_bullet_2";
    public static final String WHAT_ARE_FACTIONS_BULLET_3 = "hyperfactions_gui.help.what_are_factions_bullet_3";
    public static final String JOINING_TITLE = "hyperfactions_gui.help.joining_title";
    public static final String JOINING_DESC = "hyperfactions_gui.help.joining_desc";
    public static final String JOINING_BULLET_1 = "hyperfactions_gui.help.joining_bullet_1";
    public static final String JOINING_BULLET_2 = "hyperfactions_gui.help.joining_bullet_2";
    public static final String JOINING_BULLET_3 = "hyperfactions_gui.help.joining_bullet_3";
    public static final String CREATING_TITLE = "hyperfactions_gui.help.creating_title";
    public static final String CREATING_DESC = "hyperfactions_gui.help.creating_desc";
    public static final String CREATING_BULLET_1 = "hyperfactions_gui.help.creating_bullet_1";
    public static final String CREATING_BULLET_2 = "hyperfactions_gui.help.creating_bullet_2";
    public static final String COMMANDS_TITLE = "hyperfactions_gui.help.commands_title";
    public static final String CMD_F = "hyperfactions_gui.help.cmd_f";
    public static final String CMD_F_LIST = "hyperfactions_gui.help.cmd_f_list";
    public static final String CMD_F_JOIN = "hyperfactions_gui.help.cmd_f_join";
    public static final String CMD_F_CREATE = "hyperfactions_gui.help.cmd_f_create";
    public static final String CMD_F_HELP = "hyperfactions_gui.help.cmd_f_help";
    public static final String TIP = "hyperfactions_gui.help.tip";

    private HelpGui() {}
  }

  // =====================================================================
  // GUI — Relations page
  // =====================================================================

  /** Relations page labels and messages. */
  public static final class RelationsGui {
    public static final String TITLE = "hyperfactions_gui.relations.title";
    public static final String TAB_RELATIONS = "hyperfactions_gui.relations.tab_relations";
    public static final String TAB_PENDING = "hyperfactions_gui.relations.tab_pending";
    public static final String SET_RELATION_BTN = "hyperfactions_gui.relations.set_relation_btn";
    public static final String PREV_BTN = "hyperfactions_gui.relations.prev_btn";
    public static final String NEXT_BTN = "hyperfactions_gui.relations.next_btn";
    public static final String RELATION_COUNT = "hyperfactions_gui.relations.relation_count";
    public static final String REQUEST_COUNT = "hyperfactions_gui.relations.request_count";
    public static final String TYPE_ALLY = "hyperfactions_gui.relations.type_ally";
    public static final String TYPE_ENEMY = "hyperfactions_gui.relations.type_enemy";
    public static final String TYPE_INCOMING = "hyperfactions_gui.relations.type_incoming";
    public static final String TYPE_OUTGOING = "hyperfactions_gui.relations.type_outgoing";
    public static final String INCOMING_REQUEST = "hyperfactions_gui.relations.incoming_request";
    public static final String OUTGOING_REQUEST = "hyperfactions_gui.relations.outgoing_request";
    public static final String EMPTY_RELATIONS = "hyperfactions_gui.relations.empty_relations";
    public static final String EMPTY_RELATIONS_HINT = "hyperfactions_gui.relations.empty_relations_hint";
    public static final String EMPTY_PENDING = "hyperfactions_gui.relations.empty_pending";
    public static final String TODAY = "hyperfactions_gui.relations.today";
    public static final String ONE_DAY_AGO = "hyperfactions_gui.relations.one_day_ago";
    public static final String DAYS_AGO = "hyperfactions_gui.relations.days_ago";
    public static final String NOW_NEUTRAL = "hyperfactions_gui.relations.now_neutral";
    public static final String NOW_ENEMIES = "hyperfactions_gui.relations.now_enemies";
    public static final String REQUEST_SENT = "hyperfactions_gui.relations.request_sent";
    public static final String NOW_ALLIED = "hyperfactions_gui.relations.now_allied";
    public static final String REQUEST_DECLINED = "hyperfactions_gui.relations.request_declined";
    public static final String REQUEST_CANCELLED = "hyperfactions_gui.relations.request_cancelled";
    public static final String FAILED = "hyperfactions_gui.relations.failed";
    public static final String SEARCH_HINT = "hyperfactions_gui.relations.search_hint";
    public static final String NO_RESULTS = "hyperfactions_gui.relations.no_results";
    public static final String POWER_DISPLAY = "hyperfactions_gui.relations.power_display";
    public static final String LABEL_MEMBERS = "hyperfactions_gui.relations.label_members";
    public static final String LABEL_POWER = "hyperfactions_gui.relations.label_power";
    public static final String LABEL_SINCE = "hyperfactions_gui.relations.label_since";
    public static final String LABEL_CLAIMS = "hyperfactions_gui.relations.label_claims";
    public static final String LABEL_DIRECTION = "hyperfactions_gui.relations.label_direction";
    public static final String BTN_VIEW = "hyperfactions_gui.relations.btn_view";
    public static final String BTN_NEUTRAL = "hyperfactions_gui.relations.btn_neutral";
    public static final String BTN_ENEMY = "hyperfactions_gui.relations.btn_enemy";
    public static final String BTN_ALLY = "hyperfactions_gui.relations.btn_ally";
    public static final String BTN_ACCEPT = "hyperfactions_gui.relations.btn_accept";
    public static final String BTN_DECLINE = "hyperfactions_gui.relations.btn_decline";
    public static final String BTN_CANCEL = "hyperfactions_gui.relations.btn_cancel";

    private RelationsGui() {}
  }

  // =====================================================================
  // GUI — Settings page
  // =====================================================================

  /** Settings page labels and messages. */
  public static final class SettingsGui {
    public static final String TITLE = "hyperfactions_gui.settings.title";
    public static final String GENERAL = "hyperfactions_gui.settings.general";
    public static final String NAME_LABEL = "hyperfactions_gui.settings.name_label";
    public static final String TAG_LABEL = "hyperfactions_gui.settings.tag_label";
    public static final String DESC_LABEL = "hyperfactions_gui.settings.desc_label";
    public static final String EDIT_BTN = "hyperfactions_gui.settings.edit_btn";
    public static final String RECRUITMENT = "hyperfactions_gui.settings.recruitment";
    public static final String STATUS_LABEL = "hyperfactions_gui.settings.status_label";
    public static final String HOME_LOCATION = "hyperfactions_gui.settings.home_location";
    public static final String LOCATION_LABEL = "hyperfactions_gui.settings.location_label";
    public static final String SET_HOME_BTN = "hyperfactions_gui.settings.set_home_btn";
    public static final String TELEPORT_BTN = "hyperfactions_gui.settings.teleport_btn";
    public static final String DELETE_BTN = "hyperfactions_gui.settings.delete_btn";
    public static final String OPTIONAL_FEATURES = "hyperfactions_gui.settings.optional_features";
    public static final String CONFIGURE_MODULES = "hyperfactions_gui.settings.configure_modules";
    public static final String MODULES_BTN = "hyperfactions_gui.settings.modules_btn";
    public static final String DANGER_ZONE = "hyperfactions_gui.settings.danger_zone";
    public static final String IRREVERSIBLE = "hyperfactions_gui.settings.irreversible";
    public static final String DISBAND_BTN = "hyperfactions_gui.settings.disband_btn";
    public static final String LOCK_HINT = "hyperfactions_gui.settings.lock_hint";
    public static final String TERRITORY_PERMISSIONS = "hyperfactions_gui.settings.territory_permissions";
    public static final String COL_OUT = "hyperfactions_gui.settings.col_out";
    public static final String COL_ALLY = "hyperfactions_gui.settings.col_ally";
    public static final String COL_MEM = "hyperfactions_gui.settings.col_mem";
    public static final String COL_OFF = "hyperfactions_gui.settings.col_off";
    public static final String CAT_BUILDING = "hyperfactions_gui.settings.cat_building";
    public static final String PERM_BREAK = "hyperfactions_gui.settings.perm_break";
    public static final String PERM_PLACE = "hyperfactions_gui.settings.perm_place";
    public static final String CAT_INTERACTION = "hyperfactions_gui.settings.cat_interaction";
    public static final String INTERACTION_HINT = "hyperfactions_gui.settings.interaction_hint";
    public static final String PERM_ALL = "hyperfactions_gui.settings.perm_all";
    public static final String PERM_DOOR = "hyperfactions_gui.settings.perm_door";
    public static final String PERM_CHEST = "hyperfactions_gui.settings.perm_chest";
    public static final String PERM_BENCH = "hyperfactions_gui.settings.perm_bench";
    public static final String PERM_PROCESSING = "hyperfactions_gui.settings.perm_processing";
    public static final String PERM_SEAT = "hyperfactions_gui.settings.perm_seat";
    public static final String PERM_TRANSPORT = "hyperfactions_gui.settings.perm_transport";
    public static final String CAT_OTHER = "hyperfactions_gui.settings.cat_other";
    public static final String PERM_CRATE = "hyperfactions_gui.settings.perm_crate";
    public static final String PERM_NPC_TAME = "hyperfactions_gui.settings.perm_npc_tame";
    public static final String PERM_PVE = "hyperfactions_gui.settings.perm_pve";
    public static final String APPEARANCE = "hyperfactions_gui.settings.appearance";
    public static final String COLOR_LABEL = "hyperfactions_gui.settings.color_label";
    public static final String MOB_SPAWNING = "hyperfactions_gui.settings.mob_spawning";
    public static final String MOB_SPAWNING_HINT = "hyperfactions_gui.settings.mob_spawning_hint";
    public static final String MOB_SPAWNING_LABEL = "hyperfactions_gui.settings.mob_spawning_label";
    public static final String HOSTILE_MOBS = "hyperfactions_gui.settings.hostile_mobs";
    public static final String PASSIVE_MOBS = "hyperfactions_gui.settings.passive_mobs";
    public static final String NEUTRAL_MOBS = "hyperfactions_gui.settings.neutral_mobs";
    public static final String FACTION_SETTINGS = "hyperfactions_gui.settings.faction_settings";
    public static final String PVP_IN_TERRITORY = "hyperfactions_gui.settings.pvp_in_territory";
    public static final String OFFICERS_CAN_EDIT = "hyperfactions_gui.settings.officers_can_edit";
    public static final String LEADER_ONLY = "hyperfactions_gui.settings.leader_only";
    public static final String OFFICERS_ONLY = "hyperfactions_gui.settings.officers_only";
    public static final String DISPLAY_NONE = "hyperfactions_gui.settings.display_none";
    public static final String HOME_NOT_SET = "hyperfactions_gui.settings.home_not_set";
    public static final String NO_PERMISSION = "hyperfactions_gui.settings.no_permission";
    public static final String ONLY_LEADER_DISBAND = "hyperfactions_gui.settings.only_leader_disband";
    public static final String PERM_LOCKED = "hyperfactions_gui.settings.perm_locked";
    public static final String NO_PERM_EDIT = "hyperfactions_gui.settings.no_perm_edit";
    public static final String ONLY_LEADER_OFFICERS = "hyperfactions_gui.settings.only_leader_officers";
    public static final String PVP_ENABLED = "hyperfactions_gui.settings.pvp_enabled";
    public static final String PVP_DISABLED = "hyperfactions_gui.settings.pvp_disabled";
    public static final String NOT_IN_TERRITORY = "hyperfactions_gui.settings.not_in_territory";
    public static final String HOME_SET = "hyperfactions_gui.settings.home_set";
    public static final String RECRUITMENT_SET = "hyperfactions_gui.settings.recruitment_set";
    public static final String HOME_NO_SET = "hyperfactions_gui.settings.home_no_set";
    public static final String HOME_DELETED = "hyperfactions_gui.settings.home_deleted";

    private SettingsGui() {}
  }

  // =====================================================================
  // GUI — Modules page
  // =====================================================================

  /** Modules page labels. */
  public static final class ModulesGui {
    public static final String TITLE = "hyperfactions_gui.modules.title";
    public static final String DESCRIPTION = "hyperfactions_gui.modules.description";
    public static final String CONFIGURE_BTN = "hyperfactions_gui.modules.configure_btn";
    public static final String BACK_BTN = "hyperfactions_gui.modules.back_btn";
    public static final String TREASURY_NAME = "hyperfactions_gui.modules.treasury_name";
    public static final String TREASURY_DESC = "hyperfactions_gui.modules.treasury_desc";
    public static final String RAIDS_NAME = "hyperfactions_gui.modules.raids_name";
    public static final String RAIDS_DESC = "hyperfactions_gui.modules.raids_desc";
    public static final String LEVELS_NAME = "hyperfactions_gui.modules.levels_name";
    public static final String LEVELS_DESC = "hyperfactions_gui.modules.levels_desc";
    public static final String WAR_NAME = "hyperfactions_gui.modules.war_name";
    public static final String WAR_DESC = "hyperfactions_gui.modules.war_desc";
    public static final String COMING_SOON = "hyperfactions_gui.modules.coming_soon";
    public static final String ACTIVE = "hyperfactions_gui.modules.active";
    public static final String VIEW_TREASURY = "hyperfactions_gui.modules.view_treasury";
    public static final String UNAVAILABLE = "hyperfactions_gui.modules.unavailable";
    public static final String NO_ECONOMY = "hyperfactions_gui.modules.no_economy";
    public static final String DISABLED = "hyperfactions_gui.modules.disabled";
    public static final String ECONOMY_NOT_AVAILABLE = "hyperfactions_gui.modules.economy_not_available";

    private ModulesGui() {}
  }

  // =====================================================================
  // GUI — Treasury page
  // =====================================================================

  /** Treasury page labels and messages. */
  public static final class TreasuryGui {
    // Page labels
    public static final String TITLE = "hyperfactions_gui.treasury.title";
    public static final String BALANCE_LABEL = "hyperfactions_gui.treasury.balance_label";
    public static final String INCOME_24H = "hyperfactions_gui.treasury.income_24h";
    public static final String DEPOSITS_TRANSFERS_IN = "hyperfactions_gui.treasury.deposits_transfers_in";
    public static final String EXPENSES_24H = "hyperfactions_gui.treasury.expenses_24h";
    public static final String WITHDRAWALS_TRANSFERS_OUT = "hyperfactions_gui.treasury.withdrawals_transfers_out";
    public static final String MAINTENANCE = "hyperfactions_gui.treasury.maintenance";
    public static final String RUNWAY_LABEL = "hyperfactions_gui.treasury.runway_label";
    public static final String ADD_FUNDS = "hyperfactions_gui.treasury.add_funds";
    public static final String DEPOSIT_BTN = "hyperfactions_gui.treasury.deposit_btn";
    public static final String TAKE_FUNDS = "hyperfactions_gui.treasury.take_funds";
    public static final String WITHDRAW_BTN = "hyperfactions_gui.treasury.withdraw_btn";
    public static final String SEND_TO_FACTION = "hyperfactions_gui.treasury.send_to_faction";
    public static final String TRANSFER_BTN = "hyperfactions_gui.treasury.transfer_btn";
    public static final String TREASURY_CONFIG = "hyperfactions_gui.treasury.treasury_config";
    public static final String SETTINGS_BTN = "hyperfactions_gui.treasury.settings_btn";
    public static final String RECENT_TRANSACTIONS = "hyperfactions_gui.treasury.recent_transactions";
    public static final String NO_TRANSACTIONS = "hyperfactions_gui.treasury.no_transactions";
    public static final String COL_DATE = "hyperfactions_gui.treasury.col_date";
    public static final String COL_TYPE = "hyperfactions_gui.treasury.col_type";
    public static final String COL_BY = "hyperfactions_gui.treasury.col_by";
    public static final String COL_AMOUNT = "hyperfactions_gui.treasury.col_amount";
    public static final String COL_DETAILS = "hyperfactions_gui.treasury.col_details";
    public static final String PAY_NOW_BTN = "hyperfactions_gui.treasury.pay_now_btn";
    public static final String COST_7D = "hyperfactions_gui.treasury.cost_7d";
    public static final String COST_14D = "hyperfactions_gui.treasury.cost_14d";
    public static final String COST_30D = "hyperfactions_gui.treasury.cost_30d";
    // Dashboard labels
    public static final String WALLET_LABEL = "hyperfactions_gui.treasury.wallet_label";
    public static final String TREASURY_LABEL = "hyperfactions_gui.treasury.treasury_label";
    public static final String CHUNKS_DETAIL = "hyperfactions_gui.treasury.chunks_detail";
    public static final String COST_LABEL = "hyperfactions_gui.treasury.cost_label";
    public static final String PENDING = "hyperfactions_gui.treasury.pending";
    public static final String AUTO_PAY_ON = "hyperfactions_gui.treasury.auto_pay_on";
    public static final String AUTO_PAY_OFF = "hyperfactions_gui.treasury.auto_pay_off";
    public static final String RUNWAY_90_PLUS = "hyperfactions_gui.treasury.runway_90_plus";
    public static final String RUNWAY_DAYS = "hyperfactions_gui.treasury.runway_days";
    public static final String RUNWAY_DAY = "hyperfactions_gui.treasury.runway_day";
    public static final String RUNWAY_LESS_THAN_DAY = "hyperfactions_gui.treasury.runway_less_day";
    public static final String RUNWAY_NO_FUNDS = "hyperfactions_gui.treasury.runway_no_funds";
    public static final String GRACE_EXPIRES = "hyperfactions_gui.treasury.grace_expires";
    public static final String MISSED_PAYMENTS = "hyperfactions_gui.treasury.missed_payments";
    public static final String PAY_TO_CLEAR = "hyperfactions_gui.treasury.pay_to_clear";
    public static final String SYSTEM = "hyperfactions_gui.treasury.system";
    // Transaction types
    public static final String TYPE_DEPOSIT = "hyperfactions_gui.treasury.type_deposit";
    public static final String TYPE_WITHDRAWAL = "hyperfactions_gui.treasury.type_withdrawal";
    public static final String TYPE_TRANSFER_IN = "hyperfactions_gui.treasury.type_transfer_in";
    public static final String TYPE_TRANSFER_OUT = "hyperfactions_gui.treasury.type_transfer_out";
    public static final String TYPE_PLAYER_TRANSFER = "hyperfactions_gui.treasury.type_player_transfer";
    public static final String TYPE_UPKEEP = "hyperfactions_gui.treasury.type_upkeep";
    public static final String TYPE_TAX = "hyperfactions_gui.treasury.type_tax";
    public static final String TYPE_WAR_COST = "hyperfactions_gui.treasury.type_war_cost";
    public static final String TYPE_RAID_COST = "hyperfactions_gui.treasury.type_raid_cost";
    public static final String TYPE_SPOILS = "hyperfactions_gui.treasury.type_spoils";
    public static final String TYPE_ADMIN = "hyperfactions_gui.treasury.type_admin";
    // Deposit/Withdraw modal
    public static final String DEPOSIT_TITLE = "hyperfactions_gui.treasury.deposit_title";
    public static final String WITHDRAW_TITLE = "hyperfactions_gui.treasury.withdraw_title";
    public static final String FEE_LABEL = "hyperfactions_gui.treasury.fee_label";
    public static final String CONFIRM_DEPOSIT = "hyperfactions_gui.treasury.confirm_deposit";
    public static final String CONFIRM_WITHDRAWAL = "hyperfactions_gui.treasury.confirm_withdrawal";
    public static final String FROM_WALLET = "hyperfactions_gui.treasury.from_wallet";
    public static final String TO_WALLET = "hyperfactions_gui.treasury.to_wallet";
    public static final String ENTER_VALID_AMOUNT = "hyperfactions_gui.treasury.enter_valid_amount";
    public static final String INSUFFICIENT_WALLET = "hyperfactions_gui.treasury.insufficient_wallet";
    public static final String WALLET_WITHDRAW_FAILED = "hyperfactions_gui.treasury.wallet_withdraw_failed";
    public static final String DEPOSIT_FAILED_RETURNED = "hyperfactions_gui.treasury.deposit_failed_returned";
    public static final String DEPOSITED = "hyperfactions_gui.treasury.deposited";
    public static final String DEPOSITED_FEE = "hyperfactions_gui.treasury.deposited_fee";
    public static final String NO_WITHDRAW_PERMISSION = "hyperfactions_gui.treasury.no_withdraw_permission";
    public static final String WITHDRAW_DENIED = "hyperfactions_gui.treasury.withdraw_denied";
    public static final String INSUFFICIENT_TREASURY = "hyperfactions_gui.treasury.insufficient_treasury";
    public static final String WITHDRAW_LIMIT = "hyperfactions_gui.treasury.withdraw_limit";
    public static final String WITHDRAW_FAILED = "hyperfactions_gui.treasury.withdraw_failed";
    public static final String WALLET_DEPOSIT_WARN = "hyperfactions_gui.treasury.wallet_deposit_warn";
    public static final String WITHDREW = "hyperfactions_gui.treasury.withdrew";
    public static final String WITHDREW_FEE = "hyperfactions_gui.treasury.withdrew_fee";
    // Transfer search
    public static final String SEARCH_HINT = "hyperfactions_gui.treasury.search_hint";
    public static final String NO_RESULTS = "hyperfactions_gui.treasury.no_results";
    public static final String TAG_PLAYER = "hyperfactions_gui.treasury.tag_player";
    public static final String TAG_FACTION = "hyperfactions_gui.treasury.tag_faction";
    public static final String SOURCE_ONLINE = "hyperfactions_gui.treasury.source_online";
    public static final String SOURCE_OFFLINE = "hyperfactions_gui.treasury.source_offline";
    public static final String SOURCE_PLAYER_DB = "hyperfactions_gui.treasury.source_player_db";
    // Transfer confirm
    public static final String NO_TRANSFER_PERMISSION = "hyperfactions_gui.treasury.no_transfer_permission";
    public static final String TRANSFER_DENIED = "hyperfactions_gui.treasury.transfer_denied";
    public static final String INVALID_TARGET_FACTION = "hyperfactions_gui.treasury.invalid_target_faction";
    public static final String TARGET_FACTION_GONE = "hyperfactions_gui.treasury.target_faction_gone";
    public static final String TRANSFER_FAILED = "hyperfactions_gui.treasury.transfer_failed";
    public static final String TRANSFER_FAILED_RETURNED = "hyperfactions_gui.treasury.transfer_failed_returned";
    public static final String TRANSFERRED = "hyperfactions_gui.treasury.transferred";
    public static final String INVALID_TARGET_PLAYER = "hyperfactions_gui.treasury.invalid_target_player";
    public static final String PLAYER_TRANSFER_FAILED = "hyperfactions_gui.treasury.player_transfer_failed";
    // Treasury settings
    public static final String LEADER_ONLY_PERMS = "hyperfactions_gui.treasury.leader_only_perms";
    public static final String LEADER_ONLY_UPKEEP = "hyperfactions_gui.treasury.leader_only_upkeep";
    public static final String INVALID_LIMIT = "hyperfactions_gui.treasury.invalid_limit";
    // Treasury settings page
    public static final String SETTINGS_TITLE = "hyperfactions_gui.treasury.settings_title";
    public static final String OFFICER_PERMISSIONS = "hyperfactions_gui.treasury.officer_permissions";
    public static final String ALLOW_WITHDRAW = "hyperfactions_gui.treasury.allow_withdraw";
    public static final String ALLOW_TRANSFER = "hyperfactions_gui.treasury.allow_transfer";
    public static final String LIMITS_SECTION = "hyperfactions_gui.treasury.limits_section";
    public static final String MAX_PER_WITHDRAWAL = "hyperfactions_gui.treasury.max_per_withdrawal";
    public static final String MAX_WITHDRAWALS_PER = "hyperfactions_gui.treasury.max_withdrawals_per";
    public static final String MAX_PER_TRANSFER = "hyperfactions_gui.treasury.max_per_transfer";
    public static final String MAX_TRANSFERS_PER = "hyperfactions_gui.treasury.max_transfers_per";
    public static final String LIMIT_PERIOD = "hyperfactions_gui.treasury.limit_period";
    public static final String NO_LIMIT_HINT = "hyperfactions_gui.treasury.no_limit_hint";
    public static final String UPKEEP_SETTINGS = "hyperfactions_gui.treasury.upkeep_settings";
    public static final String AUTO_PAY_UPKEEP = "hyperfactions_gui.treasury.auto_pay_upkeep";
    // Upkeep format strings
    public static final String UPKEEP_COST_FORMAT = "hyperfactions_gui.treasury.upkeep_cost_format";
    public static final String UPKEEP_TIME_LEFT = "hyperfactions_gui.treasury.upkeep_time_left";

    private TreasuryGui() {}
  }

  // =====================================================================
  // GUI — Logs viewer
  // =====================================================================

  /** Logs viewer page labels and messages. */
  public static final class LogsGui {
    public static final String TITLE = "hyperfactions_gui.logs.title";
    public static final String ENTRY_COUNT = "hyperfactions_gui.logs.entry_count";
    public static final String FILTER_LABEL = "hyperfactions_gui.logs.filter_label";
    public static final String COL_TIME = "hyperfactions_gui.logs.col_time";
    public static final String COL_TYPE = "hyperfactions_gui.logs.col_type";
    public static final String COL_MESSAGE = "hyperfactions_gui.logs.col_message";
    public static final String PREV_BTN = "hyperfactions_gui.logs.prev_btn";
    public static final String NEXT_BTN = "hyperfactions_gui.logs.next_btn";
    public static final String ALL_TYPES = "hyperfactions_gui.logs.all_types";
    public static final String NO_LOGS_TYPE = "hyperfactions_gui.logs.no_logs_type";
    public static final String NO_LOGS = "hyperfactions_gui.logs.no_logs";
    public static final String TIME_JUST_NOW = "hyperfactions_gui.logs.time_just_now";
    public static final String TIME_MINUTE = "hyperfactions_gui.logs.time_minute";
    public static final String TIME_MINUTES = "hyperfactions_gui.logs.time_minutes";
    public static final String TIME_HOUR = "hyperfactions_gui.logs.time_hour";
    public static final String TIME_HOURS = "hyperfactions_gui.logs.time_hours";
    public static final String TIME_DAY = "hyperfactions_gui.logs.time_day";
    public static final String TIME_DAYS = "hyperfactions_gui.logs.time_days";
    public static final String TIME_WEEK = "hyperfactions_gui.logs.time_week";
    public static final String TIME_WEEKS = "hyperfactions_gui.logs.time_weeks";
    public static final String TYPE_MEMBER_JOIN = "hyperfactions_gui.logs.type_member_join";
    public static final String TYPE_MEMBER_LEAVE = "hyperfactions_gui.logs.type_member_leave";
    public static final String TYPE_MEMBER_KICK = "hyperfactions_gui.logs.type_member_kick";
    public static final String TYPE_MEMBER_PROMOTE = "hyperfactions_gui.logs.type_member_promote";
    public static final String TYPE_MEMBER_DEMOTE = "hyperfactions_gui.logs.type_member_demote";
    public static final String TYPE_CLAIM = "hyperfactions_gui.logs.type_claim";
    public static final String TYPE_UNCLAIM = "hyperfactions_gui.logs.type_unclaim";
    public static final String TYPE_OVERCLAIM = "hyperfactions_gui.logs.type_overclaim";
    public static final String TYPE_HOME_SET = "hyperfactions_gui.logs.type_home_set";
    public static final String TYPE_RELATION_ALLY = "hyperfactions_gui.logs.type_relation_ally";
    public static final String TYPE_RELATION_ENEMY = "hyperfactions_gui.logs.type_relation_enemy";
    public static final String TYPE_RELATION_NEUTRAL = "hyperfactions_gui.logs.type_relation_neutral";
    public static final String TYPE_LEADER_TRANSFER = "hyperfactions_gui.logs.type_leader_transfer";
    public static final String TYPE_SETTINGS_CHANGE = "hyperfactions_gui.logs.type_settings_change";
    public static final String TYPE_POWER_CHANGE = "hyperfactions_gui.logs.type_power_change";
    public static final String TYPE_ECONOMY = "hyperfactions_gui.logs.type_economy";
    public static final String TYPE_ADMIN_POWER = "hyperfactions_gui.logs.type_admin_power";

    /** Derives the lang key for a FactionLog.LogType enum by name. */
    public static String typeKey(String logTypeName) {
      return "hyperfactions_gui.logs.type_" + logTypeName.toLowerCase();
    }

    // === Log message templates (i18n for FactionLog.message content) ===

    // Player actions
    public static final String MSG_FACTION_CREATED = "hyperfactions_gui.logs.msg_faction_created";
    public static final String MSG_MEMBER_JOINED = "hyperfactions_gui.logs.msg_member_joined";
    public static final String MSG_MEMBER_LEFT = "hyperfactions_gui.logs.msg_member_left";
    public static final String MSG_MEMBER_KICKED = "hyperfactions_gui.logs.msg_member_kicked";
    public static final String MSG_MEMBER_PROMOTED = "hyperfactions_gui.logs.msg_member_promoted";
    public static final String MSG_MEMBER_DEMOTED = "hyperfactions_gui.logs.msg_member_demoted";
    public static final String MSG_LEADER_TRANSFERRED = "hyperfactions_gui.logs.msg_leader_transferred";
    public static final String MSG_LEADER_LEFT_TRANSFER = "hyperfactions_gui.logs.msg_leader_left_transfer";
    public static final String MSG_RELATION_SET = "hyperfactions_gui.logs.msg_relation_set";

    // Territory
    public static final String MSG_CLAIMED = "hyperfactions_gui.logs.msg_claimed";
    public static final String MSG_UNCLAIMED = "hyperfactions_gui.logs.msg_unclaimed";
    public static final String MSG_OVERCLAIM_LOST = "hyperfactions_gui.logs.msg_overclaim_lost";
    public static final String MSG_OVERCLAIM_TAKEN = "hyperfactions_gui.logs.msg_overclaim_taken";
    public static final String MSG_ALL_UNCLAIMED = "hyperfactions_gui.logs.msg_all_unclaimed";
    public static final String MSG_CLAIM_REMOVED_WORLD = "hyperfactions_gui.logs.msg_claim_removed_world";
    public static final String MSG_CLAIMS_LOST_UPKEEP = "hyperfactions_gui.logs.msg_claims_lost_upkeep";
    public static final String MSG_CLAIMS_REMOVED_INACTIVE = "hyperfactions_gui.logs.msg_claims_removed_inactive";

    // Home
    public static final String MSG_HOME_SET = "hyperfactions_gui.logs.msg_home_set";
    public static final String MSG_HOME_CLEARED = "hyperfactions_gui.logs.msg_home_cleared";
    public static final String MSG_HOME_CLEARED_WORLD = "hyperfactions_gui.logs.msg_home_cleared_world";

    // Settings
    public static final String MSG_RENAMED = "hyperfactions_gui.logs.msg_renamed";
    public static final String MSG_SET_OPEN = "hyperfactions_gui.logs.msg_set_open";
    public static final String MSG_SET_CLOSED = "hyperfactions_gui.logs.msg_set_closed";
    public static final String MSG_DESC_SET = "hyperfactions_gui.logs.msg_desc_set";
    public static final String MSG_DESC_CLEARED = "hyperfactions_gui.logs.msg_desc_cleared";
    public static final String MSG_COLOR_CHANGED = "hyperfactions_gui.logs.msg_color_changed";

    // Economy
    public static final String MSG_DEPOSIT = "hyperfactions_gui.logs.msg_deposit";
    public static final String MSG_WITHDRAWAL = "hyperfactions_gui.logs.msg_withdrawal";
    public static final String MSG_UPKEEP_PAID = "hyperfactions_gui.logs.msg_upkeep_paid";
    public static final String MSG_UPKEEP_GRACE_STARTED = "hyperfactions_gui.logs.msg_upkeep_grace_started";
    public static final String MSG_UPKEEP_MISSED = "hyperfactions_gui.logs.msg_upkeep_missed";
    public static final String MSG_UPKEEP_MANUAL = "hyperfactions_gui.logs.msg_upkeep_manual";

    // Admin power
    public static final String MSG_ADMIN_POWER_SET = "hyperfactions_gui.logs.msg_admin_power_set";
    public static final String MSG_ADMIN_POWER_ADD = "hyperfactions_gui.logs.msg_admin_power_add";
    public static final String MSG_ADMIN_POWER_REMOVE = "hyperfactions_gui.logs.msg_admin_power_remove";
    public static final String MSG_ADMIN_POWER_RESET = "hyperfactions_gui.logs.msg_admin_power_reset";
    public static final String MSG_ADMIN_POWER_ADJUSTED = "hyperfactions_gui.logs.msg_admin_power_adjusted";
    public static final String MSG_ADMIN_MAXPOWER_SET = "hyperfactions_gui.logs.msg_admin_maxpower_set";
    public static final String MSG_ADMIN_MAXPOWER_RESET = "hyperfactions_gui.logs.msg_admin_maxpower_reset";
    public static final String MSG_ADMIN_POWERLOSS_ENABLED = "hyperfactions_gui.logs.msg_admin_powerloss_enabled";
    public static final String MSG_ADMIN_POWERLOSS_DISABLED = "hyperfactions_gui.logs.msg_admin_powerloss_disabled";
    public static final String MSG_ADMIN_DECAY_ENABLED = "hyperfactions_gui.logs.msg_admin_decay_enabled";
    public static final String MSG_ADMIN_DECAY_DISABLED = "hyperfactions_gui.logs.msg_admin_decay_disabled";
    public static final String MSG_ADMIN_KD_RESET = "hyperfactions_gui.logs.msg_admin_kd_reset";
    public static final String MSG_ADMIN_POWER_SET_ALL = "hyperfactions_gui.logs.msg_admin_power_set_all";
    public static final String MSG_ADMIN_POWER_ADD_ALL = "hyperfactions_gui.logs.msg_admin_power_add_all";
    public static final String MSG_ADMIN_POWER_REMOVE_ALL = "hyperfactions_gui.logs.msg_admin_power_remove_all";
    public static final String MSG_ADMIN_POWER_RESET_ALL = "hyperfactions_gui.logs.msg_admin_power_reset_all";
    public static final String MSG_ADMIN_POWER_ADJUSTED_ALL = "hyperfactions_gui.logs.msg_admin_power_adjusted_all";

    // Admin faction
    public static final String MSG_ADMIN_KICKED = "hyperfactions_gui.logs.msg_admin_kicked";
    public static final String MSG_ADMIN_ROLE_SET = "hyperfactions_gui.logs.msg_admin_role_set";
    public static final String MSG_ADMIN_LEADER_KICK = "hyperfactions_gui.logs.msg_admin_leader_kick";
    public static final String MSG_ADMIN_ECON_ADDED = "hyperfactions_gui.logs.msg_admin_econ_added";
    public static final String MSG_ADMIN_ECON_DEDUCTED = "hyperfactions_gui.logs.msg_admin_econ_deducted";
    public static final String MSG_ADMIN_ECON_SET = "hyperfactions_gui.logs.msg_admin_econ_set";

    // Import
    public static final String MSG_LEFT_IMPORT = "hyperfactions_gui.logs.msg_left_import";
    public static final String MSG_LEADER_IMPORT_TRANSFER = "hyperfactions_gui.logs.msg_leader_import_transfer";
    public static final String MSG_IMPORTED_FROM = "hyperfactions_gui.logs.msg_imported_from";

    private LogsGui() {}
  }

  // =====================================================================
  // GUI — Chat page
  // =====================================================================

  /** Faction chat page labels and messages. */
  public static final class ChatGui {
    public static final String TITLE = "hyperfactions_gui.chat.title";
    public static final String TAB_FACTION = "hyperfactions_gui.chat.tab_faction";
    public static final String TAB_ALLY = "hyperfactions_gui.chat.tab_ally";
    public static final String SEND_BTN = "hyperfactions_gui.chat.send_btn";
    public static final String PLACEHOLDER = "hyperfactions_gui.chat.placeholder";
    public static final String NO_MESSAGES = "hyperfactions_gui.chat.no_messages";
    public static final String NO_ALLY_PERMISSION = "hyperfactions_gui.chat.no_ally_permission";
    public static final String NO_PERMISSION = "hyperfactions_gui.chat.no_permission";
    public static final String FACTION_GONE = "hyperfactions_gui.chat.faction_gone";
    public static final String TIME_NOW = "hyperfactions_gui.chat.time_now";
    public static final String TIME_MINUTES = "hyperfactions_gui.chat.time_minutes";
    public static final String TIME_HOURS = "hyperfactions_gui.chat.time_hours";

    private ChatGui() {}
  }

  // =====================================================================
  // GUI — Invites page
  // =====================================================================

  /** Faction invites page labels and messages. */
  public static final class InvitesGui {
    public static final String TITLE = "hyperfactions_gui.invites.title";
    public static final String TAB_OUTGOING = "hyperfactions_gui.invites.tab_outgoing";
    public static final String TAB_REQUESTS = "hyperfactions_gui.invites.tab_requests";
    public static final String PREV_BTN = "hyperfactions_gui.invites.prev_btn";
    public static final String NEXT_BTN = "hyperfactions_gui.invites.next_btn";
    public static final String INVITE_COUNT = "hyperfactions_gui.invites.invite_count";
    public static final String REQUEST_COUNT = "hyperfactions_gui.invites.request_count";
    public static final String INVITED_BY = "hyperfactions_gui.invites.invited_by";
    public static final String NO_MESSAGE = "hyperfactions_gui.invites.no_message";
    public static final String EXPIRES = "hyperfactions_gui.invites.expires";
    public static final String TYPE_OUTGOING = "hyperfactions_gui.invites.type_outgoing";
    public static final String TYPE_REQUEST = "hyperfactions_gui.invites.type_request";
    public static final String INVITED_BY_LABEL = "hyperfactions_gui.invites.invited_by_label";
    public static final String EMPTY_OUTGOING = "hyperfactions_gui.invites.empty_outgoing";
    public static final String EMPTY_REQUESTS = "hyperfactions_gui.invites.empty_requests";
    public static final String INVALID_PLAYER = "hyperfactions_gui.invites.invalid_player";
    public static final String CANCELLED_INVITE = "hyperfactions_gui.invites.cancelled_invite";
    public static final String PLAYER_JOINED = "hyperfactions_gui.invites.player_joined";
    public static final String FACTION_FULL = "hyperfactions_gui.invites.faction_full";
    public static final String ADD_FAILED = "hyperfactions_gui.invites.add_failed";
    public static final String REQUEST_EXPIRED = "hyperfactions_gui.invites.request_expired";
    public static final String REQUEST_DECLINED = "hyperfactions_gui.invites.request_declined";
    public static final String TIME_SECONDS = "hyperfactions_gui.invites.time_seconds";
    public static final String TIME_MINUTES = "hyperfactions_gui.invites.time_minutes";
    public static final String TIME_HOURS = "hyperfactions_gui.invites.time_hours";
    public static final String LABEL_MESSAGE = "hyperfactions_gui.invites.label_message";
    public static final String BTN_CANCEL = "hyperfactions_gui.invites.btn_cancel";
    public static final String BTN_ACCEPT = "hyperfactions_gui.invites.btn_accept";
    public static final String BTN_DECLINE = "hyperfactions_gui.invites.btn_decline";

    private InvitesGui() {}
  }

  // =====================================================================
  // GUI — Map page
  // =====================================================================

  /** Chunk map page labels and messages. */
  public static final class MapGui {
    public static final String TITLE = "hyperfactions_gui.map.title";
    public static final String ACTION_HINT = "hyperfactions_gui.map.action_hint";
    public static final String LEGEND_YOUR = "hyperfactions_gui.map.legend_your";
    public static final String LEGEND_ALLY = "hyperfactions_gui.map.legend_ally";
    public static final String LEGEND_ENEMY = "hyperfactions_gui.map.legend_enemy";
    public static final String LEGEND_OTHER = "hyperfactions_gui.map.legend_other";
    public static final String LEGEND_WILDERNESS = "hyperfactions_gui.map.legend_wilderness";
    public static final String LEGEND_SAFE = "hyperfactions_gui.map.legend_safe";
    public static final String LEGEND_WAR = "hyperfactions_gui.map.legend_war";
    public static final String LEGEND_YOU = "hyperfactions_gui.map.legend_you";
    public static final String POSITION = "hyperfactions_gui.map.position";
    public static final String LEGEND_PROTECTED = "hyperfactions_gui.map.legend_protected";
    public static final String CLAIM_STATS = "hyperfactions_gui.map.claim_stats";
    public static final String OVERCLAIMED = "hyperfactions_gui.map.overclaimed";
    public static final String POWER_DISPLAY = "hyperfactions_gui.map.power_display";
    public static final String JOIN_TO_CLAIM = "hyperfactions_gui.map.join_to_claim";
    // Claim results
    public static final String CLAIM_SUCCESS = "hyperfactions_gui.map.claim_success";
    public static final String CLAIM_NOT_IN_FACTION = "hyperfactions_gui.map.claim_not_in_faction";
    public static final String CLAIM_NOT_OFFICER = "hyperfactions_gui.map.claim_not_officer";
    public static final String CLAIM_ALREADY_YOURS = "hyperfactions_gui.map.claim_already_yours";
    public static final String CLAIM_ALREADY_CLAIMED = "hyperfactions_gui.map.claim_already_claimed";
    public static final String CLAIM_NOT_ADJACENT = "hyperfactions_gui.map.claim_not_adjacent";
    public static final String CLAIM_MAX = "hyperfactions_gui.map.claim_max";
    public static final String CLAIM_WORLD_MAX = "hyperfactions_gui.map.claim_world_max";
    public static final String CLAIM_WORLD_NOT_ALLOWED = "hyperfactions_gui.map.claim_world_not_allowed";
    public static final String CLAIM_ORBISGUARD = "hyperfactions_gui.map.claim_orbisguard";
    public static final String CLAIM_FAILED = "hyperfactions_gui.map.claim_failed";
    // Unclaim results
    public static final String UNCLAIM_SUCCESS = "hyperfactions_gui.map.unclaim_success";
    public static final String UNCLAIM_NOT_IN_FACTION = "hyperfactions_gui.map.unclaim_not_in_faction";
    public static final String UNCLAIM_NOT_OFFICER = "hyperfactions_gui.map.unclaim_not_officer";
    public static final String UNCLAIM_NOT_CLAIMED = "hyperfactions_gui.map.unclaim_not_claimed";
    public static final String UNCLAIM_NOT_YOURS = "hyperfactions_gui.map.unclaim_not_yours";
    public static final String UNCLAIM_HOME = "hyperfactions_gui.map.unclaim_home";
    public static final String UNCLAIM_FAILED = "hyperfactions_gui.map.unclaim_failed";
    // Overclaim results
    public static final String OVERCLAIM_SUCCESS = "hyperfactions_gui.map.overclaim_success";
    public static final String OVERCLAIM_NOT_IN_FACTION = "hyperfactions_gui.map.overclaim_not_in_faction";
    public static final String OVERCLAIM_NOT_OFFICER = "hyperfactions_gui.map.overclaim_not_officer";
    public static final String OVERCLAIM_ALREADY_YOURS = "hyperfactions_gui.map.overclaim_already_yours";
    public static final String OVERCLAIM_ALLY = "hyperfactions_gui.map.overclaim_ally";
    public static final String OVERCLAIM_HAS_POWER = "hyperfactions_gui.map.overclaim_has_power";
    public static final String OVERCLAIM_MAX = "hyperfactions_gui.map.overclaim_max";
    public static final String OVERCLAIM_WORLD_MAX = "hyperfactions_gui.map.overclaim_world_max";
    public static final String OVERCLAIM_FAILED = "hyperfactions_gui.map.overclaim_failed";

    private MapGui() {}
  }

  // =====================================================================
  // GUI — Create faction page
  // =====================================================================

  /** Create faction page labels and messages. */
  public static final class CreateGui {
    public static final String PREVIEW_NAME = "hyperfactions_gui.create.preview_name";
    public static final String LEADER_PREFIX = "hyperfactions_gui.create.leader_prefix";
    public static final String ENTER_NAME = "hyperfactions_gui.create.enter_name";
    public static final String NAME_TOO_SHORT = "hyperfactions_gui.create.name_too_short";
    public static final String NAME_TOO_LONG = "hyperfactions_gui.create.name_too_long";
    public static final String NAME_TAKEN = "hyperfactions_gui.create.name_taken";
    public static final String TAG_LENGTH = "hyperfactions_gui.create.tag_length";
    public static final String TAG_FORMAT = "hyperfactions_gui.create.tag_format";
    public static final String DESC_TOO_LONG = "hyperfactions_gui.create.desc_too_long";
    public static final String CREATED = "hyperfactions_gui.create.created";
    public static final String CREATED_NO_DASHBOARD = "hyperfactions_gui.create.created_no_dashboard";
    public static final String INVALID_NAME = "hyperfactions_gui.create.invalid_name";
    public static final String CREATE_FAILED = "hyperfactions_gui.create.create_failed";
    // Static UI labels
    public static final String TITLE = "hyperfactions_gui.create.title";
    public static final String SECTION_PREVIEW = "hyperfactions_gui.create.section_preview";
    public static final String SECTION_BASIC_INFO = "hyperfactions_gui.create.section_basic_info";
    public static final String SECTION_DETAILS = "hyperfactions_gui.create.section_details";
    public static final String NAME_PREFIX = "hyperfactions_gui.create.name_prefix";
    public static final String FACTION_NAME_LABEL = "hyperfactions_gui.create.faction_name_label";
    public static final String TAG_LABEL = "hyperfactions_gui.create.tag_label";
    public static final String DESC_LABEL = "hyperfactions_gui.create.desc_label";
    public static final String RECRUITMENT_LABEL = "hyperfactions_gui.create.recruitment_label";
    public static final String SECTION_FACTION_COLOR = "hyperfactions_gui.create.section_faction_color";
    public static final String SECTION_COMBAT = "hyperfactions_gui.create.section_combat";
    public static final String CREATE_BTN = "hyperfactions_gui.create.create_btn";

    private CreateGui() {}
  }

  // =====================================================================
  // GUI — New player page
  // =====================================================================

  /** New player page labels and messages (invites, browse, map). */
  public static final class NewPlayerGui {
    // Page titles and static labels
    public static final String BROWSE_TITLE = "hyperfactions_gui.newplayer.browse_title";
    public static final String INVITES_TITLE = "hyperfactions_gui.newplayer.invites_title";
    public static final String MAP_TITLE = "hyperfactions_gui.newplayer.map_title";
    public static final String VIEW_ONLY_BADGE = "hyperfactions_gui.newplayer.view_only_badge";
    public static final String LEGEND_LABEL = "hyperfactions_gui.newplayer.legend_label";
    public static final String LEGEND_SAFEZONE = "hyperfactions_gui.newplayer.legend_safezone";
    public static final String LEGEND_WARZONE = "hyperfactions_gui.newplayer.legend_warzone";
    public static final String LEGEND_FACTION = "hyperfactions_gui.newplayer.legend_faction";
    public static final String LEGEND_WILDERNESS = "hyperfactions_gui.newplayer.legend_wilderness";
    public static final String SEARCH_LABEL = "hyperfactions_gui.newplayer.search_label";
    public static final String SORT_LABEL = "hyperfactions_gui.newplayer.sort_label";
    public static final String PREV_BTN = "hyperfactions_gui.newplayer.prev_btn";
    public static final String NEXT_BTN = "hyperfactions_gui.newplayer.next_btn";
    // Invites page
    public static final String PENDING_COUNT = "hyperfactions_gui.newplayer.pending_count";
    public static final String RECEIVED_HEADER = "hyperfactions_gui.newplayer.received_header";
    public static final String REQUESTS_HEADER = "hyperfactions_gui.newplayer.requests_header";
    public static final String NO_INVITES = "hyperfactions_gui.newplayer.no_invites";
    public static final String NO_REQUESTS = "hyperfactions_gui.newplayer.no_requests";
    public static final String INVITED_BY = "hyperfactions_gui.newplayer.invited_by";
    public static final String POWER_COUNT = "hyperfactions_gui.newplayer.power_count";
    public static final String CLAIM_COUNT = "hyperfactions_gui.newplayer.claim_count";
    public static final String AWAITING_REVIEW = "hyperfactions_gui.newplayer.awaiting_review";
    public static final String EXPIRES_IN = "hyperfactions_gui.newplayer.expires_in";
    public static final String TIME_JUST_NOW = "hyperfactions_gui.newplayer.time_just_now";
    public static final String TIME_MINUTES = "hyperfactions_gui.newplayer.time_minutes";
    public static final String TIME_HOURS = "hyperfactions_gui.newplayer.time_hours";
    public static final String TIME_DAYS = "hyperfactions_gui.newplayer.time_days";
    // Shared join result messages
    public static final String INVALID_FACTION = "hyperfactions_gui.newplayer.invalid_faction";
    public static final String INVITE_EXPIRED = "hyperfactions_gui.newplayer.invite_expired";
    public static final String FACTION_GONE = "hyperfactions_gui.newplayer.faction_gone";
    public static final String JOINED = "hyperfactions_gui.newplayer.joined";
    public static final String FACTION_FULL = "hyperfactions_gui.newplayer.faction_full";
    public static final String JOIN_FAILED = "hyperfactions_gui.newplayer.join_failed";
    public static final String INVITE_DECLINED = "hyperfactions_gui.newplayer.invite_declined";
    public static final String REQUEST_CANCELLED = "hyperfactions_gui.newplayer.request_cancelled";
    // Browse page
    public static final String FACTION_COUNT = "hyperfactions_gui.newplayer.faction_count";
    public static final String BROWSE_SUBTITLE = "hyperfactions_gui.newplayer.browse_subtitle";
    public static final String SORT_POWER = "hyperfactions_gui.newplayer.sort_power";
    public static final String SORT_NAME = "hyperfactions_gui.newplayer.sort_name";
    public static final String SORT_MEMBERS = "hyperfactions_gui.newplayer.sort_members";
    public static final String BTN_ACCEPT = "hyperfactions_gui.newplayer.btn_accept";
    public static final String BTN_PENDING = "hyperfactions_gui.newplayer.btn_pending";
    public static final String BTN_JOIN = "hyperfactions_gui.newplayer.btn_join";
    public static final String BTN_REQUEST = "hyperfactions_gui.newplayer.btn_request";
    public static final String INVITE_ONLY_MSG = "hyperfactions_gui.newplayer.invite_only_msg";
    public static final String WELCOME_HINT = "hyperfactions_gui.newplayer.welcome_hint";
    public static final String FACTION_OPEN_HINT = "hyperfactions_gui.newplayer.faction_open_hint";
    public static final String ALREADY_REQUESTED = "hyperfactions_gui.newplayer.already_requested";
    public static final String HAS_INVITE_HINT = "hyperfactions_gui.newplayer.has_invite_hint";
    public static final String REQUEST_SENT = "hyperfactions_gui.newplayer.request_sent";
    public static final String OFFICER_REVIEW = "hyperfactions_gui.newplayer.officer_review";
    // Map page
    public static final String MAP_HINT = "hyperfactions_gui.newplayer.map_hint";

    private NewPlayerGui() {}
  }

  // =====================================================================
  // GUI — Player settings
  // =====================================================================

  /** Player settings page labels and messages. */
  public static final class PlayerSettings {
    public static final String TITLE = "hyperfactions_gui.player_settings.title";
    public static final String LANGUAGE_SECTION = "hyperfactions_gui.player_settings.language_section";
    public static final String AUTO_DETECT = "hyperfactions_gui.player_settings.auto_detect";
    public static final String AUTO_DETECT_DESC = "hyperfactions_gui.player_settings.auto_detect_desc";
    public static final String LANGUAGE_LABEL = "hyperfactions_gui.player_settings.language_label";
    public static final String NOTIFICATIONS_SECTION = "hyperfactions_gui.player_settings.notifications_section";
    public static final String TERRITORY_ALERTS = "hyperfactions_gui.player_settings.territory_alerts";
    public static final String TERRITORY_ALERTS_DESC = "hyperfactions_gui.player_settings.territory_alerts_desc";
    public static final String DEATH_ANNOUNCEMENTS = "hyperfactions_gui.player_settings.death_announcements";
    public static final String DEATH_ANNOUNCEMENTS_DESC = "hyperfactions_gui.player_settings.death_announcements_desc";
    public static final String POWER_NOTIFICATIONS = "hyperfactions_gui.player_settings.power_notifications";
    public static final String POWER_NOTIFICATIONS_DESC = "hyperfactions_gui.player_settings.power_notifications_desc";
    public static final String LANGUAGE_CHANGED = "hyperfactions_gui.player_settings.language_changed";
    public static final String PREF_ENABLED = "hyperfactions_gui.player_settings.pref_enabled";
    public static final String PREF_DISABLED = "hyperfactions_gui.player_settings.pref_disabled";

    private PlayerSettings() {}
  }
}
