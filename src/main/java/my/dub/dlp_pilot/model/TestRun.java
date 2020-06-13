package my.dub.dlp_pilot.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Set;

import static my.dub.dlp_pilot.Constants.*;

@Data
@NoArgsConstructor
@Entity
@Table(name = "test_run")
public class TestRun implements Serializable {
    private static final long serialVersionUID = 48L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Exclude
    private Long id;

    @Column(name = "entry_amounts_usd", nullable = false, length = STRING_PARAM_LENGTH)
    private String entryAmountsUsd;

    @Column(name = "entry_min_percentage", nullable = false, precision = 8, scale = PERCENTAGE_SCALE)
    private Double entryMinPercentage;

    @Column(name = "entry_max_percentage", nullable = false, precision = 8, scale = PERCENTAGE_SCALE)
    private Double entryMaxPercentage;

    @Column(name = "exit_diff_percentage", nullable = false, precision = 8, scale = PERCENTAGE_SCALE)
    private Double exitDiffPercentage;

    @Column(name = "trade_timeout_mins", nullable = false)
    private Integer tradeTimeoutMins;

    @Column(name = "detrimental_percentage_delta", nullable = false, precision = 8, scale = PERCENTAGE_SCALE)
    private Double detrimentalPercentageDelta;

    @Column(name = "time_start", columnDefinition = "default CURRENT_TIMESTAMP")
    private ZonedDateTime startTime;

    @Column(name = "time_end", columnDefinition = "default CURRENT_TIMESTAMP")
    private ZonedDateTime endTime;

    @Column(name = "path_to_result_file", length = FILE_PATH_PARAM_LENGTH)
    private String pathToResultFile;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "testRun")
    private Set<Trade> trades;
}
