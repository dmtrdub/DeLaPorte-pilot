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

    Set<Ticker> checkAndGetTickers();

    Optional<Ticker> getTicker(ExchangeName exchangeName, String base, String target);

    Ticker findValidEquivalentTickerFromSet(Ticker originalTicker, Set<Ticker> tickerSet);

}
