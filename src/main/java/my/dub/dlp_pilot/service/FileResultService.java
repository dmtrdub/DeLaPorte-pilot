package my.dub.dlp_pilot.service;

import my.dub.dlp_pilot.exception.TestRunEndException;
import my.dub.dlp_pilot.model.TestRun;
import my.dub.dlp_pilot.model.Trade;

/**
 * A service for recording Trade results to a local file.
 */
public interface FileResultService {

    /**
     * Initiate creation of result file and setting of internal field values.
     */
    void init();

    /**
     * Get all completed {@link Trade}s for the current {@link TestRun}, and write to a file with a predefined
     * structure, updating the fetched Trade entities. If no completed trades are left, and the test run is finished,
     * terminate by throwing a {@link TestRunEndException} exception.
     *
     * @throws TestRunEndException
     *         if all trades are closed, and all completed trades are written to file
     */
    void write();
}
