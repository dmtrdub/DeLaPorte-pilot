package my.dub.dlp_pilot.repository;

import my.dub.dlp_pilot.model.Trade;
import my.dub.dlp_pilot.model.TradeResultType;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface TradeRepository extends CrudRepository<Trade, Long> {

    Set<Trade> findByEndTimeIsNullAndResultTypeEquals(TradeResultType resultType);
}
