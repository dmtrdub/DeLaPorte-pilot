package my.dub.dlp_pilot.model;

import lombok.Data;
import my.dub.dlp_pilot.Constants;

@Data
public abstract class PriceData {

    public PriceData() {
    }

    public PriceData(ExchangeName exchangeName) {
        this.exchangeName = exchangeName;
    }

    private ExchangeName exchangeName;

    private String base;

    private String target;

    public String getPair() {
        return base + Constants.DEFAULT_PAIR_DELIMITER + target;
    }
}
