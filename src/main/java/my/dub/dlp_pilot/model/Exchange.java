package my.dub.dlp_pilot.model;

import static my.dub.dlp_pilot.Constants.FEE_SCALE;
import static my.dub.dlp_pilot.Constants.PERCENTAGE_SCALE;

import java.io.Serializable;
import java.math.BigDecimal;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "exchange",
       uniqueConstraints = { @UniqueConstraint(name = "exchange_base_endpoint_uindex", columnNames = "base_endpoint"),
               @UniqueConstraint(name = "exchange_name_uindex", columnNames = "name") })
public class Exchange implements Serializable {
    private static final long serialVersionUID = 44L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "base_endpoint", nullable = false, unique = true, length = 400)
    private String baseEndpoint;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(unique = true, nullable = false, length = 200)
    private ExchangeName name;

    @NotNull
    @Column(name = "deposit_fee_usd", nullable = false, precision = 11, scale = FEE_SCALE)
    @Digits(integer = 5, fraction = FEE_SCALE)
    private BigDecimal depositFeeUsd;

    @NotNull
    @Column(name = "withdraw_fee_usd", nullable = false, precision = 11, scale = FEE_SCALE)
    @Digits(integer = 5, fraction = FEE_SCALE)
    private BigDecimal withdrawFeeUsd;

    @NotNull
    @Column(name = "taker_fee_percentage", nullable = false, precision = 6, scale = PERCENTAGE_SCALE)
    @Digits(integer = 3, fraction = PERCENTAGE_SCALE)
    @Max(value = 100)
    private BigDecimal takerFeePercentage;

    @NotNull
    @Column(name = "bars_per_request", nullable = false, columnDefinition = "int default 1")
    private Integer maxBarsPerRequest;

    @NotNull
    @Column(name = "trust_score", nullable = false, columnDefinition = "int default 1")
    private Integer trustScore;

    @NotNull
    @Column(name = "api_request_rate_min", nullable = false, columnDefinition = "int default 1")
    private int apiRequestsPerMin;

    @NotNull
    @Column(name = "api_request_rate_min_preload", nullable = false, columnDefinition = "int default 1")
    private int apiRequestsPerMinPreload;

    // if true - bars are loaded from oldest to newest in selected range
    @NotNull
    @Column(name = "asc_preload", nullable = false, columnDefinition = "tinyint(1) default 1")
    private Boolean ascendingPreload;

    public String getFullName() {
        return name.getFullName();
    }

    public BigDecimal getFixedFeesUsd() {
        return depositFeeUsd.add(withdrawFeeUsd);
    }
}
