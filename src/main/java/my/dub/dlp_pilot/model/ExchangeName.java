package my.dub.dlp_pilot.model;

import static my.dub.dlp_pilot.Constants.BIGONE_SIMPLE_NAME;
import static my.dub.dlp_pilot.Constants.BINANCE_SIMPLE_NAME;
import static my.dub.dlp_pilot.Constants.BITBAY_SIMPLE_NAME;
import static my.dub.dlp_pilot.Constants.BITFINEX_SIMPLE_NAME;
import static my.dub.dlp_pilot.Constants.BITMART_SIMPLE_NAME;
import static my.dub.dlp_pilot.Constants.BITMAX_SIMPLE_NAME;
import static my.dub.dlp_pilot.Constants.BITTREX_SIMPLE_NAME;
import static my.dub.dlp_pilot.Constants.EXMO_SIMPLE_NAME;
import static my.dub.dlp_pilot.Constants.GATE_SIMPLE_NAME;
import static my.dub.dlp_pilot.Constants.HUOBI_SIMPLE_NAME;

import lombok.Getter;

@Getter
public enum ExchangeName {
    BIGONE("BigONE", BIGONE_SIMPLE_NAME),
    BINANCE("Binance", BINANCE_SIMPLE_NAME),
    BITBAY("BitBay", BITBAY_SIMPLE_NAME),
    BITFINEX("Bitfinex", BITFINEX_SIMPLE_NAME),
    BITMART("Bitmart", BITMART_SIMPLE_NAME),
    BITMAX("BitMax", BITMAX_SIMPLE_NAME),
    BITTREX("Bittrex", BITTREX_SIMPLE_NAME),
    EXMO("Exmo", EXMO_SIMPLE_NAME),
    GATE("Gate.io", GATE_SIMPLE_NAME),
    HUOBI("Huobi Global", HUOBI_SIMPLE_NAME);

    private final String fullName;
    private final String simpleName;

    ExchangeName(String fullName, String simpleName) {
        this.fullName = fullName;
        this.simpleName = simpleName;
    }

}
