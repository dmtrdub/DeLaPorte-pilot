package my.dub.dlp_pilot.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
@AllArgsConstructor
public class DetrimentalRecord {

    private ExchangeName exchangeShort;
    private ExchangeName exchangeLong;
    private ZonedDateTime invalidationDateTime;
}
