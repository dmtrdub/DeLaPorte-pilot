package my.dub.dlp_pilot.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
@Transactional
public class BarServiceImpl implements BarService {

    private final BarRepository repository;

    @Autowired
    public BarServiceImpl(BarRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(@NonNull Collection<Bar> bars, @NonNull TestRun testRun) {
        checkNotNull(bars, "Cannot save null Bar collection!");
        checkNotNull(testRun, "Cannot save Bars if TestRun is null!");

        if (bars.isEmpty()) {
            return;
        }
        bars.stream().filter(bar -> bar.getTestRun() == null).forEach(bar -> bar.setTestRun(testRun));
        repository.saveAll(bars);
    }

    @Override
    public boolean deleteAll(@NonNull TestRun testRun) {
        checkNotNull(testRun, "Cannot delete Bars if TestRun is null!");

        Long deletedCount = repository.deleteAllByTestRunIdEquals(testRun.getId());
        boolean deleted = deletedCount > 0L;
        if (deleted) {
            log.info("Successfully deleted {} bars", deletedCount);
        }
        return deleted;
    }

    @Override
    public List<BarAverage> loadAllBarAverages(@NonNull TestRun testRun) {
        checkNotNull(testRun, "Cannot load all BarAverages if TestRun is null!");

        return repository.getAllBarAverages(testRun.getId());
    }

    @Override
    public List<BarAverage> loadBarAverages(@NonNull TestRun testRun, @NonNull ExchangeName exchangeName) {
        checkNotNull(testRun, "Cannot load BarAverages if TestRun is null!");
        checkNotNull(exchangeName, "Cannot load BarAverages if exchangeName is null!");

        return repository.getBarAverages(exchangeName, testRun.getId());
    }

    @Override
    public List<LastBar> loadExchangeLastBars(@NonNull TestRun testRun, @NonNull ExchangeName exchangeName) {
        checkNotNull(testRun, "Cannot load LastBars if TestRun is null!");
        checkNotNull(exchangeName, "Cannot load LastBars if exchangeName is null!");

        return repository.getLastBars(exchangeName, testRun.getId());
    }

}
