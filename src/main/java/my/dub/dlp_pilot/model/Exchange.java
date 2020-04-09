package my.dub.dlp_pilot.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static my.dub.dlp_pilot.Constants.FEE_SCALE;
import static my.dub.dlp_pilot.Constants.PERCENTAGE_SCALE;


@Data
@NoArgsConstructor
@Entity
@Table(name = "exchange", uniqueConstraints = {@UniqueConstraint(name = "exchange_api_name_uindex", columnNames = "api_name"),
        @UniqueConstraint(name = "exchange_name_uindex", columnNames = "name")})
public class Exchange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "api_name", nullable = false, unique = true, length = 200)
    private String apiName;

    @Column(unique = true, nullable = false, length = 200)
    private String name;

    @Column(name = "deposit_fee", nullable = false, precision = 11, scale = FEE_SCALE)
    @Digits(integer = 5, fraction = FEE_SCALE)
    private BigDecimal depositFee;

    @Column(name = "withdraw_fee", nullable = false, precision = 11, scale = FEE_SCALE)
    @Digits(integer = 5, fraction = FEE_SCALE)
    private BigDecimal withdrawFee;

    @Column(name = "taker_fee_percentage", nullable = false, precision = 6, scale = PERCENTAGE_SCALE)
    @Digits(integer = 3, fraction = PERCENTAGE_SCALE)
    @Max(value = 100)
    private BigDecimal takerFeePercentage;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "exchange")
    private Set<Ticker> tickers = new HashSet<>();
}
