package my.dub.dlp_pilot.service.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import my.dub.dlp_pilot.Constants;
import my.dub.dlp_pilot.configuration.ParametersComponent;
import my.dub.dlp_pilot.exception.TestRunEndException;
import my.dub.dlp_pilot.model.Position;
import my.dub.dlp_pilot.model.TestRun;
import my.dub.dlp_pilot.model.Trade;
import my.dub.dlp_pilot.service.FileResultService;
import my.dub.dlp_pilot.service.TestRunService;
import my.dub.dlp_pilot.service.TradeService;
import my.dub.dlp_pilot.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Slf4j
@Component
public class FileResultServiceImpl implements FileResultService {
    private static final String USD = "USD";

    private final TradeService tradeService;
    private final TestRunService testRunService;
    private final ParametersComponent parameters;

    private String header;
    private Path filePath;

    @Autowired
    public FileResultServiceImpl(TradeService tradeService, TestRunService testRunService,
            ParametersComponent parameters) {
        this.tradeService = tradeService;
        this.testRunService = testRunService;
        this.parameters = parameters;
    }

    @Override
    public void init() {
        List<Double> entryAmounts = parameters.getEntryAmounts();
        String dynamicDelimiter = "_" + USD + ",";
        String dynamicHeaders = entryAmounts.stream().map(amount -> String
                .join(dynamicDelimiter, "PnL_Short_" + amount, "PnL_Long_" + amount, "Expenses_" + amount,
                      "Income" + amount)).collect(Collectors.joining(dynamicDelimiter)) + "_" + USD;
        header = String.join(",", "Base", "Target", "Entry_Percentage_Diff", dynamicHeaders, "Time_Start", "Time_End",
                             "Duration_(sec)", "Result_Type", "Exchange_Short", "Exchange_Long");
        initFile();
    }

    @Override
    public void write() {
        Set<Trade> completedTrades = tradeService.getCompletedTradesNotWrittenToFile();
        Set<String> linesToWrite = completedTrades.stream().map(this::getTradeResultString).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(linesToWrite)) {
            if (testRunService.isTestRunEnd()) {
                throw new TestRunEndException();
            }
            return;
        }
        try {
            Files.write(filePath, linesToWrite, StandardOpenOption.APPEND);
            tradeService.updateTradesWrittenToFile(completedTrades);
            log.debug("Successfully written {} lines to test result file {}", linesToWrite.size(), filePath);
        } catch (IOException e) {
            log.error("Error when writing to test result file {}! Details: {}", filePath, e.getMessage());
        }
    }

    @SneakyThrows
    private void initFile() {
        TestRun currentTestRun = testRunService.getCurrentTestRun();
        String fileName = "test-run#" + currentTestRun.getId() + "_" + DateUtils
                .formatDateTimeShort(currentTestRun.getStartTime()) + "_" + DateUtils
                .formatDateTimeShort(currentTestRun.getEndTime()) + ".csv";
        String resultDir = parameters.getPathToResultDir();
        Files.createDirectories(Path.of(resultDir));
        Path fullFilePath = Path.of(resultDir, fileName);
        filePath = Files.write(fullFilePath, List.of(header), StandardOpenOption.CREATE);
        log.info("Created test result file: {}", filePath);
    }

    private String getTradeResultString(Trade trade) {
        Position positionShort = trade.getPositionShort();
        Position positionLong = trade.getPositionLong();
        String dynamicResult = trade.getResultData().stream().map(resultData -> String.join(",", decimalResults(
                resultData.getPnlUsdShort(), resultData.getPnlUsdLong(), resultData.getTotalExpensesUsd(),
                resultData.getIncomeUsd()))).collect(Collectors.joining(","));
        ZonedDateTime startTime = trade.getStartTime();
        ZonedDateTime endTime = trade.getEndTime();
        return String.join(",", trade.getBase(), trade.getTarget(), decimalResult(trade.getEntryPercentageDiff()),
                           dynamicResult, DateUtils.formatDateTime(startTime), DateUtils.formatDateTime(endTime),
                           DateUtils.durationSecondsDetailed(startTime, endTime), trade.getResultType().name(),
                           positionShort.getExchange().getFullName(), positionLong.getExchange().getFullName());
    }

    private String decimalResult(BigDecimal decimal) {
        return decimal.setScale(Constants.MAX_RESULT_SCALE, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private List<String> decimalResults(BigDecimal... decimals) {
        return Arrays.stream(decimals).map(this::decimalResult).collect(Collectors.toList());
    }
}
