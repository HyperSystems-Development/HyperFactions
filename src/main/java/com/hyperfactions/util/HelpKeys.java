package com.hyperfactions.util;

/**
 * Help system message keys, split from the original MessageKeys.
 *
 * <p>
 * Contains i18n keys for the help framework, section names,
 * command descriptions, and sub-help pages.
 * Key format: {@code hyperfactions.help.{domain}.{action}}
 */
public final class HelpKeys {

  /** Help system message keys (help text, section names, command descriptions). */
  public static final class Help {
    // Help framework
    public static final String COMMANDS_LABEL = "hyperfactions.help.commands_label";
    public static final String DEFAULT_FOOTER = "hyperfactions.help.default_footer";

    // /f help
    public static final String TITLE = "hyperfactions.help.title";
    public static final String DESCRIPTION = "hyperfactions.help.description";

    // Section names
    public static final String SECTION_CORE = "hyperfactions.help.section.core";
    public static final String SECTION_MANAGEMENT = "hyperfactions.help.section.management";
    public static final String SECTION_TERRITORY = "hyperfactions.help.section.territory";
    public static final String SECTION_RELATIONS = "hyperfactions.help.section.relations";
    public static final String SECTION_TELEPORT = "hyperfactions.help.section.teleport";
    public static final String SECTION_INFORMATION = "hyperfactions.help.section.information";
    public static final String SECTION_OTHER = "hyperfactions.help.section.other";
    public static final String SECTION_ADMIN = "hyperfactions.help.section.admin";

    // /f help — command descriptions (Core)
    public static final String CMD_CREATE = "hyperfactions.help.cmd.create";
    public static final String CMD_DISBAND = "hyperfactions.help.cmd.disband";
    public static final String CMD_INVITE = "hyperfactions.help.cmd.invite";
    public static final String CMD_ACCEPT = "hyperfactions.help.cmd.accept";
    public static final String CMD_REQUEST = "hyperfactions.help.cmd.request";
    public static final String CMD_LEAVE = "hyperfactions.help.cmd.leave";
    public static final String CMD_KICK = "hyperfactions.help.cmd.kick";

    // /f help — command descriptions (Management)
    public static final String CMD_RENAME = "hyperfactions.help.cmd.rename";
    public static final String CMD_DESC = "hyperfactions.help.cmd.desc";
    public static final String CMD_COLOR = "hyperfactions.help.cmd.color";
    public static final String CMD_OPEN = "hyperfactions.help.cmd.open";
    public static final String CMD_CLOSE = "hyperfactions.help.cmd.close";
    public static final String CMD_PROMOTE = "hyperfactions.help.cmd.promote";
    public static final String CMD_DEMOTE = "hyperfactions.help.cmd.demote";
    public static final String CMD_TRANSFER = "hyperfactions.help.cmd.transfer";

    // /f help — command descriptions (Territory)
    public static final String CMD_CLAIM = "hyperfactions.help.cmd.claim";
    public static final String CMD_UNCLAIM = "hyperfactions.help.cmd.unclaim";
    public static final String CMD_OVERCLAIM = "hyperfactions.help.cmd.overclaim";
    public static final String CMD_MAP = "hyperfactions.help.cmd.map";

    // /f help — command descriptions (Relations)
    public static final String CMD_ALLY = "hyperfactions.help.cmd.ally";
    public static final String CMD_ENEMY = "hyperfactions.help.cmd.enemy";
    public static final String CMD_NEUTRAL = "hyperfactions.help.cmd.neutral";

    // /f help — command descriptions (Teleport)
    public static final String CMD_HOME = "hyperfactions.help.cmd.home";
    public static final String CMD_SETHOME = "hyperfactions.help.cmd.sethome";
    public static final String CMD_STUCK = "hyperfactions.help.cmd.stuck";

    // /f help — command descriptions (Information)
    public static final String CMD_INFO = "hyperfactions.help.cmd.info";
    public static final String CMD_LIST = "hyperfactions.help.cmd.list";
    public static final String CMD_BROWSE = "hyperfactions.help.cmd.browse";
    public static final String CMD_MEMBERS = "hyperfactions.help.cmd.members";
    public static final String CMD_INVITES = "hyperfactions.help.cmd.invites";
    public static final String CMD_WHO = "hyperfactions.help.cmd.who";
    public static final String CMD_POWER = "hyperfactions.help.cmd.power";
    public static final String CMD_GUI = "hyperfactions.help.cmd.gui";
    public static final String CMD_SETTINGS = "hyperfactions.help.cmd.settings";

    // /f help — command descriptions (Other)
    public static final String CMD_CHAT = "hyperfactions.help.cmd.chat";
    public static final String CMD_CHAT_SHORT = "hyperfactions.help.cmd.chat_short";

    // /f help — command descriptions (Admin section in main help)
    public static final String CMD_ADMIN = "hyperfactions.help.cmd.admin";
    public static final String CMD_ADMIN_RELOAD = "hyperfactions.help.cmd.admin_reload";
    public static final String CMD_ADMIN_SYNC = "hyperfactions.help.cmd.admin_sync";
    public static final String CMD_ADMIN_FACTIONS = "hyperfactions.help.cmd.admin_factions";
    public static final String CMD_ADMIN_ZONES = "hyperfactions.help.cmd.admin_zones";
    public static final String CMD_ADMIN_CONFIG = "hyperfactions.help.cmd.admin_config";
    public static final String CMD_ADMIN_BACKUPS = "hyperfactions.help.cmd.admin_backups";
    public static final String CMD_ADMIN_UPDATE = "hyperfactions.help.cmd.admin_update";
    public static final String CMD_ADMIN_DEBUG = "hyperfactions.help.cmd.admin_debug";

    // /f admin help — title and description
    public static final String ADMIN_TITLE = "hyperfactions.help.admin.title";
    public static final String ADMIN_DESCRIPTION = "hyperfactions.help.admin.description";

    // /f admin help — command descriptions
    public static final String ADMIN_CMD_DASHBOARD = "hyperfactions.help.admin.cmd.dashboard";
    public static final String ADMIN_CMD_FACTIONS = "hyperfactions.help.admin.cmd.factions";
    public static final String ADMIN_CMD_ZONE = "hyperfactions.help.admin.cmd.zone";
    public static final String ADMIN_CMD_CONFIG = "hyperfactions.help.admin.cmd.config";
    public static final String ADMIN_CMD_BACKUP = "hyperfactions.help.admin.cmd.backup";
    public static final String ADMIN_CMD_IMPORT = "hyperfactions.help.admin.cmd.import_cmd";
    public static final String ADMIN_CMD_UPDATE = "hyperfactions.help.admin.cmd.update";
    public static final String ADMIN_CMD_UPDATE_MIXIN = "hyperfactions.help.admin.cmd.update_mixin";
    public static final String ADMIN_CMD_UPDATE_TOGGLE = "hyperfactions.help.admin.cmd.update_toggle";
    public static final String ADMIN_CMD_ROLLBACK = "hyperfactions.help.admin.cmd.rollback";
    public static final String ADMIN_CMD_RELOAD = "hyperfactions.help.admin.cmd.reload";
    public static final String ADMIN_CMD_SYNC = "hyperfactions.help.admin.cmd.sync";
    public static final String ADMIN_CMD_DEBUG = "hyperfactions.help.admin.cmd.debug";
    public static final String ADMIN_CMD_DECAY = "hyperfactions.help.admin.cmd.decay";
    public static final String ADMIN_CMD_MAP = "hyperfactions.help.admin.cmd.map";
    public static final String ADMIN_CMD_SAFEZONE = "hyperfactions.help.admin.cmd.safezone";
    public static final String ADMIN_CMD_WARZONE = "hyperfactions.help.admin.cmd.warzone";
    public static final String ADMIN_CMD_REMOVEZONE = "hyperfactions.help.admin.cmd.removezone";
    public static final String ADMIN_CMD_ZONEFLAG = "hyperfactions.help.admin.cmd.zoneflag";
    public static final String ADMIN_CMD_INTEGRATIONS = "hyperfactions.help.admin.cmd.integrations";
    public static final String ADMIN_CMD_INTEGRATION = "hyperfactions.help.admin.cmd.integration";
    public static final String ADMIN_CMD_CLEARHISTORY = "hyperfactions.help.admin.cmd.clearhistory";
    public static final String ADMIN_CMD_POWER = "hyperfactions.help.admin.cmd.power";
    public static final String ADMIN_CMD_ECONOMY = "hyperfactions.help.admin.cmd.economy";
    public static final String ADMIN_CMD_ECONOMY_UPKEEP = "hyperfactions.help.admin.cmd.economy_upkeep";
    public static final String ADMIN_CMD_INFO = "hyperfactions.help.admin.cmd.info";
    public static final String ADMIN_CMD_WHO = "hyperfactions.help.admin.cmd.who";
    public static final String ADMIN_CMD_LOG = "hyperfactions.help.admin.cmd.log";
    public static final String ADMIN_CMD_WORLD = "hyperfactions.help.admin.cmd.world";
    public static final String ADMIN_CMD_VERSION = "hyperfactions.help.admin.cmd.version";
    public static final String ADMIN_CMD_SENTRY = "hyperfactions.help.admin.cmd.sentry";
    public static final String ADMIN_CMD_SENTRY_DISABLE = "hyperfactions.help.admin.cmd.sentry_disable";
    public static final String ADMIN_CMD_SENTRY_ENABLE = "hyperfactions.help.admin.cmd.sentry_enable";
    public static final String ADMIN_CMD_TEST_GUI = "hyperfactions.help.admin.cmd.test_gui";
    public static final String ADMIN_CMD_TEST_SENTRY = "hyperfactions.help.admin.cmd.test_sentry";
    public static final String ADMIN_CMD_TEST_MD = "hyperfactions.help.admin.cmd.test_md";

    // Sub-help page titles and descriptions
    public static final String BACKUP_TITLE = "hyperfactions.help.backup.title";
    public static final String BACKUP_DESCRIPTION = "hyperfactions.help.backup.description";
    public static final String BACKUP_CMD_CREATE = "hyperfactions.help.backup.cmd.create";
    public static final String BACKUP_CMD_LIST = "hyperfactions.help.backup.cmd.list";
    public static final String BACKUP_CMD_RESTORE = "hyperfactions.help.backup.cmd.restore";
    public static final String BACKUP_CMD_DELETE = "hyperfactions.help.backup.cmd.delete";

    public static final String DEBUG_TITLE = "hyperfactions.help.debug.title";
    public static final String DEBUG_DESCRIPTION = "hyperfactions.help.debug.description";
    public static final String DEBUG_CMD_TOGGLE = "hyperfactions.help.debug.cmd.toggle";
    public static final String DEBUG_CMD_STATUS = "hyperfactions.help.debug.cmd.status";
    public static final String DEBUG_CMD_POWER = "hyperfactions.help.debug.cmd.power";
    public static final String DEBUG_CMD_CLAIM = "hyperfactions.help.debug.cmd.claim";
    public static final String DEBUG_CMD_PROTECTION = "hyperfactions.help.debug.cmd.protection";
    public static final String DEBUG_CMD_COMBAT = "hyperfactions.help.debug.cmd.combat";
    public static final String DEBUG_CMD_RELATION = "hyperfactions.help.debug.cmd.relation";

    public static final String POWER_TITLE = "hyperfactions.help.power.title";
    public static final String POWER_DESCRIPTION = "hyperfactions.help.power.description";
    public static final String POWER_CMD_SET = "hyperfactions.help.power.cmd.set";
    public static final String POWER_CMD_ADD = "hyperfactions.help.power.cmd.add";
    public static final String POWER_CMD_REMOVE = "hyperfactions.help.power.cmd.remove";
    public static final String POWER_CMD_RESET = "hyperfactions.help.power.cmd.reset";
    public static final String POWER_CMD_SETMAX = "hyperfactions.help.power.cmd.setmax";
    public static final String POWER_CMD_RESETMAX = "hyperfactions.help.power.cmd.resetmax";
    public static final String POWER_CMD_NOLOSS = "hyperfactions.help.power.cmd.noloss";
    public static final String POWER_CMD_NODECAY = "hyperfactions.help.power.cmd.nodecay";
    public static final String POWER_CMD_FACTION = "hyperfactions.help.power.cmd.faction";
    public static final String POWER_CMD_INFO = "hyperfactions.help.power.cmd.info";

    public static final String ECONOMY_TITLE = "hyperfactions.help.economy.title";
    public static final String ECONOMY_DESCRIPTION = "hyperfactions.help.economy.description";
    public static final String ECONOMY_CMD_BALANCE = "hyperfactions.help.economy.cmd.balance";
    public static final String ECONOMY_CMD_SET = "hyperfactions.help.economy.cmd.set";
    public static final String ECONOMY_CMD_ADD = "hyperfactions.help.economy.cmd.add";
    public static final String ECONOMY_CMD_TAKE = "hyperfactions.help.economy.cmd.take";
    public static final String ECONOMY_CMD_TOTAL = "hyperfactions.help.economy.cmd.total";
    public static final String ECONOMY_CMD_RESET = "hyperfactions.help.economy.cmd.reset";
    public static final String ECONOMY_CMD_UPKEEP = "hyperfactions.help.economy.cmd.upkeep";

    public static final String WORLD_TITLE = "hyperfactions.help.world.title";
    public static final String WORLD_DESCRIPTION = "hyperfactions.help.world.description";
    public static final String WORLD_CMD_LIST = "hyperfactions.help.world.cmd.list";
    public static final String WORLD_CMD_INFO = "hyperfactions.help.world.cmd.info";
    public static final String WORLD_CMD_SET = "hyperfactions.help.world.cmd.set";
    public static final String WORLD_CMD_RESET = "hyperfactions.help.world.cmd.reset";

    public static final String MAP_TITLE = "hyperfactions.help.map.title";
    public static final String MAP_DESCRIPTION = "hyperfactions.help.map.description";
    public static final String MAP_CMD_STATUS = "hyperfactions.help.map.cmd.status";
    public static final String MAP_CMD_REFRESH = "hyperfactions.help.map.cmd.refresh";

    public static final String DECAY_TITLE = "hyperfactions.help.decay.title";
    public static final String DECAY_DESCRIPTION = "hyperfactions.help.decay.description";
    public static final String DECAY_CMD_STATUS = "hyperfactions.help.decay.cmd.status";
    public static final String DECAY_CMD_RUN = "hyperfactions.help.decay.cmd.run";
    public static final String DECAY_CMD_CHECK = "hyperfactions.help.decay.cmd.check";

    public static final String IMPORT_TITLE = "hyperfactions.help.import.title";
    public static final String IMPORT_DESCRIPTION = "hyperfactions.help.import.description";
    public static final String IMPORT_CMD_HYFACTIONS = "hyperfactions.help.import.cmd.hyfactions";
    public static final String IMPORT_CMD_ELBAPHFACTIONS = "hyperfactions.help.import.cmd.elbaphfactions";
    public static final String IMPORT_CMD_FACTIONSX = "hyperfactions.help.import.cmd.factionsx";
    public static final String IMPORT_CMD_SIMPLECLAIMS = "hyperfactions.help.import.cmd.simpleclaims";
    public static final String IMPORT_FLAGS_HEADER = "hyperfactions.help.import.flags_header";
    public static final String IMPORT_FLAG_DRYRUN = "hyperfactions.help.import.flag.dryrun";
    public static final String IMPORT_FLAG_OVERWRITE = "hyperfactions.help.import.flag.overwrite";
    public static final String IMPORT_FLAG_NOZONES = "hyperfactions.help.import.flag.nozones";
    public static final String IMPORT_FLAG_NOPOWER = "hyperfactions.help.import.flag.nopower";
    public static final String IMPORT_PATH_HYFACTIONS = "hyperfactions.help.import.path.hyfactions";
    public static final String IMPORT_PATH_ELBAPHFACTIONS = "hyperfactions.help.import.path.elbaphfactions";
    public static final String IMPORT_PATH_FACTIONSX = "hyperfactions.help.import.path.factionsx";
    public static final String IMPORT_PATH_SIMPLECLAIMS = "hyperfactions.help.import.path.simpleclaims";

    public static final String TEST_TITLE = "hyperfactions.help.test.title";
    public static final String TEST_DESCRIPTION = "hyperfactions.help.test.description";
    public static final String TEST_CMD_GUI = "hyperfactions.help.test.cmd.gui";
    public static final String TEST_CMD_SENTRY = "hyperfactions.help.test.cmd.sentry";
    public static final String TEST_CMD_MD = "hyperfactions.help.test.cmd.md";

    private Help() {}
  }

  private HelpKeys() {}
}
