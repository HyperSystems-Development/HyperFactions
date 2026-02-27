package com.hyperfactions.command.admin.handler;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.config.WorldSettingsResolver;
import com.hyperfactions.config.modules.WorldsConfig.WorldSettings;
import com.hyperfactions.config.modules.WorldsConfig;
import com.hyperfactions.integration.PermissionManager;
import com.hyperfactions.util.CommandHelp;
import com.hyperfactions.util.HelpFormatter;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.*;
import org.jetbrains.annotations.Nullable;

/**
 * Handler for {@code /f admin world} commands.
 *
 * <p>
 * Provides CLI-based management of per-world settings.
 */
public class AdminWorldHandler {

  private final HyperFactions hyperFactions;

  private static final String COLOR_CYAN = CommandUtil.COLOR_CYAN;

  private static final String COLOR_GREEN = CommandUtil.COLOR_GREEN;

  private static final String COLOR_RED = CommandUtil.COLOR_RED;

  private static final String COLOR_YELLOW = CommandUtil.COLOR_YELLOW;

  private static final String COLOR_GRAY = CommandUtil.COLOR_GRAY;

  private static final String COLOR_WHITE = CommandUtil.COLOR_WHITE;

  private Message prefix() {
    return CommandUtil.prefix();
  }

  private Message msg(String text, String color) {
    return CommandUtil.msg(text, color);
  }

  private boolean hasPermission(@Nullable PlayerRef player, String permission) {
    if (player == null) { // Console
      return true;
    }
    return PermissionManager.get().hasPermission(player.getUuid(), permission);
  }

  /** Creates a new AdminWorldHandler. */
  public AdminWorldHandler(HyperFactions hyperFactions) {
    this.hyperFactions = hyperFactions;
  }

  /**
   * Entry point: /f admin world [subcommand] [args...].
   */
  public void handleAdminWorld(CommandContext ctx, @Nullable PlayerRef player, String[] args) {
    if (!hasPermission(player, Permissions.ADMIN)) {
      ctx.sendMessage(prefix().insert(msg("You don't have permission.", COLOR_RED)));
      return;
    }

    if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
      showWorldHelp(ctx);
      return;
    }

    String subCmd = args[0].toLowerCase();
    String[] subArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];

    switch (subCmd) {
      case "list" -> handleList(ctx);
      case "info" -> handleInfo(ctx, subArgs);
      case "set" -> handleSet(ctx, subArgs);
      case "reset", "remove" -> handleReset(ctx, subArgs);
      default -> ctx.sendMessage(prefix().insert(msg("Unknown world command. Use /f admin world help", COLOR_RED)));
    }
  }

  private void showWorldHelp(CommandContext ctx) {
    List<CommandHelp> commands = new ArrayList<>();
    commands.add(new CommandHelp("/f admin world list", "List all configured worlds"));
    commands.add(new CommandHelp("/f admin world info <world>", "Show settings for a world"));
    commands.add(new CommandHelp("/f admin world set <world> <setting> <value>", "Set a world setting"));
    commands.add(new CommandHelp("/f admin world reset <world>", "Remove world-specific settings"));
    ctx.sendMessage(HelpFormatter.buildHelp("World Settings", "Per-world configuration", commands, null));
  }

  /**
   * /f admin world list — show all configured worlds and their settings.
   */
  private void handleList(CommandContext ctx) {
    WorldsConfig config = ConfigManager.get().worlds();

    var builder = prefix().insert(msg("Per-World Settings", COLOR_CYAN))
        .insert(msg(" (default: " + config.getDefaultPolicy() + ")", COLOR_GRAY));
    ctx.sendMessage(builder);

    if (config.getWorlds().isEmpty()) {
      ctx.sendMessage(msg("  No per-world settings configured.", COLOR_GRAY));
      return;
    }

    for (Map.Entry<String, WorldSettings> entry : config.getWorlds().entrySet()) {
      String key = entry.getKey();
      WorldSettings settings = entry.getValue();

      var line = msg("  " + key + ": ", COLOR_WHITE);

      List<String> parts = new ArrayList<>();
      if (settings.claiming() != null) {
        parts.add("claiming=" + boolStr(settings.claiming()));
      }
      if (settings.powerLoss() != null) {
        parts.add("powerLoss=" + boolStr(settings.powerLoss()));
      }
      if (settings.friendlyFireFaction() != null) {
        parts.add("ffFaction=" + boolStr(settings.friendlyFireFaction()));
      }
      if (settings.friendlyFireAlly() != null) {
        parts.add("ffAlly=" + boolStr(settings.friendlyFireAlly()));
      }

      if (parts.isEmpty()) {
        line = line.insert(msg("(no overrides)", COLOR_GRAY));
      } else {
        line = line.insert(msg(String.join(", ", parts), COLOR_YELLOW));
      }

      ctx.sendMessage(line);
    }

    if (!config.getClaimBlacklist().isEmpty()) {
      ctx.sendMessage(msg("  Claim blacklist: " + String.join(", ", config.getClaimBlacklist()), COLOR_GRAY));
    }
  }

  /**
   * /f admin world info {@code <world>} — resolve effective settings for a world.
   */
  private void handleInfo(CommandContext ctx, String[] args) {
    if (args.length == 0) {
      ctx.sendMessage(prefix().insert(msg("Usage: /f admin world info <worldName>", COLOR_RED)));
      return;
    }

    String worldName = args[0];
    ConfigManager config = ConfigManager.get();
    WorldSettingsResolver resolver = config.getWorldSettingsResolver();
    WorldSettings resolved = resolver.resolve(worldName);

    ctx.sendMessage(prefix().insert(msg("World: ", COLOR_CYAN)).insert(msg(worldName, COLOR_WHITE)));

    boolean claimAllowed = resolver.isClaimingAllowed(worldName);
    boolean powerLoss = resolver.isPowerLossEnabled(worldName);
    Boolean ffFaction = resolver.isFriendlyFireFactionAllowed(worldName);
    Boolean ffAlly = resolver.isFriendlyFireAllyAllowed(worldName);

    ctx.sendMessage(msg("  Claiming: " + boolStr(claimAllowed), COLOR_WHITE));
    ctx.sendMessage(msg("  Power loss: " + boolStr(powerLoss), COLOR_WHITE));
    ctx.sendMessage(msg("  Faction FF: " + (ffFaction != null ? boolStr(ffFaction) : "global (" + boolStr(config.isFactionDamage()) + ")"), COLOR_WHITE));
    ctx.sendMessage(msg("  Ally FF: " + (ffAlly != null ? boolStr(ffAlly) : "global (" + boolStr(config.isAllyDamage()) + ")"), COLOR_WHITE));

    if (resolved != null) {
      ctx.sendMessage(msg("  Source: per-world override", COLOR_GRAY));
    } else {
      ctx.sendMessage(msg("  Source: default policy (" + config.worlds().getDefaultPolicy() + ")", COLOR_GRAY));
    }
  }

  /**
   * /f admin world set {@code <world>} {@code <setting>} {@code <value>}
   * Settings: claiming, powerLoss, friendlyFireFaction, friendlyFireAlly
   */
  private void handleSet(CommandContext ctx, String[] args) {
    if (args.length < 3) {
      ctx.sendMessage(prefix().insert(msg("Usage: /f admin world set <world> <setting> <true|false>", COLOR_RED)));
      ctx.sendMessage(msg("  Settings: claiming, powerLoss, friendlyFireFaction, friendlyFireAlly", COLOR_GRAY));
      return;
    }

    String worldKey = args[0];
    String setting = args[1].toLowerCase();
    String valueStr = args[2].toLowerCase();

    if (!valueStr.equals("true") && !valueStr.equals("false")) {
      ctx.sendMessage(prefix().insert(msg("Value must be 'true' or 'false'.", COLOR_RED)));
      return;
    }
    boolean value = Boolean.parseBoolean(valueStr);

    WorldsConfig config = ConfigManager.get().worlds();
    WorldSettings current = config.getWorldSettings(worldKey);
    if (current == null) {
      current = WorldSettings.DEFAULTS;
    }

    WorldSettings updated = switch (setting) {
      case "claiming" -> new WorldSettings(value, current.powerLoss(), current.friendlyFireFaction(), current.friendlyFireAlly());
      case "powerloss" -> new WorldSettings(current.claiming(), value, current.friendlyFireFaction(), current.friendlyFireAlly());
      case "friendlyfirefaction", "fffaction" -> new WorldSettings(current.claiming(), current.powerLoss(), value, current.friendlyFireAlly());
      case "friendlyfireally", "ffally" -> new WorldSettings(current.claiming(), current.powerLoss(), current.friendlyFireFaction(), value);
      default -> {
        ctx.sendMessage(prefix().insert(msg("Unknown setting: " + setting, COLOR_RED)));
        ctx.sendMessage(msg("  Settings: claiming, powerLoss, friendlyFireFaction, friendlyFireAlly", COLOR_GRAY));
        yield null;
      }
    };

    if (updated == null) {
      return;
    }

    config.setWorldSettings(worldKey, updated);
    config.save();
    ConfigManager.get().getWorldSettingsResolver().rebuild(config);

    ctx.sendMessage(prefix().insert(msg("Set ", COLOR_GREEN))
        .insert(msg(setting, COLOR_CYAN))
        .insert(msg("=" + value + " for world ", COLOR_GREEN))
        .insert(msg(worldKey, COLOR_WHITE)));
  }

  /**
   * /f admin world reset {@code <world>} — remove all per-world settings for a world.
   */
  private void handleReset(CommandContext ctx, String[] args) {
    if (args.length == 0) {
      ctx.sendMessage(prefix().insert(msg("Usage: /f admin world reset <world>", COLOR_RED)));
      return;
    }

    String worldKey = args[0];
    WorldsConfig config = ConfigManager.get().worlds();

    if (!config.removeWorldSettings(worldKey)) {
      ctx.sendMessage(prefix().insert(msg("No settings found for world: " + worldKey, COLOR_YELLOW)));
      return;
    }

    config.save();
    ConfigManager.get().getWorldSettingsResolver().rebuild(config);

    ctx.sendMessage(prefix().insert(msg("Removed per-world settings for: ", COLOR_GREEN))
        .insert(msg(worldKey, COLOR_WHITE)));
  }

  private String boolStr(boolean value) {
    return value ? "true" : "false";
  }
}
