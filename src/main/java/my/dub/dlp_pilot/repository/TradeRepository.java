package my.dub.dlp_pilot.repository;

import my.dub.dlp_pilot.model.Trade;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface TradeRepository extends CrudRepository<Trade, Long> {

    Set<Trade> findDistinctByWrittenToFileFalseAndTestRunIdEquals(Long testRunId);

    @Modifying
    @Query("update Trade t set t.writtenToFile=true where t.id in :tradeIds")
    void updateTradesSetWrittenToFileTrue(@Param("tradeIds") Set<Long> tradeIds);
}
