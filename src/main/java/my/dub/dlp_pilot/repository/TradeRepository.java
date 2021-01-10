package my.dub.dlp_pilot.repository;

import java.math.BigDecimal;
import java.util.List;
import my.dub.dlp_pilot.model.Exchange;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.Position;
import my.dub.dlp_pilot.model.TestRun;
import my.dub.dlp_pilot.model.Trade;
import my.dub.dlp_pilot.model.TradeResultType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

/**
 * A JPA Repository for CRUD operations with {@link Trade} entity.
 */
@Repository
public interface TradeRepository extends CrudRepository<Trade, Long> {

    /**
     * Get all distinct {@link Trade} records with {@link Trade#getWrittenToFile()} = false  that are related to a
     * {@link TestRun} with a specific ID, sorted by {@link Trade#getEndTime()}.
     *
     * @param testRunId
     *         a non-null ID of TestRun to filter on
     *
     * @return a list of filtered {@link Trade} entities
     */
    List<Trade> findDistinctByWrittenToFileFalseAndTestRunIdEqualsOrderByEndTimeAsc(@NonNull Long testRunId);

    // TODO: rework to ensure unique check (Instant not supported)

    /**
     * Check if a record with values passed as parameters exists.
     *
     * @param base
     *         a non-null String to filter on {@link Trade#getBase()}
     * @param target
     *         a non-null String to filter on {@link Trade#getTarget()}
     * @param exchangeShort
     *         a non-null ExchangeName to filter on related short {@link Position}'s {@link Exchange#getName()}
     * @param exchangeLong
     *         a non-null ExchangeName to filter on related long {@link Position}'s {@link Exchange#getName()}
     * @param openPriceShort
     *         a non-null BigDecimal to filter on related short {@link Position#getOpenPrice()}
     * @param openPriceLong
     *         a non-null BigDecimal to filter on related long {@link Position#getOpenPrice()}
     * @param resultType
     *         a non-null TradeResultType to filter on {@link Trade#getResultType()}
     *
     * @return <code>true</code> if any match was found, <code>false</code> otherwise
     */
    @Query("select case when count(t)> 0 then true else false end from Trade t where t.base=:base"
                   + " and t.target=:target and t.positionShort.exchange.name=:exchangeShort"
                   + " and t.positionShort.openPrice=:openPriceShort and t.positionLong.exchange.name=:exchangeLong"
                   + " and t.positionLong.openPrice=:openPriceLong and t.resultType=:resultType")
    boolean checkSimilarExists(@NonNull String base, @NonNull String target, @NonNull ExchangeName exchangeShort,
            @NonNull ExchangeName exchangeLong, @NonNull BigDecimal openPriceShort, @NonNull BigDecimal openPriceLong,
            @NonNull TradeResultType resultType);

}
