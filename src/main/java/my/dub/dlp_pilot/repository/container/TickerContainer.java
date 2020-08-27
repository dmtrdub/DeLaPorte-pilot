package my.dub.dlp_pilot.repository.container;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.Ticker;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class TickerContainer {

    // Exclude concurrency exceptions by decentralizing storage
    private final Set<Ticker> bigONETickers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Ticker> binanceTickers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Ticker> bitBayTickers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Ticker> bitfinexTickers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Ticker> bithumbTickers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Ticker> bitmartTickers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Ticker> bitmaxTickers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Ticker> bittrexTickers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Ticker> bwTickers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Ticker> coinoneTickers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Ticker> exmoTickers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Ticker> gateTickers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Ticker> huobiTickers = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private Set<Ticker> tickerSet(ExchangeName exchangeName) {
        switch (exchangeName) {
            case BIGONE:
                return bigONETickers;
            case BINANCE:
                return binanceTickers;
            case BITBAY:
                return bitBayTickers;
            case BITFINEX:
                return bitfinexTickers;
            case BITHUMB:
                return bithumbTickers;
            case BITMART:
                return bitmartTickers;
            case BITMAX:
                return bitmaxTickers;
            case BITTREX:
                return bittrexTickers;
            case BW:
                return bwTickers;
            case COINONE:
                return coinoneTickers;
            case EXMO:
                return exmoTickers;
            case GATE:
                return gateTickers;
            case HUOBI:
                return huobiTickers;
        }
        return Collections.emptySet();
    }

    public Set<Ticker> getAll(boolean includeStale) {
        Stream<Ticker> tickerStream = getAllStream();
        if (!includeStale) {
            tickerStream = tickerStream.filter(ticker -> !ticker.isStale());
        }
        return tickerStream.collect(Collectors.toSet());
    }

    public Stream<Ticker> getAllStream() {
        return Stream.of(bigONETickers, binanceTickers, bitBayTickers, bitfinexTickers, bithumbTickers, bitmartTickers,
                         bitmaxTickers, bittrexTickers, bwTickers, coinoneTickers, exmoTickers, gateTickers,
                         huobiTickers).flatMap(Collection::stream);
    }

    public Set<Ticker> getTickers(ExchangeName exchangeName, boolean includeStale) {
        checkNotNull(exchangeName, "ExchangeName cannot be null when searching Tickers!");

        Set<Ticker> result = tickerSet(exchangeName);
        if (!includeStale) {
            return result.stream().filter(ticker -> !ticker.isStale()).collect(Collectors.toSet());
        }
        return result;
    }

    public Optional<Ticker> getTicker(ExchangeName exchangeName, String base, String target) {
        checkNotNull(exchangeName, "ExchangeName cannot be null when searching Ticker!");
        checkArgument(!StringUtils.isEmpty(base), "Base cannot be empty when searching Ticker!");
        checkArgument(!StringUtils.isEmpty(target), "Target cannot be empty when searching Ticker!");

        Set<Ticker> tickers = tickerSet(exchangeName);
        if (CollectionUtils.isEmpty(tickers)) {
            log.error("Ticker set for {} exchange is empty!", exchangeName);
            return Optional.empty();
        }
        return tickers.stream().filter(ticker -> ticker.getBase().equals(base) && ticker.getTarget().equals(target))
                .findFirst();
    }

    public void addTickers(ExchangeName exchangeName, Collection<Ticker> tickers) {
        if (exchangeName == null || CollectionUtils.isEmpty(tickers)) {
            return;
        }
        Set<Ticker> tickerSet = tickerSet(exchangeName);
        tickers.forEach(newTicker -> {
            Ticker existingTicker =
                    tickerSet.stream().filter(exTicker -> exTicker.isSimilar(newTicker)).findFirst().orElse(null);
            if (existingTicker != null) {
                BigDecimal existingPriceAsk = existingTicker.getPriceAsk();
                BigDecimal existingPriceBid = existingTicker.getPriceBid();
                if (existingPriceAsk.compareTo(newTicker.getPriceAsk()) != 0
                        || existingPriceBid.compareTo(newTicker.getPriceBid()) != 0) {
                    newTicker.setPreviousPriceAsk(existingPriceAsk);
                    newTicker.setPreviousPriceBid(existingPriceBid);
                    tickerSet.remove(existingTicker);
                    tickerSet.add(newTicker);
                }
            } else {
                tickerSet.add(newTicker);
            }
        });
    }
}
