package com.outlaw.economy.api;

import java.util.UUID;

/**
 * Shared economy service contract exposed by OutlawEconomy and consumed by dependant plugins.
 *
 * <p>The interface intentionally mirrors the capabilities expected by Vault-enabled plugins so
 * implementations can easily bridge to Vault or other services.</p>
 */
public interface EconomyService {

    /**
     * Returns the current balance for the given player.
     *
     * @param playerId unique identifier of the player.
     * @return the balance, never negative.
     */
    double getBalance(UUID playerId);

    /**
     * Deposits funds into the player's account.
     *
     * @param playerId unique identifier of the player.
     * @param amount   strictly positive amount to deposit.
     * @param reason   textual explanation stored for audit/debug purposes.
     * @return {@code true} when the deposit succeeds.
     */
    boolean deposit(UUID playerId, double amount, String reason);

    /**
     * Withdraws funds from the player's account when sufficient balance is available.
     *
     * @param playerId unique identifier of the player.
     * @param amount   strictly positive amount to withdraw.
     * @param reason   textual explanation stored for audit/debug purposes.
     * @return {@code true} when the withdrawal succeeds.
     */
    boolean withdraw(UUID playerId, double amount, String reason);

    /**
     * Formats a raw amount for user-facing messages.
     *
     * @param amount raw numeric amount.
     * @return formatted string (currency symbol, spacing, etc.).
     */
    String format(double amount);

    /**
     * Returns the canonical currency code used by the economy implementation.
     *
     * @return ISO currency code or human-friendly token name.
     */
    String currencyCode();
}
