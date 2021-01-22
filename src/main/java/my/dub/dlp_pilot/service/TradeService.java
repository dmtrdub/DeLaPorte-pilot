package my.dub.dlp_pilot.service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import my.dub.dlp_pilot.configuration.ParametersHolder;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.TestRun;
import my.dub.dlp_pilot.model.Trade;
import my.dub.dlp_pilot.model.TradeResultType;
import my.dub.dlp_pilot.model.dto.Ticker;
import my.dub.dlp_pilot.repository.container.TradeContainer;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.lang.NonNull;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

/**
 * Service for core trade operations, as well as for managing {@link Trade} entities.
 */
public interface TradeService {

    /**
     * Check if trade is allowed to open. Rules for opening trade are as follows:
     * <ul>
     *     <li>at least one ticker is not stale</li>
     *     <li>there are no similar trades currently opened (same base, target, short exchange and long exchanges)</li>
     *     <li>both exchanges (short and long) are not faulty</li>
     *     <li>there is no current detrimental record for current base, target, short and long exchanges</li>
     *     <li>current numbers of opened trades do not exceed the limit (parallel trades number app parameter) on
     *     both exchanges</li>
     *     <li>percentage difference between open prices for both sides is within the allowed boundaries (entry
     *     min/max percentage differences app parameters)</li>
     *     <li>total expected losses after trade open would not exceed the allowed percentage from entry amount
     *     (detriment amount percentage app parameter)</li>
     *     <li>expected income if prices will return to the average price difference level exceeds the profit
     *     percentage from entry amount (entry profit percentage app parameter)</li>
     * </ul>
     * If all conditions are met, a new {@link Trade} is created and saved to {@link TradeContainer}.
     *
     * @param tickerShort
     *         non-null short {@link Ticker} for new trade
     * @param tickerLong
     *         non-null long {@link Ticker} for new trade
     * @param averagePriceDifference
     *         non-null average price difference between short and long exchanges
     * @param testRun
     *         non-null current {@link TestRun}
     */
    void checkTradeOpen(@NonNull Ticker tickerShort, @NonNull Ticker tickerLong,
            @NonNull BigDecimal averagePriceDifference, @NonNull TestRun testRun);

    /**
     * A retryable method for handling all opened {@link Trade}s for a specific exchange. Retries in case of a {@link
     * LockAcquisitionException} that may occur due to a heavily concurrent DB access.
     * Closing a trade implies its removal from a local container and persisting to DB. For a trade to close, one of the
     * following conditions should be met:
     * <ul>
     *     <li>TIMEOUT - trade is opened longer than allowed before timeout (trade timeout duration app parameter)</li>
     *     <li>SUCCESSFUL - trade's income is currently greater than or equal to profit percentages set as app
     *     parameters ({@link ParametersHolder#getProfitPercentageOnExitSum(long)}</li>
     *     <li>DETRIMENTAL - trade's income is currently less than allowed negative percentage from entry amount
     *     (detriment amount percentage app parameter) <b>unless</b> trade is in detrimental sync condition</li>
     * </ul>
     *
     * @param exchangeName
     *         a non-null {@link ExchangeName} of exchange
     */
    @Retryable(value = LockAcquisitionException.class, backoff = @Backoff(100))
    void handleTrades(@NonNull ExchangeName exchangeName);

    /**
     * A retryable method for closing all opened {@link Trade}s for a specific exchange with a set result type. Retries
     * in case of a {@link LockAcquisitionException} that may occur due to a heavily concurrent DB access.
     * Closing a trade implies its removal from a local container and persisting to DB.
     *
     * @param exchangeName
     *         a non-null {@link ExchangeName} of exchange
     * @param tradeResultType
     *         a non-null {@link TradeResultType} to set for all closed trades
     */
    @Retryable(value = LockAcquisitionException.class, backoff = @Backoff(0))
    void closeTrades(@NonNull ExchangeName exchangeName, @NonNull TradeResultType tradeResultType);

    /**
     * Find all {@link Trade}s that were not yet written to result file within a specific test run.
     *
     * @param testRun
     *         a non-null {@link TestRun} entity
     *
     * @return a list of trades sorted by {@link Trade#getEndTime()}
     */
    List<Trade> getCompletedTradesNotWrittenToFile(@NonNull TestRun testRun);

    /**
     * Save or update all passed {@link Trade}s.
     *
     * @param trades
     *         nullable collection of trade entities
     */
    void saveOrUpdate(Collection<Trade> trades);

    /**
     * Check if all {@link Trade}s are closed - no records exist in local container ({@link TradeContainer}).
     *
     * @return {@code true} if all trades are closed, {@code false} otherwise
     */
    boolean isAllTradesClosed();
}
