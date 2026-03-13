package com.hyperfactions.command.admin;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.admin.handler.AdminBackupHandler;
import com.hyperfactions.command.admin.handler.AdminDebugHandler;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.integration.SentryIntegration;
import com.hyperfactions.command.admin.handler.AdminEconomyHandler;
import com.hyperfactions.command.admin.handler.AdminImportHandler;
import com.hyperfactions.command.admin.handler.AdminIntegrationHandler;
import com.hyperfactions.command.admin.handler.AdminMapDecayHandler;
import com.hyperfactions.command.admin.handler.AdminPowerHandler;
import com.hyperfactions.command.admin.handler.AdminTestHandler;
import com.hyperfactions.command.admin.handler.AdminUpdateHandler;
import com.hyperfactions.command.admin.handler.AdminWorldHandler;
import com.hyperfactions.command.admin.handler.AdminZoneHandler;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.data.Faction;
import com.hyperfactions.gui.admin.page.AdminPlayerInfoPage;
import com.hyperfactions.platform.HyperFactionsPlugin;
import com.hyperfactions.util.ChunkUtil;
import com.hyperfactions.util.CommandHelp;
import com.hyperfactions.util.HFMessages;
import com.hyperfactions.util.HelpFormatter;
import com.hyperfactions.util.AdminKeys;
import com.hyperfactions.util.HelpKeys;
import com.hyperfactions.util.MessageUtil;
import com.hyperfactions.util.PlayerResolver;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Container subcommand: /f admin
 * Routes to admin subcommands: reload, sync, zones, backup, debug, import, update, etc.
 * Opens admin GUI when called with no arguments.
 */
public class AdminSubCommand extends AbstractAsyncCommand {

  private final HyperFactions hyperFactions;

  private final HyperFactionsPlugin plugin;

  private static final UUID CONSOLE_UUID = new UUID(0L, 0L);

  // Handler instances
  private final AdminIntegrationHandler integrationHandler;

  private final AdminZoneHandler zoneHandler;

  private final AdminUpdateHandler updateHandler;

  private final AdminBackupHandler backupHandler;

  private final AdminImportHandler importHandler;

  private final AdminDebugHandler debugHandler;

  private final AdminPowerHandler powerHandler;

  private final AdminEconomyHandler economyHandler;

  private final AdminMapDecayHandler mapDecayHandler;

  private final AdminTestHandler testHandler;

  private final AdminWorldHandler worldHandler;

  /** Creates a new AdminSubCommand. */
  public AdminSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
    super("admin", "Admin commands");
    this.hyperFactions = hyperFactions;
    this.plugin = plugin;
    setAllowsExtraArguments(true);

    // Initialize handlers
    this.integrationHandler = new AdminIntegrationHandler(hyperFactions);
    this.zoneHandler = new AdminZoneHandler(hyperFactions);
    this.updateHandler = new AdminUpdateHandler(hyperFactions);
    this.backupHandler = new AdminBackupHandler(hyperFactions);
    this.importHandler = new AdminImportHandler(hyperFactions);
    this.debugHandler = new AdminDebugHandler(hyperFactions);
    this.powerHandler = new AdminPowerHandler(hyperFactions, plugin);
    this.economyHandler = new AdminEconomyHandler(hyperFactions);
    this.mapDecayHandler = new AdminMapDecayHandler(hyperFactions);
    this.testHandler = new AdminTestHandler(hyperFactions);
    this.worldHandler = new AdminWorldHandler(hyperFactions);
  }

  /** Checks if generate permission. */
  @Override
  protected boolean canGeneratePermission() {
    return false;
  }

  private static Message prefix() {
    return CommandUtil.prefix();
  }

  private static Message msg(String text, String color) {
    return CommandUtil.msg(text, color);
  }

  private boolean hasPermission(@Nullable PlayerRef player, String permission) {
    if (player == null) { // console always permitted
      return true;
    }
    return CommandUtil.hasPermission(player, permission);
  }

  private boolean requirePlayer(CommandContext ctx, boolean isPlayer) {
    if (!isPlayer) {
      ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.PLAYER_ONLY), COLOR_RED)));
      return false;
    }
    return true;
  }

  private static final String COLOR_CYAN = CommandUtil.COLOR_CYAN;

  private static final String COLOR_GREEN = CommandUtil.COLOR_GREEN;

  private static final String COLOR_RED = CommandUtil.COLOR_RED;

  private static final String COLOR_YELLOW = CommandUtil.COLOR_YELLOW;

  private static final String COLOR_GRAY = CommandUtil.COLOR_GRAY;

  private static final String COLOR_WHITE = CommandUtil.COLOR_WHITE;

  /** Executes the command. */
  @Override
  @NotNull
  protected CompletableFuture<Void> executeAsync(@NotNull CommandContext ctx) {
    boolean isPlayer = ctx.isPlayer();

    if (isPlayer) {
      // Player path — dispatch to world thread for safe ECS access
      // (same pattern as AbstractPlayerCommand)
      Ref<EntityStore> ref = ctx.senderAsPlayerRef();
      if (ref == null || !ref.isValid()) {
        ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.PLAYER_CONTEXT), COLOR_RED)));
        return CompletableFuture.completedFuture(null);
      }
      Store<EntityStore> store = ref.getStore();
      World currentWorld = store.getExternalData().getWorld();

      return runAsync(ctx, () -> {
        PlayerRef player = store.getComponent(ref, PlayerRef.getComponentType());
        if (player == null) {
          ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.ENTITY_NOT_FOUND), COLOR_RED)));
          return;
        }
        dispatchCommand(ctx, store, ref, player, currentWorld, true);
      }, currentWorld);
    } else {
      // Console path — no ECS access needed, run directly
      dispatchCommand(ctx, null, null, null, null, false);
      return CompletableFuture.completedFuture(null);
    }
  }

  /**
   * Dispatches the admin command after thread-safe context resolution.
   * For players, this runs on the world thread. For console, runs on the calling thread.
   */
  private void dispatchCommand(@NotNull CommandContext ctx, @Nullable Store<EntityStore> store,
                @Nullable Ref<EntityStore> ref, @Nullable PlayerRef player,
                @Nullable World currentWorld, boolean isPlayer) {

    if (!hasPermission(player, Permissions.ADMIN)) {
      ctx.sendMessage(prefix().insert(msg(HFMessages.get(player, AdminKeys.AdminCmd.NO_PERMISSION), COLOR_RED)));
      return;
    }

    String[] args = CommandUtil.parseRawArgs(ctx.getInputString(), 2);

    // No args - open admin GUI (player) or show help (console)
    if (args.length == 0) {
      if (!isPlayer) {
        showAdminHelp(ctx, player);
      } else {
        Player playerEntity = store.getComponent(ref, Player.getComponentType());
        if (playerEntity == null) {
          ctx.sendMessage(prefix().insert(msg(HFMessages.get(player, AdminKeys.AdminCmd.ENTITY_NOT_FOUND), COLOR_RED)));
        } else {
          hyperFactions.getGuiManager().openAdminMain(playerEntity, ref, store, player);
        }
      }
      return;
    }

    String adminCmd = args[0].toLowerCase();

    // Show help for admin commands
    if (adminCmd.equals("help") || adminCmd.equals("?")) {
      showAdminHelp(ctx, player);
      return;
    }

    // Extract player position (only if player)
    int chunkX = 0, chunkZ = 0;
    if (isPlayer) {
      TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
      if (transform == null) {
        return;
      }
      Vector3d pos = transform.getPosition();
      chunkX = ChunkUtil.toChunkCoord(pos.getX());
      chunkZ = ChunkUtil.toChunkCoord(pos.getZ());
    }

    // Sender UUID for operations that track who performed them
    UUID senderUuid = isPlayer ? player.getUuid() : CONSOLE_UUID;

    // Sub-args for handler delegation
    String[] subArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];

    switch (adminCmd) {
      // Console-compatible commands
      case "reload" -> handleReload(ctx, player);
      case "sync" -> handleSync(ctx, player);
      case "integrations" -> integrationHandler.handleIntegrations(ctx);
      case "integration" -> integrationHandler.handleIntegrationDetail(ctx, subArgs);
      case "update" -> updateHandler.handleAdminUpdate(ctx, senderUuid, subArgs);
      case "rollback" -> updateHandler.handleAdminRollback(ctx);
      case "backup" -> backupHandler.handleAdminBackup(ctx, player, senderUuid, subArgs);
      case "import" -> importHandler.handleAdminImport(ctx, player, subArgs);
      case "debug" -> debugHandler.handleDebug(ctx, store, ref, player, currentWorld, subArgs);
      case "decay" -> mapDecayHandler.handleAdminDecay(ctx, player, subArgs);
      case "map" -> mapDecayHandler.handleAdminMap(ctx, player, subArgs);
      case "zones", "zone" -> zoneHandler.handleAdminZone(ctx, store, ref, player, currentWorld,
          chunkX, chunkZ, subArgs, isPlayer, senderUuid);

      // Player-only commands (GUI / location)
      case "factions" -> {
        if (!requirePlayer(ctx, isPlayer)) {
          break;
        }
        Player playerEntity = store.getComponent(ref, Player.getComponentType());
        if (playerEntity != null) {
          hyperFactions.getGuiManager().openAdminFactions(playerEntity, ref, store, player);
        }
      }
      case "config" -> {
        if (!requirePlayer(ctx, isPlayer)) {
          break;
        }
        Player playerEntity = store.getComponent(ref, Player.getComponentType());
        if (playerEntity != null) {
          hyperFactions.getGuiManager().openAdminConfig(playerEntity, ref, store, player);
        }
      }
      case "backups" -> {
        if (!requirePlayer(ctx, isPlayer)) {
          break;
        }
        Player playerEntity = store.getComponent(ref, Player.getComponentType());
        if (playerEntity != null) {
          hyperFactions.getGuiManager().openAdminBackups(playerEntity, ref, store, player);
        }
      }
      case "test" -> testHandler.handleTest(ctx, store, ref, player, subArgs, isPlayer);
      case "safezone" -> { if (requirePlayer(ctx, isPlayer)) zoneHandler.handleSafezone(ctx, player, currentWorld, chunkX, chunkZ, args); }
      case "warzone" -> { if (requirePlayer(ctx, isPlayer)) zoneHandler.handleWarzone(ctx, player, currentWorld, chunkX, chunkZ, args); }
      case "removezone" -> { if (requirePlayer(ctx, isPlayer)) zoneHandler.handleRemovezone(ctx, currentWorld, chunkX, chunkZ); }
      case "zoneflag" -> { if (requirePlayer(ctx, isPlayer)) zoneHandler.handleZoneFlag(ctx, currentWorld.getName(), chunkX, chunkZ, subArgs); }
      case "clearhistory" -> powerHandler.handleClearHistory(ctx, player, subArgs);
      case "power" -> powerHandler.handleAdminPower(ctx, player, senderUuid, subArgs);
      case "economy", "econ", "treasury" -> economyHandler.handleAdminEconomy(ctx, player, senderUuid, subArgs);
      case "world", "worlds" -> worldHandler.handleAdminWorld(ctx, player, subArgs);
      case "version" -> handleVersion(ctx, store, ref, player, isPlayer);
      case "sentry" -> handleSentry(ctx, subArgs);
      case "log", "logs", "activitylog" -> {
        if (!requirePlayer(ctx, isPlayer)) {
          break;
        }
        Player playerEntity = store.getComponent(ref, Player.getComponentType());
        if (playerEntity != null) {
          hyperFactions.getGuiManager().openAdminActivityLog(playerEntity, ref, store, player);
        }
      }
      case "info" -> {
        if (!requirePlayer(ctx, isPlayer)) {
          break;
        }
        String factionName = subArgs.length > 0 ? String.join(" ", subArgs) : null;
        Faction faction;
        if (factionName != null) {
          faction = hyperFactions.getFactionManager().getFactionByName(factionName);
        } else {
          faction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());
        }
        if (faction == null) {
          ctx.sendMessage(prefix().insert(msg(HFMessages.get(player, AdminKeys.AdminCmd.FACTION_NOT_FOUND, factionName != null ? factionName : ""), COLOR_RED)));
          break;
        }
        Player playerEntity = store.getComponent(ref, Player.getComponentType());
        if (playerEntity != null) {
          hyperFactions.getGuiManager().openAdminFactionInfo(playerEntity, ref, store, player, faction.id());
        }
      }
      case "who" -> {
        if (!requirePlayer(ctx, isPlayer)) {
          break;
        }
        String targetName = subArgs.length > 0 ? subArgs[0] : null;
        UUID targetUuid;
        String resolvedName;
        if (targetName != null) {
          var resolved = PlayerResolver.resolve(hyperFactions, targetName);
          if (resolved == null) {
            ctx.sendMessage(prefix().insert(msg(HFMessages.get(player, AdminKeys.AdminCmd.PLAYER_NOT_FOUND, targetName), COLOR_RED)));
            break;
          }
          targetUuid = resolved.uuid();
          resolvedName = resolved.username();
        } else {
          targetUuid = player.getUuid();
          resolvedName = player.getUsername();
        }
        Faction targetFaction = hyperFactions.getFactionManager().getPlayerFaction(targetUuid);
        Player playerEntity = store.getComponent(ref, Player.getComponentType());
        if (playerEntity != null) {
          hyperFactions.getGuiManager().openAdminPlayerInfo(playerEntity, ref, store, player,
              targetUuid, resolvedName, targetFaction != null ? targetFaction.id() : null,
              AdminPlayerInfoPage.Origin.PLAYERS_LIST);
        }
      }
      default -> ctx.sendMessage(prefix().insert(msg(HFMessages.get(player, AdminKeys.AdminCmd.UNKNOWN_COMMAND), COLOR_RED)));
    }
  }

  private void showAdminHelp(CommandContext ctx, @Nullable PlayerRef player) {
    List<CommandHelp> commands = new ArrayList<>();
    commands.add(new CommandHelp("/f admin", HelpKeys.Help.ADMIN_CMD_DASHBOARD));
    commands.add(new CommandHelp("/f admin factions", HelpKeys.Help.ADMIN_CMD_FACTIONS));
    commands.add(new CommandHelp("/f admin zone", HelpKeys.Help.ADMIN_CMD_ZONE));
    commands.add(new CommandHelp("/f admin config", HelpKeys.Help.ADMIN_CMD_CONFIG));
    commands.add(new CommandHelp("/f admin backup", HelpKeys.Help.ADMIN_CMD_BACKUP));
    commands.add(new CommandHelp("/f admin import", HelpKeys.Help.ADMIN_CMD_IMPORT));
    commands.add(new CommandHelp("/f admin update", HelpKeys.Help.ADMIN_CMD_UPDATE));
    commands.add(new CommandHelp("/f admin update mixin", HelpKeys.Help.ADMIN_CMD_UPDATE_MIXIN));
    commands.add(new CommandHelp("/f admin update toggle-mixin-download", HelpKeys.Help.ADMIN_CMD_UPDATE_TOGGLE));
    commands.add(new CommandHelp("/f admin rollback", HelpKeys.Help.ADMIN_CMD_ROLLBACK));
    commands.add(new CommandHelp("/f admin reload", HelpKeys.Help.ADMIN_CMD_RELOAD));
    commands.add(new CommandHelp("/f admin sync", HelpKeys.Help.ADMIN_CMD_SYNC));
    commands.add(new CommandHelp("/f admin debug", HelpKeys.Help.ADMIN_CMD_DEBUG));
    commands.add(new CommandHelp("/f admin decay", HelpKeys.Help.ADMIN_CMD_DECAY));
    commands.add(new CommandHelp("/f admin map", HelpKeys.Help.ADMIN_CMD_MAP));
    commands.add(new CommandHelp("/f admin safezone [name]", HelpKeys.Help.ADMIN_CMD_SAFEZONE));
    commands.add(new CommandHelp("/f admin warzone [name]", HelpKeys.Help.ADMIN_CMD_WARZONE));
    commands.add(new CommandHelp("/f admin removezone", HelpKeys.Help.ADMIN_CMD_REMOVEZONE));
    commands.add(new CommandHelp("/f admin zoneflag <flag> <value>", HelpKeys.Help.ADMIN_CMD_ZONEFLAG));
    commands.add(new CommandHelp("/f admin integrations", HelpKeys.Help.ADMIN_CMD_INTEGRATIONS));
    commands.add(new CommandHelp("/f admin integration <name>", HelpKeys.Help.ADMIN_CMD_INTEGRATION));
    commands.add(new CommandHelp("/f admin clearhistory <player>", HelpKeys.Help.ADMIN_CMD_CLEARHISTORY));
    commands.add(new CommandHelp("/f admin power", HelpKeys.Help.ADMIN_CMD_POWER));
    commands.add(new CommandHelp("/f admin economy", HelpKeys.Help.ADMIN_CMD_ECONOMY));
    commands.add(new CommandHelp("/f admin economy upkeep", HelpKeys.Help.ADMIN_CMD_ECONOMY_UPKEEP));
    commands.add(new CommandHelp("/f admin info [faction]", HelpKeys.Help.ADMIN_CMD_INFO));
    commands.add(new CommandHelp("/f admin who [player]", HelpKeys.Help.ADMIN_CMD_WHO));
    commands.add(new CommandHelp("/f admin log", HelpKeys.Help.ADMIN_CMD_LOG));
    commands.add(new CommandHelp("/f admin world", HelpKeys.Help.ADMIN_CMD_WORLD));
    commands.add(new CommandHelp("/f admin version", HelpKeys.Help.ADMIN_CMD_VERSION));
    commands.add(new CommandHelp("/f admin sentry", HelpKeys.Help.ADMIN_CMD_SENTRY));
    commands.add(new CommandHelp("/f admin sentry disable", HelpKeys.Help.ADMIN_CMD_SENTRY_DISABLE));
    commands.add(new CommandHelp("/f admin sentry enable", HelpKeys.Help.ADMIN_CMD_SENTRY_ENABLE));
    commands.add(new CommandHelp("/f admin test gui", HelpKeys.Help.ADMIN_CMD_TEST_GUI));
    commands.add(new CommandHelp("/f admin test sentry", HelpKeys.Help.ADMIN_CMD_TEST_SENTRY));
    commands.add(new CommandHelp("/f admin test md", HelpKeys.Help.ADMIN_CMD_TEST_MD));
    ctx.sendMessage(HelpFormatter.buildHelp(HelpKeys.Help.ADMIN_TITLE, HelpKeys.Help.ADMIN_DESCRIPTION, commands, null, player));
  }

  // === Version ===
  private void handleVersion(CommandContext ctx, @Nullable Store<EntityStore> store,
               @Nullable Ref<EntityStore> ref, @Nullable PlayerRef player,
               boolean isPlayer) {
    if (isPlayer) {
      // Open GUI for players
      Player playerEntity = store.getComponent(ref, Player.getComponentType());
      if (playerEntity != null) {
        hyperFactions.getGuiManager().openAdminVersion(playerEntity, ref, store, player);
      }
    } else {
      // Console output — mirrors the integration handler format
      integrationHandler.handleIntegrations(ctx);
      ctx.sendMessage(msg("", COLOR_GRAY));
      ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.VERSION_TITLE), COLOR_CYAN)));
      ctx.sendMessage(msg("  HyperFactions: v" + HyperFactions.VERSION, COLOR_WHITE));
      String serverVersion = com.hypixel.hytale.common.util.java.ManifestUtil.getVersion();
      ctx.sendMessage(msg("  " + HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.VERSION_SERVER,
          serverVersion != null ? serverVersion : "Unknown"), COLOR_WHITE));
      ctx.sendMessage(msg("  " + HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.VERSION_JAVA,
          System.getProperty("java.version", "Unknown")), COLOR_WHITE));
      String treasuryStatus = hyperFactions.isTreasuryEnabled()
          ? HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.VERSION_ACTIVE)
          : HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.VERSION_NOT_FOUND);
      ctx.sendMessage(msg("  " + HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.VERSION_TREASURY, treasuryStatus),
          hyperFactions.isTreasuryEnabled() ? COLOR_GREEN : COLOR_GRAY));
    }
  }

  // === Sentry ===
  private void handleSentry(CommandContext ctx, String[] args) {
    var debugConfig = ConfigManager.get().debug();

    if (args.length == 0) {
      // Show status
      boolean configEnabled = debugConfig.isSentryEnabled();
      boolean running = SentryIntegration.isInitialized();
      ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.SENTRY_HEADER), COLOR_CYAN)));
      ctx.sendMessage(msg("  " + HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.SENTRY_CONFIG,
          configEnabled ? "enabled" : "disabled"), configEnabled ? COLOR_GREEN : COLOR_GRAY));
      ctx.sendMessage(msg("  " + HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.SENTRY_STATUS,
          running ? "active" : "inactive"), running ? COLOR_GREEN : COLOR_GRAY));
      ctx.sendMessage(msg("  DSN: " + debugConfig.getSentryDsn(), COLOR_GRAY));
      ctx.sendMessage(msg("  Environment: " + debugConfig.getSentryEnvironment(), COLOR_GRAY));
      return;
    }

    switch (args[0].toLowerCase()) {
      case "disable", "optout", "off" -> {
        if (!debugConfig.isSentryEnabled()) {
          ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.SENTRY_ALREADY_DISABLED), COLOR_YELLOW)));
          return;
        }
        debugConfig.setSentryEnabled(false);
        debugConfig.save();
        SentryIntegration.close();
        ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.SENTRY_DISABLED), COLOR_GREEN)));
      }
      case "enable", "optin", "on" -> {
        if (debugConfig.isSentryEnabled()) {
          ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.SENTRY_ALREADY_ENABLED), COLOR_YELLOW)));
          return;
        }
        debugConfig.setSentryEnabled(true);
        debugConfig.save();
        // Try to initialize now if not already running
        if (!SentryIntegration.isInitialized()) {
          SentryIntegration.init(debugConfig);
        }
        ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.SENTRY_ENABLED), COLOR_GREEN)));
      }
      default -> ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.SENTRY_USAGE), COLOR_RED)));
    }
  }

  // === Reload ===
  private void handleReload(CommandContext ctx, PlayerRef player) {
    if (!hasPermission(player, Permissions.ADMIN)) {
      ctx.sendMessage(prefix().insert(msg(HFMessages.get(player, AdminKeys.AdminCmd.NO_PERMISSION), COLOR_RED)));
      return;
    }

    plugin.reloadConfig();
    ctx.sendMessage(prefix().insert(msg(HFMessages.get(player, AdminKeys.AdminCmd.CONFIG_RELOADED), COLOR_GREEN)));
  }

  // === Sync ===
  private void handleSync(CommandContext ctx, PlayerRef player) {
    if (!hasPermission(player, Permissions.ADMIN)) {
      ctx.sendMessage(prefix().insert(msg(HFMessages.get(player, AdminKeys.AdminCmd.NO_PERMISSION), COLOR_RED)));
      return;
    }

    ctx.sendMessage(prefix().insert(msg(HFMessages.get(player, AdminKeys.AdminCmd.SYNC_START), COLOR_CYAN)));

    hyperFactions.getFactionManager().syncFromDisk().thenAccept(result -> {
      ctx.sendMessage(prefix().insert(msg(HFMessages.get(player, AdminKeys.AdminCmd.SYNC_COMPLETE,
          result.factionsUpdated(), result.membersAdded(), result.membersUpdated()), COLOR_GREEN)));
    }).exceptionally(e -> {
      ctx.sendMessage(prefix().insert(msg(HFMessages.get(player, AdminKeys.AdminCmd.SYNC_FAILED, e.getMessage()), COLOR_RED)));
      return null;
    });
  }
}
