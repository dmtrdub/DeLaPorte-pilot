package my.dub.dlp_pilot.repository.container;

import my.dub.dlp_pilot.model.DetrimentalRecord;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.Position;
import my.dub.dlp_pilot.model.Trade;
import my.dub.dlp_pilot.util.DateUtils;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class TradeContainer {
    private final Set<Trade> tradesInProgress = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<DetrimentalRecord> detrimentalRecords = Collections.newSetFromMap(new ConcurrentHashMap<>());

    // check if similar trade exists here, as well as before creating trade
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

    public Pair<Long, Long> tradesCount(ExchangeName exchangeName1, ExchangeName exchangeName2) {
        if (exchangeName1 == null || exchangeName2 == null) {
            return Pair.of(0L, 0L);
        }
        long exchange1Count = 0;
        long exchange2Count = 0;
        for (Trade trade : tradesInProgress) {
            if (matchExchange(exchangeName1, trade)) {
                exchange1Count++;
            }
            if (matchExchange(exchangeName2, trade)) {
                exchange2Count++;
            }
        }
        return Pair.of(exchange1Count, exchange2Count);
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
                    (exchangeShort.equals(exchange1) || exchangeLong.equals(exchange2) ||
                            exchangeShort.equals(exchange2) || exchangeLong.equals(exchange1));
        });
    }

    public boolean isSimilarPresent(Trade trade) {
        return isSimilarPresent(trade.getBase(), trade.getTarget(), trade.getPositionShort().getExchange().getName(),
                                trade.getPositionLong().getExchange().getName());
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

    public void addDetrimentalRecord(ExchangeName exchangeShort, ExchangeName exchangeLong,
                                     ZonedDateTime dateTimeClosed) {
        if (hasDetrimentalRecord(exchangeShort, exchangeLong)) {
            return;
        }
        DetrimentalRecord newDetrimentalRecord = new DetrimentalRecord(exchangeShort, exchangeLong, dateTimeClosed);
        detrimentalRecords.add(newDetrimentalRecord);
    }

    public boolean hasDetrimentalRecord(ExchangeName exchangeShort, ExchangeName exchangeLong) {
        DetrimentalRecord existingRecord = detrimentalRecords.stream().filter(detRecord -> detRecord.getExchangeShort()
                                                                                                    .equals(exchangeShort) &&
                detRecord.getExchangeLong().equals(exchangeLong)).findFirst().orElse(null);
        if (existingRecord == null) {
            return false;
        }
        if (DateUtils.currentDateTime().isAfter(existingRecord.getInvalidationDateTime())) {
            detrimentalRecords.remove(existingRecord);
            return false;
        }
        return true;
    }

}
