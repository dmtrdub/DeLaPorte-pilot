package my.dub.dlp_pilot.model;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;

@Slf4j
public enum TimeFrame {
    M1(Duration.of(1, ChronoUnit.MINUTES), "min1", "1m", "1m", "1", "1m"),
    M5(Duration.of(5, ChronoUnit.MINUTES), "min5", "5m", "5m", "5", "5m"),
    M15(Duration.of(15, ChronoUnit.MINUTES), "min15", "15m", "15m", "15", "15m"),
    M30(Duration.of(30, ChronoUnit.MINUTES), "min30", "30m", "30m", "30", "30m"),
    H1(Duration.of(1, ChronoUnit.HOURS), "hour1", "1h", "1h", "60", "1h"),
    H6(Duration.of(6, ChronoUnit.HOURS), "hour6", "6h", "6h", "360", "6h"),
    H12(Duration.of(12, ChronoUnit.HOURS), "hour12", "12h", "12h", "720", "12h"),
    D1(Duration.of(1, ChronoUnit.DAYS), "day1", "1d", "1D", "1d", "1d"),
    W1(Duration.of(7, ChronoUnit.DAYS), "week1", "1w", "7D", "1w", "7d");

    private final Duration duration;

    private final String bigoneValue;
    private final String binanceValue;
    private final String bitfinexValue;
    private final String bitmaxValue;
    private final String gateValue;

    TimeFrame(Duration duration, String bigoneValue, String binanceValue, String bitfinexValue, String bitmaxValue,
            String gateValue) {
        this.duration = duration;
        this.bigoneValue = bigoneValue;
        this.binanceValue = binanceValue;
        this.bitfinexValue = bitfinexValue;
        this.bitmaxValue = bitmaxValue;
        this.gateValue = gateValue;
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
            case BITFINEX:
                return bitfinexValue;
            case BITMAX:
                return bitmaxValue;
            case GATE:
                return gateValue;
        }
        return "";
    }
}
