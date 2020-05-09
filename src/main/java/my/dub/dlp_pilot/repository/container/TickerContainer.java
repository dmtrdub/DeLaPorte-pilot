package my.dub.dlp_pilot.repository.container;

import lombok.extern.slf4j.Slf4j;
import my.dub.dlp_pilot.model.Position;
import my.dub.dlp_pilot.model.PositionSide;
import my.dub.dlp_pilot.model.Ticker;
import my.dub.dlp_pilot.model.Trade;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TickerContainer {
    private Map<Long, Set<Ticker>> tickersMap = new ConcurrentHashMap<>();

    public Set<Ticker> getAll() {
        return tickersMap.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
    }

    public Map<Long, Set<Ticker>> getExchangeIDTickersMap() {
        return new ConcurrentHashMap<>(tickersMap);
    }

    public Set<Ticker> getTickers(Long exchangeId) {
        if (exchangeId == null) {
            return Collections.emptySet();
        }
        return tickersMap.getOrDefault(exchangeId, Collections.emptySet());
    }

    public Optional<Ticker> getTicker(Long exchangeId, String base, String target) {
        Set<Ticker> tickers = tickersMap.get(exchangeId);
        if (CollectionUtils.isEmpty(tickers)) {
            return Optional.empty();
        }
        return tickers.stream().filter(ticker -> ticker.getBase().equals(base) && ticker.getTarget().equals(target))
                .findFirst();
    }

    public Optional<Ticker> getTicker(Trade trade, PositionSide side) {
        if (trade == null || side == null) {
            return Optional.empty();
        }
        Position position = PositionSide.SHORT.equals(side) ? trade.getPositionShort() : trade.getPositionLong();
        return getTicker(position);
    }

    public Optional<Ticker> getTicker(Position position) {
        if (position == null) {
            return Optional.empty();
        }
        Set<Ticker> tickers = tickersMap.get(position.getExchange().getId());
        if (CollectionUtils.isEmpty(tickers)) {
            return Optional.empty();
        }
        return tickers.stream().filter(ticker -> ticker.getBase().equals(position.getBase()) &&
                ticker.getTarget().equals(position.getTarget())).findFirst();
    }

    public void addTicker(Long exchangeId, Ticker ticker) {
        if (exchangeId == null || ticker == null) {
            return;
        }
        Set<Ticker> tickerList = tickersMap.get(exchangeId);
        if (tickerList == null) {
            Set<Ticker> newTickerSet = new HashSet<>();
            newTickerSet.add(ticker);
            tickersMap.put(exchangeId, newTickerSet);
        } else {
            tickerList.add(ticker);
        }
    }

    public void addTickers(Long exchangeId, Collection<Ticker> tickers) {
        if (exchangeId == null || CollectionUtils.isEmpty(tickers)) {
            return;
        }
        Set<Ticker> tickerList = tickersMap.get(exchangeId);
        if (tickerList == null) {
            tickersMap.put(exchangeId, new HashSet<>(tickers));
        } else {
            tickerList.addAll(tickers);
        }
    }

    public void updateTickers(Long exchangeId, Collection<Ticker> tickers) {
        if (exchangeId == null || CollectionUtils.isEmpty(tickers)) {
            return;
        }
        Set<Ticker> tickerSet = tickersMap.get(exchangeId);
        if (tickerSet == null) {
            tickersMap.put(exchangeId, new HashSet<>(tickers));
        } else {
            tickers.forEach(ticker -> replaceAllIfAbsent(tickerSet, ticker));
        }
    }

    private void replaceAllIfAbsent(Set<Ticker> tickerList, Ticker ticker) {
        if (!tickerList.contains(ticker)) {
            List<Ticker> toRemove = tickerList.stream().filter(t -> t.getBase().equals(ticker.getBase()) &&
                    t.getTarget().equals(ticker.getTarget())).collect(Collectors.toList());
            tickerList.removeAll(toRemove);
            tickerList.add(ticker);
        }
    }
}
