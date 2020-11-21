package my.dub.dlp_pilot.model.dto;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.Data;
import lombok.EqualsAndHashCode;
import my.dub.dlp_pilot.model.ExchangeName;

@Data
@EqualsAndHashCode(callSuper = true)
public class BarAverage extends PriceData {

    private BigDecimal averagePrice;

    private Instant lastCloseTime;

    public BarAverage(ExchangeName exchangeName, String base, String target, Instant lastCloseTime,
            Double averagePrice) {
        super(exchangeName, base, target);
        this.lastCloseTime = lastCloseTime;
        this.averagePrice = BigDecimal.valueOf(averagePrice);
    }
}
