package my.dub.dlp_pilot.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;
import javax.validation.constraints.Digits;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

import static my.dub.dlp_pilot.Constants.*;

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
    private ZonedDateTime startTime;

    @Column(name = "time_end", columnDefinition = "default CURRENT_TIMESTAMP")
    private ZonedDateTime endTime;

    @Column(name = "entry_percentage_diff", nullable = false, precision = 8, scale = PERCENTAGE_SCALE)
    @Digits(integer = 5, fraction = PERCENTAGE_SCALE)
    private BigDecimal entryPercentageDiff;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "result_type", nullable = false, columnDefinition = "default 0")
    private TradeResultType resultType;

    @OneToOne(optional = false, cascade = CascadeType.ALL)
    @JoinColumn(name = "position_short_id")
    private Position positionShort;

    @OneToOne(optional = false, cascade = CascadeType.ALL)
    @JoinColumn(name = "position_long_id")
    private Position positionLong;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, mappedBy = "trade")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private List<TradeDynamicResultData> resultData;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_run_id", updatable = false, nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private TestRun testRun;

    @Column(name = "written_to_file", nullable = false, columnDefinition = "default 0")
    private Boolean writtenToFile;

    public void setPositions(Position shortPosition, Position longPosition) {
        positionShort = shortPosition;
        positionLong = longPosition;
    }

    public String getPair() {
        return base + DEFAULT_PAIR_DELIMITER + target;
    }

    public boolean isSimilar(Trade otherTrade) {
        if (otherTrade == null) return false;
        if (this == otherTrade) return true;
        if (!base.equals(otherTrade.base)) return false;
        if (!target.equals(otherTrade.target)) return false;
        if (!resultType.equals(otherTrade.getResultType())) return false;
        return positionShort.getExchange().getName().equals(otherTrade.getPositionShort().getExchange().getName()) ||
                positionLong.getExchange().getName().equals(otherTrade.getPositionLong().getExchange().getName());
    }

    public String toShortString() {
        String first =
                "Trade{pair=" + getPair() + ", fixedExpensesUsd=" + fixedExpensesUsd + ", startTime=" + startTime +
                        ", entryPercentageDiff=" +
                        entryPercentageDiff;
        if (!TradeResultType.IN_PROGRESS.equals(resultType)) {
            first = first + ", endTime=" + endTime + ", resultType=" + resultType;
        }
        return first + ", positionShort=" + positionShort.toShortString() + ", positionLong=" +
                positionLong.toShortString() + '}';
    }
}
