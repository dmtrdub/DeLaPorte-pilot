package my.dub.dlp_pilot.model;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ticker")
public class Ticker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String base;

    private String target;

    @ManyToOne(optional = false)
    @JoinColumn(name = "exchange_id", nullable = false)
    private Exchange exchange;

    private Double volume;

    @Column(name = "volume_btc")
    private Double volumeBtc;

    @Column(name = "volume_usd")
    private Double volumeUsd;

    private Double price;

    @Column(name = "price_btc")
    private Double priceBtc;

    @Column(name = "price_usd")
    private Double priceUsd;

    @Column(name = "spread_percentage")
    private Double spreadPercentage;

    private LocalDateTime time;

    private boolean anomaly;

    private boolean stale;
}
