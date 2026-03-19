package com.hyperfactions.command.teleport;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.Faction;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.platform.HyperFactionsPlugin;
import com.hyperfactions.util.ChunkUtil;
import com.hyperfactions.util.CommandKeys;
import com.hyperfactions.util.MessageUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Subcommand: /f sethome
 * Sets the faction home at the player's current location.
 */
public class SetHomeSubCommand extends FactionSubCommand {

  /** Creates a new SetHomeSubCommand. */
  public SetHomeSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
    super("sethome", "Set faction home", hyperFactions, plugin);
  }

  /** Executes the command. */
  @Override
  protected void execute(@NotNull CommandContext ctx,
             @NotNull Store<EntityStore> store,
             @NotNull Ref<EntityStore> ref,
             @NotNull PlayerRef player,
             @NotNull World currentWorld) {

    if (!hasPermission(player, Permissions.SETHOME)) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Home.SETHOME_NO_PERMISSION));
      return;
    }

    if (!ConfigManager.get().isWorldAllowed(currentWorld.getName())) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Home.SETHOME_WORLD_NOT_ALLOWED));
      return;
    }

    Faction faction = requireFaction(ctx, player);
    if (faction == null) {
      return;
    }

    TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
    if (transform == null) {
      return;
    }

    Vector3d pos = transform.getPosition();
    Vector3f rot = transform.getRotation();
    int chunkX = ChunkUtil.toChunkCoord(pos.getX());
    int chunkZ = ChunkUtil.toChunkCoord(pos.getZ());
    UUID claimOwner = hyperFactions.getClaimManager().getClaimOwner(currentWorld.getName(), chunkX, chunkZ);

    if (claimOwner == null || !claimOwner.equals(faction.id())) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Home.NOT_IN_TERRITORY));
      return;
    }

    // Capture player's look direction (yaw and pitch)
    Faction.FactionHome home = Faction.FactionHome.create(
      currentWorld.getName(), pos.getX(), pos.getY(), pos.getZ(), rot.getYaw(), rot.getPitch(), player.getUuid()
    );

    FactionManager.FactionResult result = hyperFactions.getFactionManager().setHome(faction.id(), home, player.getUuid());

    if (result == FactionManager.FactionResult.SUCCESS) {
      ctx.sendMessage(MessageUtil.success(player, CommandKeys.Home.SET));
      broadcastToFaction(faction.id(), MessageUtil.success(player, CommandKeys.Home.SETHOME_BROADCAST, player.getUsername()));
    } else if (result == FactionManager.FactionResult.NOT_OFFICER) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Home.SETHOME_NOT_OFFICER));
    } else {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Home.SETHOME_FAILED));
    }
  }
}
