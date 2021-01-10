package my.dub.dlp_pilot.model.dto;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.NotNull;
import lombok.Data;
import my.dub.dlp_pilot.Constants;
import my.dub.dlp_pilot.model.ExchangeName;

@Data
@MappedSuperclass
public abstract class PriceData {

    public PriceData() {
    }

    public PriceData(ExchangeName exchangeName) {
        this.exchangeName = exchangeName;
    }

    public PriceData(ExchangeName exchangeName, String base, String target) {
        this.exchangeName = exchangeName;
        this.base = base;
        this.target = target;
    }

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "exchange_name", nullable = false, length = 200)
    protected ExchangeName exchangeName;

    // parsed
    @NotNull
    @Column(nullable = false, length = 16)
    protected String base;

    // parsed
    @NotNull
    @Column(nullable = false, length = 16)
    protected String target;

    public String getPair() {
        return base + Constants.DEFAULT_PAIR_DELIMITER + target;
    }

    public boolean isSimilar(PriceData otherPriceData) {
        if (otherPriceData == null) {
            return false;
        }
        if (this == otherPriceData) {
            return false;
        }
        return exchangeName.equals(otherPriceData.getExchangeName()) && base.equals(otherPriceData.getBase()) && target
                .equals(otherPriceData.getTarget());
    }
}
