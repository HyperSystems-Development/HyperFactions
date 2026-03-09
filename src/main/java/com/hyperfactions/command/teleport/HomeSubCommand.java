package com.hyperfactions.command.teleport;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.api.events.EventBus;
import com.hyperfactions.api.events.FactionHomeTeleportEvent;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.data.Faction;
import com.hyperfactions.manager.TeleportManager;
import com.hyperfactions.platform.HyperFactionsPlugin;
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
      ctx.sendMessage(prefix().insert(msg("You don't have permission to teleport to faction home.", COLOR_RED)));
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
      case NOT_IN_FACTION -> ctx.sendMessage(MessageUtil.error("You are not in a faction."));
      case NO_HOME -> ctx.sendMessage(prefix().insert(msg("Your faction has no home set.", COLOR_RED)));
      case COMBAT_TAGGED -> ctx.sendMessage(prefix().insert(msg("You cannot teleport while in combat!", COLOR_RED)));
      case ON_COOLDOWN -> {} // Message sent by TeleportManager
      case SUCCESS_INSTANT -> {
        ctx.sendMessage(prefix().insert(msg("Teleported to faction home!", COLOR_GREEN)));
        // Emit event for integrations (e.g. HyperEssentials back tracking)
        Faction.FactionHome home = faction.home();
        if (home != null) {
          EventBus.publish(new FactionHomeTeleportEvent(
            playerUuid, faction.id(),
            currentWorld.getName(), pos.getX(), pos.getY(), pos.getZ(),
            home.world(), home.x(), home.y(), home.z(), home.yaw(), home.pitch()
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
