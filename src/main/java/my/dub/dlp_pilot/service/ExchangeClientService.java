package my.dub.dlp_pilot.service;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import my.dub.dlp_pilot.model.Bar;
import my.dub.dlp_pilot.model.TimeFrame;
import my.dub.dlp_pilot.model.dto.SymbolPair;
import my.dub.dlp_pilot.model.dto.Ticker;

public interface ExchangeClientService {

    List<SymbolPair> fetchSymbolPairs() throws IOException;

    Set<Ticker> fetchAllTickers(List<SymbolPair> symbolPairs) throws IOException;

    List<Bar> fetchBars(SymbolPair symbolPair, TimeFrame timeFrame, ZonedDateTime startTime, ZonedDateTime endTime)
            throws IOException;

    List<Bar> fetchBars(SymbolPair symbolPair, TimeFrame timeFrame, long barsLimit) throws IOException;
}
