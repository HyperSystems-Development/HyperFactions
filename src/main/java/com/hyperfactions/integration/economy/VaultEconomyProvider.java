package com.hyperfactions.integration.economy;

import com.hyperfactions.util.Logger;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Economy provider for VaultUnlocked (Vault2 Economy API).
 * Uses reflection to integrate with VaultUnlocked's Economy interface.
 * <p>
 * Follows the same lazy-init + permanentFailure pattern as
 * {@link com.hyperfactions.integration.permissions.VaultUnlockedProvider}.
 * <p>
 * Actual class paths (from decompiled VaultUnlocked-Hytale 2.19.0):
 * - Main: net.cfh.vault.VaultUnlocked
 * - Economy: net.milkbowl.vault2.economy.Economy
 * - EconomyResponse: net.milkbowl.vault2.economy.EconomyResponse
 * <p>
 * Economy API uses BigDecimal for all amounts and String pluginName as first param.
 * EconomyResponse has a ResponseType enum: SUCCESS, FAILURE, NOT_IMPLEMENTED.
 */
public class VaultEconomyProvider {

    private static final String PLUGIN_NAME = "HyperFactions";

    private volatile boolean available = false;
    private volatile boolean permanentFailure = false;

    // Reflection references
    private Object economyService = null;
    private Method getBalanceMethod = null;
    private Method hasMethod = null;
    private Method withdrawMethod = null;
    private Method depositMethod = null;

    // EconomyResponse helpers
    private Method transactionSuccessMethod = null;

    /**
     * Initializes the VaultUnlocked economy provider.
     * Attempts to load VaultUnlocked classes via reflection.
     * Safe to call multiple times - returns immediately if already initialized
     * or permanently failed (ClassNotFoundException = not installed).
     */
    public void init() {
        if (available || permanentFailure) return;

        try {
            // Get VaultUnlocked main class
            Class<?> vaultUnlockedClass = Class.forName("net.cfh.vault.VaultUnlocked");

            // VaultUnlocked.economy() returns Optional<Economy>
            Method economyMethod = vaultUnlockedClass.getMethod("economy");
            Object econOptional = economyMethod.invoke(null);
            if (econOptional instanceof Optional<?> opt) {
                economyService = opt.orElse(null);
            }

            if (economyService == null) {
                // Economy service not registered yet - may become available later
                Logger.debug("[VaultEconomyProvider] Economy service not available yet");
                return;
            }

            // Get Economy methods via interface
            Class<?> economyClass = Class.forName("net.milkbowl.vault2.economy.Economy");

            // getBalance(String pluginName, UUID accountID) -> BigDecimal
            getBalanceMethod = economyClass.getMethod("getBalance", String.class, UUID.class);

            // has(String pluginName, UUID accountID, BigDecimal amount) -> boolean
            hasMethod = economyClass.getMethod("has", String.class, UUID.class, BigDecimal.class);

            // withdraw(String pluginName, UUID accountID, BigDecimal amount) -> EconomyResponse
            withdrawMethod = economyClass.getMethod("withdraw", String.class, UUID.class, BigDecimal.class);

            // deposit(String pluginName, UUID accountID, BigDecimal amount) -> EconomyResponse
            depositMethod = economyClass.getMethod("deposit", String.class, UUID.class, BigDecimal.class);

            // EconomyResponse.transactionSuccess() -> boolean
            Class<?> responseClass = Class.forName("net.milkbowl.vault2.economy.EconomyResponse");
            transactionSuccessMethod = responseClass.getMethod("transactionSuccess");

            available = true;
            Logger.info("[VaultEconomyProvider] VaultUnlocked economy provider initialized");

        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            permanentFailure = true;
            Logger.debug("[VaultEconomyProvider] VaultUnlocked not installed");
        } catch (Exception e) {
            // Other errors may be temporary (service not registered yet, etc.)
            Logger.debug("[VaultEconomyProvider] Init deferred: %s", e.getMessage());
        }
    }

    /**
     * Ensures the provider is initialized. Called before every operation
     * to support lazy initialization when VaultUnlocked loads after HyperFactions.
     */
    private void ensureInitialized() {
        if (!available && !permanentFailure) {
            init();
        }
    }

    /**
     * Checks if the economy provider is available and initialized.
     *
     * @return true if VaultUnlocked economy is available
     */
    public boolean isAvailable() {
        ensureInitialized();
        return available;
    }

    /**
     * Checks if initialization permanently failed (VaultUnlocked not installed).
     *
     * @return true if VaultUnlocked is definitively not present
     */
    public boolean isPermanentFailure() {
        return permanentFailure;
    }

    /**
     * Gets a player's balance from VaultUnlocked.
     *
     * @param playerUuid the player's UUID
     * @return the balance, or 0.0 if unavailable
     */
    public double getBalance(@NotNull UUID playerUuid) {
        ensureInitialized();
        if (!available || economyService == null || getBalanceMethod == null) {
            return 0.0;
        }

        try {
            Object result = getBalanceMethod.invoke(economyService, PLUGIN_NAME, playerUuid);
            if (result instanceof BigDecimal bd) {
                return bd.doubleValue();
            }
            return 0.0;
        } catch (Exception e) {
            Logger.debug("[VaultEconomyProvider] Exception getting balance: %s", e.getMessage());
            return 0.0;
        }
    }

    /**
     * Checks if a player has at least the specified amount.
     *
     * @param playerUuid the player's UUID
     * @param amount     the amount to check
     * @return true if the player has sufficient funds
     */
    public boolean has(@NotNull UUID playerUuid, double amount) {
        ensureInitialized();
        if (!available || economyService == null || hasMethod == null) {
            return false;
        }

        try {
            Object result = hasMethod.invoke(economyService, PLUGIN_NAME, playerUuid,
                    BigDecimal.valueOf(amount));
            return result instanceof Boolean b && b;
        } catch (Exception e) {
            Logger.debug("[VaultEconomyProvider] Exception checking has: %s", e.getMessage());
            return false;
        }
    }

    /**
     * Withdraws money from a player's account.
     *
     * @param playerUuid the player's UUID
     * @param amount     the amount to withdraw
     * @return true if the withdrawal succeeded
     */
    public boolean withdraw(@NotNull UUID playerUuid, double amount) {
        ensureInitialized();
        if (!available || economyService == null || withdrawMethod == null) {
            return false;
        }

        try {
            Object response = withdrawMethod.invoke(economyService, PLUGIN_NAME, playerUuid,
                    BigDecimal.valueOf(amount));
            return isSuccess(response);
        } catch (Exception e) {
            Logger.debug("[VaultEconomyProvider] Exception withdrawing: %s", e.getMessage());
            return false;
        }
    }

    /**
     * Deposits money into a player's account.
     *
     * @param playerUuid the player's UUID
     * @param amount     the amount to deposit
     * @return true if the deposit succeeded
     */
    public boolean deposit(@NotNull UUID playerUuid, double amount) {
        ensureInitialized();
        if (!available || economyService == null || depositMethod == null) {
            return false;
        }

        try {
            Object response = depositMethod.invoke(economyService, PLUGIN_NAME, playerUuid,
                    BigDecimal.valueOf(amount));
            return isSuccess(response);
        } catch (Exception e) {
            Logger.debug("[VaultEconomyProvider] Exception depositing: %s", e.getMessage());
            return false;
        }
    }

    /**
     * Checks if an EconomyResponse indicates success.
     */
    private boolean isSuccess(Object response) {
        if (response == null || transactionSuccessMethod == null) return false;
        try {
            Object result = transactionSuccessMethod.invoke(response);
            return result instanceof Boolean b && b;
        } catch (Exception e) {
            return false;
        }
    }
}
