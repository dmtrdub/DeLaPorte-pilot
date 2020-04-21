package my.dub.dlp_pilot.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.Digits;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static my.dub.dlp_pilot.Constants.PRICE_SCALE;

@Data
@NoArgsConstructor
@Entity
@Table(name = "trade")
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Exclude
    private Long id;

    @Column(nullable = false, length = 33)
    private String pair;

    @Column(name = "pnl_currency", nullable = false, precision = 26, scale = PRICE_SCALE)
    @Digits(integer = 19, fraction = PRICE_SCALE)
    private BigDecimal pnl;

    @Column(name = "pnl_usd", nullable = false, precision = 26, scale = PRICE_SCALE)
    @Digits(integer = 19, fraction = PRICE_SCALE)
    private BigDecimal pnlUsd;

    @Column(name = "pnl_min_currency", nullable = false, precision = 26, scale = PRICE_SCALE)
    @Digits(integer = 19, fraction = PRICE_SCALE)
    private BigDecimal pnlMin;

    @Column(name = "pnl_min_usd", nullable = false, precision = 26, scale = PRICE_SCALE)
    @Digits(integer = 19, fraction = PRICE_SCALE)
    private BigDecimal pnlMinUsd;

    @Column(name = "time_start", columnDefinition = "default CURRENT_TIMESTAMP")
    private LocalDateTime startTime;

    @Column(name = "time_end", columnDefinition = "default CURRENT_TIMESTAMP")
    private LocalDateTime endTime;

    @Column(name = "expenses_currency", nullable = false, precision = 20, scale = PRICE_SCALE)
    @Digits(integer = 12, fraction = PRICE_SCALE)
    private BigDecimal expenses;

    @Column(name = "expenses_usd", nullable = false, precision = 20, scale = PRICE_SCALE)
    @Digits(integer = 13, fraction = PRICE_SCALE)
    private BigDecimal expensesUsd;

    @Column(name = "open_price_1", nullable = false, precision = 20, scale = PRICE_SCALE)
    @Digits(integer = 13, fraction = PRICE_SCALE)
    private BigDecimal openPrice1;

    @Column(name = "open_price_2", nullable = false, precision = 20, scale = PRICE_SCALE)
    @Digits(integer = 13, fraction = PRICE_SCALE)
    private BigDecimal openPrice2;

    @Column(name = "close_price_1", precision = 20, scale = PRICE_SCALE)
    @Digits(integer = 13, fraction = PRICE_SCALE)
    private BigDecimal closePrice1;

    @Column(name = "close_price_2", precision = 20, scale = PRICE_SCALE)
    @Digits(integer = 13, fraction = PRICE_SCALE)
    private BigDecimal closePrice2;

    @ManyToOne(optional = false)
    @JoinColumn(name = "exchange_1", nullable = false)
    private Exchange exchange1;

    @ManyToOne(optional = false)
    @JoinColumn(name = "exchange_2", nullable = false)
    private Exchange exchange2;
}
