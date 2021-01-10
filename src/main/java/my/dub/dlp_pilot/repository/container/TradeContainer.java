package my.dub.dlp_pilot.repository.container;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import my.dub.dlp_pilot.Constants;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.Position;
import my.dub.dlp_pilot.model.Trade;
import my.dub.dlp_pilot.model.dto.DetrimentalRecord;
import org.springframework.data.util.Pair;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * An in-memory container for not persisted {@link Trade} objects, and for related {@link DetrimentalRecord} objects.
 * The uniqueness of each added {@link Trade} object is checked using {@link #isSimilarPresent(Trade)} and {@link
 * #isSimilarPresent(String, String, ExchangeName, ExchangeName)} methods.
 */
@Component
public class TradeContainer {

    /**
     * Use map to store local id of trade in created state, in order to ensure remove only after successful persist to
     * DB
     */
    private final Map<Long, Trade> trades = new ConcurrentHashMap<>();
    private final Set<DetrimentalRecord> detrimentalRecords = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private static final AtomicLong LOCAL_ID_SEQUENCE = new AtomicLong();

    /**
     * A <b>synchronized</b> method for adding a new {@link Trade} object. Checks if similar Trade object already
     * exists. Generates and sets a new {@link Trade#getLocalId()} value.
     *
     * @param trade
     *         a non-null filled Trade object
     *
     * @return <code>true</code> if the new object was successfully added, <code>false</code> otherwise
     */
    public synchronized boolean addTrade(@NonNull Trade trade) {
        checkNotNull(trade, Constants.NULL_ARGUMENT_MESSAGE, "trade");

        if (isSimilarPresent(trade)) {
            return false;
        }
        long localId = LOCAL_ID_SEQUENCE.getAndIncrement();
        trade.setLocalId(localId);
        return trades.putIfAbsent(localId, trade) == null;
    }

    /**
     * Get all Trade records for a specific {@link ExchangeName} (for either Short or Long positions).
     *
     * @param exchange
     *         a non-null exchange name
     *
     * @return a non-null set of {@link Trade} objects with one of their related {@link Position} having the specified
     * exchange name
     */
    public Set<Trade> getTrades(@NonNull ExchangeName exchange) {
        checkNotNull(exchange, Constants.NULL_ARGUMENT_MESSAGE, "exchange");

        return trades.values().stream().filter(trade -> matchExchange(exchange, trade)).collect(Collectors.toSet());
    }

    /**
     * Count the number of {@link Trade} objects that match the condition in {@link #matchExchange(ExchangeName, Trade)}
     * method.
     *
     * @param exchange1
     *         first non-null condition input
     * @param exchange2
     *         second non-null condition input
     *
     * @return a {@link Pair} containing the number of resulting Trade objects
     */
    public Pair<Long, Long> tradesCount(@NonNull ExchangeName exchange1, @NonNull ExchangeName exchange2) {
        checkNotNull(exchange1, Constants.NULL_ARGUMENT_MESSAGE, "exchange1");
        checkNotNull(exchange2, Constants.NULL_ARGUMENT_MESSAGE, "exchange2");

        long exchange1Count = 0;
        long exchange2Count = 0;
        for (Trade trade : trades.values()) {
            if (matchExchange(exchange1, trade)) {
                exchange1Count++;
            }
            if (matchExchange(exchange2, trade)) {
                exchange2Count++;
            }
        }
        return Pair.of(exchange1Count, exchange2Count);
    }

    /**
     * Check if similar {@link Trade} object is present in the container. The similarity is defined by the equality of
     * {@link Trade#getBase()}, {@link Trade#getTarget()}, short {@link Position#getExchange()} name, and long {@link
     * Position#getExchange()} name (or vice-versa) to the input parameters.
     *
     * @param base
     *         a non-null String representing {@link Trade#getBase()}
     * @param target
     *         a non-null String representing {@link Trade#getTarget()}
     * @param exchange1
     *         a non-null {@link ExchangeName} of one of Trade's position
     * @param exchange2
     *         a non-null {@link ExchangeName} of one of Trade's position
     *
     * @return <code>true</code> if any of the present Trade records match the criteria of similarity,
     * <code>false</code> otherwise
     */
    public boolean isSimilarPresent(@NonNull String base, @NonNull String target, @NonNull ExchangeName exchange1,
            @NonNull ExchangeName exchange2) {
        checkNotNull(base, Constants.NULL_ARGUMENT_MESSAGE, "base");
        checkNotNull(target, Constants.NULL_ARGUMENT_MESSAGE, "target");
        checkNotNull(exchange1, Constants.NULL_ARGUMENT_MESSAGE, "exchange1");
        checkNotNull(exchange2, Constants.NULL_ARGUMENT_MESSAGE, "exchange2");

        return trades.values().stream().anyMatch(trade -> {
            ExchangeName exchangeShort = trade.getPositionShort().getExchange().getName();
            ExchangeName exchangeLong = trade.getPositionLong().getExchange().getName();
            return trade.getBase().equals(base) && trade.getTarget().equals(target) && (exchangeShort.equals(exchange1)
                    && exchangeLong.equals(exchange2)) || (exchangeShort.equals(exchange2) && exchangeLong
                    .equals(exchange1));
        });
    }

    /**
     * Check if similar {@link Trade} object is present in the container. The similarity is defined by the equality of
     * {@link Trade#getBase()}, {@link Trade#getTarget()}, short {@link Position#getExchange()} name, and long {@link
     * Position#getExchange()} name (or vice-versa) to the input {@link} Trade object.
     *
     * @param trade
     *         a non-null Trade object to check for similarity
     *
     * @return <code>true</code> if any of the present Trade records match the criteria of similarity,
     * <code>false</code> otherwise
     */
    public boolean isSimilarPresent(@NonNull Trade trade) {
        checkNotNull(trade, Constants.NULL_ARGUMENT_MESSAGE, "trade");

        return trades.values().stream().anyMatch(existingTrade -> {
            ExchangeName exchangeShort = existingTrade.getPositionShort().getExchange().getName();
            ExchangeName exchangeLong = existingTrade.getPositionLong().getExchange().getName();
            return existingTrade.getBase().equals(trade.getBase()) && existingTrade.getTarget()
                    .equals(trade.getTarget()) && exchangeShort.equals(trade.getPositionShort().getExchange().getName())
                    && exchangeLong.equals(trade.getPositionLong().getExchange().getName());
        });
    }

    /**
     * Remove a {@link Trade} object from the container by its {@link Trade#getLocalId()} value.
     *
     * @param localId
     *         a local ID of Trade
     *
     * @return <code>true</code> if a record was removed, <code>false</code> otherwise
     */
    public boolean remove(@NonNull Long localId) {
        return trades.remove(checkNotNull(localId, Constants.NULL_ARGUMENT_MESSAGE, "localId")) != null;
    }

    /**
     * Checks if {@link Trade} container is empty.
     *
     * @return <code>true</code> if no Trade records exist, <code>false</code> otherwise
     */
    public boolean isEmpty() {
        return trades.isEmpty();
    }

    /**
     * Create and add a new {@link DetrimentalRecord} object to container, if no object with equal fields exists in the
     * container.
     *
     * @param exchangeShort
     *         a non-null {@link ExchangeName} for short side
     * @param exchangeLong
     *         a non-null {@link ExchangeName} for long side
     * @param base
     *         a non-null base symbol
     * @param target
     *         a non-null target symbol
     * @param invalidationDate
     *         a non-null {@link Instant} representing an invalidation date
     */
    public void addDetrimentalRecord(@NonNull ExchangeName exchangeShort, @NonNull ExchangeName exchangeLong,
            @NonNull String base, @NonNull String target, @NonNull Instant invalidationDate) {
        checkNotNull(base, Constants.NULL_ARGUMENT_MESSAGE, "base");
        checkNotNull(target, Constants.NULL_ARGUMENT_MESSAGE, "target");
        checkNotNull(exchangeShort, Constants.NULL_ARGUMENT_MESSAGE, "exchangeShort");
        checkNotNull(exchangeLong, Constants.NULL_ARGUMENT_MESSAGE, "exchangeLong");
        checkNotNull(invalidationDate, Constants.NULL_ARGUMENT_MESSAGE, "invalidationDate");

        if (checkDetrimentalRecord(exchangeShort, exchangeLong, base, target)) {
            return;
        }
        DetrimentalRecord newDetrimentalRecord =
                new DetrimentalRecord(exchangeShort, exchangeLong, base, target, invalidationDate);
        detrimentalRecords.add(newDetrimentalRecord);
    }

    /**
     * Find a {@link DetrimentalRecord} object by the input parameters. If the invalidation date has passed - remove the
     * object found from the container.
     *
     * @param exchangeShort
     *         a non-null {@link ExchangeName} for short side
     * @param exchangeLong
     *         a non-null {@link ExchangeName} for long side
     * @param base
     *         a non-null base symbol
     * @param target
     *         a non-null target symbol
     *
     * @return <code>true</code> if a Detrimental record exists in the container, and is not yet invalidated, and
     * <code>false</code> otherwise
     */
    public boolean checkDetrimentalRecord(@NonNull ExchangeName exchangeShort, @NonNull ExchangeName exchangeLong,
            @NonNull String base, @NonNull String target) {
        checkNotNull(base, Constants.NULL_ARGUMENT_MESSAGE, "base");
        checkNotNull(target, Constants.NULL_ARGUMENT_MESSAGE, "target");
        checkNotNull(exchangeShort, Constants.NULL_ARGUMENT_MESSAGE, "exchangeShort");
        checkNotNull(exchangeLong, Constants.NULL_ARGUMENT_MESSAGE, "exchangeLong");

        DetrimentalRecord existingRecord = detrimentalRecords.stream()
                .filter(detRecord -> detRecord.getExchangeShort().equals(exchangeShort) && detRecord.getExchangeLong()
                        .equals(exchangeLong) && (detRecord.getBase().equals(base) || detRecord.getTarget()
                        .equals(target))).findFirst().orElse(null);
        if (existingRecord == null) {
            return false;
        }
        if (Instant.now().isAfter(existingRecord.getInvalidationDateTime())) {
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
