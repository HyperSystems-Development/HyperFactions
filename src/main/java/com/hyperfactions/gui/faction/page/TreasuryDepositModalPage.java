package com.hyperfactions.gui.faction.page;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.api.EconomyAPI;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionEconomy;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.data.FactionPermissions;
import com.hyperfactions.data.FactionRole;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.faction.data.DepositModalData;
import com.hyperfactions.integration.PermissionManager;
import com.hyperfactions.integration.economy.VaultEconomyProvider;
import com.hyperfactions.manager.EconomyManager;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.util.MessageUtil;
import com.hyperfactions.util.UiUtil;
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

/**
 * Deposit/Withdraw modal with fee preview and confirmation.
 * Mode is "deposit" or "withdraw" — controls title, labels, and action.
 */
public class TreasuryDepositModalPage extends InteractiveCustomUIPage<DepositModalData> {

  private final PlayerRef playerRef;

  private final FactionManager factionManager;

  private final EconomyManager economyManager;

  private final GuiManager guiManager;

  private final HyperFactions plugin;

  private final Faction faction;

  private final String mode; // "deposit" or "withdraw"

  /** Creates a new TreasuryDepositModalPage. */
  public TreasuryDepositModalPage(PlayerRef playerRef,
                  FactionManager factionManager,
                  EconomyManager economyManager,
                  GuiManager guiManager,
                  HyperFactions plugin,
                  Faction faction,
                  String mode) {
    super(playerRef, CustomPageLifetime.CanDismiss, DepositModalData.CODEC);
    this.playerRef = playerRef;
    this.factionManager = factionManager;
    this.economyManager = economyManager;
    this.guiManager = guiManager;
    this.plugin = plugin;
    this.faction = faction;
    this.mode = mode;
  }

  /** Builds . */
  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {

    cmd.append(UIPaths.TREASURY_DEPOSIT_MODAL);

    boolean isDeposit = "deposit".equals(mode);
    UUID uuid = playerRef.getUuid();

    // Set mode subtitle
    cmd.set("#ModeLabel.Text", isDeposit ? "Deposit to Treasury" : "Withdraw from Treasury");

    // Set balances
    VaultEconomyProvider vault = economyManager.getVaultProvider();
    cmd.set("#WalletLabel.Text", "Your wallet: " + economyManager.formatCurrency(vault.getBalanceBigDecimal(uuid)));

    FactionEconomy economy = economyManager.getEconomy(faction.id());
    BigDecimal treasuryBalance = economy != null ? economy.balance() : BigDecimal.ZERO;
    cmd.set("#TreasuryLabel.Text", "Treasury balance: " + economyManager.formatCurrency(treasuryBalance));

    // Fee label
    EconomyAPI.TransactionType txType = isDeposit ? EconomyAPI.TransactionType.DEPOSIT : EconomyAPI.TransactionType.WITHDRAW;
    BigDecimal feePercent = isDeposit ? ConfigManager.get().getDepositFeePercent() : ConfigManager.get().getWithdrawFeePercent();
    cmd.set("#FeeLabel.Text", "Fee (" + feePercent.toPlainString() + "%):");

    // Confirm button text
    cmd.set("#ConfirmBtn.Text", isDeposit ? "Confirm Deposit" : "Confirm Withdrawal");

    // Check withdraw permission
    if (!isDeposit) {
      FactionMember member = faction.getMember(uuid);
      boolean isLeader = member != null && member.role() == FactionRole.LEADER;
      boolean isOfficerOrHigher = member != null && member.isOfficerOrHigher();
      FactionPermissions perms = faction.getEffectivePermissions();
      boolean canWithdraw = isLeader
          || (isOfficerOrHigher && perms.get(FactionPermissions.TREASURY_WITHDRAW));
      if (!canWithdraw) {
        cmd.set("#ConfirmBtn.Disabled", true);
      }
    }

    // Cancel button
    events.addEventBinding(CustomUIEventBindingType.Activating, "#CancelBtn",
        EventData.of("Button", "Cancel"), false);

    // Preview on value change
    events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#AmountInput",
        EventData.of("Button", "Preview").append("@Amount", "#AmountInput.Value"), false);

    // Confirm button
    events.addEventBinding(CustomUIEventBindingType.Activating, "#ConfirmBtn",
        EventData.of("Button", "Confirm").append("@Amount", "#AmountInput.Value"), false);
  }

  /** Handles data event. */
  @Override
  public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                DepositModalData data) {
    super.handleDataEvent(ref, store, data);

    Player player = store.getComponent(ref, Player.getComponentType());
    PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

    if (player == null || playerRef == null || data.button == null) {
      sendUpdate();
      return;
    }

    switch (data.button) {
      case "Cancel" -> {
        Faction fresh = factionManager.getFaction(faction.id());
        if (fresh != null) {
          guiManager.openFactionTreasury(player, ref, store, playerRef, fresh);
        }
      }
      case "Preview" -> handlePreview(data);
      case "Confirm" -> handleConfirm(player, ref, store, playerRef, data);
      default -> sendUpdate();
    }
  }

  private void handlePreview(DepositModalData data) {
    BigDecimal amount = UiUtil.parseAmount(data.amount);
    boolean isDeposit = "deposit".equals(mode);
    EconomyAPI.TransactionType txType = isDeposit ? EconomyAPI.TransactionType.DEPOSIT : EconomyAPI.TransactionType.WITHDRAW;

    UICommandBuilder cmd = new UICommandBuilder();
    UIEventBuilder events = new UIEventBuilder();

    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      cmd.set("#FeeAmount.Text", "--");
      cmd.set("#FeeValue.Text", "--");
      cmd.set("#FeeTotal.Text", "--");
    } else {
      BigDecimal fee = economyManager.calculateFee(amount, txType);
      BigDecimal total;
      if (isDeposit) {
        // Deposit: player pays amount + fee from wallet
        total = amount.add(fee);
      } else {
        // Withdraw: player receives amount - fee
        total = amount;
      }
      cmd.set("#FeeAmount.Text", economyManager.formatCurrency(amount));
      cmd.set("#FeeValue.Text", fee.compareTo(BigDecimal.ZERO) > 0 ? "-" + economyManager.formatCurrency(fee) : economyManager.formatCurrency(BigDecimal.ZERO));
      if (isDeposit) {
        cmd.set("#FeeTotal.Text", economyManager.formatCurrency(total) + " from wallet");
      } else {
        BigDecimal net = amount.subtract(fee);
        cmd.set("#FeeTotal.Text", economyManager.formatCurrency(net) + " to wallet");
      }
    }

    // Re-bind events to preserve interactivity
    events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#AmountInput",
        EventData.of("Button", "Preview").append("@Amount", "#AmountInput.Value"), false);
    events.addEventBinding(CustomUIEventBindingType.Activating, "#CancelBtn",
        EventData.of("Button", "Cancel"), false);
    events.addEventBinding(CustomUIEventBindingType.Activating, "#ConfirmBtn",
        EventData.of("Button", "Confirm").append("@Amount", "#AmountInput.Value"), false);

    sendUpdate(cmd, events, false);
  }

  private void handleConfirm(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                PlayerRef playerRef, DepositModalData data) {
    UUID uuid = playerRef.getUuid();
    BigDecimal amount = UiUtil.parseAmount(data.amount);
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      player.sendMessage(MessageUtil.errorText("Enter a valid positive amount."));
      sendUpdate();
      return;
    }

    boolean isDeposit = "deposit".equals(mode);
    EconomyAPI.TransactionType txType = isDeposit ? EconomyAPI.TransactionType.DEPOSIT : EconomyAPI.TransactionType.WITHDRAW;
    BigDecimal fee = economyManager.calculateFee(amount, txType);

    if (isDeposit) {
      handleDepositConfirm(player, ref, store, playerRef, uuid, amount, fee);
    } else {
      handleWithdrawConfirm(player, ref, store, playerRef, uuid, amount, fee);
    }
  }

  private void handleDepositConfirm(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                   PlayerRef playerRef, UUID uuid, BigDecimal amount, BigDecimal fee) {
    BigDecimal totalFromWallet = amount.add(fee);
    VaultEconomyProvider vault = economyManager.getVaultProvider();

    if (!vault.has(uuid, totalFromWallet)) {
      player.sendMessage(MessageUtil.errorText(
          "Insufficient wallet funds. Need " + economyManager.formatCurrency(totalFromWallet)
          + ", have " + economyManager.formatCurrency(vault.getBalanceBigDecimal(uuid))));
      sendUpdate();
      return;
    }

    if (!vault.withdraw(uuid, totalFromWallet)) {
      player.sendMessage(MessageUtil.errorText("Failed to withdraw from your wallet."));
      sendUpdate();
      return;
    }

    EconomyAPI.TransactionResult result = economyManager.deposit(
        faction.id(), amount, uuid, "Player deposit").join();

    if (result != EconomyAPI.TransactionResult.SUCCESS) {
      vault.deposit(uuid, totalFromWallet); // Rollback
      player.sendMessage(MessageUtil.errorText("Failed to deposit. Money returned."));
      sendUpdate();
      return;
    }

    String msg = "Deposited " + economyManager.formatCurrency(amount) + " into the treasury.";
    if (fee.compareTo(BigDecimal.ZERO) > 0) {
      msg += " (fee: " + economyManager.formatCurrency(fee) + ")";
    }
    player.sendMessage(MessageUtil.successText(msg));

    Faction fresh = factionManager.getFaction(faction.id());
    if (fresh != null) {
      guiManager.openFactionTreasury(player, ref, store, playerRef, fresh);
    }
  }

  private void handleWithdrawConfirm(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                    PlayerRef playerRef, UUID uuid, BigDecimal amount, BigDecimal fee) {
    // Permission check
    if (!PermissionManager.get().hasPermission(uuid, Permissions.ECONOMY_WITHDRAW)) {
      player.sendMessage(MessageUtil.errorText("You don't have permission to withdraw."));
      sendUpdate();
      return;
    }

    // Limit check
    String limitReason = economyManager.checkWithdrawLimits(faction.id(), amount);
    if (limitReason != null) {
      player.sendMessage(MessageUtil.errorText("Withdrawal denied: " + limitReason));
      sendUpdate();
      return;
    }

    EconomyAPI.TransactionResult result = economyManager.withdraw(
        faction.id(), amount, uuid, "Player withdrawal").join();

    if (result != EconomyAPI.TransactionResult.SUCCESS) {
      switch (result) {
        case INSUFFICIENT_FUNDS ->
          player.sendMessage(MessageUtil.errorText("Insufficient funds in treasury."));
        case LIMIT_EXCEEDED ->
          player.sendMessage(MessageUtil.errorText("Withdrawal limit exceeded."));
        default ->
          player.sendMessage(MessageUtil.errorText("Withdrawal failed: " + result));
      }
      sendUpdate();
      return;
    }

    BigDecimal netToWallet = amount.subtract(fee);
    VaultEconomyProvider vault = economyManager.getVaultProvider();
    if (!vault.deposit(uuid, netToWallet)) {
      player.sendMessage(MessageUtil.errorText(
          "Warning: Failed to deposit to your wallet. Contact an admin."));
      sendUpdate();
      return;
    }

    String msg = "Withdrew " + economyManager.formatCurrency(amount) + " from the treasury.";
    if (fee.compareTo(BigDecimal.ZERO) > 0) {
      msg += " (fee: " + economyManager.formatCurrency(fee) + ", received: "
          + economyManager.formatCurrency(netToWallet) + ")";
    }
    player.sendMessage(MessageUtil.successText(msg));

    Faction fresh = factionManager.getFaction(faction.id());
    if (fresh != null) {
      guiManager.openFactionTreasury(player, ref, store, playerRef, fresh);
    }
  }

}
