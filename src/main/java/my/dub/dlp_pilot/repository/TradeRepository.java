package my.dub.dlp_pilot.repository;

import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.Trade;
import my.dub.dlp_pilot.model.TradeResultType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface TradeRepository extends CrudRepository<Trade, Long> {

    @Query("select t from Trade t where t.resultType=:resultType and (t.positionLong.exchange.name=:exchangeName or t.positionShort.exchange.name=:exchangeName)")
    Set<Trade> findTradesForExchange(@Param("resultType") TradeResultType resultType,
                                     @Param("exchangeName") ExchangeName exchangeName);
}
