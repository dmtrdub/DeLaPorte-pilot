package my.dub.dlp_pilot.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.Digits;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static my.dub.dlp_pilot.Constants.PRICE_SCALE;

@Data
@NoArgsConstructor
@Entity
@Table(name = "transfer")
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Exclude
    private Long id;

    @Column(nullable = false, length = 33)
    private String pair;

    @Column(nullable = false, precision = 26, scale = PRICE_SCALE)
    @Digits(integer = 19, fraction = PRICE_SCALE)
    private BigDecimal sum;

    @Column(name = "time_begin", columnDefinition = "default CURRENT_TIMESTAMP")
    private LocalDateTime beginTime;

    @Column(name = "time_end", columnDefinition = "default CURRENT_TIMESTAMP")
    private LocalDateTime endTime;

    @ManyToOne(optional = false)
    @JoinColumn(name = "exchange_sender", nullable = false)
    private Exchange sender;

    @ManyToOne(optional = false)
    @JoinColumn(name = "exchange_recipient", nullable = false)
    private Exchange recipient;

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false, columnDefinition = "default 0")
    private TransferStatus status;
}
