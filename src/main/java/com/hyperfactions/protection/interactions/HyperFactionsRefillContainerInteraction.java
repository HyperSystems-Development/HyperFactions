package com.hyperfactions.protection.interactions;

import com.hyperfactions.platform.HyperFactionsPlugin;
import com.hyperfactions.protection.ProtectionChecker;
import com.hyperfactions.protection.ProtectionMessageDebounce;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.iterator.BlockIterator;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.fluid.FluidTicker;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.InteractionConfiguration;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.RefillContainerInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.FluidSection;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Custom interaction codec replacement for RefillContainerInteraction.
 * Adds HyperFactions protection checks (zones + faction territory) before allowing
 * fluid pickup (scooping water/lava with empty buckets).
 *
 * <p>Registered as a codec replacement for "RefillContainer" so all bucket-fill interactions
 * route through our protection system without requiring mixins.
 */
public class HyperFactionsRefillContainerInteraction extends RefillContainerInteraction {

  public static final BuilderCodec<HyperFactionsRefillContainerInteraction> CODEC =
      BuilderCodec.builder(
          HyperFactionsRefillContainerInteraction.class,
          HyperFactionsRefillContainerInteraction::new,
          RefillContainerInteraction.CODEC
      ).build();

  /** First Run. */
  @Override
  protected void firstRun(InteractionType type, InteractionContext context,
              CooldownHandler cooldownHandler) {
    CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
    if (commandBuffer == null) {
      context.getState().state = InteractionState.Failed;
      return;
    }

    World world = commandBuffer.getExternalData().getWorld();
    Ref<EntityStore> ref = context.getEntity();

    Player playerComponent = commandBuffer.getComponent(ref, Player.getComponentType());
    if (playerComponent == null) {
      super.firstRun(type, context, cooldownHandler);
      return;
    }

    PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
    if (playerRef == null) {
      super.firstRun(type, context, cooldownHandler);
      return;
    }

    // Raycast to find the fluid block the player is targeting
    TransformComponent transformComponent = commandBuffer.getComponent(ref, TransformComponent.getComponentType());
    HeadRotation headRotation = commandBuffer.getComponent(ref, HeadRotation.getComponentType());
    ModelComponent modelComponent = commandBuffer.getComponent(ref, ModelComponent.getComponentType());
    ItemStack heldItem = context.getHeldItem();

    if (transformComponent == null || headRotation == null || modelComponent == null || heldItem == null) {
      super.firstRun(type, context, cooldownHandler);
      return;
    }

    InteractionConfiguration interactionConfig = heldItem.getItem().getInteractionConfig();
    float distance = interactionConfig.getUseDistance(playerComponent.getGameMode());
    Vector3d fromPos = transformComponent.getPosition().clone();
    fromPos.y += (double) modelComponent.getModel().getEyeHeight(ref, commandBuffer);
    Vector3d lookDir = headRotation.getDirection();
    Vector3d toPos = fromPos.clone().add(lookDir.scale((double) distance));

    // Find the fluid position via raycast (same logic as vanilla/OrbisGuard)
    AtomicReference<int[]> fluidPos = new AtomicReference<>(null);
    AtomicBoolean hitSolid = new AtomicBoolean(false);
    int[] allowedFluidIds = this.getAllowedFluidIds();

    BlockIterator.iterateFromTo(fromPos, toPos, (x, y, z, px, py, pz, qx, qy, qz) -> {
      Ref section = world.getChunkStore().getChunkSectionReference(
          ChunkUtil.chunkCoordinate(x), ChunkUtil.chunkCoordinate(y), ChunkUtil.chunkCoordinate(z));
      if (section == null) {
        return true;
      }

      BlockSection blockSection = (BlockSection) section.getStore().getComponent(section, BlockSection.getComponentType());
      if (blockSection == null) {
        return true;
      }

      if (FluidTicker.isSolid((BlockType) BlockType.getAssetMap().getAsset(blockSection.get(x, y, z)))) {
        hitSolid.set(true);
        return false;
      }

      FluidSection fluidSection = (FluidSection) section.getStore().getComponent(section, FluidSection.getComponentType());
      if (fluidSection == null) {
        return true;
      }

      int fluidId = fluidSection.getFluidId(x, y, z);
      if (fluidId != 0 && Arrays.binarySearch(allowedFluidIds, fluidId) >= 0) {
        fluidPos.set(new int[]{x, y, z});
        return false;
      }
      return true;
    });

    // If we found a fluid block and didn't hit a solid block first, check protection
    if (fluidPos.get() != null && !hitSolid.get()) {
      int[] pos = fluidPos.get();
      HyperFactionsPlugin plugin = HyperFactionsPlugin.getInstance();
      if (plugin != null) {
        ProtectionChecker checker = plugin.getHyperFactions().getProtectionChecker();
        ProtectionChecker.ProtectionResult result = checker.canInteract(
            playerRef.getUuid(),
            world.getName(),
            pos[0], pos[2],
            ProtectionChecker.InteractionType.BUILD
        );

        if (!checker.isAllowed(result)) {
          Logger.debugProtection("Fluid pickup blocked for %s at (%d,%d,%d) in %s: %s",
              playerRef.getUsername(), pos[0], pos[1], pos[2], world.getName(), result);
          ProtectionMessageDebounce.sendDenial(playerRef, "refill_container",
              checker.getDenialMessage(result, ProtectionChecker.InteractionType.BUILD));
          context.getState().state = InteractionState.Failed;
          return;
        }

        Logger.debugProtection("Fluid pickup allowed for %s at (%d,%d,%d) in %s: %s",
            playerRef.getUsername(), pos[0], pos[1], pos[2], world.getName(), result);
      }
    }

    super.firstRun(type, context, cooldownHandler);
  }
}
