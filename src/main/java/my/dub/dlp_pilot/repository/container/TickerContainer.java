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
    private Map<Long, List<Ticker>> tickersMap = new ConcurrentHashMap<>();

    public List<Ticker> getAll() {
        return tickersMap.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    public Map<Long, List<Ticker>> getExchangeIDTickersMap() {
        return new ConcurrentHashMap<>(tickersMap);
    }

    public List<Ticker> getTickers(Long exchangeId) {
        if (exchangeId == null) {
            return Collections.emptyList();
        }
        return tickersMap.getOrDefault(exchangeId, Collections.emptyList());
    }

    public Optional<Ticker> getTicker(Long exchangeId, String base, String target) {
        List<Ticker> tickers = tickersMap.get(exchangeId);
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
        List<Ticker> tickers = tickersMap.get(position.getExchange().getId());
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
        List<Ticker> tickerList = tickersMap.get(exchangeId);
        if (tickerList == null) {
            List<Ticker> newTickerList = new ArrayList<>();
            newTickerList.add(ticker);
            tickersMap.put(exchangeId, newTickerList);
        } else {
            tickerList.add(ticker);
        }
    }

    public void addTickers(Long exchangeId, Collection<Ticker> tickers) {
        if (exchangeId == null || CollectionUtils.isEmpty(tickers)) {
            return;
        }
        List<Ticker> tickerList = tickersMap.get(exchangeId);
        if (tickerList == null) {
            tickersMap.put(exchangeId, new ArrayList<>(tickers));
        } else {
            tickerList.addAll(tickers);
        }
    }

    public void updateTickers(Long exchangeId, Collection<Ticker> tickers) {
        if (exchangeId == null || CollectionUtils.isEmpty(tickers)) {
            return;
        }
        List<Ticker> tickerList = tickersMap.get(exchangeId);
        if (tickerList == null) {
            tickersMap.put(exchangeId, new ArrayList<>(tickers));
        } else {
            tickers.forEach(ticker -> replaceAllIfAbsent(tickerList, ticker));
        }
    }

    private void replaceAllIfAbsent(List<Ticker> tickerList, Ticker ticker) {
        if (!tickerList.contains(ticker)) {
            List<Ticker> toRemove = tickerList.stream().filter(t -> t.getBase().equals(ticker.getBase()) &&
                    t.getTarget().equals(ticker.getTarget())).collect(Collectors.toList());
            tickerList.removeAll(toRemove);
            tickerList.add(ticker);
        }
    }
}
