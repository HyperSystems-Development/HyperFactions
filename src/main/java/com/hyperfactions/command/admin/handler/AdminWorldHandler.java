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
import com.hyperfactions.util.HFMessages;
import com.hyperfactions.util.HelpFormatter;
import com.hyperfactions.util.AdminKeys;
import com.hyperfactions.util.HelpKeys;
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
      ctx.sendMessage(prefix().insert(msg(HFMessages.get(player, AdminKeys.AdminCmd.NO_PERMISSION), COLOR_RED)));
      return;
    }

    if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
      showWorldHelp(ctx, player);
      return;
    }

    String subCmd = args[0].toLowerCase();
    String[] subArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];

    switch (subCmd) {
      case "list" -> handleList(ctx, player);
      case "info" -> handleInfo(ctx, subArgs);
      case "set" -> handleSet(ctx, player, subArgs);
      case "reset", "remove" -> handleReset(ctx, player, subArgs);
      default -> ctx.sendMessage(prefix().insert(msg(HFMessages.get(player, AdminKeys.AdminCmd.WORLD_UNKNOWN_CMD), COLOR_RED)));
    }
  }

  private void showWorldHelp(CommandContext ctx, @Nullable PlayerRef player) {
    List<CommandHelp> commands = new ArrayList<>();
    commands.add(new CommandHelp("/f admin world list", HelpKeys.Help.WORLD_CMD_LIST));
    commands.add(new CommandHelp("/f admin world info <world>", HelpKeys.Help.WORLD_CMD_INFO));
    commands.add(new CommandHelp("/f admin world set <world> <setting> <value>", HelpKeys.Help.WORLD_CMD_SET));
    commands.add(new CommandHelp("/f admin world reset <world>", HelpKeys.Help.WORLD_CMD_RESET));
    ctx.sendMessage(HelpFormatter.buildHelp(HelpKeys.Help.WORLD_TITLE, HelpKeys.Help.WORLD_DESCRIPTION, commands, null, player));
  }

  /**
   * /f admin world list — show all configured worlds and their settings.
   */
  private void handleList(CommandContext ctx, @Nullable PlayerRef player) {
    WorldsConfig config = ConfigManager.get().worlds();

    var builder = prefix().insert(msg("Per-World Settings", COLOR_CYAN))
        .insert(msg(" (default: " + config.getDefaultPolicy() + ")", COLOR_GRAY));
    ctx.sendMessage(builder);

    if (config.getWorlds().isEmpty()) {
      ctx.sendMessage(msg("  " + HFMessages.get(player, AdminKeys.AdminCmd.WORLD_NO_SETTINGS), COLOR_GRAY));
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
      if (settings.maxClaims() != null && settings.maxClaims() > 0) {
        parts.add("maxClaims=" + settings.maxClaims());
      }

      if (parts.isEmpty()) {
        line = line.insert(msg("(no overrides)", COLOR_GRAY));
      } else {
        line = line.insert(msg(String.join(", ", parts), COLOR_YELLOW));
      }

      ctx.sendMessage(line);
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
    Integer maxClaims = resolved != null ? resolved.maxClaims() : null;
    ctx.sendMessage(msg("  Max claims: " + (maxClaims != null && maxClaims > 0 ? maxClaims : "unlimited (global)"), COLOR_WHITE));

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
  private void handleSet(CommandContext ctx, @Nullable PlayerRef player, String[] args) {
    if (args.length < 3) {
      ctx.sendMessage(prefix().insert(msg("Usage: /f admin world set <world> <setting> <value>", COLOR_RED)));
      ctx.sendMessage(msg("  Settings: claiming, powerLoss, friendlyFireFaction, friendlyFireAlly, maxClaims", COLOR_GRAY));
      return;
    }

    String worldKey = args[0];
    String setting = args[1].toLowerCase();
    String valueStr = args[2].toLowerCase();

    // Handle integer settings
    if (setting.equals("maxclaims")) {
        WorldsConfig config = ConfigManager.get().worlds();
        WorldSettings current = config.getWorldSettings(worldKey);
        if (current == null) {
            current = WorldSettings.DEFAULTS;
        }

        Integer maxClaimsVal;
        if (valueStr.equals("default") || valueStr.equals("null") || valueStr.equals("0")) {
            maxClaimsVal = null;
        } else {
            try {
                maxClaimsVal = Integer.parseInt(valueStr);
            } catch (NumberFormatException e) {
                ctx.sendMessage(prefix().insert(msg("maxClaims must be a number, 'default', or '0'.", COLOR_RED)));
                return;
            }
            if (maxClaimsVal < 0) {
                ctx.sendMessage(prefix().insert(msg("maxClaims cannot be negative.", COLOR_RED)));
                return;
            }
        }

        WorldSettings updated = new WorldSettings(current.claiming(), current.powerLoss(),
            current.friendlyFireFaction(), current.friendlyFireAlly(), maxClaimsVal);
        config.setWorldSettings(worldKey, updated);
        config.save();
        ConfigManager.get().getWorldSettingsResolver().rebuild(config);

        String displayVal = maxClaimsVal != null ? String.valueOf(maxClaimsVal) : "unlimited";
        ctx.sendMessage(prefix().insert(msg(HFMessages.get(player, AdminKeys.AdminCmd.WORLD_SET, "maxClaims", displayVal, worldKey), COLOR_GREEN)));
        return;
    }

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
      case "claiming" -> new WorldSettings(value, current.powerLoss(), current.friendlyFireFaction(), current.friendlyFireAlly(), current.maxClaims());
      case "powerloss" -> new WorldSettings(current.claiming(), value, current.friendlyFireFaction(), current.friendlyFireAlly(), current.maxClaims());
      case "friendlyfirefaction", "fffaction" -> new WorldSettings(current.claiming(), current.powerLoss(), value, current.friendlyFireAlly(), current.maxClaims());
      case "friendlyfireally", "ffally" -> new WorldSettings(current.claiming(), current.powerLoss(), current.friendlyFireFaction(), value, current.maxClaims());
      default -> {
        ctx.sendMessage(prefix().insert(msg(HFMessages.get(player, AdminKeys.AdminCmd.WORLD_UNKNOWN_SETTING, setting), COLOR_RED)));
        ctx.sendMessage(msg("  Settings: claiming, powerLoss, friendlyFireFaction, friendlyFireAlly, maxClaims", COLOR_GRAY));
        yield null;
      }
    };

    if (updated == null) {
      return;
    }

    config.setWorldSettings(worldKey, updated);
    config.save();
    ConfigManager.get().getWorldSettingsResolver().rebuild(config);

    ctx.sendMessage(prefix().insert(msg(HFMessages.get(player, AdminKeys.AdminCmd.WORLD_SET, setting, String.valueOf(value), worldKey), COLOR_GREEN)));
  }

  /**
   * /f admin world reset {@code <world>} — remove all per-world settings for a world.
   */
  private void handleReset(CommandContext ctx, @Nullable PlayerRef player, String[] args) {
    if (args.length == 0) {
      ctx.sendMessage(prefix().insert(msg("Usage: /f admin world reset <world>", COLOR_RED)));
      return;
    }

    String worldKey = args[0];
    WorldsConfig config = ConfigManager.get().worlds();

    if (!config.removeWorldSettings(worldKey)) {
      ctx.sendMessage(prefix().insert(msg(HFMessages.get(player, AdminKeys.AdminCmd.WORLD_NOT_FOUND, worldKey), COLOR_YELLOW)));
      return;
    }

    config.save();
    ConfigManager.get().getWorldSettingsResolver().rebuild(config);

    ctx.sendMessage(prefix().insert(msg(HFMessages.get(player, AdminKeys.AdminCmd.WORLD_RESET, worldKey), COLOR_GREEN)));
  }

  private String boolStr(boolean value) {
    return value ? "true" : "false";
  }
}
