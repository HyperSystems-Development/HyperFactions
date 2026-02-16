package com.hyperfactions.gui.admin.page;

import com.hyperfactions.api.EconomyAPI;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionEconomy;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.admin.AdminNavBarHelper;
import com.hyperfactions.gui.admin.data.AdminEconomyAdjustData;
import com.hyperfactions.manager.EconomyManager;
import com.hyperfactions.manager.FactionManager;
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

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Admin Economy Adjust modal - allows adjusting a faction's treasury balance.
 */
public class AdminEconomyAdjustPage extends InteractiveCustomUIPage<AdminEconomyAdjustData> {

    private final PlayerRef playerRef;
    private final FactionManager factionManager;
    private final EconomyManager economyManager;
    private final GuiManager guiManager;
    private final UUID factionId;

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

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {

        // Load template
        cmd.append("HyperFactions/admin/admin_economy_adjust.ui");

        // Setup admin nav bar
        AdminNavBarHelper.setupBar(playerRef, "economy", cmd, events);

        // Get faction info
        Faction faction = factionManager.getFaction(factionId);
        if (faction == null) {
            cmd.set("#TargetFactionName.Text", "Faction Not Found");
            cmd.set("#CurrentBalance.Text", "N/A");
            return;
        }

        FactionEconomy economy = economyManager.getEconomy(factionId);
        double currentBalance = economy != null ? economy.balance() : 0;

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
                double amount = parseAmountOrError(data.amount);
                if (Double.isNaN(amount)) return;

                if (amount == 0) {
                    showError("Amount cannot be zero.");
                    return;
                }

                String desc = amount >= 0
                        ? "Admin added " + economyManager.formatCurrency(amount)
                        : "Admin deducted " + economyManager.formatCurrency(Math.abs(amount));

                Logger.debugEconomy("Admin adjust: faction=%s amount=%.2f by=%s",
                        factionId, amount, playerRef.getUuid());

                economyManager.adminAdjust(factionId, amount, playerRef.getUuid(), desc)
                        .thenAccept(result -> handleResult(result, player, ref, store, playerRef))
                        .exceptionally(ex -> {
                            Logger.severe("Admin economy adjust failed for faction %s", ex, factionId);
                            showError("An error occurred.");
                            return null;
                        });
            }

            case "SetBalance" -> {
                double newBalance = parseAmountOrError(data.amount);
                if (Double.isNaN(newBalance)) return;

                if (newBalance < 0) {
                    showError("Balance cannot be negative.");
                    return;
                }

                Logger.debugEconomy("Admin set balance: faction=%s newBalance=%.2f by=%s",
                        factionId, newBalance, playerRef.getUuid());

                economyManager.setBalance(factionId, newBalance, playerRef.getUuid())
                        .thenAccept(result -> handleResult(result, player, ref, store, playerRef))
                        .exceptionally(ex -> {
                            Logger.severe("Admin economy set balance failed for faction %s", ex, factionId);
                            showError("An error occurred.");
                            return null;
                        });
            }

            case "Back" -> guiManager.openAdminEconomy(player, ref, store, playerRef);
        }
    }

    /**
     * Parses the amount string from the UI input.
     * Shows an error message and returns NaN if invalid.
     */
    private double parseAmountOrError(@Nullable String amount) {
        if (amount == null || amount.isBlank()) {
            showError("Please enter an amount.");
            return Double.NaN;
        }
        try {
            return Double.parseDouble(amount.trim());
        } catch (NumberFormatException e) {
            showError("Invalid number: " + amount);
            return Double.NaN;
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
            showError("Failed: " + result.name());
        }
    }

    private void showError(String message) {
        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set("#ErrorMessage.Text", message);
        sendUpdate(cmd, new UIEventBuilder(), false);
    }
}
