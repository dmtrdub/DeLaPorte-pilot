package my.dub.dlp_pilot.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.Digits;
import java.math.BigDecimal;

import static my.dub.dlp_pilot.Constants.PRICE_SCALE;

@Data
@NoArgsConstructor
@Entity
@Table(name = "position")
public class Position {

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

    @Column(name = "pnl_currency", nullable = false, precision = 26, scale = PRICE_SCALE, columnDefinition = "default 0")
    @Digits(integer = 19, fraction = PRICE_SCALE)
    private BigDecimal pnl;

    @Column(name = "pnl_usd", nullable = false, precision = 26, scale = PRICE_SCALE, columnDefinition = "default 0")
    @Digits(integer = 19, fraction = PRICE_SCALE)
    private BigDecimal pnlUsd;

    @Column(name = "pnl_min_currency", nullable = false, precision = 26, scale = PRICE_SCALE, columnDefinition = "default 0")
    @Digits(integer = 19, fraction = PRICE_SCALE)
    private BigDecimal pnlMin;

    @Column(name = "pnl_min_usd", nullable = false, precision = 26, scale = PRICE_SCALE, columnDefinition = "default 0")
    @Digits(integer = 19, fraction = PRICE_SCALE)
    private BigDecimal pnlMinUsd;

    @Column(name = "open_price", nullable = false, precision = 20, scale = PRICE_SCALE, columnDefinition = "default 0")
    @Digits(integer = 13, fraction = PRICE_SCALE)
    private BigDecimal openPrice;

    @Column(name = "open_price_usd", nullable = false, precision = 20, scale = PRICE_SCALE, columnDefinition = "default 0")
    @Digits(integer = 13, fraction = PRICE_SCALE)
    private BigDecimal openPriceUsd;

    @Column(name = "close_price", precision = 20, scale = PRICE_SCALE, columnDefinition = "default 0")
    @Digits(integer = 13, fraction = PRICE_SCALE)
    private BigDecimal closePrice;

    @Column(name = "close_price_usd", precision = 20, scale = PRICE_SCALE, columnDefinition = "default 0")
    @Digits(integer = 13, fraction = PRICE_SCALE)
    private BigDecimal closePriceUsd;

    @ManyToOne(optional = false)
    @JoinColumn(name = "exchange_id")
    private Exchange exchange;
}
