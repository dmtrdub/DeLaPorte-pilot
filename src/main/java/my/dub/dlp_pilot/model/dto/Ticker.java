package my.dub.dlp_pilot.model.dto;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.Data;
import lombok.EqualsAndHashCode;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.PositionSide;
import my.dub.dlp_pilot.util.Calculations;

@Data
@EqualsAndHashCode(callSuper = true)
public class Ticker extends PriceData {

    public Ticker(ExchangeName exchangeName) {
        super(exchangeName);
        dateTime = Instant.now();
    }

    private BigDecimal priceBid;

    private BigDecimal priceAsk;

    // if empty - replaced by priceBid
    private BigDecimal closePrice;

    private BigDecimal askQuantity;

    private BigDecimal bidQuantity;

    @EqualsAndHashCode.Exclude
    private boolean stale;

    @EqualsAndHashCode.Exclude
    private BigDecimal previousPriceAsk;

    @EqualsAndHashCode.Exclude
    private BigDecimal previousPriceBid;

    @EqualsAndHashCode.Exclude
    private Instant dateTime;

    public BigDecimal getPriceOnOpen(PositionSide side) {
        if (PositionSide.SHORT.equals(side)) {
            return priceBid;
        } else if (PositionSide.LONG.equals(side)) {
            return priceAsk;
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal getTotalQuantity() {
        return askQuantity.add(bidQuantity);
    }

    public BigDecimal getSpread() {
        return priceAsk.subtract(priceBid);
    }

    public boolean isPriceInvalid() {
        return priceAsk == null || priceBid == null || Calculations.isNotPositive(priceAsk) || Calculations
                .isNotPositive(priceBid);
    }
}
