package my.dub.dlp_pilot.model.dto;

import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;
import my.dub.dlp_pilot.model.ExchangeName;

@Data
@EqualsAndHashCode(callSuper = true)
public class PriceDifference extends PriceData {

    public PriceDifference(String base, String target, ExchangeName exchangeName, BigDecimal exchange1Average,
            ExchangeName exchangeName2, BigDecimal exchange2Average) {
        super(exchangeName, base, target);
        this.exchange1Average = exchange1Average;
        this.exchangeName2 = exchangeName2;
        this.exchange2Average = exchange2Average;
    }

    private BigDecimal exchange1Average;

    private BigDecimal exchange2Average;

    private ExchangeName exchangeName2;

    public BigDecimal getAverage() {
        return exchange1Average.subtract(exchange2Average);
    }
}
