package my.dub.dlp_pilot.model.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import my.dub.dlp_pilot.model.ExchangeName;

@Data
@AllArgsConstructor
public class DetrimentalRecord {

    private ExchangeName exchangeShort;

    private ExchangeName exchangeLong;

    private String base;

    private String target;

    private Instant invalidationDateTime;
}
