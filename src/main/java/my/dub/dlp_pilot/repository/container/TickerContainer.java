package my.dub.dlp_pilot.repository.container;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import my.dub.dlp_pilot.Constants;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.dto.PriceData;
import my.dub.dlp_pilot.model.dto.Ticker;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * An in-memory container for {@link Ticker} objects. Uniqueness of each added object is checked using {@link
 * Ticker#isSimilar(PriceData)}. If a {@link Ticker} object has a stale price (bid and ask/ close) i.e. price hasn't
 * changed, then this object would not be added to container.
 * <p>
 * Uses separate concurrent sets to avoid concurrency exceptions without using synchronization.
 */
@Component
public class TickerContainer {

    // Exclude concurrency exceptions by decentralizing storage
    private final Set<Ticker> bigONETickers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Ticker> binanceTickers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Ticker> bitBayTickers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Ticker> bitfinexTickers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Ticker> bitmartTickers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Ticker> bitmaxTickers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Ticker> bittrexTickers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Ticker> exmoTickers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Ticker> gateTickers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Ticker> huobiTickers = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Get all records in container.
     *
     * @return non-null set of all {@link Ticker} objects in container
     */
    public Set<Ticker> getAll() {
        return getAllStream().collect(Collectors.toSet());
    }

    /**
     * Get all records in container as a {@link Stream}.
     *
     * @return a Stream of all Ticker records
     */
    public Stream<Ticker> getAllStream() {
        return Stream.of(bigONETickers, binanceTickers, bitBayTickers, bitfinexTickers, bitmartTickers, bitmaxTickers,
                         bittrexTickers, exmoTickers, gateTickers, huobiTickers).flatMap(Collection::stream);
    }

    /**
     * Get all records for a specific {@link ExchangeName}.
     *
     * @param exchangeName
     *         a non-null exchange name
     *
     * @return a non-null set of {@link Ticker} objects with the specified exchange name
     */
    public Set<Ticker> getTickers(@NonNull ExchangeName exchangeName) {
        checkNotNull(exchangeName, Constants.NULL_ARGUMENT_MESSAGE, "exchangeName");

        return tickerSet(exchangeName);
    }

    /**
     * Get a record with a specific {@link Ticker#getExchangeName}, {@link Ticker#getBase()} and {@link
     * Ticker#getTarget()}.
     *
     * @param exchangeName
     *         a non-null exchange name
     * @param base
     *         a not empty base
     * @param target
     *         a not empty target
     *
     * @return an {@link Optional} of searched Ticker
     */
    public Optional<Ticker> getTicker(@NonNull ExchangeName exchangeName, @NonNull String base,
            @NonNull String target) {
        checkNotNull(exchangeName, Constants.NULL_ARGUMENT_MESSAGE, "exchangeName");
        checkArgument(StringUtils.isNotEmpty(base), Constants.EMPTY_STRING_ARGUMENT_MESSAGE, "base");
        checkArgument(StringUtils.isNotEmpty(target), Constants.EMPTY_STRING_ARGUMENT_MESSAGE, "target");

        Set<Ticker> tickers = tickerSet(exchangeName);
        if (CollectionUtils.isEmpty(tickers)) {
            return Optional.empty();
        }
        return tickers.stream().filter(ticker -> ticker.getBase().equals(base) && ticker.getTarget().equals(target))
                .findFirst();
    }

    /**
     * Add multiple {@link Ticker} objects to container. For an object to be added to container, replacing similar
     * Ticker, it should have a different bid or ask price, and a different close price. Once a new ticker is added, its
     * previous prices are updated.
     *
     * @param exchangeName
     *         a non-null exchange name
     * @param tickers
     *         a nullable collection of tickers, having {@link Ticker#getExchangeName()} = {@param exchangeName}
     */
    public void addTickers(@NonNull ExchangeName exchangeName, @Nullable Collection<Ticker> tickers) {
        checkNotNull(exchangeName, Constants.NULL_ARGUMENT_MESSAGE, "exchangeName");
        if (CollectionUtils.isEmpty(tickers)) {
            return;
        }
        checkArgument(tickers.stream().allMatch(ticker -> exchangeName.equals(ticker.getExchangeName())),
                      "Argument exchangeName does not match with tickers exchangeName!");

        Set<Ticker> tickerSet = tickerSet(exchangeName);
        tickers.forEach(newTicker -> getTicker(exchangeName, newTicker.getBase(), newTicker.getTarget())
                .ifPresentOrElse(existingTicker -> {
                    BigDecimal existingPriceAsk = existingTicker.getPriceAsk();
                    BigDecimal existingPriceBid = existingTicker.getPriceBid();
                    if ((existingPriceAsk.compareTo(newTicker.getPriceAsk()) != 0
                            || existingPriceBid.compareTo(newTicker.getPriceBid()) != 0)) {
                        BigDecimal existingClosePrice = existingTicker.getClosePrice();
                        if (existingClosePrice != null
                                && existingClosePrice.compareTo(newTicker.getClosePrice()) == 0) {
                            return;
                        }
                        newTicker.setPreviousPriceAsk(existingPriceAsk);
                        newTicker.setPreviousPriceBid(existingPriceBid);
                        tickerSet.remove(existingTicker);
                        tickerSet.add(newTicker);
                    }
                }, () -> tickerSet.add(newTicker)));
    }

    private Set<Ticker> tickerSet(@NonNull ExchangeName exchangeName) {
        checkNotNull(exchangeName, Constants.NULL_ARGUMENT_MESSAGE, "exchangeName");
        switch (exchangeName) {
            case BIGONE:
                return bigONETickers;
            case BINANCE:
                return binanceTickers;
            case BITBAY:
                return bitBayTickers;
            case BITFINEX:
                return bitfinexTickers;
            case BITMART:
                return bitmartTickers;
            case BITMAX:
                return bitmaxTickers;
            case BITTREX:
                return bittrexTickers;
            case EXMO:
                return exmoTickers;
            case GATE:
                return gateTickers;
            case HUOBI:
                return huobiTickers;
        }
        return new HashSet<>();
    }
}
