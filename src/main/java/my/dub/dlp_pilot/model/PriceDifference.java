package my.dub.dlp_pilot.model;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PriceDifference extends PriceData {

    private BigDecimal avgValue;

    private List<BigDecimal> values = new ArrayList<>();

    private ExchangeName exchangeName2;

    private ZonedDateTime lastRecordDateTime;

    public BigDecimal getCurrentValue() {
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return values.get(values.size() - 1);
    }

    public String toShortString() {
        return new StringJoiner(", ", PriceDifference.class.getSimpleName() + "[", "]")
                .add("base=" + getBase())
                .add("target=" + getTarget())
                .add("exchangeName1=" + getExchangeName())
                .add("exchangeName2=" + exchangeName2)
                .add(avgValue == null ? "" : "avgValue=" + avgValue)
                .toString();
    }
}
