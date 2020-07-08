package my.dub.dlp_pilot.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import my.dub.dlp_pilot.model.client.Ticker;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static my.dub.dlp_pilot.Constants.FEE_SCALE;
import static my.dub.dlp_pilot.Constants.PERCENTAGE_SCALE;


@Data
@NoArgsConstructor
@Entity
@Table(name = "exchange", uniqueConstraints = {
        @UniqueConstraint(name = "exchange_base_endpoint_uindex", columnNames = "base_endpoint"),
        @UniqueConstraint(name = "exchange_name_uindex", columnNames = "name")})
public class Exchange implements Serializable {
    private static final long serialVersionUID = 44L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "base_endpoint", nullable = false, unique = true, length = 400)
    private String baseEndpoint;

    @Enumerated(EnumType.STRING)
    @Column(unique = true, nullable = false, length = 200)
    private ExchangeName name;

    @Column(name = "deposit_fee_usd", nullable = false, precision = 11, scale = FEE_SCALE)
    @Digits(integer = 5, fraction = FEE_SCALE)
    private BigDecimal depositFeeUsd;

    @Column(name = "withdraw_fee_usd", nullable = false, precision = 11, scale = FEE_SCALE)
    @Digits(integer = 5, fraction = FEE_SCALE)
    private BigDecimal withdrawFeeUsd;

    @Column(name = "taker_fee_percentage", nullable = false, precision = 6, scale = PERCENTAGE_SCALE)
    @Digits(integer = 3, fraction = PERCENTAGE_SCALE)
    @Max(value = 100)
    private BigDecimal takerFeePercentage;

    @Column(name = "trust_score", nullable = false, columnDefinition = "default 1")
    private Integer trustScore;

    @Column(name = "api_request_rate_min", nullable = false, columnDefinition = "default 1")
    private int apiRequestsPerMin;

    @EqualsAndHashCode.Exclude
    @Transient
    private transient Set<Ticker> tickers = new HashSet<>();

    @EqualsAndHashCode.Exclude
    @Transient
    private transient boolean faulty;

    public String getFullName() {
        return name.getFullName();
    }

    public BigDecimal getFixedFeesUsd() {
        if (depositFeeUsd == null || withdrawFeeUsd == null) {
            return BigDecimal.ZERO;
        }
        return depositFeeUsd.add(withdrawFeeUsd);
    }
}
