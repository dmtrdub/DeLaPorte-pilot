package my.dub.dlp_pilot.service.impl;

import static com.google.common.base.Preconditions.checkState;
import static my.dub.dlp_pilot.util.Calculations.decimalResult;
import static my.dub.dlp_pilot.util.Calculations.originalDecimalResult;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import my.dub.dlp_pilot.configuration.ParametersHolder;
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
    private final ParametersHolder parameters;

    private String header;
    private Path filePath;

    @Autowired
    public FileResultServiceImpl(TradeService tradeService, TestRunService testRunService,
            ParametersHolder parameters) {
        this.tradeService = tradeService;
        this.testRunService = testRunService;
        this.parameters = parameters;
    }

    @Override
    public void init() {
        double entryAmount = parameters.getEntryAmountUsdDouble();
        String amountHeaderPrefix = String.format("_%s_%s", entryAmount, USD);
        header = String.join(",", "Base", "Target", "Entry_Percentage_Diff", "Open_Price_Diff", "Average_Price_Diff",
                             "Close_Price_Diff", "PnL_Min_Short" + amountHeaderPrefix, "PnL_Short_Min_Time",
                             "PnL_Short" + amountHeaderPrefix, "PnL_Max_Short" + amountHeaderPrefix,
                             "PnL_Short_Max_Time", "PnL_Min_Long" + amountHeaderPrefix, "PnL_Long_Min_Time",
                             "PnL_Long" + amountHeaderPrefix, "PnL_Max_Long" + amountHeaderPrefix, "PnL_Long_Max_Time",
                             "Expenses_" + amountHeaderPrefix, "Income_" + amountHeaderPrefix, "Time_Start", "Time_End",
                             "Duration_(sec)", "Result_Type", "Exchange_Short", "Exchange_Long");
        initFile();
    }

    @Override
    public void write() {
        Set<Trade> completedTrades =
                tradeService.getCompletedTradesNotWrittenToFile(testRunService.getCurrentTestRun());
        if (CollectionUtils.isEmpty(completedTrades)) {
            if (testRunService.checkTestRunEnd()) {
                checkState(tradeService.isAllTradesClosed(), "Cannot exit test run if some trades are still opened!");
                throw new TestRunEndException();
            }
            return;
        }
        Set<String> linesToWrite = completedTrades.stream().map(this::getTradeResultString).collect(Collectors.toSet());
        try {
            Files.write(filePath, linesToWrite, StandardOpenOption.APPEND);
            completedTrades.forEach(trade -> trade.setWrittenToFile(true));
            tradeService.saveOrUpdate(completedTrades);
            log.debug("Successfully written {} lines to test result file {}", linesToWrite.size(), filePath);
        } catch (IOException e) {
            log.error("Error when writing to test result file {}! Details: {}", filePath, e.getMessage());
        }
    }

    @SneakyThrows
    private void initFile() {
        TestRun currentTestRun = testRunService.getCurrentTestRun();
        String fileName = "test-run#" + currentTestRun.getId() + "_" + DateUtils
                .formatDateTimeShort(currentTestRun.getStartTime()) + ".csv";
        String resultDir = parameters.getPathToResultDir();
        Files.createDirectories(Path.of(resultDir));
        Path fullFilePath = Path.of(resultDir, fileName);
        filePath = Files.write(fullFilePath, List.of(header), StandardOpenOption.CREATE);
        log.info("Created test result file: {}", filePath);
        testRunService.updateResultFile(filePath.toString());
    }

    private String getTradeResultString(Trade trade) {
        Position positionShort = trade.getPositionShort();
        Position positionLong = trade.getPositionLong();

        LocalDateTime startTime = DateUtils.toLocalDateTime(trade.getStartTime());
        LocalDateTime endTime = DateUtils.toLocalDateTime(trade.getEndTime());
        return joinResults(trade.getBase(), trade.getTarget(), trade.getEntryPercentageDiff(),
                           originalDecimalResult(trade.getOpenPriceDiff()),
                           originalDecimalResult(trade.getAveragePriceDiff()),
                           originalDecimalResult(trade.getClosePriceDiff()), positionShort.getMinPnlUsd(),
                           positionShort.getMinPnlTime(), positionShort.getPnlUsd(), positionShort.getMaxPnlUsd(),
                           positionShort.getMaxPnlTime(), positionLong.getMinPnlUsd(), positionLong.getMinPnlTime(),
                           positionLong.getPnlUsd(), positionLong.getMaxPnlUsd(), positionLong.getMaxPnlTime(),
                           trade.getTotalExpensesUsd(), trade.getIncomeUsd(), startTime, endTime,
                           DateUtils.durationSecondsDetailed(startTime, endTime), trade.getResultType(),
                           positionShort.getExchange().getFullName(), positionLong.getExchange().getFullName());
    }

    private String joinResults(Object... values) {
        List<String> result = new ArrayList<>();
        Arrays.stream(values).forEachOrdered(value -> {
            if (value == null) {
                result.add("");
            } else if (value instanceof BigDecimal) {
                result.add(decimalResult((BigDecimal) value));
            } else if (value instanceof Instant) {
                result.add(DateUtils.formatLocalDateTime((Instant) value));
            } else if (value instanceof LocalDateTime) {
                result.add(DateUtils.formatDateTime((LocalDateTime) value));
            } else if (value instanceof Enum<?>) {
                result.add(((Enum<?>) value).name());
            } else {
                result.add(value.toString());
            }
        });
        return String.join(",", result);
    }
}
