package my.dub.dlp_pilot.service;

import java.util.Collection;
import java.util.Set;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.PriceDifference;
import my.dub.dlp_pilot.model.Ticker;
import my.dub.dlp_pilot.model.Trade;

public interface TradeService {

    void checkTradeOpen(PriceDifference priceDifference, Ticker tickerShort, Ticker tickerLong);

    void handleTrades(ExchangeName exchangeName);

    Set<Trade> getCompletedTradesNotWrittenToFile();

    void updateTradesWrittenToFile(Collection<Trade> trades);

    boolean isAllTradesClosed();
}
