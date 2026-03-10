package com.hyperfactions.gui;

/**
 * Centralized UI template path constants.
 * All paths are relative to the plugin's Custom/Pages/ asset root.
 */
public final class UIPaths {

  private UIPaths() {}

  private static final String BASE = "HyperFactions/";

  // ── Shared ──────────────────────────────────────────────────────────────
  public static final String STYLES = BASE + "shared/styles.ui";

  public static final String MAIN_MENU = BASE + "shared/main_menu.ui";

  public static final String MAIN_MENU_FACTION = BASE + "shared/main_menu_faction.ui";

  public static final String MAIN_MENU_NO_FACTION = BASE + "shared/main_menu_no_faction.ui";

  public static final String MAIN_MENU_BROWSE = BASE + "shared/main_menu_browse.ui";

  public static final String MAIN_MENU_TERRITORY = BASE + "shared/main_menu_territory.ui";

  public static final String MAIN_MENU_ADMIN = BASE + "shared/main_menu_admin.ui";

  public static final String MENU_SECTION = BASE + "shared/menu_section.ui";

  public static final String MODAL_CONFIRMATION = BASE + "shared/modal_confirmation.ui";

  public static final String MODAL_INPUT = BASE + "shared/modal_input.ui";

  public static final String MODAL_INPUT_MULTILINE = BASE + "shared/modal_input_multiline.ui";

  public static final String DESCRIPTION_MODAL = BASE + "shared/description_modal.ui";

  public static final String TAG_MODAL = BASE + "shared/tag_modal.ui";

  public static final String RENAME_MODAL = BASE + "shared/rename_modal.ui";

  public static final String FACTION_INFO = BASE + "shared/faction_info.ui";

  public static final String PLACEHOLDER_PAGE = BASE + "shared/placeholder_page.ui";

  public static final String ERROR_PAGE = BASE + "shared/error_page.ui";

  public static final String PLAYER_SETTINGS = BASE + "shared/player_settings.ui";

  public static final String INVITE_NOTIFICATION = BASE + "shared/invite_notification.ui";

  public static final String DISBAND_CONFIRM = BASE + "shared/disband_confirm.ui";

  public static final String LEAVE_CONFIRM = BASE + "shared/leave_confirm.ui";

  public static final String LEADER_LEAVE_CONFIRM = BASE + "shared/leader_leave_confirm.ui";

  public static final String NO_FACTION_ACTIONS = BASE + "shared/no_faction_actions.ui";

  // ── Navigation ──────────────────────────────────────────────────────────
  public static final String NAV_BUTTON = BASE + "nav/nav_button.ui";

  // ── Faction pages ───────────────────────────────────────────────────────
  public static final String FACTION_MAIN = BASE + "faction/faction_main.ui";

  public static final String FACTION_DASHBOARD = BASE + "faction/faction_dashboard.ui";

  public static final String FACTION_ACTIONS = BASE + "faction/faction_actions.ui";

  public static final String DASHBOARD_ACTION_BTN = BASE + "faction/dashboard_action_btn.ui";

  public static final String FACTION_MEMBERS = BASE + "faction/faction_members.ui";

  public static final String MEMBER_ENTRY = BASE + "faction/member_entry.ui";

  public static final String FACTION_SETTINGS = BASE + "faction/faction_settings.ui";

  public static final String FACTION_MODULES = BASE + "faction/faction_modules.ui";

  public static final String FACTION_RELATIONS = BASE + "faction/faction_relations.ui";

  public static final String FACTION_RELATION_ENTRY = BASE + "faction/faction_relation_entry.ui";

  public static final String RELATION_EMPTY = BASE + "faction/relation_empty.ui";

  public static final String SET_RELATION_MODAL = BASE + "faction/set_relation_modal.ui";

  public static final String SET_RELATION_CARD = BASE + "faction/set_relation_card.ui";

  public static final String INDICATOR_ALLY = BASE + "faction/indicator_ally.ui";

  public static final String INDICATOR_ENEMY = BASE + "faction/indicator_enemy.ui";

  public static final String FACTION_INVITES = BASE + "faction/faction_invites.ui";

  public static final String FACTION_INVITE_ENTRY = BASE + "faction/faction_invite_entry.ui";

  public static final String FACTION_BROWSER = BASE + "faction/faction_browser.ui";

  public static final String FACTION_BROWSE_ENTRY = BASE + "faction/faction_browse_entry.ui";

  public static final String FACTION_LEADERBOARD = BASE + "faction/faction_leaderboard.ui";

  public static final String FACTION_LEADERBOARD_ENTRY = BASE + "faction/faction_leaderboard_entry.ui";

  public static final String FACTION_CHAT = BASE + "faction/faction_chat.ui";

  public static final String CHAT_MESSAGE_ENTRY = BASE + "faction/chat_message_entry.ui";

  public static final String FACTION_TREASURY = BASE + "faction/faction_treasury.ui";

  public static final String TREASURY_SETTINGS = BASE + "faction/treasury_settings.ui";

  public static final String TREASURY_DEPOSIT_MODAL = BASE + "faction/treasury_deposit_modal.ui";

  public static final String TREASURY_TRANSFER_SEARCH = BASE + "faction/treasury_transfer_search.ui";

  public static final String TRANSFER_SEARCH_ENTRY = BASE + "faction/transfer_search_entry.ui";

  public static final String TREASURY_TRANSFER_CONFIRM = BASE + "faction/treasury_transfer_confirm.ui";

  public static final String PLAYER_INFO = BASE + "faction/player_info.ui";

  public static final String TRANSFER_CONFIRM = BASE + "faction/transfer_confirm.ui";

  public static final String LOGS_VIEWER = BASE + "faction/logs_viewer.ui";

  public static final String LOG_ENTRY = BASE + "faction/log_entry.ui";

  public static final String ACTIVITY_ENTRY = BASE + "faction/activity_entry.ui";

  public static final String HISTORY_ENTRY = BASE + "faction/history_entry.ui";

  // ── Chunk map ───────────────────────────────────────────────────────────
  public static final String CHUNK_MAP = BASE + "faction/chunk_map.ui";

  public static final String CHUNK_MAP_TERRAIN = BASE + "faction/chunk_map_terrain.ui";

  public static final String CHUNK_MAP_TERRAIN_BTN = BASE + "faction/chunk_map_terrain_btn.ui";

  public static final String CHUNK_MAP_MARKER = BASE + "faction/chunk_map_marker.ui";

  public static final String CHUNK_MARKER = BASE + "faction/chunk_marker.ui";

  public static final String CHUNK_BTN = BASE + "faction/chunk_btn.ui";

  // ── New player pages ────────────────────────────────────────────────────
  public static final String NEWPLAYER_BROWSE = BASE + "newplayer/browse.ui";

  public static final String NEWPLAYER_FACTION_ENTRY = BASE + "newplayer/newplayer_faction_entry.ui";

  public static final String NEWPLAYER_CREATE_FACTION = BASE + "newplayer/create_faction.ui";

  public static final String NEWPLAYER_INVITES = BASE + "newplayer/invites.ui";

  public static final String NEWPLAYER_INVITE_CARD = BASE + "newplayer/invite_card.ui";

  public static final String NEWPLAYER_REQUEST_CARD = BASE + "newplayer/request_card.ui";

  public static final String NEWPLAYER_HELP = BASE + "newplayer/help.ui";

  // ── Help pages ──────────────────────────────────────────────────────────
  public static final String HELP_MAIN = BASE + "help/help_main.ui";

  public static final String HELP_TOPIC_CARD = BASE + "help/help_topic_card.ui";

  public static final String HELP_LINE_TEXT = BASE + "help/help_line_text.ui";

  public static final String HELP_LINE_COMMAND = BASE + "help/help_line_command.ui";

  public static final String HELP_LINE_TIP = BASE + "help/help_line_tip.ui";

  public static final String HELP_LINE_HEADING = BASE + "help/help_line_heading.ui";

  public static final String HELP_SPACER = BASE + "help/help_spacer.ui";

  public static final String HELP_LINE_BOLD = BASE + "help/help_line_bold.ui";

  public static final String HELP_LINE_ITALIC = BASE + "help/help_line_italic.ui";

  public static final String HELP_LINE_LIST = BASE + "help/help_line_list.ui";

  public static final String HELP_SEPARATOR = BASE + "help/help_separator.ui";

  public static final String HELP_LINE_CALLOUT = BASE + "help/help_line_callout.ui";

  // ── Admin pages ─────────────────────────────────────────────────────────
  public static final String ADMIN_MAIN = BASE + "admin/admin_main.ui";

  public static final String ADMIN_DASHBOARD = BASE + "admin/admin_dashboard.ui";

  public static final String ADMIN_NAV_BUTTON = BASE + "admin/admin_nav_button.ui";

  public static final String ADMIN_HELP = BASE + "admin/admin_help.ui";

  public static final String ADMIN_ACTIONS = BASE + "admin/admin_actions.ui";

  public static final String ADMIN_CONFIG = BASE + "admin/admin_config.ui";

  public static final String ADMIN_VERSION = BASE + "admin/admin_version.ui";

  public static final String ADMIN_UPDATES = BASE + "admin/admin_updates.ui";

  public static final String ADMIN_BACKUPS = BASE + "admin/admin_backups.ui";

  public static final String ADMIN_FACTIONS = BASE + "admin/admin_factions.ui";

  public static final String ADMIN_FACTION_ENTRY = BASE + "admin/admin_faction_entry.ui";

  public static final String ADMIN_FACTION_INFO = BASE + "admin/admin_faction_info.ui";

  public static final String ADMIN_FACTION_SETTINGS = BASE + "admin/admin_faction_settings.ui";

  public static final String ADMIN_FACTION_MEMBERS = BASE + "admin/admin_faction_members.ui";

  public static final String ADMIN_FACTION_MEMBERS_ENTRY = BASE + "admin/admin_faction_members_entry.ui";

  public static final String ADMIN_FACTION_RELATIONS = BASE + "admin/admin_faction_relations.ui";

  public static final String ADMIN_FACTION_RELATIONS_ENTRY = BASE + "admin/admin_faction_relations_entry.ui";

  public static final String ADMIN_PLAYERS = BASE + "admin/admin_players.ui";

  public static final String ADMIN_PLAYER_ENTRY = BASE + "admin/admin_player_entry.ui";

  public static final String ADMIN_PLAYER_INFO = BASE + "admin/admin_player_info.ui";

  public static final String ADMIN_HISTORY_ENTRY = BASE + "admin/admin_history_entry.ui";

  public static final String ADMIN_ECONOMY = BASE + "admin/admin_economy.ui";

  public static final String ADMIN_ECONOMY_ENTRY = BASE + "admin/admin_economy_entry.ui";

  public static final String ADMIN_ECONOMY_ADJUST = BASE + "admin/admin_economy_adjust.ui";

  public static final String ADMIN_BULK_ECONOMY = BASE + "admin/admin_bulk_economy.ui";

  public static final String ADMIN_ACTIVITY_LOG = BASE + "admin/admin_activity_log.ui";

  public static final String ADMIN_ACTIVITY_LOG_ENTRY = BASE + "admin/admin_activity_log_entry.ui";

  public static final String ADMIN_ZONES = BASE + "admin/admin_zones.ui";

  public static final String ADMIN_ZONE_ENTRY = BASE + "admin/admin_zone_entry.ui";

  public static final String ADMIN_ZONE_SETTINGS = BASE + "admin/admin_zone_settings.ui";

  public static final String ADMIN_ZONE_PROPERTIES = BASE + "admin/admin_zone_properties.ui";

  public static final String ADMIN_ZONE_INTEGRATION_FLAGS = BASE + "admin/admin_zone_integration_flags.ui";

  public static final String ADMIN_ZONE_MAP = BASE + "admin/admin_zone_map.ui";

  public static final String ADMIN_ZONE_MAP_TERRAIN = BASE + "admin/admin_zone_map_terrain.ui";

  public static final String CREATE_ZONE_WIZARD = BASE + "admin/create_zone_wizard.ui";

  public static final String ZONE_RENAME_MODAL = BASE + "admin/zone_rename_modal.ui";

  public static final String ZONE_CHANGE_TYPE_MODAL = BASE + "admin/zone_change_type_modal.ui";

  public static final String UNCLAIM_ALL_CONFIRM = BASE + "admin/unclaim_all_confirm.ui";

  public static final String ADMIN_DISBAND_CONFIRM = BASE + "admin/admin_disband_confirm.ui";

  // ── Test ────────────────────────────────────────────────────────────────
  public static final String BUTTON_TEST = BASE + "test/button_test.ui";

  public static final String MARKDOWN_TEST = BASE + "test/markdown_test.ui";
}
