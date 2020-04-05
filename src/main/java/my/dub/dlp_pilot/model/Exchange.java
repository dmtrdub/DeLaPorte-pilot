package my.dub.dlp_pilot.model;

import lombok.Data;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "exchange")
public class Exchange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "deposit_fee")
    private Double depositFee;

    @Column(name = "withdraw_fee")
    private Double withdrawFee;

    @Column(name = "taker_fee_percentage")
    private Double takerFeePercentage;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "exchange")
    private Set<Ticker> tickers = new HashSet<>();
}
