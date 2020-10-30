package my.dub.dlp_pilot.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SymbolPair extends PriceData {

    private final String name;

    public SymbolPair(ExchangeName exchangeName, String name) {
        super(exchangeName);
        this.name = name;
    }
}
