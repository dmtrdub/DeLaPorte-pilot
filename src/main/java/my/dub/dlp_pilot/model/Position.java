package my.dub.dlp_pilot.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import my.dub.dlp_pilot.Constants;

import javax.persistence.*;
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

    @Column(nullable = false, length = 16)
    private String base;

    @Column(nullable = false, length = 16)
    private String target;

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    private PositionSide side;

    @Column(name = "open_price", nullable = false, precision = 25, scale = PRICE_SCALE, columnDefinition = "default 0")
    @Digits(integer = 13, fraction = PRICE_SCALE)
    private BigDecimal openPrice;

    @Column(name = "open_price_usd", nullable = false, precision = 25, scale = PRICE_SCALE, columnDefinition = "default 0")
    @Digits(integer = 13, fraction = PRICE_SCALE)
    private BigDecimal openPriceUsd;

    @Column(name = "close_price", precision = 25, scale = PRICE_SCALE, columnDefinition = "default 0")
    @Digits(integer = 13, fraction = PRICE_SCALE)
    private BigDecimal closePrice;

    @Column(name = "close_price_usd", precision = 25, scale = PRICE_SCALE, columnDefinition = "default 0")
    @Digits(integer = 13, fraction = PRICE_SCALE)
    private BigDecimal closePriceUsd;

    @ManyToOne(optional = false)
    @JoinColumn(name = "exchange_id")
    private Exchange exchange;

    public boolean isSimilar(Position otherPosition) {
        if (otherPosition == null) return false;
        if (this == otherPosition) return true;
        if (!base.equals(otherPosition.base)) return false;
        if (!target.equals(otherPosition.target)) return false;
        if(!exchange.getName().equals(otherPosition.exchange.getName())) return false;
        return side == otherPosition.side;
    }

    public String toShortString() {
        String closePrices =
                closePrice != null ? ", closePrice=" + closePrice + ", closePriceUsd=" + closePriceUsd : "";
        return "Position{" + "symbol='" + base + Constants.DEFAULT_PAIR_DELIMITER + target + "', openPrice=" +
                openPrice + ", openPriceUsd=" + openPriceUsd + closePrices + ", exchange=" + exchange.getFullName() +
                '}';
    }
}
