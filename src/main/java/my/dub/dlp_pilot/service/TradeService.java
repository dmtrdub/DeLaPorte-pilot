package my.dub.dlp_pilot.service;

import my.dub.dlp_pilot.model.Exchange;
import my.dub.dlp_pilot.model.ExchangeName;

public interface TradeService {

    void searchForTrades(Exchange exchange);

    void handleTrades(ExchangeName exchangeName);
}
