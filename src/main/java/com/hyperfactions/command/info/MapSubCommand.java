package com.hyperfactions.command.info;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.FactionCommandContext;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.platform.HyperFactionsPlugin;
import com.hyperfactions.util.ChunkUtil;
import com.hyperfactions.util.HFMessages;
import com.hyperfactions.util.MessageKeys;
import com.hyperfactions.util.MessageUtil;
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
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Subcommand: /f map
 * Views the territory map.
 */
public class MapSubCommand extends FactionSubCommand {

  /** Creates a new MapSubCommand. */
  public MapSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
    super("map", "View territory map", hyperFactions, plugin);
  }

  /** Executes the command. */
  @Override
  protected void execute(@NotNull CommandContext ctx,
             @NotNull Store<EntityStore> store,
             @NotNull Ref<EntityStore> ref,
             @NotNull PlayerRef player,
             @NotNull World currentWorld) {

    if (!hasPermission(player, Permissions.MAP)) {
      ctx.sendMessage(MessageUtil.error(player, MessageKeys.Info.MAP_NO_PERMISSION));
      return;
    }

    String[] rawArgs = CommandUtil.parseRawArgs(ctx.getInputString(), 2);
    FactionCommandContext fctx = parseContext(rawArgs);

    // GUI mode: open ChunkMapPage
    if (fctx.shouldOpenGui()) {
      Player playerEntity = store.getComponent(ref, Player.getComponentType());
      if (playerEntity != null) {
        hyperFactions.getGuiManager().openChunkMap(playerEntity, ref, store, player);
        return;
      }
    }

    // Text mode: ASCII map
    TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
    if (transform == null) {
      return;
    }

    Vector3d pos = transform.getPosition();
    int centerChunkX = ChunkUtil.toChunkCoord(pos.getX());
    int centerChunkZ = ChunkUtil.toChunkCoord(pos.getZ());

    UUID playerFactionId = hyperFactions.getFactionManager().getPlayerFactionId(player.getUuid());

    ctx.sendMessage(msg(HFMessages.get(player, MessageKeys.Info.MAP_HEADER), COLOR_CYAN).bold(true));

    for (int dz = -3; dz <= 3; dz++) {
      StringBuilder row = new StringBuilder();
      for (int dx = -3; dx <= 3; dx++) {
        int chunkX = centerChunkX + dx;
        int chunkZ = centerChunkZ + dz;
        boolean isCenter = (dx == 0 && dz == 0);

        UUID owner = hyperFactions.getClaimManager().getClaimOwner(currentWorld.getName(), chunkX, chunkZ);
        boolean isOwned = playerFactionId != null && playerFactionId.equals(owner);
        boolean isAlly = playerFactionId != null && owner != null
          && hyperFactions.getRelationManager().areAllies(playerFactionId, owner);
        boolean isEnemy = playerFactionId != null && owner != null
          && hyperFactions.getRelationManager().areEnemies(playerFactionId, owner);
        boolean isSafeZone = hyperFactions.getZoneManager().isInSafeZone(currentWorld.getName(), chunkX, chunkZ);
        boolean isWarZone = hyperFactions.getZoneManager().isInWarZone(currentWorld.getName(), chunkX, chunkZ);

        row.append(ChunkUtil.getMapChar(isOwned, isAlly, isEnemy, owner != null, isCenter, isSafeZone, isWarZone));
      }
      ctx.sendMessage(Message.raw(row.toString()));
    }
    ctx.sendMessage(msg(HFMessages.get(player, MessageKeys.Info.MAP_LEGEND), COLOR_GRAY));
    ctx.sendMessage(msg(HFMessages.get(player, MessageKeys.Info.MAP_GUI_HINT), COLOR_GRAY));
  }
}
