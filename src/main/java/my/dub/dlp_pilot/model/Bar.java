package my.dub.dlp_pilot.model;

import static my.dub.dlp_pilot.Constants.PRICE_SCALE;
import static my.dub.dlp_pilot.Constants.VOLUME_SCALE;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.Digits;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Entity
@Table(name = "bar")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Bar extends PriceData implements Serializable {
    private static final long serialVersionUID = 50L;

    public Bar(ExchangeName exchangeName, String base, String target, BigDecimal open, BigDecimal volume,
            ZonedDateTime openTime, TestRun testRun) {
        super(exchangeName, base, target);
        this.open = this.close = this.high = this.low = open;
        this.volume = volume;
        this.openTime = openTime;
        this.testRun = testRun;
    }

    public Bar(ExchangeName exchangeName, String base, String target, ZonedDateTime openTime, ZonedDateTime closeTime) {
        super(exchangeName, base, target);
        this.openTime = openTime;
        this.closeTime = closeTime;
    }

    public Bar(ExchangeName exchangeName, String base, String target) {
        super(exchangeName, base, target);
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Exclude
    private Long id;

    @Column(name = "open_price", nullable = false, precision = 25, scale = PRICE_SCALE, columnDefinition = "default 0")
    @Digits(integer = 13, fraction = PRICE_SCALE)
    private BigDecimal open;

    @Column(name = "high_price", nullable = false, precision = 25, scale = PRICE_SCALE, columnDefinition = "default 0")
    @Digits(integer = 13, fraction = PRICE_SCALE)
    private BigDecimal high;

    @Column(name = "low_price", nullable = false, precision = 25, scale = PRICE_SCALE, columnDefinition = "default 0")
    @Digits(integer = 13, fraction = PRICE_SCALE)
    private BigDecimal low;

    @Column(name = "close_price", nullable = false, precision = 25, scale = PRICE_SCALE, columnDefinition = "default 0")
    @Digits(integer = 13, fraction = PRICE_SCALE)
    private BigDecimal close;

    @Column(name = "volume", nullable = false, precision = 19, scale = VOLUME_SCALE, columnDefinition = "default 0")
    @Digits(integer = 13, fraction = VOLUME_SCALE)
    private BigDecimal volume;

    @Column(name = "time_open", columnDefinition = "default CURRENT_TIMESTAMP")
    private ZonedDateTime openTime;

    @Column(name = "time_close", columnDefinition = "default CURRENT_TIMESTAMP")
    private ZonedDateTime closeTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_run_id", updatable = false, nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private TestRun testRun;

    private transient boolean closed;

    public void setOpenCloseTime(ZonedDateTime openTime, TimeFrame timeFrame) {
        this.openTime = openTime;
        this.closeTime = openTime.plus(timeFrame.getDuration());
    }
}

