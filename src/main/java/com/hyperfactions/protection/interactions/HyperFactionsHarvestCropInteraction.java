package com.hyperfactions.protection.interactions;

import com.hyperfactions.platform.HyperFactionsPlugin;
import com.hyperfactions.protection.ProtectionChecker;
import com.hyperfactions.protection.ProtectionMessageDebounce;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.builtin.adventure.farming.interactions.HarvestCropInteraction;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Codec replacement for HarvestCropInteraction that adds zone and faction territory
 * protection checks before allowing scythe/tool-based crop harvesting.
 *
 * <p>Scythes use HarvestCropInteraction -> FarmingUtil.harvest(), a completely different
 * code path from F-key harvesting (which goes through BlockHarvestUtils.performPickupByInteraction).
 * F-key harvesting is handled by the OrbisGuard-Mixins harvest hook (BlockHarvestUtilsMixin).
 *
 * <p>This codec replacement serves as defense-in-depth alongside the HarvestCropInteractionMixin
 * (added in OG-Mixins 0.8.3). The mixin checks orbisguard.bypass first; this replacement
 * handles cases where our plugin's codec override is the active one.
 */
public class HyperFactionsHarvestCropInteraction extends HarvestCropInteraction {

  public static final BuilderCodec<HyperFactionsHarvestCropInteraction> CODEC =
      BuilderCodec.builder(
          HyperFactionsHarvestCropInteraction.class,
          HyperFactionsHarvestCropInteraction::new,
          HarvestCropInteraction.CODEC
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
        // Check admin bypass
        if (plugin.getHyperFactions().isAdminBypassEnabled(playerRef.getUuid())) {
          super.interactWithBlock(world, commandBuffer, type, context, itemInHand, targetBlock, cooldownHandler);
          return;
        }

        ProtectionChecker checker = plugin.getHyperFactions().getProtectionChecker();

        // Check zone protection (ITEM_PICKUP_MANUAL flag — crop harvest = manual item acquisition)
        boolean zoneAllows = plugin.getHyperFactions().getZoneInteractionProtection()
            .isManualPickupAllowed(world.getName(), targetBlock.getX(), targetBlock.getZ());
        if (!zoneAllows) {
          ProtectionMessageDebounce.sendIfNotOnCooldown(playerRef, "zone_harvest",
              Message.raw("You cannot harvest plants in this zone.").color("#FF5555"));
          return;
        }

        // Check faction territory protection
        ProtectionChecker.ProtectionResult result = checker.canInteract(
            playerRef.getUuid(),
            world.getName(),
            targetBlock.getX(), targetBlock.getZ(),
            ProtectionChecker.InteractionType.INTERACT
        );

        if (!checker.isAllowed(result)) {
          Logger.debugProtection("Crop harvest blocked for %s at (%d,%d,%d) in %s: %s",
              playerRef.getUsername(), targetBlock.getX(), targetBlock.getY(),
              targetBlock.getZ(), world.getName(), result);
          ProtectionMessageDebounce.sendIfNotOnCooldown(playerRef, "crop_harvest",
              Message.raw(checker.getDenialMessage(result, ProtectionChecker.InteractionType.INTERACT)).color("#FF5555"));
          return;
        }
      }
    }

    super.interactWithBlock(world, commandBuffer, type, context, itemInHand, targetBlock, cooldownHandler);
  }
}
