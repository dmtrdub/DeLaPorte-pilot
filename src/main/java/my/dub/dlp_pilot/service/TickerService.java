package my.dub.dlp_pilot.service;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import my.dub.dlp_pilot.model.Exchange;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.Ticker;

public interface TickerService {

    void fetchAndSave(Exchange exchange);

    void save(ExchangeName exchangeName, Collection<Ticker> tickers);

    Set<Ticker> getTickers(ExchangeName exchangeName);

    Set<Ticker> getAllTickers();

    Optional<Ticker> getTicker(ExchangeName exchangeName, String base, String target);

    Optional<Ticker> findValidEquivalentTickerFromSet(Ticker originalTicker, Set<Ticker> tickerSet);

    boolean checkStale(Ticker ticker1, Ticker ticker2);
}
