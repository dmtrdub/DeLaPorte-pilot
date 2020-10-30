package my.dub.dlp_pilot.exception.client;

import lombok.Getter;

@Getter
public class NonexistentUSDPriceException extends RuntimeException {
    private final String exchange;
    private final String baseSymbol;

    public NonexistentUSDPriceException(String exchange, String baseSymbol) {
        super(String.format("Unable to get USD price for base symbol %s on %s exchange",
                            baseSymbol, exchange));
        this.exchange = exchange;
        this.baseSymbol = baseSymbol;
    }

}
