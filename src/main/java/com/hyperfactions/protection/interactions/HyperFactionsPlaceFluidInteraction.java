package com.hyperfactions.protection.interactions;

import com.hyperfactions.platform.HyperFactionsPlugin;
import com.hyperfactions.protection.ProtectionChecker;
import com.hyperfactions.protection.ProtectionMessageDebounce;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.PlaceFluidInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Custom interaction codec replacement for PlaceFluidInteraction.
 * Adds HyperFactions protection checks (zones + faction territory) before allowing
 * fluid placement (water/lava buckets).
 *
 * <p>Registered as a codec replacement for "PlaceFluid" so all bucket interactions
 * route through our protection system without requiring mixins.
 */
public class HyperFactionsPlaceFluidInteraction extends PlaceFluidInteraction {

  public static final BuilderCodec<HyperFactionsPlaceFluidInteraction> CODEC =
      BuilderCodec.builder(
          HyperFactionsPlaceFluidInteraction.class,
          HyperFactionsPlaceFluidInteraction::new,
          PlaceFluidInteraction.CODEC
      ).build();

  /** Interact With Block. */
  @Override
  protected void interactWithBlock(World world, CommandBuffer<EntityStore> commandBuffer,
                   InteractionType type, InteractionContext context,
                   ItemStack itemInHand, Vector3i targetBlock,
                   CooldownHandler cooldownHandler) {
    Ref<EntityStore> ref = context.getEntity();
    PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());

    if (playerRef != null) {
      HyperFactionsPlugin plugin = HyperFactionsPlugin.getInstance();
      if (plugin != null) {
        ProtectionChecker checker = plugin.getHyperFactions().getProtectionChecker();
        ProtectionChecker.ProtectionResult result = checker.canInteract(
            playerRef.getUuid(),
            world.getName(),
            targetBlock.getX(), targetBlock.getZ(),
            ProtectionChecker.InteractionType.BUILD
        );

        if (!checker.isAllowed(result)) {
          Logger.debugProtection("Fluid placement blocked for %s at (%d,%d,%d) in %s: %s",
              playerRef.getUsername(), targetBlock.getX(), targetBlock.getY(),
              targetBlock.getZ(), world.getName(), result);
          ProtectionMessageDebounce.sendIfNotOnCooldown(playerRef, "place_fluid",
              Message.raw(checker.getDenialMessage(result)).color("#FF5555"));
          return;
        }

        Logger.debugProtection("Fluid placement allowed for %s at (%d,%d,%d) in %s: %s",
            playerRef.getUsername(), targetBlock.getX(), targetBlock.getY(),
            targetBlock.getZ(), world.getName(), result);
      }
    }

    super.interactWithBlock(world, commandBuffer, type, context, itemInHand, targetBlock, cooldownHandler);
  }
}
