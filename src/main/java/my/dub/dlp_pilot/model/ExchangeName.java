package my.dub.dlp_pilot.model;

public enum ExchangeName {
    BIGONE("BigONE"),
    BINANCE("Binance"),
    BITBAY("BitBay"),
    BITFINEX("Bitfinex"),
    BITHUMB("Bithumb Global"),
    BITMART("Bitmart"),
    BITMAX("BitMax"),
    BITTREX("Bittrex"),
    BW("BW.com"),
    COINONE("Coinone"),
    EXMO("Exmo"),
    GATE("Gate.io"),
    HUOBI("Huobi Global");

    private final String fullName;

    ExchangeName(String fullName) {
        this.fullName = fullName;
    }

    public String getFullName() {
        return fullName;
    }
}
