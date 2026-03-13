package com.hyperfactions.util;

/**
 * Static constants for admin command and navigation message keys.
 *
 * <p>
 * Split from the original MessageKeys for maintainability. Contains:
 * <ul>
 *   <li>{@link Admin} — {@code /f admin} command messages</li>
 *   <li>{@link AdminCmd} — admin CLI handler messages (non-GUI admin feedback)</li>
 *   <li>{@link AdminNav} — admin navigation bar labels</li>
 * </ul>
 */
public final class AdminKeys {

  private AdminKeys() {}

  // =====================================================================
  // Admin — /f admin command messages
  // =====================================================================

  /** /f admin command messages. */
  public static final class Admin {
    public static final String RELOAD_SUCCESS = "hyperfactions.cmd.admin.reload_success";
    public static final String SYNC_SUCCESS = "hyperfactions.cmd.admin.sync_success";
    public static final String BYPASS_ON = "hyperfactions.cmd.admin.bypass_on";
    public static final String BYPASS_OFF = "hyperfactions.cmd.admin.bypass_off";
    public static final String NOT_ADMIN = "hyperfactions.cmd.admin.not_admin";

    private Admin() {}
  }

  // =====================================================================
  // AdminCmd — admin CLI handler messages (non-GUI admin feedback)
  // =====================================================================

  /** Admin CLI handler messages (feedback from admin commands in chat). */
  public static final class AdminCmd {
    // Common admin errors
    public static final String NO_PERMISSION = "hyperfactions.admincmd.no_permission";
    public static final String PLAYER_ONLY = "hyperfactions.admincmd.player_only";
    public static final String PLAYER_CONTEXT = "hyperfactions.admincmd.player_context";
    public static final String ENTITY_NOT_FOUND = "hyperfactions.admincmd.entity_not_found";
    public static final String UNKNOWN_COMMAND = "hyperfactions.admincmd.unknown_command";
    public static final String FACTION_NOT_FOUND = "hyperfactions.admincmd.faction_not_found";
    public static final String PLAYER_NOT_FOUND = "hyperfactions.admincmd.player_not_found";
    public static final String INVALID_NUMBER = "hyperfactions.admincmd.invalid_number";
    public static final String AMOUNT_POSITIVE = "hyperfactions.admincmd.amount_positive";
    public static final String BALANCE_NOT_NEGATIVE = "hyperfactions.admincmd.balance_not_negative";
    public static final String ERROR_GENERIC = "hyperfactions.admincmd.error_generic";

    // Reload / Sync
    public static final String CONFIG_RELOADED = "hyperfactions.admincmd.reload.success";
    public static final String SYNC_START = "hyperfactions.admincmd.sync.start";
    public static final String SYNC_COMPLETE = "hyperfactions.admincmd.sync.complete";
    public static final String SYNC_FAILED = "hyperfactions.admincmd.sync.failed";

    // Version
    public static final String VERSION_TITLE = "hyperfactions.admincmd.version.title";
    public static final String VERSION_SERVER = "hyperfactions.admincmd.version.server";
    public static final String VERSION_JAVA = "hyperfactions.admincmd.version.java";
    public static final String VERSION_TREASURY = "hyperfactions.admincmd.version.treasury";
    public static final String VERSION_ACTIVE = "hyperfactions.admincmd.version.active";
    public static final String VERSION_NOT_FOUND = "hyperfactions.admincmd.version.not_found";

    // Sentry
    public static final String SENTRY_HEADER = "hyperfactions.admincmd.sentry.header";
    public static final String SENTRY_CONFIG = "hyperfactions.admincmd.sentry.config";
    public static final String SENTRY_STATUS = "hyperfactions.admincmd.sentry.status";
    public static final String SENTRY_ALREADY_DISABLED = "hyperfactions.admincmd.sentry.already_disabled";
    public static final String SENTRY_ALREADY_ENABLED = "hyperfactions.admincmd.sentry.already_enabled";
    public static final String SENTRY_DISABLED = "hyperfactions.admincmd.sentry.disabled";
    public static final String SENTRY_ENABLED = "hyperfactions.admincmd.sentry.enabled";
    public static final String SENTRY_USAGE = "hyperfactions.admincmd.sentry.usage";
    public static final String SENTRY_NOT_INITIALIZED = "hyperfactions.admincmd.sentry.not_initialized";
    public static final String SENTRY_TEST_SENT = "hyperfactions.admincmd.sentry.test_sent";
    public static final String SENTRY_TEST_FAILED = "hyperfactions.admincmd.sentry.test_failed";

    // Backup
    public static final String BACKUP_NO_PERMISSION = "hyperfactions.admincmd.backup.no_permission";
    public static final String BACKUP_CREATING = "hyperfactions.admincmd.backup.creating";
    public static final String BACKUP_CREATED = "hyperfactions.admincmd.backup.created";
    public static final String BACKUP_NAME = "hyperfactions.admincmd.backup.name";
    public static final String BACKUP_SIZE = "hyperfactions.admincmd.backup.size";
    public static final String BACKUP_FAILED = "hyperfactions.admincmd.backup.failed";
    public static final String BACKUP_NONE = "hyperfactions.admincmd.backup.none";
    public static final String BACKUP_HEADER = "hyperfactions.admincmd.backup.header";
    public static final String BACKUP_NOT_FOUND = "hyperfactions.admincmd.backup.not_found";
    public static final String BACKUP_UNKNOWN_CMD = "hyperfactions.admincmd.backup.unknown_command";
    public static final String BACKUP_USAGE_RESTORE = "hyperfactions.admincmd.backup.usage_restore";
    public static final String BACKUP_USAGE_DELETE = "hyperfactions.admincmd.backup.usage_delete";
    public static final String BACKUP_RESTORE_WARNING = "hyperfactions.admincmd.backup.restore_warning";
    public static final String BACKUP_RESTORE_CONFIRM = "hyperfactions.admincmd.backup.restore_confirm";
    public static final String BACKUP_RESTORING = "hyperfactions.admincmd.backup.restoring";
    public static final String BACKUP_RESTORED = "hyperfactions.admincmd.backup.restored";
    public static final String BACKUP_RESTORE_FAILED = "hyperfactions.admincmd.backup.restore_failed";
    public static final String BACKUP_CONFIRM_CANCEL = "hyperfactions.admincmd.backup.confirm_cancelled";
    public static final String BACKUP_DELETED = "hyperfactions.admincmd.backup.deleted";
    public static final String BACKUP_DELETE_FAILED = "hyperfactions.admincmd.backup.delete_failed";

    // Debug
    public static final String DEBUG_NO_PERMISSION = "hyperfactions.admincmd.debug.no_permission";
    public static final String DEBUG_UNKNOWN_CMD = "hyperfactions.admincmd.debug.unknown_command";
    public static final String DEBUG_PLAYER_ONLY = "hyperfactions.admincmd.debug.player_only";
    public static final String DEBUG_TOGGLE_SET = "hyperfactions.admincmd.debug.toggle_set";
    public static final String DEBUG_ALL_ENABLED = "hyperfactions.admincmd.debug.all_enabled";
    public static final String DEBUG_ALL_DISABLED = "hyperfactions.admincmd.debug.all_disabled";
    public static final String DEBUG_UNKNOWN_CATEGORY = "hyperfactions.admincmd.debug.unknown_category";
    public static final String DEBUG_NOT_IMPLEMENTED = "hyperfactions.admincmd.debug.not_implemented";

    // Economy
    public static final String ECON_UNKNOWN_CMD = "hyperfactions.admincmd.econ.unknown_command";
    public static final String ECON_SET = "hyperfactions.admincmd.econ.set";
    public static final String ECON_ADDED = "hyperfactions.admincmd.econ.added";
    public static final String ECON_DEDUCTED = "hyperfactions.admincmd.econ.deducted";
    public static final String ECON_RESET = "hyperfactions.admincmd.econ.reset";
    public static final String ECON_FAILED = "hyperfactions.admincmd.econ.failed";
    public static final String ECON_TOTAL_HEADER = "hyperfactions.admincmd.econ.total_header";
    public static final String ECON_UPKEEP_DISABLED = "hyperfactions.admincmd.econ.upkeep_disabled";
    public static final String ECON_UPKEEP_TRIGGER = "hyperfactions.admincmd.econ.upkeep_trigger";
    public static final String ECON_UPKEEP_COMPLETE = "hyperfactions.admincmd.econ.upkeep_complete";
    public static final String ECON_UPKEEP_FAILED = "hyperfactions.admincmd.econ.upkeep_failed";

    // Power
    public static final String POWER_NO_PERMISSION = "hyperfactions.admincmd.power.no_permission";
    public static final String POWER_UNKNOWN_CMD = "hyperfactions.admincmd.power.unknown_command";
    public static final String POWER_MAX_POSITIVE = "hyperfactions.admincmd.power.max_positive";
    public static final String POWER_FACTION_UNKNOWN_ACTION = "hyperfactions.admincmd.power.faction_unknown_action";

    // Clear history
    public static final String HISTORY_NO_DATA = "hyperfactions.admincmd.history.no_data";
    public static final String HISTORY_EMPTY = "hyperfactions.admincmd.history.empty";
    public static final String HISTORY_CLEARED = "hyperfactions.admincmd.history.cleared";
    public static final String HISTORY_CLEARED_REINIT = "hyperfactions.admincmd.history.cleared_reinit";

    // Zone
    public static final String ZONE_CREATED = "hyperfactions.admincmd.zone.created";
    public static final String ZONE_CHUNK_CLAIMED = "hyperfactions.admincmd.zone.chunk_claimed";
    public static final String ZONE_ALREADY_EXISTS = "hyperfactions.admincmd.zone.already_exists";
    public static final String ZONE_NAME_TAKEN = "hyperfactions.admincmd.zone.name_taken";
    public static final String ZONE_NOT_FOUND = "hyperfactions.admincmd.zone.not_found";
    public static final String ZONE_UNCLAIMED = "hyperfactions.admincmd.zone.unclaimed";
    public static final String ZONE_NO_CHUNK = "hyperfactions.admincmd.zone.no_chunk";
    public static final String ZONE_NONE = "hyperfactions.admincmd.zone.none";
    public static final String ZONE_DELETED = "hyperfactions.admincmd.zone.deleted";
    public static final String ZONE_RENAMED = "hyperfactions.admincmd.zone.renamed";
    public static final String ZONE_INVALID_TYPE = "hyperfactions.admincmd.zone.invalid_type";
    public static final String ZONE_INVALID_NAME = "hyperfactions.admincmd.zone.invalid_name";
    public static final String ZONE_CLAIMED_RADIUS = "hyperfactions.admincmd.zone.claimed_radius";
    public static final String ZONE_NO_CHUNKS_CLAIMED = "hyperfactions.admincmd.zone.no_chunks_claimed";
    public static final String ZONE_UNKNOWN_CMD = "hyperfactions.admincmd.zone.unknown_command";
    public static final String ZONE_CHUNK_HAS_ZONE = "hyperfactions.admincmd.zone.chunk_has_zone";
    public static final String ZONE_CHUNK_HAS_FACTION = "hyperfactions.admincmd.zone.chunk_has_faction";
    public static final String ZONE_NOTIFY_SET = "hyperfactions.admincmd.zone.notify_set";
    public static final String ZONE_TITLE_SET = "hyperfactions.admincmd.zone.title_set";
    public static final String ZONE_TITLE_CLEARED = "hyperfactions.admincmd.zone.title_cleared";
    public static final String ZONE_NO_ZONE_AT = "hyperfactions.admincmd.zone.no_zone_at";
    public static final String ZONE_FLAG_CLEARED = "hyperfactions.admincmd.zone.flag_cleared";
    public static final String ZONE_FLAG_SET = "hyperfactions.admincmd.zone.flag_set";
    public static final String ZONE_FLAG_INVALID = "hyperfactions.admincmd.zone.flag_invalid";
    public static final String ZONE_FLAGS_CLEARED = "hyperfactions.admincmd.zone.flags_cleared";
    public static final String ZONE_FAILED = "hyperfactions.admincmd.zone.failed";
    public static final String ZONE_FAILED_DELETE = "hyperfactions.admincmd.zone.failed_delete";
    public static final String ZONE_FAILED_RENAME = "hyperfactions.admincmd.zone.failed_rename";
    public static final String ZONE_FAILED_FLAGS = "hyperfactions.admincmd.zone.failed_flags";
    public static final String ZONE_FAILED_FLAG = "hyperfactions.admincmd.zone.failed_flag";
    public static final String ZONE_LIST_HEADER = "hyperfactions.admincmd.zone.list_header";
    public static final String ZONE_INFO_HEADER = "hyperfactions.admincmd.zone.info_header";
    public static final String ZONE_INFO_NOTIFY = "hyperfactions.admincmd.zone.info_notify";
    public static final String ZONE_INFO_UPPER_TITLE = "hyperfactions.admincmd.zone.info_upper_title";
    public static final String ZONE_INFO_LOWER_TITLE = "hyperfactions.admincmd.zone.info_lower_title";
    public static final String ZONE_INFO_CUSTOM_FLAGS = "hyperfactions.admincmd.zone.info_custom_flags";
    public static final String ZONE_FLAGS_HEADER = "hyperfactions.admincmd.zone.flags_header";
    public static final String ZONE_FLAGS_TYPE = "hyperfactions.admincmd.zone.flags_type";
    public static final String ZONE_PLAYER_ONLY = "hyperfactions.admincmd.zone.player_only";

    // World
    public static final String WORLD_UNKNOWN_CMD = "hyperfactions.admincmd.world.unknown_command";
    public static final String WORLD_NO_SETTINGS = "hyperfactions.admincmd.world.no_settings";
    public static final String WORLD_UNKNOWN_SETTING = "hyperfactions.admincmd.world.unknown_setting";
    public static final String WORLD_SET = "hyperfactions.admincmd.world.set";
    public static final String WORLD_RESET = "hyperfactions.admincmd.world.reset";
    public static final String WORLD_NOT_FOUND = "hyperfactions.admincmd.world.not_found";

    // Map / Decay
    public static final String MAP_NOT_AVAILABLE = "hyperfactions.admincmd.map.not_available";
    public static final String MAP_REFRESHING = "hyperfactions.admincmd.map.refreshing";
    public static final String MAP_REFRESHED = "hyperfactions.admincmd.map.refreshed";
    public static final String MAP_UNKNOWN_CMD = "hyperfactions.admincmd.map.unknown_command";
    public static final String DECAY_DISABLED = "hyperfactions.admincmd.decay.disabled";
    public static final String DECAY_RUNNING = "hyperfactions.admincmd.decay.running";
    public static final String DECAY_COMPLETE = "hyperfactions.admincmd.decay.complete";
    public static final String DECAY_UNKNOWN_CMD = "hyperfactions.admincmd.decay.unknown_command";
    public static final String DECAY_STATUS_HEADER = "hyperfactions.admincmd.decay.status_header";
    public static final String DECAY_ENABLE_HINT = "hyperfactions.admincmd.decay.enable_hint";
    public static final String DECAY_ERROR = "hyperfactions.admincmd.decay.error";
    public static final String DECAY_CHECK_HEADER = "hyperfactions.admincmd.decay.check_header";
    public static final String DECAY_CHECK_NOT_FOUND = "hyperfactions.admincmd.decay.check_not_found";
    public static final String DECAY_NO_CLAIMS = "hyperfactions.admincmd.decay.no_claims";
    public static final String DECAY_DISABLED_GLOBALLY = "hyperfactions.admincmd.decay.disabled_globally";

    // Map display
    public static final String MAP_STATUS_HEADER = "hyperfactions.admincmd.map.status_header";

    // Debug display
    public static final String DEBUG_STATUS_HEADER = "hyperfactions.admincmd.debug.status_header";
    public static final String DEBUG_FULL_STATUS_HEADER = "hyperfactions.admincmd.debug.full_status_header";

    // Update
    public static final String UPDATE_NOT_AVAILABLE = "hyperfactions.admincmd.update.not_available";
    public static final String UPDATE_CHECKING = "hyperfactions.admincmd.update.checking";
    public static final String UPDATE_UP_TO_DATE = "hyperfactions.admincmd.update.up_to_date";
    public static final String UPDATE_AVAILABLE = "hyperfactions.admincmd.update.available";
    public static final String UPDATE_UNKNOWN_TARGET = "hyperfactions.admincmd.update.unknown_target";
    public static final String UPDATE_NO_INFO = "hyperfactions.admincmd.update.no_info";
    public static final String UPDATE_CREATING_BACKUP = "hyperfactions.admincmd.update.creating_backup";
    public static final String UPDATE_BACKUP_CREATED = "hyperfactions.admincmd.update.backup_created";
    public static final String UPDATE_BACKUP_WARNING = "hyperfactions.admincmd.update.backup_warning";
    public static final String UPDATE_BACKUP_CONTINUE = "hyperfactions.admincmd.update.backup_continue";
    public static final String UPDATE_DOWNLOADING = "hyperfactions.admincmd.update.downloading";
    public static final String UPDATE_DOWNLOAD_FAILED = "hyperfactions.admincmd.update.download_failed";
    public static final String UPDATE_DOWNLOADED = "hyperfactions.admincmd.update.downloaded";
    public static final String UPDATE_FILE_LABEL = "hyperfactions.admincmd.update.file_label";
    public static final String UPDATE_CLEANUP = "hyperfactions.admincmd.update.cleanup";
    public static final String UPDATE_KEPT_BACKUP = "hyperfactions.admincmd.update.kept_backup";
    public static final String UPDATE_RESTART = "hyperfactions.admincmd.update.restart";
    public static final String UPDATE_USE_ROLLBACK = "hyperfactions.admincmd.update.use_rollback";
    public static final String UPDATE_USAGE_HF = "hyperfactions.admincmd.update.usage_hf";
    public static final String UPDATE_USAGE_MIXIN = "hyperfactions.admincmd.update.usage_mixin";
    public static final String UPDATE_USAGE_TOGGLE = "hyperfactions.admincmd.update.usage_toggle";

    // Mixin update
    public static final String UPDATE_MIXIN_CURRENT = "hyperfactions.admincmd.update.mixin_current";
    public static final String UPDATE_MIXIN_UP_TO_DATE = "hyperfactions.admincmd.update.mixin_up_to_date";
    public static final String UPDATE_MIXIN_NONE = "hyperfactions.admincmd.update.mixin_none";
    public static final String UPDATE_MIXIN_AVAILABLE = "hyperfactions.admincmd.update.mixin_available";
    public static final String UPDATE_MIXIN_DOWNLOADING = "hyperfactions.admincmd.update.mixin_downloading";
    public static final String UPDATE_MIXIN_DOWNLOADED = "hyperfactions.admincmd.update.mixin_downloaded";
    public static final String UPDATE_MIXIN_FAILED = "hyperfactions.admincmd.update.mixin_failed";
    public static final String UPDATE_MIXIN_LOCATION = "hyperfactions.admincmd.update.mixin_location";
    public static final String UPDATE_MIXIN_RESTART = "hyperfactions.admincmd.update.mixin_restart";
    public static final String UPDATE_MIXIN_AUTO_ON = "hyperfactions.admincmd.update.mixin_auto_on";
    public static final String UPDATE_MIXIN_AUTO_ON_DESC = "hyperfactions.admincmd.update.mixin_auto_on_desc";
    public static final String UPDATE_MIXIN_AUTO_OFF = "hyperfactions.admincmd.update.mixin_auto_off";
    public static final String UPDATE_MIXIN_AUTO_OFF_DESC = "hyperfactions.admincmd.update.mixin_auto_off_desc";

    // Rollback
    public static final String ROLLBACK_NO_BACKUP = "hyperfactions.admincmd.rollback.no_backup";
    public static final String ROLLBACK_UNSAFE = "hyperfactions.admincmd.rollback.unsafe";
    public static final String ROLLBACK_UNSAFE_REASON = "hyperfactions.admincmd.rollback.unsafe_reason";
    public static final String ROLLBACK_UNSAFE_MIGRATION = "hyperfactions.admincmd.rollback.unsafe_migration";
    public static final String ROLLBACK_INSTRUCTIONS = "hyperfactions.admincmd.rollback.instructions";
    public static final String ROLLBACK_FIND_BACKUP = "hyperfactions.admincmd.rollback.find_backup";
    public static final String ROLLBACK_ROLLING = "hyperfactions.admincmd.rollback.rolling";
    public static final String ROLLBACK_FROM = "hyperfactions.admincmd.rollback.from";
    public static final String ROLLBACK_TO = "hyperfactions.admincmd.rollback.to";
    public static final String ROLLBACK_VERSION = "hyperfactions.admincmd.rollback.version";
    public static final String ROLLBACK_SUCCESS = "hyperfactions.admincmd.rollback.success";
    public static final String ROLLBACK_RESTORED = "hyperfactions.admincmd.rollback.restored";
    public static final String ROLLBACK_REMOVED = "hyperfactions.admincmd.rollback.removed";
    public static final String ROLLBACK_RESTART = "hyperfactions.admincmd.rollback.restart";
    public static final String ROLLBACK_FAILED = "hyperfactions.admincmd.rollback.failed";

    // Import
    public static final String IMPORT_UNKNOWN_SOURCE = "hyperfactions.admincmd.import.unknown_source";
    public static final String IMPORT_IMPORTING = "hyperfactions.admincmd.import.importing";
    public static final String IMPORT_COMPLETE = "hyperfactions.admincmd.import.complete";
    public static final String IMPORT_FAILED = "hyperfactions.admincmd.import.failed";

    // Update notification
    public static final String UPDATE_NOTIFY_NEW_VERSION = "hyperfactions.admincmd.update_notify.new_version";
    public static final String UPDATE_NOTIFY_VERSION_INFO = "hyperfactions.admincmd.update_notify.version_info";
    public static final String UPDATE_NOTIFY_INSTRUCTION = "hyperfactions.admincmd.update_notify.instruction";
    public static final String UPDATE_NOTIFY_UP_TO_DATE = "hyperfactions.admincmd.update_notify.up_to_date";

    private AdminCmd() {}
  }

  // =====================================================================
  // AdminNav — admin navigation bar labels
  // =====================================================================

  /** Admin navigation bar labels. */
  public static final class AdminNav {
    public static final String DASHBOARD = "hyperfactions_admin.nav.dashboard";
    public static final String ACTIONS = "hyperfactions_admin.nav.actions";
    public static final String FACTIONS = "hyperfactions_admin.nav.factions";
    public static final String PLAYERS = "hyperfactions_admin.nav.players";
    public static final String ECONOMY = "hyperfactions_admin.nav.economy";
    public static final String ZONES = "hyperfactions_admin.nav.zones";
    public static final String CONFIG = "hyperfactions_admin.nav.config";
    public static final String BACKUPS = "hyperfactions_admin.nav.backups";
    public static final String LOG = "hyperfactions_admin.nav.log";
    public static final String UPDATES = "hyperfactions_admin.nav.updates";
    public static final String HELP = "hyperfactions_admin.nav.help";
    public static final String VERSION = "hyperfactions_admin.nav.version";

    private AdminNav() {}
  }
}
