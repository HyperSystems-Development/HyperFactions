package com.hyperfactions.command.admin.handler;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.data.Zone;
import com.hyperfactions.data.ZoneFlags;
import com.hyperfactions.data.ZoneType;
import com.hyperfactions.manager.ZoneManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Arrays;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * Handles /f admin zone, safezone, warzone, removezone, and zoneflag commands.
 */
public class AdminZoneHandler {

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

  /** Creates a new AdminZoneHandler. */
  public AdminZoneHandler(HyperFactions hyperFactions) {
    this.hyperFactions = hyperFactions;
  }

  /** Handles safezone. */
  public void handleSafezone(CommandContext ctx, PlayerRef player, World world, int chunkX, int chunkZ, String[] args) {
    String zoneName = args.length > 1 ? args[1] : "SafeZone-" + chunkX + "_" + chunkZ;
    ZoneManager.ZoneResult result = hyperFactions.getZoneManager().createZone(
      zoneName, ZoneType.SAFE, world.getName(), chunkX, chunkZ, player.getUuid()
    );
    if (result == ZoneManager.ZoneResult.SUCCESS) {
      ctx.sendMessage(prefix().insert(msg("Created SafeZone '" + zoneName + "' at " + chunkX + ", " + chunkZ, COLOR_GREEN)));
    } else if (result == ZoneManager.ZoneResult.CHUNK_CLAIMED) {
      ctx.sendMessage(prefix().insert(msg("Cannot create zone: This chunk is claimed by a faction.", COLOR_RED)));
    } else if (result == ZoneManager.ZoneResult.ALREADY_EXISTS) {
      ctx.sendMessage(prefix().insert(msg("A zone already exists at this location.", COLOR_RED)));
    } else if (result == ZoneManager.ZoneResult.NAME_TAKEN) {
      ctx.sendMessage(prefix().insert(msg("A zone with that name already exists.", COLOR_RED)));
    } else {
      ctx.sendMessage(prefix().insert(msg("Failed: " + result, COLOR_RED)));
    }
  }

  /** Handles warzone. */
  public void handleWarzone(CommandContext ctx, PlayerRef player, World world, int chunkX, int chunkZ, String[] args) {
    String zoneName = args.length > 1 ? args[1] : "WarZone-" + chunkX + "_" + chunkZ;
    ZoneManager.ZoneResult result = hyperFactions.getZoneManager().createZone(
      zoneName, ZoneType.WAR, world.getName(), chunkX, chunkZ, player.getUuid()
    );
    if (result == ZoneManager.ZoneResult.SUCCESS) {
      ctx.sendMessage(prefix().insert(msg("Created WarZone '" + zoneName + "' at " + chunkX + ", " + chunkZ, COLOR_GREEN)));
    } else if (result == ZoneManager.ZoneResult.CHUNK_CLAIMED) {
      ctx.sendMessage(prefix().insert(msg("Cannot create zone: This chunk is claimed by a faction.", COLOR_RED)));
    } else if (result == ZoneManager.ZoneResult.ALREADY_EXISTS) {
      ctx.sendMessage(prefix().insert(msg("A zone already exists at this location.", COLOR_RED)));
    } else if (result == ZoneManager.ZoneResult.NAME_TAKEN) {
      ctx.sendMessage(prefix().insert(msg("A zone with that name already exists.", COLOR_RED)));
    } else {
      ctx.sendMessage(prefix().insert(msg("Failed: " + result, COLOR_RED)));
    }
  }

  /** Handles removezone. */
  public void handleRemovezone(CommandContext ctx, World world, int chunkX, int chunkZ) {
    ZoneManager.ZoneResult result = hyperFactions.getZoneManager().unclaimChunkAt(world.getName(), chunkX, chunkZ);
    if (result == ZoneManager.ZoneResult.SUCCESS) {
      ctx.sendMessage(prefix().insert(msg("Unclaimed chunk from zone.", COLOR_GREEN)));
    } else {
      ctx.sendMessage(prefix().insert(msg("No zone chunk found at this location.", COLOR_RED)));
    }
  }

  /** Handles admin zone. */
  public void handleAdminZone(CommandContext ctx, @Nullable Store<EntityStore> store, @Nullable Ref<EntityStore> ref,
                @Nullable PlayerRef player, @Nullable World world,
                int chunkX, int chunkZ, String[] args, boolean isPlayer, UUID senderUuid) {
    // No args - open zone GUI (player) or show zone list (console)
    if (args.length == 0) {
      if (!isPlayer) {
        handleZoneList(ctx);
        return;
      }
      Player playerEntity = store.getComponent(ref, Player.getComponentType());
      if (playerEntity != null) {
        hyperFactions.getGuiManager().openAdminZone(playerEntity, ref, store, player);
      }
      return;
    }

    String subCmd = args[0].toLowerCase();
    String[] subArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];
    String worldName = world != null ? world.getName() : null;

    switch (subCmd) {
      case "list" -> handleZoneList(ctx);
      case "create" -> handleZoneCreate(ctx, worldName != null ? worldName : "world", senderUuid, subArgs);
      case "delete" -> handleZoneDelete(ctx, subArgs);
      case "rename" -> handleZoneRename(ctx, subArgs);
      case "info" -> {
        if (subArgs.length == 0 && !isPlayer) {
          ctx.sendMessage(prefix().insert(msg("Usage: /f admin zone info <name>", COLOR_RED)));
        } else {
          handleZoneInfo(ctx, worldName, chunkX, chunkZ, subArgs);
        }
      }
      case "claim" -> {
        if (isPlayer) {
          handleZoneClaim(ctx, worldName, chunkX, chunkZ, subArgs);
        } else {
          ctx.sendMessage(prefix().insert(msg("This command can only be used by a player.", COLOR_RED)));
        }
      }
      case "unclaim" -> {
        if (isPlayer) {
          handleZoneUnclaim(ctx, worldName, chunkX, chunkZ);
        } else {
          ctx.sendMessage(prefix().insert(msg("This command can only be used by a player.", COLOR_RED)));
        }
      }
      case "radius" -> {
        if (isPlayer) {
          handleZoneRadius(ctx, worldName, chunkX, chunkZ, subArgs);
        } else {
          ctx.sendMessage(prefix().insert(msg("This command can only be used by a player.", COLOR_RED)));
        }
      }
      case "notify" -> handleZoneNotify(ctx, subArgs);
      case "title" -> handleZoneTitle(ctx, subArgs);
      default -> ctx.sendMessage(prefix().insert(msg("Unknown zone command. Use /f admin help", COLOR_RED)));
    }
  }

  /** Handles zone list. */
  public void handleZoneList(CommandContext ctx) {
    var zones = hyperFactions.getZoneManager().getAllZones();
    if (zones.isEmpty()) {
      ctx.sendMessage(prefix().insert(msg("No zones defined.", COLOR_GRAY)));
      return;
    }

    ctx.sendMessage(msg("=== Zones (" + zones.size() + ") ===", COLOR_CYAN).bold(true));
    for (Zone zone : zones) {
      String typeColor = zone.isSafeZone() ? "#2dd4bf" : "#c084fc";
      ctx.sendMessage(msg("  " + zone.name(), typeColor)
        .insert(msg(" (" + zone.type().name() + ", " + zone.getChunkCount() + " chunks)", COLOR_GRAY)));
    }
  }

  /** Handles zone create. */
  public void handleZoneCreate(CommandContext ctx, String worldName, UUID createdBy, String[] args) {
    if (args.length < 2) {
      ctx.sendMessage(prefix().insert(msg("Usage: /f admin zone create <safe|war> <name>", COLOR_RED)));
      return;
    }

    String typeStr = args[0].toLowerCase();
    String name = args[1];

    ZoneType type;
    if (typeStr.equals("safe") || typeStr.equals("safezone")) {
      type = ZoneType.SAFE;
    } else if (typeStr.equals("war") || typeStr.equals("warzone")) {
      type = ZoneType.WAR;
    } else {
      ctx.sendMessage(prefix().insert(msg("Invalid zone type. Use 'safe' or 'war'", COLOR_RED)));
      return;
    }

    ZoneManager.ZoneResult result = hyperFactions.getZoneManager().createZone(name, type, worldName, createdBy);
    switch (result) {
      case SUCCESS -> ctx.sendMessage(prefix().insert(msg("Created " + type.getDisplayName() + " '" + name + "' (empty, use claim to add chunks)", COLOR_GREEN)));
      case NAME_TAKEN -> ctx.sendMessage(prefix().insert(msg("A zone with that name already exists.", COLOR_RED)));
      case INVALID_NAME -> ctx.sendMessage(prefix().insert(msg("Invalid zone name. Must be 1-32 characters.", COLOR_RED)));
      default -> ctx.sendMessage(prefix().insert(msg("Failed: " + result, COLOR_RED)));
    }
  }

  /** Handles zone delete. */
  public void handleZoneDelete(CommandContext ctx, String[] args) {
    if (args.length < 1) {
      ctx.sendMessage(prefix().insert(msg("Usage: /f admin zone delete <name>", COLOR_RED)));
      return;
    }

    String name = args[0];
    Zone zone = hyperFactions.getZoneManager().getZoneByName(name);
    if (zone == null) {
      ctx.sendMessage(prefix().insert(msg("Zone '" + name + "' not found.", COLOR_RED)));
      return;
    }

    ZoneManager.ZoneResult result = hyperFactions.getZoneManager().removeZone(zone.id());
    if (result == ZoneManager.ZoneResult.SUCCESS) {
      ctx.sendMessage(prefix().insert(msg("Deleted zone '" + name + "' (" + zone.getChunkCount() + " chunks released)", COLOR_GREEN)));
    } else {
      ctx.sendMessage(prefix().insert(msg("Failed to delete zone: " + result, COLOR_RED)));
    }
  }

  /** Handles zone rename. */
  public void handleZoneRename(CommandContext ctx, String[] args) {
    if (args.length < 2) {
      ctx.sendMessage(prefix().insert(msg("Usage: /f admin zone rename <current-name> <new-name>", COLOR_RED)));
      return;
    }

    String currentName = args[0];
    String newName = args[1];

    Zone zone = hyperFactions.getZoneManager().getZoneByName(currentName);
    if (zone == null) {
      ctx.sendMessage(prefix().insert(msg("Zone '" + currentName + "' not found.", COLOR_RED)));
      return;
    }

    ZoneManager.ZoneResult result = hyperFactions.getZoneManager().renameZone(zone.id(), newName);
    switch (result) {
      case SUCCESS -> ctx.sendMessage(prefix().insert(msg("Renamed zone '" + currentName + "' to '" + newName + "'", COLOR_GREEN)));
      case NAME_TAKEN -> ctx.sendMessage(prefix().insert(msg("A zone with the name '" + newName + "' already exists.", COLOR_RED)));
      case INVALID_NAME -> ctx.sendMessage(prefix().insert(msg("Invalid zone name. Must be 1-32 characters.", COLOR_RED)));
      default -> ctx.sendMessage(prefix().insert(msg("Failed to rename zone: " + result, COLOR_RED)));
    }
  }

  /** Handles zone info. */
  public void handleZoneInfo(CommandContext ctx, String worldName, int chunkX, int chunkZ, String[] args) {
    Zone zone;
    if (args.length > 0) {
      zone = hyperFactions.getZoneManager().getZoneByName(args[0]);
      if (zone == null) {
        ctx.sendMessage(prefix().insert(msg("Zone '" + args[0] + "' not found.", COLOR_RED)));
        return;
      }
    } else {
      zone = hyperFactions.getZoneManager().getZone(worldName, chunkX, chunkZ);
      if (zone == null) {
        ctx.sendMessage(prefix().insert(msg("No zone at your location.", COLOR_RED)));
        return;
      }
    }

    String typeColor = zone.isSafeZone() ? "#2dd4bf" : "#c084fc";
    ctx.sendMessage(msg("=== Zone: " + zone.name() + " ===", typeColor).bold(true));
    ctx.sendMessage(msg("Type: ", COLOR_GRAY).insert(msg(zone.type().getDisplayName(), typeColor)));
    ctx.sendMessage(msg("World: ", COLOR_GRAY).insert(msg(zone.world(), COLOR_WHITE)));
    ctx.sendMessage(msg("Chunks: ", COLOR_GRAY).insert(msg(String.valueOf(zone.getChunkCount()), COLOR_WHITE)));

    // Notification settings
    boolean notifyEnabled = zone.notifyOnEntry() == null || zone.notifyOnEntry();
    ctx.sendMessage(msg("Notify: ", COLOR_GRAY).insert(msg(notifyEnabled ? "enabled" : "disabled",
        notifyEnabled ? COLOR_GREEN : COLOR_RED)));
    if (zone.notifyTitleUpper() != null) {
      ctx.sendMessage(msg("  Upper title: ", COLOR_GRAY).insert(msg(zone.notifyTitleUpper(), COLOR_YELLOW)));
    }
    if (zone.notifyTitleLower() != null) {
      ctx.sendMessage(msg("  Lower title: ", COLOR_GRAY).insert(msg(zone.notifyTitleLower(), COLOR_YELLOW)));
    }

    if (!zone.getFlags().isEmpty()) {
      ctx.sendMessage(msg("Custom Flags:", COLOR_GRAY));
      for (var entry : zone.getFlags().entrySet()) {
        ctx.sendMessage(msg("  " + entry.getKey() + ": " + entry.getValue(), COLOR_YELLOW));
      }
    }
  }

  /** Handles zone claim. */
  public void handleZoneClaim(CommandContext ctx, String worldName, int chunkX, int chunkZ, String[] args) {
    if (args.length < 1) {
      ctx.sendMessage(prefix().insert(msg("Usage: /f admin zone claim <name>", COLOR_RED)));
      return;
    }

    String name = args[0];
    Zone zone = hyperFactions.getZoneManager().getZoneByName(name);
    if (zone == null) {
      ctx.sendMessage(prefix().insert(msg("Zone '" + name + "' not found.", COLOR_RED)));
      return;
    }

    ZoneManager.ZoneResult result = hyperFactions.getZoneManager().claimChunk(zone.id(), worldName, chunkX, chunkZ);
    switch (result) {
      case SUCCESS -> ctx.sendMessage(prefix().insert(msg("Claimed chunk (" + chunkX + ", " + chunkZ + ") for zone '" + name + "'", COLOR_GREEN)));
      case CHUNK_HAS_ZONE -> ctx.sendMessage(prefix().insert(msg("This chunk already belongs to another zone.", COLOR_RED)));
      case CHUNK_HAS_FACTION -> ctx.sendMessage(prefix().insert(msg("This chunk is claimed by a faction.", COLOR_RED)));
      default -> ctx.sendMessage(prefix().insert(msg("Failed: " + result, COLOR_RED)));
    }
  }

  /** Handles zone unclaim. */
  public void handleZoneUnclaim(CommandContext ctx, String worldName, int chunkX, int chunkZ) {
    ZoneManager.ZoneResult result = hyperFactions.getZoneManager().unclaimChunkAt(worldName, chunkX, chunkZ);
    if (result == ZoneManager.ZoneResult.SUCCESS) {
      ctx.sendMessage(prefix().insert(msg("Unclaimed chunk (" + chunkX + ", " + chunkZ + ") from zone.", COLOR_GREEN)));
    } else {
      ctx.sendMessage(prefix().insert(msg("No zone chunk found at this location.", COLOR_RED)));
    }
  }

  /** Handles zone radius. */
  public void handleZoneRadius(CommandContext ctx, String worldName, int chunkX, int chunkZ, String[] args) {
    if (args.length < 2) {
      ctx.sendMessage(prefix().insert(msg("Usage: /f admin zone radius <name> <radius> [circle|square]", COLOR_RED)));
      ctx.sendMessage(msg("  radius: 1-20 chunks", COLOR_GRAY));
      ctx.sendMessage(msg("  shape: circle (default) or square", COLOR_GRAY));
      return;
    }

    String name = args[0];
    Zone zone = hyperFactions.getZoneManager().getZoneByName(name);
    if (zone == null) {
      ctx.sendMessage(prefix().insert(msg("Zone '" + name + "' not found.", COLOR_RED)));
      return;
    }

    int radius;
    try {
      radius = Integer.parseInt(args[1]);
      if (radius < 1 || radius > 20) {
        ctx.sendMessage(prefix().insert(msg("Radius must be between 1 and 20.", COLOR_RED)));
        return;
      }
    } catch (NumberFormatException e) {
      ctx.sendMessage(prefix().insert(msg("Invalid radius number.", COLOR_RED)));
      return;
    }

    boolean circle = true;
    if (args.length > 2) {
      String shape = args[2].toLowerCase();
      if (shape.equals("square")) {
        circle = false;
      } else if (!shape.equals("circle")) {
        ctx.sendMessage(prefix().insert(msg("Invalid shape. Use 'circle' or 'square'.", COLOR_RED)));
        return;
      }
    }

    int claimed = hyperFactions.getZoneManager().claimRadius(zone.id(), worldName, chunkX, chunkZ, radius, circle);
    if (claimed > 0) {
      ctx.sendMessage(prefix().insert(msg("Claimed " + claimed + " chunks for zone '" + name + "'", COLOR_GREEN)));
    } else {
      ctx.sendMessage(prefix().insert(msg("No chunks could be claimed (all occupied or already in zone).", COLOR_YELLOW)));
    }
  }

  /** Handles zone notify. */
  public void handleZoneNotify(CommandContext ctx, String[] args) {
    if (args.length < 2) {
      ctx.sendMessage(prefix().insert(msg("Usage: /f admin zone notify <zone> <true|false>", COLOR_RED)));
      return;
    }

    String name = args[0];
    Zone zone = hyperFactions.getZoneManager().getZoneByName(name);
    if (zone == null) {
      ctx.sendMessage(prefix().insert(msg("Zone '" + name + "' not found.", COLOR_RED)));
      return;
    }

    String value = args[1].toLowerCase();
    Boolean notifyValue;
    if (value.equals("true") || value.equals("on") || value.equals("enable")) {
      notifyValue = null; // null = default (enabled)
    } else if (value.equals("false") || value.equals("off") || value.equals("disable")) {
      notifyValue = false;
    } else {
      ctx.sendMessage(prefix().insert(msg("Invalid value. Use: true/false", COLOR_RED)));
      return;
    }

    ZoneManager.ZoneResult result = hyperFactions.getZoneManager().setZoneNotifyOnEntry(zone.id(), notifyValue);
    if (result == ZoneManager.ZoneResult.SUCCESS) {
      boolean enabled = notifyValue == null || notifyValue;
      ctx.sendMessage(prefix().insert(msg("Zone '" + name + "' entry notification "
          + (enabled ? "enabled" : "disabled"), COLOR_GREEN)));
    } else {
      ctx.sendMessage(prefix().insert(msg("Failed: " + result, COLOR_RED)));
    }
  }

  /** Handles zone title. */
  public void handleZoneTitle(CommandContext ctx, String[] args) {
    if (args.length < 3) {
      ctx.sendMessage(prefix().insert(msg("Usage: /f admin zone title <zone> upper|lower <text|clear>", COLOR_RED)));
      return;
    }

    String name = args[0];
    Zone zone = hyperFactions.getZoneManager().getZoneByName(name);
    if (zone == null) {
      ctx.sendMessage(prefix().insert(msg("Zone '" + name + "' not found.", COLOR_RED)));
      return;
    }

    String position = args[1].toLowerCase();
    String text = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

    ZoneManager.ZoneResult result;
    if (position.equals("upper")) {
      result = hyperFactions.getZoneManager().setZoneNotifyTitle(zone.id(), text, null);
    } else if (position.equals("lower")) {
      result = hyperFactions.getZoneManager().setZoneNotifyTitle(zone.id(), null, text);
    } else {
      ctx.sendMessage(prefix().insert(msg("Invalid position. Use: upper or lower", COLOR_RED)));
      return;
    }

    if (result == ZoneManager.ZoneResult.SUCCESS) {
      if (text.equals("clear")) {
        ctx.sendMessage(prefix().insert(msg("Cleared " + position + " title for zone '" + name + "' (using default)", COLOR_GREEN)));
      } else {
        ctx.sendMessage(prefix().insert(msg("Set " + position + " title for zone '" + name + "' to: " + text, COLOR_GREEN)));
      }
    } else {
      ctx.sendMessage(prefix().insert(msg("Failed: " + result, COLOR_RED)));
    }
  }

  /** Handles zone flag. */
  public void handleZoneFlag(CommandContext ctx, String worldName, int chunkX, int chunkZ, String[] args) {
    Zone zone = hyperFactions.getZoneManager().getZone(worldName, chunkX, chunkZ);
    if (zone == null) {
      ctx.sendMessage(prefix().insert(msg("No zone at your location. Stand in a zone to manage flags.", COLOR_RED)));
      return;
    }

    if (args.length == 0) {
      ctx.sendMessage(msg("=== Zone Flags: " + zone.name() + " ===", COLOR_CYAN).bold(true));
      ctx.sendMessage(msg("Zone Type: " + zone.type().getDisplayName(), COLOR_GRAY));
      ctx.sendMessage(msg("", COLOR_GRAY));

      for (String flag : ZoneFlags.ALL_FLAGS) {
        boolean effectiveValue = zone.getEffectiveFlag(flag);
        boolean isCustom = zone.hasFlagSet(flag);
        String valueStr = effectiveValue ? "true" : "false";
        String customStr = isCustom ? " (custom)" : " (default)";
        String color = effectiveValue ? COLOR_GREEN : COLOR_RED;
        ctx.sendMessage(msg("  " + flag + ": ", COLOR_GRAY).insert(msg(valueStr, color)).insert(msg(customStr, COLOR_GRAY)));
      }
      ctx.sendMessage(msg("", COLOR_GRAY));
      ctx.sendMessage(msg("Usage: /f admin zoneflag <flag> <true|false|clear>", COLOR_YELLOW));
      ctx.sendMessage(msg("Usage: /f admin zoneflag clearall (reset all to defaults)", COLOR_YELLOW));
      return;
    }

    if (args[0].equalsIgnoreCase("clearall") || args[0].equalsIgnoreCase("resetall")) {
      ZoneManager.ZoneResult result = hyperFactions.getZoneManager().clearAllZoneFlags(zone.id());
      if (result == ZoneManager.ZoneResult.SUCCESS) {
        ctx.sendMessage(prefix().insert(msg("Cleared all custom flags for '" + zone.name() + "' - now using zone type defaults.", COLOR_GREEN)));
      } else {
        ctx.sendMessage(prefix().insert(msg("Failed to clear flags.", COLOR_RED)));
      }
      return;
    }

    String flagName = args[0].toLowerCase();
    if (!ZoneFlags.isValidFlag(flagName)) {
      ctx.sendMessage(prefix().insert(msg("Invalid flag: " + flagName, COLOR_RED)));
      ctx.sendMessage(msg("Valid flags: " + String.join(", ", ZoneFlags.ALL_FLAGS), COLOR_GRAY));
      return;
    }

    if (args.length == 1) {
      boolean effectiveValue = zone.getEffectiveFlag(flagName);
      boolean isCustom = zone.hasFlagSet(flagName);
      ctx.sendMessage(prefix().insert(msg("Flag '" + flagName + "' = " + effectiveValue, effectiveValue ? COLOR_GREEN : COLOR_RED))
        .insert(msg(isCustom ? " (custom)" : " (default)", COLOR_GRAY)));
      return;
    }

    String action = args[1].toLowerCase();
    ZoneManager.ZoneResult result;

    if (action.equals("clear") || action.equals("default") || action.equals("reset")) {
      result = hyperFactions.getZoneManager().clearZoneFlag(zone.id(), flagName);
      if (result == ZoneManager.ZoneResult.SUCCESS) {
        boolean defaultValue = zone.isSafeZone() ? ZoneFlags.getSafeZoneDefault(flagName) : ZoneFlags.getWarZoneDefault(flagName);
        ctx.sendMessage(prefix().insert(msg("Cleared flag '" + flagName + "' (now using default: " + defaultValue + ")", COLOR_GREEN)));
      } else {
        ctx.sendMessage(prefix().insert(msg("Failed to clear flag.", COLOR_RED)));
      }
    } else if (action.equals("true") || action.equals("false")) {
      boolean value = action.equals("true");
      result = hyperFactions.getZoneManager().setZoneFlag(zone.id(), flagName, value);
      if (result == ZoneManager.ZoneResult.SUCCESS) {
        ctx.sendMessage(prefix().insert(msg("Set flag '" + flagName + "' to " + value, COLOR_GREEN)));
      } else {
        ctx.sendMessage(prefix().insert(msg("Failed to set flag.", COLOR_RED)));
      }
    } else {
      ctx.sendMessage(prefix().insert(msg("Invalid value. Use: true, false, or clear", COLOR_RED)));
    }
  }
}
