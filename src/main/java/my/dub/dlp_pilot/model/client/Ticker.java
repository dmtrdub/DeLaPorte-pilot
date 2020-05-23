package my.dub.dlp_pilot.model.client;

import lombok.Data;
import lombok.EqualsAndHashCode;
import my.dub.dlp_pilot.util.DateUtils;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Data
public abstract class Ticker {

    public Ticker() {
        dateTime = DateUtils.currentDateTime();
    }

    private String exchange;

    private String pair;

    private BigDecimal price;

    private String baseEndpoint;

    @EqualsAndHashCode.Exclude
    private ZonedDateTime dateTime;
}
