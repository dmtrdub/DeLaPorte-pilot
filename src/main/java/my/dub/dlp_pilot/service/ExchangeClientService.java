package my.dub.dlp_pilot.service;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import my.dub.dlp_pilot.model.Bar;
import my.dub.dlp_pilot.model.SymbolPair;
import my.dub.dlp_pilot.model.Ticker;
import my.dub.dlp_pilot.model.TimeFrame;

public interface ExchangeClientService {

    List<SymbolPair> fetchSymbolPairs() throws IOException;

    Set<Ticker> fetchAllTickers(List<SymbolPair> symbolPairs) throws IOException;

    List<Bar> fetchBars(SymbolPair symbolPair, TimeFrame timeFrame, ZonedDateTime startTime, ZonedDateTime endTime)
            throws IOException;

    Bar fetchBar(SymbolPair symbolPair, TimeFrame timeFrame) throws IOException;
}
