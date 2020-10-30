package my.dub.dlp_pilot.model;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;

@Slf4j
public enum TimeFrame {
    M1(Duration.of(1, ChronoUnit.MINUTES), "min1", "1m"),
    M5(Duration.of(5, ChronoUnit.MINUTES), "min5", "5m"),
    M15(Duration.of(15, ChronoUnit.MINUTES), "min15", "15m"),
    M30(Duration.of(30, ChronoUnit.MINUTES), "min30", "30m"),
    H1(Duration.of(1, ChronoUnit.HOURS), "hour1", "h1"),
    H4(Duration.of(4, ChronoUnit.HOURS), "hour4", "h4"),
    H6(Duration.of(6, ChronoUnit.HOURS), "hour6", "h6"),
    D1(Duration.of(1, ChronoUnit.DAYS), "day1", "d1");

    private final Duration duration;

    private final String bigoneValue;
    private final String binanceValue;

    TimeFrame(Duration duration, String bigoneValue, String binanceValue) {
        this.duration = duration;
        this.bigoneValue = bigoneValue;
        this.binanceValue = binanceValue;
    }

    public Duration getDuration() {
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
        }
        return "";
    }
}
