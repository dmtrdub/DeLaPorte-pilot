package my.dub.dlp_pilot.model.dto;

import java.time.Instant;
import lombok.Data;
import lombok.EqualsAndHashCode;
import my.dub.dlp_pilot.model.ExchangeName;

@Data
@EqualsAndHashCode(callSuper = true)
public class LastBar extends PriceData {

    private Instant closeTime;

    public LastBar(ExchangeName exchangeName, String base, String target, Instant closeTime) {
        super(exchangeName, base, target);
        this.closeTime = closeTime;
    }
}
