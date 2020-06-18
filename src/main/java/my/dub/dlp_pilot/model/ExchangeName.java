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
    BW("BW.com");

    private String fullName;

    ExchangeName(String fullName) {
        this.fullName = fullName;
    }

    public String getFullName() {
        return fullName;
    }
}