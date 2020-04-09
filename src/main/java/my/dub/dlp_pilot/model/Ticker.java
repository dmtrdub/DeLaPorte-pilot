package my.dub.dlp_pilot.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static my.dub.dlp_pilot.Constants.*;

@Data
@NoArgsConstructor
@Entity
@Table(name = "ticker")
public class Ticker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 16)
    private String base;

    @Column(nullable = false, length = 16)
    private String target;

    @ManyToOne(optional = false)
    @JoinColumn(name = "exchange_id", nullable = false)
    private Exchange exchange;

    @Column(nullable = false, precision = 19, scale = VOLUME_SCALE)
    @Digits(integer = 16, fraction = VOLUME_SCALE)
    private BigDecimal volume;

    @Column(name = "volume_btc", nullable = false, precision = 19, scale = VOLUME_SCALE)
    @Digits(integer = 16, fraction = VOLUME_SCALE)
    private BigDecimal volumeBtc;

    @Column(name = "volume_usd", nullable = false, precision = 19, scale = VOLUME_SCALE)
    @Digits(integer = 16, fraction = VOLUME_SCALE)
    private BigDecimal volumeUsd;

    @Column(nullable = false, precision = 16, scale = PRICE_SCALE)
    @Digits(integer = 8, fraction = PRICE_SCALE)
    private BigDecimal price;

    @Column(name = "price_btc", nullable = false, precision = 16, scale = PRICE_SCALE)
    @Digits(integer = 8, fraction = PRICE_SCALE)
    private BigDecimal priceBtc;

    @Column(name = "price_usd", nullable = false, precision = 16, scale = PRICE_SCALE)
    @Digits(integer = 8, fraction = PRICE_SCALE)
    private BigDecimal priceUsd;

    @Column(name = "spread_percentage", nullable = false, precision = 6, scale = PERCENTAGE_SCALE)
    @Digits(integer = 3, fraction = PERCENTAGE_SCALE)
    @Max(value = 100)
    private BigDecimal spreadPercentage;

    @Column(columnDefinition = "default CURRENT_TIMESTAMP")
    private LocalDateTime time;

    @Column(columnDefinition = "default 0")
    private boolean anomaly;

    @Column(columnDefinition = "default 0")
    private boolean stale;
}
