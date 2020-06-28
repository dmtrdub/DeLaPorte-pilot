package my.dub.dlp_pilot.repository.container;

import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.Position;
import my.dub.dlp_pilot.model.Trade;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class CurrentTradeContainer {
    private final Set<Trade> tradesInProgress = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public boolean addTrade(Trade trade) {
        if (trade == null) {
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

    public boolean isSimilarPresent(String base, String target, ExchangeName exchange1, ExchangeName exchange2) {
        return tradesInProgress.stream().anyMatch(trade -> {
            ExchangeName exchangeShort = trade.getPositionShort().getExchange().getName();
            ExchangeName exchangeLong = trade.getPositionLong().getExchange().getName();
            return trade.getBase().equals(base) && trade.getTarget().equals(target) &&
                    ((exchangeShort.equals(exchange1) && exchangeLong.equals(exchange2)) ||
                            (exchangeShort.equals(exchange2) && exchangeLong.equals(exchange1)));
        });
    }

    public void removeTrades(Collection<Trade> trades) {
        if (CollectionUtils.isEmpty(trades)) {
            return;
        }
        tradesInProgress.removeAll(trades);
    }

    public boolean isEmpty() {
        return tradesInProgress.isEmpty();
    }

}
