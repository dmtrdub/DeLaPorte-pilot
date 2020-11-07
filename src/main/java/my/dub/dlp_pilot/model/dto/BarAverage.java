package my.dub.dlp_pilot.model.dto;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import my.dub.dlp_pilot.model.ExchangeName;

@Data
@EqualsAndHashCode(callSuper = true)
public class BarAverage extends PriceData {

    private BigDecimal averagePrice;

    private ZonedDateTime closeTime;

    public BarAverage(ExchangeName exchangeName, String base, String target, ZonedDateTime closeTime,
            Double averagePrice) {
        super(exchangeName, base, target);
        this.closeTime = closeTime;
        this.averagePrice = BigDecimal.valueOf(averagePrice);
    }
}
