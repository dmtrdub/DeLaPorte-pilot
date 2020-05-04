package my.dub.dlp_pilot.repository;

import my.dub.dlp_pilot.model.Transfer;
import my.dub.dlp_pilot.model.TransferStatus;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.Set;

@Repository
public interface TransferRepository extends CrudRepository<Transfer, Long> {

    Set<Transfer> findByStatusEqualsAndEndTimeBefore(TransferStatus status, ZonedDateTime time);
}
