package my.dub.dlp_pilot.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.Digits;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

import static my.dub.dlp_pilot.Constants.*;

@Data
@NoArgsConstructor
@Entity
@Table(name = "trade")
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Exclude
    private Long id;

    @Column(precision = 26, scale = AMOUNT_SCALE, columnDefinition = "default 0")
    @Digits(integer = 19, fraction = AMOUNT_SCALE)
    private BigDecimal amount;

    @Column(name = "pnl_currency", nullable = false, precision = 31, scale = PRICE_SCALE)
    @Digits(integer = 19, fraction = PRICE_SCALE)
    private BigDecimal pnl;

    @Column(name = "pnl_usd", nullable = false, precision = 31, scale = PRICE_SCALE)
    @Digits(integer = 19, fraction = PRICE_SCALE)
    private BigDecimal pnlUsd;

    @Column(name = "expenses_usd", nullable = false, precision = 25, scale = PRICE_SCALE)
    @Digits(integer = 13, fraction = PRICE_SCALE)
    private BigDecimal expensesUsd;

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

    //pnlUsd-expensesUsd at the end of trade
    @Column(name = "total_income", nullable = false, precision = 31, scale = PRICE_SCALE)
    @Digits(integer = 19, fraction = PRICE_SCALE)
    private BigDecimal totalIncome;

    @OneToOne(optional = false, cascade = CascadeType.ALL)
    @JoinColumn(name = "position_short_id")
    private Position positionShort;

    @OneToOne(optional = false, cascade = CascadeType.ALL)
    @JoinColumn(name = "position_long_id")
    private Position positionLong;

    public void setPositions(Position shortPosition, Position longPosition) {
        positionShort = shortPosition;
        positionLong = longPosition;
    }

    public void addExpensesUsd(BigDecimal addedExpenses) {
        if (expensesUsd == null || addedExpenses == null) {
            return;
        }
        expensesUsd = expensesUsd.add(addedExpenses);
    }

    public String toShortString() {
        String first =
                "Trade{" + "expensesUsd=" + expensesUsd + ", startTime=" + startTime + ", entryPercentageDiff=" +
                        entryPercentageDiff;
        if (!TradeResultType.IN_PROGRESS.equals(resultType)) {
            first = first + ", endTime=" + endTime + ", pnl=" + pnl + ", pnlUsd=" + pnlUsd + ", resultType=" + resultType;
        }
        return first + ", positionShort=" + positionShort.toShortString() + ", positionLong=" +
                positionLong.toShortString() + '}';
    }
}
