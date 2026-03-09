package com.hyperfactions.economy;

import com.hyperfactions.api.EconomyAPI;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.config.modules.EconomyConfig.ScalingTier;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionEconomy;
import com.hyperfactions.data.FactionLog;
import com.hyperfactions.manager.ClaimManager;
import com.hyperfactions.manager.EconomyManager;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.integration.economy.VaultEconomyProvider;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Core upkeep processing logic for faction territory maintenance costs.
 * Handles cost calculation (flat/progressive), payment processing,
 * grace periods, and claim forfeiture on persistent non-payment.
 */
public class UpkeepProcessor {

  private final EconomyManager economyManager;
  private final FactionManager factionManager;
  private final ClaimManager claimManager;

  @Nullable
  private UpkeepNotificationCallback notificationCallback;

  /**
   * Callback for sending upkeep notifications to faction members.
   */
  @FunctionalInterface
  public interface UpkeepNotificationCallback {
    void notifyFaction(UUID factionId, String message, String hexColor);
  }

  public UpkeepProcessor(@NotNull EconomyManager economyManager,
              @NotNull FactionManager factionManager,
              @NotNull ClaimManager claimManager) {
    this.economyManager = economyManager;
    this.factionManager = factionManager;
    this.claimManager = claimManager;
  }

  /**
   * Sets the notification callback for sending messages to faction members.
   */
  public void setNotificationCallback(@Nullable UpkeepNotificationCallback callback) {
    this.notificationCallback = callback;
  }

  /**
   * Processes upkeep for all factions with claims.
   * This is the main entry point called by PeriodicTaskManager.
   */
  public void processUpkeep() {
    ConfigManager config = ConfigManager.get();
    if (!config.isUpkeepEnabled()) {
      return;
    }

    // Check if economy plugin is available
    VaultEconomyProvider vault = economyManager.getVaultProvider();
    if (!vault.isAvailable()) {
      Logger.warn("[Upkeep] No economy plugin available — skipping upkeep collection");
      return;
    }

    int freeChunks = config.getUpkeepFreeChunks();
    int gracePeriodHours = config.getUpkeepGracePeriodHours();
    int claimLossPerCycle = config.getUpkeepClaimLossPerCycle();
    long gracePeriodMs = gracePeriodHours * 3600L * 1000L;

    int paid = 0;
    int inGrace = 0;
    int lostClaims = 0;
    int skipped = 0;

    for (Faction faction : factionManager.getAllFactions()) {
      int claimCount = faction.getClaimCount();
      if (claimCount <= 0) {
        continue;
      }

      FactionEconomy economy = economyManager.getEconomy(faction.id());
      if (economy == null) {
        continue;
      }

      int billableChunks = Math.max(0, claimCount - freeChunks);

      // All chunks are free — no upkeep needed
      if (billableChunks == 0) {
        FactionEconomy updated = economy.withLastUpkeepTimestamp(System.currentTimeMillis())
            .withGraceReset();
        economyManager.updateEconomy(faction.id(), updated);
        skipped++;
        continue;
      }

      BigDecimal cost = calculateUpkeepCost(billableChunks);

      // New faction grace: skip first charge (initialize timer)
      if (economy.lastUpkeepTimestamp() == 0L) {
        FactionEconomy updated = economy.withLastUpkeepTimestamp(System.currentTimeMillis());
        economyManager.updateEconomy(faction.id(), updated);
        Logger.debugEconomy("Upkeep initialized for %s (first cycle skipped)", faction.name());
        skipped++;
        continue;
      }

      // Auto-pay disabled — enter grace immediately
      if (!economy.upkeepAutoPay()) {
        FactionEconomy updated = handlePaymentFailure(faction, economy, gracePeriodMs, claimLossPerCycle,
            "Upkeep due! Auto-pay is off. " + economyManager.formatCurrency(cost) + " needed.");
        if (updated.upkeepGraceStartTimestamp() > 0 && isGraceExpired(updated, gracePeriodMs)) {
          lostClaims++;
        } else {
          inGrace++;
        }
        continue;
      }

      // Try to pay
      if (economyManager.hasFunds(faction.id(), cost)) {
        // Successful payment
        EconomyAPI.TransactionResult result = economyManager.systemWithdraw(
            faction.id(), cost, EconomyAPI.TransactionType.UPKEEP,
            String.format("Upkeep: %d billable chunks", billableChunks));

        if (result == EconomyAPI.TransactionResult.SUCCESS) {
          // Reset grace state and update timestamp
          FactionEconomy current = economyManager.getEconomy(faction.id());
          if (current != null) {
            FactionEconomy updated = current.withLastUpkeepTimestamp(System.currentTimeMillis())
                .withGraceReset();
            economyManager.updateEconomy(faction.id(), updated);
          }

          notifyFaction(faction.id(),
              String.format("Upkeep paid: %s for %d billable chunks",
                  economyManager.formatCurrency(cost), billableChunks),
              "#55FF55");
          logToFaction(faction.id(), FactionLog.LogType.ECONOMY,
              String.format("Upkeep paid: %s (%d billable chunks)",
                  economyManager.formatCurrency(cost), billableChunks));
          paid++;
          Logger.debugEconomy("Upkeep paid for %s: %s (%d billable chunks)",
              faction.name(), economyManager.formatCurrency(cost), billableChunks);
        } else {
          // Unexpected failure — treat as insufficient funds
          FactionEconomy updated = handlePaymentFailure(faction, economy, gracePeriodMs, claimLossPerCycle,
              "Upkeep failed! " + economyManager.formatCurrency(cost) + " needed.");
          inGrace++;
        }
      } else {
        // Insufficient funds
        FactionEconomy current = economyManager.getEconomy(faction.id());
        if (current == null) current = economy;
        FactionEconomy updated = handlePaymentFailure(faction, current, gracePeriodMs, claimLossPerCycle,
            String.format("Upkeep failed! Insufficient funds (%s needed, balance: %s)",
                economyManager.formatCurrency(cost), economyManager.formatCurrency(current.balance())));
        if (updated.upkeepGraceStartTimestamp() > 0 && isGraceExpired(updated, gracePeriodMs)) {
          lostClaims++;
        } else {
          inGrace++;
        }
      }
    }

    Logger.info("[Upkeep] Collection complete: %d paid, %d in grace, %d lost claims, %d skipped",
        paid, inGrace, lostClaims, skipped);
  }

  /**
   * Handles a failed upkeep payment — starts or continues grace period,
   * or forfeits claims if grace has expired.
   *
   * @return the updated FactionEconomy
   */
  @NotNull
  private FactionEconomy handlePaymentFailure(@NotNull Faction faction, @NotNull FactionEconomy economy,
                         long gracePeriodMs, int claimLossPerCycle, @NotNull String reason) {
    long now = System.currentTimeMillis();
    int missed = economy.consecutiveMissedPayments() + 1;

    if (economy.upkeepGraceStartTimestamp() == 0L) {
      // Start grace period
      FactionEconomy updated = economy
          .withUpkeepGraceStartTimestamp(now)
          .withConsecutiveMissedPayments(missed)
          .withLastUpkeepTimestamp(now);
      economyManager.updateEconomy(faction.id(), updated);

      ConfigManager config = ConfigManager.get();
      notifyFaction(faction.id(),
          reason + " Grace period: " + config.getUpkeepGracePeriodHours() + "h",
          "#FFAA00");
      logToFaction(faction.id(), FactionLog.LogType.ECONOMY,
          "Upkeep failed: grace period started (" + config.getUpkeepGracePeriodHours() + "h)");

      Logger.info("[Upkeep] Grace started for %s: %s (missed: %d)", faction.name(), reason, missed);
      return updated;
    }

    long graceElapsed = now - economy.upkeepGraceStartTimestamp();

    if (graceElapsed < gracePeriodMs) {
      // Still in grace
      FactionEconomy updated = economy
          .withConsecutiveMissedPayments(missed)
          .withLastUpkeepTimestamp(now);
      economyManager.updateEconomy(faction.id(), updated);

      long remainingMs = gracePeriodMs - graceElapsed;
      String remaining = formatDuration(remainingMs);
      notifyFaction(faction.id(),
          "Upkeep still unpaid! Grace expires in " + remaining,
          "#FFAA00");
      logToFaction(faction.id(), FactionLog.LogType.ECONOMY,
          "Upkeep missed (payment " + missed + "), grace expires in " + remaining);

      Logger.debugEconomy("Grace continues for %s: %s remaining (missed: %d)",
          faction.name(), remaining, missed);
      return updated;
    }

    // Grace expired — forfeit claims
    FactionEconomy updated = economy
        .withConsecutiveMissedPayments(missed)
        .withLastUpkeepTimestamp(now);
    economyManager.updateEconomy(faction.id(), updated);

    int removed = claimManager.progressiveDecay(faction.id(), claimLossPerCycle, "unpaid upkeep");

    if (removed > 0) {
      notifyFaction(faction.id(),
          String.format("Territory lost! %d claim(s) forfeited due to unpaid upkeep", removed),
          "#FF5555");

      // Log to faction activity
      Faction current = factionManager.getFaction(faction.id());
      if (current != null) {
        Faction logged = current.withLog(FactionLog.create(FactionLog.LogType.UNCLAIM,
            String.format("Lost %d claim(s) to upkeep (missed %d payments)", removed, missed), null));
        factionManager.updateFaction(logged);
      }

      Logger.info("[Upkeep] %s lost %d claims (grace expired, missed: %d)", faction.name(), removed, missed);
    }

    return updated;
  }

  /**
   * Processes upkeep warning notifications for factions approaching collection.
   * Should run more frequently than the upkeep cycle (e.g. every 15 minutes).
   */
  public void processWarnings() {
    ConfigManager config = ConfigManager.get();
    if (!config.isUpkeepEnabled()) {
      return;
    }

    int warningHours = config.getUpkeepWarningHours();
    if (warningHours <= 0) {
      return;
    }

    int intervalHours = config.getUpkeepIntervalHours();
    long warningMs = warningHours * 3600L * 1000L;
    long intervalMs = intervalHours * 3600L * 1000L;
    long now = System.currentTimeMillis();
    int freeChunks = config.getUpkeepFreeChunks();

    for (Faction faction : factionManager.getAllFactions()) {
      int claimCount = faction.getClaimCount();
      if (claimCount <= 0) continue;

      int billableChunks = Math.max(0, claimCount - freeChunks);
      if (billableChunks == 0) continue;

      FactionEconomy economy = economyManager.getEconomy(faction.id());
      if (economy == null || economy.lastUpkeepTimestamp() == 0L) continue;

      long nextUpkeep = economy.lastUpkeepTimestamp() + intervalMs;
      long timeUntil = nextUpkeep - now;

      // Only warn if within warning window and not past due
      if (timeUntil > 0 && timeUntil <= warningMs) {
        BigDecimal cost = calculateUpkeepCost(billableChunks);
        boolean canAfford = economy.hasFunds(cost);
        String timeStr = formatDuration(timeUntil);

        if (!canAfford) {
          notifyFaction(faction.id(),
              String.format("Upkeep warning! %s due in %s. Balance: %s (need %s)",
                  economyManager.formatCurrency(cost), timeStr,
                  economyManager.formatCurrency(economy.balance()),
                  economyManager.formatCurrency(cost)),
              "#FFAA00");
        }
      }
    }
  }

  /**
   * Calculates the upkeep cost for a number of billable chunks.
   * Uses flat or progressive scaling based on config.
   *
   * @param billableChunks number of chunks after free chunk exemption
   * @return total cost
   */
  @NotNull
  public BigDecimal calculateUpkeepCost(int billableChunks) {
    if (billableChunks <= 0) {
      return BigDecimal.ZERO;
    }

    ConfigManager config = ConfigManager.get();
    BigDecimal cost;

    if ("progressive".equals(config.getUpkeepScalingMode())) {
      cost = calculateProgressiveCost(billableChunks, config.getUpkeepScalingTiers());
    } else {
      cost = config.getUpkeepCostPerChunk().multiply(BigDecimal.valueOf(billableChunks));
    }

    // Apply max cost cap
    BigDecimal cap = config.getUpkeepMaxCostCap();
    if (cap.compareTo(BigDecimal.ZERO) > 0 && cost.compareTo(cap) > 0) {
      cost = cap;
    }

    return cost.setScale(2, RoundingMode.HALF_UP);
  }

  /**
   * Calculates cost using progressive tiered pricing.
   *
   * @param billableChunks total billable chunks
   * @param tiers          tier definitions (chunkCount=0 means all remaining)
   * @return total cost
   */
  @NotNull
  public static BigDecimal calculateProgressiveCost(int billableChunks, @NotNull List<ScalingTier> tiers) {
    if (tiers.isEmpty()) {
      return BigDecimal.ZERO;
    }

    BigDecimal total = BigDecimal.ZERO;
    int remaining = billableChunks;

    for (ScalingTier tier : tiers) {
      if (remaining <= 0) break;

      int chunksInTier;
      if (tier.chunkCount() <= 0) {
        // All remaining chunks
        chunksInTier = remaining;
      } else {
        chunksInTier = Math.min(remaining, tier.chunkCount());
      }

      total = total.add(tier.costPerChunk().multiply(BigDecimal.valueOf(chunksInTier)));
      remaining -= chunksInTier;
    }

    return total;
  }

  private boolean isGraceExpired(@NotNull FactionEconomy economy, long gracePeriodMs) {
    if (economy.upkeepGraceStartTimestamp() == 0L) return false;
    return (System.currentTimeMillis() - economy.upkeepGraceStartTimestamp()) >= gracePeriodMs;
  }

  private void logToFaction(@NotNull UUID factionId, @NotNull FactionLog.LogType type,
               @NotNull String message) {
    Faction faction = factionManager.getFaction(factionId);
    if (faction != null) {
      Faction logged = faction.withLog(FactionLog.system(type, message));
      factionManager.updateFaction(logged);
    }
  }

  private void notifyFaction(@NotNull UUID factionId, @NotNull String message, @NotNull String hexColor) {
    if (notificationCallback != null) {
      try {
        notificationCallback.notifyFaction(factionId, message, hexColor);
      } catch (Exception e) {
        ErrorHandler.report("Failed to send upkeep notification", e);
      }
    }
  }

  /**
   * Formats a duration in milliseconds to a human-readable string (e.g. "24h 30m").
   */
  @NotNull
  public static String formatDuration(long ms) {
    if (ms <= 0) return "0m";
    long totalMinutes = ms / (60 * 1000);
    long hours = totalMinutes / 60;
    long minutes = totalMinutes % 60;
    if (hours > 0 && minutes > 0) {
      return hours + "h " + minutes + "m";
    } else if (hours > 0) {
      return hours + "h";
    } else {
      return minutes + "m";
    }
  }
}
