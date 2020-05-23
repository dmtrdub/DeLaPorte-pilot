package my.dub.dlp_pilot.model.client;

import my.dub.dlp_pilot.model.ExchangeName;

public class BittrexTicker extends Ticker{

    public BittrexTicker() {
        setExchange(ExchangeName.BITTREX.getFullName());
    }
}
