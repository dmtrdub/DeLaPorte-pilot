package my.dub.dlp_pilot.exception.rest;

public class NonexistentUSDPriceException extends RuntimeException {
    private final String exchange;
    private final String fallbackExchange;
    private final String baseSymbol;

    public NonexistentUSDPriceException(String exchange, String fallbackExchange, String baseSymbol) {
        super(String.format("Unable to get USD price for base symbol %s on %s exchange and on fallback exchange %s",
                            baseSymbol, exchange, fallbackExchange));
        this.exchange = exchange;
        this.fallbackExchange = fallbackExchange;
        this.baseSymbol = baseSymbol;
    }

    public String getExchange() {
        return exchange;
    }

    public String getFallbackExchange() {
        return fallbackExchange;
    }

    public String getBaseSymbol() {
        return baseSymbol;
    }
}
