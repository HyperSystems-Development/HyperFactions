package com.hyperfactions.command.territory;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.FactionCommandContext;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.data.Faction;
import com.hyperfactions.manager.ClaimManager;
import com.hyperfactions.platform.HyperFactionsPlugin;
import com.hyperfactions.util.ChunkUtil;
import com.hyperfactions.util.CommandKeys;
import com.hyperfactions.util.CommonKeys;
import com.hyperfactions.util.MessageUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

/**
 * Subcommand: /f overclaim
 * Overclaims enemy territory (when they are raidable).
 */
public class OverclaimSubCommand extends FactionSubCommand {

  /** Creates a new OverclaimSubCommand. */
  public OverclaimSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
    super("overclaim", "Overclaim enemy territory", hyperFactions, plugin);
  }

  /** Executes the command. */
  @Override
  protected void execute(@NotNull CommandContext ctx,
             @NotNull Store<EntityStore> store,
             @NotNull Ref<EntityStore> ref,
             @NotNull PlayerRef player,
             @NotNull World currentWorld) {

    if (!hasPermission(player, Permissions.OVERCLAIM)) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Claim.OVERCLAIM_NO_PERMISSION));
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
    int chunkX = ChunkUtil.toChunkCoord(pos.getX());
    int chunkZ = ChunkUtil.toChunkCoord(pos.getZ());

    String[] rawArgs = CommandUtil.parseRawArgs(ctx.getInputString(), 2);
    FactionCommandContext fctx = parseContext(rawArgs);

    ClaimManager.ClaimResult result = hyperFactions.getClaimManager().overclaim(
      player.getUuid(), currentWorld.getName(), chunkX, chunkZ
    );

    switch (result) {
      case SUCCESS -> {
        ctx.sendMessage(MessageUtil.success(player, CommandKeys.Claim.OVERCLAIMED));
        // Show map after overclaiming (if not text mode)
        if (!fctx.isTextMode()) {
          Player playerEntity = store.getComponent(ref, Player.getComponentType());
          if (playerEntity != null) {
            hyperFactions.getGuiManager().openChunkMap(playerEntity, ref, store, player);
          }
        }
      }
      case NOT_IN_FACTION -> ctx.sendMessage(MessageUtil.error(player, CommonKeys.Common.NOT_IN_FACTION));
      case NOT_OFFICER -> ctx.sendMessage(MessageUtil.error(player, CommandKeys.Claim.OVERCLAIM_NOT_OFFICER));
      case CHUNK_NOT_CLAIMED -> ctx.sendMessage(MessageUtil.error(player, CommandKeys.Claim.OVERCLAIM_NOT_CLAIMED));
      case ALREADY_CLAIMED_SELF -> ctx.sendMessage(MessageUtil.error(player, CommandKeys.Claim.OVERCLAIM_OWN));
      case ALREADY_CLAIMED_ALLY -> ctx.sendMessage(MessageUtil.error(player, CommandKeys.Claim.OVERCLAIM_ALLY));
      case TARGET_HAS_POWER -> ctx.sendMessage(MessageUtil.error(player, CommandKeys.Claim.TARGET_HAS_POWER));
      case MAX_CLAIMS_REACHED -> ctx.sendMessage(MessageUtil.error(player, CommandKeys.Claim.MAX_CLAIMS));
      default -> ctx.sendMessage(MessageUtil.error(player, CommandKeys.Claim.OVERCLAIM_FAILED));
    }
  }
}
