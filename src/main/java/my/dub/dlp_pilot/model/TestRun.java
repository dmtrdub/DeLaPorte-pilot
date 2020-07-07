package my.dub.dlp_pilot.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Set;

import static my.dub.dlp_pilot.Constants.FILE_PATH_PARAM_LENGTH;

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

    @Column(name = "config_params", nullable = false, length = 4000)
    private String configParams;

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
