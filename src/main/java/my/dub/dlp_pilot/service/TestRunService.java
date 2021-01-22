package my.dub.dlp_pilot.service;

import my.dub.dlp_pilot.model.Exchange;
import my.dub.dlp_pilot.model.ExchangeName;
import my.dub.dlp_pilot.model.TestRun;
import org.springframework.lang.NonNull;

/**
 * Service for executing test run process operations.
 */
public interface TestRunService {

    /**
     * Initialize data before preload stage.
     */
    void init();

    /**
     * Execute preload stage for a specific exchange.
     *
     * @param exchange
     *         a non-null {@link Exchange}
     *
     * @return {@code true} if preload stage is completed, {@code false} otherwise
     */
    boolean runPreload(@NonNull Exchange exchange);

    /**
     * Execute post-preload operations for a specific exchange.
     *
     * @param exchangeName
     *         a non-null {@link ExchangeName} of exchange
     */
    void onPreloadComplete(@NonNull ExchangeName exchangeName);

    /**
     * Execute refresh load stage for a specific exchange.
     *
     * @param exchangeName
     *         a non-null {@link ExchangeName} of exchange
     *
     * @return {@code true} if refresh load stage is completed, {@code false} otherwise
     */
    boolean runRefreshLoad(@NonNull ExchangeName exchangeName);

    /**
     * Execute post-refresh load operations, distinguished by current state of preload stage.
     *
     * @param exchangeName
     *         a non-null {@link ExchangeName} of exchange
     * @param isPreloadComplete
     *         a boolean flag indicating whether preload stage is complete ({@code true})
     */
    void onRefreshLoadComplete(@NonNull ExchangeName exchangeName, boolean isPreloadComplete);

    /**
     * Execute test stage for a specific exchange.
     *
     * @param exchangeName
     *         a non-null {@link ExchangeName} of exchange
     */
    void runTest(@NonNull ExchangeName exchangeName);

    /**
     * Execute pre-test operations common for all exchanges.
     */
    void prepareRunTest();

    /**
     * Execute operations before successful application exit (test stage is successfully completed).
     */
    void onExit();

    /**
     * Update the path to result file value of current {@link TestRun}.
     *
     * @param filePath
     *         non-null String path value
     */
    void updateResultFile(@NonNull String filePath);

    /**
     * Get the current {@link TestRun} entity.
     *
     * @return current {@link TestRun} entity
     */
    TestRun getCurrentTestRun();

    /**
     * Check if test stage is ended based on time value.
     *
     * @return boolean value representing whether {@link TestRun#getEndTime()} is passed ({@code true})
     */
    boolean checkTestRunEnd();

    /**
     * Check the state of trigger file for forced exit. If such file is detected - update current {@link TestRun}
     * properties.
     */
    void checkExitFile();

    /**
     * Check if time of trade stop (during test stage) is passed based on time value.
     *
     * @return boolean value representing whether trade stop time is passed ({@code true})
     */
    boolean checkTradeStopped();
}
