package my.dub.dlp_pilot.service;

import my.dub.dlp_pilot.model.Trade;

import java.util.Collection;
import java.util.Set;

public interface TradeService {
    void save(Collection<Trade> trades);

    Set<Trade> findTradesInProgress();

    void trade();

    void handleTransfers();

    void handleTrades();
}
