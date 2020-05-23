package my.dub.dlp_pilot.model.client;

import my.dub.dlp_pilot.model.ExchangeName;

public class BWTicker extends Ticker {

    public BWTicker() {
        setExchange(ExchangeName.BW.getFullName());
    }
}
