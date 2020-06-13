package my.dub.dlp_pilot.service;

import my.dub.dlp_pilot.model.TestRun;

public interface TestRunService {

    void createAndSave();

    TestRun getCurrentTestRun();
}
