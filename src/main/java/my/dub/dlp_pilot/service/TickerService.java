package my.dub.dlp_pilot.service;

import my.dub.dlp_pilot.model.*;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface TickerService {

    Iterable<Ticker> save(Collection<Ticker> tickers);

    void saveAndUpdateLocal(Collection<Ticker> tickers, long exchangeId);

    void fetchMarketData(Exchange exchange);

    boolean isPairEquivalent(String baseOriginal, String targetOriginal, String baseCompared, String targetCompared);

    Set<Ticker> getTickers(Long exchangeId);

    Optional<Ticker> getTicker(Long exchangeId, String base, String target);

    Optional<Ticker> getTicker(Trade trade, PositionSide side);

    Optional<Ticker> getTicker(Position position);

    Map<Long, Set<Ticker>> getExchangeIDTickersMap();
}
