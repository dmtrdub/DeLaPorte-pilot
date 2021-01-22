package my.dub.dlp_pilot.service;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import my.dub.dlp_pilot.exception.MissingEntityException;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.dto.Ticker;
import my.dub.dlp_pilot.repository.container.TickerContainer;
import org.springframework.lang.NonNull;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

/**
 * Service for managing {@link Ticker} entities.
 */
public interface TickerService {

    /**
     * Get recent tickers from the exchange with the specified {@link ExchangeName}. Save loaded objects to {@link
     * TickerContainer}.
     *
     * @param exchangeName
     *         a non-null exchange name
     */
    void fetchAndSave(@NonNull ExchangeName exchangeName);

    /**
     * Get loaded tickers from {@link TickerContainer} with a specific {@link ExchangeName}.
     *
     * @param exchangeName
     *         a non-null exchange name
     *
     * @return a set of saved {@link Ticker} objects
     */
    Set<Ticker> getTickers(@NonNull ExchangeName exchangeName);

    /**
     * Get all loaded tickers from {@link TickerContainer}.
     *
     * @return a set containing all saved {@link Ticker} objects
     */
    Set<Ticker> getAllTickers();

    /**
     * Get a ticker with specified exchange name, base and target. If no matching object was found, throw {@link
     * MissingEntityException} and retry 3 times before propagating exception.
     *
     * @param exchangeName
     *         a non-null {@link ExchangeName} to filter on
     * @param base
     *         a non-null base to filter on
     * @param target
     *         a non-null target to filter on
     *
     * @return
     */
    @Retryable(value = MissingEntityException.class, backoff = @Backoff(0))
    Ticker getTickerWithRetry(@NonNull ExchangeName exchangeName, @NonNull String base, @NonNull String target);

    /**
     * Find an equivalent valid ticker to the original, from the passed ticker set. The equivalency is defined by
     * equality of {@link Ticker#getBase()} and {@link Ticker#getTarget()}. The validity is defined by non-null positive
     * or zero bid and ask prices.
     *
     * @param originalTicker
     *         non-null ticker with input data for search
     * @param tickerSet
     *         non-null set of tickers to search in
     *
     * @return an {@link Optional} of similar ticker
     */
    Optional<Ticker> findValidEquivalentTickerFromSet(@NonNull Ticker originalTicker, @NonNull Set<Ticker> tickerSet);

    /**
     * Check separately if 1st and 2nd ticker passed are stale - the period from ticker creation is longer than duration
     * of passed stale interval. Update the {@link Ticker#isStale()} value.
     *
     * @param ticker1
     *         non-null 1st ticker for stale check
     * @param ticker2
     *         non-null 2nd ticker for stale check
     * @param staleIntervalDuration
     *         non-null {@link Duration} that defines if ticker is stale
     *
     * @return {@code true} if both tickers are stale, {@code false} otherwise
     */
    boolean checkStale(@NonNull Ticker ticker1, @NonNull Ticker ticker2, @NonNull Duration staleIntervalDuration);
}
