package my.dub.dlp_pilot.model.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import my.dub.dlp_pilot.model.ExchangeName;

@Data
@EqualsAndHashCode(callSuper = true)
public class SymbolPair extends PriceData {

    private final String name;

    public SymbolPair(ExchangeName exchangeName, String name) {
        super(exchangeName);
        this.name = name;
    }
}
