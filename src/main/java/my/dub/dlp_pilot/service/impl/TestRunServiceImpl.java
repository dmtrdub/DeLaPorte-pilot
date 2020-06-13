package my.dub.dlp_pilot.service.impl;

import my.dub.dlp_pilot.configuration.ParametersComponent;
import my.dub.dlp_pilot.model.TestRun;
import my.dub.dlp_pilot.repository.TestRunRepository;
import my.dub.dlp_pilot.service.TestRunService;
import my.dub.dlp_pilot.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;

@Service
public class TestRunServiceImpl implements TestRunService {

    private final TestRunRepository repository;
    private final ParametersComponent parameters;

    private TestRun currentTestRun;

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
        testRun.setEndTime(startTime.plus(parameters.getTestRunDuration()));
        currentTestRun = repository.save(testRun);
    }

    @Override
    public TestRun getCurrentTestRun() {
        return currentTestRun;
    }
}
