package my.dub.dlp_pilot.model;

public enum ExchangeName {
    BIGONE("BigONE"),
    BINANCE("Binance"),
    BITBAY("BitBay"),
    BITFINEX("Bitfinex"),
    BITMART("Bitmart"),
    BITMAX("BitMax"),
    BITTREX("Bittrex"),
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
