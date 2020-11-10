package my.dub.dlp_pilot.service.client;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import my.dub.dlp_pilot.model.Bar;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.TimeFrame;
import my.dub.dlp_pilot.model.dto.LastBar;
import my.dub.dlp_pilot.model.dto.Ticker;
import org.springframework.lang.NonNull;

public interface ClientService {

    void loadAllSymbolPairs(@NonNull Collection<ExchangeName> exchangeNames);

    int getSymbolPairsCount(@NonNull ExchangeName exchangeName);

    void removeSymbolPair(@NonNull ExchangeName exchangeName, int index);

    Set<Ticker> fetchTickers(@NonNull ExchangeName exchangeName);

    List<Bar> fetchBars(@NonNull ExchangeName exchangeName, @NonNull TimeFrame timeFrame,
            @NonNull ZonedDateTime startTime, int symbolPairIndex, ZonedDateTime endTime);

    List<Bar> fetchBars(@NonNull ExchangeName exchangeName, @NonNull TimeFrame timeFrame, int symbolPairIndex,
            @NonNull Collection<LastBar> lastBars);

    void updateSymbolPairs();
}
