package com.hyperfactions.command.teleport;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.Faction;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.platform.HyperFactionsPlugin;
import com.hyperfactions.util.ChunkUtil;
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
      ctx.sendMessage(prefix().insert(msg("You don't have permission to set faction home.", COLOR_RED)));
      return;
    }

    if (!ConfigManager.get().isWorldAllowed(currentWorld.getName())) {
      ctx.sendMessage(prefix().insert(msg("Cannot set home in this world.", COLOR_RED)));
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
      ctx.sendMessage(prefix().insert(msg("You can only set home in your faction's territory.", COLOR_RED)));
      return;
    }

    // Capture player's look direction (yaw and pitch)
    Faction.FactionHome home = Faction.FactionHome.create(
      currentWorld.getName(), pos.getX(), pos.getY(), pos.getZ(), rot.getYaw(), rot.getPitch(), player.getUuid()
    );

    FactionManager.FactionResult result = hyperFactions.getFactionManager().setHome(faction.id(), home, player.getUuid());

    if (result == FactionManager.FactionResult.SUCCESS) {
      ctx.sendMessage(prefix().insert(msg("Faction home set!", COLOR_GREEN)));
      broadcastToFaction(faction.id(), prefix().insert(msg(player.getUsername(), COLOR_YELLOW))
        .insert(msg(" set the faction home.", COLOR_GREEN)));
    } else if (result == FactionManager.FactionResult.NOT_OFFICER) {
      ctx.sendMessage(prefix().insert(msg("You must be an officer to set the home.", COLOR_RED)));
    } else {
      ctx.sendMessage(prefix().insert(msg("Failed to set home.", COLOR_RED)));
    }
  }
}
