package my.dub.dlp_pilot.model.client;

import lombok.Data;
import lombok.EqualsAndHashCode;
import my.dub.dlp_pilot.Constants;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.util.DateUtils;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Data
public abstract class Ticker {

    public Ticker() {
        dateTime = DateUtils.currentDateTime();
    }

    private ExchangeName exchangeName;

    private String base;

    private String target;

    private BigDecimal price;

    @EqualsAndHashCode.Exclude
    private boolean stale;

    @EqualsAndHashCode.Exclude
    private BigDecimal previousPrice;

    @EqualsAndHashCode.Exclude
    private ZonedDateTime dateTime;

    public String getPair() {
        return base + Constants.DEFAULT_PAIR_DELIMITER + target;
    }

    public boolean isSimilar(Ticker newTicker) {
        if (newTicker == null) return false;
        if (this == newTicker) return false;
        return exchangeName.equals(newTicker.exchangeName) && base.equals(newTicker.base) &&
                target.equals(newTicker.target);
    }
}
