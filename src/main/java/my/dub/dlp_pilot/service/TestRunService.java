package my.dub.dlp_pilot.service;

import my.dub.dlp_pilot.model.Exchange;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.TestRun;
import org.springframework.lang.NonNull;

public interface TestRunService {

    void init();

    boolean runPreload(Exchange exchange);

    void onPreloadComplete(ExchangeName exchangeName);

    boolean runRefreshLoad(ExchangeName exchange);

    void onRefreshLoadComplete(ExchangeName exchangeName, boolean isPreloadComplete);

    void runTest(ExchangeName exchangeName);

    void prepareRunTest();

    void onExit();

    void updateResultFile(@NonNull String filePath);

    TestRun getCurrentTestRun();

    boolean checkTestRunEnd();

    void checkExitFile();

    boolean checkTradeStopped();
}
