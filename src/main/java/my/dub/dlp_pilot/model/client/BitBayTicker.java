package my.dub.dlp_pilot.model.client;

import my.dub.dlp_pilot.model.ExchangeName;

//NOTE: no ping endpoint
public class BitBayTicker extends Ticker {

    public BitBayTicker() {
        setExchange(ExchangeName.BITBAY.getFullName());
    }
}
