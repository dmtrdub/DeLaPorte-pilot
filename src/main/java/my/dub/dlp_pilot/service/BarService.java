package my.dub.dlp_pilot.service;

import java.util.Collection;
import java.util.List;
import my.dub.dlp_pilot.model.Bar;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.TestRun;
import my.dub.dlp_pilot.model.dto.BarAverage;
import my.dub.dlp_pilot.model.dto.LastBar;
import org.springframework.lang.NonNull;

public interface BarService {

    void save(@NonNull Collection<Bar> bars, TestRun testRun);

    boolean deleteAll(@NonNull TestRun testRun);

    List<BarAverage> loadAllBarAverages(@NonNull TestRun testRun);

    List<BarAverage> loadBarAverages(@NonNull TestRun testRun, @NonNull ExchangeName exchangeName);

    List<LastBar> loadExchangeLastBars(@NonNull TestRun testRun, @NonNull ExchangeName exchangeName);
}
