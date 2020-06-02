package my.dub.dlp_pilot.repository.container;

import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.Position;
import my.dub.dlp_pilot.model.Trade;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class CurrentTradeContainer {
    private final Set<Trade> tradesInProgress = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public boolean addTrade(Trade trade) {
        if (trade == null || isSimilarPresent(trade)) {
            return false;
        }
        return tradesInProgress.add(trade);
    }

    public Set<Trade> getTrades(ExchangeName exchangeName) {
        if (exchangeName == null) {
            return Collections.emptySet();
        }
        return tradesInProgress.stream().filter(trade -> matchExchange(exchangeName, trade))
                .collect(Collectors.toSet());
    }

    private boolean matchExchange(ExchangeName exchangeName, Trade trade) {
        Position positionLong = trade.getPositionLong();
        Position positionShort = trade.getPositionShort();

        return (positionLong != null && exchangeName.equals(positionLong.getExchange().getName())) ||
                (positionShort != null && exchangeName.equals(positionShort.getExchange().getName()));
    }

    public boolean isSimilarPresent(Trade otherTrade) {
        if (otherTrade == null) {
            return false;
        }
        return tradesInProgress.stream().anyMatch(
                trade -> otherTrade.getPositionShort().isSimilar(trade.getPositionShort()) ||
                        otherTrade.getPositionLong().isSimilar(trade.getPositionLong()));
    }

    public void removeTrade(Trade trade) {
        if (trade == null) {
            return;
        }
        tradesInProgress.remove(trade);
    }

}
