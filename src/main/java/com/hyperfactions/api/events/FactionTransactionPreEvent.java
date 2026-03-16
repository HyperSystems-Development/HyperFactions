package com.hyperfactions.api.events;

import com.hyperfactions.api.EconomyAPI;
import java.math.BigDecimal;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Published before a faction treasury transaction. Can be cancelled to block it.
 */
public final class FactionTransactionPreEvent implements Cancellable {

  private final UUID factionId;
  private final EconomyAPI.TransactionType transactionType;
  private final BigDecimal amount;
  private final UUID actorUuid;
  private final String description;
  private boolean cancelled;
  private String cancelReason;

  public FactionTransactionPreEvent(@NotNull UUID factionId,
                                    @NotNull EconomyAPI.TransactionType transactionType,
                                    @NotNull BigDecimal amount,
                                    @Nullable UUID actorUuid,
                                    @NotNull String description) {
    this.factionId = factionId;
    this.transactionType = transactionType;
    this.amount = amount;
    this.actorUuid = actorUuid;
    this.description = description;
  }

  @NotNull public UUID factionId() { return factionId; }
  @NotNull public EconomyAPI.TransactionType transactionType() { return transactionType; }
  @NotNull public BigDecimal amount() { return amount; }
  @Nullable public UUID actorUuid() { return actorUuid; }
  @NotNull public String description() { return description; }

  @Override public boolean isCancelled() { return cancelled; }
  @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
  @Override @Nullable public String getCancelReason() { return cancelReason; }
  @Override public void setCancelReason(@Nullable String reason) { this.cancelReason = reason; }
}
