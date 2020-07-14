package my.dub.dlp_pilot.model.client;

import lombok.Data;
import lombok.EqualsAndHashCode;
import my.dub.dlp_pilot.Constants;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.PositionSide;
import my.dub.dlp_pilot.util.Calculations;
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

    private BigDecimal priceBid;

    private BigDecimal priceAsk;

    @EqualsAndHashCode.Exclude
    private boolean stale;

    @EqualsAndHashCode.Exclude
    private BigDecimal previousPriceAsk;

    @EqualsAndHashCode.Exclude
    private BigDecimal previousPriceBid;

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

    public BigDecimal getPriceOnOpen(PositionSide side) {
        if (PositionSide.SHORT.equals(side)) {
            return priceBid;
        } else if (PositionSide.LONG.equals(side)) {
            return priceAsk;
        }
        return BigDecimal.ZERO;
    }

    public boolean isPriceInvalid() {
        return priceAsk == null || priceBid == null || Calculations.isNotPositive(priceAsk) ||
                Calculations.isNotPositive(priceBid) || previousPriceAsk == null || previousPriceBid == null ||
                Calculations.isNotPositive(previousPriceAsk) || Calculations.isNotPositive(previousPriceBid);
    }
}
