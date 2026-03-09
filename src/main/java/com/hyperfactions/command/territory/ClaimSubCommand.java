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
import com.hyperfactions.util.MessageKeys;
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
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Subcommand: /f claim
 * Claims the current chunk for your faction.
 */
public class ClaimSubCommand extends FactionSubCommand {

  /** Creates a new ClaimSubCommand. */
  public ClaimSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
    super("claim", "Claim this chunk", hyperFactions, plugin);
  }

  /** Executes the command. */
  @Override
  protected void execute(@NotNull CommandContext ctx,
             @NotNull Store<EntityStore> store,
             @NotNull Ref<EntityStore> ref,
             @NotNull PlayerRef player,
             @NotNull World currentWorld) {

    if (!hasPermission(player, Permissions.CLAIM)) {
      ctx.sendMessage(MessageUtil.error(player, MessageKeys.Claim.NO_PERMISSION));
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

    // Context-aware behavior - check current chunk status
    UUID playerFactionId = faction.id();
    UUID chunkOwner = hyperFactions.getClaimManager().getClaimOwner(currentWorld.getName(), chunkX, chunkZ);

    // If own territory and not text mode, just show map
    if (playerFactionId != null && playerFactionId.equals(chunkOwner) && !fctx.isTextMode()) {
      Player playerEntity = store.getComponent(ref, Player.getComponentType());
      if (playerEntity != null) {
        ctx.sendMessage(MessageUtil.info(player, MessageKeys.Claim.ALREADY_YOURS, COLOR_GRAY));
        hyperFactions.getGuiManager().openChunkMap(playerEntity, ref, store, player);
        return;
      }
    }

    // If enemy territory and not text mode, suggest overclaim via map
    if (chunkOwner != null && !chunkOwner.equals(playerFactionId) && !fctx.isTextMode()) {
      boolean isAlly = playerFactionId != null && hyperFactions.getRelationManager().areAllies(playerFactionId, chunkOwner);
      if (isAlly) {
        ctx.sendMessage(MessageUtil.error(player, MessageKeys.Claim.CANNOT_CLAIM_ALLY));
      } else {
        ctx.sendMessage(MessageUtil.error(player, MessageKeys.Claim.ALREADY_CLAIMED_HINT));
      }
      Player playerEntity = store.getComponent(ref, Player.getComponentType());
      if (playerEntity != null) {
        hyperFactions.getGuiManager().openChunkMap(playerEntity, ref, store, player);
      }
      return;
    }

    // Execute claim
    ClaimManager.ClaimResult result = hyperFactions.getClaimManager().claim(
      player.getUuid(), currentWorld.getName(), chunkX, chunkZ
    );

    switch (result) {
      case SUCCESS -> {
        ctx.sendMessage(MessageUtil.success(player, MessageKeys.Claim.SUCCESS, chunkX, chunkZ));
        // Show map after claiming (if not text mode)
        if (!fctx.isTextMode()) {
          Player playerEntity = store.getComponent(ref, Player.getComponentType());
          if (playerEntity != null) {
            hyperFactions.getGuiManager().openChunkMap(playerEntity, ref, store, player);
          }
        }
      }
      case NOT_IN_FACTION -> ctx.sendMessage(MessageUtil.error(player, MessageKeys.Common.NOT_IN_FACTION));
      case NOT_OFFICER -> ctx.sendMessage(MessageUtil.error(player, MessageKeys.Claim.NOT_OFFICER));
      case ALREADY_CLAIMED_SELF -> ctx.sendMessage(MessageUtil.error(player, MessageKeys.Claim.ALREADY_YOURS));
      case ALREADY_CLAIMED_OTHER -> ctx.sendMessage(MessageUtil.error(player, MessageKeys.Claim.ALREADY_CLAIMED));
      case MAX_CLAIMS_REACHED -> ctx.sendMessage(MessageUtil.error(player, MessageKeys.Claim.MAX_CLAIMS));
      case NOT_ADJACENT -> ctx.sendMessage(MessageUtil.error(player, MessageKeys.Claim.NOT_CONNECTED));
      case WORLD_NOT_ALLOWED -> ctx.sendMessage(MessageUtil.error(player, MessageKeys.Claim.WORLD_NOT_ALLOWED));
      case ORBISGUARD_PROTECTED -> ctx.sendMessage(MessageUtil.error(player, MessageKeys.Claim.ORBISGUARD));
      case ZONE_PROTECTED -> ctx.sendMessage(MessageUtil.error(player, MessageKeys.Claim.ZONE_PROTECTED));
      default -> ctx.sendMessage(MessageUtil.error(player, MessageKeys.Claim.FAILED));
    }
  }
}
