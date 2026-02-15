package com.hyperfactions.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Represents a player's power data for faction mechanics.
 *
 * @param uuid              the player's UUID
 * @param power             the current power level
 * @param maxPower          the maximum power this player can have (global config value)
 * @param lastDeath         when the player last died (epoch millis, 0 if never)
 * @param lastRegen         when power was last regenerated (epoch millis)
 * @param maxPowerOverride  per-player max power override (null = use global config)
 * @param powerLossDisabled absolute bypass: player never loses power from any source
 * @param claimDecayExempt  player treated as always online for claim decay calculations
 */
public record PlayerPower(
    @NotNull UUID uuid,
    double power,
    double maxPower,
    long lastDeath,
    long lastRegen,
    @Nullable Double maxPowerOverride,
    boolean powerLossDisabled,
    boolean claimDecayExempt
) {
    /**
     * Creates a new PlayerPower with default values.
     *
     * @param uuid         the player's UUID
     * @param startingPower the starting power value
     * @param maxPower     the maximum power
     * @return a new PlayerPower
     */
    public static PlayerPower create(@NotNull UUID uuid, double startingPower, double maxPower) {
        long now = System.currentTimeMillis();
        return new PlayerPower(uuid, startingPower, maxPower, 0, now, null, false, false);
    }

    /**
     * Gets the effective max power, considering per-player override.
     *
     * @return the effective max power for this player
     */
    public double getEffectiveMaxPower() {
        return maxPowerOverride != null ? maxPowerOverride : maxPower;
    }

    /**
     * Creates a copy with updated power.
     *
     * @param newPower the new power value
     * @return a new PlayerPower with clamped power
     */
    public PlayerPower withPower(double newPower) {
        double clamped = Math.max(0, Math.min(getEffectiveMaxPower(), newPower));
        return new PlayerPower(uuid, clamped, maxPower, lastDeath, lastRegen, maxPowerOverride, powerLossDisabled, claimDecayExempt);
    }

    /**
     * Creates a copy with power reduced by the death penalty.
     *
     * @param penalty the amount to reduce
     * @return a new PlayerPower with reduced power
     */
    public PlayerPower withDeathPenalty(double penalty) {
        double newPower = Math.max(0, power - penalty);
        return new PlayerPower(uuid, newPower, maxPower, System.currentTimeMillis(), lastRegen, maxPowerOverride, powerLossDisabled, claimDecayExempt);
    }

    /**
     * Creates a copy with power increased by regeneration.
     *
     * @param amount the amount to add
     * @return a new PlayerPower with increased power
     */
    public PlayerPower withRegen(double amount) {
        double effectiveMax = getEffectiveMaxPower();
        double newPower = Math.min(effectiveMax, power + amount);
        return new PlayerPower(uuid, newPower, maxPower, lastDeath, System.currentTimeMillis(), maxPowerOverride, powerLossDisabled, claimDecayExempt);
    }

    /**
     * Creates a copy with updated max power.
     *
     * @param newMaxPower the new max power
     * @return a new PlayerPower with updated max
     */
    public PlayerPower withMaxPower(double newMaxPower) {
        double newPower = Math.min(power, newMaxPower);
        return new PlayerPower(uuid, newPower, newMaxPower, lastDeath, lastRegen, maxPowerOverride, powerLossDisabled, claimDecayExempt);
    }

    /**
     * Creates a copy with a per-player max power override.
     * If the override is lower than current power, power is clamped down.
     *
     * @param override the max power override, or null to clear
     * @return a new PlayerPower with the override applied
     */
    public PlayerPower withMaxPowerOverride(@Nullable Double override) {
        double newEffectiveMax = override != null ? override : maxPower;
        double clampedPower = Math.min(power, newEffectiveMax);
        return new PlayerPower(uuid, clampedPower, maxPower, lastDeath, lastRegen, override, powerLossDisabled, claimDecayExempt);
    }

    /**
     * Creates a copy with power loss disabled flag toggled.
     *
     * @param disabled true to disable power loss
     * @return a new PlayerPower with the flag set
     */
    public PlayerPower withPowerLossDisabled(boolean disabled) {
        return new PlayerPower(uuid, power, maxPower, lastDeath, lastRegen, maxPowerOverride, disabled, claimDecayExempt);
    }

    /**
     * Creates a copy with claim decay exempt flag toggled.
     *
     * @param exempt true to exempt from claim decay
     * @return a new PlayerPower with the flag set
     */
    public PlayerPower withClaimDecayExempt(boolean exempt) {
        return new PlayerPower(uuid, power, maxPower, lastDeath, lastRegen, maxPowerOverride, powerLossDisabled, exempt);
    }

    /**
     * Gets the power as an integer (floored).
     *
     * @return the floored power value
     */
    public int getPowerInt() {
        return (int) Math.floor(power);
    }

    /**
     * Gets the max power as an integer (floored).
     *
     * @return the floored max power value
     */
    public int getMaxPowerInt() {
        return (int) Math.floor(maxPower);
    }

    /**
     * Gets the effective max power as an integer (floored).
     *
     * @return the floored effective max power value
     */
    public int getEffectiveMaxPowerInt() {
        return (int) Math.floor(getEffectiveMaxPower());
    }

    /**
     * Checks if power is at maximum.
     *
     * @return true if power >= effectiveMaxPower
     */
    public boolean isAtMax() {
        return power >= getEffectiveMaxPower();
    }

    /**
     * Gets the power percentage (0-100).
     *
     * @return the power percentage
     */
    public int getPowerPercent() {
        double effectiveMax = getEffectiveMaxPower();
        if (effectiveMax <= 0) return 0;
        return (int) Math.round((power / effectiveMax) * 100);
    }
}
