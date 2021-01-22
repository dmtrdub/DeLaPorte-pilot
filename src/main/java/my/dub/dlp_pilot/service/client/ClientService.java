package my.dub.dlp_pilot.service.client;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import my.dub.dlp_pilot.exception.TestRunEndException;
import my.dub.dlp_pilot.model.Bar;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.TimeFrame;
import my.dub.dlp_pilot.model.dto.LastBar;
import my.dub.dlp_pilot.model.dto.SymbolPair;
import my.dub.dlp_pilot.model.dto.Ticker;
import my.dub.dlp_pilot.repository.container.SymbolPairContainer;
import org.springframework.lang.NonNull;

/**
 * A service for API client-related operations.
 */
public interface ClientService {

    /**
     * Get all relevant (with similar pairs on other exchanges) symbol pairs for each exchange among passed. Add all
     * fetched objects to {@link SymbolPairContainer}.
     *
     * @param exchangeNames
     *         a non-null collection of {@link ExchangeName} of exchanges
     *
     * @throws TestRunEndException
     *         if no relevant symbols were found
     */
    void loadAllSymbolPairs(@NonNull Collection<ExchangeName> exchangeNames);

    /**
     * Get the number of loaded {@link SymbolPair}s for a specific exchange.
     *
     * @param exchangeName
     *         the non-null {@link ExchangeName} of exchange
     *
     * @return the number of loaded symbol pairs for a specific exchange
     */
    int getSymbolPairsCount(@NonNull ExchangeName exchangeName);

    /**
     * Remove a loaded {@link SymbolPair} of a specific exchange by its storage index.
     *
     * @param exchangeName
     *         the non-null {@link ExchangeName} of exchange
     * @param index
     *         symbol pair storage index
     *
     * @see SymbolPairContainer
     */
    void removeSymbolPair(@NonNull ExchangeName exchangeName, int index);

    /**
     * Get recent ticker for each symbol pair of a specific exchange. Set the exchange faulty if any error occurs.
     *
     * @param exchangeName
     *         the non-null {@link ExchangeName} of exchange
     *
     * @return a set (possibly empty) of loaded tickers
     */
    Set<Ticker> fetchTickers(@NonNull ExchangeName exchangeName);

    /**
     * Get a list of bars for a time frame, within specific dates, and for a symbol pair with a storage index, from a
     * specific exchange. Intended to be invoked on preload stage.
     *
     * @param exchangeName
     *         non-null {@link ExchangeName} of exchange
     * @param timeFrame
     *         non-null preload {@link TimeFrame}
     * @param startTime
     *         non-null {@link Instant} preload start time
     * @param symbolPairIndex
     *         symbol pair storage index
     * @param endTime
     *         non-null {@link Instant} preload end time
     *
     * @return a list of loaded bars. If the result list is empty, the load process is considered faulty
     *
     * @throws TestRunEndException
     *         if an {@link java.io.IOException} occurs during load process
     */
    List<Bar> fetchBarsPreload(@NonNull ExchangeName exchangeName, @NonNull TimeFrame timeFrame,
            @NonNull Instant startTime, int symbolPairIndex, Instant endTime);

    /**
     * Get a list of bars for a time frame and for a symbol pair with a storage index, from a specific exchange. Bars
     * are loaded only if the similar last bar, recorded to DB, is older than the selected time frame. Intended to be
     * invoked on refresh load stage.
     *
     * @param exchangeName
     *         non-null {@link ExchangeName} of exchange
     * @param timeFrame
     *         non-null {@link TimeFrame}
     * @param symbolPairIndex
     *         symbol pair storage index
     * @param lastBars
     *         a non-null collection of {@link LastBar} containing data for the last bar records in DB
     *
     * @return a list of loaded bars
     */
    List<Bar> fetchBars(@NonNull ExchangeName exchangeName, @NonNull TimeFrame timeFrame, int symbolPairIndex,
            @NonNull Collection<LastBar> lastBars);

    /**
     * Check loaded symbol pairs for relevancy (symbol pairs with similar pairs on other exchanges). Update records in
     * {@link SymbolPairContainer}.
     */
    void updateLoadedSymbolPairs();
}
