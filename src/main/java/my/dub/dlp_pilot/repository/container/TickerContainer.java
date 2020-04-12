package my.dub.dlp_pilot.repository.container;

import lombok.extern.slf4j.Slf4j;
import my.dub.dlp_pilot.model.Ticker;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TickerContainer {
    private Map<Long, List<Ticker>> tickersMap = new ConcurrentHashMap<>();

    public List<Ticker> getTickers(Long exchangeId) {
        if (exchangeId == null) {
            return Collections.emptyList();
        }
        return tickersMap.getOrDefault(exchangeId, Collections.emptyList());
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
