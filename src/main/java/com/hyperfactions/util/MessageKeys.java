package com.hyperfactions.util;

/**
 * Static constants for all HyperFactions i18n message keys.
 *
 * <p>
 * Organized by nested inner classes — one per feature domain.
 * Key format: {@code {file_prefix}.{domain}.{action}}
 *
 * <p>
 * File prefixes map to .lang file names:
 * <ul>
 *   <li>{@code hyperfactions.*} → {@code hyperfactions.lang} (commands, errors, common)</li>
 *   <li>{@code hyperfactions_gui.*} → {@code hyperfactions_gui.lang} (GUI labels, buttons)</li>
 *   <li>{@code hyperfactions_help.*} → {@code hyperfactions_help.lang} (help content, build-generated)</li>
 *   <li>{@code hyperfactions_admin.*} → {@code hyperfactions_admin.lang} (admin GUI)</li>
 * </ul>
 */
public final class MessageKeys {

  private MessageKeys() {}

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

    private Common() {}
  }

  // =====================================================================
  // Commands — organized by command group
  // =====================================================================

  /** /f create command messages. */
  public static final class Create {
    public static final String NO_PERMISSION = "hyperfactions.cmd.create.no_permission";
    public static final String USAGE = "hyperfactions.cmd.create.usage";
    public static final String SUCCESS = "hyperfactions.cmd.create.success";
    public static final String ALREADY_IN_NAMED = "hyperfactions.cmd.create.already_in_named";
    public static final String USE_LEAVE_FIRST = "hyperfactions.cmd.create.use_leave_first";
    public static final String NAME_TAKEN = "hyperfactions.cmd.create.name_taken";
    public static final String NAME_TOO_SHORT = "hyperfactions.cmd.create.name_too_short";
    public static final String NAME_TOO_LONG = "hyperfactions.cmd.create.name_too_long";
    public static final String FAILED = "hyperfactions.cmd.create.failed";

    private Create() {}
  }

  /** /f disband command messages. */
  public static final class Disband {
    public static final String NO_PERMISSION = "hyperfactions.cmd.disband.no_permission";
    public static final String NOT_LEADER = "hyperfactions.cmd.disband.not_leader";
    public static final String CONFIRM_PROMPT = "hyperfactions.cmd.disband.confirm_prompt";
    public static final String CONFIRM_INSTRUCTION = "hyperfactions.cmd.disband.confirm_instruction";
    public static final String SUCCESS = "hyperfactions.cmd.disband.success";
    public static final String FAILED = "hyperfactions.cmd.disband.failed";
    public static final String CANCELLED = "hyperfactions.cmd.disband.cancelled";

    private Disband() {}
  }

  /** /f rename command messages. */
  public static final class Rename {
    public static final String NO_PERMISSION = "hyperfactions.cmd.rename.no_permission";
    public static final String NOT_LEADER = "hyperfactions.cmd.rename.not_leader";
    public static final String USAGE = "hyperfactions.cmd.rename.usage";
    public static final String TOO_SHORT = "hyperfactions.cmd.rename.too_short";
    public static final String TOO_LONG = "hyperfactions.cmd.rename.too_long";
    public static final String NAME_TAKEN = "hyperfactions.cmd.rename.name_taken";
    public static final String SUCCESS = "hyperfactions.cmd.rename.success";
    public static final String BROADCAST = "hyperfactions.cmd.rename.broadcast";

    private Rename() {}
  }

  /** /f desc command messages. */
  public static final class Desc {
    public static final String NO_PERMISSION = "hyperfactions.cmd.desc.no_permission";
    public static final String NOT_OFFICER = "hyperfactions.cmd.desc.not_officer";
    public static final String SET = "hyperfactions.cmd.desc.set";
    public static final String CLEARED = "hyperfactions.cmd.desc.cleared";

    private Desc() {}
  }

  /** /f open command messages. */
  public static final class Open {
    public static final String NO_PERMISSION = "hyperfactions.cmd.open.no_permission";
    public static final String NOT_LEADER = "hyperfactions.cmd.open.not_leader";
    public static final String ALREADY_OPEN = "hyperfactions.cmd.open.already_open";
    public static final String SUCCESS = "hyperfactions.cmd.open.success";
    public static final String BROADCAST = "hyperfactions.cmd.open.broadcast";

    private Open() {}
  }

  /** /f close command messages. */
  public static final class Close {
    public static final String NO_PERMISSION = "hyperfactions.cmd.close.no_permission";
    public static final String NOT_LEADER = "hyperfactions.cmd.close.not_leader";
    public static final String ALREADY_CLOSED = "hyperfactions.cmd.close.already_closed";
    public static final String SUCCESS = "hyperfactions.cmd.close.success";
    public static final String BROADCAST = "hyperfactions.cmd.close.broadcast";

    private Close() {}
  }

  /** /f color command messages. */
  public static final class Color {
    public static final String NO_PERMISSION = "hyperfactions.cmd.color.no_permission";
    public static final String NOT_OFFICER = "hyperfactions.cmd.color.not_officer";
    public static final String COLORS_DISABLED = "hyperfactions.cmd.color.colors_disabled";
    public static final String USAGE = "hyperfactions.cmd.color.usage";
    public static final String USAGE_HINT = "hyperfactions.cmd.color.usage_hint";
    public static final String INVALID = "hyperfactions.cmd.color.invalid";
    public static final String SUCCESS = "hyperfactions.cmd.color.success";

    private Color() {}
  }

  /** /f invite command messages. */
  public static final class Invite {
    public static final String NO_PERMISSION = "hyperfactions.cmd.invite.no_permission";
    public static final String NOT_OFFICER = "hyperfactions.cmd.invite.not_officer";
    public static final String USAGE = "hyperfactions.cmd.invite.usage";
    public static final String PLAYER_NOT_FOUND = "hyperfactions.cmd.invite.player_not_found";
    public static final String TARGET_IN_FACTION = "hyperfactions.cmd.invite.target_in_faction";
    public static final String SENT = "hyperfactions.cmd.invite.sent";
    public static final String RECEIVED = "hyperfactions.cmd.invite.received";
    public static final String ACCEPT_HINT = "hyperfactions.cmd.invite.accept_hint";

    private Invite() {}
  }

  /** /f join, /f accept, /f request command messages. */
  public static final class Join {
    public static final String NO_PERMISSION = "hyperfactions.cmd.join.no_permission";
    public static final String ALREADY_IN_NAMED = "hyperfactions.cmd.join.already_in_named";
    public static final String USE_LEAVE_HINT = "hyperfactions.cmd.join.use_leave_hint";
    public static final String NO_INVITES = "hyperfactions.cmd.join.no_invites";
    public static final String FACTION_NOT_FOUND = "hyperfactions.cmd.join.faction_not_found";
    public static final String NOT_INVITED = "hyperfactions.cmd.join.not_invited";
    public static final String FACTION_GONE = "hyperfactions.cmd.join.faction_gone";
    public static final String SUCCESS = "hyperfactions.cmd.join.success";
    public static final String BROADCAST = "hyperfactions.cmd.join.broadcast";
    public static final String FACTION_FULL = "hyperfactions.cmd.join.faction_full";
    public static final String FAILED = "hyperfactions.cmd.join.failed";

    private Join() {}
  }

  /** /f leave command messages. */
  public static final class Leave {
    public static final String NO_PERMISSION = "hyperfactions.cmd.leave.no_permission";
    public static final String CONFIRM_PROMPT = "hyperfactions.cmd.leave.confirm_prompt";
    public static final String CONFIRM_INSTRUCTION = "hyperfactions.cmd.leave.confirm_instruction";
    public static final String SUCCESS = "hyperfactions.cmd.leave.success";
    public static final String BROADCAST = "hyperfactions.cmd.leave.broadcast";
    public static final String FAILED = "hyperfactions.cmd.leave.failed";
    public static final String CANCELLED = "hyperfactions.cmd.leave.cancelled";

    private Leave() {}
  }

  /** /f kick command messages. */
  public static final class Kick {
    public static final String NO_PERMISSION = "hyperfactions.cmd.kick.no_permission";
    public static final String USAGE = "hyperfactions.cmd.kick.usage";
    public static final String NOT_IN_YOUR_FACTION = "hyperfactions.cmd.kick.not_in_your_faction";
    public static final String SUCCESS = "hyperfactions.cmd.kick.success";
    public static final String BROADCAST = "hyperfactions.cmd.kick.broadcast";
    public static final String KICKED = "hyperfactions.cmd.kick.kicked";
    public static final String CANNOT_KICK_HIGHER = "hyperfactions.cmd.kick.cannot_kick_higher";
    public static final String CANNOT_KICK_LEADER = "hyperfactions.cmd.kick.cannot_kick_leader";
    public static final String FAILED = "hyperfactions.cmd.kick.failed";

    private Kick() {}
  }

  /** /f promote, /f demote, /f transfer command messages. */
  public static final class Rank {
    // Promote
    public static final String PROMOTE_NO_PERMISSION = "hyperfactions.cmd.rank.promote_no_permission";
    public static final String PROMOTE_USAGE = "hyperfactions.cmd.rank.promote_usage";
    public static final String PROMOTED = "hyperfactions.cmd.rank.promoted";
    public static final String PROMOTE_BROADCAST = "hyperfactions.cmd.rank.promote_broadcast";
    public static final String ALREADY_HIGHEST = "hyperfactions.cmd.rank.already_highest";
    public static final String PROMOTE_FAILED = "hyperfactions.cmd.rank.promote_failed";
    // Demote
    public static final String DEMOTE_NO_PERMISSION = "hyperfactions.cmd.rank.demote_no_permission";
    public static final String DEMOTE_USAGE = "hyperfactions.cmd.rank.demote_usage";
    public static final String DEMOTED = "hyperfactions.cmd.rank.demoted";
    public static final String DEMOTE_BROADCAST = "hyperfactions.cmd.rank.demote_broadcast";
    public static final String ALREADY_LOWEST = "hyperfactions.cmd.rank.already_lowest";
    public static final String DEMOTE_FAILED = "hyperfactions.cmd.rank.demote_failed";
    // Transfer
    public static final String TRANSFER_NO_PERMISSION = "hyperfactions.cmd.rank.transfer_no_permission";
    public static final String TRANSFER_USAGE = "hyperfactions.cmd.rank.transfer_usage";
    public static final String PLAYER_NOT_IN_FACTION = "hyperfactions.cmd.rank.player_not_in_faction";
    public static final String TRANSFER_CONFIRM = "hyperfactions.cmd.rank.transfer_confirm";
    public static final String TRANSFER_CONFIRM_INSTRUCTION = "hyperfactions.cmd.rank.transfer_confirm_instruction";
    public static final String TRANSFERRED = "hyperfactions.cmd.rank.transferred";
    public static final String TRANSFER_BROADCAST = "hyperfactions.cmd.rank.transfer_broadcast";
    public static final String TRANSFER_FAILED = "hyperfactions.cmd.rank.transfer_failed";
    public static final String TRANSFER_CANCELLED = "hyperfactions.cmd.rank.transfer_cancelled";

    private Rank() {}
  }

  /** /f claim, /f unclaim, /f overclaim command messages. */
  public static final class Claim {
    // Claim
    public static final String NO_PERMISSION = "hyperfactions.cmd.claim.no_permission";
    public static final String SUCCESS = "hyperfactions.cmd.claim.success";
    public static final String ALREADY_CLAIMED = "hyperfactions.cmd.claim.already_claimed";
    public static final String ALREADY_YOURS = "hyperfactions.cmd.claim.already_yours";
    public static final String CANNOT_CLAIM_ALLY = "hyperfactions.cmd.claim.cannot_claim_ally";
    public static final String ALREADY_CLAIMED_HINT = "hyperfactions.cmd.claim.already_claimed_hint";
    public static final String NOT_OFFICER = "hyperfactions.cmd.claim.not_officer";
    public static final String NOT_CONNECTED = "hyperfactions.cmd.claim.not_adjacent";
    public static final String MAX_CLAIMS = "hyperfactions.cmd.claim.max_claims";
    public static final String WORLD_NOT_ALLOWED = "hyperfactions.cmd.claim.world_not_allowed";
    public static final String ORBISGUARD = "hyperfactions.cmd.claim.orbisguard";
    public static final String ZONE_PROTECTED = "hyperfactions.cmd.claim.zone_protected";
    public static final String FAILED = "hyperfactions.cmd.claim.failed";
    // Unclaim
    public static final String UNCLAIM_NO_PERMISSION = "hyperfactions.cmd.unclaim.no_permission";
    public static final String UNCLAIMED = "hyperfactions.cmd.unclaim.success";
    public static final String UNCLAIM_NOT_OFFICER = "hyperfactions.cmd.unclaim.not_officer";
    public static final String CHUNK_NOT_CLAIMED = "hyperfactions.cmd.unclaim.chunk_not_claimed";
    public static final String NOT_YOUR_CLAIM = "hyperfactions.cmd.unclaim.not_your_claim";
    public static final String CANNOT_UNCLAIM_HOME = "hyperfactions.cmd.unclaim.cannot_unclaim_home";
    public static final String WOULD_DISCONNECT = "hyperfactions.cmd.unclaim.would_disconnect";
    public static final String UNCLAIM_FAILED = "hyperfactions.cmd.unclaim.failed";
    // Overclaim
    public static final String OVERCLAIM_NO_PERMISSION = "hyperfactions.cmd.overclaim.no_permission";
    public static final String OVERCLAIMED = "hyperfactions.cmd.overclaim.success";
    public static final String OVERCLAIM_NOT_OFFICER = "hyperfactions.cmd.overclaim.not_officer";
    public static final String OVERCLAIM_NOT_CLAIMED = "hyperfactions.cmd.overclaim.not_claimed";
    public static final String OVERCLAIM_OWN = "hyperfactions.cmd.overclaim.own_chunk";
    public static final String OVERCLAIM_ALLY = "hyperfactions.cmd.overclaim.ally";
    public static final String TARGET_HAS_POWER = "hyperfactions.cmd.overclaim.target_has_power";
    public static final String OVERCLAIM_FAILED = "hyperfactions.cmd.overclaim.failed";

    private Claim() {}
  }

  /** /f home, /f sethome, /f delhome, /f stuck command messages. */
  public static final class Home {
    // Home
    public static final String NO_PERMISSION = "hyperfactions.cmd.home.no_permission";
    public static final String NO_HOME = "hyperfactions.cmd.home.no_home";
    public static final String COMBAT_TAGGED = "hyperfactions.cmd.home.combat_tagged";
    public static final String TELEPORTED = "hyperfactions.cmd.home.teleported";
    public static final String WARMUP = "hyperfactions.cmd.home.warmup";
    public static final String WARMUP_CANCELLED = "hyperfactions.cmd.home.warmup_cancelled";
    public static final String COOLDOWN = "hyperfactions.cmd.home.cooldown";
    // SetHome
    public static final String SETHOME_NO_PERMISSION = "hyperfactions.cmd.sethome.no_permission";
    public static final String SETHOME_WORLD_NOT_ALLOWED = "hyperfactions.cmd.sethome.world_not_allowed";
    public static final String NOT_IN_TERRITORY = "hyperfactions.cmd.sethome.not_in_territory";
    public static final String SET = "hyperfactions.cmd.sethome.set";
    public static final String SETHOME_BROADCAST = "hyperfactions.cmd.sethome.broadcast";
    public static final String SETHOME_NOT_OFFICER = "hyperfactions.cmd.sethome.not_officer";
    public static final String SETHOME_FAILED = "hyperfactions.cmd.sethome.failed";
    // DelHome
    public static final String DELHOME_NO_PERMISSION = "hyperfactions.cmd.delhome.no_permission";
    public static final String DELHOME_NO_HOME = "hyperfactions.cmd.delhome.no_home";
    public static final String DELETED = "hyperfactions.cmd.delhome.deleted";
    public static final String DELHOME_BROADCAST = "hyperfactions.cmd.delhome.broadcast";
    public static final String DELHOME_NOT_OFFICER = "hyperfactions.cmd.delhome.not_officer";
    public static final String DELHOME_FAILED = "hyperfactions.cmd.delhome.failed";
    // Stuck
    public static final String STUCK_NO_PERMISSION = "hyperfactions.cmd.stuck.no_permission";
    public static final String STUCK_NOT_STUCK = "hyperfactions.cmd.stuck.not_stuck";
    public static final String STUCK_COMBAT_TAGGED = "hyperfactions.cmd.stuck.combat_tagged";
    public static final String STUCK_NO_SAFE = "hyperfactions.cmd.stuck.no_safe";
    public static final String STUCK_TELEPORTING = "hyperfactions.cmd.stuck.teleporting";

    private Home() {}
  }

  /** /f power command messages. */
  public static final class Power {
    public static final String PERSONAL = "hyperfactions.cmd.power.personal";
    public static final String FACTION = "hyperfactions.cmd.power.faction";
    public static final String DEATH_LOSS = "hyperfactions.cmd.power.death_loss";
    public static final String REGEN = "hyperfactions.cmd.power.regen";
    public static final String NO_PERMISSION = "hyperfactions.cmd.power.no_permission";
    public static final String HEADER = "hyperfactions.cmd.power.header";
    public static final String CURRENT = "hyperfactions.cmd.power.current";

    private Power() {}
  }

  /** /f ally, /f enemy, /f neutral, /f relations command messages. */
  public static final class Relation {
    public static final String ALLY_SENT = "hyperfactions.cmd.relation.ally_sent";
    public static final String ALLY_RECEIVED = "hyperfactions.cmd.relation.ally_received";
    public static final String ALLY_FORMED = "hyperfactions.cmd.relation.ally_formed";
    public static final String ENEMY_DECLARED = "hyperfactions.cmd.relation.enemy_declared";
    public static final String ENEMY_RECEIVED = "hyperfactions.cmd.relation.enemy_received";
    public static final String NEUTRAL_SET = "hyperfactions.cmd.relation.neutral_set";
    public static final String ALREADY_RELATION = "hyperfactions.cmd.relation.already_relation";
    public static final String CANNOT_SELF = "hyperfactions.cmd.relation.cannot_self";
    public static final String MAX_ALLIES = "hyperfactions.cmd.relation.max_allies";
    // Ally
    public static final String ALLY_NO_PERMISSION = "hyperfactions.cmd.relation.ally_no_permission";
    public static final String ALLY_USAGE = "hyperfactions.cmd.relation.ally_usage";
    public static final String ALREADY_ALLY = "hyperfactions.cmd.relation.already_ally";
    public static final String ALLY_FAILED = "hyperfactions.cmd.relation.ally_failed";
    // Enemy
    public static final String ENEMY_NO_PERMISSION = "hyperfactions.cmd.relation.enemy_no_permission";
    public static final String ENEMY_USAGE = "hyperfactions.cmd.relation.enemy_usage";
    public static final String ALREADY_ENEMY = "hyperfactions.cmd.relation.already_enemy";
    public static final String MAX_ENEMIES = "hyperfactions.cmd.relation.max_enemies";
    public static final String ENEMY_FAILED = "hyperfactions.cmd.relation.enemy_failed";
    // Neutral
    public static final String NEUTRAL_NO_PERMISSION = "hyperfactions.cmd.relation.neutral_no_permission";
    public static final String NEUTRAL_USAGE = "hyperfactions.cmd.relation.neutral_usage";
    public static final String ALREADY_NEUTRAL = "hyperfactions.cmd.relation.already_neutral";
    public static final String NEUTRAL_FAILED = "hyperfactions.cmd.relation.neutral_failed";
    // Relations list
    public static final String VIEW_NO_PERMISSION = "hyperfactions.cmd.relation.view_no_permission";
    public static final String HEADER = "hyperfactions.cmd.relation.header";
    public static final String ALLIES_COUNT = "hyperfactions.cmd.relation.allies_count";
    public static final String ENEMIES_COUNT = "hyperfactions.cmd.relation.enemies_count";
    public static final String LIST_ENTRY = "hyperfactions.cmd.relation.list_entry";

    private Relation() {}
  }

  /** /f c (chat) command messages. */
  public static final class Chat {
    public static final String MODE_FACTION = "hyperfactions.cmd.chat.mode_faction";
    public static final String MODE_ALLY = "hyperfactions.cmd.chat.mode_ally";
    public static final String MODE_PUBLIC = "hyperfactions.cmd.chat.mode_public";
    public static final String USAGE = "hyperfactions.cmd.chat.usage";
    public static final String NO_PERMISSION = "hyperfactions.cmd.chat.no_permission";
    public static final String MODE_SET = "hyperfactions.cmd.chat.mode_set";

    private Chat() {}
  }

  /** /f invites command messages. */
  public static final class Invites {
    public static final String NOT_OFFICER = "hyperfactions.cmd.invites.not_officer";
    public static final String HEADER = "hyperfactions.cmd.invites.header";
    public static final String NO_PENDING = "hyperfactions.cmd.invites.no_pending";
    public static final String OUTGOING = "hyperfactions.cmd.invites.outgoing";
    public static final String OUTGOING_ENTRY = "hyperfactions.cmd.invites.outgoing_entry";
    public static final String REQUESTS = "hyperfactions.cmd.invites.requests";
    public static final String REQUEST_ENTRY = "hyperfactions.cmd.invites.request_entry";
    public static final String YOUR_INVITES_HEADER = "hyperfactions.cmd.invites.your_invites_header";
    public static final String NO_INVITES = "hyperfactions.cmd.invites.no_invites";
    public static final String INVITE_ENTRY = "hyperfactions.cmd.invites.invite_entry";

    private Invites() {}
  }

  /** /f request command messages. */
  public static final class Request {
    public static final String NO_PERMISSION = "hyperfactions.cmd.request.no_permission";
    public static final String ALREADY_IN_NAMED = "hyperfactions.cmd.request.already_in_named";
    public static final String USE_LEAVE_HINT = "hyperfactions.cmd.request.use_leave_hint";
    public static final String USAGE = "hyperfactions.cmd.request.usage";
    public static final String FACTION_OPEN = "hyperfactions.cmd.request.faction_open";
    public static final String ALREADY_REQUESTED = "hyperfactions.cmd.request.already_requested";
    public static final String HAS_INVITE = "hyperfactions.cmd.request.has_invite";
    public static final String SENT = "hyperfactions.cmd.request.sent";
    public static final String YOUR_MESSAGE = "hyperfactions.cmd.request.your_message";
    public static final String OFFICER_REVIEW = "hyperfactions.cmd.request.officer_review";
    public static final String OFFICER_NOTIFY = "hyperfactions.cmd.request.officer_notify";
    public static final String OFFICER_REVIEW_HINT = "hyperfactions.cmd.request.officer_review_hint";

    private Request() {}
  }

  /** /f rename, /f desc, /f color, /f open, /f close, /f settings command messages. */
  public static final class Settings {
    public static final String RENAMED = "hyperfactions.cmd.settings.renamed";
    public static final String DESCRIPTION_SET = "hyperfactions.cmd.settings.description_set";
    public static final String COLOR_SET = "hyperfactions.cmd.settings.color_set";
    public static final String OPENED = "hyperfactions.cmd.settings.opened";
    public static final String CLOSED = "hyperfactions.cmd.settings.closed";

    private Settings() {}
  }

  /** /f balance, /f deposit, /f withdraw, /f money command messages. */
  public static final class Economy {
    public static final String BALANCE = "hyperfactions.cmd.economy.balance";
    public static final String DEPOSITED = "hyperfactions.cmd.economy.deposited";
    public static final String WITHDRAWN = "hyperfactions.cmd.economy.withdrawn";
    public static final String TRANSFERRED = "hyperfactions.cmd.economy.transferred";
    public static final String INSUFFICIENT = "hyperfactions.cmd.economy.insufficient";
    public static final String INVALID_AMOUNT = "hyperfactions.cmd.economy.invalid_amount";
    public static final String ECONOMY_DISABLED = "hyperfactions.cmd.economy.economy_disabled";
    // Balance
    public static final String BALANCE_NO_PERMISSION = "hyperfactions.cmd.economy.balance_no_permission";
    public static final String TREASURY_UNAVAILABLE = "hyperfactions.cmd.economy.treasury_unavailable";
    public static final String BALANCE_DISPLAY = "hyperfactions.cmd.economy.balance_display";
    // Deposit
    public static final String DEPOSIT_NO_PERMISSION = "hyperfactions.cmd.economy.deposit_no_permission";
    public static final String DEPOSIT_FACTION_DENIED = "hyperfactions.cmd.economy.deposit_faction_denied";
    public static final String DEPOSIT_USAGE = "hyperfactions.cmd.economy.deposit_usage";
    public static final String AMOUNT_POSITIVE = "hyperfactions.cmd.economy.amount_positive";
    public static final String WALLET_INSUFFICIENT = "hyperfactions.cmd.economy.wallet_insufficient";
    public static final String WALLET_WITHDRAW_FAILED = "hyperfactions.cmd.economy.wallet_withdraw_failed";
    public static final String DEPOSIT_FAILED = "hyperfactions.cmd.economy.deposit_failed";
    // Withdraw
    public static final String WITHDRAW_NO_PERMISSION = "hyperfactions.cmd.economy.withdraw_no_permission";
    public static final String WITHDRAW_FACTION_DENIED = "hyperfactions.cmd.economy.withdraw_faction_denied";
    public static final String WITHDRAW_USAGE = "hyperfactions.cmd.economy.withdraw_usage";
    public static final String WITHDRAW_LIMIT_DENIED = "hyperfactions.cmd.economy.withdraw_limit_denied";
    public static final String WALLET_DEPOSIT_FAILED = "hyperfactions.cmd.economy.wallet_deposit_failed";
    public static final String WITHDRAW_LIMIT_EXCEEDED = "hyperfactions.cmd.economy.withdraw_limit_exceeded";
    public static final String WITHDRAW_FAILED = "hyperfactions.cmd.economy.withdraw_failed";
    // Transfer
    public static final String TRANSFER_NO_PERMISSION = "hyperfactions.cmd.economy.transfer_no_permission";
    public static final String TRANSFER_FACTION_DENIED = "hyperfactions.cmd.economy.transfer_faction_denied";
    public static final String TRANSFER_USAGE = "hyperfactions.cmd.economy.transfer_usage";
    public static final String TRANSFER_SELF = "hyperfactions.cmd.economy.transfer_self";
    public static final String TRANSFER_LIMIT_DENIED = "hyperfactions.cmd.economy.transfer_limit_denied";
    public static final String TRANSFER_LIMIT_EXCEEDED = "hyperfactions.cmd.economy.transfer_limit_exceeded";
    public static final String TRANSFER_FAILED = "hyperfactions.cmd.economy.transfer_failed";
    // Log
    public static final String LOG_NO_PERMISSION = "hyperfactions.cmd.economy.log_no_permission";
    public static final String LOG_HEADER = "hyperfactions.cmd.economy.log_header";
    public static final String LOG_EMPTY = "hyperfactions.cmd.economy.log_empty";
    // Money help
    public static final String MONEY_HELP_HEADER = "hyperfactions.cmd.economy.money_help_header";
    public static final String MONEY_HELP_BALANCE = "hyperfactions.cmd.economy.money_help_balance";
    public static final String MONEY_HELP_DEPOSIT = "hyperfactions.cmd.economy.money_help_deposit";
    public static final String MONEY_HELP_WITHDRAW = "hyperfactions.cmd.economy.money_help_withdraw";
    public static final String MONEY_HELP_TRANSFER = "hyperfactions.cmd.economy.money_help_transfer";
    public static final String MONEY_HELP_LOG = "hyperfactions.cmd.economy.money_help_log";

    private Economy() {}
  }

  /** /f info, /f who, /f list, /f members, /f map, /f help command messages. */
  public static final class Info {
    public static final String FACTION_HEADER = "hyperfactions.cmd.info.faction_header";
    public static final String PLAYER_HEADER = "hyperfactions.cmd.info.player_header";
    // Info command
    public static final String NO_PERMISSION = "hyperfactions.cmd.info.no_permission";
    public static final String FACTION_NOT_FOUND = "hyperfactions.cmd.info.faction_not_found";
    public static final String NOT_IN_FACTION_HINT = "hyperfactions.cmd.info.not_in_faction_hint";
    public static final String LEADER = "hyperfactions.cmd.info.leader";
    public static final String MEMBERS = "hyperfactions.cmd.info.members";
    public static final String POWER = "hyperfactions.cmd.info.power";
    public static final String CLAIMS = "hyperfactions.cmd.info.claims";
    public static final String RAIDABLE = "hyperfactions.cmd.info.raidable";
    public static final String ALLIES = "hyperfactions.cmd.info.allies";
    public static final String ENEMIES = "hyperfactions.cmd.info.enemies";
    public static final String THEY_CONSIDER = "hyperfactions.cmd.info.they_consider";
    public static final String YOU_CONSIDER = "hyperfactions.cmd.info.you_consider";
    // Members command
    public static final String MEMBERS_NO_PERMISSION = "hyperfactions.cmd.info.members_no_permission";
    public static final String MEMBERS_HEADER = "hyperfactions.cmd.info.members_header";
    public static final String MEMBER_ONLINE = "hyperfactions.cmd.info.member_online";
    // List command
    public static final String LIST_NO_PERMISSION = "hyperfactions.cmd.info.list_no_permission";
    public static final String LIST_EMPTY = "hyperfactions.cmd.info.list_empty";
    public static final String LIST_HEADER = "hyperfactions.cmd.info.list_header";
    public static final String LIST_ENTRY = "hyperfactions.cmd.info.list_entry";
    public static final String LIST_ENTRY_RAIDABLE = "hyperfactions.cmd.info.list_entry_raidable";
    // Help command
    public static final String HELP_NO_PERMISSION = "hyperfactions.cmd.info.help_no_permission";
    // Who command
    public static final String WHO_NO_PERMISSION = "hyperfactions.cmd.info.who_no_permission";
    public static final String WHO_FACTION = "hyperfactions.cmd.info.who_faction";
    public static final String WHO_ROLE = "hyperfactions.cmd.info.who_role";
    public static final String WHO_JOINED = "hyperfactions.cmd.info.who_joined";
    public static final String WHO_FACTION_NONE = "hyperfactions.cmd.info.who_faction_none";
    public static final String WHO_POWER = "hyperfactions.cmd.info.who_power";
    public static final String WHO_STATUS = "hyperfactions.cmd.info.who_status";
    public static final String WHO_LAST_SEEN = "hyperfactions.cmd.info.who_last_seen";
    // Map command
    public static final String MAP_NO_PERMISSION = "hyperfactions.cmd.info.map_no_permission";
    public static final String MAP_HEADER = "hyperfactions.cmd.info.map_header";
    public static final String MAP_LEGEND = "hyperfactions.cmd.info.map_legend";
    public static final String MAP_GUI_HINT = "hyperfactions.cmd.info.map_gui_hint";

    private Info() {}
  }

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
  // Protection — denial messages
  // =====================================================================

  /** Protection denial messages shown when actions are blocked. */
  public static final class Protection {
    public static final String BUILD = "hyperfactions.protection.build";
    public static final String BREAK = "hyperfactions.protection.break_block";
    public static final String INTERACT = "hyperfactions.protection.interact";
    public static final String CONTAINER = "hyperfactions.protection.container";
    public static final String PVP_DISABLED = "hyperfactions.protection.pvp_disabled";
    public static final String SAFEZONE = "hyperfactions.protection.safezone";
    public static final String WARZONE = "hyperfactions.protection.warzone";

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

    private Territory() {}
  }

  // =====================================================================
  // Announcements — faction-wide broadcasts
  // =====================================================================

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

    private Announce() {}
  }

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

    private Nav() {}
  }

  /** Dashboard page labels. */
  public static final class Dashboard {
    public static final String POWER_LABEL = "hyperfactions_gui.dashboard.power_label";
    public static final String LAND_LABEL = "hyperfactions_gui.dashboard.land_label";
    public static final String MEMBERS_LABEL = "hyperfactions_gui.dashboard.members_label";
    public static final String ONLINE_LABEL = "hyperfactions_gui.dashboard.online_label";
    public static final String ALLIES_LABEL = "hyperfactions_gui.dashboard.allies_label";
    public static final String ENEMIES_LABEL = "hyperfactions_gui.dashboard.enemies_label";

    private Dashboard() {}
  }

  /** Help GUI category display names. */
  public static final class HelpGui {
    public static final String WELCOME = "hyperfactions_gui.help.category.welcome";
    public static final String YOUR_FACTION = "hyperfactions_gui.help.category.your_faction";
    public static final String POWER_LAND = "hyperfactions_gui.help.category.power_land";
    public static final String DIPLOMACY = "hyperfactions_gui.help.category.diplomacy";
    public static final String COMBAT = "hyperfactions_gui.help.category.combat";
    public static final String ECONOMY = "hyperfactions_gui.help.category.economy";
    public static final String QUICK_REF = "hyperfactions_gui.help.category.quick_ref";

    private HelpGui() {}
  }

  /** Player settings page labels. */
  public static final class PlayerSettings {
    public static final String TITLE = "hyperfactions_gui.player_settings.title";
    public static final String LANGUAGE_SECTION = "hyperfactions_gui.player_settings.language_section";
    public static final String AUTO_DETECT = "hyperfactions_gui.player_settings.auto_detect";
    public static final String NOTIFICATIONS_SECTION = "hyperfactions_gui.player_settings.notifications_section";
    public static final String TERRITORY_ALERTS = "hyperfactions_gui.player_settings.territory_alerts";
    public static final String DEATH_ANNOUNCEMENTS = "hyperfactions_gui.player_settings.death_announcements";
    public static final String POWER_NOTIFICATIONS = "hyperfactions_gui.player_settings.power_notifications";

    private PlayerSettings() {}
  }
}
