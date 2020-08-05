package my.dub.dlp_pilot.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import my.dub.dlp_pilot.util.Calculations;
import my.dub.dlp_pilot.util.DateUtils;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class Ticker extends PriceData {

    public Ticker(ExchangeName exchangeName) {
        super(exchangeName);
        dateTime = DateUtils.currentDateTime();
    }

    private BigDecimal priceBid;

    private BigDecimal priceAsk;

    @EqualsAndHashCode.Exclude
    private boolean stale;

    //TODO: possibly remove previous price
    @EqualsAndHashCode.Exclude
    private BigDecimal previousPriceAsk;

    @EqualsAndHashCode.Exclude
    private BigDecimal previousPriceBid;

    @EqualsAndHashCode.Exclude
    private ZonedDateTime dateTime;

    public boolean isSimilar(Ticker newTicker) {
        if (newTicker == null) return false;
        if (this == newTicker) return false;
        return getExchangeName().equals(newTicker.getExchangeName()) && getBase().equals(newTicker.getBase()) &&
                getTarget().equals(newTicker.getTarget());
    }

    public BigDecimal getPriceOnOpen(PositionSide side) {
        if (PositionSide.SHORT.equals(side)) {
            return priceBid;
        } else if (PositionSide.LONG.equals(side)) {
            return priceAsk;
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal getSpread() {
        return priceAsk.subtract(priceBid);
    }

    public boolean isPriceInvalid() {
        return priceAsk == null || priceBid == null || Calculations.isNotPositive(priceAsk) ||
                Calculations.isNotPositive(priceBid) || previousPriceAsk == null || previousPriceBid == null ||
                Calculations.isNotPositive(previousPriceAsk) || Calculations.isNotPositive(previousPriceBid);
    }
}
