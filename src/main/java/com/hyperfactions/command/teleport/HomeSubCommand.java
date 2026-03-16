package com.hyperfactions.command.teleport;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.api.events.EventBus;
import com.hyperfactions.api.events.FactionHomeTeleportEvent;
import com.hyperfactions.api.events.FactionHomeTeleportPreEvent;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.data.Faction;
import com.hyperfactions.manager.TeleportManager;
import com.hyperfactions.platform.HyperFactionsPlugin;
import com.hyperfactions.util.CommandKeys;
import com.hyperfactions.util.CommonKeys;
import com.hyperfactions.util.MessageUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Subcommand: /f home
 * Teleports to the faction home.
 */
public class HomeSubCommand extends FactionSubCommand {

  /** Creates a new HomeSubCommand. */
  public HomeSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
    super("home", "Teleport to faction home", hyperFactions, plugin);
  }

  /** Executes the command. */
  @Override
  protected void execute(@NotNull CommandContext ctx,
             @NotNull Store<EntityStore> store,
             @NotNull Ref<EntityStore> ref,
             @NotNull PlayerRef player,
             @NotNull World currentWorld) {

    if (!hasPermission(player, Permissions.HOME)) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Home.NO_PERMISSION));
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
    UUID playerUuid = player.getUuid();

    // Create start location for movement checking
    TeleportManager.StartLocation startLoc = new TeleportManager.StartLocation(
      currentWorld.getName(), pos.getX(), pos.getY(), pos.getZ()
    );

    // Pre-event: allow external plugins to cancel the teleport
    Faction.FactionHome home = faction.home();
    if (home != null && EventBus.publishCancellable(new FactionHomeTeleportPreEvent(
        playerUuid, faction.id(),
        currentWorld.getName(), pos.getX(), pos.getY(), pos.getZ(),
        home.world(), home.x(), home.y(), home.z()))) {
      ctx.sendMessage(MessageUtil.error(player, CommonKeys.Common.NO_PERMISSION));
      return;
    }

    // Call TeleportManager
    // - For instant teleport (warmup=0): doTeleport is called immediately
    // - For warmup teleport: destination is stored, TerritoryTickingSystem executes later
    TeleportManager.TeleportResult result = hyperFactions.getTeleportManager().teleportToHome(
      playerUuid,
      startLoc,
      // Teleport executor (only used for instant teleport when warmup=0)
      targetFaction -> executeTeleport(store, ref, currentWorld, targetFaction),
      // Message sender
      ctx::sendMessage,
      // Combat tag checker
      () -> hyperFactions.getCombatTagManager().isTagged(playerUuid)
    );

    // Handle immediate results (warmup teleports are handled by TerritoryTickingSystem)
    switch (result) {
      case NOT_IN_FACTION -> ctx.sendMessage(MessageUtil.error(player, CommonKeys.Common.NOT_IN_FACTION));
      case NO_HOME -> ctx.sendMessage(MessageUtil.error(player, CommandKeys.Home.NO_HOME));
      case COMBAT_TAGGED -> ctx.sendMessage(MessageUtil.error(player, CommandKeys.Home.COMBAT_TAGGED));
      case ON_COOLDOWN -> {} // Message sent by TeleportManager
      case SUCCESS_INSTANT -> {
        ctx.sendMessage(MessageUtil.success(player, CommandKeys.Home.TELEPORTED));
        // Emit event for integrations (e.g. HyperEssentials back tracking)
        Faction.FactionHome fHome = faction.home();
        if (fHome != null) {
          EventBus.publish(new FactionHomeTeleportEvent(
            playerUuid, faction.id(),
            currentWorld.getName(), pos.getX(), pos.getY(), pos.getZ(),
            fHome.world(), fHome.x(), fHome.y(), fHome.z(), fHome.yaw(), fHome.pitch()
          ));
        }
      }
      case SUCCESS_WARMUP -> {} // Message sent by TeleportManager, teleport executed by TerritoryTickingSystem
      default -> {}
    }
  }

  /**
   * Executes the actual teleport to faction home using the proper Teleport component.
   * Only called for instant teleport (warmup=0).
   */
  private TeleportManager.TeleportResult executeTeleport(Store<EntityStore> store, Ref<EntityStore> ref,
                             World currentWorld, Faction faction) {
    Faction.FactionHome home = faction.home();
    if (home == null) {
      return TeleportManager.TeleportResult.NO_HOME;
    }

    // Get target world (supports cross-world teleportation)
    World targetWorld;
    if (currentWorld.getName().equals(home.world())) {
      targetWorld = currentWorld;
    } else {
      targetWorld = Universe.get().getWorld(home.world());
      if (targetWorld == null) {
        return TeleportManager.TeleportResult.WORLD_NOT_FOUND;
      }
    }

    // Execute on the player's CURRENT world thread (store belongs to this thread).
    // Pass targetWorld to createForPlayer so TeleportSystems handles the cross-world move.
    currentWorld.execute(() -> {
      Vector3d position = new Vector3d(home.x(), home.y(), home.z());
      Vector3f rotation = new Vector3f(home.pitch(), home.yaw(), 0);
      Teleport teleport = Teleport.createForPlayer(targetWorld, position, rotation);
      store.addComponent(ref, Teleport.getComponentType(), teleport);
    });

    return TeleportManager.TeleportResult.SUCCESS_INSTANT;
  }
}
