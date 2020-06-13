package my.dub.dlp_pilot.service;

import my.dub.dlp_pilot.model.Exchange;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.client.Ticker;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface TickerService {

    void fetchAndSave(Exchange exchange);

    void save(ExchangeName exchangeName, Collection<Ticker> tickers);

    boolean checkStale(Ticker ticker);

    Set<Ticker> getTickers(ExchangeName exchangeName);

    Set<Ticker> getAllTickers();

    Optional<Ticker> getTicker(ExchangeName exchangeName, String base, String target);

    Ticker findEquivalentTickerFromSet(Ticker originalTicker, Set<Ticker> tickerSet);

}
