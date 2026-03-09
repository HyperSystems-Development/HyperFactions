package com.hyperfactions.gui.admin.page;

import com.hyperfactions.api.EconomyAPI;
import com.hyperfactions.data.Faction;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.admin.AdminNavBarHelper;
import com.hyperfactions.gui.admin.data.AdminBulkEconomyData;
import com.hyperfactions.manager.EconomyManager;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.util.ErrorHandler;
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
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Admin Bulk Economy modal - adjusts all faction treasuries at once.
 * Supports add/remove operations applied to every faction with a treasury.
 */
public class AdminBulkEconomyPage extends InteractiveCustomUIPage<AdminBulkEconomyData> {

  private final PlayerRef playerRef;

  private final FactionManager factionManager;

  private final EconomyManager economyManager;

  private final GuiManager guiManager;

  /** Creates a new AdminBulkEconomyPage. */
  public AdminBulkEconomyPage(PlayerRef playerRef,
                FactionManager factionManager,
                EconomyManager economyManager,
                GuiManager guiManager) {
    super(playerRef, CustomPageLifetime.CanDismiss, AdminBulkEconomyData.CODEC);
    this.playerRef = playerRef;
    this.factionManager = factionManager;
    this.economyManager = economyManager;
    this.guiManager = guiManager;
  }

  /** Builds the page. */
  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {

    cmd.append(UIPaths.ADMIN_BULK_ECONOMY);

    AdminNavBarHelper.setupBar(playerRef, "actions", cmd, events);

    int factionCount = economyManager.getFactionEconomyCount();
    BigDecimal totalBalance = economyManager.getServerTotalBalance();

    cmd.set("#FactionCount.Text", String.valueOf(factionCount));
    cmd.set("#TotalBalance.Text", economyManager.formatCurrency(totalBalance));

    // Confirm button
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#ConfirmBtn",
        EventData.of("Button", "Confirm")
            .append("@Amount", "#AmountInput.Value"),
        false
    );

    // Back button
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#BackBtn",
        EventData.of("Button", "Back"),
        false
    );
  }

  /** Handles data event. */
  @Override
  public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                AdminBulkEconomyData data) {
    super.handleDataEvent(ref, store, data);

    Player player = store.getComponent(ref, Player.getComponentType());
    PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

    if (player == null || playerRef == null) {
      return;
    }

    if (AdminNavBarHelper.handleNavEvent(data, player, ref, store, playerRef, guiManager)) {
      return;
    }

    if (data.button == null) {
      return;
    }

    switch (data.button) {
      case "Confirm" -> {
        BigDecimal amount = parseAmountOrError(data.amount);
        if (amount == null) {
          return;
        }

        if (amount.compareTo(BigDecimal.ZERO) == 0) {
          showError("Amount cannot be zero.");
          return;
        }

        String action = amount.compareTo(BigDecimal.ZERO) > 0 ? "added" : "removed";
        String desc = "Admin bulk " + action + " " + economyManager.formatCurrency(amount.abs());

        Logger.info("[Admin] %s bulk adjusting all factions by %s",
            playerRef.getUsername(), amount.toPlainString());

        Collection<Faction> allFactions = factionManager.getAllFactions();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // Process all factions
        CompletableFuture<?>[] futures = allFactions.stream()
            .filter(f -> economyManager.getEconomy(f.id()) != null)
            .map(faction -> economyManager.adminAdjust(faction.id(), amount, playerRef.getUuid(), desc)
                .thenAccept(result -> {
                  if (result == EconomyAPI.TransactionResult.SUCCESS) {
                    successCount.incrementAndGet();
                  } else {
                    failCount.incrementAndGet();
                  }
                })
                .exceptionally(ex -> {
                  failCount.incrementAndGet();
                  return null;
                }))
            .toArray(CompletableFuture[]::new);

        ErrorHandler.guard("Bulk economy adjustment completion", CompletableFuture.allOf(futures)).thenRun(() -> {
          String msg = String.format("Bulk adjust complete: %s %s to %d factions",
              action, economyManager.formatCurrency(amount.abs()), successCount.get());
          if (failCount.get() > 0) {
            msg += String.format(" (%d failed)", failCount.get());
          }
          Logger.info("[Admin] %s", msg);

          // Reopen the actions page
          guiManager.openAdminActions(player, ref, store, playerRef);
        });
      }

      case "Back" -> guiManager.openAdminActions(player, ref, store, playerRef);
      default -> { }
    }
  }

  private BigDecimal parseAmountOrError(String amount) {
    if (amount == null || amount.isBlank()) {
      showError("Please enter an amount.");
      return null;
    }
    try {
      return new BigDecimal(amount.trim());
    } catch (NumberFormatException e) {
      showError("Invalid number: " + amount);
      return null;
    }
  }

  private void showError(String message) {
    UICommandBuilder cmd = new UICommandBuilder();
    cmd.set("#ErrorMessage.Text", message);
    sendUpdate(cmd, new UIEventBuilder(), false);
  }
}
