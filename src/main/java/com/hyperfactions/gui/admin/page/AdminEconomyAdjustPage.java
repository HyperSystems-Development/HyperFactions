package com.hyperfactions.gui.admin.page;

import com.hyperfactions.util.HFMessages;
import com.hyperfactions.util.MessageKeys;

import com.hyperfactions.api.EconomyAPI;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionEconomy;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.admin.AdminNavBarHelper;
import com.hyperfactions.gui.admin.data.AdminEconomyAdjustData;
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
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * Admin Economy Adjust modal - allows adjusting a faction's treasury balance.
 */
public class AdminEconomyAdjustPage extends InteractiveCustomUIPage<AdminEconomyAdjustData> {

  private final PlayerRef playerRef;

  private final FactionManager factionManager;

  private final EconomyManager economyManager;

  private final GuiManager guiManager;

  private final UUID factionId;

  /** Creates a new AdminEconomyAdjustPage. */
  public AdminEconomyAdjustPage(PlayerRef playerRef,
                 FactionManager factionManager,
                 EconomyManager economyManager,
                 GuiManager guiManager,
                 UUID factionId) {
    super(playerRef, CustomPageLifetime.CanDismiss, AdminEconomyAdjustData.CODEC);
    this.playerRef = playerRef;
    this.factionManager = factionManager;
    this.economyManager = economyManager;
    this.guiManager = guiManager;
    this.factionId = factionId;
  }

  /** Builds . */
  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {

    // Load template
    cmd.append(UIPaths.ADMIN_ECONOMY_ADJUST);

    // Setup admin nav bar
    AdminNavBarHelper.setupBar(playerRef, "economy", cmd, events);

    // Localize labels
    cmd.set("#Title.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_TITLE_ECONOMY_ADJUST));
    cmd.set("#SectionHeader.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_ECADJ_HEADER));
    cmd.set("#FactionLabel.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_ECADJ_FACTION_LABEL));
    cmd.set("#CurrentBalanceLabel.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_ECADJ_CURRENT_BALANCE));
    cmd.set("#AmountLabel.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_ECADJ_AMOUNT_HINT));
    cmd.set("#HintText.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_ECADJ_PREVIEW_HINT));
    cmd.set("#AdjustmentLabel.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_ECADJ_ADJUSTMENT));
    cmd.set("#NewBalanceLabel.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_ECADJ_NEW_BALANCE));
    cmd.set("#BackBtn.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_BACK));
    cmd.set("#SetBalanceBtn.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_ECADJ_SET_BALANCE));
    cmd.set("#ConfirmBtn.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_ECADJ_CONFIRM));

    // Get faction info
    Faction faction = factionManager.getFaction(factionId);
    if (faction == null) {
      cmd.set("#TargetFactionName.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.FACTION_NOT_FOUND_LABEL));
      cmd.set("#CurrentBalance.Text", HFMessages.get(playerRef, MessageKeys.Common.NA));
      return;
    }

    FactionEconomy economy = economyManager.getEconomy(factionId);
    BigDecimal currentBalance = economy != null ? economy.balance() : BigDecimal.ZERO;

    cmd.set("#TargetFactionName.Text", faction.name());
    cmd.set("#CurrentBalance.Text", economyManager.formatCurrency(currentBalance));

    // Preview event — fires when amount changes
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#ConfirmBtn",
        EventData.of("Button", "Confirm")
            .append("@Amount", "#AmountInput.Value"),
        false
    );

    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#SetBalanceBtn",
        EventData.of("Button", "SetBalance")
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
                AdminEconomyAdjustData data) {
    super.handleDataEvent(ref, store, data);

    Player player = store.getComponent(ref, Player.getComponentType());
    PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

    if (player == null || playerRef == null) {
      return;
    }

    // Handle admin nav bar navigation
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
          showError(HFMessages.get(playerRef, MessageKeys.AdminGui.ECON_AMOUNT_ZERO));
          return;
        }

        String desc = amount.compareTo(BigDecimal.ZERO) >= 0
            ? "Admin added " + economyManager.formatCurrency(amount)
            : "Admin deducted " + economyManager.formatCurrency(amount.abs());

        Logger.debugEconomy("Admin adjust: faction=%s amount=%s by=%s",
            factionId, amount.toPlainString(), playerRef.getUuid());

        economyManager.adminAdjust(factionId, amount, playerRef.getUuid(), desc)
            .thenAccept(result -> handleResult(result, player, ref, store, playerRef))
            .exceptionally(ex -> {
              ErrorHandler.report(String.format("Admin economy adjust failed for faction %s", factionId), ex);
              showError(HFMessages.get(playerRef, MessageKeys.AdminGui.ECON_ERROR));
              return null;
            });
      }

      case "SetBalance" -> {
        BigDecimal newBalance = parseAmountOrError(data.amount);
        if (newBalance == null) {
          return;
        }

        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
          showError(HFMessages.get(playerRef, MessageKeys.AdminGui.ECON_BALANCE_NEGATIVE));
          return;
        }

        Logger.debugEconomy("Admin set balance: faction=%s newBalance=%s by=%s",
            factionId, newBalance.toPlainString(), playerRef.getUuid());

        economyManager.setBalance(factionId, newBalance, playerRef.getUuid())
            .thenAccept(result -> handleResult(result, player, ref, store, playerRef))
            .exceptionally(ex -> {
              ErrorHandler.report(String.format("Admin economy set balance failed for faction %s", factionId), ex);
              showError(HFMessages.get(playerRef, MessageKeys.AdminGui.ECON_ERROR));
              return null;
            });
      }

      case "Back" -> guiManager.openAdminEconomy(player, ref, store, playerRef);
      default -> throw new IllegalStateException("Unexpected value");
    }
  }

  /**
   * Parses the amount string from the UI input.
   * Shows an error message and returns null if invalid.
   */
  private @Nullable BigDecimal parseAmountOrError(@Nullable String amount) {
    if (amount == null || amount.isBlank()) {
      showError(HFMessages.get(playerRef, MessageKeys.AdminGui.ECON_ENTER_AMOUNT));
      return null;
    }
    try {
      return new BigDecimal(amount.trim());
    } catch (NumberFormatException e) {
      showError(HFMessages.get(playerRef, MessageKeys.AdminGui.ECON_INVALID_NUMBER, amount));
      return null;
    }
  }

  private void handleResult(EconomyAPI.TransactionResult result,
               Player player, Ref<EntityStore> ref,
               Store<EntityStore> store, PlayerRef playerRef) {
    if (result == EconomyAPI.TransactionResult.SUCCESS) {
      Logger.debugEconomy("Admin economy operation succeeded for faction %s", factionId);
      guiManager.openAdminEconomy(player, ref, store, playerRef);
    } else {
      Logger.debugEconomy("Admin economy operation failed for faction %s: %s", factionId, result.name());
      showError(HFMessages.get(playerRef, MessageKeys.AdminGui.ECON_FAILED, result.name()));
    }
  }

  private void showError(String message) {
    UICommandBuilder cmd = new UICommandBuilder();
    cmd.set("#ErrorMessage.Text", message);
    sendUpdate(cmd, new UIEventBuilder(), false);
  }
}
