package my.dub.dlp_pilot.service;

import java.util.List;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.TestRun;
import my.dub.dlp_pilot.model.dto.BarAverage;
import org.springframework.lang.NonNull;

public interface PriceDifferenceService {

    void createPriceDifferences(List<BarAverage> barAverages);

    void updatePriceDifferences(@NonNull List<BarAverage> barAverages);

    void handlePriceDifference(ExchangeName exchangeName, TestRun testRun);
}
