package my.dub.dlp_pilot.model;

import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BarAverage extends PriceData {

    private BigDecimal averagePrice;

    public BarAverage(ExchangeName exchangeName, String base, String target, Double averagePrice) {
        super(exchangeName, base, target);
        this.averagePrice = BigDecimal.valueOf(averagePrice);
    }
}
