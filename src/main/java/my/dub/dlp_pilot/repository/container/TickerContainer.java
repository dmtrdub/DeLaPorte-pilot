package my.dub.dlp_pilot.repository.container;

import lombok.extern.slf4j.Slf4j;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.client.Ticker;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class TickerContainer {

    // Exclude possible concurrency exceptions by decentralizing storage
    private final Set<Ticker> bigONETickers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Ticker> binanceTickers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Ticker> bitBayTickers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Ticker> bitfinexTickers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Ticker> bithumbTickers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Ticker> bitmartTickers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Ticker> bitmaxTickers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Ticker> bittrexTickers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Ticker> bwTickers = Collections.newSetFromMap(new ConcurrentHashMap<>());

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
        }
        return Collections.emptySet();
    }

    public Set<Ticker> getAll(boolean includeStale) {
        Stream<Ticker> tickerStream =
                Stream.of(bigONETickers, binanceTickers, bitBayTickers, bitfinexTickers, bithumbTickers, bitmartTickers,
                          bitmaxTickers, bittrexTickers, bwTickers).flatMap(Collection::stream);
        if (!includeStale) {
            tickerStream = tickerStream.filter(ticker -> !ticker.isStale());
        }
        return tickerStream.collect(Collectors.toSet());
    }

    public Set<Ticker> getTickers(ExchangeName exchangeName, boolean includeStale) {
        if (exchangeName == null) {
            return Collections.emptySet();
        }
        Set<Ticker> result = tickerSet(exchangeName);
        if (!includeStale) {
            return result.stream().filter(ticker -> !ticker.isStale()).collect(Collectors.toSet());
        }
        return result;
    }

    public Optional<Ticker> getTicker(ExchangeName exchangeName, String base, String target) {
        if (exchangeName == null || StringUtils.isEmpty(base) || StringUtils.isEmpty(target)) {
            return Optional.empty();
        }
        Set<Ticker> tickers = tickerSet(exchangeName);
        if (CollectionUtils.isEmpty(tickers)) {
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
                BigDecimal existingPrice = existingTicker.getPrice();
                if (existingPrice.compareTo(newTicker.getPrice()) != 0) {
                    newTicker.setPreviousPrice(existingPrice);
                    tickerSet.remove(existingTicker);
                    tickerSet.add(newTicker);
                }
            } else {
                tickerSet.add(newTicker);
            }
        });
    }

    public boolean isEmpty(ExchangeName exchangeName) {
        return CollectionUtils.isEmpty(tickerSet(exchangeName));
    }
}
