package my.dub.dlp_pilot.repository;

import java.util.List;
import my.dub.dlp_pilot.model.Bar;
import my.dub.dlp_pilot.model.BarAverage;
import my.dub.dlp_pilot.model.ExchangeName;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BarRepository extends CrudRepository<Bar, Long> {

    @Modifying
    Long deleteAllByTestRunIdEquals(Long testRunId);

    @Query("select new my.dub.dlp_pilot.model.BarAverage(b.exchangeName, b.base, b.target, avg((b.high + b.close + b"
                   + ".low)/3)) from Bar b where b.testRun.id=:testRunId group by b.exchangeName, b.base, b.target")
    List<BarAverage> getAllBarAverages(@Param("testRunId") Long testRunId);

    @Query("select new my.dub.dlp_pilot.model.BarAverage(b.exchangeName, b.base, b.target, avg((b.high + b.close + b"
                   + ".low)/3)) from Bar b where (b.exchangeName = :exchangeName and b.testRun.id=:testRunId) group "
                   + "by b.exchangeName, b.base, b.target")
    List<BarAverage> getBarAverages(@Param("exchangeName") ExchangeName exchangeName,
            @Param("testRunId") Long testRunId);
}
