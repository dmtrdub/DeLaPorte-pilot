package my.dub.dlp_pilot.model;

import java.time.Duration;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;

@Slf4j
public enum TimeFrame {
    M1(Duration.of(1, ChronoUnit.MINUTES), "min1", "1m", "1m"),
    M5(Duration.of(5, ChronoUnit.MINUTES), "min5", "5m", "5m"),
    M15(Duration.of(15, ChronoUnit.MINUTES), "min15", "15m", "15m"),
    M30(Duration.of(30, ChronoUnit.MINUTES), "min30", "30m", "30m"),
    H1(Duration.of(1, ChronoUnit.HOURS), "hour1", "1h", "1h"),
    H6(Duration.of(6, ChronoUnit.HOURS), "hour6", "6h", "6h"),
    H12(Duration.of(12, ChronoUnit.HOURS), "hour12", "12h", "12h"),
    D1(Duration.of(1, ChronoUnit.DAYS), "day1", "1d", "1D"),
    W1(Duration.of(7, ChronoUnit.DAYS), "week1", "1w", "7D"),
    MN1(Duration.ZERO, "month1", "1M", "1M");

    private final Duration duration;

    private final String bigoneValue;
    private final String binanceValue;
    private final String bitfinexValue;

    TimeFrame(Duration duration, String bigoneValue, String binanceValue, String bitfinexValue) {
        this.duration = duration;
        this.bigoneValue = bigoneValue;
        this.binanceValue = binanceValue;
        this.bitfinexValue = bitfinexValue;
    }

    public Duration getDuration() {
        if (MN1.equals(this)) {
            return Duration.ofDays(YearMonth.now().lengthOfMonth());
        }
        return duration;
    }

    public static TimeFrame parse(@NonNull String value) {
        TimeFrame timeFrame = null;
        try {
            timeFrame = TimeFrame.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.debug("Unable to parse TimeFrame from {}. Searching for alternative mappings...", value);
            for (TimeFrame tF : TimeFrame.values()) {
                for (ExchangeName exchange : ExchangeName.values()) {
                    if (StringUtils.isNotEmpty(tF.getExchangeValue(exchange))) {
                        return tF;
                    }
                }
            }
        }
        return timeFrame;
    }

    public String getExchangeValue(@NonNull ExchangeName exchangeName) {
        switch (exchangeName) {
            case BIGONE:
                return bigoneValue;
            case BINANCE:
                return binanceValue;
            case BITFINEX:
                return bitfinexValue;
        }
        return "";
    }
}
