package my.dub.dlp_pilot.repository;

import my.dub.dlp_pilot.model.Exchange;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * A JPA Repository for CRUD operations with {@link Exchange} entity.
 */
@Repository
public interface ExchangeRepository extends CrudRepository<Exchange, Long> {
}
