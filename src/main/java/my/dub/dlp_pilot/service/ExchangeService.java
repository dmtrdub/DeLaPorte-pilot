package my.dub.dlp_pilot.service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import my.dub.dlp_pilot.model.Exchange;
import my.dub.dlp_pilot.model.ExchangeName;
import org.springframework.lang.NonNull;

/**
 * A service for managing {@link Exchange} entities.
 */
public interface ExchangeService {
    /**
     * Get the total number of existing {@link Exchange} entities.
     *
     * @return the count number
     */
    long countAll();

    /**
     * Find all existing {@link Exchange} entities.
     *
     * @return a Set of all entities
     */
    Set<Exchange> findAll();

    /**
     * Find an {@link Exchange} entity by its unique ID.
     *
     * @param id
     *         a non-null ID to filter on
     *
     * @return an {@link Optional} of Exchange
     */
    Optional<Exchange> findById(@NonNull Long id);

    /**
     * Find an {@link Exchange} entity by its {@link ExchangeName}.
     *
     * @param exchangeName
     *         a non-null exchange name to filter on
     *
     * @return an Exchange result
     *
     * @throws my.dub.dlp_pilot.exception.MissingEntityException
     *         if no suitable record was found
     */
    Exchange findByName(@NonNull ExchangeName exchangeName);

    /**
     * Get the sum of all expenses (deposit, withdrawal and taker fees) in USD of an {@link Exchange} with a specified
     * {@link Exchange#getName()} for a specific amount.
     *
     * @param exchangeName
     *         a non-null exchange name to filter on
     * @param tradeAmount
     *         a non-null BigDecimal amount for expenses calculation
     *
     * @return a {@link BigDecimal} sum of expenses
     */
    BigDecimal getTotalExpenses(@NonNull ExchangeName exchangeName, @NonNull BigDecimal tradeAmount);

    /**
     * Enable or disable fault for an {@link Exchange} with a specified {@link Exchange#getName()}.
     *
     * @param exchangeName
     *         a non-null exchange name to filter on
     * @param faulty
     *         value of fault
     */
    void updateExchangeFault(@NonNull ExchangeName exchangeName, boolean faulty);

    /**
     * Check if an {@link Exchange} with a specified {@link Exchange#getName()} is faulty. A faulty Exchange has
     * connection issues or does not function as expected.
     *
     * @param exchangeName
     *         a non-null exchange name to filter on
     *
     * @return a boolean value of fault
     */
    boolean isExchangeFaulty(@NonNull ExchangeName exchangeName);
}
