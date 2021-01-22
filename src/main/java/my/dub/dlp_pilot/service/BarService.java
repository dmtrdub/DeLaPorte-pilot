package my.dub.dlp_pilot.service;

import java.util.Collection;
import java.util.List;
import my.dub.dlp_pilot.model.Bar;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.TestRun;
import my.dub.dlp_pilot.model.dto.BarAverage;
import my.dub.dlp_pilot.model.dto.LastBar;
import org.springframework.lang.NonNull;

/**
 * A service for managing {@link Bar} entities and related DTOs.
 */
public interface BarService {

    /**
     * Persist a collection of {@link Bar} entities, setting the passed {@link TestRun}.
     *
     * @param bars
     *         a non-null collection of Bars to persist
     * @param testRun
     *         a non-null TestRun entity to set
     */
    void save(@NonNull Collection<Bar> bars, TestRun testRun);

    /**
     * Delete all {@link Bar} entities, which have a specific {@link Bar#getTestRun()}.
     *
     * @param testRun
     *         a non-null TestRun entity to filter on
     *
     * @return <code>true</code> if any matching record was deleted, <code>false</code> otherwise
     */
    boolean deleteAll(@NonNull TestRun testRun);

    /**
     * Get all {@link BarAverage}s containing an average price <pre>(High+Low+Close)/3</pre> for a symbol pair on one
     * exchange.
     *
     * @param testRun
     *         a non-null TestRun entity to filter on
     *
     * @return a list of filtered BarAverages with a specific {@link Bar#getTestRun()}
     */
    List<BarAverage> loadAllBarAverages(@NonNull TestRun testRun);

    /**
     * Get all {@link BarAverage}s containing an average price <pre>(High+Low+Close)/3</pre> for a symbol pair on a
     * specific exchange.
     *
     * @param testRun
     *         a non-null TestRun entity to filter on
     * @param exchangeName
     *         a non-null exchange name to filter on
     *
     * @return a list of filtered BarAverages with a specific {@link Bar#getTestRun()} and {@link Bar#getExchangeName()}
     */
    List<BarAverage> loadBarAverages(@NonNull TestRun testRun, @NonNull ExchangeName exchangeName);

    /**
     * Get all {@link LastBar}s containing the close time of the last {@link Bar} records for a symbol pair on a
     * specific exchange.
     *
     * @param testRun
     *         a non-null TestRun entity to filter on
     * @param exchangeName
     *         a non-null exchange name to filter on
     *
     * @return a list of filtered LastBars with a specific {@link Bar#getTestRun()} and {@link Bar#getExchangeName()}
     */
    List<LastBar> loadLastBars(@NonNull TestRun testRun, @NonNull ExchangeName exchangeName);
}
