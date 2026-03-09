package com.hyperfactions.economy;

import com.hyperfactions.config.modules.EconomyConfig.ScalingTier;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for UpkeepProcessor cost calculation and utility methods.
 * Tests the static/pure methods that don't require manager dependencies.
 */
class UpkeepProcessorTest {

  // === Progressive Cost Calculation ===

  @Test
  void progressiveCost_emptyTiers_returnsZero() {
    BigDecimal cost = UpkeepProcessor.calculateProgressiveCost(10, List.of());
    assertEquals(0, cost.compareTo(BigDecimal.ZERO));
  }

  @Test
  void progressiveCost_zeroBillable_returnsZero() {
    List<ScalingTier> tiers = List.of(new ScalingTier(10, new BigDecimal("2.00")));
    BigDecimal cost = UpkeepProcessor.calculateProgressiveCost(0, tiers);
    assertEquals(0, cost.compareTo(BigDecimal.ZERO));
  }

  @Test
  void progressiveCost_singleTier_allChunks() {
    // 10 chunks at $2/ea = $20
    List<ScalingTier> tiers = List.of(new ScalingTier(0, new BigDecimal("2.00")));
    BigDecimal cost = UpkeepProcessor.calculateProgressiveCost(10, tiers);
    assertEquals(0, cost.compareTo(new BigDecimal("20.00")));
  }

  @Test
  void progressiveCost_multipleTiers_exactFit() {
    // 10 chunks at $2, 15 chunks at $3 = $20 + $45 = $65
    List<ScalingTier> tiers = List.of(
        new ScalingTier(10, new BigDecimal("2.00")),
        new ScalingTier(15, new BigDecimal("3.00"))
    );
    BigDecimal cost = UpkeepProcessor.calculateProgressiveCost(25, tiers);
    assertEquals(0, cost.compareTo(new BigDecimal("65.00")));
  }

  @Test
  void progressiveCost_multipleTiersWithRemainder() {
    // Example from plan: 30 billable chunks
    // First 10 @ $2 = $20, next 15 @ $3 = $45, remaining 5 @ $5 = $25
    // Total = $90
    List<ScalingTier> tiers = List.of(
        new ScalingTier(10, new BigDecimal("2.00")),
        new ScalingTier(15, new BigDecimal("3.00")),
        new ScalingTier(0, new BigDecimal("5.00"))
    );
    BigDecimal cost = UpkeepProcessor.calculateProgressiveCost(30, tiers);
    assertEquals(0, cost.compareTo(new BigDecimal("90.00")));
  }

  @Test
  void progressiveCost_fewerChunksThanFirstTier() {
    // Only 5 chunks, first tier covers 10
    // 5 chunks at $2 = $10
    List<ScalingTier> tiers = List.of(
        new ScalingTier(10, new BigDecimal("2.00")),
        new ScalingTier(0, new BigDecimal("5.00"))
    );
    BigDecimal cost = UpkeepProcessor.calculateProgressiveCost(5, tiers);
    assertEquals(0, cost.compareTo(new BigDecimal("10.00")));
  }

  @Test
  void progressiveCost_exactlyOneTierBoundary() {
    // Exactly 10 chunks, exactly one tier
    List<ScalingTier> tiers = List.of(
        new ScalingTier(10, new BigDecimal("2.00")),
        new ScalingTier(0, new BigDecimal("5.00"))
    );
    BigDecimal cost = UpkeepProcessor.calculateProgressiveCost(10, tiers);
    assertEquals(0, cost.compareTo(new BigDecimal("20.00")));
  }

  @Test
  void progressiveCost_moreChunksThanDefinedTiers() {
    // 20 chunks but only one tier for 10 — remaining 10 get nothing
    List<ScalingTier> tiers = List.of(
        new ScalingTier(10, new BigDecimal("2.00"))
    );
    BigDecimal cost = UpkeepProcessor.calculateProgressiveCost(20, tiers);
    // Only 10 are covered by tiers, the other 10 are unaccounted for
    assertEquals(0, cost.compareTo(new BigDecimal("20.00")));
  }

  // === Format Duration ===

  @Test
  void formatDuration_zero() {
    assertEquals("0m", UpkeepProcessor.formatDuration(0));
  }

  @Test
  void formatDuration_negative() {
    assertEquals("0m", UpkeepProcessor.formatDuration(-1000));
  }

  @Test
  void formatDuration_minutesOnly() {
    assertEquals("30m", UpkeepProcessor.formatDuration(30 * 60 * 1000L));
  }

  @Test
  void formatDuration_hoursOnly() {
    assertEquals("2h", UpkeepProcessor.formatDuration(2 * 60 * 60 * 1000L));
  }

  @Test
  void formatDuration_hoursAndMinutes() {
    assertEquals("24h 30m", UpkeepProcessor.formatDuration((24 * 60 + 30) * 60 * 1000L));
  }

  @Test
  void formatDuration_oneMinute() {
    assertEquals("1m", UpkeepProcessor.formatDuration(60 * 1000L));
  }

  @Test
  void formatDuration_oneHour() {
    assertEquals("1h", UpkeepProcessor.formatDuration(60 * 60 * 1000L));
  }
}
