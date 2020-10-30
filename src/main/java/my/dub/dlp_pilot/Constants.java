package my.dub.dlp_pilot;

import java.util.List;

public final class Constants {
    private Constants() {
    }

    public static final int PRICE_SCALE = 12;
    public static final int PERCENTAGE_SCALE = 3;
    public static final int VOLUME_SCALE = 6;
    public static final int FEE_SCALE = 6;
    public static final int AMOUNT_SCALE = 7;
    public static final int STRING_PARAM_LENGTH = 50;
    public static final int FILE_PATH_PARAM_LENGTH = 260;

    public static final int MAX_RESULT_SCALE = 5;

    public static final String BIGONE_SIMPLE_NAME = "bigone";
    public static final String BINANCE_SIMPLE_NAME = "binance";
    public static final String BITBAY_SIMPLE_NAME = "bitbay";
    public static final String BITFINEX_SIMPLE_NAME = "bitfinex";
    public static final String BITMART_SIMPLE_NAME = "bitmart";
    public static final String BITMAX_SIMPLE_NAME = "bitmax";
    public static final String BITTREX_SIMPLE_NAME = "bittrex";
    public static final String EXMO_SIMPLE_NAME = "exmo";
    public static final String GATE_SIMPLE_NAME = "gate";
    public static final String HUOBI_SIMPLE_NAME = "huobi";

    public static final String CLIENT_SERVICE_BEAN_NAME_SUFFIX = "ClientService";
    public static final String BIGONE_CLIENT_SERVICE_BEAN_NAME = BIGONE_SIMPLE_NAME + CLIENT_SERVICE_BEAN_NAME_SUFFIX;
    public static final String BINANCE_CLIENT_SERVICE_BEAN_NAME = BINANCE_SIMPLE_NAME + CLIENT_SERVICE_BEAN_NAME_SUFFIX;
    public static final String BITBAY_CLIENT_SERVICE_BEAN_NAME = BITBAY_SIMPLE_NAME + CLIENT_SERVICE_BEAN_NAME_SUFFIX;
    public static final String BITFINEX_CLIENT_SERVICE_BEAN_NAME =
            BITFINEX_SIMPLE_NAME + CLIENT_SERVICE_BEAN_NAME_SUFFIX;
    public static final String BITMART_CLIENT_SERVICE_BEAN_NAME = BITMART_SIMPLE_NAME + CLIENT_SERVICE_BEAN_NAME_SUFFIX;
    public static final String BITMAX_CLIENT_SERVICE_BEAN_NAME = BITMAX_SIMPLE_NAME + CLIENT_SERVICE_BEAN_NAME_SUFFIX;
    public static final String BITTREX_CLIENT_SERVICE_BEAN_NAME = BITTREX_SIMPLE_NAME + CLIENT_SERVICE_BEAN_NAME_SUFFIX;
    public static final String EXMO_CLIENT_SERVICE_BEAN_NAME = EXMO_SIMPLE_NAME + CLIENT_SERVICE_BEAN_NAME_SUFFIX;
    public static final String GATE_CLIENT_SERVICE_BEAN_NAME = GATE_SIMPLE_NAME + CLIENT_SERVICE_BEAN_NAME_SUFFIX;
    public static final String HUOBI_CLIENT_SERVICE_BEAN_NAME = HUOBI_SIMPLE_NAME + CLIENT_SERVICE_BEAN_NAME_SUFFIX;

    public static final List<String> STELLAR_SYMBOLS = List.of("XLM", "STR");
    public static final List<String> BITCOIN_SYMBOLS = List.of("BTC", "XBT");
    public static final List<String> BITCOIN_CASH_SYMBOLS = List.of("BCH", "BCC");
    public static final List<String> BITCOIN_SV_SYMBOLS = List.of("BSV", "BCHSV");

    public static final String DEFAULT_PAIR_DELIMITER = "/";

    public static final List<String> BITFINEX_TARGET_SYMBOLS = List.of("BTC", "ETH", "USD", "EUR", "JPY", "GBP", "EOS");

    public static final String NO_TICKERS_FOUND_IN_RESPONSE_MSG = "No tickers found in response!";
    public static final String NO_BARS_FOUND_IN_RESPONSE_MSG = "No bars found in response!";
    public static final String NO_SYMBOL_DATA_FOUND_IN_RESPONSE_MSG = "No symbol data found in response!";
}
