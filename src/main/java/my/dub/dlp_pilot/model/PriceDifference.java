package my.dub.dlp_pilot.model;

import java.math.BigDecimal;
import java.util.StringJoiner;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PriceDifference extends PriceData {

    public PriceDifference(String base, String target, ExchangeName exchangeName, ExchangeName exchangeName2) {
        super(exchangeName, base, target);
        this.exchangeName2 = exchangeName2;
    }

    //exchange1 price - exchange2 price
    private BigDecimal average;
    private ExchangeName exchangeName2;

    public String toShortString() {
        return new StringJoiner(", ", PriceDifference.class.getSimpleName() + "[", "]").add("base=" + getBase())
                .add("target=" + getTarget()).add("exchangeName1=" + getExchangeName())
                .add("exchangeName2=" + exchangeName2).add(average == null ? "" : "average=" + average).toString();
    }
}
