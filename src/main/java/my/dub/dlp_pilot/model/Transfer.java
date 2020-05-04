package my.dub.dlp_pilot.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.Digits;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

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

    @Column(name = "base_1", nullable = false, length = 16)
    private String base1;

    @Column(name = "target_1", nullable = false, length = 16)
    private String target1;

    @Column(name = "base_2", nullable = false, length = 16)
    private String base2;

    @Column(name = "target_2", nullable = false, length = 16)
    private String target2;

    @Column(name = "sum_each", nullable = false, precision = 26, scale = PRICE_SCALE)
    @Digits(integer = 19, fraction = PRICE_SCALE)
    private BigDecimal sumEach;

    @Column(name = "time_begin", columnDefinition = "default CURRENT_TIMESTAMP")
    private ZonedDateTime beginTime;

    @Column(name = "time_end", columnDefinition = "default CURRENT_TIMESTAMP")
    private ZonedDateTime endTime;

    @ManyToOne(optional = false)
    @JoinColumn(name = "exchange_sender_1")
    private Exchange sender1;

    @ManyToOne(optional = false)
    @JoinColumn(name = "exchange_sender_2")
    private Exchange sender2;

    @ManyToOne(optional = false)
    @JoinColumn(name = "exchange_recipient_1")
    private Exchange recipient1;

    @ManyToOne(optional = false)
    @JoinColumn(name = "exchange_recipient_2")
    private Exchange recipient2;

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false, columnDefinition = "default 0")
    private TransferStatus status;
}
