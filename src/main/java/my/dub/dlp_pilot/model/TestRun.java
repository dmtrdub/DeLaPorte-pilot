package my.dub.dlp_pilot.model;

import static my.dub.dlp_pilot.Constants.FILE_PATH_PARAM_LENGTH;

import java.io.Serializable;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

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

    @NotNull
    @Column(name = "config_params", nullable = false, length = 4000)
    private String configParams;

    @Column(name = "time_start", nullable = false, columnDefinition = "datetime default CURRENT_TIMESTAMP")
    private LocalDateTime startTime;

    @Column(name = "preload_time_start", nullable = false, columnDefinition = "datetime default CURRENT_TIMESTAMP")
    private LocalDateTime preloadStartTime;

    @Column(name = "trades_time_start")
    private LocalDateTime tradesStartTime;

    @Column(name = "time_end")
    private LocalDateTime endTime;

    @Column(name = "path_to_result_file", length = FILE_PATH_PARAM_LENGTH)
    private String pathToResultFile;

    @Column(name = "forced_exit")
    private Boolean forcedExit;
}
