package my.dub.dlp_pilot.service;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import my.dub.dlp_pilot.model.Bar;
import my.dub.dlp_pilot.model.TimeFrame;
import my.dub.dlp_pilot.model.dto.SymbolPair;
import my.dub.dlp_pilot.model.dto.Ticker;
import org.springframework.lang.NonNull;

/**
 * A service for REST client communication with exchanges.
 */
public interface ExchangeClientService {

    /**
     * Get a list of all symbol pairs ready for trade.
     *
     * @return a list of {@link SymbolPair} objects
     *
     * @throws IOException
     *         if an error occurs when executing request
     */
    List<SymbolPair> fetchSymbolPairs() throws IOException;

    /**
     * Get all latest tickers with symbol pairs from the specified list.
     *
     * @param symbolPairs
     *         a non-null list of {@link SymbolPair} objects
     *
     * @return a set of recent {@link Ticker} objects
     *
     * @throws IOException
     *         if an error occurs when executing request
     */
    Set<Ticker> fetchAllTickers(@NonNull List<SymbolPair> symbolPairs) throws IOException;

    /**
     * Get a list of bars for a specific symbol pair, time frame and within specific dates.
     *
     * @param symbolPair
     *         a non-null {@link SymbolPair} object
     * @param timeFrame
     *         a non-null {@link TimeFrame} object
     * @param startTime
     *         a non-null {@link Instant} representing time 'from'
     * @param endTime
     *         a non-null {@link Instant} representing time 'to'
     *
     * @return a list of {@link Bar} objects
     *
     * @throws IOException
     *         if an error occurs when executing request
     */
    List<Bar> fetchBars(@NonNull SymbolPair symbolPair, @NonNull TimeFrame timeFrame, @NonNull Instant startTime,
            @NonNull Instant endTime) throws IOException;

    /**
     * Get a list of recent bars for a specific symbol pair and time frame, limited by a specific number of records.
     *
     * @param symbolPair
     *         a non-null {@link SymbolPair} object
     * @param timeFrame
     *         a non-null {@link TimeFrame} object
     * @param barsLimit
     *         a maximum number of fetched records
     *
     * @return a list of {@link Bar} objects
     *
     * @throws IOException
     *         if an error occurs when executing request
     */
    List<Bar> fetchBars(@NonNull SymbolPair symbolPair, @NonNull TimeFrame timeFrame, long barsLimit)
            throws IOException;
}
