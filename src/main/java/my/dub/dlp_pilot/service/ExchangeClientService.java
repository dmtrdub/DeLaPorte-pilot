package my.dub.dlp_pilot.service;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import my.dub.dlp_pilot.model.Bar;
import my.dub.dlp_pilot.model.TimeFrame;
import my.dub.dlp_pilot.model.dto.SymbolPair;
import my.dub.dlp_pilot.model.dto.Ticker;
import org.springframework.lang.NonNull;

public interface ExchangeClientService {

    List<SymbolPair> fetchSymbolPairs() throws IOException;

    Set<Ticker> fetchAllTickers(@NonNull List<SymbolPair> symbolPairs) throws IOException;

    List<Bar> fetchBars(@NonNull SymbolPair symbolPair, @NonNull TimeFrame timeFrame, @NonNull Instant startTime,
            @NonNull Instant endTime) throws IOException;

    List<Bar> fetchBars(@NonNull SymbolPair symbolPair, @NonNull TimeFrame timeFrame, long barsLimit)
            throws IOException;
}
