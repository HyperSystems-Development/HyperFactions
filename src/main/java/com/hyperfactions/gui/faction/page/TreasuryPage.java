package com.hyperfactions.gui.faction.page;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.api.EconomyAPI;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionEconomy;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.data.FactionPermissions;
import com.hyperfactions.data.FactionRole;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.faction.FactionPageRegistry;
import com.hyperfactions.gui.faction.NavBarHelper;
import com.hyperfactions.gui.faction.data.TreasuryData;
import com.hyperfactions.manager.EconomyManager;
import com.hyperfactions.manager.FactionManager;
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
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Faction Treasury page v2 — stat cards, upkeep display, quick action buttons
 * that open modal pages, transaction log, and a settings button for leaders.
 */
public class TreasuryPage extends InteractiveCustomUIPage<TreasuryData> {

  public static final String PAGE_ID = "treasury";

  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("MM/dd HH:mm")
      .withZone(ZoneId.systemDefault());

  private static final int MAX_VISIBLE_TRANSACTIONS = 20;

  private static final long TWENTY_FOUR_HOURS_MS = 24 * 3600L * 1000L;

  private final PlayerRef playerRef;

  private final FactionManager factionManager;

  private final EconomyManager economyManager;

  private final GuiManager guiManager;

  private final HyperFactions plugin;

  private final Faction faction;

  /** Creates a new TreasuryPage. */
  public TreasuryPage(PlayerRef playerRef,
            FactionManager factionManager,
            EconomyManager economyManager,
            GuiManager guiManager,
            HyperFactions plugin,
            Faction faction) {
    super(playerRef, CustomPageLifetime.CanDismiss, TreasuryData.CODEC);
    this.playerRef = playerRef;
    this.factionManager = factionManager;
    this.economyManager = economyManager;
    this.guiManager = guiManager;
    this.plugin = plugin;
    this.faction = faction;
  }

  /** Builds . */
  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {

    cmd.append(UIPaths.FACTION_TREASURY);
    NavBarHelper.setupBar(playerRef, faction, PAGE_ID, cmd, events);

    UUID uuid = playerRef.getUuid();
    FactionMember member = faction.getMember(uuid);
    boolean isLeader = member != null && member.role() == FactionRole.LEADER;
    boolean isOfficerOrHigher = member != null && member.isOfficerOrHigher();

    FactionEconomy economy = economyManager.getEconomy(faction.id());

    // A. Stat cards
    buildStatCards(cmd, economy, uuid);

    // B. Net P&L row
    buildPnlRow(cmd, economy);

    // C. Upkeep section (if enabled)
    buildUpkeepSection(cmd, economy);

    // D. Quick action buttons
    buildQuickActions(cmd, events, member, isLeader, isOfficerOrHigher);

    // E. Transaction log
    buildTransactionLog(cmd, economy);

    // F. Settings button (leader only)
    if (isLeader) {
      cmd.set("#SettingsRow.Visible", true);
      events.addEventBinding(CustomUIEventBindingType.Activating, "#SettingsBtn",
          EventData.of("Button", "Settings"), false);
    }
  }

  // === Stat Cards ===

  private void buildStatCards(UICommandBuilder cmd, FactionEconomy economy, UUID uuid) {
    BigDecimal balance = economy != null ? economy.balance() : BigDecimal.ZERO;
    cmd.set("#BalanceValue.Text", economyManager.formatCurrencyCompact(balance));

    // Wallet balance
    BigDecimal walletBalance = economyManager.getVaultProvider().getBalanceBigDecimal(uuid);
    cmd.set("#WalletBalance.Text", "Your wallet: " + economyManager.formatCurrencyCompact(walletBalance));

    // 24h P&L
    PnlResult pnl = calculatePnl(economy);
    cmd.set("#IncomeValue.Text", economyManager.formatCurrencyCompact(pnl.income));
    cmd.set("#ExpensesValue.Text", economyManager.formatCurrencyCompact(pnl.expenses));
  }

  // === P&L Row ===

  private void buildPnlRow(UICommandBuilder cmd, FactionEconomy economy) {
    PnlResult pnl = calculatePnl(economy);
    BigDecimal net = pnl.income.subtract(pnl.expenses);

    String color = net.compareTo(BigDecimal.ZERO) >= 0 ? "#44CC44" : "#FF5555";
    String text;
    if (net.compareTo(BigDecimal.ZERO) >= 0) {
      text = "Net (24h): +" + economyManager.formatCurrency(net);
    } else {
      text = "Net (24h): -" + economyManager.formatCurrency(net.abs());
    }

    cmd.set("#PnlValue.Text", text);
    cmd.set("#PnlValue.Style.TextColor", color);
  }

  // === Upkeep Section ===

  private void buildUpkeepSection(UICommandBuilder cmd, FactionEconomy economy) {
    ConfigManager config = ConfigManager.get();
    if (!config.isUpkeepEnabled()) {
      return;
    }

    cmd.set("#UpkeepSection.Visible", true);

    int claimCount = faction.getClaimCount();
    BigDecimal costPerCycle = config.getUpkeepCostPerChunk().multiply(BigDecimal.valueOf(claimCount));

    long lastUpkeep = economy != null ? economy.lastUpkeepTimestamp() : 0L;
    long intervalMs = config.getUpkeepIntervalHours() * 3600_000L;
    long nextUpkeep = lastUpkeep + intervalMs;
    long remaining = Math.max(0, nextUpkeep - System.currentTimeMillis());
    double progress = intervalMs > 0 ? 1.0 - ((double) remaining / intervalMs) : 0.0;

    cmd.set("#UpkeepCost.Text", "Cost: " + economyManager.formatCurrency(costPerCycle)
        + " every " + config.getUpkeepIntervalHours() + "h");
    cmd.set("#UpkeepBar.Value", progress);
    cmd.set("#UpkeepTimer.Text", formatDuration(remaining) + " left");

    boolean autoPay = economy != null && economy.upkeepAutoPay();
    cmd.set("#AutoPayStatus.Text", "Auto-pay: " + (autoPay ? "ON" : "OFF"));
    cmd.set("#AutoPayStatus.Style.TextColor", autoPay ? "#55FF55" : "#FF5555");
  }

  // === Quick Action Buttons ===

  private void buildQuickActions(UICommandBuilder cmd, UIEventBuilder events,
                 FactionMember member, boolean isLeader,
                 boolean isOfficerOrHigher) {
    FactionPermissions perms = faction.getEffectivePermissions();

    // Deposit: any member can deposit
    if (member != null) {
      events.addEventBinding(CustomUIEventBindingType.Activating, "#DepositBtn",
          EventData.of("Button", "Deposit"), false);
    } else {
      cmd.set("#DepositBtn.Disabled", true);
    }

    // Withdraw: leader always, officers if flag set
    boolean canWithdraw = isLeader
        || (isOfficerOrHigher && perms.get(FactionPermissions.TREASURY_WITHDRAW));
    if (canWithdraw) {
      events.addEventBinding(CustomUIEventBindingType.Activating, "#WithdrawBtn",
          EventData.of("Button", "Withdraw"), false);
    } else {
      cmd.set("#WithdrawBtn.Disabled", true);
    }

    // Transfer: leader always, officers if flag set
    boolean canTransfer = isLeader
        || (isOfficerOrHigher && perms.get(FactionPermissions.TREASURY_TRANSFER));
    if (canTransfer) {
      events.addEventBinding(CustomUIEventBindingType.Activating, "#TransferBtn",
          EventData.of("Button", "Transfer"), false);
    } else {
      cmd.set("#TransferBtn.Disabled", true);
    }
  }

  // === Transaction Log ===

  private void buildTransactionLog(UICommandBuilder cmd, FactionEconomy economy) {
    if (economy == null || economy.transactionHistory().isEmpty()) {
      return;
    }

    cmd.set("#NoTransactions.Visible", false);

    List<EconomyAPI.Transaction> transactions = economy.getRecentTransactions(MAX_VISIBLE_TRANSACTIONS);
    for (int i = 0; i < transactions.size(); i++) {
      EconomyAPI.Transaction tx = transactions.get(i);
      String time = TIME_FORMAT.format(Instant.ofEpochMilli(tx.timestamp()));
      String typeName = getHumanTypeName(tx.type());
      String typeColor = getTypeColor(tx.type());
      String sign = getTypeSign(tx.type());
      String amountStr = sign + economyManager.formatCurrency(tx.amount());
      String actorName = resolveActorName(tx.actorId());
      String desc = UiUtil.sanitize(tx.description());
      String bgColor = (i % 2 == 0) ? "#0d1520" : "#111a28";

      cmd.appendInline("#TransactionList",
          "Group { LayoutMode: Left; Anchor: (Height: 22); Background: (Color: " + bgColor + "); Padding: (Left: 6, Right: 6); "
          + "Label { Text: \"" + time + "\"; Style: (FontSize: 10, TextColor: #666666); Anchor: (Width: 100); } "
          + "Label { Text: \"" + typeName + "\"; Style: (FontSize: 10, TextColor: " + typeColor + "); Anchor: (Width: 100); } "
          + "Label { Text: \"" + actorName + "\"; Style: (FontSize: 10, TextColor: #AAAAAA); Anchor: (Width: 90); } "
          + "Label { Text: \"" + amountStr + "\"; Style: (FontSize: 10, TextColor: #FFFFFF); Anchor: (Width: 100); } "
          + "Label { Text: \"" + desc + "\"; Style: (FontSize: 10, TextColor: #555555); FlexWeight: 1; } "
          + "}");
    }
  }

  // === Event Handling ===

  /** Handles data event. */
  @Override
  public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                TreasuryData data) {
    super.handleDataEvent(ref, store, data);

    Player player = store.getComponent(ref, Player.getComponentType());
    PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

    if (player == null || playerRef == null || data.button == null) {
      sendUpdate();
      return;
    }

    // Handle navigation
    if ("Nav".equals(data.button) && data.navBar != null) {
      FactionPageRegistry.Entry entry = FactionPageRegistry.getInstance().getEntry(data.navBar);
      if (entry != null) {
        Faction currentFaction = factionManager.getFaction(faction.id());
        var page = entry.guiSupplier().create(player, ref, store, playerRef, currentFaction, guiManager);
        if (page != null) {
          player.getPageManager().openCustomPage(ref, store, page);
          return;
        }
      }
      sendUpdate();
      return;
    }

    switch (data.button) {
      case "Deposit" -> guiManager.openTreasuryDepositModal(player, ref, store, playerRef, faction, "deposit");
      case "Withdraw" -> guiManager.openTreasuryDepositModal(player, ref, store, playerRef, faction, "withdraw");
      case "Transfer" -> guiManager.openTreasuryTransferSearch(player, ref, store, playerRef, faction);
      case "Settings" -> guiManager.openTreasurySettings(player, ref, store, playerRef, faction);
      default -> sendUpdate();
    }
  }

  // === P&L Calculation ===

  private record PnlResult(BigDecimal income, BigDecimal expenses) {}

  private PnlResult calculatePnl(FactionEconomy economy) {
    if (economy == null || economy.transactionHistory().isEmpty()) {
      return new PnlResult(BigDecimal.ZERO, BigDecimal.ZERO);
    }

    long cutoff = System.currentTimeMillis() - TWENTY_FOUR_HOURS_MS;
    BigDecimal income = BigDecimal.ZERO;
    BigDecimal expenses = BigDecimal.ZERO;

    for (EconomyAPI.Transaction tx : economy.transactionHistory()) {
      if (tx.timestamp() < cutoff) {
        break;
      }
      switch (tx.type()) {
        case DEPOSIT, TRANSFER_IN, SPOILS, TAX_COLLECTION -> income = income.add(tx.amount());
        case WITHDRAW, TRANSFER_OUT, PLAYER_TRANSFER_OUT, UPKEEP, WAR_COST, RAID_COST ->
          expenses = expenses.add(tx.amount());
        default -> {}
      }
    }

    return new PnlResult(income, expenses);
  }

  // === Helper Methods ===

  private static String formatDuration(long millis) {
    if (millis <= 0) {
      return "0m";
    }
    long totalMinutes = millis / 60_000;
    long hours = totalMinutes / 60;
    long minutes = totalMinutes % 60;
    if (hours > 0) {
      return hours + "h " + minutes + "m";
    }
    return minutes + "m";
  }

  private static String getHumanTypeName(EconomyAPI.TransactionType type) {
    return switch (type) {
      case DEPOSIT -> "Deposit";
      case WITHDRAW -> "Withdrawal";
      case TRANSFER_IN -> "Transfer In";
      case TRANSFER_OUT -> "Transfer Out";
      case PLAYER_TRANSFER_OUT -> "Player Transfer";
      case UPKEEP -> "Upkeep";
      case TAX_COLLECTION -> "Tax Collection";
      case WAR_COST -> "War Cost";
      case RAID_COST -> "Raid Cost";
      case SPOILS -> "Spoils";
      case ADMIN_ADJUSTMENT -> "Admin Adjustment";
    };
  }

  private static String getTypeColor(EconomyAPI.TransactionType type) {
    return switch (type) {
      case DEPOSIT, TRANSFER_IN, SPOILS, TAX_COLLECTION -> "#55FF55";
      case WITHDRAW, TRANSFER_OUT, PLAYER_TRANSFER_OUT, UPKEEP, WAR_COST, RAID_COST -> "#FF5555";
      default -> "#FFAA00";
    };
  }

  private static String getTypeSign(EconomyAPI.TransactionType type) {
    return switch (type) {
      case DEPOSIT, TRANSFER_IN, SPOILS, TAX_COLLECTION -> "+";
      case WITHDRAW, TRANSFER_OUT, PLAYER_TRANSFER_OUT, UPKEEP, WAR_COST, RAID_COST -> "-";
      default -> "";
    };
  }

  private String resolveActorName(UUID actorId) {
    if (actorId == null) {
      return "System";
    }
    FactionMember member = faction.getMember(actorId);
    if (member != null) {
      return UiUtil.sanitize(member.username());
    }

    // Try online players for non-members (e.g., admin adjustments)
    for (PlayerRef ref : plugin.getOnlinePlayerRefs()) {
      if (ref.getUuid().equals(actorId)) {
        String name = ref.getUsername();
        if (name != null) {
          return UiUtil.sanitize(name);
        }
      }
    }
    return actorId.toString().substring(0, 8) + "...";
  }

}
