package my.dub.dlp_pilot.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import my.dub.dlp_pilot.Constants;
import my.dub.dlp_pilot.model.Bar;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.TestRun;
import my.dub.dlp_pilot.model.dto.BarAverage;
import my.dub.dlp_pilot.model.dto.LastBar;
import my.dub.dlp_pilot.repository.BarRepository;
import my.dub.dlp_pilot.service.BarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * An implementation of {@link BarService} service.
 */
@Slf4j
@Service
@Transactional
public class BarServiceImpl implements BarService {

    private static final String TEST_RUN_PARAMETER = "testRun";

    private final BarRepository repository;

    @Autowired
    public BarServiceImpl(BarRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(@NonNull Collection<Bar> bars, @NonNull TestRun testRun) {
        checkNotNull(bars, Constants.NULL_ARGUMENT_MESSAGE, "bars");
        checkNotNull(testRun, Constants.NULL_ARGUMENT_MESSAGE, TEST_RUN_PARAMETER);

        if (bars.isEmpty()) {
            return;
        }
        bars.stream().filter(bar -> bar.getTestRun() == null).forEach(bar -> bar.setTestRun(testRun));
        repository.saveAll(bars);
    }

    @Override
    public boolean deleteAll(@NonNull TestRun testRun) {
        checkNotNull(testRun, Constants.NULL_ARGUMENT_MESSAGE, TEST_RUN_PARAMETER);

        Long deletedCount = repository.deleteAllByTestRunIdEquals(testRun.getId());
        boolean deleted = deletedCount > 0L;
        if (deleted) {
            log.info("Successfully deleted {} bars", deletedCount);
        }
        return deleted;
    }

    @Override
    public List<BarAverage> loadAllBarAverages(@NonNull TestRun testRun) {
        checkNotNull(testRun, Constants.NULL_ARGUMENT_MESSAGE, TEST_RUN_PARAMETER);

        return repository.getAllBarAverages(testRun.getId());
    }

    @Override
    public List<BarAverage> loadBarAverages(@NonNull TestRun testRun, @NonNull ExchangeName exchangeName) {
        checkNotNull(testRun, Constants.NULL_ARGUMENT_MESSAGE, TEST_RUN_PARAMETER);
        checkNotNull(exchangeName, Constants.NULL_ARGUMENT_MESSAGE, "exchangeName");

        return repository.getBarAverages(exchangeName, testRun.getId());
    }

    @Override
    public List<LastBar> loadLastBars(@NonNull TestRun testRun, @NonNull ExchangeName exchangeName) {
        checkNotNull(testRun, Constants.NULL_ARGUMENT_MESSAGE, TEST_RUN_PARAMETER);
        checkNotNull(exchangeName, Constants.NULL_ARGUMENT_MESSAGE, "exchangeName");

        return repository.getLastBars(exchangeName, testRun.getId());
    }

}
