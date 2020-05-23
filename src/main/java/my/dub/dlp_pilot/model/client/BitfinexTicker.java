package my.dub.dlp_pilot.model.client;

import my.dub.dlp_pilot.model.ExchangeName;

//NOTE: use platform/status endpoint to ping
public class BitfinexTicker extends Ticker {

    public BitfinexTicker() {
        setExchange(ExchangeName.BITFINEX.getFullName());
    }
}
