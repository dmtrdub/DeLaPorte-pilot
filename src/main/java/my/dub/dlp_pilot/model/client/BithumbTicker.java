package my.dub.dlp_pilot.model.client;

import my.dub.dlp_pilot.model.ExchangeName;

// NOTE: use /serverTime as ping check
public class BithumbTicker extends Ticker {

    public BithumbTicker() {
        setExchangeName(ExchangeName.BITHUMB);
    }
}
