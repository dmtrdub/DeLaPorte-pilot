package my.dub.dlp_pilot.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.Digits;
import java.io.Serializable;
import java.math.BigDecimal;

import static my.dub.dlp_pilot.Constants.AMOUNT_SCALE;
import static my.dub.dlp_pilot.Constants.PRICE_SCALE;

@Data
@NoArgsConstructor
@Entity
@Table(name = "trade_dynamic_result_data")
public class TradeDynamicResultData implements Serializable {
    private static final long serialVersionUID = 46L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Exclude
    private Long id;

    @Column(name = "amount_usd", precision = 26, scale = AMOUNT_SCALE, columnDefinition = "default 0")
    @Digits(integer = 19, fraction = AMOUNT_SCALE)
    private BigDecimal amountUsd;

    @Column(name = "expenses_usd", nullable = false, precision = 25, scale = PRICE_SCALE)
    @Digits(integer = 13, fraction = PRICE_SCALE)
    private BigDecimal totalExpensesUsd;

    @Column(name = "pnl_usd_short", nullable = false, precision = 31, scale = PRICE_SCALE)
    @Digits(integer = 19, fraction = PRICE_SCALE)
    private BigDecimal pnlUsdShort;

    @Column(name = "pnl_usd_long", nullable = false, precision = 31, scale = PRICE_SCALE)
    @Digits(integer = 19, fraction = PRICE_SCALE)
    private BigDecimal pnlUsdLong;

    @Column(name = "income_usd", nullable = false, precision = 31, scale = PRICE_SCALE)
    @Digits(integer = 19, fraction = PRICE_SCALE)
    private BigDecimal incomeUsd;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Trade trade;
}
