package my.dub.dlp_pilot.service;

import java.util.List;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.TestRun;
import my.dub.dlp_pilot.model.Trade;
import my.dub.dlp_pilot.model.dto.BarAverage;
import my.dub.dlp_pilot.model.dto.PriceDifference;
import my.dub.dlp_pilot.model.dto.Ticker;
import org.springframework.lang.NonNull;

/**
 * A service for managing price differences via related DTOs. {@link PriceDifference} object contains average close
 * price values for symbol pairs on 2 different exchanges.
 */
public interface PriceDifferenceService {

    /**
     * Create distinct {@link PriceDifference} objects based on {@link BarAverage} data. Save the created DTOs to a
     * cached storage.
     *
     * @param barAverages
     *         a non-null list of {@link BarAverage} DTOs for price difference creation
     */
    void createPriceDifferences(@NonNull List<BarAverage> barAverages);

    /**
     * Update average close prices of existing cached {@link PriceDifference} objects.
     *
     * @param barAverages
     *         a non-null list of {@link BarAverage} DTOs containing average close price data
     */
    void updatePriceDifferences(@NonNull List<BarAverage> barAverages);

    /**
     * Check all current {@link Ticker}s for similarity with {@link Ticker}s under a specific {@link ExchangeName}, and
     * compare the ticker pairs found against the average price differences. If the current ticker price difference
     * exceeds the average price difference - proceed to a new {@link Trade} creation check.
     *
     * @param exchangeName
     *         a non-null {@link ExchangeName} for ticker check
     * @param testRun
     *         a non-null current {@link TestRun} object
     */
    void handlePriceDifference(@NonNull ExchangeName exchangeName, @NonNull TestRun testRun);
}
