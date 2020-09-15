package my.dub.dlp_pilot.service.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import my.dub.dlp_pilot.configuration.ParametersHolder;
import my.dub.dlp_pilot.model.TestRun;
import my.dub.dlp_pilot.repository.TestRunRepository;
import my.dub.dlp_pilot.service.TestRunService;
import my.dub.dlp_pilot.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class TestRunServiceImpl implements TestRunService {

    private final TestRunRepository repository;
    private final ParametersHolder parameters;

    private TestRun currentTestRun;
    private LocalDateTime tradeStopDateTime;
    private LocalDateTime testRunEndDateTime;
    private LocalDateTime initDataCaptureEndDateTime;

    private final AtomicBoolean tradeStop = new AtomicBoolean();
    private final AtomicBoolean testRunEnd = new AtomicBoolean();
    private final AtomicBoolean initialDataCapture = new AtomicBoolean(true);

    @Autowired
    public TestRunServiceImpl(TestRunRepository repository, ParametersHolder parameters) {
        this.repository = repository;
        this.parameters = parameters;
    }

    @Override
    @Transactional
    public void createAndSave() {
        TestRun testRun = new TestRun();
        String configuration = parameters.getConfiguration()
                .orElseThrow(() -> new IllegalArgumentException("Empty configuration passed to Test Run!"));
        testRun.setConfigParams(configuration);
        LocalDateTime startTime = LocalDateTime.now();
        testRun.setStartTime(startTime);
        Duration dataCapturePeriodDuration = parameters.getDataCapturePeriodDuration();
        tradeStopDateTime = startTime.plus(parameters.getTestRunDuration()).plus(dataCapturePeriodDuration);
        testRun.setEndTime(tradeStopDateTime.plus(parameters.getExitDelayDuration()));
        currentTestRun = repository.save(testRun);
        testRunEndDateTime = currentTestRun.getEndTime();
        initDataCaptureEndDateTime = startTime.plus(dataCapturePeriodDuration);
        log.info("TEST RUN ENDING AT: {}\n", DateUtils.formatDateTime(testRunEndDateTime));
    }

    @Override
    public TestRun getCurrentTestRun() {
        return currentTestRun;
    }

    @Override
    public boolean checkTradeStopped() {
        if (!tradeStop.get()) {
            tradeStop.set(!LocalDateTime.now().isBefore(tradeStopDateTime));
        }
        return tradeStop.get();
    }

    @Override
    public boolean checkTestRunEnd() {
        if (!testRunEnd.get()) {
            testRunEnd.set(!LocalDateTime.now().isBefore(testRunEndDateTime));
        }
        return testRunEnd.get();
    }

    @Override
    public boolean checkInitialDataCapture() {
        if (initialDataCapture.get()) {
            initialDataCapture.set(!LocalDateTime.now().isAfter(initDataCaptureEndDateTime));
        }
        return initialDataCapture.get();
    }

    @Override
    public void checkExitFile() {
        if (checkTradeStopped()) {
            return;
        }
        String forcedExitFilePath = parameters.getForcedExitFilePath();
        if (StringUtils.isEmpty(forcedExitFilePath)) {
            return;
        }
        File exitFile = new File(forcedExitFilePath);
        if (!exitFile.exists()) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(exitFile.toPath());
            if (lines.stream().anyMatch(line -> line.contains(parameters.getExitCode()))) {
                tradeStopDateTime = LocalDateTime.now();
                testRunEndDateTime = tradeStopDateTime.plus(parameters.getExitDelayDuration());
                log.info("Found force exit file {} containing valid exit code! Stopping trades now. "
                                 + "Test Run will end at {}", exitFile.getName(),
                         DateUtils.formatDateTime(testRunEndDateTime));
                Files.delete(exitFile.toPath());
            }
        } catch (IOException e) {
            log.error("Error when reading exit file {}! Details: {}", exitFile, e.getMessage());
        }
    }

}
