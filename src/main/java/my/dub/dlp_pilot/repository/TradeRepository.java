package my.dub.dlp_pilot.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.Trade;
import my.dub.dlp_pilot.model.TradeResultType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TradeRepository extends CrudRepository<Trade, Long> {

    Set<Trade> findByWrittenToFileFalseAndTestRunIdEqualsOrderByEndTimeAsc(Long testRunId);

    @Query("select case when count(t)> 0 then true else false end from Trade t where t.base=:base"
                   + " and t.target=:target and t.startTime=:startTime and t.positionShort.exchange.name=:exchShort"
                   + " and t.positionShort.openPrice=:openPriceShort and t.positionLong.exchange.name=:exchLong"
                   + " and t.positionLong.openPrice=:openPriceLong and t.resultType=:resType")
    boolean checkSimilarExists(@Param("base") String base, @Param("target") String target,
            @Param("startTime") Instant startTime, @Param("exchShort") ExchangeName exchangeShort,
            @Param("exchLong") ExchangeName exchangeLong, @Param("openPriceShort") BigDecimal openPriceShort,
            @Param("openPriceLong") BigDecimal openPriceLong, @Param("resType") TradeResultType resultType);

}
