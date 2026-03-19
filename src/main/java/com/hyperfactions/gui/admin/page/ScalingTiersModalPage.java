package com.hyperfactions.gui.admin.page;

import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.config.modules.EconomyConfig;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.admin.data.ScalingTiersData;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Modal page for editing upkeep scaling tiers.
 * Allows add/remove/edit of tier entries, saves directly to config on confirm.
 * Shows a live cost example that updates as tiers are edited.
 */
public class ScalingTiersModalPage extends InteractiveCustomUIPage<ScalingTiersData> {

  private final PlayerRef playerRef;
  private final GuiManager guiManager;
  private final List<EconomyConfig.ScalingTier> tiers;
  private final AdminConfigPage parentPage;

  /** Creates a new ScalingTiersModalPage. */
  public ScalingTiersModalPage(PlayerRef playerRef, GuiManager guiManager) {
    this(playerRef, guiManager, null);
  }

  /** Creates a new ScalingTiersModalPage that returns to an existing config page. */
  public ScalingTiersModalPage(PlayerRef playerRef, GuiManager guiManager, AdminConfigPage parentPage) {
    super(playerRef, CustomPageLifetime.CanDismiss, ScalingTiersData.CODEC);
    this.playerRef = playerRef;
    this.guiManager = guiManager;
    this.parentPage = parentPage;
    this.tiers = new ArrayList<>(ConfigManager.get().economy().getUpkeepScalingTiers());
  }

  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                     UIEventBuilder events, Store<EntityStore> store) {
    cmd.append(UIPaths.ADMIN_CONFIG_SCALING_MODAL);
    buildTierList(cmd, events);
    updateExample(cmd);

    events.addEventBinding(CustomUIEventBindingType.Activating, "#AddTierBtn",
        EventData.of("Button", "AddTier"), false);
    events.addEventBinding(CustomUIEventBindingType.Activating, "#SaveBtn",
        EventData.of("Button", "Save"), false);
    events.addEventBinding(CustomUIEventBindingType.Activating, "#CancelBtn",
        EventData.of("Button", "Cancel"), false);
  }

  private void buildTierList(UICommandBuilder cmd, UIEventBuilder events) {
    cmd.clear("#TierContainer");
    for (int i = 0; i < tiers.size(); i++) {
      EconomyConfig.ScalingTier tier = tiers.get(i);
      cmd.append("#TierContainer", UIPaths.ADMIN_CONFIG_SCALING_ENTRY);
      String idx = "#TierContainer[" + i + "]";
      cmd.set(idx + " #ChunkInput.Value", String.valueOf(tier.chunkCount()));
      cmd.set(idx + " #CostInput.Value", tier.costPerChunk().toPlainString());

      if (i == 0) cmd.set(idx + " #UpBtn.Disabled", true);
      events.addEventBinding(CustomUIEventBindingType.Activating, idx + " #UpBtn",
          EventData.of("Button", "MoveTierUp").append("TierIndex", String.valueOf(i)), false);
      if (i == tiers.size() - 1) cmd.set(idx + " #DownBtn.Disabled", true);
      events.addEventBinding(CustomUIEventBindingType.Activating, idx + " #DownBtn",
          EventData.of("Button", "MoveTierDown").append("TierIndex", String.valueOf(i)), false);
      events.addEventBinding(CustomUIEventBindingType.Activating, idx + " #RemoveBtn",
          EventData.of("Button", "RemoveTier").append("TierIndex", String.valueOf(i)), false);
      events.addEventBinding(CustomUIEventBindingType.ValueChanged, idx + " #ChunkInput",
          EventData.of("Button", "EditChunk").append("TierIndex", String.valueOf(i))
              .append("@chunkInput", idx + " #ChunkInput.Value"), false);
      events.addEventBinding(CustomUIEventBindingType.ValueChanged, idx + " #CostInput",
          EventData.of("Button", "EditCost").append("TierIndex", String.valueOf(i))
              .append("@costInput", idx + " #CostInput.Value"), false);
    }
  }

  /** Calculates and updates the cost example label using total tier chunks + 10. */
  private void updateExample(UICommandBuilder cmd) {
    int freeChunks = ConfigManager.get().economy().getUpkeepFreeChunks();
    // Dynamic example: sum of all tier chunk counts + 10 overflow
    int tierTotal = 0;
    for (EconomyConfig.ScalingTier tier : tiers) {
      tierTotal += tier.chunkCount();
    }
    int exampleChunks = tierTotal + 10 + freeChunks;
    int billable = Math.max(0, exampleChunks - freeChunks);
    BigDecimal cost = calculateTieredCost(billable);
    String symbol = ConfigManager.get().economy().getCurrencySymbol();
    cmd.set("#ExampleLabel.Text",
        "Example: " + exampleChunks + " chunks (" + freeChunks + " free, "
        + billable + " billable) = " + symbol
        + cost.setScale(2, RoundingMode.HALF_UP).toPlainString() + "/cycle");
  }

  /** Calculates the total cost for a given number of billable chunks using current tiers. */
  private BigDecimal calculateTieredCost(int billableChunks) {
    BigDecimal total = BigDecimal.ZERO;
    int remaining = billableChunks;

    for (EconomyConfig.ScalingTier tier : tiers) {
      if (remaining <= 0) break;
      int count = tier.chunkCount() > 0 ? Math.min(remaining, tier.chunkCount()) : remaining;
      total = total.add(tier.costPerChunk().multiply(BigDecimal.valueOf(count)));
      remaining -= count;
    }

    // If chunks remain after all tiers, use the last tier's cost
    if (remaining > 0 && !tiers.isEmpty()) {
      BigDecimal lastCost = tiers.getLast().costPerChunk();
      total = total.add(lastCost.multiply(BigDecimal.valueOf(remaining)));
    }

    return total;
  }

  @Override
  public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                               ScalingTiersData data) {
    super.handleDataEvent(ref, store, data);

    Player player = store.getComponent(ref, Player.getComponentType());
    PlayerRef pRef = store.getComponent(ref, PlayerRef.getComponentType());
    if (player == null || pRef == null || data.button == null) return;

    switch (data.button) {
      case "Cancel" -> {
        returnToParent(player, ref, store, pRef);
      }

      case "Save" -> {
        ConfigManager cfg = ConfigManager.get();
        cfg.economy().setUpkeepScalingTiers(new ArrayList<>(tiers));
        cfg.saveAll();
        Logger.info("[ConfigEditor] Scaling tiers saved by admin (%d tiers)", tiers.size());
        returnToParent(player, ref, store, pRef);
      }

      case "AddTier" -> {
        tiers.add(new EconomyConfig.ScalingTier(0, new BigDecimal("1.00")));
        refresh(ref, store);
      }

      case "MoveTierUp" -> {
        int idx = parseIndex(data.tierIndex);
        if (idx > 0 && idx < tiers.size()) {
          EconomyConfig.ScalingTier tier = tiers.remove(idx);
          tiers.add(idx - 1, tier);
          refresh(ref, store);
        }
      }

      case "MoveTierDown" -> {
        int idx = parseIndex(data.tierIndex);
        if (idx >= 0 && idx < tiers.size() - 1) {
          EconomyConfig.ScalingTier tier = tiers.remove(idx);
          tiers.add(idx + 1, tier);
          refresh(ref, store);
        }
      }

      case "RemoveTier" -> {
        int idx = parseIndex(data.tierIndex);
        if (idx >= 0 && idx < tiers.size()) {
          tiers.remove(idx);
          refresh(ref, store);
        }
      }

      case "EditChunk" -> {
        int idx = parseIndex(data.tierIndex);
        if (idx >= 0 && idx < tiers.size() && data.chunkInput != null) {
          try {
            int chunks = Integer.parseInt(data.chunkInput.trim());
            chunks = Math.max(0, chunks);
            EconomyConfig.ScalingTier old = tiers.get(idx);
            tiers.set(idx, new EconomyConfig.ScalingTier(chunks, old.costPerChunk()));
          } catch (NumberFormatException ignored) {}
        }
        refreshExample(ref, store);
      }

      case "EditCost" -> {
        int idx = parseIndex(data.tierIndex);
        if (idx >= 0 && idx < tiers.size() && data.costInput != null) {
          try {
            BigDecimal cost = new BigDecimal(data.costInput.trim());
            if (cost.compareTo(BigDecimal.ZERO) < 0) cost = BigDecimal.ZERO;
            EconomyConfig.ScalingTier old = tiers.get(idx);
            tiers.set(idx, new EconomyConfig.ScalingTier(old.chunkCount(), cost));
          } catch (NumberFormatException ignored) {}
        }
        refreshExample(ref, store);
      }
    }
  }

  private void refresh(Ref<EntityStore> ref, Store<EntityStore> store) {
    UICommandBuilder cmd = new UICommandBuilder();
    UIEventBuilder events = new UIEventBuilder();
    buildTierList(cmd, events);
    updateExample(cmd);
    events.addEventBinding(CustomUIEventBindingType.Activating, "#AddTierBtn",
        EventData.of("Button", "AddTier"), false);
    events.addEventBinding(CustomUIEventBindingType.Activating, "#SaveBtn",
        EventData.of("Button", "Save"), false);
    events.addEventBinding(CustomUIEventBindingType.Activating, "#CancelBtn",
        EventData.of("Button", "Cancel"), false);
    sendUpdate(cmd, events, false);
  }

  /** Updates just the example label without rebuilding the tier list. */
  private void refreshExample(Ref<EntityStore> ref, Store<EntityStore> store) {
    UICommandBuilder cmd = new UICommandBuilder();
    UIEventBuilder events = new UIEventBuilder();
    updateExample(cmd);
    sendUpdate(cmd, events, false);
  }

  /** Returns to the parent config page (preserving pending state) or opens a fresh one. */
  private void returnToParent(Player player, Ref<EntityStore> ref,
                               Store<EntityStore> store, PlayerRef pRef) {
    if (parentPage != null) {
      player.getPageManager().openCustomPage(ref, store, parentPage);
    } else {
      guiManager.openAdminConfig(player, ref, store, pRef, "economy");
    }
  }

  private static int parseIndex(String s) {
    if (s == null) return -1;
    try { return Integer.parseInt(s); }
    catch (NumberFormatException e) { return -1; }
  }
}
