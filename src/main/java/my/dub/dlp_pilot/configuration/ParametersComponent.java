package my.dub.dlp_pilot.configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import my.dub.dlp_pilot.Constants;
import my.dub.dlp_pilot.util.DateUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

@Component
@Getter
@Slf4j
public class ParametersComponent implements InitializingBean {

    @Value("${init_trade_delay_ms}")
    private long initDelayMs;
    @Value("${price_data_stale_difference_seconds}")
    private int staleDifferenceSeconds;
    @Value("${price_data_capture_period_seconds}")
    private int dataCapturePeriodSeconds;
    @Value("${price_data_capture_interval_duration}")
    private String dataCaptureIntervalDurationParam;
    @Value("${price_data_invalidate_after_seconds}")
    private int priceDataInvalidateAfterSeconds;
    @Value("${trade_entry_profit_percentage}")
    private double entryProfitPercentageDouble;
    @Value("${trade_entry_max_percentage}")
    private double entryMaxPercentageDouble;
    @Value("${trade_exit_profit_percentage}")
    private double exitProfitPercentageDouble;
    @Value("${trade_decrease_exit_profit_percentage_after_seconds}")
    private int exitProfitPercentageDecreaseAfterSeconds;
    @Value("${trade_decrease_exit_profit_percentage_by}")
    private double exitProfitPercentageDecreaseByDouble;
    @Value("${trade_entry_amounts_usd}")
    private String[] entryAmountsUsdParam;
    @Value("${trade_exit_sync_pnl_percentage_diff}")
    private double exitSyncOnPnlPercentageDiffDouble;
    @Value("${trade_minutes_timeout}")
    private int tradeMinutesTimeout;
    @Value("${trade_detrimental_amount_percentage}")
    private double detrimentAmountPercentageDouble;
    @Value("${trade_parallel_number}")
    private int parallelTradesNumber;
    @Value("${trade_suspense_after_detrimental_duration_seconds}")
    private int suspenseAfterDetrimentalSeconds;
    @Value("${test_run_duration}")
    private String testRunDurationParam;
    @Value("${test_run_result_csv_dir_path}")
    private String pathToResultDir;
    @Value("${test_run_forced_exit_file_path}")
    private String forcedExitFilePath;
    @Value("${test_run_forced_exit_code}")
    private String exitCode;
    @Value("${test_run_delay_on_exit_seconds}")
    private int exitDelaySeconds;

    private Duration dataCaptureIntervalDuration;
    private BigDecimal entryProfitPercentage;
    private BigDecimal entryMaxPercentage;
    private BigDecimal exitProfitPercentage;
    private BigDecimal exitProfitPercentageDecreaseBy;
    private BigDecimal exitSyncOnPnlPercentageDiff;
    private List<Double> entryAmounts;
    private BigDecimal detrimentAmountPercentage;
    private Duration testRunDuration;

    @Override
    public void afterPropertiesSet() {
        validateInputParams();
        initDelayMs = initDelayMs > 0 ? initDelayMs : 0;
        dataCaptureIntervalDuration = DateUtils.parseDuration(dataCaptureIntervalDurationParam);
        if (Duration.ZERO.equals(dataCaptureIntervalDuration)) {
            log.warn("Price data capture interval is set to 0! Memory may be overloaded!");
        } else {
            log.info("Price data capture interval is set to {}", DateUtils.formatDuration(dataCaptureIntervalDuration));
        }
        priceDataInvalidateAfterSeconds = priceDataInvalidateAfterSeconds > 0 ? priceDataInvalidateAfterSeconds : 0;
        entryProfitPercentage = BigDecimal.valueOf(entryProfitPercentageDouble);
        entryMaxPercentage = BigDecimal.valueOf(entryMaxPercentageDouble);
        exitProfitPercentage = BigDecimal.valueOf(exitProfitPercentageDouble);
        exitProfitPercentageDecreaseBy = BigDecimal.valueOf(exitProfitPercentageDecreaseByDouble);
        entryAmounts = Arrays.stream(entryAmountsUsdParam).mapToDouble(Double::parseDouble).boxed().distinct().sorted()
                .collect(Collectors.toList());
        detrimentAmountPercentage = BigDecimal.valueOf(detrimentAmountPercentageDouble);
        exitSyncOnPnlPercentageDiff = exitSyncOnPnlPercentageDiffDouble > 0
                ? BigDecimal.valueOf(exitSyncOnPnlPercentageDiffDouble)
                : BigDecimal.ZERO;
        tradeMinutesTimeout = tradeMinutesTimeout > 0 ? tradeMinutesTimeout : 0;
        parallelTradesNumber = parallelTradesNumber > 0 ? parallelTradesNumber : 0;
        suspenseAfterDetrimentalSeconds = suspenseAfterDetrimentalSeconds > 0 ? suspenseAfterDetrimentalSeconds : 0;
        exitDelaySeconds = exitDelaySeconds > 0 ? exitDelaySeconds : 0;
        testRunDuration = DateUtils.parseDuration(testRunDurationParam.toUpperCase());
        if (Duration.ZERO.equals(testRunDuration)) {
            log.warn("Test run duration is 0!");
        } else {
            log.info("Test run duration is set to {}", DateUtils.formatDuration(testRunDuration));
        }
    }

    private void validateInputParams() {
        if (staleDifferenceSeconds < 1) {
            throw new IllegalArgumentException("Stale difference for ticker cannot be < 1 seconds!");
        }
        if (dataCapturePeriodSeconds < 1) {
            throw new IllegalArgumentException("Price data capture interval cannot be < 1 seconds!");
        }
        if (entryProfitPercentageDouble < 0.0d) {
            throw new IllegalArgumentException("Trade entry min percentage cannot be < 0!");
        }
        if (entryMaxPercentageDouble <= entryProfitPercentageDouble) {
            throw new IllegalArgumentException("Trade entry max percentage cannot be <= entry min percentage!");
        }
        if (exitProfitPercentageDecreaseByDouble >= exitProfitPercentageDouble) {
            throw new IllegalArgumentException("Trade exit percentage diff decrease cannot be >= exit percentage!");
        }
        if (ArrayUtils.isEmpty(entryAmountsUsdParam)) {
            throw new IllegalArgumentException("Trade entry sums cannot be empty!");
        }
        if (String.join(",", entryAmountsUsdParam).length() > Constants.STRING_PARAM_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("Trade entry sums cannot be longer than %d chars!", Constants.STRING_PARAM_LENGTH));
        }
        if (Arrays.stream(entryAmountsUsdParam).mapToDouble(Double::parseDouble).boxed()
                .anyMatch(aDouble -> aDouble < 10)) {
            throw new IllegalArgumentException("Trade entry sum cannot be < $10");
        }
        if (detrimentAmountPercentageDouble <= 0) {
            throw new IllegalArgumentException("Detrimental exit amount percentage cannot be <= 0!");
        }
        if (detrimentAmountPercentageDouble >= 100) {
            throw new IllegalArgumentException("Detrimental exit amount percentage cannot be >= 100!");
        }
        if (StringUtils.isEmpty(testRunDurationParam)) {
            throw new IllegalArgumentException("Duration parameter cannot be empty!");
        }
        Duration duration = DateUtils.parseDuration(testRunDurationParam);
        if (duration == null || duration.equals(Duration.ZERO)) {
            throw new IllegalArgumentException("Duration parameter cannot be parsed!");
        }
        if (StringUtils.isEmpty(pathToResultDir)) {
            throw new IllegalArgumentException("Path to result dir parameter cannot be empty!");
        }
        if (pathToResultDir.length() > Constants.FILE_PATH_PARAM_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("Path to result dir parameter cannot be longer than %d chars!",
                                  Constants.FILE_PATH_PARAM_LENGTH));
        }
        if (StringUtils.isNotEmpty(forcedExitFilePath) && StringUtils.isBlank(exitCode)) {
            throw new IllegalArgumentException("Exit code cannot be empty if forced exit is enabled!");
        }
    }

    public BigDecimal getExitProfitPercentage(long tradeDurationSeconds) {
        if (exitProfitPercentageDecreaseAfterSeconds <= 0 || exitProfitPercentageDecreaseByDouble <= 0) {
            return exitProfitPercentage;
        }
        long decreaseTimes = tradeDurationSeconds / exitProfitPercentageDecreaseAfterSeconds;
        if (decreaseTimes <= 0) {
            return exitProfitPercentage;
        }
        BigDecimal exitPerc = exitProfitPercentage
                .subtract(exitProfitPercentageDecreaseBy.multiply(BigDecimal.valueOf(decreaseTimes)));
        return exitPerc.compareTo(BigDecimal.ZERO) <= 0 ? BigDecimal.valueOf(0.01d) : exitPerc;
    }

    public Optional<String> getConfiguration() {
        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream(ResourceUtils.getFile("classpath:application.properties")));
        } catch (IOException e) {
            log.error("Unable to load application.properties file! Details: " + e.getMessage());
            return Optional.empty();
        }
        List<String> configProperties = prop.entrySet().stream().filter(entry -> {
            String key = (String) entry.getKey();
            return key.startsWith("price") || key.startsWith("trade");
        }).map(entry -> entry.getKey() + ":" + entry.getValue()).collect(Collectors.toList());
        return Optional.of(String.join(";", configProperties));
    }

    public BigDecimal getMinEntryAmount() {
        return BigDecimal.valueOf(entryAmounts.get(0));
    }
}
