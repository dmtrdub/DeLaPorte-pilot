package my.dub.dlp_pilot.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.Digits;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

import static my.dub.dlp_pilot.Constants.AMOUNT_SCALE;
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

    @Column(precision = 26, scale = AMOUNT_SCALE, columnDefinition = "default 0")
    @Digits(integer = 19, fraction = AMOUNT_SCALE)
    private BigDecimal amount;

    @Column(name = "pnl_currency", nullable = false, precision = 26, scale = PRICE_SCALE)
    @Digits(integer = 19, fraction = PRICE_SCALE)
    private BigDecimal pnl;

    @Column(name = "pnl_usd", nullable = false, precision = 26, scale = PRICE_SCALE)
    @Digits(integer = 19, fraction = PRICE_SCALE)
    private BigDecimal pnlUsd;

    @Column(name = "pnl_min_currency", precision = 26, scale = PRICE_SCALE)
    @Digits(integer = 19, fraction = PRICE_SCALE)
    private BigDecimal pnlMin;

    @Column(name = "pnl_min_usd", precision = 26, scale = PRICE_SCALE)
    @Digits(integer = 19, fraction = PRICE_SCALE)
    private BigDecimal pnlMinUsd;

    @Column(name = "expenses_usd", nullable = false, precision = 20, scale = PRICE_SCALE)
    @Digits(integer = 13, fraction = PRICE_SCALE)
    private BigDecimal expensesUsd;

    @Column(name = "time_start", columnDefinition = "default CURRENT_TIMESTAMP")
    private ZonedDateTime startTime;

    @Column(name = "time_end", columnDefinition = "default CURRENT_TIMESTAMP")
    private ZonedDateTime endTime;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "result_type", nullable = false, columnDefinition = "default 0")
    private TradeResultType resultType;

    //pnlUsd-expensesUsd at the end of trade
    @Column(name = "total_income", nullable = false, precision = 26, scale = PRICE_SCALE)
    @Digits(integer = 19, fraction = PRICE_SCALE)
    private BigDecimal totalIncome;

    @OneToOne(optional = false)
    @JoinColumn(name = "position_short_id")
    private Position positionShort;

    @OneToOne(optional = false)
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

    public BigDecimal getWithdrawFeesTotal() {
        if (positionShort == null || positionLong == null) {
            return BigDecimal.ZERO;
        }
        return positionShort.getExchange().getWithdrawFeeUsd().add(positionLong.getExchange().getWithdrawFeeUsd());
    }
}
