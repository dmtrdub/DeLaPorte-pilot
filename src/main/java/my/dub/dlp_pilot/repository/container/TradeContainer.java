package my.dub.dlp_pilot.repository.container;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import my.dub.dlp_pilot.model.DetrimentalRecord;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.Position;
import my.dub.dlp_pilot.model.Trade;
import my.dub.dlp_pilot.util.DateUtils;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

@Component
public class TradeContainer {
    // use map to store hash of trade in created state, in order to ensure remove only after successful persist to db
    private final Map<Integer, Trade> trades = new ConcurrentHashMap<>();
    private final Set<DetrimentalRecord> detrimentalRecords = Collections.newSetFromMap(new ConcurrentHashMap<>());

    // check if similar trade exists here, as well as before creating trade
    public void addTrade(Trade trade) {
        if (trade == null || isSimilarPresent(trade)) {
            return;
        }
        trades.put(trade.hashCode(), trade);
    }

    public Set<Trade> getTrades(ExchangeName exchangeName) {
        if (exchangeName == null) {
            return Collections.emptySet();
        }
        return trades.values().stream().filter(trade -> matchExchange(exchangeName, trade)).collect(Collectors.toSet());
    }

    public Pair<Long, Long> tradesCount(ExchangeName exchangeName1, ExchangeName exchangeName2) {
        if (exchangeName1 == null || exchangeName2 == null) {
            return Pair.of(0L, 0L);
        }
        long exchange1Count = 0;
        long exchange2Count = 0;
        for (Trade trade : trades.values()) {
            if (matchExchange(exchangeName1, trade)) {
                exchange1Count++;
            }
            if (matchExchange(exchangeName2, trade)) {
                exchange2Count++;
            }
        }
        return Pair.of(exchange1Count, exchange2Count);
    }

    // Similar trade = equal base, target, exchange1 and exchange2 (or vice-versa)
    public boolean isSimilarPresent(String base, String target, ExchangeName exchange1, ExchangeName exchange2) {
        return trades.values().stream().anyMatch(trade -> {
            ExchangeName exchangeShort = trade.getPositionShort().getExchange().getName();
            ExchangeName exchangeLong = trade.getPositionLong().getExchange().getName();
            return trade.getBase().equals(base) && trade.getTarget().equals(target) && (exchangeShort.equals(exchange1)
                    && exchangeLong.equals(exchange2)) || (exchangeShort.equals(exchange2) && exchangeLong
                    .equals(exchange1));
        });
    }

    // Similar trade = equal base, target, exchangeShort and exchangeLong
    public boolean isSimilarPresent(Trade trade) {
        return trades.values().stream().anyMatch(existingTrade -> {
            ExchangeName exchangeShort = existingTrade.getPositionShort().getExchange().getName();
            ExchangeName exchangeLong = existingTrade.getPositionLong().getExchange().getName();
            return existingTrade.getBase().equals(trade.getBase()) && existingTrade.getTarget()
                    .equals(trade.getTarget()) && exchangeShort.equals(trade.getPositionShort().getExchange().getName())
                    && exchangeLong.equals(trade.getPositionLong().getExchange().getName());
        });
    }

    public boolean remove(Integer hash) {
        return trades.remove(hash) != null;
    }

    public boolean isEmpty() {
        return trades.isEmpty();
    }

    public void addDetrimentalRecord(ExchangeName exchangeShort, ExchangeName exchangeLong, String base, String target,
            ZonedDateTime dateTimeClosed) {
        if (checkHasDetrimentalRecord(exchangeShort, exchangeLong, base, target)) {
            return;
        }
        DetrimentalRecord newDetrimentalRecord =
                new DetrimentalRecord(exchangeShort, exchangeLong, base, target, dateTimeClosed);
        detrimentalRecords.add(newDetrimentalRecord);
    }

    public boolean checkHasDetrimentalRecord(ExchangeName exchangeShort, ExchangeName exchangeLong, String base,
            String target) {
        DetrimentalRecord existingRecord = detrimentalRecords.stream()
                .filter(detRecord -> detRecord.getExchangeShort().equals(exchangeShort) && detRecord.getExchangeLong()
                        .equals(exchangeLong) && (detRecord.getBase().equals(base) || detRecord.getTarget()
                        .equals(target))).findFirst().orElse(null);
        if (existingRecord == null) {
            return false;
        }
        if (DateUtils.currentDateTimeUTC().isAfter(existingRecord.getInvalidationDateTime())) {
            detrimentalRecords.remove(existingRecord);
            return false;
        }
        return true;
    }

    private boolean matchExchange(ExchangeName exchangeName, Trade trade) {
        Position positionLong = trade.getPositionLong();
        Position positionShort = trade.getPositionShort();

        return (positionLong != null && exchangeName.equals(positionLong.getExchange().getName())) || (
                positionShort != null && exchangeName.equals(positionShort.getExchange().getName()));
    }
}
