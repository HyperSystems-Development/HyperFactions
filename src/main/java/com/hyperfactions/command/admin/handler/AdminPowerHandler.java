package com.hyperfactions.command.admin.handler;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionLog;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.data.MembershipRecord;
import com.hyperfactions.data.PlayerData;
import com.hyperfactions.data.PlayerPower;
import com.hyperfactions.platform.HyperFactionsPlugin;
import com.hyperfactions.util.CommandHelp;
import com.hyperfactions.util.HelpFormatter;
import com.hyperfactions.util.MessageKeys;
import com.hyperfactions.util.PlayerResolver;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * Handles /f admin power commands and /f admin clearhistory.
 */
public class AdminPowerHandler {

  private final HyperFactions hyperFactions;

  private final HyperFactionsPlugin plugin;

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

  /** Creates a new AdminPowerHandler. */
  public AdminPowerHandler(HyperFactions hyperFactions, HyperFactionsPlugin plugin) {
    this.hyperFactions = hyperFactions;
    this.plugin = plugin;
  }

  /** Handles admin power. */
  public void handleAdminPower(CommandContext ctx, @Nullable PlayerRef player, UUID senderUuid, String[] args) {
    if (!hasPermission(player, Permissions.ADMIN_POWER)) {
      ctx.sendMessage(prefix().insert(msg("You don't have permission.", COLOR_RED)));
      return;
    }

    if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
      showAdminPowerHelp(ctx);
      return;
    }

    String subCmd = args[0].toLowerCase();

    switch (subCmd) {
      case "set" -> handlePowerSet(ctx, senderUuid, args);
      case "add" -> handlePowerAdd(ctx, senderUuid, args);
      case "remove" -> handlePowerRemove(ctx, senderUuid, args);
      case "reset" -> handlePowerReset(ctx, senderUuid, args);
      case "setmax" -> handlePowerSetMax(ctx, senderUuid, args);
      case "resetmax" -> handlePowerResetMax(ctx, senderUuid, args);
      case "noloss" -> handlePowerNoLoss(ctx, senderUuid, args);
      case "nodecay" -> handlePowerNoDecay(ctx, senderUuid, args);
      case "faction" -> handlePowerFaction(ctx, senderUuid, args);
      case "info" -> handlePowerInfo(ctx, args);
      default -> ctx.sendMessage(prefix().insert(msg("Unknown power command. Use /f admin power help", COLOR_RED)));
    }
  }

  private void showAdminPowerHelp(CommandContext ctx) {
    List<CommandHelp> commands = new ArrayList<>();
    commands.add(new CommandHelp("/f admin power set <player> <amount>", "Set exact power"));
    commands.add(new CommandHelp("/f admin power add <player> <amount>", "Increase power"));
    commands.add(new CommandHelp("/f admin power remove <player> <amount>", "Decrease power"));
    commands.add(new CommandHelp("/f admin power reset <player>", "Reset to default"));
    commands.add(new CommandHelp("/f admin power setmax <player> <amount>", "Set max power override"));
    commands.add(new CommandHelp("/f admin power resetmax <player>", "Clear max override"));
    commands.add(new CommandHelp("/f admin power noloss <player>", "Toggle power loss bypass"));
    commands.add(new CommandHelp("/f admin power nodecay <player>", "Toggle claim decay exemption"));
    commands.add(new CommandHelp("/f admin power faction <name> <action>", "Faction-wide operations"));
    commands.add(new CommandHelp("/f admin power info <player>", "Show player power details"));
    ctx.sendMessage(HelpFormatter.buildHelp("Admin Power", "Manage player/faction power", commands, null));
  }

  /**
   * Resolves a player UUID from their username using centralized resolution.
   * Chain: online players -> faction members -> PlayerDB API.
   */
  private record ResolvedPlayer(UUID uuid, String name) {}

  @Nullable
  private ResolvedPlayer resolvePlayer(String targetName) {
    var resolved = PlayerResolver.resolve(hyperFactions, targetName);
    return resolved != null ? new ResolvedPlayer(resolved.uuid(), resolved.username()) : null;
  }

  /**
   * Logs an admin power change to the player's faction activity log.
   */
  private void logAdminPowerChange(UUID targetUuid, UUID adminUuid, String message) {
    Faction faction = hyperFactions.getFactionManager().getPlayerFaction(targetUuid);
    if (faction != null) {
      Faction updated = faction.withLog(FactionLog.create(FactionLog.LogType.ADMIN_POWER, message, adminUuid));
      hyperFactions.getFactionManager().updateFaction(updated);
    }
  }

  private void logAdminPowerChange(UUID targetUuid, UUID adminUuid, String message, String key, String... args) {
    Faction faction = hyperFactions.getFactionManager().getPlayerFaction(targetUuid);
    if (faction != null) {
      Faction updated = faction.withLog(FactionLog.create(FactionLog.LogType.ADMIN_POWER, message, adminUuid, key, args));
      hyperFactions.getFactionManager().updateFaction(updated);
    }
  }

  // /f admin power set <player> <amount>
  /** Handles power set. */
  public void handlePowerSet(CommandContext ctx, UUID senderUuid, String[] args) {
    if (args.length < 3) {
      ctx.sendMessage(prefix().insert(msg("Usage: /f admin power set <player> <amount>", COLOR_YELLOW)));
      return;
    }
    ResolvedPlayer target = resolvePlayer(args[1]);
    if (target == null) {
      ctx.sendMessage(prefix().insert(msg("Player not found: " + args[1], COLOR_RED)));
      return;
    }
    double amount;
    try {
      amount = Double.parseDouble(args[2]);
    } catch (NumberFormatException e) {
      ctx.sendMessage(prefix().insert(msg("Invalid number: " + args[2], COLOR_RED)));
      return;
    }

    double oldPower = hyperFactions.getPowerManager().getPlayerPower(target.uuid()).power();
    double newPower = hyperFactions.getPowerManager().setPlayerPower(target.uuid(), amount);
    logAdminPowerChange(target.uuid(), senderUuid,
      "Admin set " + target.name() + "'s power to " + String.format("%.1f", newPower) + " (was " + String.format("%.1f", oldPower) + ")",
      MessageKeys.LogsGui.MSG_ADMIN_POWER_SET, target.name(), String.format("%.1f", newPower), String.format("%.1f", oldPower));
    ctx.sendMessage(prefix().insert(msg("Set ", COLOR_GREEN))
      .insert(msg(target.name(), COLOR_CYAN))
      .insert(msg("'s power to ", COLOR_GREEN))
      .insert(msg(String.format("%.1f", newPower), COLOR_WHITE))
      .insert(msg(" (was " + String.format("%.1f", oldPower) + ")", COLOR_GRAY)));
  }

  // /f admin power add <player> <amount>
  /** Handles power add. */
  public void handlePowerAdd(CommandContext ctx, UUID senderUuid, String[] args) {
    if (args.length < 3) {
      ctx.sendMessage(prefix().insert(msg("Usage: /f admin power add <player> <amount>", COLOR_YELLOW)));
      return;
    }
    ResolvedPlayer target = resolvePlayer(args[1]);
    if (target == null) {
      ctx.sendMessage(prefix().insert(msg("Player not found: " + args[1], COLOR_RED)));
      return;
    }
    double amount;
    try {
      amount = Double.parseDouble(args[2]);
    } catch (NumberFormatException e) {
      ctx.sendMessage(prefix().insert(msg("Invalid number: " + args[2], COLOR_RED)));
      return;
    }

    double oldPower = hyperFactions.getPowerManager().getPlayerPower(target.uuid()).power();
    double newPower = hyperFactions.getPowerManager().adjustPlayerPower(target.uuid(), amount);
    logAdminPowerChange(target.uuid(), senderUuid,
      "Admin added " + String.format("%.1f", amount) + " power to " + target.name() + " (" + String.format("%.1f", oldPower) + " -> " + String.format("%.1f", newPower) + ")",
      MessageKeys.LogsGui.MSG_ADMIN_POWER_ADD, String.format("%.1f", amount), target.name(), String.format("%.1f", oldPower), String.format("%.1f", newPower));
    ctx.sendMessage(prefix().insert(msg("Added ", COLOR_GREEN))
      .insert(msg(String.format("%.1f", amount), COLOR_WHITE))
      .insert(msg(" power to ", COLOR_GREEN))
      .insert(msg(target.name(), COLOR_CYAN))
      .insert(msg(" (" + String.format("%.1f", oldPower) + " -> " + String.format("%.1f", newPower) + ")", COLOR_GRAY)));
  }

  // /f admin power remove <player> <amount>
  /** Handles power remove. */
  public void handlePowerRemove(CommandContext ctx, UUID senderUuid, String[] args) {
    if (args.length < 3) {
      ctx.sendMessage(prefix().insert(msg("Usage: /f admin power remove <player> <amount>", COLOR_YELLOW)));
      return;
    }
    ResolvedPlayer target = resolvePlayer(args[1]);
    if (target == null) {
      ctx.sendMessage(prefix().insert(msg("Player not found: " + args[1], COLOR_RED)));
      return;
    }
    double amount;
    try {
      amount = Double.parseDouble(args[2]);
    } catch (NumberFormatException e) {
      ctx.sendMessage(prefix().insert(msg("Invalid number: " + args[2], COLOR_RED)));
      return;
    }

    double oldPower = hyperFactions.getPowerManager().getPlayerPower(target.uuid()).power();
    double newPower = hyperFactions.getPowerManager().adjustPlayerPower(target.uuid(), -amount);
    logAdminPowerChange(target.uuid(), senderUuid,
      "Admin removed " + String.format("%.1f", amount) + " power from " + target.name() + " (" + String.format("%.1f", oldPower) + " -> " + String.format("%.1f", newPower) + ")",
      MessageKeys.LogsGui.MSG_ADMIN_POWER_REMOVE, String.format("%.1f", amount), target.name(), String.format("%.1f", oldPower), String.format("%.1f", newPower));
    ctx.sendMessage(prefix().insert(msg("Removed ", COLOR_GREEN))
      .insert(msg(String.format("%.1f", amount), COLOR_WHITE))
      .insert(msg(" power from ", COLOR_GREEN))
      .insert(msg(target.name(), COLOR_CYAN))
      .insert(msg(" (" + String.format("%.1f", oldPower) + " -> " + String.format("%.1f", newPower) + ")", COLOR_GRAY)));
  }

  // /f admin power reset <player>
  /** Handles power reset. */
  public void handlePowerReset(CommandContext ctx, UUID senderUuid, String[] args) {
    if (args.length < 2) {
      ctx.sendMessage(prefix().insert(msg("Usage: /f admin power reset <player>", COLOR_YELLOW)));
      return;
    }
    ResolvedPlayer target = resolvePlayer(args[1]);
    if (target == null) {
      ctx.sendMessage(prefix().insert(msg("Player not found: " + args[1], COLOR_RED)));
      return;
    }

    double oldPower = hyperFactions.getPowerManager().getPlayerPower(target.uuid()).power();
    double newPower = hyperFactions.getPowerManager().resetPlayerPower(target.uuid());
    logAdminPowerChange(target.uuid(), senderUuid,
      "Admin reset " + target.name() + "'s power to " + String.format("%.1f", newPower) + " (was " + String.format("%.1f", oldPower) + ")",
      MessageKeys.LogsGui.MSG_ADMIN_POWER_RESET, target.name(), String.format("%.1f", newPower), String.format("%.1f", oldPower));
    ctx.sendMessage(prefix().insert(msg("Reset ", COLOR_GREEN))
      .insert(msg(target.name(), COLOR_CYAN))
      .insert(msg("'s power to ", COLOR_GREEN))
      .insert(msg(String.format("%.1f", newPower), COLOR_WHITE))
      .insert(msg(" (was " + String.format("%.1f", oldPower) + ")", COLOR_GRAY)));
  }

  // /f admin power setmax <player> <amount>
  /** Handles power set max. */
  public void handlePowerSetMax(CommandContext ctx, UUID senderUuid, String[] args) {
    if (args.length < 3) {
      ctx.sendMessage(prefix().insert(msg("Usage: /f admin power setmax <player> <amount>", COLOR_YELLOW)));
      return;
    }
    ResolvedPlayer target = resolvePlayer(args[1]);
    if (target == null) {
      ctx.sendMessage(prefix().insert(msg("Player not found: " + args[1], COLOR_RED)));
      return;
    }
    double amount;
    try {
      amount = Double.parseDouble(args[2]);
      if (amount <= 0) {
        ctx.sendMessage(prefix().insert(msg("Max power must be positive.", COLOR_RED)));
        return;
      }
    } catch (NumberFormatException e) {
      ctx.sendMessage(prefix().insert(msg("Invalid number: " + args[2], COLOR_RED)));
      return;
    }

    PlayerPower oldPower = hyperFactions.getPowerManager().getPlayerPower(target.uuid());
    double oldMax = oldPower.getEffectiveMaxPower();
    double newCurrentPower = hyperFactions.getPowerManager().setPlayerMaxPower(target.uuid(), amount);
    logAdminPowerChange(target.uuid(), senderUuid,
      "Admin set " + target.name() + "'s max power to " + String.format("%.1f", amount) + " (was " + String.format("%.1f", oldMax) + ")",
      MessageKeys.LogsGui.MSG_ADMIN_MAXPOWER_SET, target.name(), String.format("%.1f", amount), String.format("%.1f", oldMax));
    ctx.sendMessage(prefix().insert(msg("Set ", COLOR_GREEN))
      .insert(msg(target.name(), COLOR_CYAN))
      .insert(msg("'s max power to ", COLOR_GREEN))
      .insert(msg(String.format("%.1f", amount), COLOR_WHITE))
      .insert(msg(" (was " + String.format("%.1f", oldMax) + ", power now " + String.format("%.1f", newCurrentPower) + ")", COLOR_GRAY)));
  }

  // /f admin power resetmax <player>
  /** Handles power reset max. */
  public void handlePowerResetMax(CommandContext ctx, UUID senderUuid, String[] args) {
    if (args.length < 2) {
      ctx.sendMessage(prefix().insert(msg("Usage: /f admin power resetmax <player>", COLOR_YELLOW)));
      return;
    }
    ResolvedPlayer target = resolvePlayer(args[1]);
    if (target == null) {
      ctx.sendMessage(prefix().insert(msg("Player not found: " + args[1], COLOR_RED)));
      return;
    }

    PlayerPower oldPower = hyperFactions.getPowerManager().getPlayerPower(target.uuid());
    double oldMax = oldPower.getEffectiveMaxPower();
    hyperFactions.getPowerManager().resetPlayerMaxPower(target.uuid());
    double globalMax = ConfigManager.get().getMaxPlayerPower();
    logAdminPowerChange(target.uuid(), senderUuid,
      "Admin reset " + target.name() + "'s max power to global default (" + String.format("%.1f", globalMax) + ")",
      MessageKeys.LogsGui.MSG_ADMIN_MAXPOWER_RESET, target.name(), String.format("%.1f", globalMax));
    ctx.sendMessage(prefix().insert(msg("Reset ", COLOR_GREEN))
      .insert(msg(target.name(), COLOR_CYAN))
      .insert(msg("'s max power to global default ", COLOR_GREEN))
      .insert(msg(String.format("%.1f", globalMax), COLOR_WHITE))
      .insert(msg(" (was " + String.format("%.1f", oldMax) + ")", COLOR_GRAY)));
  }

  // /f admin power noloss <player>
  /** Handles power no loss. */
  public void handlePowerNoLoss(CommandContext ctx, UUID senderUuid, String[] args) {
    if (args.length < 2) {
      ctx.sendMessage(prefix().insert(msg("Usage: /f admin power noloss <player>", COLOR_YELLOW)));
      return;
    }
    ResolvedPlayer target = resolvePlayer(args[1]);
    if (target == null) {
      ctx.sendMessage(prefix().insert(msg("Player not found: " + args[1], COLOR_RED)));
      return;
    }

    PlayerPower current = hyperFactions.getPowerManager().getPlayerPower(target.uuid());
    boolean newState = !current.powerLossDisabled();
    hyperFactions.getPowerManager().setPlayerPowerLossDisabled(target.uuid(), newState);
    logAdminPowerChange(target.uuid(), senderUuid,
      "Admin " + (newState ? "disabled" : "enabled") + " power loss for " + target.name(),
      newState ? MessageKeys.LogsGui.MSG_ADMIN_POWERLOSS_DISABLED : MessageKeys.LogsGui.MSG_ADMIN_POWERLOSS_ENABLED, target.name());
    ctx.sendMessage(prefix().insert(msg("Power loss ", COLOR_GREEN))
      .insert(msg(newState ? "disabled" : "enabled", newState ? COLOR_RED : COLOR_GREEN))
      .insert(msg(" for ", COLOR_GREEN))
      .insert(msg(target.name(), COLOR_CYAN)));
  }

  // /f admin power nodecay <player>
  /** Handles power no decay. */
  public void handlePowerNoDecay(CommandContext ctx, UUID senderUuid, String[] args) {
    if (args.length < 2) {
      ctx.sendMessage(prefix().insert(msg("Usage: /f admin power nodecay <player>", COLOR_YELLOW)));
      return;
    }
    ResolvedPlayer target = resolvePlayer(args[1]);
    if (target == null) {
      ctx.sendMessage(prefix().insert(msg("Player not found: " + args[1], COLOR_RED)));
      return;
    }

    PlayerPower current = hyperFactions.getPowerManager().getPlayerPower(target.uuid());
    boolean newState = !current.claimDecayExempt();
    hyperFactions.getPowerManager().setPlayerClaimDecayExempt(target.uuid(), newState);
    logAdminPowerChange(target.uuid(), senderUuid,
      "Admin " + (newState ? "enabled" : "disabled") + " claim decay exemption for " + target.name(),
      newState ? MessageKeys.LogsGui.MSG_ADMIN_DECAY_ENABLED : MessageKeys.LogsGui.MSG_ADMIN_DECAY_DISABLED, target.name());
    ctx.sendMessage(prefix().insert(msg("Claim decay exemption ", COLOR_GREEN))
      .insert(msg(newState ? "enabled" : "disabled", newState ? COLOR_GREEN : COLOR_RED))
      .insert(msg(" for ", COLOR_GREEN))
      .insert(msg(target.name(), COLOR_CYAN)));
  }

  // /f admin power faction <name> set|add|remove|reset <amount>
  /** Handles power faction. */
  public void handlePowerFaction(CommandContext ctx, UUID senderUuid, String[] args) {
    if (args.length < 3) {
      ctx.sendMessage(prefix().insert(msg("Usage: /f admin power faction <name> set|add|remove|reset [amount]", COLOR_YELLOW)));
      return;
    }
    String factionName = args[1];
    Faction faction = hyperFactions.getFactionManager().getFactionByName(factionName);
    if (faction == null) {
      ctx.sendMessage(prefix().insert(msg("Faction not found: " + factionName, COLOR_RED)));
      return;
    }

    String action = args[2].toLowerCase();
    Set<UUID> members = faction.members().keySet();

    switch (action) {
      case "set" -> {
        if (args.length < 4) {
          ctx.sendMessage(prefix().insert(msg("Usage: /f admin power faction <name> set <amount>", COLOR_YELLOW)));
          return;
        }
        double amount = parseDouble(ctx, args[3]);
        if (Double.isNaN(amount)) {
          return;
        }
        for (UUID memberUuid : members) {
          hyperFactions.getPowerManager().setPlayerPower(memberUuid, amount);
        }
        hyperFactions.getFactionManager().updateFaction(faction.withLog(FactionLog.create(
          FactionLog.LogType.ADMIN_POWER,
          "Admin set all " + members.size() + " members' power to " + String.format("%.1f", amount),
          senderUuid,
          MessageKeys.LogsGui.MSG_ADMIN_POWER_SET_ALL, String.valueOf(members.size()), String.format("%.1f", amount))));
        ctx.sendMessage(prefix().insert(msg("Set power to ", COLOR_GREEN))
          .insert(msg(String.format("%.1f", amount), COLOR_WHITE))
          .insert(msg(" for " + members.size() + " members of ", COLOR_GREEN))
          .insert(msg(faction.name(), COLOR_CYAN)));
      }
      case "add" -> {
        if (args.length < 4) {
          ctx.sendMessage(prefix().insert(msg("Usage: /f admin power faction <name> add <amount>", COLOR_YELLOW)));
          return;
        }
        double amount = parseDouble(ctx, args[3]);
        if (Double.isNaN(amount)) {
          return;
        }
        for (UUID memberUuid : members) {
          hyperFactions.getPowerManager().adjustPlayerPower(memberUuid, amount);
        }
        hyperFactions.getFactionManager().updateFaction(faction.withLog(FactionLog.create(
          FactionLog.LogType.ADMIN_POWER,
          "Admin added " + String.format("%.1f", amount) + " power to all " + members.size() + " members",
          senderUuid,
          MessageKeys.LogsGui.MSG_ADMIN_POWER_ADD_ALL, String.format("%.1f", amount), String.valueOf(members.size()))));
        ctx.sendMessage(prefix().insert(msg("Added ", COLOR_GREEN))
          .insert(msg(String.format("%.1f", amount), COLOR_WHITE))
          .insert(msg(" power to " + members.size() + " members of ", COLOR_GREEN))
          .insert(msg(faction.name(), COLOR_CYAN)));
      }
      case "remove" -> {
        if (args.length < 4) {
          ctx.sendMessage(prefix().insert(msg("Usage: /f admin power faction <name> remove <amount>", COLOR_YELLOW)));
          return;
        }
        double amount = parseDouble(ctx, args[3]);
        if (Double.isNaN(amount)) {
          return;
        }
        for (UUID memberUuid : members) {
          hyperFactions.getPowerManager().adjustPlayerPower(memberUuid, -amount);
        }
        hyperFactions.getFactionManager().updateFaction(faction.withLog(FactionLog.create(
          FactionLog.LogType.ADMIN_POWER,
          "Admin removed " + String.format("%.1f", amount) + " power from all " + members.size() + " members",
          senderUuid,
          MessageKeys.LogsGui.MSG_ADMIN_POWER_REMOVE_ALL, String.format("%.1f", amount), String.valueOf(members.size()))));
        ctx.sendMessage(prefix().insert(msg("Removed ", COLOR_GREEN))
          .insert(msg(String.format("%.1f", amount), COLOR_WHITE))
          .insert(msg(" power from " + members.size() + " members of ", COLOR_GREEN))
          .insert(msg(faction.name(), COLOR_CYAN)));
      }
      case "reset" -> {
        for (UUID memberUuid : members) {
          hyperFactions.getPowerManager().resetPlayerPower(memberUuid);
        }
        hyperFactions.getFactionManager().updateFaction(faction.withLog(FactionLog.create(
          FactionLog.LogType.ADMIN_POWER,
          "Admin reset power for all " + members.size() + " members",
          senderUuid,
          MessageKeys.LogsGui.MSG_ADMIN_POWER_RESET_ALL, String.valueOf(members.size()))));
        ctx.sendMessage(prefix().insert(msg("Reset power for ", COLOR_GREEN))
          .insert(msg(String.valueOf(members.size()), COLOR_WHITE))
          .insert(msg(" members of ", COLOR_GREEN))
          .insert(msg(faction.name(), COLOR_CYAN)));
      }
      default -> ctx.sendMessage(prefix().insert(msg("Unknown faction power action. Use: set, add, remove, reset", COLOR_RED)));
    }
  }

  // /f admin power info <player>
  /** Handles power info. */
  public void handlePowerInfo(CommandContext ctx, String[] args) {
    if (args.length < 2) {
      ctx.sendMessage(prefix().insert(msg("Usage: /f admin power info <player>", COLOR_YELLOW)));
      return;
    }
    ResolvedPlayer target = resolvePlayer(args[1]);
    if (target == null) {
      ctx.sendMessage(prefix().insert(msg("Player not found: " + args[1], COLOR_RED)));
      return;
    }

    PlayerPower power = hyperFactions.getPowerManager().getPlayerPower(target.uuid());
    ctx.sendMessage(prefix().insert(msg("Power Info: ", COLOR_CYAN)).insert(msg(target.name(), COLOR_WHITE)));
    ctx.sendMessage(msg("  Power: ", COLOR_GRAY)
      .insert(msg(String.format("%.1f / %.1f (%d%%)", power.power(), power.getEffectiveMaxPower(), power.getPowerPercent()), COLOR_WHITE)));
    ctx.sendMessage(msg("  Base Max: ", COLOR_GRAY)
      .insert(msg(String.format("%.1f", power.maxPower()), COLOR_WHITE)));
    if (power.maxPowerOverride() != null) {
      ctx.sendMessage(msg("  Max Override: ", COLOR_GRAY)
        .insert(msg(String.format("%.1f", power.maxPowerOverride()), COLOR_YELLOW)));
    } else {
      ctx.sendMessage(msg("  Max Override: ", COLOR_GRAY)
        .insert(msg("None (using global)", COLOR_GRAY)));
    }
    ctx.sendMessage(msg("  Power Loss: ", COLOR_GRAY)
      .insert(msg(power.powerLossDisabled() ? "DISABLED (immune)" : "Enabled", power.powerLossDisabled() ? COLOR_RED : COLOR_GREEN)));
    ctx.sendMessage(msg("  Claim Decay: ", COLOR_GRAY)
      .insert(msg(power.claimDecayExempt() ? "EXEMPT (always online)" : "Normal", power.claimDecayExempt() ? COLOR_YELLOW : COLOR_GREEN)));

    Faction faction = hyperFactions.getFactionManager().getPlayerFaction(target.uuid());
    if (faction != null) {
      ctx.sendMessage(msg("  Faction: ", COLOR_GRAY).insert(msg(faction.name(), COLOR_CYAN)));
    } else {
      ctx.sendMessage(msg("  Faction: ", COLOR_GRAY).insert(msg("None", COLOR_GRAY)));
    }
  }

  /** Handles clear history. */
  public void handleClearHistory(CommandContext ctx, @Nullable PlayerRef player, String[] args) {
    if (!hasPermission(player, Permissions.ADMIN)) {
      ctx.sendMessage(prefix().insert(msg("You don't have permission.", COLOR_RED)));
      return;
    }

    if (args.length < 1) {
      ctx.sendMessage(prefix().insert(msg("Usage: /f admin clearhistory <player>", COLOR_YELLOW)));
      return;
    }

    String targetName = args[0];

    // Resolve player using centralized resolver (online -> faction members -> PlayerDB)
    var resolved = PlayerResolver.resolve(hyperFactions, targetName);
    if (resolved == null) {
      ctx.sendMessage(prefix().insert(msg("Player not found.", COLOR_RED)));
      return;
    }

    UUID targetUuid = resolved.uuid();
    String resolvedName = resolved.username();

    final String finalName = resolvedName;
    hyperFactions.getPlayerStorage().loadPlayerData(targetUuid).thenAccept(opt -> {
      if (opt.isEmpty()) {
        ctx.sendMessage(prefix().insert(msg("No player data found for " + finalName + ".", COLOR_RED)));
        return;
      }

      PlayerData data = opt.get();
      int count = data.getMembershipHistory().size();

      if (count == 0) {
        ctx.sendMessage(prefix().insert(msg(finalName + " has no membership history.", COLOR_YELLOW)));
        return;
      }

      data.clearHistory();

      // Re-initialize with current faction if the player is in one
      Faction currentFaction = hyperFactions.getFactionManager().getPlayerFaction(data.getUuid());
      if (currentFaction != null) {
        FactionMember member = currentFaction.getMember(data.getUuid());
        if (member != null) {
          MembershipRecord activeRecord = new MembershipRecord(
            currentFaction.id(), currentFaction.name(), currentFaction.tag(),
            member.role(), member.joinedAt(), 0, MembershipRecord.LeaveReason.ACTIVE
          );
          data.addRecord(activeRecord, ConfigManager.get().getMaxMembershipHistory());
        }
      }

      hyperFactions.getPlayerStorage().savePlayerData(data).thenRun(() -> {
        if (currentFaction != null) {
          ctx.sendMessage(prefix().insert(msg("Cleared " + count + " history records for " + finalName
            + " (re-initialized with current faction: " + currentFaction.name() + ").", COLOR_GREEN)));
        } else {
          ctx.sendMessage(prefix().insert(msg("Cleared " + count + " history records for " + finalName + ".", COLOR_GREEN)));
        }
      });
    });
  }

  /**
   * Parses a double from string, sending error message if invalid.
   * Returns NaN on failure.
   */
  private double parseDouble(CommandContext ctx, String value) {
    try {
      return Double.parseDouble(value);
    } catch (NumberFormatException e) {
      ctx.sendMessage(prefix().insert(msg("Invalid number: " + value, COLOR_RED)));
      return Double.NaN;
    }
  }
}
