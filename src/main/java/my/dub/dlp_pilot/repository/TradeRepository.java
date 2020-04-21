package my.dub.dlp_pilot.repository;

import my.dub.dlp_pilot.model.Trade;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TradeRepository extends CrudRepository<Trade, Long> {
}
