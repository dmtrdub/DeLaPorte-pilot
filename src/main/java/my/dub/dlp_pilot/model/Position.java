package my.dub.dlp_pilot.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.Digits;
import java.io.Serializable;
import java.math.BigDecimal;

import static my.dub.dlp_pilot.Constants.PRICE_SCALE;

@Data
@NoArgsConstructor
@Entity
@Table(name = "position")
public class Position implements Serializable {
    private static final long serialVersionUID = 42L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Exclude
    private Long id;

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    private PositionSide side;

    @Column(name = "open_price", nullable = false, precision = 25, scale = PRICE_SCALE, columnDefinition = "default 0")
    @Digits(integer = 13, fraction = PRICE_SCALE)
    private BigDecimal openPrice;

    @Column(name = "close_price", precision = 25, scale = PRICE_SCALE, columnDefinition = "default 0")
    @Digits(integer = 13, fraction = PRICE_SCALE)
    private BigDecimal closePrice;

    @ManyToOne(optional = false)
    @JoinColumn(name = "exchange_id")
    private Exchange exchange;

    public String toShortString() {
        String closePrices = closePrice != null ? ", closePrice=" + closePrice : "";
        return "Position{openPrice=" + openPrice + closePrices + ", exchange=" + exchange.getFullName() + '}';
    }
}
