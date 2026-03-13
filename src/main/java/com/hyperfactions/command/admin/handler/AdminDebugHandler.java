package com.hyperfactions.command.admin.handler;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.Zone;
import com.hyperfactions.util.ChunkUtil;
import com.hyperfactions.util.CommandHelp;
import com.hyperfactions.util.HFMessages;
import com.hyperfactions.util.HelpFormatter;
import com.hyperfactions.util.AdminKeys;
import com.hyperfactions.util.HelpKeys;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * Handles /f admin debug commands (toggle, status, power, claim, protection, combat, relation).
 */
public class AdminDebugHandler {

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

  /** Creates a new AdminDebugHandler. */
  public AdminDebugHandler(HyperFactions hyperFactions) {
    this.hyperFactions = hyperFactions;
  }

  /** Handles debug. */
  public void handleDebug(CommandContext ctx, @Nullable Store<EntityStore> store, @Nullable Ref<EntityStore> ref,
              @Nullable PlayerRef player, @Nullable World world, String[] args) {
    if (!hasPermission(player, Permissions.ADMIN_DEBUG)) {
      ctx.sendMessage(prefix().insert(msg(HFMessages.get(player, AdminKeys.AdminCmd.DEBUG_NO_PERMISSION), COLOR_RED)));
      return;
    }

    if (args.length == 0) {
      showDebugHelp(ctx, player);
      return;
    }

    String subCmd = args[0].toLowerCase();
    String[] subArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];

    switch (subCmd) {
      case "toggle" -> handleDebugToggle(ctx, subArgs);
      case "status" -> handleDebugStatus(ctx);
      case "power" -> handleDebugPower(ctx, subArgs);
      case "claim" -> {
        if (store == null) {
          ctx.sendMessage(prefix().insert(msg(HFMessages.get(player, AdminKeys.AdminCmd.DEBUG_PLAYER_ONLY), COLOR_RED)));
        } else {
          handleDebugClaim(ctx, store, ref, world, subArgs);
        }
      }
      case "protection" -> {
        if (store == null) {
          ctx.sendMessage(prefix().insert(msg(HFMessages.get(player, AdminKeys.AdminCmd.DEBUG_PLAYER_ONLY), COLOR_RED)));
        } else {
          handleDebugProtection(ctx, store, ref, world, subArgs);
        }
      }
      case "combat" -> handleDebugCombat(ctx, subArgs);
      case "relation" -> handleDebugRelation(ctx, subArgs);
      case "help", "?" -> showDebugHelp(ctx, player);
      default -> {
        ctx.sendMessage(prefix().insert(msg(HFMessages.get(player, AdminKeys.AdminCmd.DEBUG_UNKNOWN_CMD), COLOR_RED)));
        showDebugHelp(ctx, player);
      }
    }
  }

  private void showDebugHelp(CommandContext ctx, @Nullable PlayerRef player) {
    List<CommandHelp> commands = new ArrayList<>();
    commands.add(new CommandHelp("/f admin debug toggle <category> [on|off]", HelpKeys.Help.DEBUG_CMD_TOGGLE));
    commands.add(new CommandHelp("/f admin debug status", HelpKeys.Help.DEBUG_CMD_STATUS));
    commands.add(new CommandHelp("/f admin debug power <player>", HelpKeys.Help.DEBUG_CMD_POWER));
    commands.add(new CommandHelp("/f admin debug claim [x z]", HelpKeys.Help.DEBUG_CMD_CLAIM));
    commands.add(new CommandHelp("/f admin debug protection <player>", HelpKeys.Help.DEBUG_CMD_PROTECTION));
    commands.add(new CommandHelp("/f admin debug combat <player>", HelpKeys.Help.DEBUG_CMD_COMBAT));
    commands.add(new CommandHelp("/f admin debug relation <faction1> <faction2>", HelpKeys.Help.DEBUG_CMD_RELATION));
    ctx.sendMessage(HelpFormatter.buildHelp(HelpKeys.Help.DEBUG_TITLE, HelpKeys.Help.DEBUG_DESCRIPTION, commands, null, player));
  }

  /** Handles debug toggle. */
  public void handleDebugToggle(CommandContext ctx, String[] args) {
    var debugConfig = ConfigManager.get().debug();

    if (args.length == 0) {
      // Show current status
      ctx.sendMessage(msg("=== " + HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.DEBUG_STATUS_HEADER) + " ===", COLOR_CYAN).bold(true));
      ctx.sendMessage(msg("Categories:", COLOR_GRAY));
      ctx.sendMessage(msg("  power: ", COLOR_WHITE).insert(msg(debugConfig.isPower() ? "ON" : "OFF", debugConfig.isPower() ? COLOR_GREEN : COLOR_RED)));
      ctx.sendMessage(msg("  claim: ", COLOR_WHITE).insert(msg(debugConfig.isClaim() ? "ON" : "OFF", debugConfig.isClaim() ? COLOR_GREEN : COLOR_RED)));
      ctx.sendMessage(msg("  combat: ", COLOR_WHITE).insert(msg(debugConfig.isCombat() ? "ON" : "OFF", debugConfig.isCombat() ? COLOR_GREEN : COLOR_RED)));
      ctx.sendMessage(msg("  protection: ", COLOR_WHITE).insert(msg(debugConfig.isProtection() ? "ON" : "OFF", debugConfig.isProtection() ? COLOR_GREEN : COLOR_RED)));
      ctx.sendMessage(msg("  relation: ", COLOR_WHITE).insert(msg(debugConfig.isRelation() ? "ON" : "OFF", debugConfig.isRelation() ? COLOR_GREEN : COLOR_RED)));
      ctx.sendMessage(msg("  territory: ", COLOR_WHITE).insert(msg(debugConfig.isTerritory() ? "ON" : "OFF", debugConfig.isTerritory() ? COLOR_GREEN : COLOR_RED)));
      ctx.sendMessage(msg("  worldmap: ", COLOR_WHITE).insert(msg(debugConfig.isWorldmap() ? "ON" : "OFF", debugConfig.isWorldmap() ? COLOR_GREEN : COLOR_RED)));
      ctx.sendMessage(msg("  interaction: ", COLOR_WHITE).insert(msg(debugConfig.isInteraction() ? "ON" : "OFF", debugConfig.isInteraction() ? COLOR_GREEN : COLOR_RED)));
      ctx.sendMessage(msg("  mixin: ", COLOR_WHITE).insert(msg(debugConfig.isMixin() ? "ON" : "OFF", debugConfig.isMixin() ? COLOR_GREEN : COLOR_RED)));
      ctx.sendMessage(msg("  spawning: ", COLOR_WHITE).insert(msg(debugConfig.isSpawning() ? "ON" : "OFF", debugConfig.isSpawning() ? COLOR_GREEN : COLOR_RED)));
      ctx.sendMessage(msg("  integration: ", COLOR_WHITE).insert(msg(debugConfig.isIntegration() ? "ON" : "OFF", debugConfig.isIntegration() ? COLOR_GREEN : COLOR_RED)));
      ctx.sendMessage(msg("  economy: ", COLOR_WHITE).insert(msg(debugConfig.isEconomy() ? "ON" : "OFF", debugConfig.isEconomy() ? COLOR_GREEN : COLOR_RED)));
      ctx.sendMessage(msg("Usage: /f admin debug toggle <category|all> [on|off]", COLOR_GRAY));
      return;
    }

    String category = args[0].toLowerCase();

    // Handle "all" category
    if (category.equals("all")) {
      boolean enable = args.length > 1 ? args[1].equalsIgnoreCase("on") : !debugConfig.isEnabledByDefault();
      if (enable) {
        debugConfig.enableAll();
        ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.DEBUG_ALL_ENABLED), COLOR_GREEN)));
      } else {
        debugConfig.disableAll();
        ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.DEBUG_ALL_DISABLED), COLOR_GREEN)));
      }
      debugConfig.save();
      return;
    }

    // Get current value and determine new value
    boolean currentValue;
    switch (category) {
      case "power" -> currentValue = debugConfig.isPower();
      case "claim" -> currentValue = debugConfig.isClaim();
      case "combat" -> currentValue = debugConfig.isCombat();
      case "protection" -> currentValue = debugConfig.isProtection();
      case "relation" -> currentValue = debugConfig.isRelation();
      case "territory" -> currentValue = debugConfig.isTerritory();
      case "worldmap", "map" -> currentValue = debugConfig.isWorldmap();
      case "interaction" -> currentValue = debugConfig.isInteraction();
      case "mixin" -> currentValue = debugConfig.isMixin();
      case "spawning" -> currentValue = debugConfig.isSpawning();
      case "integration" -> currentValue = debugConfig.isIntegration();
      case "economy" -> currentValue = debugConfig.isEconomy();
      default -> {
        ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.DEBUG_UNKNOWN_CATEGORY, category), COLOR_RED)));
        ctx.sendMessage(msg("Valid categories: power, claim, combat, protection, relation, territory, worldmap, interaction, mixin, spawning, integration, economy, all", COLOR_GRAY));
        return;
      }
    }

    // Determine new value: if explicit on/off provided use that, otherwise toggle
    boolean newValue = args.length > 1
      ? args[1].equalsIgnoreCase("on")
      : !currentValue;

    // Apply the change
    switch (category) {
      case "power" -> debugConfig.setPower(newValue);
      case "claim" -> debugConfig.setClaim(newValue);
      case "combat" -> debugConfig.setCombat(newValue);
      case "protection" -> debugConfig.setProtection(newValue);
      case "relation" -> debugConfig.setRelation(newValue);
      case "territory" -> debugConfig.setTerritory(newValue);
      case "worldmap", "map" -> debugConfig.setWorldmap(newValue);
      case "interaction" -> debugConfig.setInteraction(newValue);
      case "mixin" -> debugConfig.setMixin(newValue);
      case "spawning" -> debugConfig.setSpawning(newValue);
      case "integration" -> debugConfig.setIntegration(newValue);
      case "economy" -> debugConfig.setEconomy(newValue);
      default -> throw new IllegalStateException("Unexpected value");
    }

    // Save to persist the change
    debugConfig.save();

    ctx.sendMessage(prefix().insert(
      msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.DEBUG_TOGGLE_SET, category, newValue ? "ON" : "OFF"), COLOR_GREEN)
    ));
  }

  /** Handles debug status. */
  public void handleDebugStatus(CommandContext ctx) {
    var debugConfig = ConfigManager.get().debug();

    ctx.sendMessage(msg("=== " + HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.DEBUG_FULL_STATUS_HEADER) + " ===", COLOR_CYAN).bold(true));

    // Data counts
    ctx.sendMessage(msg("Data:", COLOR_GRAY));
    ctx.sendMessage(msg("  Factions: " + hyperFactions.getFactionManager().getAllFactions().size(), COLOR_WHITE));
    ctx.sendMessage(msg("  Zones: " + hyperFactions.getZoneManager().getAllZones().size(), COLOR_WHITE));
    ctx.sendMessage(msg("  Claims: " + hyperFactions.getClaimManager().getTotalClaimCount(), COLOR_WHITE));

    // Debug logging status
    ctx.sendMessage(msg("Debug Logging:", COLOR_GRAY));
    ctx.sendMessage(msg("  power: ", COLOR_WHITE).insert(msg(debugConfig.isPower() ? "ON" : "OFF", debugConfig.isPower() ? COLOR_GREEN : COLOR_RED)));
    ctx.sendMessage(msg("  claim: ", COLOR_WHITE).insert(msg(debugConfig.isClaim() ? "ON" : "OFF", debugConfig.isClaim() ? COLOR_GREEN : COLOR_RED)));
    ctx.sendMessage(msg("  combat: ", COLOR_WHITE).insert(msg(debugConfig.isCombat() ? "ON" : "OFF", debugConfig.isCombat() ? COLOR_GREEN : COLOR_RED)));
    ctx.sendMessage(msg("  protection: ", COLOR_WHITE).insert(msg(debugConfig.isProtection() ? "ON" : "OFF", debugConfig.isProtection() ? COLOR_GREEN : COLOR_RED)));
    ctx.sendMessage(msg("  relation: ", COLOR_WHITE).insert(msg(debugConfig.isRelation() ? "ON" : "OFF", debugConfig.isRelation() ? COLOR_GREEN : COLOR_RED)));
    ctx.sendMessage(msg("  territory: ", COLOR_WHITE).insert(msg(debugConfig.isTerritory() ? "ON" : "OFF", debugConfig.isTerritory() ? COLOR_GREEN : COLOR_RED)));
    ctx.sendMessage(msg("  worldmap: ", COLOR_WHITE).insert(msg(debugConfig.isWorldmap() ? "ON" : "OFF", debugConfig.isWorldmap() ? COLOR_GREEN : COLOR_RED)));
    ctx.sendMessage(msg("  interaction: ", COLOR_WHITE).insert(msg(debugConfig.isInteraction() ? "ON" : "OFF", debugConfig.isInteraction() ? COLOR_GREEN : COLOR_RED)));
    ctx.sendMessage(msg("  mixin: ", COLOR_WHITE).insert(msg(debugConfig.isMixin() ? "ON" : "OFF", debugConfig.isMixin() ? COLOR_GREEN : COLOR_RED)));
    ctx.sendMessage(msg("  spawning: ", COLOR_WHITE).insert(msg(debugConfig.isSpawning() ? "ON" : "OFF", debugConfig.isSpawning() ? COLOR_GREEN : COLOR_RED)));
    ctx.sendMessage(msg("  integration: ", COLOR_WHITE).insert(msg(debugConfig.isIntegration() ? "ON" : "OFF", debugConfig.isIntegration() ? COLOR_GREEN : COLOR_RED)));
    ctx.sendMessage(msg("  economy: ", COLOR_WHITE).insert(msg(debugConfig.isEconomy() ? "ON" : "OFF", debugConfig.isEconomy() ? COLOR_GREEN : COLOR_RED)));
  }

  /** Handles debug power. */
  public void handleDebugPower(CommandContext ctx, String[] args) {
    if (args.length < 1) {
      ctx.sendMessage(prefix().insert(msg("Usage: /f admin debug power <player>", COLOR_RED)));
      return;
    }
    ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.DEBUG_NOT_IMPLEMENTED), COLOR_YELLOW)));
  }

  /** Handles debug claim. */
  public void handleDebugClaim(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, World world, String[] args) {
    TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
    if (transform == null) {
      return;
    }

    Vector3d pos = transform.getPosition();
    int chunkX = ChunkUtil.toChunkCoord(pos.getX());
    int chunkZ = ChunkUtil.toChunkCoord(pos.getZ());

    if (args.length >= 2) {
      try {
        chunkX = Integer.parseInt(args[0]);
        chunkZ = Integer.parseInt(args[1]);
      } catch (NumberFormatException e) {
        ctx.sendMessage(prefix().insert(msg("Invalid chunk coordinates.", COLOR_RED)));
        return;
      }
    }

    ctx.sendMessage(msg("=== Claim Debug (" + chunkX + ", " + chunkZ + ") ===", COLOR_CYAN).bold(true));
    UUID owner = hyperFactions.getClaimManager().getClaimOwner(world.getName(), chunkX, chunkZ);
    if (owner != null) {
      var faction = hyperFactions.getFactionManager().getFaction(owner);
      ctx.sendMessage(msg("Owner: " + (faction != null ? faction.name() : owner.toString()), COLOR_WHITE));
    } else {
      ctx.sendMessage(msg("Owner: None (wilderness)", COLOR_GRAY));
    }

    Zone zone = hyperFactions.getZoneManager().getZone(world.getName(), chunkX, chunkZ);
    if (zone != null) {
      ctx.sendMessage(msg("Zone: " + zone.name() + " (" + zone.type().getDisplayName() + ")", COLOR_WHITE));
    }
  }

  /** Handles debug protection. */
  public void handleDebugProtection(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, World world, String[] args) {
    ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.DEBUG_NOT_IMPLEMENTED), COLOR_YELLOW)));
  }

  /** Handles debug combat. */
  public void handleDebugCombat(CommandContext ctx, String[] args) {
    if (args.length < 1) {
      ctx.sendMessage(prefix().insert(msg("Usage: /f admin debug combat <player>", COLOR_RED)));
      return;
    }
    ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.DEBUG_NOT_IMPLEMENTED), COLOR_YELLOW)));
  }

  public void handleDebugRelation(CommandContext ctx, String[] args) {
    if (args.length < 2) {
      ctx.sendMessage(prefix().insert(msg("Usage: /f admin debug relation <faction1> <faction2>", COLOR_RED)));
      return;
    }
    ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.DEBUG_NOT_IMPLEMENTED), COLOR_YELLOW)));
  }
}
