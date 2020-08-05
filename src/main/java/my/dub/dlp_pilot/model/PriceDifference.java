package my.dub.dlp_pilot.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import my.dub.dlp_pilot.util.DateUtils;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

@Data
@EqualsAndHashCode(callSuper = true)
public class PriceDifference extends PriceData {

    private BigDecimal avgValue;

    private List<BigDecimal> values = new ArrayList<>();

    // average value at which the breakthrough occurred, can be updated
    private BigDecimal breakThroughAvgPriceDiff;

    private ZonedDateTime breakThroughDateTime;

    private ExchangeName exchangeName2;

    private ZonedDateTime lastRecordDateTime;

    public BigDecimal getCurrentValue() {
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return values.get(values.size() - 1);
    }

    public void setCurrentBreakThroughPrice(BigDecimal avgPrice) {
        breakThroughAvgPriceDiff = avgPrice;
        breakThroughDateTime = DateUtils.currentDateTime();
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
