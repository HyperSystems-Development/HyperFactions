package com.hyperfactions.command.admin.handler;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.util.CommandHelp;
import com.hyperfactions.util.HFMessages;
import com.hyperfactions.util.HelpFormatter;
import com.hyperfactions.util.AdminKeys;
import com.hyperfactions.util.HelpKeys;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.Nullable;

/**
 * Handles /f admin map and /f admin decay commands.
 */
public class AdminMapDecayHandler {

  private final HyperFactions hyperFactions;

  private static final String COLOR_CYAN = CommandUtil.COLOR_CYAN;

  private static final String COLOR_GREEN = CommandUtil.COLOR_GREEN;

  private static final String COLOR_RED = CommandUtil.COLOR_RED;

  private static final String COLOR_YELLOW = CommandUtil.COLOR_YELLOW;

  private static final String COLOR_GRAY = CommandUtil.COLOR_GRAY;

  private static final String COLOR_WHITE = CommandUtil.COLOR_WHITE;

  private static Message prefix() {
    return CommandUtil.prefix();
  }

  private static Message msg(String text, String color) {
    return CommandUtil.msg(text, color);
  }

  private boolean hasPermission(@Nullable PlayerRef player, String permission) {
    if (player == null) {
      return true;
    }
    return CommandUtil.hasPermission(player, permission);
  }

  /** Creates a new AdminMapDecayHandler. */
  public AdminMapDecayHandler(HyperFactions hyperFactions) {
    this.hyperFactions = hyperFactions;
  }

  // === Admin Map Commands ===

  /** Handles admin map. */
  public void handleAdminMap(CommandContext ctx, @Nullable PlayerRef player, String[] args) {
    if (!hasPermission(player, Permissions.ADMIN)) {
      ctx.sendMessage(prefix().insert(msg(HFMessages.get(player, AdminKeys.AdminCmd.NO_PERMISSION), COLOR_RED)));
      return;
    }

    if (args.length == 0) {
      showMapHelp(ctx, player);
      return;
    }

    String subCmd = args[0].toLowerCase();

    switch (subCmd) {
      case "refresh" -> handleMapRefresh(ctx, player);
      case "status" -> handleMapStatus(ctx);
      case "help", "?" -> showMapHelp(ctx, player);
      default -> {
        ctx.sendMessage(prefix().insert(msg(HFMessages.get(player, AdminKeys.AdminCmd.MAP_UNKNOWN_CMD), COLOR_RED)));
        showMapHelp(ctx, player);
      }
    }
  }

  private void showMapHelp(CommandContext ctx, @Nullable PlayerRef player) {
    List<CommandHelp> commands = new ArrayList<>();
    commands.add(new CommandHelp("/f admin map status", HelpKeys.Help.MAP_CMD_STATUS));
    commands.add(new CommandHelp("/f admin map refresh", HelpKeys.Help.MAP_CMD_REFRESH));
    ctx.sendMessage(HelpFormatter.buildHelp(HelpKeys.Help.MAP_TITLE, HelpKeys.Help.MAP_DESCRIPTION, commands, null, player));
  }

  /** Handles map refresh. */
  public void handleMapRefresh(CommandContext ctx, @Nullable PlayerRef player) {
    var worldMapService = hyperFactions.getWorldMapService();
    if (worldMapService == null) {
      ctx.sendMessage(prefix().insert(msg(HFMessages.get(player, AdminKeys.AdminCmd.MAP_NOT_AVAILABLE), COLOR_RED)));
      return;
    }

    ctx.sendMessage(prefix().insert(msg(HFMessages.get(player, AdminKeys.AdminCmd.MAP_REFRESHING), COLOR_YELLOW)));

    worldMapService.forceFullRefresh();

    ctx.sendMessage(prefix().insert(msg(HFMessages.get(player, AdminKeys.AdminCmd.MAP_REFRESHED), COLOR_GREEN)));
  }

  /** Handles map status. */
  public void handleMapStatus(CommandContext ctx) {
    var worldMapConfig = ConfigManager.get().worldMap();
    var worldMapService = hyperFactions.getWorldMapService();

    ctx.sendMessage(msg("=== " + HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.MAP_STATUS_HEADER) + " ===", COLOR_CYAN).bold(true));

    // Config status
    ctx.sendMessage(msg("Enabled: ", COLOR_GRAY)
      .insert(msg(worldMapConfig.isEnabled() ? "Yes" : "No", worldMapConfig.isEnabled() ? COLOR_GREEN : COLOR_RED)));
    ctx.sendMessage(msg("Refresh Mode: ", COLOR_GRAY)
      .insert(msg(worldMapConfig.getRefreshMode().getConfigName(), COLOR_WHITE)));

    if (worldMapService == null) {
      ctx.sendMessage(msg("Service: ", COLOR_GRAY).insert(msg("Not initialized", COLOR_RED)));
      return;
    }

    var scheduler = worldMapService.getRefreshScheduler();
    if (scheduler == null) {
      ctx.sendMessage(msg("Scheduler: ", COLOR_GRAY).insert(msg("Not initialized", COLOR_YELLOW)));
      return;
    }

    // Scheduler status
    ctx.sendMessage(msg("Effective Mode: ", COLOR_GRAY)
      .insert(msg(scheduler.getEffectiveMode().getConfigName(), COLOR_WHITE)));

    if (scheduler.isInFallbackMode()) {
      ctx.sendMessage(msg("Fallback: ", COLOR_GRAY)
        .insert(msg("Active (reflection failed)", COLOR_YELLOW)));
    }

    // Mode-specific settings
    var mode = worldMapConfig.getRefreshMode();
    switch (mode) {
      case PROXIMITY -> {
        ctx.sendMessage(msg("Proximity Radius: ", COLOR_GRAY)
          .insert(msg(worldMapConfig.getProximityChunkRadius() + " chunks", COLOR_WHITE)));
        ctx.sendMessage(msg("Batch Interval: ", COLOR_GRAY)
          .insert(msg(worldMapConfig.getProximityBatchIntervalTicks() + " ticks", COLOR_WHITE)));
        ctx.sendMessage(msg("Max Chunks/Batch: ", COLOR_GRAY)
          .insert(msg(String.valueOf(worldMapConfig.getProximityMaxChunksPerBatch()), COLOR_WHITE)));
      }
      case INCREMENTAL -> {
        ctx.sendMessage(msg("Batch Interval: ", COLOR_GRAY)
          .insert(msg(worldMapConfig.getIncrementalBatchIntervalTicks() + " ticks", COLOR_WHITE)));
        ctx.sendMessage(msg("Max Chunks/Batch: ", COLOR_GRAY)
          .insert(msg(String.valueOf(worldMapConfig.getIncrementalMaxChunksPerBatch()), COLOR_WHITE)));
      }
      case DEBOUNCED -> {
        ctx.sendMessage(msg("Debounce Delay: ", COLOR_GRAY)
          .insert(msg(worldMapConfig.getDebouncedDelaySeconds() + " seconds", COLOR_WHITE)));
      }
      case IMMEDIATE, MANUAL -> {
        // No additional settings to show
      }
      default -> throw new IllegalStateException("Unexpected value");
    }

    // Statistics
    ctx.sendMessage(msg("", COLOR_GRAY));
    ctx.sendMessage(msg("Statistics:", COLOR_YELLOW));
    ctx.sendMessage(msg("  Pending Chunks: ", COLOR_GRAY)
      .insert(msg(String.valueOf(scheduler.getPendingChunkCount()), COLOR_WHITE)));
    ctx.sendMessage(msg("  Total Refreshes: ", COLOR_GRAY)
      .insert(msg(String.valueOf(scheduler.getTotalRefreshes()), COLOR_WHITE)));
    ctx.sendMessage(msg("  Chunks Processed: ", COLOR_GRAY)
      .insert(msg(String.valueOf(scheduler.getChunksProcessed()), COLOR_WHITE)));
    ctx.sendMessage(msg("  Players Notified: ", COLOR_GRAY)
      .insert(msg(String.valueOf(scheduler.getPlayersNotified()), COLOR_WHITE)));

    var lastRefresh = scheduler.getLastRefreshTime();
    if (lastRefresh != null) {
      long secondsAgo = java.time.Duration.between(lastRefresh, java.time.Instant.now()).getSeconds();
      ctx.sendMessage(msg("  Last Refresh: ", COLOR_GRAY)
        .insert(msg(secondsAgo + " seconds ago", COLOR_WHITE)));
    } else {
      ctx.sendMessage(msg("  Last Refresh: ", COLOR_GRAY)
        .insert(msg("Never", COLOR_GRAY)));
    }
  }

  // === Admin Decay Commands ===

  /** Handles admin decay. */
  public void handleAdminDecay(CommandContext ctx, @Nullable PlayerRef player, String[] args) {
    if (!hasPermission(player, Permissions.ADMIN)) {
      ctx.sendMessage(prefix().insert(msg(HFMessages.get(player, AdminKeys.AdminCmd.NO_PERMISSION), COLOR_RED)));
      return;
    }

    if (args.length == 0) {
      showDecayStatus(ctx);
      return;
    }

    String subCmd = args[0].toLowerCase();

    switch (subCmd) {
      case "run", "trigger" -> handleDecayRun(ctx);
      case "check" -> handleDecayCheck(ctx, Arrays.copyOfRange(args, 1, args.length));
      case "status" -> showDecayStatus(ctx);
      case "help", "?" -> showDecayHelp(ctx, player);
      default -> {
        ctx.sendMessage(prefix().insert(msg(HFMessages.get(player, AdminKeys.AdminCmd.DECAY_UNKNOWN_CMD), COLOR_RED)));
        showDecayHelp(ctx, player);
      }
    }
  }

  private void showDecayHelp(CommandContext ctx, @Nullable PlayerRef player) {
    List<CommandHelp> commands = new ArrayList<>();
    commands.add(new CommandHelp("/f admin decay", HelpKeys.Help.DECAY_CMD_STATUS));
    commands.add(new CommandHelp("/f admin decay run", HelpKeys.Help.DECAY_CMD_RUN));
    commands.add(new CommandHelp("/f admin decay check <faction>", HelpKeys.Help.DECAY_CMD_CHECK));
    ctx.sendMessage(HelpFormatter.buildHelp(HelpKeys.Help.DECAY_TITLE, HelpKeys.Help.DECAY_DESCRIPTION, commands, null, player));
  }

  private void showDecayStatus(CommandContext ctx) {
    ConfigManager config = ConfigManager.get();

    ctx.sendMessage(msg("=== " + HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.DECAY_STATUS_HEADER) + " ===", COLOR_CYAN).bold(true));
    ctx.sendMessage(msg("Enabled: ", COLOR_GRAY)
      .insert(msg(config.isDecayEnabled() ? "Yes" : "No", config.isDecayEnabled() ? COLOR_GREEN : COLOR_RED)));
    ctx.sendMessage(msg("Inactivity Threshold: ", COLOR_GRAY)
      .insert(msg(config.getDecayDaysInactive() + " days", COLOR_WHITE)));
    ctx.sendMessage(msg("Check Interval: ", COLOR_GRAY)
      .insert(msg("Every hour", COLOR_WHITE)));

    // Count factions at risk
    if (config.isDecayEnabled()) {
      int atRisk = 0;
      int totalWithClaims = 0;
      for (var faction : hyperFactions.getFactionManager().getAllFactions()) {
        if (faction.getClaimCount() > 0) {
          totalWithClaims++;
          int daysUntil = hyperFactions.getClaimManager().getDaysUntilDecay(faction.id());
          if (daysUntil >= 0 && daysUntil <= 7) {
            atRisk++;
          }
        }
      }
      ctx.sendMessage(msg("Factions with claims: ", COLOR_GRAY)
        .insert(msg(String.valueOf(totalWithClaims), COLOR_WHITE)));
      ctx.sendMessage(msg("At risk (<=7 days): ", COLOR_GRAY)
        .insert(msg(String.valueOf(atRisk), atRisk > 0 ? COLOR_YELLOW : COLOR_GREEN)));
    }
  }

  /** Handles decay run. */
  public void handleDecayRun(CommandContext ctx) {
    ConfigManager config = ConfigManager.get();

    if (!config.isDecayEnabled()) {
      ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.DECAY_DISABLED), COLOR_YELLOW)));
      ctx.sendMessage(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.DECAY_ENABLE_HINT), COLOR_GRAY));
      return;
    }

    ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.DECAY_RUNNING), COLOR_YELLOW)));

    // Run decay on separate thread to avoid blocking
    CompletableFuture.runAsync(() -> {
      try {
        hyperFactions.getClaimManager().tickClaimDecay();
        ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.DECAY_COMPLETE, "?"), COLOR_GREEN)));
      } catch (Exception e) {
        ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.DECAY_ERROR, e.getMessage()), COLOR_RED)));
      }
    });
  }

  /** Handles decay check. */
  public void handleDecayCheck(CommandContext ctx, String[] args) {
    if (args.length < 1) {
      ctx.sendMessage(prefix().insert(msg("Usage: /f admin decay check <faction>", COLOR_RED)));
      return;
    }

    String factionName = args[0];
    var faction = hyperFactions.getFactionManager().getFactionByName(factionName);
    if (faction == null) {
      ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.DECAY_CHECK_NOT_FOUND, factionName), COLOR_RED)));
      return;
    }

    ConfigManager config = ConfigManager.get();

    ctx.sendMessage(msg("=== " + HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.DECAY_CHECK_HEADER, faction.name()) + " ===", COLOR_CYAN).bold(true));
    ctx.sendMessage(msg("Claims: ", COLOR_GRAY).insert(msg(String.valueOf(faction.getClaimCount()), COLOR_WHITE)));

    if (faction.getClaimCount() == 0) {
      ctx.sendMessage(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.DECAY_NO_CLAIMS), COLOR_GRAY));
      return;
    }

    if (!config.isDecayEnabled()) {
      ctx.sendMessage(msg("Decay Status: ", COLOR_GRAY).insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.DECAY_DISABLED_GLOBALLY), COLOR_YELLOW)));
      return;
    }

    // Find most recent login
    long mostRecentLogin = 0;
    String mostRecentPlayer = "Unknown";
    for (var member : faction.members().values()) {
      if (member.lastOnline() > mostRecentLogin) {
        mostRecentLogin = member.lastOnline();
        mostRecentPlayer = member.username();
      }
    }

    long daysSinceActive = (System.currentTimeMillis() - mostRecentLogin) / (24L * 60 * 60 * 1000);
    int daysUntilDecay = hyperFactions.getClaimManager().getDaysUntilDecay(faction.id());
    boolean isInactive = hyperFactions.getClaimManager().isFactionInactive(faction.id());

    ctx.sendMessage(msg("Last Active: ", COLOR_GRAY)
      .insert(msg(daysSinceActive + " days ago", COLOR_WHITE))
      .insert(msg(" (" + mostRecentPlayer + ")", COLOR_GRAY)));
    ctx.sendMessage(msg("Threshold: ", COLOR_GRAY)
      .insert(msg(config.getDecayDaysInactive() + " days", COLOR_WHITE)));

    if (isInactive) {
      ctx.sendMessage(msg("Status: ", COLOR_GRAY)
        .insert(msg("INACTIVE - Claims will decay on next check!", COLOR_RED).bold(true)));
    } else if (daysUntilDecay <= 7) {
      ctx.sendMessage(msg("Status: ", COLOR_GRAY)
        .insert(msg("AT RISK - " + daysUntilDecay + " days until decay", COLOR_YELLOW)));
    } else {
      ctx.sendMessage(msg("Status: ", COLOR_GRAY)
        .insert(msg("Active - " + daysUntilDecay + " days until decay", COLOR_GREEN)));
    }
  }
}
