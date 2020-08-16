package my.dub.dlp_pilot.model;

import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DetrimentalRecord {

    private ExchangeName exchangeShort;

    private ExchangeName exchangeLong;

    private String base;

    private String target;

    private ZonedDateTime invalidationDateTime;
}
