package my.dub.dlp_pilot.model.client;

import my.dub.dlp_pilot.model.ExchangeName;

public class BinanceTicker extends Ticker {

    public BinanceTicker() {
        setExchangeName(ExchangeName.BINANCE);
    }
}
