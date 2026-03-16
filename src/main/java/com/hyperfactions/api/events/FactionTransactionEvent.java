package com.hyperfactions.api.events;

import com.hyperfactions.api.EconomyAPI;
import java.math.BigDecimal;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Published after a faction treasury transaction completes successfully.
 *
 * @param factionId       the faction
 * @param transactionType the type of transaction
 * @param amount          the amount (always positive)
 * @param balanceAfter    the balance after the transaction
 * @param actorUuid       the player who initiated (null for system)
 * @param description     transaction description
 */
public record FactionTransactionEvent(
    @NotNull UUID factionId,
    @NotNull EconomyAPI.TransactionType transactionType,
    @NotNull BigDecimal amount,
    @NotNull BigDecimal balanceAfter,
    @Nullable UUID actorUuid,
    @NotNull String description
) {}
