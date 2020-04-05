package my.dub.dlp_pilot.repository;

import my.dub.dlp_pilot.model.Ticker;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TickerRepository extends CrudRepository<Ticker, Long> {

}
