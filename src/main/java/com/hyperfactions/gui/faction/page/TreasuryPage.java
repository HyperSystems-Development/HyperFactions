package com.hyperfactions.gui.faction.page;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.api.EconomyAPI;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionEconomy;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.data.FactionLog;
import com.hyperfactions.data.FactionPermissions;
import com.hyperfactions.data.FactionRole;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.faction.FactionPageRegistry;
import com.hyperfactions.gui.faction.NavBarHelper;
import com.hyperfactions.gui.faction.data.TreasuryData;
import com.hyperfactions.manager.EconomyManager;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.util.HFMessages;
import com.hyperfactions.util.MessageKeys;
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

    // Localize static labels
    cmd.set("#TreasuryTitle.Text", HFMessages.get(playerRef, MessageKeys.TreasuryGui.TITLE));
    cmd.set("#BalanceLabel.Text", HFMessages.get(playerRef, MessageKeys.TreasuryGui.BALANCE_LABEL));
    cmd.set("#IncomeLabel.Text", HFMessages.get(playerRef, MessageKeys.TreasuryGui.INCOME_24H));
    cmd.set("#IncomeDescLabel.Text", HFMessages.get(playerRef, MessageKeys.TreasuryGui.DEPOSITS_TRANSFERS_IN));
    cmd.set("#ExpensesLabel.Text", HFMessages.get(playerRef, MessageKeys.TreasuryGui.EXPENSES_24H));
    cmd.set("#ExpensesDescLabel.Text", HFMessages.get(playerRef, MessageKeys.TreasuryGui.WITHDRAWALS_TRANSFERS_OUT));
    cmd.set("#MaintenanceLabel.Text", HFMessages.get(playerRef, MessageKeys.TreasuryGui.MAINTENANCE));
    cmd.set("#RunwayLabel.Text", HFMessages.get(playerRef, MessageKeys.TreasuryGui.RUNWAY_LABEL));
    cmd.set("#AddFundsLabel.Text", HFMessages.get(playerRef, MessageKeys.TreasuryGui.ADD_FUNDS));
    cmd.set("#DepositBtn.Text", HFMessages.get(playerRef, MessageKeys.TreasuryGui.DEPOSIT_BTN));
    cmd.set("#TakeFundsLabel.Text", HFMessages.get(playerRef, MessageKeys.TreasuryGui.TAKE_FUNDS));
    cmd.set("#WithdrawBtn.Text", HFMessages.get(playerRef, MessageKeys.TreasuryGui.WITHDRAW_BTN));
    cmd.set("#SendToFactionLabel.Text", HFMessages.get(playerRef, MessageKeys.TreasuryGui.SEND_TO_FACTION));
    cmd.set("#TransferBtn.Text", HFMessages.get(playerRef, MessageKeys.TreasuryGui.TRANSFER_BTN));
    cmd.set("#TreasuryConfigLabel.Text", HFMessages.get(playerRef, MessageKeys.TreasuryGui.TREASURY_CONFIG));
    cmd.set("#SettingsBtn.Text", HFMessages.get(playerRef, MessageKeys.TreasuryGui.SETTINGS_BTN));
    cmd.set("#RecentTransactionsLabel.Text", HFMessages.get(playerRef, MessageKeys.TreasuryGui.RECENT_TRANSACTIONS));
    cmd.set("#ColDateLabel.Text", HFMessages.get(playerRef, MessageKeys.TreasuryGui.COL_DATE));
    cmd.set("#ColTypeLabel.Text", HFMessages.get(playerRef, MessageKeys.TreasuryGui.COL_TYPE));
    cmd.set("#ColByLabel.Text", HFMessages.get(playerRef, MessageKeys.TreasuryGui.COL_BY));
    cmd.set("#ColAmountLabel.Text", HFMessages.get(playerRef, MessageKeys.TreasuryGui.COL_AMOUNT));
    cmd.set("#ColDetailsLabel.Text", HFMessages.get(playerRef, MessageKeys.TreasuryGui.COL_DETAILS));
    cmd.set("#PayNowBtn.Text", HFMessages.get(playerRef, MessageKeys.TreasuryGui.PAY_NOW_BTN));

    NavBarHelper.setupBar(playerRef, faction, PAGE_ID, cmd, events);

    UUID uuid = playerRef.getUuid();
    FactionMember member = faction.getMember(uuid);
    boolean isLeader = member != null && member.role() == FactionRole.LEADER;
    boolean isOfficerOrHigher = member != null && member.isOfficerOrHigher();

    FactionEconomy economy = economyManager.getEconomy(faction.id());

    // A. Stat cards
    buildStatCards(cmd, economy, uuid);

    // C. Upkeep section (if enabled)
    buildUpkeepSection(cmd, events, economy);

    // D. Quick action buttons (includes settings for leaders)
    buildQuickActions(cmd, events, member, isLeader, isOfficerOrHigher);

    // E. Transaction log
    buildTransactionLog(cmd, economy);
  }

  // === Stat Cards ===

  private void buildStatCards(UICommandBuilder cmd, FactionEconomy economy, UUID uuid) {
    BigDecimal balance = economy != null ? economy.balance() : BigDecimal.ZERO;
    cmd.set("#BalanceValue.Text", economyManager.formatCurrencyCompact(balance));

    // Wallet balance
    BigDecimal walletBalance = economyManager.getVaultProvider().getBalanceBigDecimal(uuid);
    cmd.set("#WalletBalance.Text", HFMessages.get(playerRef, MessageKeys.TreasuryGui.WALLET_LABEL,
        economyManager.formatCurrencyCompact(walletBalance)));

    // 24h P&L
    PnlResult pnl = calculatePnl(economy);
    cmd.set("#IncomeValue.Text", economyManager.formatCurrencyCompact(pnl.income));
    cmd.set("#ExpensesValue.Text", economyManager.formatCurrencyCompact(pnl.expenses));
  }

  // === Upkeep Section ===

  private void buildUpkeepSection(UICommandBuilder cmd, UIEventBuilder events,
                  FactionEconomy economy) {
    ConfigManager config = ConfigManager.get();
    if (!config.isUpkeepEnabled()) {
      return;
    }

    cmd.set("#UpkeepSection.Visible", true);

    int claimCount = faction.getClaimCount();
    int freeChunks = config.getUpkeepFreeChunks();
    int billableChunks = Math.max(0, claimCount - freeChunks);

    // Calculate cost using UpkeepProcessor for consistency
    com.hyperfactions.economy.UpkeepProcessor costCalc =
        new com.hyperfactions.economy.UpkeepProcessor(economyManager, null, null);
    BigDecimal costPerCycle = costCalc.calculateUpkeepCost(billableChunks);

    int intervalHours = config.getUpkeepIntervalHours();
    long intervalMs = intervalHours * 3600_000L;
    long lastUpkeep = economy != null ? economy.lastUpkeepTimestamp() : 0L;

    // Timer: calculate time until next collection
    long remaining;
    double progress;
    if (lastUpkeep == 0L) {
      // No cycle has run yet — show "Pending" with full bar
      remaining = -1; // sentinel for "pending"
      progress = 1.0;
    } else {
      long nextUpkeep = lastUpkeep + intervalMs;
      remaining = Math.max(0, nextUpkeep - System.currentTimeMillis());
      progress = intervalMs > 0 ? 1.0 - ((double) remaining / intervalMs) : 0.0;
    }

    // Show chunk breakdown
    String chunkDetail = HFMessages.get(playerRef, MessageKeys.TreasuryGui.CHUNKS_DETAIL,
        Math.min(freeChunks, claimCount), billableChunks);
    String costString = economyManager.formatCurrency(costPerCycle) + " every " + intervalHours + "h";
    cmd.set("#UpkeepCost.Text", HFMessages.get(playerRef, MessageKeys.TreasuryGui.COST_LABEL, costString));
    cmd.set("#UpkeepDetail.Text", chunkDetail);

    // Color-code the progress bar based on status
    boolean inGrace = economy != null && economy.upkeepGraceStartTimestamp() > 0;
    boolean canAfford = economy != null && economy.hasFunds(costPerCycle);
    String barColor;
    if (inGrace) {
      barColor = "#FF5555"; // Red — in grace
    } else if (!canAfford && billableChunks > 0) {
      barColor = "#FFAA00"; // Yellow — insufficient funds
    } else {
      barColor = "#55FF55"; // Green — paid/sufficient
    }
    cmd.set("#UpkeepBar.Value", progress);
    cmd.set("#UpkeepBar.Bar.Color", barColor);
    cmd.set("#UpkeepTimer.Text", remaining < 0
        ? HFMessages.get(playerRef, MessageKeys.TreasuryGui.PENDING)
        : formatDuration(remaining) + " left");

    boolean autoPay = economy != null && economy.upkeepAutoPay();
    cmd.set("#AutoPayStatus.Text", autoPay
        ? HFMessages.get(playerRef, MessageKeys.TreasuryGui.AUTO_PAY_ON)
        : HFMessages.get(playerRef, MessageKeys.TreasuryGui.AUTO_PAY_OFF));
    cmd.set("#AutoPayStatus.Style.TextColor", autoPay ? "#55FF55" : "#FF5555");

    // Cost projections row
    if (costPerCycle.compareTo(BigDecimal.ZERO) > 0) {
      cmd.set("#ProjectionsRow.Visible", true);
      BigDecimal balance = economy != null ? economy.balance() : BigDecimal.ZERO;

      // Cycles per day = 24 / intervalHours
      double cyclesPerDay = 24.0 / intervalHours;
      BigDecimal dailyCost = costPerCycle.multiply(BigDecimal.valueOf(cyclesPerDay))
          .setScale(2, java.math.RoundingMode.HALF_UP);

      cmd.set("#Cost7d.Text", economyManager.formatCurrency(dailyCost.multiply(BigDecimal.valueOf(7))));
      cmd.set("#Cost14d.Text", economyManager.formatCurrency(dailyCost.multiply(BigDecimal.valueOf(14))));
      cmd.set("#Cost30d.Text", economyManager.formatCurrency(dailyCost.multiply(BigDecimal.valueOf(30))));

      // Runway: how many days until funds run out
      if (dailyCost.compareTo(BigDecimal.ZERO) > 0 && balance.compareTo(BigDecimal.ZERO) > 0) {
        int runwayDays = balance.divide(dailyCost, 0, java.math.RoundingMode.FLOOR).intValue();
        String runwayText;
        String runwayColor;
        if (runwayDays > 90) {
          runwayText = HFMessages.get(playerRef, MessageKeys.TreasuryGui.RUNWAY_90_PLUS);
          runwayColor = "#55FF55";
        } else if (runwayDays > 0) {
          runwayText = runwayDays != 1
              ? HFMessages.get(playerRef, MessageKeys.TreasuryGui.RUNWAY_DAYS, runwayDays)
              : HFMessages.get(playerRef, MessageKeys.TreasuryGui.RUNWAY_DAY, runwayDays);
          runwayColor = runwayDays <= 3 ? "#FF5555" : runwayDays <= 7 ? "#FFAA00" : "#55FF55";
        } else {
          runwayText = HFMessages.get(playerRef, MessageKeys.TreasuryGui.RUNWAY_LESS_THAN_DAY);
          runwayColor = "#FF5555";
        }
        cmd.set("#RunwayValue.Text", runwayText);
        cmd.set("#RunwayValue.Style.TextColor", runwayColor);
      } else {
        cmd.set("#RunwayValue.Text", balance.compareTo(BigDecimal.ZERO) == 0
            ? HFMessages.get(playerRef, MessageKeys.TreasuryGui.RUNWAY_NO_FUNDS)
            : HFMessages.get(playerRef, MessageKeys.Common.NA));
        cmd.set("#RunwayValue.Style.TextColor", "#FF5555");
      }
    }

    // Grace period warning
    if (inGrace) {
      cmd.set("#GraceWarning.Visible", true);
      long graceMs = config.getUpkeepGracePeriodHours() * 3600_000L;
      long graceElapsed = System.currentTimeMillis() - economy.upkeepGraceStartTimestamp();
      long graceRemaining = Math.max(0, graceMs - graceElapsed);
      cmd.set("#GraceTimer.Text", HFMessages.get(playerRef, MessageKeys.TreasuryGui.GRACE_EXPIRES,
          formatDuration(graceRemaining)));
      cmd.set("#MissedCount.Text", HFMessages.get(playerRef, MessageKeys.TreasuryGui.MISSED_PAYMENTS,
          economy.consecutiveMissedPayments()));

      // Show Pay Now button if faction can afford the upkeep cost
      if (canAfford && billableChunks > 0) {
        cmd.set("#PayNowRow.Visible", true);
        cmd.set("#PayNowCost.Text", HFMessages.get(playerRef, MessageKeys.TreasuryGui.PAY_TO_CLEAR,
            economyManager.formatCurrency(costPerCycle)));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#PayNowBtn",
            EventData.of("Button", "PayNow"), false);
      }
    }
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

    // Settings: leader only
    if (isLeader) {
      cmd.set("#SettingsBox.Visible", true);
      events.addEventBinding(CustomUIEventBindingType.Activating, "#SettingsBtn",
          EventData.of("Button", "Settings"), false);
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
      case "PayNow" -> handlePayNow(player, ref, store, playerRef);
      default -> sendUpdate();
    }
  }

  // === Pay Now (Grace Period) ===

  private void handlePayNow(Player player, Ref<EntityStore> ref,
               Store<EntityStore> store, PlayerRef playerRef) {
    Faction currentFaction = factionManager.getFaction(faction.id());
    if (currentFaction == null) {
      sendUpdate();
      return;
    }

    FactionEconomy economy = economyManager.getEconomy(faction.id());
    if (economy == null || economy.upkeepGraceStartTimestamp() == 0L) {
      // Not in grace — just refresh
      reopenTreasury(player, ref, store, playerRef, currentFaction);
      return;
    }

    ConfigManager config = ConfigManager.get();
    int freeChunks = config.getUpkeepFreeChunks();
    int billableChunks = Math.max(0, currentFaction.getClaimCount() - freeChunks);
    if (billableChunks <= 0) {
      // No cost — just clear grace
      FactionEconomy updated = economy.withGraceReset()
          .withLastUpkeepTimestamp(System.currentTimeMillis());
      economyManager.updateEconomy(faction.id(), updated);
      reopenTreasury(player, ref, store, playerRef, currentFaction);
      return;
    }

    com.hyperfactions.economy.UpkeepProcessor costCalc =
        new com.hyperfactions.economy.UpkeepProcessor(economyManager, null, null);
    BigDecimal cost = costCalc.calculateUpkeepCost(billableChunks);

    if (!economy.hasFunds(cost)) {
      // Not enough funds — refresh to show current state
      reopenTreasury(player, ref, store, playerRef, currentFaction);
      return;
    }

    // Withdraw upkeep cost
    EconomyAPI.TransactionResult result = economyManager.systemWithdraw(
        faction.id(), cost, EconomyAPI.TransactionType.UPKEEP,
        String.format("Manual upkeep payment: %d billable chunks", billableChunks));

    if (result == EconomyAPI.TransactionResult.SUCCESS) {
      // Reset grace state
      FactionEconomy current = economyManager.getEconomy(faction.id());
      if (current != null) {
        FactionEconomy updated = current.withGraceReset()
            .withLastUpkeepTimestamp(System.currentTimeMillis());
        economyManager.updateEconomy(faction.id(), updated);
      }

      // Log to faction activity
      Faction factionNow = factionManager.getFaction(faction.id());
      if (factionNow != null) {
        Faction logged = factionNow.withLog(FactionLog.create(FactionLog.LogType.ECONOMY,
            String.format("Upkeep paid manually: %s (%d billable chunks, grace cleared)",
                economyManager.formatCurrency(cost), billableChunks),
            playerRef.getUuid()));
        factionManager.updateFaction(logged);
      }
    }

    // Reopen treasury to show updated state
    reopenTreasury(player, ref, store, playerRef,
        factionManager.getFaction(faction.id()));
  }

  private void reopenTreasury(Player player, Ref<EntityStore> ref,
                Store<EntityStore> store, PlayerRef playerRef, Faction faction) {
    TreasuryPage page = new TreasuryPage(playerRef, factionManager, economyManager,
        guiManager, plugin, faction != null ? faction : this.faction);
    player.getPageManager().openCustomPage(ref, store, page);
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

  private String getHumanTypeName(EconomyAPI.TransactionType type) {
    return switch (type) {
      case DEPOSIT -> HFMessages.get(playerRef, MessageKeys.TreasuryGui.TYPE_DEPOSIT);
      case WITHDRAW -> HFMessages.get(playerRef, MessageKeys.TreasuryGui.TYPE_WITHDRAWAL);
      case TRANSFER_IN -> HFMessages.get(playerRef, MessageKeys.TreasuryGui.TYPE_TRANSFER_IN);
      case TRANSFER_OUT -> HFMessages.get(playerRef, MessageKeys.TreasuryGui.TYPE_TRANSFER_OUT);
      case PLAYER_TRANSFER_OUT -> HFMessages.get(playerRef, MessageKeys.TreasuryGui.TYPE_PLAYER_TRANSFER);
      case UPKEEP -> HFMessages.get(playerRef, MessageKeys.TreasuryGui.TYPE_UPKEEP);
      case TAX_COLLECTION -> HFMessages.get(playerRef, MessageKeys.TreasuryGui.TYPE_TAX);
      case WAR_COST -> HFMessages.get(playerRef, MessageKeys.TreasuryGui.TYPE_WAR_COST);
      case RAID_COST -> HFMessages.get(playerRef, MessageKeys.TreasuryGui.TYPE_RAID_COST);
      case SPOILS -> HFMessages.get(playerRef, MessageKeys.TreasuryGui.TYPE_SPOILS);
      case ADMIN_ADJUSTMENT -> HFMessages.get(playerRef, MessageKeys.TreasuryGui.TYPE_ADMIN);
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
      return HFMessages.get(playerRef, MessageKeys.TreasuryGui.SYSTEM);
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
