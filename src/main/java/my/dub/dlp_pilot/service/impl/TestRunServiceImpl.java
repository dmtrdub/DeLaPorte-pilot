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

@Slf4j
@Service
public class TestRunServiceImpl implements TestRunService {

    private final TestRunRepository repository;
    private final ParametersComponent parameters;

    private TestRun currentTestRun;
    private ZonedDateTime tradeStopDateTime;
    private ZonedDateTime testRunEndDateTime;
    private ZonedDateTime tickerDataCaptureEndDateTime;
    private boolean tradeStop;
    private boolean testRunEnd;
    private boolean tickerDataCapture;

    @Autowired
    public TestRunServiceImpl(TestRunRepository repository, ParametersComponent parameters) {
        this.repository = repository;
        this.parameters = parameters;
    }

    @Transactional
    @Override
    public void createAndSave() {
        TestRun testRun = new TestRun();
        testRun.setEntryAmountsUsd(String.join(",", parameters.getEntryAmountsUsdParam()));
        testRun.setEntryMinPercentage(parameters.getEntryMinPercentageDouble());
        testRun.setEntryMaxPercentage(parameters.getEntryMaxPercentageDouble());
        testRun.setExitDiffPercentage(parameters.getExitPercentageDiffDouble());
        testRun.setTradeTimeoutMins(parameters.getTradeMinutesTimeout());
        testRun.setDetrimentalPercentageDelta(parameters.getDetrimentPercentageDeltaDouble());
        ZonedDateTime startTime = DateUtils.currentDateTime();
        testRun.setStartTime(startTime);
        int tickerDataCaptureDurationSeconds = parameters.getStaleDifferenceSeconds() + 1;
        testRun.setEndTime(
                startTime.plus(parameters.getTestRunDuration()).plusSeconds(tickerDataCaptureDurationSeconds));
        currentTestRun = repository.save(testRun);
        tradeStopDateTime = currentTestRun.getEndTime();
        testRunEndDateTime = tradeStopDateTime.plusSeconds(parameters.getExitMaxDelaySeconds());
        tickerDataCaptureEndDateTime = startTime.plusSeconds(tickerDataCaptureDurationSeconds);
    }

    @Override
    public TestRun getCurrentTestRun() {
        return currentTestRun;
    }

    @Override
    public boolean isTradeStopped() {
        if (tradeStopDateTime != null && !tradeStop) {
            tradeStop = DateUtils.currentDateTime().isAfter(tradeStopDateTime);
        }
        return tradeStop;
    }

    @Override
    public boolean isTestRunEnd() {
        if (testRunEndDateTime != null && !testRunEnd) {
            testRunEnd = DateUtils.currentDateTime().isAfter(testRunEndDateTime);
        }
        return testRunEnd;
    }

    @Override
    public boolean isTickerDataCapture() {
        if (tickerDataCaptureEndDateTime != null && !tickerDataCapture) {
            tickerDataCapture = DateUtils.currentDateTime().isBefore(tickerDataCaptureEndDateTime);
        }
        return testRunEnd;
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
