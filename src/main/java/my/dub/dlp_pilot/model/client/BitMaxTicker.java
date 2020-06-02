package my.dub.dlp_pilot.model.client;

import my.dub.dlp_pilot.model.ExchangeName;

//NOTE: no ping endpoint
public class BitMaxTicker extends Ticker {

    public BitMaxTicker() {
        setExchangeName(ExchangeName.BITMAX);
    }
}
