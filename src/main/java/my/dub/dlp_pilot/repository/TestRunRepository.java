package my.dub.dlp_pilot.repository;

import my.dub.dlp_pilot.model.TestRun;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TestRunRepository extends CrudRepository<TestRun, Long> {
}
