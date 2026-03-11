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
    public static final String INSUFFICIENT_POWER = "hyperfactions.cmd.claim.insufficient_power";

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
    public static final String CREATE = "hyperfactions_gui.nav.create";
    public static final String PLAYER_SETTINGS = "hyperfactions_gui.nav.player_settings";

    private Nav() {}
  }

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

  /** Faction info page labels. */
  public static final class FactionInfoGui {
    public static final String TITLE = "hyperfactions_gui.faction_info.title";
    public static final String NO_DESCRIPTION = "hyperfactions_gui.faction_info.no_description";
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
    public static final String BACK_BTN = "hyperfactions_gui.faction_info.back_btn";

    private FactionInfoGui() {}
  }

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

    private DashboardGui() {}
  }

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

    private GuiCommon() {}
  }

  /** Members page labels and messages. */
  public static final class MembersGui {
    public static final String TITLE = "hyperfactions_gui.members.title";
    public static final String SEARCH_LABEL = "hyperfactions_gui.members.search_label";
    public static final String SORT_LABEL = "hyperfactions_gui.members.sort_label";
    public static final String PREV_BTN = "hyperfactions_gui.members.prev_btn";
    public static final String NEXT_BTN = "hyperfactions_gui.members.next_btn";
    public static final String MEMBER_COUNT = "hyperfactions_gui.members.count";
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

    private MembersGui() {}
  }

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
    public static final String BACK_BTN = "hyperfactions_gui.playerinfo.back_btn";
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

  /** Chat channel display names (ChatManager). */
  public static final class ChatDisplay {
    public static final String PUBLIC = "hyperfactions.chat.display.public";
    public static final String FACTION = "hyperfactions.chat.display.faction";
    public static final String ALLY = "hyperfactions.chat.display.ally";

    private ChatDisplay() {}
  }

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
    public static final String MEMBER_COUNT_DISPLAY = "hyperfactions_gui.relations.member_count";

    private RelationsGui() {}
  }

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
    public static final String BACK_BTN = "hyperfactions_gui.treasury.back_btn";

    private TreasuryGui() {}
  }

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

    private InvitesGui() {}
  }

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
    public static final String OVERCLAIM_FAILED = "hyperfactions_gui.map.overclaim_failed";

    private MapGui() {}
  }


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
    public static final String MEMBER_COUNT = "hyperfactions_gui.newplayer.member_count";
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
    public static final String MEMBERS_SUFFIX = "hyperfactions_admin.common.members_suffix";
    public static final String CLAIMS_SUFFIX = "hyperfactions_admin.common.claims_suffix";
    public static final String FACTIONS_SUFFIX = "hyperfactions_admin.common.factions_suffix";
    public static final String PLAYERS_SUFFIX = "hyperfactions_admin.common.players_suffix";
    public static final String CHUNKS_SUFFIX = "hyperfactions_admin.common.chunks_suffix";
    public static final String ENTRIES_SUFFIX = "hyperfactions_admin.common.entries_suffix";
    public static final String FOUND_SUFFIX = "hyperfactions_admin.common.found_suffix";
    public static final String POWER_FORMAT = "hyperfactions_admin.common.power_format";
    public static final String RAIDABLE = "hyperfactions_admin.common.raidable";
    public static final String PROTECTED = "hyperfactions_admin.common.protected";
    public static final String NO_DESCRIPTION = "hyperfactions_admin.common.no_description";
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
    public static final String GUI_BACK = "hyperfactions_admin.gui.back";
    public static final String GUI_DONE = "hyperfactions_admin.gui.done";
    public static final String GUI_CANCEL = "hyperfactions_admin.gui.cancel";
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
    public static final String GUI_CONFIG_HEADING = "hyperfactions_admin.gui.config_heading";
    public static final String GUI_CONFIG_DESC1 = "hyperfactions_admin.gui.config_desc1";
    public static final String GUI_CONFIG_DESC2 = "hyperfactions_admin.gui.config_desc2";
    public static final String GUI_HELP_HEADING = "hyperfactions_admin.gui.help_heading";
    public static final String GUI_HELP_DESC1 = "hyperfactions_admin.gui.help_desc1";
    public static final String GUI_HELP_DESC2 = "hyperfactions_admin.gui.help_desc2";
    public static final String GUI_UPDATES_HEADING = "hyperfactions_admin.gui.updates_heading";
    public static final String GUI_UPDATES_DESC1 = "hyperfactions_admin.gui.updates_desc1";
    public static final String GUI_UPDATES_DESC2 = "hyperfactions_admin.gui.updates_desc2";

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
    public static final String GUI_SAVE = "hyperfactions_admin.gui.save";
    public static final String GUI_CLEAR = "hyperfactions_admin.gui.clear";

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
