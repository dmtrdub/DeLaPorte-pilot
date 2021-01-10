package my.dub.dlp_pilot.repository;

import java.util.List;
import my.dub.dlp_pilot.model.Bar;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.TestRun;
import my.dub.dlp_pilot.model.dto.BarAverage;
import my.dub.dlp_pilot.model.dto.LastBar;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

/**
 * A JPA Repository for CRUD operations with {@link Bar} entity and related DTOs
 */
@Repository
public interface BarRepository extends CrudRepository<Bar, Long> {

    /**
     * Delete all {@link Bar} records that are related to a {@link TestRun} with a specific ID.
     *
     * @param testRunId
     *         a non-null ID of TestRun to filter on
     *
     * @return a long representing the number of records deleted
     */
    Long deleteAllByTestRunIdEquals(@NonNull Long testRunId);

    /**
     * Get all {@link BarAverage} DTOs based on {@link Bar} records related to a {@link TestRun} with a specific ID.
     *
     * @param testRunId
     *         a non-null ID of TestRun to filter on
     *
     * @return a list of filtered {@link BarAverage} DTOs
     */
    @Query("select new my.dub.dlp_pilot.model.dto.BarAverage(b.exchangeName, b.base, b.target, max(b.closeTime), avg((b"
                   + ".high + b.close + b.low)/3)) from Bar b where b.testRun.id=:testRunId group by b.exchangeName, "
                   + "b.base, b.target")
    List<BarAverage> getAllBarAverages(@NonNull Long testRunId);

    /**
     * Get all {@link BarAverage} DTOs based on {@link Bar} records with a specific {@link Bar#getExchangeName()} and
     * related to a {@link TestRun} with a specific ID.
     *
     * @param exchangeName
     *         a non-null exchange name to filter on
     * @param testRunId
     *         a non-null ID of TestRun to filter on
     *
     * @return a list of filtered {@link BarAverage} DTOs
     */
    @Query("select new my.dub.dlp_pilot.model.dto.BarAverage(b.exchangeName, b.base, b.target, max(b.closeTime), avg((b"
                   + ".high + b.close + b.low)/3)) from Bar b where (b.exchangeName = :exchangeName and b.testRun"
                   + ".id=:testRunId) group by b.exchangeName, b.base, b.target")
    List<BarAverage> getBarAverages(@NonNull ExchangeName exchangeName, @NonNull Long testRunId);

    /**
     * Get all {@link LastBar} DTOs based on {@link Bar} records with a specific {@link Bar#getExchangeName()} and
     * related to a {@link TestRun} with a specific ID.
     *
     * @param exchangeName
     *         a non-null exchange name to filter on
     * @param testRunId
     *         a non-null ID of TestRun to filter on
     *
     * @return a list of filtered {@link LastBar} DTOs
     */
    @Query("select new my.dub.dlp_pilot.model.dto.LastBar(b.exchangeName, b.base, b.target, max(b.closeTime)) from "
                   + "Bar b where b.testRun.id = :testRunId and b.exchangeName = :exchangeName group by b"
                   + ".exchangeName, b.base, b.target")
    List<LastBar> getLastBars(@NonNull ExchangeName exchangeName, @NonNull Long testRunId);
}
