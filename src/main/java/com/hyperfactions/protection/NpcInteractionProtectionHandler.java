package com.hyperfactions.protection;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Protects NPC interactions (F-key tame, contextual NPC use) in faction territory.
 *
 * <p>Listens to {@link PlayerInteractEvent} (regular event, not ECS) and cancels
 * NPC interactions that are denied by the protection system. This avoids needing
 * a mixin and gives HyperFactions full control over which NPC interaction types
 * to block.
 *
 * <p>Currently blocks all NPC interactions ({@code InteractionType.Use} targeting
 * an entity with {@code NPCEntity} component) in protected territory.
 */
public class NpcInteractionProtectionHandler {

  private final HyperFactions hyperFactions;

  /** Creates a new NpcInteractionProtectionHandler. */
  public NpcInteractionProtectionHandler(@NotNull HyperFactions hyperFactions) {
    this.hyperFactions = hyperFactions;
  }

  /**
   * Handles a player interact event. Cancels NPC interactions in protected territory.
   */
  @SuppressWarnings("deprecation")
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (event.isCancelled()) {
      return;
    }
    if (event.getActionType() != InteractionType.Use) {
      return;
    }

    // Only intercept entity interactions
    Ref<EntityStore> targetRef = event.getTargetRef();
    if (targetRef == null || !targetRef.isValid()) {
      return;
    }

    try {
      Ref<EntityStore> playerRef = event.getPlayerRef();
      Store<EntityStore> store = playerRef.getStore();

      // Check if target is an NPC
      NPCEntity npcComponent = store.getComponent(targetRef, NPCEntity.getComponentType());
      if (npcComponent == null) { // Not an NPC, skip
        return;
      }

      Player player = event.getPlayer();
      UUID playerUuid = player.getUuid();

      World world = store.getExternalData().getWorld();
      if (world == null) {
        return;
      }
      String worldName = world.getName();

      // Get player position for protection check
      TransformComponent transform = store.getComponent(playerRef, TransformComponent.getComponentType());
      if (transform == null) {
        return;
      }
      Vector3d pos = transform.getPosition();

      // Check protection using NPC_TAME type
      ProtectionChecker.ProtectionResult result = hyperFactions.getProtectionChecker().canInteract(
        playerUuid, worldName, pos.getX(), pos.getZ(),
        ProtectionChecker.InteractionType.NPC_TAME
      );

      boolean blocked = !hyperFactions.getProtectionChecker().isAllowed(result);

      Logger.debugInteraction("[NPC:Interact] player=%s, world=%s, pos=(%.0f,%.0f,%.0f), blocked=%b",
        playerUuid, worldName, pos.getX(), pos.getY(), pos.getZ(), blocked);

      if (blocked) {
        event.setCancelled(true);
        String denyMsg = hyperFactions.getProtectionChecker().getDenialMessage(result);
        player.sendMessage(Message.raw(denyMsg).color("#FF5555"));
      }
    } catch (Exception e) {
      // Fail-open for NPC interactions to avoid breaking vanilla gameplay
      Logger.debugInteraction("[NPC:Interact] Error checking protection (fail-open): %s", e.getMessage());
    }
  }
}
