package my.dub.dlp_pilot.model;

import static my.dub.dlp_pilot.Constants.DEFAULT_PAIR_DELIMITER;
import static my.dub.dlp_pilot.Constants.PERCENTAGE_SCALE;
import static my.dub.dlp_pilot.Constants.PRICE_SCALE;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.Digits;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import my.dub.dlp_pilot.util.DateUtils;

@Data
@NoArgsConstructor
@Entity
@Table(name = "trade")
public class Trade implements Serializable {
    private static final long serialVersionUID = 40L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Exclude
    private Long id;

    @Column(nullable = false, length = 16)
    private String base;

    @Column(nullable = false, length = 16)
    private String target;

    @Column(name = "fixed_expenses_usd", nullable = false, precision = 25, scale = PRICE_SCALE)
    @Digits(integer = 13, fraction = PRICE_SCALE)
    private BigDecimal fixedExpensesUsd;

    @Column(name = "time_start", columnDefinition = "default CURRENT_TIMESTAMP")
    private Instant startTime;

    @Column(name = "time_end", columnDefinition = "default CURRENT_TIMESTAMP")
    private Instant endTime;

    @Column(name = "entry_percentage_diff", nullable = false, precision = 8, scale = PERCENTAGE_SCALE)
    @Digits(integer = 5, fraction = PERCENTAGE_SCALE)
    private BigDecimal entryPercentageDiff;

    @Column(name = "average_price_diff", nullable = false, precision = 25, scale = PRICE_SCALE)
    @Digits(integer = 13, fraction = PRICE_SCALE)
    private BigDecimal averagePriceDiff;

    @Column(name = "open_price_diff", nullable = false, precision = 25, scale = PRICE_SCALE)
    @Digits(integer = 13, fraction = PRICE_SCALE)
    private BigDecimal openPriceDiff;

    @Column(name = "close_price_diff", nullable = false, precision = 25, scale = PRICE_SCALE)
    @Digits(integer = 13, fraction = PRICE_SCALE)
    private BigDecimal closePriceDiff;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "result_type", nullable = false, columnDefinition = "default 0")
    private TradeResultType resultType;

    @OneToOne(optional = false, cascade = CascadeType.ALL)
    @JoinColumn(name = "position_short_id")
    private Position positionShort;

    @OneToOne(optional = false, cascade = CascadeType.ALL)
    @JoinColumn(name = "position_long_id")
    private Position positionLong;

    @Column(name = "expenses_usd", nullable = false, precision = 25, scale = PRICE_SCALE)
    @Digits(integer = 13, fraction = PRICE_SCALE)
    private BigDecimal totalExpensesUsd;

    @Column(name = "income_usd", nullable = false, precision = 31, scale = PRICE_SCALE)
    @Digits(integer = 19, fraction = PRICE_SCALE)
    private BigDecimal incomeUsd;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_run_id", updatable = false, nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private TestRun testRun;

    @Column(name = "written_to_file", nullable = false, columnDefinition = "default 0")
    private Boolean writtenToFile;

    // for local storage
    private transient Long localId;

    public void setPositions(Position shortPosition, Position longPosition) {
        positionShort = shortPosition;
        positionLong = longPosition;
    }

    public String getPair() {
        return base + DEFAULT_PAIR_DELIMITER + target;
    }

    public String toShortString() {
        String firstSubStr = String.format("Trade{pair=%s, startTime=%s, entryPercentageDiff=%s", getPair(),
                                           DateUtils.formatDateTime(startTime), entryPercentageDiff);
        StringBuilder builder = new StringBuilder(firstSubStr);
        if (!TradeResultType.IN_PROGRESS.equals(resultType)) {
            builder.append(String.format(", endTime=%s, resultType=%s", DateUtils.formatDateTime(endTime), resultType));
        }
        builder.append(String.format(", positionShort=%s, positionLong=%s}", positionShort.toShortString(),
                                     positionLong.toShortString()));
        return builder.toString();
    }
}
