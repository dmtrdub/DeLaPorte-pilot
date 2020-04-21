package my.dub.dlp_pilot;

import java.util.List;

public final class Constants {
    private Constants() {
    }

    public static final int VOLUME_SCALE = 3;
    public static final int PRICE_SCALE = 7;
    public static final int PERCENTAGE_SCALE = 3;
    public static final int FEE_SCALE = 6;

    public static final List<String> USD_SYMBOLS = List.of("USD", "USDT");
    public static final List<String> STELLAR_SYMBOLS = List.of("XLM", "STR");
    public static final List<String> BITCOIN_SYMBOLS = List.of("BTC", "XBT");
    public static final List<String> BITCOIN_CASH_SYMBOLS = List.of("BCH", "BCC");
    public static final List<String> BITCOIN_SV_SYMBOLS = List.of("BSV", "BCHSV");

}
