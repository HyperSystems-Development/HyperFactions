package com.hyperfactions.gui.faction.page;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.api.EconomyAPI;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionEconomy;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.faction.data.TreasuryTransferConfirmData;
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

import java.util.UUID;

/**
 * Transfer confirmation page — shows target info, amount input, fee breakdown.
 */
public class TreasuryTransferConfirmPage extends InteractiveCustomUIPage<TreasuryTransferConfirmData> {

    private final PlayerRef playerRef;
    private final FactionManager factionManager;
    private final EconomyManager economyManager;
    private final GuiManager guiManager;
    private final HyperFactions plugin;
    private final Faction faction;
    private final String targetId;
    private final String targetName;
    private final String targetType; // "player" or "faction"

    public TreasuryTransferConfirmPage(PlayerRef playerRef,
                                        FactionManager factionManager,
                                        EconomyManager economyManager,
                                        GuiManager guiManager,
                                        HyperFactions plugin,
                                        Faction faction,
                                        String targetId,
                                        String targetName,
                                        String targetType) {
        super(playerRef, CustomPageLifetime.CanDismiss, TreasuryTransferConfirmData.CODEC);
        this.playerRef = playerRef;
        this.factionManager = factionManager;
        this.economyManager = economyManager;
        this.guiManager = guiManager;
        this.plugin = plugin;
        this.faction = faction;
        this.targetId = targetId;
        this.targetName = targetName;
        this.targetType = targetType;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {

        cmd.append("HyperFactions/faction/treasury_transfer_confirm.ui");

        // Target info
        cmd.set("#TargetName.Text", targetName);
        String typeTag = "player".equals(targetType) ? "[Player]" : "[Faction]";
        cmd.set("#TargetType.Text", typeTag);
        // Set tag color dynamically (Labels support .Style.TextColor)
        if ("faction".equals(targetType)) {
            cmd.set("#TargetType.Style.TextColor", "#00AAFF");
        }

        // Treasury balance
        FactionEconomy economy = economyManager.getEconomy(faction.id());
        double treasuryBalance = economy != null ? economy.balance() : 0.0;
        cmd.set("#TreasuryLabel.Text", "Treasury: " + economyManager.formatCurrency(treasuryBalance));

        // Fee label
        double feePercent = ConfigManager.get().getTransferFeePercent();
        cmd.set("#FeeLabel.Text", String.format("Fee (%.1f%%):", feePercent));

        // Event bindings
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CancelBtn",
                EventData.of("Button", "Cancel"), false);

        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#AmountInput",
                EventData.of("Button", "Preview").append("@Amount", "#AmountInput.Value"), false);

        events.addEventBinding(CustomUIEventBindingType.Activating, "#ConfirmBtn",
                EventData.of("Button", "Confirm").append("@Amount", "#AmountInput.Value"), false);
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                                TreasuryTransferConfirmData data) {
        super.handleDataEvent(ref, store, data);

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        if (player == null || playerRef == null || data.button == null) {
            sendUpdate();
            return;
        }

        switch (data.button) {
            case "Cancel" -> guiManager.openTreasuryTransferSearch(player, ref, store, playerRef, faction);
            case "Preview" -> handlePreview(data);
            case "Confirm" -> handleConfirm(player, ref, store, playerRef, data);
            default -> sendUpdate();
        }
    }

    private void handlePreview(TreasuryTransferConfirmData data) {
        double amount = UiUtil.parseAmount(data.amount);

        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();

        if (amount <= 0) {
            cmd.set("#FeeAmount.Text", "--");
            cmd.set("#FeeValue.Text", "--");
            cmd.set("#FeeTotal.Text", "--");
            cmd.set("#RecipientAmount.Text", "--");
        } else {
            double fee = economyManager.calculateFee(amount, EconomyAPI.TransactionType.TRANSFER_OUT);
            double recipientReceives = amount - fee;
            cmd.set("#FeeAmount.Text", economyManager.formatCurrency(amount));
            cmd.set("#FeeValue.Text", fee > 0 ? "-" + economyManager.formatCurrency(fee) : economyManager.formatCurrency(0));
            cmd.set("#FeeTotal.Text", economyManager.formatCurrency(amount));
            cmd.set("#RecipientAmount.Text", economyManager.formatCurrency(recipientReceives));
        }

        // Re-bind events
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#AmountInput",
                EventData.of("Button", "Preview").append("@Amount", "#AmountInput.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CancelBtn",
                EventData.of("Button", "Cancel"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ConfirmBtn",
                EventData.of("Button", "Confirm").append("@Amount", "#AmountInput.Value"), false);

        sendUpdate(cmd, events, false);
    }

    private void handleConfirm(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                                PlayerRef playerRef, TreasuryTransferConfirmData data) {
        UUID uuid = playerRef.getUuid();
        double amount = UiUtil.parseAmount(data.amount);
        if (amount <= 0) {
            player.sendMessage(MessageUtil.errorText("Enter a valid positive amount."));
            sendUpdate();
            return;
        }

        // Permission check
        if (!PermissionManager.get().hasPermission(uuid, Permissions.ECONOMY_TRANSFER)) {
            player.sendMessage(MessageUtil.errorText("You don't have permission to transfer."));
            sendUpdate();
            return;
        }

        // Limit check
        String limitReason = economyManager.checkTransferLimits(faction.id(), amount);
        if (limitReason != null) {
            player.sendMessage(MessageUtil.errorText("Transfer denied: " + limitReason));
            sendUpdate();
            return;
        }

        double fee = economyManager.calculateFee(amount, EconomyAPI.TransactionType.TRANSFER_OUT);
        double recipientReceives = amount - fee;

        if ("faction".equals(targetType)) {
            handleFactionTransfer(player, ref, store, playerRef, uuid, amount, recipientReceives);
        } else {
            handlePlayerTransfer(player, ref, store, playerRef, uuid, amount, recipientReceives);
        }
    }

    private void handleFactionTransfer(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                                        PlayerRef playerRef, UUID uuid, double deducted, double recipientReceives) {
        UUID targetFactionId;
        try {
            targetFactionId = UUID.fromString(targetId);
        } catch (IllegalArgumentException e) {
            player.sendMessage(MessageUtil.errorText("Invalid target faction."));
            sendUpdate();
            return;
        }

        Faction targetFaction = factionManager.getFaction(targetFactionId);
        if (targetFaction == null) {
            player.sendMessage(MessageUtil.errorText("Target faction no longer exists."));
            sendUpdate();
            return;
        }

        // Withdraw full amount from source (records as TRANSFER_OUT)
        EconomyAPI.TransactionResult withdrawResult = economyManager.withdraw(
                faction.id(), deducted, uuid, "Transfer to " + targetName,
                EconomyAPI.TransactionType.TRANSFER_OUT).join();

        if (withdrawResult != EconomyAPI.TransactionResult.SUCCESS) {
            player.sendMessage(MessageUtil.errorText("Transfer failed: " + withdrawResult));
            sendUpdate();
            return;
        }

        // Deposit net amount to target faction (records as DEPOSIT — target sees incoming funds)
        EconomyAPI.TransactionResult depositResult = economyManager.deposit(
                targetFactionId, recipientReceives, uuid, "Transfer from " + faction.name()).join();

        if (depositResult != EconomyAPI.TransactionResult.SUCCESS) {
            // Rollback
            economyManager.deposit(faction.id(), deducted, null, "Rollback: transfer to " + targetName + " failed");
            player.sendMessage(MessageUtil.errorText("Transfer failed. Funds returned."));
            sendUpdate();
            return;
        }

        player.sendMessage(MessageUtil.successText(
                "Transferred " + economyManager.formatCurrency(recipientReceives) + " to " + targetName + "."));

        Faction fresh = factionManager.getFaction(faction.id());
        if (fresh != null) {
            guiManager.openFactionTreasury(player, ref, store, playerRef, fresh);
        }
    }

    private void handlePlayerTransfer(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                                       PlayerRef playerRef, UUID uuid, double deducted, double recipientReceives) {
        UUID targetUuid;
        try {
            targetUuid = UUID.fromString(targetId);
        } catch (IllegalArgumentException e) {
            player.sendMessage(MessageUtil.errorText("Invalid target player."));
            sendUpdate();
            return;
        }

        // Withdraw from faction treasury (records as PLAYER_TRANSFER_OUT)
        EconomyAPI.TransactionResult withdrawResult = economyManager.withdraw(
                faction.id(), deducted, uuid, "Transfer to player: " + targetName,
                EconomyAPI.TransactionType.PLAYER_TRANSFER_OUT).join();

        if (withdrawResult != EconomyAPI.TransactionResult.SUCCESS) {
            player.sendMessage(MessageUtil.errorText("Transfer failed: " + withdrawResult));
            sendUpdate();
            return;
        }

        // Deposit to player wallet
        VaultEconomyProvider vault = economyManager.getVaultProvider();
        if (!vault.deposit(targetUuid, recipientReceives)) {
            // Rollback
            economyManager.deposit(faction.id(), deducted, null, "Rollback: player transfer failed");
            player.sendMessage(MessageUtil.errorText("Failed to deposit to player wallet. Transfer rolled back."));
            sendUpdate();
            return;
        }

        player.sendMessage(MessageUtil.successText(
                "Transferred " + economyManager.formatCurrency(recipientReceives) + " to " + targetName + "."));

        Faction fresh = factionManager.getFaction(faction.id());
        if (fresh != null) {
            guiManager.openFactionTreasury(player, ref, store, playerRef, fresh);
        }
    }

}
