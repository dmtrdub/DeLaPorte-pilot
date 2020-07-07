package my.dub.dlp_pilot.service.impl;

import lombok.extern.slf4j.Slf4j;
import my.dub.dlp_pilot.configuration.ParametersComponent;
import my.dub.dlp_pilot.model.TestRun;
import my.dub.dlp_pilot.repository.TestRunRepository;
import my.dub.dlp_pilot.service.TestRunService;
import my.dub.dlp_pilot.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class TestRunServiceImpl implements TestRunService {

    private final TestRunRepository repository;
    private final ParametersComponent parameters;

    private TestRun currentTestRun;
    private ZonedDateTime tradeStopDateTime;
    private ZonedDateTime testRunEndDateTime;
    private ZonedDateTime tickerDataCaptureEndDateTime;

    private final AtomicBoolean tradeStop = new AtomicBoolean();
    private final AtomicBoolean testRunEnd = new AtomicBoolean();
    private final AtomicBoolean tickerDataCapture = new AtomicBoolean(true);

    @Autowired
    public TestRunServiceImpl(TestRunRepository repository, ParametersComponent parameters) {
        this.repository = repository;
        this.parameters = parameters;
    }

    @Override
    @Transactional
    public void createAndSave() {
        TestRun testRun = new TestRun();
        String configuration = parameters.getConfiguration().orElseThrow(
                () -> new IllegalArgumentException("Empty configuration passed to Test Run!"));
        testRun.setConfigParams(configuration);
        ZonedDateTime startTime = DateUtils.currentDateTime();
        testRun.setStartTime(startTime);
        int tickerDataCaptureDurationSeconds = parameters.getStaleDifferenceSeconds() + 1;
        tradeStopDateTime =
                startTime.plus(parameters.getTestRunDuration()).plusSeconds(tickerDataCaptureDurationSeconds);
        testRun.setEndTime(tradeStopDateTime.plusSeconds(parameters.getExitMaxDelaySeconds()));
        currentTestRun = repository.save(testRun);
        testRunEndDateTime = currentTestRun.getEndTime();
        tickerDataCaptureEndDateTime = startTime.plusSeconds(tickerDataCaptureDurationSeconds);
    }

    @Override
    public TestRun getCurrentTestRun() {
        return currentTestRun;
    }

    @Override
    public boolean isTradeStopped() {
        if (tradeStopDateTime != null && !tradeStop.get()) {
            tradeStop.set(!DateUtils.currentDateTime().isBefore(tradeStopDateTime));
        }
        return tradeStop.get();
    }

    @Override
    public boolean isTestRunEnd() {
        if (testRunEndDateTime != null && !testRunEnd.get()) {
            testRunEnd.set(!DateUtils.currentDateTime().isBefore(testRunEndDateTime));
        }
        return testRunEnd.get();
    }

    @Override
    public boolean isTickerDataCapture() {
        if (tickerDataCaptureEndDateTime != null && tickerDataCapture.get()) {
            tickerDataCapture.set(!DateUtils.currentDateTime().isAfter(tickerDataCaptureEndDateTime));
        }
        return tickerDataCapture.get();
    }

    @Override
    public void checkExitFile() {
        if (isTradeStopped()) {
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
                tradeStopDateTime = DateUtils.currentDateTime();
                testRunEndDateTime = tradeStopDateTime.plusSeconds(parameters.getExitMaxDelaySeconds());
                log.info(
                        "Found force exit file {} containing valid exit code! Stopping trades now. Test Run will end at {}",
                        exitFile.getName(), DateUtils.formatDateTime(testRunEndDateTime));
                Files.delete(exitFile.toPath());
            }
        } catch (IOException e) {
            log.error("Error when reading exit file {}! Details: {}", exitFile, e.getMessage());
        }
    }

}
