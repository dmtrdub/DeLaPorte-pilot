package my.dub.dlp_pilot.repository;

import my.dub.dlp_pilot.model.TestRun;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * A JPA Repository for CRUD operations with {@link TestRun} entity.
 */
@Repository
public interface TestRunRepository extends CrudRepository<TestRun, Long> {
}
