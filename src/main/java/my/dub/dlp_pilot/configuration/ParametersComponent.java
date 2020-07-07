package my.dub.dlp_pilot.configuration;

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

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

@Component
@Getter
@Slf4j
public class ParametersComponent implements InitializingBean {

    @Value("${init_trade_delay_ms}")
    private long initDelayMs;
    @Value("${ticker_stale_difference_seconds}")
    private int staleDifferenceSeconds;
    @Value("${trade_entry_min_percentage}")
    private double entryMinPercentageDouble;
    @Value("${trade_entry_max_percentage}")
    private double entryMaxPercentageDouble;
    @Value("${trade_exit_diff_percentage}")
    private double exitPercentageDiffDouble;
    @Value("${trade_decrease_exit_diff_percentage_after_seconds}")
    private int exitPercentageDiffDecreaseAfterSeconds;
    @Value("${trade_decrease_exit_diff_percentage_by}")
    private double exitPercentageDiffDecreaseByDouble;
    @Value("${trade_entry_amounts_usd}")
    private String[] entryAmountsUsdParam;
    @Value("${trade_minutes_timeout}")
    private int tradeMinutesTimeout;
    @Value("${trade_detrimental_percentage_delta}")
    private double detrimentPercentageDeltaDouble;
    @Value("${trade_detrimental_entry_amount_percentage}")
    private double detrimentEntryAmountPercentageDouble;
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
    @Value("${test_run_max_delay_on_exit_seconds}")
    private int exitMaxDelaySeconds;

    private BigDecimal entryMinPercentage;
    private BigDecimal entryMaxPercentage;
    private BigDecimal exitPercentageDiff;
    private BigDecimal exitPercentageDiffDecreaseBy;
    private List<Double> entryAmounts;
    private BigDecimal detrimentPercentageDelta;
    private BigDecimal detrimentEntryAmountPercentage;
    private Duration testRunDuration;

    @Override
    public void afterPropertiesSet() {
        validateInputParams();
        initDelayMs = initDelayMs > 0 ? initDelayMs : 0;
        entryMinPercentage = BigDecimal.valueOf(entryMinPercentageDouble);
        entryMaxPercentage = BigDecimal.valueOf(entryMaxPercentageDouble);
        exitPercentageDiff = BigDecimal.valueOf(exitPercentageDiffDouble);
        exitPercentageDiffDecreaseBy = BigDecimal.valueOf(exitPercentageDiffDecreaseByDouble);
        entryAmounts = Arrays.stream(entryAmountsUsdParam).mapToDouble(Double::parseDouble).boxed().distinct().sorted()
                             .collect(Collectors.toList());
        detrimentPercentageDelta = BigDecimal.valueOf(detrimentPercentageDeltaDouble);
        detrimentEntryAmountPercentage = BigDecimal.valueOf(detrimentEntryAmountPercentageDouble);
        parallelTradesNumber = parallelTradesNumber > 0 ? parallelTradesNumber : 0;
        suspenseAfterDetrimentalSeconds = suspenseAfterDetrimentalSeconds > 0 ? suspenseAfterDetrimentalSeconds : 0;
        testRunDuration = DateUtils.parseDuration(testRunDurationParam.toUpperCase());
    }

    private void validateInputParams() {
        if (staleDifferenceSeconds < 1) {
            throw new IllegalArgumentException("Stale difference for ticker cannot be < 1 seconds!");
        }
        if (entryMinPercentageDouble <= 0.0) {
            throw new IllegalArgumentException("Trade entry min percentage cannot be <= 0!");
        }
        if (entryMaxPercentageDouble <= entryMinPercentageDouble) {
            throw new IllegalArgumentException("Trade entry max percentage cannot be <= entry min percentage!");
        }
        if (exitPercentageDiffDouble >= entryMinPercentageDouble) {
            throw new IllegalArgumentException("Trade exit percentage diff cannot be >= entry min percentage!");
        }
        if (exitPercentageDiffDecreaseByDouble >= exitPercentageDiffDouble) {
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
        if (tradeMinutesTimeout <= 0) {
            throw new IllegalArgumentException("Trade timeout minutes cannot be <= 0!");
        }
        if (exitPercentageDiffDecreaseAfterSeconds >= (tradeMinutesTimeout * 60)) {
            throw new IllegalArgumentException(
                    "Exit percentage decrease diff after seconds cannot be >= trade timeout minutes!");
        }
        if (detrimentPercentageDeltaDouble <= 0) {
            throw new IllegalArgumentException("Detrimental percentage delta cannot be <= 0!");
        }
        if (detrimentEntryAmountPercentageDouble <= 0) {
            throw new IllegalArgumentException("Detrimental entry amount percentage cannot be <= 0!");
        }
        if (detrimentEntryAmountPercentageDouble >= 100) {
            throw new IllegalArgumentException("Detrimental entry amount percentage cannot be >= 100!");
        }
        if (StringUtils.isEmpty(testRunDurationParam)) {
            throw new IllegalArgumentException("Duration parameter cannot be empty!");
        }
        if (DateUtils.parseDuration(testRunDurationParam) == null) {
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
        if (exitMaxDelaySeconds < 1) {
            throw new IllegalArgumentException("Max delay before end of test run cannot be < 1 seconds!");
        }
    }

    public BigDecimal getExitPercentageDiff(long tradeDurationSeconds) {
        if (exitPercentageDiffDecreaseAfterSeconds <= 0 || exitPercentageDiffDecreaseByDouble <= 0) {
            return exitPercentageDiff;
        }
        long decreaseTimes = tradeDurationSeconds / exitPercentageDiffDecreaseAfterSeconds;
        if (decreaseTimes <= 0) {
            return exitPercentageDiff;
        }
        return exitPercentageDiff.subtract(exitPercentageDiffDecreaseBy.multiply(BigDecimal.valueOf(decreaseTimes)));
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
            return key.startsWith("ticker") || key.startsWith("trade");
        }).map(entry -> entry.getKey() + ":" + entry.getValue()).collect(Collectors.toList());
        return Optional.of(String.join(";", configProperties));
    }
}
